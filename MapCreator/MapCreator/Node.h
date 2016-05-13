#pragma once
#include <iostream>
#include <fstream>
#include <set>

class Node {
public:
	int pixelX, pixelY, id;
	Node() {}
	Node(int _id, int _pixelX, int _pixelY) {
		id = _id;
		pixelX = _pixelX;
		pixelY = _pixelY;
	}

	friend std::ostream& operator<<(std::ostream &file, const Node &n);
};


Node findInNodes(std::set<Node> &nodes, Node currentNode, double distanceThreshold) {
	for (auto it = nodes.begin(); it != nodes.end(); ++it)
	{
		double dist = (it->pixelX - currentNode.pixelX)*(it->pixelX - currentNode.pixelX) + (it->pixelY - currentNode.pixelY)*(it->pixelY - currentNode.pixelY);
		if (dist < distanceThreshold*distanceThreshold)
			return *it;
	}
	return currentNode;
}

Node findById(std::set<Node> &nodes, int id) {
	for (auto &n : nodes) {
		if (n.id == id)
			return n;
	}
	return Node(-1,0,0);
}

std::ostream& operator<<(std::ostream & file, const Node & n)
{
	file << "NODE " << n.id << " " << n.pixelX << " " << n.pixelY << std::endl;
	return file;
}

bool operator<(const Node& lhs, const Node& rhs)
{
	return lhs.id < rhs.id;
}