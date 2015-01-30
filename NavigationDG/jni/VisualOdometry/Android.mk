LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Visual Odometry library
include C:/DiamentowyGrant/OpenCV-2.4.10-android-sdk/sdk/native/jni/OpenCV.mk

# DetectDescriebe contains:
#	- LDB
#	- NonFree module
#	- detection and description of features
include $(LOCAL_PATH)/DetectDescribe/Android.mk


LOCAL_MODULE    := VisualOdometryModule
LOCAL_SRC_FILES += FivePoint/FivePointAlgorithm.cpp VisualOdometryModuleExport.cpp


#LOCAL_C_INCLUDES := Eigen
LOCAL_SRC_FILES += FivePointMadeEasy/Rpoly.cpp FivePointMadeEasy/5point.cpp FivePointMadeEasy/main.cpp


#LOCAL_CFLAGS += -fopenmp -O3
LOCAL_LDLIBS +=  -llog -ldl -O3 -DNDEBUG
LOCAL_LDFLAGS +=  -pthread
LOCAL_CFLAGS += -DNDEBUG 

include $(BUILD_SHARED_LIBRARY)

