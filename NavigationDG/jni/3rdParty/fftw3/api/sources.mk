sources:=  apiplan.c configure.c execute.c  malloc.c mapflags.c mktensor-rowmajor.c plan-many-dft-r2c.c rdft2-pad.c  the-planner.c

LOCAL_SRC_FILES += $(addprefix api/, $(sources))
