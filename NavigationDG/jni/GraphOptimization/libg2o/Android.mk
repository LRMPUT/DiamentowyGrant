LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_calibration_odom_laser
LOCAL_SRC_FILES := libg2o_calibration_odom_laser.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_cli
LOCAL_SRC_FILES := libg2o_cli.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_core
LOCAL_SRC_FILES := libg2o_core.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_csparse_extension
LOCAL_SRC_FILES := libg2o_csparse_extension.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_ext_csparse
LOCAL_SRC_FILES := libg2o_ext_csparse.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_interface
LOCAL_SRC_FILES := libg2o_interface.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_parser
LOCAL_SRC_FILES := libg2o_parser.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_simulator
LOCAL_SRC_FILES := libg2o_simulator.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_csparse
LOCAL_SRC_FILES := libg2o_solver_csparse.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_dense
LOCAL_SRC_FILES := libg2o_solver_dense.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_eigen
LOCAL_SRC_FILES := libg2o_solver_eigen.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_pcg
LOCAL_SRC_FILES := libg2o_solver_pcg.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_slam2d_linear
LOCAL_SRC_FILES := libg2o_solver_slam2d_linear.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_structure_only
LOCAL_SRC_FILES := libg2o_solver_structure_only.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_stuff
LOCAL_SRC_FILES := libg2o_stuff.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_tutorial_slam2d
LOCAL_SRC_FILES := libg2o_tutorial_slam2d.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_data
LOCAL_SRC_FILES := libg2o_types_data.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_icp
LOCAL_SRC_FILES := libg2o_types_icp.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_sba
LOCAL_SRC_FILES := libg2o_types_sba.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_sclam2d
LOCAL_SRC_FILES := libg2o_types_sclam2d.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_sim3
LOCAL_SRC_FILES := libg2o_types_sim3.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_slam2d
LOCAL_SRC_FILES := libg2o_types_slam2d.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_slam3d
LOCAL_SRC_FILES := libg2o_types_slam3d.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_SHARED_LIBRARY)



