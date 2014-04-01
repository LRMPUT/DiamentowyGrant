LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Visual Odometry library
include ../OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := vo
LOCAL_SRC_FILES := visual_odometry.cpp
LOCAL_LDLIBS +=  -llog -ldl
LOCAL_STATIC_LIBRARIES := fivepoint_static_library

include $(BUILD_SHARED_LIBRARY)
