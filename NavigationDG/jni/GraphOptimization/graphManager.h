#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <pthread.h>

// Core
#include "g2o/core/sparse_optimizer.h"
#include "g2o/core/block_solver.h"
#include "g2o/core/optimization_algorithm_gauss_newton.h"
#include "g2o/core/optimization_algorithm_levenberg.h"
#include "g2o/core/optimizable_graph.h"
#include "g2o/core/base_binary_edge.h"

// Types
#include "g2o/types/slam3d/vertex_se3.h"
#include "g2o/types/slam2d/g2o_types_slam2d_api.h"
#include "g2o/types/slam2d/vertex_se2.h"
#include "g2o/types/slam2d/vertex_point_xy.h"

// Solver
#include "g2o/solvers/pcg/linear_solver_pcg.h"

// G2o rest
#include "g2o/stuff/command_args.h"
#include "g2o/config.h"

// Custom edges
#include "edge_se2_pointXY_distance.h"
#include "edge_se2_distanceOrientation.h"
#include "edge_se2_placeVicinity.h"


using namespace std;
using namespace g2o;

G2O_USE_TYPE_GROUP(slam2d);

#define DEBUG_TAG "NDK_DG_GraphManager"

namespace ail {
class Vertex {
public:
	int vertexId;

	enum Type {
		/// Vertex 2D -- x, y
		VERTEX2D,
		/// Vertex SE(2) -- x, y, theta
		VERTEXSE2
	};

	Type type;
};

class Vertex2D: public Vertex {
public:
	Vertex2D() {
		pos = Eigen::Vector2d::Zero();
	}
	Eigen::Vector2d pos;
};

class VertexSE2: public Vertex {
public:
	VertexSE2() {
		pos = Eigen::Vector2d::Zero();
		orient = 0.0;
	}
	Eigen::Vector2d pos;
	double orient;
};

}



class GraphManager {
private:
	SparseOptimizer optimizer;

	// User only for initial estimates
	double prevUserPositionX, prevUserPositionY, prevUserPositionTheta;

public:
	GraphManager();

	// Perform optimization for given number of iterations
	int optimize(int iterationCount);

	// Save optimized graph to file
	int saveOptimizationResult(ofstream &ofs);

	// Add information in string to graph
	void addToGraph(string dataToProcess);

	// Get information about position of vertex with given id
	std::vector<double> getVertexPosition(int id);

	// Get information about position of all vertices
	std::vector<double> getPositionOfAllVertices();

private:
	// Adding vertices:
	// 0 - SE2
	// 1 - XY
	int addVertex(stringstream &data, int type);

	// add Vicinity Edge
	int addVicinityEdge(stringstream &data, string name);

	// WiFi Edge
	int addEdgeWiFi(stringstream &data);

	// Stepometer
	int addEdgeStepometer(stringstream &data);

	// Typical odometry SE2 edge
	int addEdgeSE2(stringstream &data);

	// findIndex of vertex with given id
	int findIndexInVertices(int id);

	// Current estimates
	std::vector<ail::Vertex*> vertices;

	// Lock to graph
	pthread_mutex_t graphMtx;

	// Lock to current estimate - vertices
	pthread_mutex_t verticesMtx;
};
