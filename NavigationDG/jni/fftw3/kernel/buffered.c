
#include "ifftw.h"

#define DEFAULT_MAXNBUF ((INT)256)

/* approx. 512KB of buffers for complex data */
#define MAXBUFSZ (256 * 1024 / (INT)(sizeof(float)))

INT fftwf_nbuf(INT n, INT vl, INT maxnbuf)
{
     INT i, nbuf, lb;

     if (!maxnbuf)
	  maxnbuf = DEFAULT_MAXNBUF;

     nbuf = fftwf_imin(maxnbuf,
		    fftwf_imin(vl, fftwf_imax((INT)1, MAXBUFSZ / n)));

     /*
      * Look for a buffer number (not too small) that divides the
      * vector length, in order that we only need one child plan:
      */
     lb = fftwf_imax(1, nbuf / 4);
     for (i = nbuf; i >= lb; --i)
          if (vl % i == 0)
               return i;

     /* whatever... */
     return nbuf;
}

#define SKEW 6 /* need to be even for SIMD */
#define SKEWMOD 8

INT fftwf_bufdist(INT n, INT vl)
{
     if (vl == 1)
	  return n;
     else
	  /* return smallest X such that X >= N and X == SKEW (mod SKEWMOD) */
	  return n + fftwf_modulo(SKEW - n, SKEWMOD);
}

int fftwf_toobig(INT n)
{
     return n > MAXBUFSZ;
}

/* TRUE if there exists i < which such that maxnbuf[i] and
   maxnbuf[which] yield the same value, in which case we canonicalize
   on the minimum value */
int fftwf_nbuf_redundant(INT n, INT vl, int which,
		      const INT *maxnbuf, int nmaxnbuf)
{
     int i;
     for (i = 0; i < which; ++i)
	  if (fftwf_nbuf(n, vl, maxnbuf[i]) == fftwf_nbuf(n, vl, maxnbuf[which]))
	       return 1;
     return 0;
}
