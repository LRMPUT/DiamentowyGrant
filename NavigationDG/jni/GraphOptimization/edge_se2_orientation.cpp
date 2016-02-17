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

#include "edge_se2_orientation.h"

#ifdef G2O_HAVE_OPENGL
#include "g2o/stuff/opengl_wrapper.h"
#include "g2o/stuff/opengl_primitives.h"
#endif

namespace g2o {

  EdgeSE2Orientation::EdgeSE2Orientation()
  {
  }

void EdgeSE2Orientation::initialEstimate(
		const OptimizableGraph::VertexSet& from,
		OptimizableGraph::Vertex* /*to*/) {
	VertexSE2* fromEdge = static_cast<VertexSE2*>(_vertices[0]);
	VertexSE2* toEdge = static_cast<VertexSE2*>(_vertices[1]);


	double theta = _measurement;
	SE2 tmpMeasurement(0,0,theta);
	SE2 inverseTmpMeasurement  = tmpMeasurement.inverse();
	if (from.count(fromEdge) > 0)
		toEdge->setEstimate(fromEdge->estimate() * tmpMeasurement);
	else
		fromEdge->setEstimate(toEdge->estimate() * inverseTmpMeasurement);
  }

bool EdgeSE2Orientation::read(std::istream& is) {
	double p;
	is >> p;
	setMeasurement(p);
	is >> information()(0,0);

	return true;
}

bool EdgeSE2Orientation::write(std::ostream& os) const {
	os << _measurement << information()(0, 0);
	return os.good();
}


  EdgeSE2OrientationWriteGnuplotAction::EdgeSE2OrientationWriteGnuplotAction(): WriteGnuplotAction(typeid(EdgeSE2Orientation).name()){}

  HyperGraphElementAction* EdgeSE2OrientationWriteGnuplotAction::operator()(HyperGraph::HyperGraphElement* element,
                         HyperGraphElementAction::Parameters* params_){
    if (typeid(*element).name()!=_typeName)
      return 0;
    WriteGnuplotAction::Parameters* params=static_cast<WriteGnuplotAction::Parameters*>(params_);
    if (!params->os){
      std::cerr << __PRETTY_FUNCTION__ << ": warning, on valid os specified" << std::endl;
      return 0;
    }

    EdgeSE2Orientation* e =  static_cast<EdgeSE2Orientation*>(element);
    VertexSE2* fromEdge = static_cast<VertexSE2*>(e->vertex(0));
    VertexPointXY* toEdge   = static_cast<VertexPointXY*>(e->vertex(1));
    *(params->os) << fromEdge->estimate().translation().x() << " " << fromEdge->estimate().translation().y()
      << " " << fromEdge->estimate().rotation().angle() << std::endl;
    *(params->os) << toEdge->estimate().x() << " " << toEdge->estimate().y() << std::endl;
    *(params->os) << std::endl;
    return this;
  }

#ifdef G2O_HAVE_OPENGL
  EdgeSE2OrientationDrawAction::EdgeSE2OrientationDrawAction(): DrawAction(typeid(EdgeSE2Orientation).name()){}

  HyperGraphElementAction* EdgeSE2OrientationDrawAction::operator()(HyperGraph::HyperGraphElement* element,  HyperGraphElementAction::Parameters* params_){
    if (typeid(*element).name()!=_typeName)
      return 0;

    refreshPropertyPtrs(params_);
    if (! _previousParams)
      return this;

    if (_show && !_show->value())
      return this;

    EdgeSE2Orientation* e =  static_cast<EdgeSE2Orientation*>(element);
    VertexSE2* from = static_cast<VertexSE2*>(e->vertex(0));
    VertexPointXY* to   = static_cast<VertexPointXY*>(e->vertex(1));
    if (! from)
      return this;
    double guessRange=5;
    double theta = e->measurement();
    Vector2D p(cos(theta)*guessRange, sin(theta)*guessRange);
    glPushAttrib(GL_ENABLE_BIT|GL_LIGHTING|GL_COLOR);
    glDisable(GL_LIGHTING);
    if (!to){
      p=from->estimate()*p;
      glColor3f(LANDMARK_EDGE_GHOST_COLOR);
      glPushAttrib(GL_POINT_SIZE);
      glPointSize(3);
      glBegin(GL_POINTS);
      glVertex3f((float)p.x(),(float)p.y(),0.f);
      glEnd();
      glPopAttrib();
    } else {
      p=to->estimate();
      glColor3f(LANDMARK_EDGE_COLOR);
    }
    glBegin(GL_LINES);
    glVertex3f((float)from->estimate().translation().x(),(float)from->estimate().translation().y(),0.f);
    glVertex3f((float)p.x(),(float)p.y(),0.f);
    glEnd();
    glPopAttrib();
    return this;
  }
#endif

} // end namespace
