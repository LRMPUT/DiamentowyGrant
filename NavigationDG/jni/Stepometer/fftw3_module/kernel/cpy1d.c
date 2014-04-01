

#include "ifftw.h"

void fftwf_cpy1d(float *I, float *O, INT n0, INT is0, INT os0, INT vl)
{
     INT i0, v;

     A(I != O);
     switch (vl) {
	 case 1:
	      if ((n0 & 1) || is0 != 1 || os0 != 1) {
		   for (; n0 > 0; --n0, I += is0, O += os0)
			*O = *I;
		   break;
	      }
	      n0 /= 2; is0 = 2; os0 = 2;
	      /* fall through */
	 case 2:
	      if ((n0 & 1) || is0 != 2 || os0 != 2) {
		   for (; n0 > 0; --n0, I += is0, O += os0) {
			float x0 = I[0];
			float x1 = I[1];
			O[0] = x0;
			O[1] = x1;
		   }
		   break;
	      }
	      n0 /= 2; is0 = 4; os0 = 4;
	      /* fall through */
	 case 4:
	      for (; n0 > 0; --n0, I += is0, O += os0) {
		   float x0 = I[0];
		   float x1 = I[1];
		   float x2 = I[2];
		   float x3 = I[3];
		   O[0] = x0;
		   O[1] = x1;
		   O[2] = x2;
		   O[3] = x3;
	      }
	      break;
	 default:
	      for (i0 = 0; i0 < n0; ++i0)
		   for (v = 0; v < vl; ++v) {
			float x0 = I[i0 * is0 + v];
			O[i0 * os0 + v] = x0;
		   }
	      break;
     }
}
