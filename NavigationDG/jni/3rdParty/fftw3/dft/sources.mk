sources:= conf.c bluestein.c buffered.c  ct.c dftw-generic.c dftw-genericbuf.c generic.c indirect.c indirect-transpose.c kdft.c \
 kdft-dif.c kdft-difsq.c kdft-dit.c plan.c problem.c rader.c rank-geq2.c solve.c  vrank-geq1.c zero.c nop.c direct.c dftw-direct.c dftw-directsq.c

LOCAL_SRC_FILES += $(addprefix dft/, $(sources))
