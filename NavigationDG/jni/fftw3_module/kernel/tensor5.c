


#include "ifftw.h"

static void dimcpy(iodim *dst, const iodim *src, int rnk)
{
     int i;
     if (FINITE_RNK(rnk))
          for (i = 0; i < rnk; ++i)
               dst[i] = src[i];
}

tensor *fftwf_tensor_copy(const tensor *sz)
{
     tensor *x =fftwf_mktensor(sz->rnk);
     dimcpy(x->dims, sz->dims, sz->rnk);
     return x;
}

/* likefftwf_tensor_copy), but makes strides in-place by
   setting os = is if k == INPLACE_IS or is = os if k == INPLACE_OS. */
tensor *fftwf_tensor_copy_inplace(const tensor *sz, inplace_kind k)
{
     tensor *x =fftwf_tensor_copy(sz);
     if (FINITE_RNK(x->rnk)) {
	  int i;
	  if (k == INPLACE_OS)
	       for (i = 0; i < x->rnk; ++i)
		    x->dims[i].is = x->dims[i].os;
	  else
	       for (i = 0; i < x->rnk; ++i)
		    x->dims[i].os = x->dims[i].is;
     }
     return x;
}

/* Likefftwf_tensor_copy), but copy all of the dimensions *except*
   except_dim. */
tensor *fftwf_tensor_copy_except(const tensor *sz, int except_dim)
{
     tensor *x;

     A(FINITE_RNK(sz->rnk) && sz->rnk >= 1 && except_dim < sz->rnk);
     x = fftwf_mktensor(sz->rnk - 1);
     dimcpy(x->dims, sz->dims, except_dim);
     dimcpy(x->dims + except_dim, sz->dims + except_dim + 1,
            x->rnk - except_dim);
     return x;
}

/* Likefftwf_tensor_copy), but copy only rnk dimensions starting
   with start_dim. */
tensor *fftwf_tensor_copy_sub(const tensor *sz, int start_dim, int rnk)
{
     tensor *x;

     A(FINITE_RNK(sz->rnk) && start_dim + rnk <= sz->rnk);
     x = fftwf_mktensor(rnk);
     dimcpy(x->dims, sz->dims + start_dim, rnk);
     return x;
}

tensor *fftwf_tensor_append(const tensor *a, const tensor *b)
{
     if (!FINITE_RNK(a->rnk) || !FINITE_RNK(b->rnk)) {
          return fftwf_mktensor(RNK_MINFTY);
     } else {
	  tensor *x =fftwf_mktensor(a->rnk + b->rnk);
          dimcpy(x->dims, a->dims, a->rnk);
          dimcpy(x->dims + a->rnk, b->dims, b->rnk);
	  return x;
     }
}
