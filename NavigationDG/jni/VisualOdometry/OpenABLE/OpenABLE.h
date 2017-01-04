/******************************************************************************/

/**
 * @file    OpenABLE.cpp
 * @brief   Core functions of the open place recognition method called ABLE 
            (Able for Binary-appearance Loop-closure Evaluation)
 * @author  Roberto Arroyo
 * @author  Michal Nowicki
 * @date    March 31, 2016
 */

/******************************************************************************/

#ifndef OPENABLE_H
#define OPENABLE_H

// System Includes
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <cstdlib>
#include <string>
#include <vector>
#include <math.h>
#include <iostream>
#include <fstream>
#include <sys/time.h>

// OpenCV Includes
#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>
//#include <opencv2/xfeatures2d.hpp>
//#include <opencv2/objdetect.hpp>

// Other Includes
#include "../DetectDescribe/LDB/ldb.h"
#include "dbscan.h"


#include "opencv2/flann/flann.hpp"

//#include "boost/filesystem.hpp"

#include <chrono>

// Namespaces
using namespace std;
using namespace cv;

/******************************************************************************/

// OpenABLE class
class OpenABLE{

private:

	// Configuration: general parameters

	// If printed, you can choose the similarity matrix color
	int s_color;
	// If 1, the program shows the processed images
	int show_images;

	// Number of bits used in the patch for global image description (if description_type = 0)
	int patch_size;

	// The number of images to be matched in sequence in each iteration
	int compareLength;

	// The coefficient used to scale thresholds
	double moreMatchCoeff;


	// Configuration: dataset parameters
	// Path where the images of the tested dataset are located (if frame_mode = 0)
	std::string trainSequencePath, testSequencePath;


	// Time attributes
	// Total time for describing all the images.
	double t_description;
	// Total time for matching all the images.
	double t_matching, t_matchingNew;
	// Average time for describing an image.
	double t_avg_description;
	// Average time for matching two images.
	double t_avg_matching, t_avg_matchingNew;


	// Method that performs ABLE matching of testDescriptors against trainingDescriptors as in OpenABLE implementation
	void openABLE_matching(int imageCounter,
						   const std::vector<Mat> & testDescriptors,
						   const std::vector<Mat> & trainingDescriptors,
						   Mat similarityMatrix);

	// Method that performs ABLE matching of testDescriptors against trainingDescriptors as in proposed FastABLE implementation
	// (previousDistances is a vector used to save those previously computed values that can be used)
	void fastABLE_matching(int imageCounter,
						   const std::vector<Mat> & testDescriptors,
						   const std::vector<Mat> & trainingDescriptors,
						   std::vector<int>& previousDistances,
						   Mat similarityMatrix);

	// TODO: LSH version of OpenABLE
	void OpenABLE_LSH_matching();

	// Returns the vector of directories found inside path
//	std::vector<boost::filesystem::path> getSubDirectories(std::string path);

	// Reads and computes global descriptors for sequences inside all directories inside dirs
//	int readTrainSequences(const std::vector<boost::filesystem::path>& dirs,
//						   std::vector<std::vector<Mat> >& trainingSequences,
//						   std::vector<std::vector<Mat> >& trainingDescriptors);

	// Method used to estimate threshold for $p$ parts of training sequences (it doesn't use test sequence)
	std::vector<double> automaticThresholdEstimation(const std::vector<std::vector<Mat> >& trainingDescriptors);

	// Tests results against threshold, returns and saves those raw results to file
	std::vector<std::vector<std::pair<int, int> > > computeAndSaveRawVisualPlaceRecognitions(
			const std::vector<std::vector<Mat> >& trainingSequences,
			const std::vector<Mat>& fastABLE_similarityMatrices,
			const std::vector<double>& localThresholds);

	// Clusters raw results to ease marking as correct/incorrect and saves clustered data
	void markAndSaveClusteredVisualPlaceRecognitions(
			const std::vector<std::vector<Mat> >& trainingSequences,
			const std::vector<std::vector<std::pair<int, int> > >& results,
			const std::vector<Mat>& testSequence);

public:

	// Constructor
	OpenABLE(const char* config_name);

	// Methods
	Mat global_description(Mat image);
	int hamming_matching(Mat desc1, Mat desc2);
	void similarity_matrix_normalization(Mat matrix);
	void similarity_matrix_to_txt(Mat matrix, std::string fileName);
	void similarity_matrix_to_png(Mat matrix, int image_sequences, std::string fileName);
	void compute_OpenABLE();
	void show_times();
	void getMatchingTimes(float& tOpenable, float& tFastable, int& numImgs);

	// Performs complete ABLE matching of training and test sequences using OpenABLE and FastABLE implementations
	void computeVisualPlaceRecognition(
			const std::vector<std::vector<Mat> >& trainingDescriptors,
			const std::vector<Mat>& testDescriptors,
			std::vector<Mat>& openABLE_similarityMatrices,
			std::vector<Mat>& fastABLE_similarityMatrices,
			bool saveMatrices = true);

	void computeForVideo(std::string videoPath[2], int startingFrameId, int numberOfFrames);

};


#endif // OPENABLE_H

/******************************************************************************/
