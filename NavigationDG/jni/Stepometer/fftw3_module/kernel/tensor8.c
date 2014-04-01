


#include "ifftw.h"

void fftwf_tensor_destroy2(tensor *a, tensor *b)
{
     fftwf_tensor_destroy(a);
     fftwf_tensor_destroy(b);
}

void fftwf_tensor_destroy4(tensor *a, tensor *b, tensor *c, tensor *d)
{
     fftwf_tensor_destroy2(a, b);
     fftwf_tensor_destroy2(c, d);
}
