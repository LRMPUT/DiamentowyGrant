


#include "ifftw.h"

tensor *fftwf_mktensor_2d(INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1)
{
     tensor *x =fftwf_mktensor(2);
     x->dims[0].n = n0;
     x->dims[0].is = is0;
     x->dims[0].os = os0;
     x->dims[1].n = n1;
     x->dims[1].is = is1;
     x->dims[1].os = os1;
     return x;
}


tensor *fftwf_mktensor_3d(INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1,
		       INT n2, INT is2, INT os2)
{
     tensor *x =fftwf_mktensor(3);
     x->dims[0].n = n0;
     x->dims[0].is = is0;
     x->dims[0].os = os0;
     x->dims[1].n = n1;
     x->dims[1].is = is1;
     x->dims[1].os = os1;
     x->dims[2].n = n2;
     x->dims[2].is = is2;
     x->dims[2].os = os2;
     return x;
}
