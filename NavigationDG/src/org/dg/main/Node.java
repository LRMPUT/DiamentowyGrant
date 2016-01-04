package org.dg.main;

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
	
	
	public Node (int id, double px, double py) {
		init(id, px, py, 0.0, 0.0);
	}
	
	public Node(int id, double px, double py, double x, double y) {
		init(id, px, py, x, y);
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

}
