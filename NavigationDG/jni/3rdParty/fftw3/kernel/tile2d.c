#include "ifftw.h"

void fftwf_tile2d(INT n0l, INT n0u, INT n1l, INT n1u, INT tilesz,
	       void (*f)(INT n0l, INT n0u, INT n1l, INT n1u, void *args),
	       void *args)
{
     INT d0, d1;

     A(tilesz > 0); /* infinite loops otherwise */

 tail:
     d0 = n0u - n0l;
     d1 = n1u - n1l;

     if (d0 >= d1 && d0 > tilesz) {
	  INT n0m = (n0u + n0l) / 2;
	  fftwf_tile2d(n0l, n0m, n1l, n1u, tilesz, f, args);
	  n0l = n0m; goto tail;
     } else if (/* d1 >= d0 && */ d1 > tilesz) {
	  INT n1m = (n1u + n1l) / 2;
	  fftwf_tile2d(n0l, n0u, n1l, n1m, tilesz, f, args);
	  n1l = n1m; goto tail;
     } else {
	  f(n0l, n0u, n1l, n1u, args);
     }
}

INT fftwf_compute_tilesz(INT vl, int how_many_tiles_in_cache)
{
     return fftwf_isqrt(CACHESIZE /
		     (((INT)sizeof(float)) * vl * (INT)how_many_tiles_in_cache));
}
