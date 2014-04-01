


#include "ifftw.h"

void fftwf_ops_zero(opcnt *dst)
{
     dst->add = dst->mul = dst->fma = dst->other = 0;
}

void fftwf_ops_cpy(const opcnt *src, opcnt *dst)
{
     *dst = *src;
}

void fftwf_ops_other(INT o, opcnt *dst)
{
     fftwf_ops_zero(dst);
     dst->other = o;
}

void fftwf_ops_madd(INT m, const opcnt *a, const opcnt *b, opcnt *dst)
{
     dst->add = m * a->add + b->add;
     dst->mul = m * a->mul + b->mul;
     dst->fma = m * a->fma + b->fma;
     dst->other = m * a->other + b->other;
}

void fftwf_ops_add(const opcnt *a, const opcnt *b, opcnt *dst)
{
     fftwf_ops_madd(1, a, b, dst);
}

void fftwf_ops_add2(const opcnt *a, opcnt *dst)
{
     fftwf_ops_add(a, dst, dst);
}

void fftwf_ops_madd2(INT m, const opcnt *a, opcnt *dst)
{
     fftwf_ops_madd(m, a, dst, dst);
}

