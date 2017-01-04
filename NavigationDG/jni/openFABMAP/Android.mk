LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Visual Place Recognition library
include C:/DiamentowyGrant/OpenCV-2.4.10-android-sdk/sdk/native/jni/OpenCV.mk
#include /home/jachu/lib/NVPACK/OpenCV-2.4.10-android-sdk/sdk/native/jni/OpenCV.mk

# DetectDescriebe contains:
#	- LDB
#	- NonFree module
#	- detection and description of features
include $(LOCAL_PATH)/DetectDescribe/Android.mk

# FABMAP library containing appearance-based loop closure
sources:= BOWMSCTrainer.cpp ChowLiuTree.cpp FabMap.cpp openFABMAPcli.cpp VisualPlaceRecognitionModuleExport.cpp

#LOCAL_SRC_FILES += $(addprefix openFABMAP/, $(sources))
LOCAL_SRC_FILES += $(sources)

# Module name
LOCAL_MODULE    := VisualPlaceRecognitionModule

#LOCAL_CFLAGS += -fopenmp -O3
LOCAL_LDLIBS +=  -llog -ldl -O3 -DNDEBUG
LOCAL_LDFLAGS +=  -pthread
LOCAL_CFLAGS += -DNDEBUG 

include $(BUILD_SHARED_LIBRARY)