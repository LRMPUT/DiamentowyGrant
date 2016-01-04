#pragma once

#include <opencv2/highgui.hpp>
#include <iostream>
#include <vector>
#include <set>
#include "Node.h"

void CallBackLeftClick(int event, int x, int y, int flags, void* userdata)
{
	if (event == cv::EVENT_LBUTTONDOWN)
	{
		std::cout << "LeftClick: X (" << x << ", " << y << ")" << std::endl;
		std::vector<std::pair<int, int>> * clickedPoints = static_cast<std::vector<std::pair<int, int>> *>(userdata);
		clickedPoints->push_back(std::make_pair(x, y));
	}
}

bool shouldBreak, rightClick = false;
int id = 0;
std::vector<std::pair<Node, Node>> links;
std::set<Node> nodes;
Node lastNode;

double distanceThreshold = 2;
double pixelsToMetres = 0;



void CallBackFunc(int event, int x, int y, int flags, void* userdata)
{
	if (event == cv::EVENT_LBUTTONDOWN)
	{
		std::cout << "Left button of the mouse is clicked - position (" << x << ", " << y << ")" << std::endl;

		Node tmp(id, x, y, x / pixelsToMetres, y / pixelsToMetres);
		Node current = findInNodes(nodes, tmp, distanceThreshold*pixelsToMetres);

		std::cout << "current node - position (" << x << ", " << y << ")" << std::endl;



		if (id != 0 && !rightClick) {
			links.push_back(std::make_pair(lastNode, current));
		}

		nodes.insert(current);
		lastNode = current;

		// Let's see if we need to add new node

		rightClick = false;
		if (current.id == id)
			id++;
	}
	else if (event == cv::EVENT_RBUTTONDOWN)
	{
		std::cout << "Right button of the mouse is clicked - position (" << x << ", " << y << ")" << std::endl;
		rightClick = true;
	}
	else if (event == cv::EVENT_MBUTTONDOWN) {
		std::cout << "Middle button of the mouse is clicked - position (" << x << ", " << y << ")" << std::endl;
		shouldBreak = true;
	}
}
