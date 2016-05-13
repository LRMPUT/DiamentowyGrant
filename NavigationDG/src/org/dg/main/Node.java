// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
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

package org.dg.main;

import android.util.Log;

/**
 * Class Node used to store information about the node of the prior map:
 * - contains the id of the node
 * - pixel position in the originally loaded image
 * - metric position w.r.t the origin
 * 
 * @author Michal Nowicki
 */
public class Node {
	
	private int id;
	private double px, py;
	private double x, y;
	
	public Node previousNodeForNav;
	
	public Node (int id, double px, double py) {
		init(id, px, py, 0.0, 0.0);
	}
	
	public Node(int id, double px, double py, double x, double y) {
		init(id, px, py, x, y);
	}
	
	public Node(Node n) {
		init(n.id, n.px, n.py, n.x, n.y);
	}
	
	private void init(int _id, double _px, double _py, double _x, double _y) {
		this.id = _id;
		this.px = _px;
		this.py = _py;
		this.x = _x;
		this.y = _y;
	}
	
	public int getId() {
		return id;
	}
	
	public double getPx() {
		return px;
	}
	
	public double getPy() {
		return py;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double euclideanDistance(Node b) {
		return Math.sqrt( Math.pow(x - b.getX(),2) + Math.pow(y - b.getY(),2));
	}
	
	public double euclideanDistance(double posX, double posY) {
		return euclideanDistance(new Node(0, 0, 0, posX, posY));
	}

}
