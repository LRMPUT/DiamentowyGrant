


#include "ifftw.h"

tensor *fftwf_mktensor_0d(void)
{
     return fftwf_mktensor(0);
}

tensor *fftwf_mktensor_1d (INT n, INT is, INT os)
{
     tensor *x = fftwf_mktensor(1);
     x->dims[0].n = n;
     x->dims[0].is = is;
     x->dims[0].os = os;
     return x;
}
