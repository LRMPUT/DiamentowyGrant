package org.dg.openAIL;

import java.util.ArrayList;
import java.util.List;

import org.dg.main.Edge;
import org.dg.main.Node;

import android.graphics.Bitmap;

public class BuildingPlan {
	public List<Node> nodeLocations = new ArrayList<Node>();
	public List<Edge> edgeLocations = new ArrayList<Edge>();
	public Bitmap mBackgroundImage = null;
	
	public double oldMapPixels2Metres = 1;
	public double backgroundResizedPx2OriginalPx = 1;
	public double originX=0, originY=0;
}
