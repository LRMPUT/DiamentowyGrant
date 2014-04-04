LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Visual Odometry library
include ../OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk
#include $(LOCAL_PATH)/FivePoint/Android.mk
#include $(LOCAL_PATH)/NonFree/Android.mk
#include $(LOCAL_PATH)/ScaleEstimation/Android.mk

LOCAL_MODULE    := VisualOdometryModule
LOCAL_SRC_FILES := FivePoint/FivePointAlgorithm.cpp VisualOdometryModuleExport.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)

