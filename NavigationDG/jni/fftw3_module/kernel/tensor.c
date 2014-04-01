


#include "ifftw.h"

tensor *fftwf_mktensor (int rnk) 
{
     tensor *x;

     A(rnk >= 0);

#if defined(STRUCT_HACK_KR)
     if (FINITE_RNK(rnk) && rnk > 1)
	  x = (tensor *)MALLOC(sizeof(tensor) + (rnk - 1) * sizeof(iodim),
				    TENSORS);
     else
	  x = (tensor *)MALLOC(sizeof(tensor), TENSORS);
#elif defined(STRUCT_HACK_C99)
     if (FINITE_RNK(rnk))
	  x = (tensor *)MALLOC(sizeof(tensor) + rnk * sizeof(iodim),
				    TENSORS);
     else
	  x = (tensor *)MALLOC(sizeof(tensor), TENSORS);
#else
     x = (tensor *)MALLOC(sizeof(tensor), TENSORS);
     if (FINITE_RNK(rnk) && rnk > 0)
          x->dims = (iodim *)MALLOC(sizeof(iodim) * rnk, TENSORS);
     else
          x->dims = 0;
#endif

     x->rnk = rnk;
     return x;
}

void fftwf_tensor_destroy(tensor *sz)
{
#if !defined(STRUCT_HACK_C99) && !defined(STRUCT_HACK_KR)
     fftwf_ifree0(sz->dims);
#endif
     fftwf_ifree(sz);
}

INT fftwf_tensor_sz(const tensor *sz)
{
     int i;
     INT n = 1;

     if (!FINITE_RNK(sz->rnk))
          return 0;

     for (i = 0; i < sz->rnk; ++i)
          n *= sz->dims[i].n;
     return n;
}

void fftwf_tensor_md5(md5 *p, const tensor *t)
{
     int i;
     fftwf_md5int(p, t->rnk);
     if (FINITE_RNK(t->rnk)) {
	  for (i = 0; i < t->rnk; ++i) {
	       const iodim *q = t->dims + i;
	       fftwf_md5INT(p, q->n);
	       fftwf_md5INT(p, q->is);
	       fftwf_md5INT(p, q->os);
	  }
     }
}

/* treat a (rank <= 1)-tensor as a rank-1 tensor, extracting
   appropriate n, is, and os components */
int fftwf_tensor_tornk1(const tensor *t, INT *n, INT *is, INT *os)
{
     A(t->rnk <= 1);
     if (t->rnk == 1) {
	  const iodim *vd = t->dims;
          *n = vd[0].n;
          *is = vd[0].is;
          *os = vd[0].os;
     } else {
          *n = 1;
          *is = *os = 0;
     }
     return 1;
}

void fftwf_tensor_print(const tensor *x, printer *p)
{
     if (FINITE_RNK(x->rnk)) {
	  int i;
	  int first = 1;
	  p->print(p, "(");
	  for (i = 0; i < x->rnk; ++i) {
	       const iodim *d = x->dims + i;
	       p->print(p, "%s(%D %D %D)", 
			first ? "" : " ",
			d->n, d->is, d->os);
	       first = 0;
	  }
	  p->print(p, ")");
     } else {
	  p->print(p, "rank-minfty"); 
     }
}
