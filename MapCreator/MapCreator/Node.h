#pragma once
#include <iostream>
#include <fstream>

class Node {
public:
	int pixelX, pixelY, id;
	double metricX, metricY;
	Node() {}
	Node(int _id, int _pixelX, int _pixelY, double _metricX, double _metricY) {
		id = _id;
		pixelX = _pixelX;
		pixelY = _pixelY;
		metricX = _metricX;
		metricY = _metricY;
	}

	friend std::ostream& operator<<(std::ostream &file, const Node &n);
};

std::ostream& operator<<(std::ostream & file, const Node & n)
{
	file << "NODE " << n.id << " " << n.pixelX << " " << n.pixelY << " " << n.metricX << " " << n.metricY << std::endl;
	return file;
}

bool operator<(const Node& lhs, const Node& rhs)
{
	return lhs.id < rhs.id;
}