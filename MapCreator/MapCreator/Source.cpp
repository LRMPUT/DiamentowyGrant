#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <iostream>
#include <vector>
#include <set>
#include <math.h>
#include <fstream>
#include <tuple>

#include "Node.h"

#define M_PI 3.14159265358979323846

using namespace cv;
using namespace std;



bool shouldBreak, rightClick = false;
int id = 0;
vector<pair<Node, Node>> links;
set<Node> nodes;
Node lastNode;

double distanceThreshold = 2;
double pixelsToMetres = 0;


Node findInNodes(set<Node> &nodes, Node currentNode) {
	for (auto it = nodes.begin(); it != nodes.end(); ++it)
	{
		double dist = (it->pixelX - currentNode.pixelX)*(it->pixelX - currentNode.pixelX) + (it->pixelY - currentNode.pixelY)*(it->pixelY - currentNode.pixelY);
		if (dist < distanceThreshold*distanceThreshold*pixelsToMetres*pixelsToMetres)
			return *it;
	}
	return currentNode;
}

void CallBackFunc(int event, int x, int y, int flags, void* userdata)
{
	if (event == EVENT_LBUTTONDOWN)
	{
		cout << "Left button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
		
		Node tmp(id, x, y, x/pixelsToMetres, y / pixelsToMetres);
		Node current = findInNodes(nodes, tmp);

		cout << "current node - position (" << x << ", " << y << ")" << endl;

		

		if (id != 0 && !rightClick) {
			links.push_back(make_pair(lastNode, current));	
		}
		
		nodes.insert(current);
		lastNode = current;

		// Let's see if we need to add new node
		
		rightClick = false;
		if ( current.id == id)
			id++;
	}
	else if (event == EVENT_RBUTTONDOWN)
	{
		cout << "Right button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
		rightClick = true;
	}
	else if (event == EVENT_MBUTTONDOWN) {
		cout << "Middle button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
		shouldBreak = true;
	}
}

vector<pair<int, int>> coordinateSystem;
void CallBackOrigin(int event, int x, int y, int flags, void* origin)
{
	if (event == EVENT_LBUTTONDOWN)
	{
		cout << "X (" << x << ", " << y << ")" << endl;
		coordinateSystem.push_back(make_pair(x, y));
	
		if ( coordinateSystem.size() >= 2)
			shouldBreak = true;
	}
}


std::pair<int, int> fitToMyScreen(int cols, int rows, double &scale) {

	std::pair<int, int> sizeToScreen;

	// I assume 1920x1080
	sizeToScreen.first = 960;
	sizeToScreen.second = (sizeToScreen.first * 1.0 / cols) * rows;

	scale = (cols * 1.0) / (sizeToScreen.first);

	return sizeToScreen;
}


std::tuple<double, double, double> pinpointOriginOfCoordinateSystem(Mat image) {
	cout << "Choose (0,0) point, then X direction (Y is clockwise, Z to the user)" << endl;

	Mat imgToShow = image.clone();

	imshow("Choosing (0,0) point", imgToShow);
	setMouseCallback("Choosing (0,0) point", CallBackOrigin, NULL);

	shouldBreak = false;
	while (!shouldBreak) {
		cvWaitKey(20);
	}
	double x0 = coordinateSystem[0].first;
	double y0 = coordinateSystem[0].second;
	double x1 = coordinateSystem[1].first;
	double y1 = coordinateSystem[1].second;
	line(imgToShow, cvPoint(x0, y0), cvPoint(x1, y1), cvScalar(0, 255.0, 0, 0), 5);

	double xprim = x1 - x0, yprim = y1 - y0;
	double dl = sqrt(xprim*xprim + yprim*yprim);
	double angle = atan2(yprim, xprim);

	double xbis = x0 + dl * cos(angle - M_PI / 2);
	double ybis = y0 + dl * sin(angle - M_PI / 2);

	line(imgToShow, cvPoint(coordinateSystem[0].first, coordinateSystem[0].second), cvPoint(xbis, ybis), cvScalar(0, 0, 255.0, 0), 5);
	imshow("Choosing (0,0) point", imgToShow);
	cvWaitKey(2000);
	cvDestroyWindow("Choosing (0,0) point");

	return std::make_tuple(x0, y0, angle * 180.0 / M_PI);
}

vector<pair<int, int>> scaleCoordinateSystem;
void CallBackScale(int event, int x, int y, int flags, void* userdata)
{
	if (event == EVENT_LBUTTONDOWN)
	{
		cout << "Scale (" << x << ", " << y << ")" << endl;
		scaleCoordinateSystem.push_back(make_pair(x, y));

		if (scaleCoordinateSystem.size() >= 2)
			shouldBreak = true;
	}
}

void acquireScale(Mat image) {
	Mat imgToShow = image.clone();

	imshow("Getting the scale", imgToShow);
	setMouseCallback("Getting the scale", CallBackScale, NULL);

	shouldBreak = false;
	while (!shouldBreak) {
		cvWaitKey(20);
	}
	cvDestroyWindow("Getting the scale");

	double metricDistance;
	cout << "Provide distance in metres" << endl;
	cin >> metricDistance;

	double pixelDistance = sqrt(pow(scaleCoordinateSystem[0].first - scaleCoordinateSystem[1].first, 2.0f) + pow(scaleCoordinateSystem[0].second - scaleCoordinateSystem[1].second, 2.0f));

	pixelsToMetres = pixelDistance / metricDistance;

	
	
}

void saveMap(std::tuple<double, double, double> &origin, double currentViewScale) {

	ofstream mapFile("cmbin.map");
	mapFile << "ORIGIN " << std::get<0>(origin)*currentViewScale << " " << std::get<1>(origin)*currentViewScale << " " << std::get<2>(origin)*currentViewScale << endl;

	mapFile << "SCALE " << pixelsToMetres*currentViewScale << endl;
	for (auto it = nodes.begin(); it != nodes.end(); ++it)
	{
		mapFile << "NODE " << it->id << " " << it->pixelX*currentViewScale << " " << it->pixelY*currentViewScale << " " << it->metricX*currentViewScale << " " << it->metricY*currentViewScale << std::endl;
	}
	for (auto &link : links) {
		mapFile << "EDGE " << link.first.id << " " << link.second.id << endl;
	}

	mapFile.close();
}

void saveWiFiPositions() {
	ofstream positionsFile("positions.list");
	for (auto it = nodes.begin(); it != nodes.end(); ++it)
	{
		positionsFile << 10000 + it->id << " " << it->metricX << " " << it->metricY << " 0.0 0.0" << endl;
	}
	positionsFile.close();
}


int main(int argc, char** argv)
{
	// Name of the file
	string fileName = "CMBiN.jpg";
	//string fileName = "mtp.jpg";

	// Reading image
	Mat image,dst;
	image = imread(fileName, IMREAD_COLOR); 

	// Rescaling to screen
	double viewScale = 0.0;
	std::pair<int, int> sizeToScreen = fitToMyScreen(image.cols, image.rows, viewScale);
	cv::resize(image, dst, Size(sizeToScreen.first, sizeToScreen.second));

	// If end if we could not find the file
	if (image.empty())
	{
		cout << "Could not open or find the image" << std::endl;
		return -1;
	}

	// Starting the origin of the coordinate system
	std::tuple<double, double, double> origin = pinpointOriginOfCoordinateSystem(dst);


	// Now write the distance in metres
	acquireScale(dst);

	shouldBreak = false;
	namedWindow("Display window", WINDOW_AUTOSIZE); // Create a window for display.
	setMouseCallback("Display window", CallBackFunc, NULL);
	imshow("Display window", dst); // Show our image inside it.
	while (!shouldBreak) {
		/*for (int i = 0; i < nodes.size(); i++)
			circle(dst, cvPoint(nodes[i].first, nodes[i].second), 6, cvScalar(255.0, 0, 0, 0), 5);*/
		
		for (auto it = nodes.begin(); it != nodes.end(); ++it)
		{
			// Position
			circle(dst, cvPoint(it->pixelX, it->pixelY), 6, cvScalar(255.0, 0, 0, 0), 5);
			// Area of force
			circle(dst, cvPoint(it->pixelX, it->pixelY), distanceThreshold * pixelsToMetres, cvScalar(255.0, 0, 0, 0), 1);
		}
		for (int i = 0; i < links.size(); i++)
		{
			// Link
			line(dst, cvPoint(links[i].first.pixelX, links[i].first.pixelY), cvPoint(links[i].second.pixelX, links[i].second.pixelY), cvScalar(255.0, 0, 0, 0), 5);
		}
		imshow("Display window", dst); // Show our image inside it.
		cvWaitKey(10);
	}
	

	// Saving to map file
	saveMap(origin, viewScale);

	// Saving wifi positions
	saveWiFiPositions();
	
	return 0;
}