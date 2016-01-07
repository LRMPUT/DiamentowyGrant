package org.dg.main;

public class Edge {
	public Edge(Node _from, Node _to, double _distance) {
		from = _from;
		to = _to;
		distance = _distance;
	}
	
	public Edge(Edge e) {
		from = e.from;
		to = e.to;
		distance = e.distance;
	}
	
	public void revert() {
		Node tmp = from;
		from = to;
		to = tmp;
		
		tmp = from.previousNodeForNav;
		from.previousNodeForNav = to.previousNodeForNav;
		to.previousNodeForNav = tmp;
	}
	
	public Node from, to;
	public double distance;
}
