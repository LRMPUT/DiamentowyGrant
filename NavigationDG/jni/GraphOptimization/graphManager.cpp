#include "graphManager.h"

GraphManager::GraphManager() {

	if (pthread_mutex_init(&graphMtx, NULL)) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "Mutex init failed!");
	}

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
	pthread_mutex_lock(&graphMtx);

	if ( optimizer.edges().size() == 0 )
	{
		pthread_mutex_unlock(&graphMtx);
		return 0;
	}

	// TODO: Not sure if it is supposed to be here
	int res = optimizer.initializeOptimization();
	//optimizer.computeInitialGuess();

	res = optimizer.optimize(iterationCount);

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Chi2 [%f]",
						optimizer.chi2());



	std::set<g2o::OptimizableGraph::Vertex*,
				g2o::OptimizableGraph::VertexIDCompare> verticesToCopy;
		for (g2o::HyperGraph::EdgeSet::const_iterator it =
				optimizer.edges().begin(); it != optimizer.edges().end(); ++it) {
			g2o::OptimizableGraph::Edge* e =
					static_cast<g2o::OptimizableGraph::Edge*>(*it);
			if (e->level() == 0) {
				for (std::vector<g2o::HyperGraph::Vertex*>::const_iterator it =
						e->vertices().begin(); it != e->vertices().end(); ++it) {
					g2o::OptimizableGraph::Vertex* v =
							static_cast<g2o::OptimizableGraph::Vertex*>(*it);
					//__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Vertex id: %d and fixed: %d", v->id(), v->fixed());
					if (!v->fixed())
						verticesToCopy.insert(
								static_cast<g2o::OptimizableGraph::Vertex*>(*it));
				}
			}
		}

		for (std::set<g2o::OptimizableGraph::Vertex*,
				g2o::OptimizableGraph::VertexIDCompare>::const_iterator it =
				verticesToCopy.begin(); it != verticesToCopy.end(); ++it) {
			g2o::OptimizableGraph::Vertex* v = *it;
			std::vector<double> estimate;
			v->getEstimateData(estimate);

			int index = findIndexInVertices(v->id());

			if (index < 0) {
				__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Wanted to update the estimates, but there is no vertex with wanted id");
			}
			if(	vertices[ index ]->type == ail::Vertex::Type::VERTEX2D) {
				ail::Vertex2D* vertex = static_cast<ail::Vertex2D*> (vertices[index]);
				vertex->pos[0] = estimate[0];
				vertex->pos[1] = estimate[1];
			}
			else if (vertices[ index ]->type == ail::Vertex::Type::VERTEXSE2) {
				ail::VertexSE2* vertex = static_cast<ail::VertexSE2*> (vertices[index]);
				vertex->pos[0] = estimate[0];
				vertex->pos[1] = estimate[1];
				vertex->orient = estimate[2];
			}

			//__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Vertex id: %d\tEstimates: %f %f %f", v->id(), estimate[0], estimate[1], estimate[2]);
		}

	pthread_mutex_unlock(&graphMtx);
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

	pthread_mutex_lock(&graphMtx);

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
		else if (type == "EDGE_SE2:VPR_VICINITY")
			addEdgeVPRVicinity(data);
		else if (type == "VERTEX_SE2")
			addVertex(data, 0);
		else if (type == "VERTEX_XY")
			addVertex(data, 1);
	}
	pthread_mutex_unlock(&graphMtx);

}

// Get information about position of vertex with given id
std::vector<double> GraphManager::getVertexPosition(int id) {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK: Called getVertexPosition");

	std::vector<double> estimate;
	for (int i=0;i<vertices.size();i++) {
		if (vertices[i]->vertexId == id)
		{
			estimate.push_back(id);
			if ( vertices[i]->type == ail::Vertex::Type::VERTEX2D) {
				ail::Vertex2D* vertex = static_cast<ail::Vertex2D*> (vertices[i]);
				estimate.push_back(vertex->pos[0]);
				estimate.push_back(vertex->pos[1]);
			}
			else if (vertices[i]->type == ail::Vertex::Type::VERTEXSE2) {
				ail::VertexSE2* vertex = static_cast<ail::VertexSE2*>(vertices[i]);
				estimate.push_back(vertex->pos[0]);
				estimate.push_back(vertex->pos[1]);
				estimate.push_back(vertex->orient);
			}
		}
	}
	return estimate;
}

// Get information about position of all vertices
std::vector<double> GraphManager::getPositionOfAllVertices() {
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG,
			"NDK: Called getPositionOfAllVertices");

	std::vector<double> estimate;
	for (int i = 0; i < vertices.size(); i++) {
		estimate.push_back(vertices[i]->vertexId);
		if (vertices[i]->type == ail::Vertex::Type::VERTEX2D) {
			ail::Vertex2D* vertex = static_cast<ail::Vertex2D*>(vertices[i]);
			estimate.push_back(vertex->pos[0]);
			estimate.push_back(vertex->pos[1]);
			estimate.push_back(0);
		} else if (vertices[i]->type == ail::Vertex::Type::VERTEXSE2) {
			ail::VertexSE2* vertex = static_cast<ail::VertexSE2*>(vertices[i]);
			estimate.push_back(vertex->pos[0]);
			estimate.push_back(vertex->pos[1]);
			estimate.push_back(vertex->orient);
		}

	}
	return estimate;
}

int GraphManager::addVertex(stringstream &data, int type) {

	g2o::OptimizableGraph::Vertex* v;
	ail::Vertex *ailVertex;

	if (type == 0)
	{
		v = new VertexSE2();
		ailVertex = new ail::VertexSE2();
		ailVertex->type = ail::Vertex::Type::VERTEXSE2;
	}
	else if (type == 1)
	{
		v = new VertexPointXY();
		ailVertex = new ail::Vertex2D();
		ailVertex->type = ail::Vertex::Type::VERTEX2D;
	}
	int id;
	data >> id;
	ailVertex->vertexId = id;

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

	// Adding to vertices
	vertices.push_back(ailVertex);
}

int GraphManager::addVicinityEdge(stringstream &data, string name)
{
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [Adding %s edge]", name.c_str());
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
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [Unable to add edge %s]", name.c_str());
			delete e;
			return -1;
		}
	}
	return 0;
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
	return addVicinityEdge(data, "WiFi Fingerprint");
}

// Visual Place Recognition vicinity edge
int GraphManager::addEdgeVPRVicinity(stringstream &data) {
	return addVicinityEdge(data, "VPR Vicinity");
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

int GraphManager::findIndexInVertices(int id) {
	for (int i = 0; i < vertices.size(); i++) {
		if (vertices[i]->vertexId == id)
			return i;
	}
	return -1;
}
