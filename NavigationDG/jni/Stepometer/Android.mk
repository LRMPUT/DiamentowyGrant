LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Visual Odometry library

LOCAL_MODULE    := StepometerModule
LOCAL_SRC_FILES := StepometerModuleExport.cpp
LOCAL_LDLIBS +=  -llog -ldl
#LOCAL_SHARED_LIBRARIES := fftw3_mod

include $(BUILD_SHARED_LIBRARY)
