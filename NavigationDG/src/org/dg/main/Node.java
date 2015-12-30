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
	
	private void init(int id, double px, double py, double x, double y) {
		this.id = id;
		this.px = x;
		this.py = y;
		this.x = x;
		this.y = y;
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
