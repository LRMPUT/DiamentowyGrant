#include "rdft.h"

/* Deal with annoyance because the tensor (is,os) applies to
   (r,rio/iio) for R2HC and vice-versa for HC2R.  We originally had
   (is,os) always apply to (r,rio/iio), but this causes other
   headaches with the tensor functions. */
void fftwf_rdft2_strides(rdft_kind kind, const iodim *d, INT *rs, INT *cs)
{
     if (kind == R2HC) {
	  *rs = d->is;
	  *cs = d->os;
     }
     else {
	  A(kind == HC2R);
	  *rs = d->os;
	  *cs = d->is;
     }
}
