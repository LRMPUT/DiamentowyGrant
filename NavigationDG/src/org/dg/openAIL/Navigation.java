package org.dg.openAIL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.dg.main.Edge;
import org.dg.main.Node;
import org.dg.openAIL.ConfigurationReader.Parameters.InertialSensors.Record;

import android.util.Log;
import android.util.Pair;

public class Navigation {
	private static final String moduleLogName = "Navigation";
	static Double largeValue = 99999999.0;
	
	private List<Node> nodes = null;
	private List<List<Edge>> edges = null;
	
	public Navigation(BuildingPlan buildingPlan) {
		Log.d(moduleLogName, "Navigation()");
		nodes = buildingPlan.nodeLocations;
		edges = repackEdges(buildingPlan.edgeLocations, nodes.size());
		
		Log.d(moduleLogName, "nodes = " + nodes.size());
		Log.d(moduleLogName, "edges = " + edges.size());
	}
	
	public List<Pair<Double, Double>> startNavigation(double startX, double startY, double endX, double endY) {
		Log.d(moduleLogName, "startNavigation() - startX=" + startX
				+ " startY=" + startY + " endX=" + endX + " endY=" + endY);

		// find closest node to start
		Node start = findClosestNode(startX, startY);
		
		// find closest node to end
		Node end = findClosestNode(endX, endY);
		
		Log.d(moduleLogName, "start.id = " + start.getId());
		Log.d(moduleLogName, "end.id = " + end.getId());
		
		// Initial cost
		List<Double> fValue = new ArrayList<Double>(), costFromStart = new ArrayList<Double>();
		List<Integer> stillOpen = new ArrayList<Integer>();
		List<Integer> closed = new ArrayList<Integer>();
		for (int i=0;i<nodes.size();i++) {
			fValue.add(largeValue);
			costFromStart.add(largeValue);
		}
		fValue.set(start.getId(), computeHeuristic(start, end));
		costFromStart.set(start.getId(), Double.valueOf(0.0));
		stillOpen.add(Integer.valueOf(0));
		
		// Search for path to goal
		Boolean goalReached = false;
		while (!goalReached) {
			// Find next node
			Integer id = findNodeWithLowestValue(fValue, stillOpen);			
			stillOpen.remove(Integer.valueOf(id));
			closed.add(Integer.valueOf(id));
			
			for (Integer x : stillOpen)
				Log.d(moduleLogName, "Stillopen : " + x);
			
			Log.d(moduleLogName, "Processing node: " + id);

			
			// for all edges from this node
			for (Edge e : edges.get(id)) {
				
				if ( e.to.getId() == id ) {
					Log.d(moduleLogName, "Should not have happened!");
				}
				
				// Cost from start + edge
				Double value = costFromStart.get(e.from.getId()) + e.distance;
				
				// If it is better than previous
				if ( value < costFromStart.get(e.to.getId()) ) {
					Log.d(moduleLogName, "Previous for: " + e.to.getId() + " is " + e.from.getId());
					costFromStart.set(e.to.getId(), value );
					
					// Correct prediction to end
					fValue.set(e.to.getId(), value + computeHeuristic(e.to, end)); 
					e.to.previousNodeForNav = e.from;
				}
				
				
				// if it is a goal
				if ( e.to.getId() == end.getId() ) {
					e.to.previousNodeForNav = e.from;
					goalReached = true;
					break;
				}
				// if it is a new node
				Integer secondNodeId = Integer.valueOf(e.to.getId());
				if ( stillOpen.contains(secondNodeId) == false && closed.contains(secondNodeId) == false) {
					Log.d(moduleLogName, "Adding to stillOpen: " + secondNodeId);
					stillOpen.add(secondNodeId);
				}
				// Old 
				else {
					// TODO
					// This should not have happened
				}
				
			}
			
			//TODO REMOVE LATER
			try {
				Thread.sleep(100,0);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
		for (Node n : nodes) {
			if ( n.previousNodeForNav != null)
				Log.d(moduleLogName, "Previous test: For " + n.getId() + " it is " + n.previousNodeForNav.getId());
			else
				Log.d(moduleLogName, "Previous test: For " + n.getId() + " it is null");
		}
		
//		for (List<Edge> le : edges) {
//			for (Edge e : le) {
//				if ( e.from.previousNodeForNav != null)
//					Log.d(moduleLogName, "Edge test: For " + e.from.getId() + " it is " + e.from.previousNodeForNav.getId());
//				else
//					Log.d(moduleLogName, "Edge test: For " + e.from.getId() + " it is null");
//				
//				if ( e.to.previousNodeForNav != null)
//					Log.d(moduleLogName, "Edge test: For " + e.to.getId() + " it is " + e.to.previousNodeForNav.getId());
//				else
//					Log.d(moduleLogName, "Edge test: For " + e.to.getId() + " it is null");
//			}
//		}
		
		
		Log.d(moduleLogName, "Now end. Still need to reconstruct path");
		
		// Reconstruct path
		List<Pair<Double, Double>> pathFromGoal = new LinkedList<Pair<Double, Double>>();
		
		Node reconstruct = end;
		while (reconstruct.getId() != start.getId()) {
			pathFromGoal.add(new Pair<Double, Double>(reconstruct.getX(), reconstruct.getY()));
			
			Log.d(moduleLogName, "Reconstruct : id = " + reconstruct.getId());
			reconstruct = reconstruct.previousNodeForNav;
			
			if ( reconstruct == null )
				Log.d(moduleLogName, "Reconstruct : NULL!");
		}
		pathFromGoal.add(new Pair<Double, Double>(reconstruct.getX(), reconstruct.getY()));
		Log.d(moduleLogName, "Reconstruct finish: id = " + reconstruct.getId());
		
		for (int i=0;i<costFromStart.size();i++)
			Log.d(moduleLogName, "costFromStart: costFromStart["+i+"] = " +  costFromStart.get(i));
		
		Collections.reverse(pathFromGoal);
		return pathFromGoal;
	}
	
	private List<List<Edge>> repackEdges(List<Edge> edges, int size) {
		List<List<Edge>> edgeList = new ArrayList<List<Edge>>();
		for (int i=0;i<size;i++) {
			edgeList.add(new ArrayList<Edge>());
		}
		
		for (Edge e : edges) {
			List<Edge> a = edgeList.get(e.from.getId());
			a.add(e);
			List<Edge> b = edgeList.get(e.to.getId());
			Edge e2 = new Edge(e); 
			e2.revert();
			b.add(e2);
		}
		
		return edgeList;
	}
	
	
	private Node findClosestNode(double x, double y) {
		Node closest = nodes.get(0);
		double closestDist = closest.euclideanDistance(x, y);
		
		for (Node n : nodes) {
			double dist = n.euclideanDistance(x, y);
			if ( dist < closestDist) {
				closest = n;
				closestDist = dist;
			}
		}
		
		return closest;
	}
	
	private Integer findNodeWithLowestValue(final List<Double> fValue, final List<Integer> stillOpen) {
		Double fMin = fValue.get(stillOpen.get(0));
		int fMinPos = stillOpen.get(0);
		
		
		for (Integer i : stillOpen) {
			Double testVal =  fValue.get(stillOpen.get(0));
			if ( fMin > testVal) {
				fMin = testVal;
				fMinPos = i;
			}
		}
		
		return fMinPos;
	}
	
	private Double computeHeuristic(Node a, Node b) {
		return a.euclideanDistance(b);
	}
	
	
}
