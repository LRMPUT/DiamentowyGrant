LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_calibration_odom_laser
LOCAL_SRC_FILES := libg2o_calibration_odom_laser.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_cli
LOCAL_SRC_FILES := libg2o_cli.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_core
LOCAL_SRC_FILES := libg2o_core.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_csparse_extension
LOCAL_SRC_FILES := libg2o_csparse_extension.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_ext_csparse
LOCAL_SRC_FILES := libg2o_ext_csparse.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_interface
LOCAL_SRC_FILES := libg2o_interface.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_parser
LOCAL_SRC_FILES := libg2o_parser.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_simulator
LOCAL_SRC_FILES := libg2o_simulator.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_csparse
LOCAL_SRC_FILES := libg2o_solver_csparse.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_dense
LOCAL_SRC_FILES := libg2o_solver_dense.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_eigen
LOCAL_SRC_FILES := libg2o_solver_eigen.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_pcg
LOCAL_SRC_FILES := libg2o_solver_pcg.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_slam2d_linear
LOCAL_SRC_FILES := libg2o_solver_slam2d_linear.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_solver_structure_only
LOCAL_SRC_FILES := libg2o_solver_structure_only.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_stuff
LOCAL_SRC_FILES := libg2o_stuff.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_tutorial_slam2d
LOCAL_SRC_FILES := libg2o_tutorial_slam2d.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_data
LOCAL_SRC_FILES := libg2o_types_data.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_icp
LOCAL_SRC_FILES := libg2o_types_icp.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_sba
LOCAL_SRC_FILES := libg2o_types_sba.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_sclam2d
LOCAL_SRC_FILES := libg2o_types_sclam2d.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_sim3
LOCAL_SRC_FILES := libg2o_types_sim3.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_slam2d
LOCAL_SRC_FILES := libg2o_types_slam2d.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := g2o_types_slam3d
LOCAL_SRC_FILES := libg2o_types_slam3d.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../csparse
include $(PREBUILT_STATIC_LIBRARY)



