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
	sizeToScreen.first = 960;
	sizeToScreen.second = (sizeToScreen.first * 1.0 / cols) * rows;

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
	double ybis = y0 - imgToShow.cols / 8;

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