
#include "rdft.h"

/* like X(tensor_max_index), but takes into account the special n/2+1
   final dimension for the complex output/input of an R2HC/HC2R transform. */
INT fftwf_rdft2_tensor_max_index(const tensor *sz, rdft_kind k)
{
     int i;
     INT n = 0;

     A(FINITE_RNK(sz->rnk));
     for (i = 0; i + 1 < sz->rnk; ++i) {
          const iodim *p = sz->dims + i;
          n += (p->n - 1) * fftwf_imax(fftwf_iabs(p->is), fftwf_iabs(p->os));
     }
     if (i < sz->rnk) {
	  const iodim *p = sz->dims + i;
	  INT is, os;
	  fftwf_rdft2_strides(k, p, &is, &os);
	  n += fftwf_imax((p->n - 1) * fftwf_iabs(is), (p->n/2) * fftwf_iabs(os));
     }
     return n;
}
