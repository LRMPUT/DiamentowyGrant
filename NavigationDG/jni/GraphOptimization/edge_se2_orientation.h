// g2o - General Graph Optimization
// Copyright (C) 2011 R. Kuemmerle, G. Grisetti, W. Burgard
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// Modified by Michal Nowicki (michal.nowicki@put.poznan.pl) 2015
// Added EdgeSE2Orientation (Pure orientation measurement)

#ifndef G2O_EDGE_SE2_ORIENTATION_H
#define G2O_EDGE_SE2_ORIENTATION_H

#include "g2o/config.h"
#include "g2o/types/slam2d/vertex_se2.h"
#include "g2o/types/slam2d/vertex_point_xy.h"
#include "g2o/core/base_binary_edge.h"
#include "g2o/types/slam2d/g2o_types_slam2d_api.h"

#include "g2o/core/sparse_optimizer.h"
#include "g2o/core/block_solver.h"
#include "g2o/core/optimization_algorithm_gauss_newton.h"
#include "g2o/core/optimization_algorithm_levenberg.h"
#include "g2o/core/optimizable_graph.h"
#include "g2o/solvers/csparse/linear_solver_csparse.h"
#include "g2o/types/slam3d/vertex_se3.h"
//#include "g2o/types/slam3d/edge_se3.h"

#include "g2o/core/factory.h"
#include "g2o/stuff/command_args.h"

#include "g2o/types/slam2d/edge_se2.h"
#include "g2o/solvers/pcg/linear_solver_pcg.h"

#include "g2o/config.h"
#include "g2o/types/slam2d/vertex_se2.h"
#include "g2o/types/slam2d/vertex_point_xy.h"
#include "g2o/core/base_binary_edge.h"
#include "g2o/types/slam2d/g2o_types_slam2d_api.h"
#include <android/log.h>

#include <math.h>
using namespace std;
using namespace g2o;

namespace g2o {

class G2O_TYPES_SLAM2D_API EdgeSE2Orientation: public BaseBinaryEdge<1,
	double, VertexSE2, VertexSE2> {
public:
	EIGEN_MAKE_ALIGNED_OPERATOR_NEW
	EdgeSE2Orientation();
	void computeError() {
		const VertexSE2* v1 = static_cast<const VertexSE2*>(_vertices[0]);
		const VertexSE2* l2 = static_cast<const VertexSE2*>(_vertices[1]);
		Vector3d delta = (v1->estimate().inverse() * l2->estimate()).toVector();

		_error[0] = normalize_theta(_measurement - delta[2]);

		float angle = (v1->estimate()).toVector()[2], angle2 = (l2->estimate()).toVector()[2];
//		__android_log_print(ANDROID_LOG_VERBOSE, "XXX", "Angle %d %d: %f %f", v1->id(), l2->id(), angle, angle2);
//		__android_log_print(ANDROID_LOG_VERBOSE, "XXX", "Delta: %f Measurement: %f", delta[2], _measurement);
//		__android_log_print(ANDROID_LOG_VERBOSE, "XXX", "Error: %f !", _error[0]);

	}

	virtual bool setMeasurementData(double * d) {

		_measurement = d[0];
		return true;
	}

	virtual bool getMeasurementData(double * d) const {
		d[0] = _measurement;
		return true;
	}

	int measurementDimension() const {
		return 1;
	}

	virtual bool setMeasurementFromState() {
		const VertexSE2* v1 = static_cast<const VertexSE2*>(_vertices[0]);
		const VertexSE2* l2 = static_cast<const VertexSE2*>(_vertices[1]);
		Vector3d delta = (v1->estimate().inverse() * l2->estimate()).toVector();

		_measurement = delta[2];
		return true;
	}

	virtual bool read(std::istream& is);
	virtual bool write(std::ostream& os) const;

	virtual double initialEstimatePossible(
			const OptimizableGraph::VertexSet& from,
			OptimizableGraph::Vertex*) {
		return (from.count(_vertices[0]) == 1 ? 1.0 : -1.0);
	}
	virtual void initialEstimate(const OptimizableGraph::VertexSet& from,
			OptimizableGraph::Vertex* to);
};

class G2O_TYPES_SLAM2D_API EdgeSE2OrientationWriteGnuplotAction: public WriteGnuplotAction {
public:
	EdgeSE2OrientationWriteGnuplotAction();
	virtual HyperGraphElementAction* operator()(
			HyperGraph::HyperGraphElement* element,
			HyperGraphElementAction::Parameters* params_);
};

#ifdef G2O_HAVE_OPENGL
class G2O_TYPES_SLAM2D_API EdgeSE2OrientationDrawAction: public DrawAction {
public:
	EdgeSE2OrientationDrawAction();
	virtual HyperGraphElementAction* operator()(HyperGraph::HyperGraphElement* element,
			HyperGraphElementAction::Parameters* params_);
};
#endif


}

#endif
