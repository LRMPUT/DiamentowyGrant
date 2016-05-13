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
#include "Callbacks.h"

using namespace cv;
using namespace std;

std::pair<int, int> fitToMyScreen(int cols, int rows, double &scale) {

	std::pair<int, int> sizeToScreen;

	// I assume 1920x1080
	if (1920.0 / cols < 1080.0 / rows)
	{
		sizeToScreen.first = 1850;
		sizeToScreen.second = (sizeToScreen.first * 1.0 / cols) * rows;
	}
	else {
		sizeToScreen.second = 1000;
		sizeToScreen.first = (sizeToScreen.second * 1.0 / rows) * cols;
	}


	scale = (cols * 1.0) / (sizeToScreen.first);

	return sizeToScreen;
}

std::tuple<double, double, double> pinpointOriginOfCoordinateSystem(Mat image) {
	cout << "Choose (0,0) point, then X direction (Y is clockwise, Z to the user)" << endl;

	Mat imgToShow = image.clone();

	imshow("Choosing (0,0) point", imgToShow);
	vector<pair<int, int>>  coordinateSystem;
	setMouseCallback("Choosing (0,0) point", CallBackLeftClick, &coordinateSystem);

	shouldBreak = false;
	while (coordinateSystem.size() < 1) {
		cvWaitKey(20);
	}
	double x0 = coordinateSystem[0].first;
	double y0 = coordinateSystem[0].second;
	double x1 = coordinateSystem[0].first+ imgToShow.cols / 8;
	double y1 = coordinateSystem[0].second;
	line(imgToShow, cvPoint(x0, y0), cvPoint(x1, y1), cvScalar(0, 255.0, 0, 0), 5);
	putText(imgToShow, "X", cvPoint(x1 + 10, y1 + 10), CV_FONT_NORMAL, 2, cvScalar(0, 255.0, 0, 0), 3);

	double xbis = x0;
	double ybis = y0 + imgToShow.cols / 8;

	line(imgToShow, cvPoint(coordinateSystem[0].first, coordinateSystem[0].second), cvPoint(xbis, ybis), cvScalar(0, 0, 255.0, 0), 5);
	putText(imgToShow, "Y", cvPoint(xbis, ybis), CV_FONT_NORMAL, 2, cvScalar(0, 0.0, 255.0, 0), 3);

	imshow("Choosing (0,0) point", imgToShow);
	cvWaitKey(0);
	cvDestroyWindow("Choosing (0,0) point");

	return std::make_tuple(x0, y0, 0);
}

void acquireScale(Mat image) {
	Mat imgToShow = image.clone();

	imshow("Getting the scale", imgToShow);

	vector<pair<int, int>> scaleCoordinateSystem;
	setMouseCallback("Getting the scale", CallBackLeftClick, &scaleCoordinateSystem);

	while (scaleCoordinateSystem.size() < 2) {
		cvWaitKey(20);
	}
	
	rectangle(imgToShow, cvPoint(scaleCoordinateSystem[0].first-5, scaleCoordinateSystem[0].second-5), cvPoint(scaleCoordinateSystem[0].first+5, scaleCoordinateSystem[0].second+5), cvScalar(255.0, 0.0, 0.0, 0), 8);
	rectangle(imgToShow, cvPoint(scaleCoordinateSystem[1].first - 5, scaleCoordinateSystem[1].second - 5), cvPoint(scaleCoordinateSystem[1].first + 5, scaleCoordinateSystem[1].second + 5), cvScalar(255.0, 0.0, 0.0, 0), 8);
	line(imgToShow, cvPoint(scaleCoordinateSystem[0].first, scaleCoordinateSystem[0].second), cvPoint(scaleCoordinateSystem[1].first, scaleCoordinateSystem[1].second), cvScalar(0, 0, 255.0, 0), 5);
	
	imshow("Getting the scale", imgToShow);
	cvWaitKey(0);
	cvDestroyWindow("Getting the scale");

	double metricDistance;
	cout << "Provide distance in metres" << endl;
	cin >> metricDistance;

	double pixelDistance = sqrt(pow(scaleCoordinateSystem[0].first - scaleCoordinateSystem[1].first, 2.0f) + pow(scaleCoordinateSystem[0].second - scaleCoordinateSystem[1].second, 2.0f));

	pixelsToMetres = pixelDistance / metricDistance;

	cout << "pixelDistance = " << pixelDistance << endl;
	cout << "metricDistance = " << metricDistance << endl;
	
}

void saveMap(std::tuple<double, double, double> &origin, double currentViewScale) {

	ofstream mapFile("buildingPlan.map");
	mapFile << "ORIGIN " << std::get<0>(origin)*currentViewScale << " " << std::get<1>(origin)*currentViewScale << " " << std::get<2>(origin)*currentViewScale << endl;

	mapFile << "SCALE " << pixelsToMetres*currentViewScale << endl;
	for (auto it = nodes.begin(); it != nodes.end(); ++it)
	{
		mapFile << "NODE " << it->id << " " << it->pixelX*currentViewScale << " " << it->pixelY*currentViewScale << " " << (it->pixelX- std::get<0>(origin))/ pixelsToMetres << " " << (it->pixelY - std::get<1>(origin))/ pixelsToMetres << std::endl;
	}
	for (auto &link : links) {
		if (link.first.id != link.second.id) {
			Node x = findById(nodes, link.first.id), y = findById(nodes, link.second.id);
			std::cout << "preEDGE: " << x.pixelX << " " << x.pixelY << " " << y.pixelX << " " << y.pixelY << std::endl;
			double dist = sqrt(pow(x.pixelX - y.pixelX, 2) + pow(x.pixelY - y.pixelY, 2)) / pixelsToMetres;


			mapFile << "EDGE " << link.first.id << " " << link.second.id << " " << dist << endl;
		}
			
	}

	mapFile.close();
}

void saveWiFiPositions(std::tuple<double, double, double> &origin) {
	ofstream positionsFile("positions.list");
	for (auto it = nodes.begin(); it != nodes.end(); ++it)
	{
		positionsFile << 10000 + it->id << " " << (it->pixelX - std::get<0>(origin)) / pixelsToMetres << " " << (it->pixelY - std::get<1>(origin)) / pixelsToMetres << " 0.0 0.0" << endl;
	}
	positionsFile.close();
}

int main(int argc, char** argv)
{
	cout << "What is the name of the background plan image?" << endl;
	//string fileName;
	//cin >> fileName;

	// Name of the file
	string fileName = "buildingPlan.jpg";

	// Reading image
	Mat image,dst;
	image = imread(fileName, IMREAD_COLOR); 
	//imwrite("buildingPlan.jpg", image);

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

	// Corridors
	shouldBreak = false;
	Mat imgToShow = dst.clone();
	namedWindow("Map possible corridors", WINDOW_AUTOSIZE); // Create a window for display.
	setMouseCallback("Map possible corridors", CallBackFunc, NULL);
	imshow("Map possible corridors", imgToShow); // Show our image inside it.
	while (!shouldBreak) {
		for (auto it = nodes.begin(); it != nodes.end(); ++it)
		{
			// Position
			circle(imgToShow, cvPoint(it->pixelX, it->pixelY), 6, cvScalar(255.0, 0, 0, 0), 5);
			// Area of force
			circle(imgToShow, cvPoint(it->pixelX, it->pixelY), distanceThreshold * pixelsToMetres, cvScalar(255.0, 0, 0, 0), 1);
		}
		for (int i = 0; i < links.size(); i++)
		{
			// Link
			line(imgToShow, cvPoint(links[i].first.pixelX, links[i].first.pixelY), cvPoint(links[i].second.pixelX, links[i].second.pixelY), cvScalar(255.0, 0, 0, 0), 5);
		}
		imshow("Map possible corridors", imgToShow); // Show our image inside it.
		cvWaitKey(10);
	}
	cvDestroyWindow("Map possible corridors");

	// Saving to map file
	saveMap(origin, viewScale);

	// WiFi/Image positions
	nodes.clear();
	id = 0;
	shouldBreak = false;
	namedWindow("WiFi / Image positions", WINDOW_AUTOSIZE); // Create a window for display.
	setMouseCallback("WiFi / Image positions", CallBackWiFi, NULL);
	imshow("WiFi / Image positions", dst); // Show our image inside it.
	std::cout << "We add each point 4 times!" << std::endl;
	while (!shouldBreak) {
		for (auto it = nodes.begin(); it != nodes.end(); ++it)
		{
			// Position
			circle(dst, cvPoint(it->pixelX, it->pixelY), 4, cvScalar(0, 0, 255.0, 0), 5);

			// 
			if (it->id % 20 == 0)
			{
				stringstream ss;
				ss << it->id/20;
				putText(dst, ss.str(), cvPoint(it->pixelX+5, it->pixelY-5), CV_FONT_NORMAL, 1, cvScalar(0, 0.0, 255.0, 0), 3);
			}
			
		}
		imshow("WiFi / Image positions", dst); // Show our image inside it.
		cvWaitKey(10);
	}
	cvDestroyWindow("WiFi / Image positions");

	// Save image with WiFi positions
	imwrite("buildingPlan_WIFI.jpg", dst);

	// Saving wifi positions
	saveWiFiPositions(origin);
	
	return 0;
}