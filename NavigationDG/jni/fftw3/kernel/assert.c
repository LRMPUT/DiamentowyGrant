

#include "ifftw.h"
#include <stdio.h>
#include <stdlib.h>

void fftwf_assertion_failed(const char *s, int line, const char *file)
{
     fflush(stdout);
     fprintf(stderr, "fftw: %s:%d: assertion failed: %s\n", file, line, s);
#ifdef HAVE_ABORT
     abort();
#else
     exit(EXIT_FAILURE);
#endif
}
