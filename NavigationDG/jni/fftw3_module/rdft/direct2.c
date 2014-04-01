#include "rdft.h"

typedef struct {
     solver super;
     const kr2c_desc *desc;
     kr2c k;
} S;

typedef struct {
     plan_rdft2 super;

     stride rs, cs;
     INT vl;
     INT ivs, ovs;
     kr2c k;
     const S *slv;
     INT ilast;
} P;

static void apply(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;
     ASSERT_ALIGNED_DOUBLE;
     ego->k(r0, r1, cr, ci,
	    ego->rs, ego->cs, ego->cs,
	    ego->vl, ego->ivs, ego->ovs);
}

static void apply_r2hc(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;
     INT i, vl = ego->vl, ovs = ego->ovs;
     ASSERT_ALIGNED_DOUBLE;
     ego->k(r0, r1, cr, ci,
	    ego->rs, ego->cs, ego->cs,
	    vl, ego->ivs, ovs);
     for (i = 0; i < vl; ++i, ci += ovs)
	  ci[0] = ci[ego->ilast] = 0;
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_stride_destroy(ego->rs);
     fftwf_stride_destroy(ego->cs);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     const S *s = ego->slv;

     p->print(p, "(rdft2-%s-direct-%D%v \"%s\")",
	      fftwf_rdft_kind_str(s->desc->genus->kind), s->desc->n,
	      ego->vl, s->desc->nam);
}

static int applicable(const solver *ego_, const problem *p_)
{
     const S *ego = (const S *) ego_;
     const kr2c_desc *desc = ego->desc;
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     INT vl;
     INT ivs, ovs;

     return (
	  1
	  && p->sz->rnk == 1
	  && p->vecsz->rnk <= 1
	  && p->sz->dims[0].n == desc->n
	  && p->kind == desc->genus->kind

	  /* check strides etc */
	  && fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs)

	  && (0
	      /* can operate out-of-place */
	      || p->r0 != p->cr

	      /*
	       * can compute one transform in-place, no matter
	       * what the strides are.
	       */
	      || p->vecsz->rnk == 0

	      /* can operate in-place as long as strides are the same */
	      || fftwf_rdft2_inplace_strides(p, RNK_MINFTY)
	       )
	  );
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     P *pln;
     const problem_rdft2 *p;
     iodim *d;
     int r2hc_kindp;

     static const plan_adt padt = {
	  fftwf_rdft2_solve, fftwf_null_awake, print, destroy
     };

     UNUSED(plnr);

     if (!applicable(ego_, p_))
          return (plan *)0;

     p = (const problem_rdft2 *) p_;

     r2hc_kindp = R2HC_KINDP(p->kind);
     A(r2hc_kindp || HC2R_KINDP(p->kind));

     pln = MKPLAN_RDFT2(P, &padt, p->kind == R2HC ? apply_r2hc : apply);

     d = p->sz->dims;

     pln->k = ego->k;

     pln->rs = fftwf_mkstride(d->n, r2hc_kindp ? d->is : d->os);
     pln->cs = fftwf_mkstride(d->n, r2hc_kindp ? d->os : d->is);

     fftwf_tensor_tornk1(p->vecsz, &pln->vl, &pln->ivs, &pln->ovs);

     /* Nyquist freq., if any */
     pln->ilast = (d->n % 2) ? 0 : (d->n/2) * d->os;

     pln->slv = ego;
     fftwf_ops_zero(&pln->super.super.ops);
     fftwf_ops_madd2(pln->vl / ego->desc->genus->vl,
		  &ego->desc->ops,
		  &pln->super.super.ops);
     if (p->kind == R2HC)
	  pln->super.super.ops.other += 2 * pln->vl; /* + 2 stores */

     pln->super.super.could_prune_now_p = 1;
     return &(pln->super.super);
}

/* constructor */
solver *fftwf_mksolver_rdft2_direct(kr2c k, const kr2c_desc *desc)
{
     static const solver_adt sadt = { PROBLEM_RDFT2, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->k = k;
     slv->desc = desc;
     return &(slv->super);
}