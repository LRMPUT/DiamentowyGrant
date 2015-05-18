#include "graphManager.h"

GraphManager::GraphManager() {
	prevUserPositionTheta = prevUserPositionX = prevUserPositionY = 0.0;

	BlockSolverX::LinearSolverType* linearSolver = new LinearSolverPCG<
			BlockSolverX::PoseMatrixType>();
	BlockSolverX* blockSolver = new BlockSolverX(linearSolver);

	// create the algorithm to carry out the optimization
	OptimizationAlgorithmLevenberg* optimizationAlgorithm =
			new OptimizationAlgorithmLevenberg(blockSolver);

	optimizer.setVerbose(true);
	optimizer.setAlgorithm(optimizationAlgorithm);
}

int GraphManager::optimize(int iterationCount) {
	int res = optimizer.initializeOptimization();
	//optimizer.computeInitialGuess();

	for (int i=0;i<2;i++)
	{
		res = optimizer.optimize(iterationCount);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Chi2 [%f]",
						optimizer.chi2());
	}

	return res;
}

int GraphManager::saveOptimizationResult(ofstream &ofs) {
	int res = optimizer.save(ofs);
	return res;
}

void GraphManager::addToGraph(string dataToProcess) {
	// Convert string to stream to process line by line
	std::istringstream f(dataToProcess);
	std::string line;

	// Get new line
	while (std::getline(f, line)) {
		stringstream data(line);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Adding: [%s]",
				data.str().c_str());
		// Getting operation type
		string type;
		data >> type;

		// Adding vertex/edge
		if (type == "EDGE_SE2:WIFI")
			addEdgeWiFi(data);
		else if (type == "EDGE_SE2:STEP")
			addEdgeStepometer(data);
		else if (type == "EDGE_SE2")
			addEdgeSE2(data);
		else if (type == "EDGE_SE2:WIFI_FINGERPRINT")
			addEdgeWiFiFingerprint(data);
		else if (type == "VERTEX_SE2")
			addVertex(data, 0);
		else if (type == "VERTEX_XY")
			addVertex(data, 1);
	}

}

int GraphManager::addVertex(stringstream &data, int type) {

	g2o::OptimizableGraph::Vertex* v;

	if (type == 0)
		v = new VertexSE2();
	else if (type == 1)
		v = new VertexPointXY();

	int id;
	data >> id;
	if (id == 0 || (id >= 5000 && id < 10000)) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
				"NDK: setFixed(TRUE)!");
		v->setFixed(true);
	}

	bool r = v->read(data);
	if (!r) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
				"Error reading vertex!");
		return -1;
	}
	v->setId(id);
	r = optimizer.addVertex(v);
	if (!r) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
				"Failure adding Vertex!");
		return -1;
	}
}

int GraphManager::addEdgeWiFi(stringstream &data) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Adding wifi edge");
	int id1, id2;
	EdgeSE2PointXYDistance* e = new EdgeSE2PointXYDistance();
	data >> id1 >> id2;
	OptimizableGraph::Vertex* from = optimizer.vertex(id1);
	OptimizableGraph::Vertex* to = optimizer.vertex(id2);
	if (!to) {
		stringstream tmp;
		tmp << id2 << " " << prevUserPositionX << " " << prevUserPositionY
				<< "\n";
		addVertex(tmp, 1);
		to = optimizer.vertex(id2);
	}

	if (from && to) {
		e->setVertex(0, from);
		e->setVertex(1, to);
		e->read(data);
		if (!optimizer.addEdge(e)) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Unable to add edge wifi");
			delete e;
			return -1;
		}
	}
	return 0;
}

int GraphManager::addEdgeWiFiFingerprint(stringstream &data) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Adding wifi fingerprint edge");
	int id1, id2;
	EdgeSE2PlaceVicinity* e = new EdgeSE2PlaceVicinity();

	data >> id1 >> id2;
	OptimizableGraph::Vertex* from = optimizer.vertex(id1);
	OptimizableGraph::Vertex* to = optimizer.vertex(id2);

	if (from && to) {
		e->setVertex(0, from);
		e->setVertex(1, to);
		e->read(data);
		if (!optimizer.addEdge(e)) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Unable to add edge wifi fingerprint");
			delete e;
			return -1;
		}
	}
	return 0;
}

int GraphManager::addEdgeStepometer(stringstream &data) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Adding stepometer edge");

	int id1, id2;
	EdgeSE2DistanceOrientation* e = new EdgeSE2DistanceOrientation();
	data >> id1 >> id2;
	OptimizableGraph::Vertex* from = optimizer.vertex(id1);
	OptimizableGraph::Vertex* to = optimizer.vertex(id2);
	if (!from) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
				"Adding initial vertex");
		stringstream tmp;
		tmp << id1 << " 0.0 0.0 0.0\n";
		addVertex(tmp, 0);
		from = optimizer.vertex(id1);
	}

	if (!to) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
				"Adding new vertex");
		stringstream tmpStream(data.str());
		double distance, theta;
		string xxx;
		tmpStream >> xxx >> id1 >> id2 >> distance >> theta;
		prevUserPositionTheta = prevUserPositionTheta + theta;
		prevUserPositionX = prevUserPositionX
				+ distance * cos(prevUserPositionTheta);
		prevUserPositionY = prevUserPositionY
				+ distance * sin(prevUserPositionTheta);

		stringstream tmp;
		tmp << id2 << " " << prevUserPositionX << " " << prevUserPositionY
				<< " " << prevUserPositionTheta << " \n";
		addVertex(tmp, 0);
		to = optimizer.vertex(id2);
	}

	if (from && to) {
		e->setVertex(0, from);
		e->setVertex(1, to);
		e->read(data);
		if (!optimizer.addEdge(e)) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Unable to add edge stepometer");
			delete e;
			return -1;
		}
	}
	return 0;
}


int GraphManager::addEdgeSE2(stringstream &data) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Adding SE2 edge");

	int id1, id2;
	EdgeSE2* e = new EdgeSE2();
	data >> id1 >> id2;
	OptimizableGraph::Vertex* from = optimizer.vertex(id1);
	OptimizableGraph::Vertex* to = optimizer.vertex(id2);
	if (!from) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
				"Adding initial vertex");
		stringstream tmp;
		tmp << id1 << " 0.0 0.0 0.0\n";
		addVertex(tmp, 0);
		from = optimizer.vertex(id1);
	}

	if (!to) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
				"Adding new vertex");
		stringstream tmpStream(data.str());
		double distX, distY, theta;
		string xxx;
		tmpStream >> xxx >> id1 >> id2 >> distX >> distY >> theta;

		prevUserPositionX = prevUserPositionX + cos(prevUserPositionTheta) * distX - sin(prevUserPositionTheta) * distY;
		prevUserPositionY = prevUserPositionY + sin(prevUserPositionTheta) * distX + cos(prevUserPositionTheta) * distY;

		prevUserPositionTheta = prevUserPositionTheta + theta;

		stringstream tmp;
		tmp << id2 << " " << prevUserPositionX << " " << prevUserPositionY
				<< " " << prevUserPositionTheta << " \n";
		addVertex(tmp, 0);
		to = optimizer.vertex(id2);
	}

	if (from && to) {
		e->setVertex(0, from);
		e->setVertex(1, to);
		e->read(data);
		if (!optimizer.addEdge(e)) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]",
					"Unable to add edge SE2");
			delete e;
			return -1;
		}
	}
	return 0;
}
