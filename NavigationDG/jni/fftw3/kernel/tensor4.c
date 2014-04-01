


#include "ifftw.h"

INT fftwf_tensor_max_index(const tensor *sz)
{
     int i;
     INT ni = 0, no = 0;

     A(FINITE_RNK(sz->rnk));
     for (i = 0; i < sz->rnk; ++i) {
          const iodim *p = sz->dims + i;
          ni += (p->n - 1) *fftwf_iabs(p->is);
          no += (p->n - 1) *fftwf_iabs(p->os);
     }
     return fftwf_imax(ni, no);
}

#define tensor_min_xstride(sz, xs) {			\
     A(FINITE_RNK(sz->rnk));				\
     if (sz->rnk == 0) return 0;			\
     else {						\
          int i;					\
          INT s =fftwf_iabs(sz->dims[0].xs);		\
          for (i = 1; i < sz->rnk; ++i)			\
               s =fftwf_imin(s,fftwf_iabs(sz->dims[i].xs));	\
          return s;					\
     }							\
}

INT fftwf_tensor_min_istride(const tensor *sz) tensor_min_xstride(sz, is)
INT fftwf_tensor_min_ostride(const tensor *sz) tensor_min_xstride(sz, os)

INT fftwf_tensor_min_stride(const tensor *sz)
{
     return fftwf_imin(fftwf_tensor_min_istride(sz),fftwf_tensor_min_ostride(sz));
}

int fftwf_tensor_inplace_strides(const tensor *sz)
{
     int i;
     A(FINITE_RNK(sz->rnk));
     for (i = 0; i < sz->rnk; ++i) {
          const iodim *p = sz->dims + i;
          if (p->is != p->os)
               return 0;
     }
     return 1;
}

int fftwf_tensor_inplace_strides2(const tensor *a, const tensor *b)
{
     return fftwf_tensor_inplace_strides(a) && fftwf_tensor_inplace_strides(b);
}

/* return true (1) iff *any* strides of sz decrease when we
   tensor_inplace_copy(sz, k). */
static int tensor_strides_decrease(const tensor *sz, inplace_kind k)
{
     if (FINITE_RNK(sz->rnk)) {
          int i;
          for (i = 0; i < sz->rnk; ++i)
               if ((sz->dims[i].os - sz->dims[i].is)
                   * (k == INPLACE_OS ? (INT)1 : (INT)-1) < 0)
                    return 1;
     }
     return 0;
}

/* Return true (1) iff *any* strides of sz decrease when we
   tensor_inplace_copy(k) *or* if *all* strides of sz are unchanged
   but *any* strides of vecsz decrease.  This is used in indirect.c
   to determine whether to use INPLACE_IS or INPLACE_OS.

   Note: X(tensor_strides_decrease)(sz, vecsz, INPLACE_IS)
         || X(tensor_strides_decrease)(sz, vecsz, INPLACE_OS)
         || X(tensor_inplace_strides2)(p->sz, p->vecsz)
   must always be true. */
int fftwf_tensor_strides_decrease(const tensor *sz, const tensor *vecsz,
			       inplace_kind k)
{
     return(tensor_strides_decrease(sz, k)
	    || (fftwf_tensor_inplace_strides(sz)
		&& tensor_strides_decrease(vecsz, k)));
}
