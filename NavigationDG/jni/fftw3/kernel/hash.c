#include "ifftw.h"

unsigned fftwf_hash(const char *s)
{
     unsigned h = 0xDEADBEEFu;
     do {
	  h = h * 17 + (int)*s;
     } while (*s++);
     return h;
}
