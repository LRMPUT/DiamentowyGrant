#include <jni.h>
#include <string.h>
#include <android/log.h>

#include "graphManager.h"

using namespace std;
using namespace g2o;

#ifndef DEBUG_TAG
#define DEBUG_TAG "NDK_MainActivity"
#endif

extern "C" {
// Exported methods to call on graph
JNIEXPORT jlong JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphCreate(
		JNIEnv* env, jobject self);

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphAddVertexEdge(
		JNIEnv* env, jobject self, jlong addrGraph, jstring g2oVertexEdge);

JNIEXPORT jdoubleArray JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphGetVertexPosition(
		JNIEnv* env, jobject self, jlong addrGraph, jint id);

JNIEXPORT jdoubleArray JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphGetPositionOfAllVertices(
		JNIEnv* env, jobject self, jlong addrGraph);

JNIEXPORT jint JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphOptimize(
		JNIEnv* env, jobject self, jlong addrGraph, jint iterationCount,
		jstring logThis);

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphDestroy(
		JNIEnv* env, jobject self, jlong addrGraph);

// Declarations
JNIEXPORT jlong JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphCreate(
		JNIEnv* env, jobject self) {
	// Create new object
	return (long) (new GraphManager());
}


JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphAddVertexEdge(
		JNIEnv* env, jobject self, jlong addrGraph, jstring g2oVertexEdge) {
	// Calling
	GraphManager &graphManager = *(GraphManager*) addrGraph;

	// Retrieve string from jstring concerning the g2o
	jboolean isCopy;
	const char * szLogThis = env->GetStringUTFChars(g2oVertexEdge, &isCopy);
	string g2oStream(szLogThis);
	env->ReleaseStringUTFChars(g2oVertexEdge, szLogThis);

	// Adding all elements
	graphManager.addToGraph(g2oStream);
}

JNIEXPORT jdoubleArray JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphGetVertexPosition(
		JNIEnv* env, jobject self, jlong addrGraph, jint id) {
	// Calling
	GraphManager &graphManager = *(GraphManager*) addrGraph;

	// Get the position
	std::vector<double> estimate = graphManager.getVertexPosition(id);


	jdoubleArray result;
	int size = 4;
	result = env->NewDoubleArray(size);
	if (result == NULL) {
	    return NULL; /* out of memory error thrown */
	}

	jdouble fill[4];
	memset(fill,0, 4);
	for(int i=0;i<estimate.size();i++) {
		fill[i] = estimate[i];
	}

	// move from the temp structure to the java structure
	(env)->SetDoubleArrayRegion(result, 0, size, fill);
	return result;
}

JNIEXPORT jdoubleArray JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphGetPositionOfAllVertices(
		JNIEnv* env, jobject self, jlong addrGraph)
{
	// Calling
	GraphManager &graphManager = *(GraphManager*) addrGraph;

	// Get the position
	std::vector<double> estimate = graphManager.getPositionOfAllVertices();

	jdoubleArray result;
	int size = estimate.size();
	result = env->NewDoubleArray(size);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	jdouble fill[ size ];
	for (int i = 0; i < estimate.size(); i++) {
		fill[i] = estimate[i];
	}

	// move from the temp structure to the java structure
	(env)->SetDoubleArrayRegion(result, 0, size, fill);
	return result;
}


JNIEXPORT jint JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphOptimize(
		JNIEnv* env, jobject self, jlong addrGraph, jint iterationCount,
		jstring logThis) {
	// Calling
	GraphManager &graphManager = *(GraphManager*) addrGraph;
	int res = graphManager.optimize(iterationCount);

	// Retrieve string from jstring concerning the path
	jboolean isCopy;
	const char * szLogThis = env->GetStringUTFChars(logThis, &isCopy);
	string path(szLogThis);
	env->ReleaseStringUTFChars(logThis, szLogThis);

	// Save result
	ofstream ofs(path + "lastCreatedOptimizedGraph.g2o");
	graphManager.saveOptimizationResult(ofs);
	ofs.close();

	return res;
}

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphDestroy(
		JNIEnv* env, jobject self, jlong addrGraph) {
	// Destroy object
	delete (GraphManager*) (addrGraph);
}
}
