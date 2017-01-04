/** @file dbscan.cpp
 *
 * \brief The proposed clustering algorithm
 * \author Michal Nowicki
 *
 */
#include "dbscan.h"

#include <iostream>

DBScan::DBScan(double _eps, int _minPts, int _featuresFromCluster) {
	eps = _eps;
	minPts = _minPts;
	featuresFromCluster = _featuresFromCluster;
}

void DBScan::expandCluster(std::vector<int> neighbourList,
		int clusteringSetSize,
		int &C) {

	// testing the neighbours
	for (int j = 0; j < neighbourList.size(); j++) {
		int x = neighbourList[j];

		// If not visited
		if (visited[x] != true) {
			visited[x] = true;
			std::vector<int> neighbourNeighbourList;

			// Calculating the number of neighbours
			for (int k = 0; k < clusteringSetSize; k++) {
				if (!visited[k] && dist[std::min(x, k)][std::max(x, k)] < eps) {
					neighbourNeighbourList.push_back(k);
				}
			}

			// If it has enough neighbours it's neighbours can be checked
			// Merging ...
			if (neighbourNeighbourList.size() >= minPts) {
				neighbourList.insert(neighbourList.end(), neighbourNeighbourList.begin(), neighbourNeighbourList.end());
			}
		}

		// if it is not yet labeled
		if (cluster[x] == 0)
			cluster[x] = C;
	}
}

int DBScan::findingClusters(int clusteringSetSize) {
	// Starting cluster id
	int C = 1;

	// For all points
	for (int i = 0; i < clusteringSetSize; i++) {
		if (visited[i] != true) {
			visited[i] = true;
			std::vector<int> neighbourList;
			// Finding neighbours
			for (int k = 0; k < clusteringSetSize; k++) {
				if (dist[std::min(i, k)][std::max(i, k)] < eps) {
					neighbourList.push_back(k);
				}
			}
			// If there are not enough neighbours to form a cluster
			if (neighbourList.size() < minPts)
				cluster[i] = -1;
			else {
				// There is a need cluster!
				cluster[i] = C;
				expandCluster(neighbourList, clusteringSetSize, C);
				C++;
			}
		}
	}
	return C;
}

std::vector< std::tuple<int, int, int> > DBScan::run(std::vector<std::pair<int,int>>  clusteringSet, std::vector<std::pair<int,int>> &clusterMinMax) {
	int clusteringSetSize = clusteringSet.size();

	// Calculating similarity matrix
	dist = std::vector<std::vector<float> >(clusteringSetSize,
			std::vector<float>(clusteringSetSize, 0));

	for (int i = 0; i < clusteringSetSize; i++)
		for (int j = i; j < clusteringSetSize; j++)
			dist[i][j] = std::sqrt( pow(clusteringSet[i].first - clusteringSet[j].first,2) + pow(clusteringSet[i].second - clusteringSet[j].second,2) );

	// Preparation - visited nodes information
	visited = std::vector<bool>(clusteringSetSize, false);

	// Output information
	// -1 means noise
	// 0 not yet processed
	// >0 belongs to group of given id
	cluster = std::vector<int>(clusteringSetSize);

	// For all points
	int clusterCount = findingClusters(clusteringSetSize);

	std::cout<<"Found clusters" << std::endl;

	// Just leave strongest from each cluster
	std::vector<int> clusterChosenCount(clusterCount);
	std::vector<std::pair<int,int>> clusterPos(clusterCount+1);
	clusterMinMax.resize(clusterCount+1, std::make_pair(-1,-1));
	for (int i=0;i<clusteringSetSize;i++ ) {
		int clusterId = cluster[i];
		if ( clusterId > 0)
		{
			clusterPos[clusterId].first += clusteringSet[i].first;
			clusterPos[clusterId].second += clusteringSet[i].second;
			clusterChosenCount[clusterId]++;

			// MIN
			if (clusteringSet[i].first < clusterMinMax[clusterId].first
					|| clusterMinMax[clusterId].first == -1) {
				clusterMinMax[clusterId].first = clusteringSet[i].first;
			}

			// MAX
			if (clusteringSet[i].first > clusterMinMax[clusterId].second
					|| clusterMinMax[clusterId].second == -1) {
				clusterMinMax[clusterId].second = clusteringSet[i].first;
			}

//			std::cout <<"WTF: " << clusterPos[clusterId].first << " " << clusterMinMax[clusterId].first << " " << clusterMinMax[clusterId].second << std::endl;
		}
	}

	std::cout<<"Position cumulated!" << std::endl;

	std::vector< std::tuple<int, int, int> > result;
	int minGt, maxGt;
	for (int i = 1; i < clusterCount; i++) {
		if ( clusterChosenCount[i] > 0)
		{
			clusterPos[i].first /= clusterChosenCount[i];
			clusterPos[i].second /= clusterChosenCount[i];
			result.push_back(std::make_tuple(clusterPos[i].first, clusterPos[i].second, clusterChosenCount[i]));
		}
	}

	std::cout<<"Cluster origin" << std::endl;


	return result;
}
