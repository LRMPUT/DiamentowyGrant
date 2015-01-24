LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_ext_csparse
LOCAL_SRC_FILES := libg2o_ext_csparse.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_csparse_extension
LOCAL_SRC_FILES := libg2o_csparse_extension.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := g2o_stuff
LOCAL_SRC_FILES := libg2o_stuff.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_core
LOCAL_SRC_FILES := libg2o_core.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_csparse
LOCAL_SRC_FILES := libg2o_solver_csparse.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_slam2d
LOCAL_SRC_FILES := libg2o_types_slam2d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_slam3d
LOCAL_SRC_FILES := libg2o_types_slam3d.so
include $(PREBUILT_SHARED_LIBRARY)
