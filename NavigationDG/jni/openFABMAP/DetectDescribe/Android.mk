
include $(LOCAL_PATH)/DetectDescribe/LDB/Android.mk
include $(LOCAL_PATH)/DetectDescribe/NonFree/Android.mk

sources:= DetectDescribe.cpp

LOCAL_SRC_FILES += $(addprefix DetectDescribe/, $(sources))