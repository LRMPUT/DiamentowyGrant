LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# FABMAP library
include C:/DiamentowyGrant/OpenCV-2.4.10-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := FABMAPModule
LOCAL_SRC_FILES += BOWMSCTrainer.cpp ChowLiuTree.cpp FabMap.cpp

LOCAL_LDLIBS +=  -llog -ldl -O3 -DNDEBUG
LOCAL_CFLAGS += -DNDEBUG 

include $(BUILD_SHARED_LIBRARY)

