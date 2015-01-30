LOCAL_PATH := $(call my-dir)
MY_PATH := $(LOCAL_PATH)

include $(LOCAL_PATH)/libg2o/Android.mk

include $(CLEAR_VARS)
LOCAL_PATH := $(MY_PATH)
LOCAL_LDLIBS := -llog
LOCAL_MODULE    := GraphOptimizationModule
LOCAL_SRC_FILES := edge_se2_pointXY_distance.cpp edge_se2_distanceOrientation.cpp graphManager.cpp GraphOptimizationModuleExport.cpp

LOCAL_SHARED_LIBRARIES := g2o_stuff g2o_core g2o_ext_csparse g2o_csparse_extension g2o_solver_csparse g2o_types_slam2d g2o_types_slam3d

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/csparse


include $(BUILD_SHARED_LIBRARY)