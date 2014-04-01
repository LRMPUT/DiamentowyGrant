#include <jni.h>
#include <string.h>
#include <android/log.h>

#include "g2o/core/sparse_optimizer.h"
#include "g2o/core/block_solver.h"
#include "g2o/core/optimization_algorithm_gauss_newton.h"
#include "g2o/core/optimization_algorithm_levenberg.h"
#include "g2o/solvers/csparse/linear_solver_csparse.h"

#include "g2o/core/factory.h"
#include "g2o/stuff/command_args.h"

#define DEBUG_TAG "NDK_MainActivity"

using namespace std;
using namespace g2o;

extern "C" {

	JNIEXPORT void JNICALL Java_com_example_czm_MainActivity_helloLog(JNIEnv * env, jobject self, jstring logThis);

	JNIEXPORT void JNICALL Java_com_example_czm_MainActivity_helloLog(JNIEnv * env, jobject self, jstring logThis)
	{
		jboolean isCopy;
		const char * szLogThis = env->GetStringUTFChars(logThis, &isCopy);
		string path(szLogThis);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", szLogThis);


		string outputFilename = path + "parking-out.g2o";
		string inputFilename = path + "parking-garage.g2o";

		// create the linear solver
		BlockSolverX::LinearSolverType * linearSolver = new LinearSolverCSparse<BlockSolverX::PoseMatrixType>();

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 1");

		// create the block solver on top of the linear solver
		BlockSolverX* blockSolver = new BlockSolverX(linearSolver);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 2");

		// create the algorithm to carry out the optimization
		//OptimizationAlgorithmGaussNewton* optimizationAlgorithm = new OptimizationAlgorithmGaussNewton(blockSolver);
		OptimizationAlgorithmLevenberg* optimizationAlgorithm = new OptimizationAlgorithmLevenberg(blockSolver);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 3");

		// NOTE: We skip to fix a variable here, either this is stored in the file
		// itself or Levenberg will handle it.

		// create the optimizer to load the data and carry out the optimization
		SparseOptimizer optimizer;
		optimizer.setVerbose(true);
		optimizer.setAlgorithm(optimizationAlgorithm);

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 4");

		ifstream ifs(inputFilename.c_str());
		if (! ifs) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "unable to open input file");
			return;
		}

		string line;
		getline(ifs,line);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", line.c_str());

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 5");

		optimizer.load(ifs);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 6");
		optimizer.initializeOptimization();
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "debug 7");
		optimizer.optimize(10);

		ofstream ofs(outputFilename.c_str());

		if (! ofs) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "unable to open output file");
			return;
		}

		ofs<<"test\n";

		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "saving");
		bool result = optimizer.save(ofs);
		if(result) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "NDK:LC: [%s]", "saved");
		}

		ofs.close();
		ifs.close();

		env->ReleaseStringUTFChars(logThis, szLogThis);
	}
}
