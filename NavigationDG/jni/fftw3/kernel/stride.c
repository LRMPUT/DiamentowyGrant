
#include "ifftw.h"

const INT fftwf_an_INT_guaranteed_to_be_zero = 0;

#ifdef PRECOMPUTE_ARRAY_INDICES
stride fftwf_mkstride(INT n, INT s)
{
     int i;
     INT *p = (INT *) MALLOC(n * sizeof(INT), STRIDES);

     for (i = 0; i < n; ++i)
          p[i] = s * i;

     return p;
}

void fftwf_stride_destroy(stride p)
{
     fftwf_ifree0(p);
}

#endif