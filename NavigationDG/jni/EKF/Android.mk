LOCAL_PATH := $(call my-dir)

# Extended Kalman Filter library
include $(CLEAR_VARS)
include ../OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := EKF
LOCAL_SRC_FILES := EKF/EKF.cpp EKF_export.cpp 
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)