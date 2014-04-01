#include "rdft.h"

typedef struct {
     solver super;
     const kr2r_desc *desc;
     kr2r k;
} S;

typedef struct {
     plan_rdft super;

     INT vl, ivs, ovs;
     stride is, os;
     kr2r k;
     const S *slv;
} P;

static void apply(const plan *ego_, float *I, float *O)
{
     const P *ego = (const P *) ego_;
     ASSERT_ALIGNED_DOUBLE;
     ego->k(I, O, ego->is, ego->os, ego->vl, ego->ivs, ego->ovs);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_stride_destroy(ego->is);
     fftwf_stride_destroy(ego->os);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     const S *s = ego->slv;

     p->print(p, "(rdft-%s-direct-r2r-%D%v \"%s\")",
	      fftwf_rdft_kind_str(s->desc->kind), s->desc->n,
	      ego->vl, s->desc->nam);
}

static int applicable(const solver *ego_, const problem *p_)
{
     const S *ego = (const S *) ego_;
     const problem_rdft *p = (const problem_rdft *) p_;
     INT vl;
     INT ivs, ovs;

     return (
	  1
	  && p->sz->rnk == 1
	  && p->vecsz->rnk <= 1
	  && p->sz->dims[0].n == ego->desc->n
	  && p->kind[0] == ego->desc->kind

	  /* check strides etc */
	  && fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs)

	  && (0
	      /* can operate out-of-place */
	      || p->I != p->O

	      /* computing one transform */
	      || vl == 1

	      /* can operate in-place as long as strides are the same */
	      || fftwf_tensor_inplace_strides2(p->sz, p->vecsz)
	       )
	  );
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     P *pln;
     const problem_rdft *p;
     iodim *d;

     static const plan_adt padt = {
	  fftwf_rdft_solve, fftwf_null_awake, print, destroy
     };

     UNUSED(plnr);

     if (!applicable(ego_, p_))
          return (plan *)0;

     p = (const problem_rdft *) p_;


     pln = MKPLAN_RDFT(P, &padt, apply);

     d = p->sz->dims;

     pln->k = ego->k;

     pln->is = fftwf_mkstride(d->n, d->is);
     pln->os = fftwf_mkstride(d->n, d->os);

     fftwf_tensor_tornk1(p->vecsz, &pln->vl, &pln->ivs, &pln->ovs);

     pln->slv = ego;
     fftwf_ops_zero(&pln->super.super.ops);
     fftwf_ops_madd2(pln->vl / ego->desc->genus->vl,
		  &ego->desc->ops,
		  &pln->super.super.ops);

     pln->super.super.could_prune_now_p = 1;

     return &(pln->super.super);
}

/* constructor */
solver *fftwf_mksolver_rdft_r2r_direct(kr2r k, const kr2r_desc *desc)
{
     static const solver_adt sadt = { PROBLEM_RDFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->k = k;
     slv->desc = desc;
     return &(slv->super);
}
