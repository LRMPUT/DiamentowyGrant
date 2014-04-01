LOCAL_PATH := $(call my-dir)

# nonfree
include $(CLEAR_VARS)
include ../OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := surfsift
LOCAL_C_INCLUDES:= ../OpenCV-2.4.8-android-sdk/sdk/native/jni/include
LOCAL_SRC_FILES := nonfree/nonfreeInit.cpp nonfree/precomp.cpp nonfree/sift.cpp nonfree/surf.cpp export.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)