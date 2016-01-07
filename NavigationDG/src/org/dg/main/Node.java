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
