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

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphOptimize(
		JNIEnv* env, jobject self, jlong addrGraph, jint iterationCount,
		jstring logThis);

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphDestroy(
		JNIEnv* env, jobject self, jlong addrGraph);

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

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphOptimize(
		JNIEnv* env, jobject self, jlong addrGraph, jint iterationCount,
		jstring logThis) {
	// Calling
	GraphManager &graphManager = *(GraphManager*) addrGraph;
	graphManager.optimize(iterationCount);

	// Retrieve string from jstring concerning the path
	jboolean isCopy;
	const char * szLogThis = env->GetStringUTFChars(logThis, &isCopy);
	string path(szLogThis);
	env->ReleaseStringUTFChars(logThis, szLogThis);

	// Save result
	ofstream ofs(path + "lastCreatedOptimizedGraph.g2o");
	graphManager.saveOptimizationResult(ofs);
	ofs.close();

}

JNIEXPORT void JNICALL Java_org_dg_graphManager_GraphManager_NDKGraphDestroy(
		JNIEnv* env, jobject self, jlong addrGraph) {
	// Destroy object
	delete (GraphManager*) (addrGraph);
}
}
