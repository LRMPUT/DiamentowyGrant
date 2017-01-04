/******************************************************************************/

/**
 * @file    OpenABLE.cpp
 * @brief   Core functions of the open place recognition method called ABLE
 (Able for Binary-appearance Loop-closure Evaluation)
 * @author  Roberto Arroyo
 * @author  Michal Nowicki
 * @date    August 21, 2016
 */

/******************************************************************************/

#include "OpenABLE.h"

#include <limits>       // std::numeric_limits
#include <chrono>
#include <android/log.h>

// Namespaces
using namespace std;
using namespace cv;
//using namespace cv::xfeatures2d;

/******************************************************************************/

/**
 * @brief This constructor loads the different configuration parameters
 * @param config_name Name given to the configuration file
 */
OpenABLE::OpenABLE(const char* config_name) {

	//The configuration file of OpenABLE is opened
	FileStorage fs;
	fs.open(config_name, FileStorage::READ);
	if (!fs.isOpened()) {

		cout << "Couldn't open the configuration file." << endl;

	}

	// Obtaining the configuration parameters
	// General parameters
	fs["s_color"] >> s_color;
	fs["show_images"] >> show_images;

	// Description and matching parameters
	fs["patch_size"] >> patch_size;
	fs["compareLength"] >> compareLength;
	fs["moreMatchCoeff"] >> moreMatchCoeff;

	// Dataset parameters;
	fs["trainSequencePath"] >> trainSequencePath;
	fs["testSequencePath"] >> testSequencePath;

	fs.release();

	t_matching = 0;
	t_matchingNew = 0;

}

/******************************************************************************/

/**
 * @brief This function computes the global descriptor for an image
 * @param image Computed image
 * @return Descriptor obtained
 */
Mat OpenABLE::global_description(Mat image) {

	Mat descriptor;

//	__android_log_print(ANDROID_LOG_DEBUG, "FastABLE", "resizing");
	// Resize the image
	Mat image_resized;
	resize(image, image_resized, Size(patch_size, patch_size), 0, 0,
			INTER_LINEAR);

//	__android_log_print(ANDROID_LOG_DEBUG, "FastABLE", "keypoint selection");
	// Select the central keypoint
	vector<KeyPoint> kpts;
	KeyPoint kpt;
	kpt.pt.x = patch_size / 2 + 1;
	kpt.pt.y = patch_size / 2 + 1;
	kpt.size = 1.0;
	kpt.angle = 0.0;
	kpts.push_back(kpt);

//	__android_log_print(ANDROID_LOG_DEBUG, "FastABLE", "ldb computation, resized.size() = (%d, %d)", image_resized.cols, image_resized.rows);
	LDB ldb;
	ldb.compute(image_resized, kpts, descriptor);

//	__android_log_print(ANDROID_LOG_DEBUG, "FastABLE", "returning");
	return descriptor;

}

/******************************************************************************/


/**
 * @brief This method computes the Hamming distance between two binary descriptors
 * @param desc1 First descriptor
 * @param desc2 Second descriptor
 * @return Hamming distance between the two descriptors
 */
int OpenABLE::hamming_matching(Mat desc1, Mat desc2) {

	int distance = 0;

	if (desc1.rows != desc2.rows || desc1.cols != desc2.cols || desc1.rows != 1
			|| desc2.rows != 1) {

		cout << "The dimension of the descriptors is different." << desc1.rows << " " << desc1.cols << " " << desc2.rows << " " << desc2.cols << endl;
		return -1;

	}

	for (int i = 0; i < desc1.cols; i++) {

	// MNowicki: According to my knowledge, the version from OpenABLE (below) is incorrect
	//
	//	distance += (*(desc1.ptr<unsigned char>(0) + i))
	//				^ (*(desc2.ptr<unsigned char>(0) + i));
	//
		distance += __builtin_popcount((*(desc1.ptr<unsigned char>(0) + i))
						^ (*(desc2.ptr<unsigned char>(0) + i)));
	}

	// MNowicki: Alternatively, it is possible to compute distance with OpenCV
	//double dist = cv::norm( desc1, desc2, NORM_HAMMING);

	return distance;

}

/**
 * @brief This method normalizes the similarity matrix
 */
void OpenABLE::similarity_matrix_normalization(Mat matrix) {

	int nRows = matrix.rows;
	int nCols = matrix.cols;
	double min, max;

	minMaxLoc(matrix, &min, &max);


	for (int i = 0; i < nRows; i++) {

		for (int j = 0; j < nCols; j++) {

			matrix.at<float>(i, j) = matrix.at<float>(i,
					j) / max;

		}

	}

}


/******************************************************************************/

/**
 * @brief This method saves the similarity matrix into a .png file
 */
void OpenABLE::similarity_matrix_to_png(Mat matrix, int image_sequences, std::string fileName) {

	int nRows = matrix.rows;
	int nCols = matrix.cols;
	Mat similarity_color(nRows, nCols, CV_32FC3);
	Mat similarity_color_8U(nRows, nCols, CV_8UC3);
	float similarity;

	for (int i = 0; i < nRows; i++) {

		for (int j = 0; j < nCols; j++) {

			similarity = matrix.at<float>(i, j);

			if (s_color == 0) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			} else if (s_color == 1) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 0;
				similarity_color.ptr<Point3_<float> >(i, j)->y = 0;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			} else if (s_color == 2) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 0;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = 0;

			} else if (s_color == 3) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = 0;
				similarity_color.ptr<Point3_<float> >(i, j)->z = 0;

			} else if (s_color == 4) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 1 - similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = 1 - similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			} else if (s_color == 5) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 1 - similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = 1 - similarity;

			} else if (s_color == 6) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = 1 - similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = 1 - similarity;

			} else if (s_color == 7) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 0;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = 1;

			} else if (s_color == 8) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 1;
				similarity_color.ptr<Point3_<float> >(i, j)->y = 1 - similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			} else if (s_color == 9) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = similarity
						* 0.4980;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity
						* 0.7804;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			} else if (s_color == 10) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 1 - similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = 1;

			} else if (s_color == 11) {

				similarity_color.ptr<Point3_<float> >(i, j)->x = 0;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity
						* 0.5 + 0.5;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			} else {

				similarity_color.ptr<Point3_<float> >(i, j)->x = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->y = similarity;
				similarity_color.ptr<Point3_<float> >(i, j)->z = similarity;

			}

		}

	}

	similarity_color.convertTo(similarity_color_8U, CV_8UC1, 255, 0);
	imwrite(fileName.c_str(), similarity_color_8U);

}

/*
 *
 * Reads and returns all images in the directory specified by the pathName
 *
 */
//std::vector<Mat> readImageSequence(std::string pathName) {
//	std::vector<Mat> images;
//
//	// Reading the names of all files
//	boost::filesystem::path path(pathName);
//	std::vector<boost::filesystem::path> files;
//	for (boost::filesystem::recursive_directory_iterator i(pathName), eod;
//			i != eod; ++i) {
//
//		const boost::filesystem::path cp = (*i);
//		if (is_regular_file(cp)) {
//			files.push_back(cp);
//		}
//
//	}
//
//	// Sorting those names
//	std::sort(files.begin(), files.end());
//
//	// Reading images with specified names
//	for (auto &f : files) {
//		images.push_back(imread(f.string(), 1));
//	}
//
//	return images;
//}


void OpenABLE::openABLE_matching(int imageCounter,
		const std::vector<Mat> & testDescriptors,
		const std::vector<Mat> & trainingDescriptors,
		Mat similarityMatrix) {

	std::vector<float> similarityMatrixLine;
	int trainingSize = trainingDescriptors.size();

	// If there is enough images inside the moving window
	if (imageCounter >= compareLength - 1) {

		// We move the training sequence
		for (int trainingShift = trainingSize - 1;
				trainingShift >= compareLength - 1; trainingShift--) {

			float distance = 0.0;
			for (int k = 0; k < compareLength; k++) {
				distance = distance
						+ hamming_matching(testDescriptors[imageCounter - k],
								trainingDescriptors[trainingShift - k]);
			}

			if (!similarityMatrix.empty())
				similarityMatrix.at<float>((imageCounter - compareLength + 1), (trainingShift - compareLength + 1)) = distance;
		}
	}
}

void OpenABLE::fastABLE_matching(int imageCounter,
		const std::vector<Mat> & testDescriptors,
		const std::vector<Mat> & trainigDescriptors,
		std::vector<int>& previousDistances,
		Mat similarityMatrix) {

	std::vector<float> similarityMatrixLine;
	int trainingSize = trainigDescriptors.size();

	if (imageCounter >= compareLength - 1) {

		for (int trainingShift = trainingSize - 1, iter = 0;
				trainingShift >= compareLength - 1;
				trainingShift--, iter++) {

			float distance = 0.0;

			// If it is not the first matching (we create results in else) and to make sure we check that those previousDistances exist
			if (imageCounter != compareLength - 1
					&& trainingShift != compareLength - 1
					&& previousDistances.empty() == false) {

				// We compute current result as a result from previous iteration adding last and subtracting first
				distance = previousDistances[iter + 1]
						+ hamming_matching(testDescriptors[imageCounter],
								trainigDescriptors[trainingShift])
						- hamming_matching(
								testDescriptors[imageCounter - compareLength + 1],
								trainigDescriptors[trainingShift - compareLength + 1]);

				// Let's save those precomputed distances for next iteration
				previousDistances[iter] = distance;
			}
			// We need to compute it normally for first iteration and for one matching in each following iteration
			else
			{
				// for each element in the sequence to be matched
				for (int k = 0; k < compareLength; k++) {
					// Distance between:
					// 		testDescriptors shifted by (imageCounter-k)
					// 		trainDescriptors shifted by (trainingShift-k)
					distance = distance
							+ hamming_matching(testDescriptors[imageCounter - k],
									trainigDescriptors[trainingShift - k]);
				}
				// if we just started, we initialize the previouseDistances to later use modified recurrence
				if (imageCounter == compareLength - 1)
					previousDistances.push_back(distance);

				// the last one in series, so we update the previouseDistances at [0]
				else if (trainingShift == compareLength - 1)
					previousDistances[iter] = distance;
			}


			if (!similarityMatrix.empty())
				similarityMatrix.at<float>((imageCounter - compareLength + 1),(trainingShift - compareLength + 1) ) =
					distance;
		}
	}
}

void OpenABLE::OpenABLE_LSH_matching() {


	// TESTS - TODO: LSH tests

//		std::cout<<"TEST SIZE: " << trainingDescriptors[0].size() <<" " << trainingDescriptors[0][0].rows << " " << trainingDescriptors[0][0].cols << std::endl;
//		std::cout<<trainingDescriptors[0][0].type() << std::endl;
//	// LSH
//	cvflann::Matrix<unsigned char> data(
//			(unsigned char*) trainingDescriptors[0][0].data,
//			trainingDescriptors[0][0].rows, trainingDescriptors[0][0].cols);
//	cvflann::IndexParams indexParams = cvflann::LshIndexParams();//(20, 15, 2); // LinearIndexParams();//
//	cvflann::Index<cvflann::Hamming<unsigned char>> lsh(data, indexParams);
//	for (auto trainSeq : trainingDescriptors) {
//		lsh.push_back(cv::flann::GenericIndex<cv::flann::Hamming<int>>(trainSeq,
//			indexParams));
//	}
//	cv::flann::GenericIndex<cv::flann::Hamming<int>> lsh(testDescriptors[0],
//			indexParams);

}

//std::vector<boost::filesystem::path> OpenABLE::getSubDirectories(std::string path) {
//
//	// Reading directories with parts of ground truth trajectory
//	boost::filesystem::path targetDir(path);
//	std::vector<boost::filesystem::path> dirs;
//	for (boost::filesystem::recursive_directory_iterator i(targetDir), eod;
//			i != eod; ++i) {
//		const boost::filesystem::path cp = (*i);
//		if (!is_regular_file(cp)) {
//			dirs.push_back(cp);
//		}
//	}
//	std::sort(dirs.begin(), dirs.end());
//	return dirs;
//}

void OpenABLE::computeVisualPlaceRecognition(
		const std::vector<std::vector<Mat> >& trainingDescriptors,
		const std::vector<Mat>& testDescriptors,
		std::vector<Mat>& openABLE_similarityMatrices,
		std::vector<Mat>& fastABLE_similarityMatrices,
		bool saveMatrices) {

	// We zero times
	t_matching = 0, t_matchingNew = 0;

	double t1, t2;

	int id = 0;
	for (auto &trainDesc : trainingDescriptors) {
		if (saveMatrices) {
			openABLE_similarityMatrices.push_back(
					Mat::ones(testDescriptors.size() - compareLength + 1, trainDesc.size() - compareLength + 1,
							CV_32FC1));
			fastABLE_similarityMatrices.push_back(
					Mat::ones(testDescriptors.size() - compareLength + 1, trainDesc.size() - compareLength + 1,
							CV_32FC1));
		} else {
			openABLE_similarityMatrices.push_back(Mat());
			fastABLE_similarityMatrices.push_back(Mat());
			id++;
		}
	}


	// Used to save previous results
	std::vector<std::vector<int> > previousDistances {
			trainingDescriptors.size(), std::vector<int>() };
	int imageCounter = 0;


	// This loop computes each iteration of ABLE
	for (auto dummy : testDescriptors) {
		for (uint i = 0; i < trainingDescriptors.size(); i++) {

			// Image matching
			t1 = getTickCount();
			openABLE_matching(imageCounter, testDescriptors,
					trainingDescriptors[i], openABLE_similarityMatrices[i]);
			t2 = getTickCount();
			t_matching += 1000.0 * (t2 - t1) / getTickFrequency();


			// Image matching - new version to compare time
			t1 = getTickCount();
			fastABLE_matching(imageCounter, testDescriptors,
					trainingDescriptors[i], previousDistances[i],
					fastABLE_similarityMatrices[i]);
			t2 = getTickCount();
			t_matchingNew += 1000.0 * (t2 - t1) / getTickFrequency();

		}
		imageCounter++;
	}

}

//int OpenABLE::readTrainSequences(
//		const std::vector<boost::filesystem::path>& dirs,
//		std::vector<std::vector<Mat> >& trainingSequences,
//		std::vector<std::vector<Mat> >& trainingDescriptors) {
//	int trainingImagesNumber = 0;
//	for (auto& d : dirs) {
//		std::cout << "Reading images from directory: " << d.string()
//				<< std::endl;
//		std::vector<Mat> trainingPart = readImageSequence(d.string());
//		trainingSequences.push_back(trainingPart);
//
//		trainingImagesNumber += trainingPart.size();
//		std::vector<Mat> trainingPartDesc;
//		for (auto image : trainingPart) {
//			cvtColor(image, image, COLOR_BGR2GRAY);
//			trainingPartDesc.push_back(global_description(image));
//		}
//		trainingDescriptors.push_back(trainingPartDesc);
//	}
//	return trainingImagesNumber;
//}

std::vector<double> OpenABLE::automaticThresholdEstimation(const std::vector<std::vector<Mat> >& trainingDescriptors) {

	std::vector<double> localThresholds;
	double globalMin = std::numeric_limits<double>::max();
	for (uint i = 0; i < trainingDescriptors.size(); i++) {
		std::vector<Mat> chosenDesc = trainingDescriptors[i];

		std::vector<std::vector<Mat> > tmpGroundTruthDescriptors(
				trainingDescriptors);

		tmpGroundTruthDescriptors.erase(tmpGroundTruthDescriptors.begin() + i);
		std::vector<Mat> openABLE_similarityMatrices,
				fastABLE_similarityMatrices;

		computeVisualPlaceRecognition(tmpGroundTruthDescriptors, chosenDesc,
				openABLE_similarityMatrices, fastABLE_similarityMatrices);

		double localMin = std::numeric_limits<double>::max();
		for (auto& m : fastABLE_similarityMatrices) {
			double minVal, maxVal;
			cv::minMaxLoc(m, &minVal, &maxVal);
			localMin = std::min(localMin, minVal);
		}
		std::cout << "localMin = " << localMin / compareLength << std::endl;
		localThresholds.push_back(localMin / compareLength);
		globalMin = std::min(globalMin, localMin);
	}
	std::cout << "GlobalMin/compareLength  = " << globalMin / compareLength
			<< std::endl;

	return localThresholds;
}

std::vector<std::vector<std::pair<int, int> > > OpenABLE::computeAndSaveRawVisualPlaceRecognitions(
		const std::vector<std::vector<Mat> >& trainingSequences,
		const std::vector<Mat>& fastABLE_similarityMatrices,
		const std::vector<double>& localThresholds) {

	ofstream matchedStream("rawVisualPlaceRecognitions.txt");
	std::vector<std::vector<std::pair<int, int> > > results(
			trainingSequences.size(), std::vector<std::pair<int, int> >());
	int seqNo = 0;
	for (auto& matrix : fastABLE_similarityMatrices) {
		int nRows = matrix.rows;
		int nCols = matrix.cols;
		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nCols; j++) {
				if (matrix.at<float>(i, j)
						< localThresholds[seqNo] * compareLength
								* moreMatchCoeff) {
					matchedStream << "Groundtruth: seqNo= " << seqNo << " id= "
							<< j << "\t Test: id= " << i << "\t Value: "
							<< matrix.at<float>(i, j) / compareLength
							<< std::endl;
					results[seqNo].push_back(std::make_pair(j, i));
				}
			}
		}
		seqNo++;
	}
	matchedStream.close();
	return results;
}

// We cluster those matches to easily mark as correct/incorrect and save final results
void OpenABLE::markAndSaveClusteredVisualPlaceRecognitions(
		const std::vector<std::vector<Mat> >& trainingSequences,
		const std::vector<std::vector<std::pair<int, int> > >& results,
		const std::vector<Mat>& testSequence) {

	// Let's create needed variables
	DBScan dbscan(2, 1, 1);
	ofstream resultStream("resultStatistics.txt");
	int correct = 0, correctCount = 0, incorrect = 0, placeCount = 0;

	// We idenpedently process each part of traing sequence
	for (uint i = 0; i < trainingSequences.size(); i++) {


		// Running clustering algorithm, the result is:
		//	-	clusterMinMax -> vector with minimal and maximal index of train sequence
		//	-	<int,int,int> -> vector of test index, train index and number of matches
		std::vector<std::pair<int, int> > clusterMinMax;
		std::vector<std::tuple<int, int, int> > clusteredResults = dbscan.run(
				results[i], clusterMinMax);


		// For each clustered result
		int groupNo = 0;
		for (auto& r : clusteredResults) {

			// Retrieve indices
			int trainIndex = std::get < 0 > (r);
			int testIndex = std::get < 1 > (r);
			int matchesCount = std::get < 2 > (r);

			// Show corresponding image from train and test sequence
			cv::imshow("TraingSequence", trainingSequences[i][trainIndex]);
			cv::imshow("TestSequence", testSequence[testIndex]);

			// Mark as either positive or negatice visual loop closure
			int key = waitKey(0);
			if ((char) (key) == 'y') {
				resultStream << "YES\t";
				correctCount++;
				correct += matchesCount;
			} else {
				resultStream << "NO\t";
				incorrect += matchesCount;
			}

			// Save to file
			resultStream << "Count: " << matchesCount << "\t Test < " << testIndex << " >\t GT (seqNo = " << i << ") < "
							<< clusterMinMax[groupNo + 1].first << " "
							<< trainIndex << " " << clusterMinMax[groupNo + 1].second << " > "
							<< std::endl;

		}
		placeCount += clusteredResults.size();
	}

	resultStream << std::endl;
	resultStream << "Places count: " << placeCount << std::endl;
	resultStream << "Correctly recognized places: " << correctCount
			<< std::endl;
	resultStream << "False positives places: " << placeCount - correctCount
			<< std::endl;
	resultStream << "Correct LC: " << correct << std::endl;
	resultStream << "False positive LC: " << incorrect << std::endl;
	resultStream.close();
}

/******************************************************************************/

/**
 * @brief This method computes the OpenABLE algorithm for a complete image dataset.
 */
void OpenABLE::compute_OpenABLE() {

	// Getting the paths of subdirectories with parts of ground truth trajectory
//	std::vector<boost::filesystem::path> dirs = getSubDirectories(trainSequencePath);
//
//
//	// Reading the images and computing descriptors for trainSequences
//	std::vector<std::vector<Mat>> trainSequences;
//	std::vector<std::vector<Mat>> trainDescriptors;
//	int trainingImagesNumber = readTrainSequences(dirs, trainSequences,
//			trainDescriptors);
//
//	// Reading the images for test sequence
//	std::vector<Mat> testSequence = readImageSequence(testSequencePath);
//
//	// Computing the descriptors for test sequence
//	std::vector<Mat> testDescriptors;
//	for (auto image : testSequence)
//	{
//		cvtColor(image, image, COLOR_BGR2GRAY);
//		auto start_time = chrono::high_resolution_clock::now();
//		testDescriptors.push_back(global_description(image));
//		auto end_time = chrono::high_resolution_clock::now();
//		t_description += chrono::duration_cast<chrono::microseconds>(end_time - start_time).count() / 1000.0;
//	}
//
//	std::cout << "Now, automatic threshold estimation!" << std::endl;
//
//	// Estimates threshold for each part of the training sequence based on other part of training sequence
//	std::vector<double> localThresholds = automaticThresholdEstimation(trainDescriptors);
//
//
//	std::cout << "Now, processing sequence!" << std::endl;
//
//	// Compute matches between sequences - we store both matrices for comparison to prove that both are identical
//	std::vector<Mat> openABLE_similarityMatrices, fastABLE_similarityMatrices;
//	computeVisualPlaceRecognition(trainDescriptors, testDescriptors,
//			openABLE_similarityMatrices, fastABLE_similarityMatrices, true);
//
//
//	// The final times are saved for studying performance.
//	t_avg_description = t_description / ((float) (trainingImagesNumber));
//	t_avg_matching = t_matching
//			/ ((float) (trainingImagesNumber) * (float) (testSequence.size())/2.0);
//	t_avg_matchingNew = t_matchingNew
//				/ ((float) (trainingImagesNumber) * (float) (testSequence.size())/2.0);
//
//
//
//	// Results contain raw matches from ABLE algorithm
//	// It reverses the indexes:
//	//	-> previously it was (test, train)
//	//	-> results are in (train, test)
//	std::vector<std::vector<std::pair<int, int> > > results =
//			computeAndSaveRawVisualPlaceRecognitions(trainSequences,
//					fastABLE_similarityMatrices, localThresholds);
//
//	for (auto &res : results) {
//		std::cout <<"We got " << res.size() <<" matches" << std::endl;
//	}
//
//
//	// We cluster those matches to easily mark as correct/incorrect and save final results
//	markAndSaveClusteredVisualPlaceRecognitions(trainSequences, results,
//			testSequence);
//
//
//	// We save similarity matrices as it is done in OpenABLE
//	for (uint i = 0; i < trainSequences.size(); i++) {
//		similarity_matrix_normalization(openABLE_similarityMatrices[i]);
//		similarity_matrix_to_png(openABLE_similarityMatrices[i], compareLength, "openABLE_similarityMatrices" + to_string(i) + ".jpg");
//
//		similarity_matrix_normalization(fastABLE_similarityMatrices[i]);
//		similarity_matrix_to_png(fastABLE_similarityMatrices[i], compareLength, "fastABLE_similarityMatrices" + to_string(i) + ".jpg");
//
//	}
}


/******************************************************************************/

/**
 * @brief This method prints some information about computational times of OpenABLE.
 */
void OpenABLE::show_times() {

cout << "Computational times in ms" << endl;

cout << "OpenABLE/FastABLE: average time for describing an image: " << t_avg_description << " ms" << endl;
cout << "OpenABLE: total matching time: " << t_matching << " ms" << endl;
cout << "OpenABLE: average time for matching two images: " << t_avg_matching << " ms" << endl;
cout << "FastABLE: total matching time: " << t_matchingNew << " ms" << endl;
cout << "FastABLE: average time for matching two images: " << t_avg_matchingNew << " ms" << endl;

cout << "FastABLE is " << t_matching / t_matchingNew  << " times faster than OpenABLE" << std::endl;



}


void OpenABLE::getMatchingTimes(float& tOpenable, float& tFastable, int& numImgs) {
	tOpenable = t_matching;
	tFastable = t_matchingNew;
	numImgs = 1;
}

/******************************************************************************/

/**
 * @brief This method is used when comparing two videos (e.g. Nordlandsbanen dataset)
 */
void OpenABLE::computeForVideo(std::string videoPath[2], int startingFrameId, int numberOfFrames) {


}
