#include "ifftw.h"

/* decompose complex pointer into real and imaginary parts.
   Flip real and imaginary if there the sign does not match
   FFTW's idea of what the sign should be */

void fftwf_extract_reim(int sign, float *c, float **r, float **i)
{
     if (sign == FFT_SIGN) {
          *r = c + 0;
          *i = c + 1;
     } else {
          *r = c + 1;
          *i = c + 0;
     }
}