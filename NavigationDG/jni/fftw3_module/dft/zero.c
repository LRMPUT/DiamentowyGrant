


#include "dft.h"


static void recur(const iodim *dims, int rnk,float *ri,float *ii)
{
     if (rnk == RNK_MINFTY)
          return;
     else if (rnk == 0)
          ri[0] = ii[0] = K(0.0);
     else if (rnk > 0) {
          INT i, n = dims[0].n;
          INT is = dims[0].is;

	  if (rnk == 1) {
	       /* this case is redundant but faster */
	       for (i = 0; i < n; ++i)
		    ri[i * is] = ii[i * is] = K(0.0);
	  } else {
	       for (i = 0; i < n; ++i)
		    recur(dims + 1, rnk - 1, ri + i * is, ii + i * is);
	  }
     }
}


void fftwf_dft_zerotens(tensor *sz,float *ri,float *ii)
{
     recur(sz->dims, sz->rnk, ri, ii);
}
