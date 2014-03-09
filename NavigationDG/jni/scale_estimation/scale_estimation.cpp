#include <jni.h>
#include <vector>

using namespace std;

extern "C" {

// Export declarations
JNIEXPORT void JNICALL Java_org_dg_main_MainActivity_Scale(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL Java_org_dg_main_MainActivity_Scale(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{

}

}

