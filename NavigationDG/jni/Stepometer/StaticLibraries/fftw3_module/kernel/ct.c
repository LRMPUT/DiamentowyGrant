

#include "ifftw.h"

#define POW2P(n) (((n) > 0) && (((n) & ((n) - 1)) == 0))

/* TRUE if radix-r is ugly for size n */
int fftwf_ct_uglyp(INT min_n, INT v, INT n, INT r)
{
     return (n <= min_n) || (POW2P(n) && (v * (n / r)) <= 4);
}
