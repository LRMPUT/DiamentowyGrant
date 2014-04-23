sources:=  nonfree/nonfreeInit.cpp nonfree/precomp.cpp nonfree/sift.cpp nonfree/surf.cpp 

LOCAL_C_INCLUDES:= ../OpenCV-2.4.8-android-sdk/sdk/native/jni/include
LOCAL_SRC_FILES += $(addprefix NonFree/, $(sources))