#include "rdft.h"


typedef struct {
     solver super;
} S;

typedef struct {
     plan_rdft2 super;

     plan *cld, *cldrest;
     INT n, vl, nbuf, bufdist;
     INT cs, ivs, ovs;
} P;

/***************************************************************************/

/* FIXME: have alternate copy functions that push a vector loop inside
   the n loops? */

/* copy halfcomplex array r (contiguous) to complex (strided) array rio/iio. */
static void hc2c(INT n, float *r, float *rio, float *iio, INT os)
{
     INT i;

     rio[0] = r[0];
     iio[0] = 0;

     for (i = 1; i + i < n; ++i) {
	  rio[i * os] = r[i];
	  iio[i * os] = r[n - i];
     }

     if (i + i == n) {	/* store the Nyquist frequency */
	  rio[i * os] = r[i];
	  iio[i * os] = K(0.0);
     }
}

/* reverse of hc2c */
static void c2hc(INT n, float *rio, float *iio, INT is, float *r)
{
     INT i;

     r[0] = rio[0];

     for (i = 1; i + i < n; ++i) {
	  r[i] = rio[i * is];
	  r[n - i] = iio[i * is];
     }

     if (i + i == n)		/* store the Nyquist frequency */
	  r[i] = rio[i * is];
}

/***************************************************************************/

static void apply_r2hc(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;
     plan_rdft *cld = (plan_rdft *) ego->cld;
     INT i, j, vl = ego->vl, nbuf = ego->nbuf, bufdist = ego->bufdist;
     INT n = ego->n;
     INT ivs = ego->ivs, ovs = ego->ovs, os = ego->cs;
     float *bufs = (float *)MALLOC(sizeof(float) * nbuf * bufdist, BUFFERS);
     plan_rdft2 *cldrest;

     for (i = nbuf; i <= vl; i += nbuf) {
          /* transform to bufs: */
          cld->apply((plan *) cld, r0, bufs);
	  r0 += ivs * nbuf; r1 += ivs * nbuf;

          /* copy back */
	  for (j = 0; j < nbuf; ++j, cr += ovs, ci += ovs)
	       hc2c(n, bufs + j*bufdist, cr, ci, os);
     }

     fftwf_ifree(bufs);

     /* Do the remaining transforms, if any: */
     cldrest = (plan_rdft2 *) ego->cldrest;
     cldrest->apply((plan *) cldrest, r0, r1, cr, ci);
}

static void apply_hc2r(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;
     plan_rdft *cld = (plan_rdft *) ego->cld;
     INT i, j, vl = ego->vl, nbuf = ego->nbuf, bufdist = ego->bufdist;
     INT n = ego->n;
     INT ivs = ego->ivs, ovs = ego->ovs, is = ego->cs;
     float *bufs = (float *)MALLOC(sizeof(float) * nbuf * bufdist, BUFFERS);
     plan_rdft2 *cldrest;

     for (i = nbuf; i <= vl; i += nbuf) {
          /* copy to bufs */
	  for (j = 0; j < nbuf; ++j, cr += ivs, ci += ivs)
	       c2hc(n, cr, ci, is, bufs + j*bufdist);

          /* transform back: */
          cld->apply((plan *) cld, bufs, r0);
	  r0 += ovs * nbuf; r1 += ovs * nbuf;
     }

     fftwf_ifree(bufs);

     /* Do the remaining transforms, if any: */
     cldrest = (plan_rdft2 *) ego->cldrest;
     cldrest->apply((plan *) cldrest, r0, r1, cr, ci);
}

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;

     fftwf_plan_awake(ego->cld, wakefulness);
     fftwf_plan_awake(ego->cldrest, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cldrest);
     fftwf_plan_destroy_internal(ego->cld);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     p->print(p, "(rdft2-rdft-%s-%D%v/%D-%D%(%p%)%(%p%))",
	      ego->super.apply == apply_r2hc ? "r2hc" : "hc2r",
              ego->n, ego->nbuf,
              ego->vl, ego->bufdist % ego->n,
              ego->cld, ego->cldrest);
}

static INT min_nbuf(const problem_rdft2 *p, INT n, INT vl)
{
     INT is, os, ivs, ovs;

     if (p->r0 != p->cr)
	  return 1;
     if (fftwf_rdft2_inplace_strides(p, RNK_MINFTY))
	  return 1;
     A(p->vecsz->rnk == 1); /*  rank 0 and MINFTY are inplace */

     fftwf_rdft2_strides(p->kind, p->sz->dims, &is, &os);
     fftwf_rdft2_strides(p->kind, p->vecsz->dims, &ivs, &ovs);

     /* handle one potentially common case: "contiguous" real and
	complex arrays, which overlap because of the differing sizes. */
     if (n * fftwf_iabs(is) <= fftwf_iabs(ivs)
	 && (n/2 + 1) * fftwf_iabs(os) <= fftwf_iabs(ovs)
	 && ( ((p->cr - p->ci) <= fftwf_iabs(os)) ||
	      ((p->ci - p->cr) <= fftwf_iabs(os)) )
	 && ivs > 0 && ovs > 0) {
	  INT vsmin = fftwf_imin(ivs, ovs);
	  INT vsmax = fftwf_imax(ivs, ovs);
	  return(((vsmax - vsmin) * vl + vsmin - 1) / vsmin);
     }

     return vl; /* punt: just buffer the whole vector */
}

static int applicable0(const problem *p_, const S *ego, const planner *plnr)
{
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     UNUSED(ego);
     return(1
	    && p->vecsz->rnk <= 1
	    && p->sz->rnk == 1

	    /* FIXME: does it make sense to do R2HCII ? */
	    && (p->kind == R2HC || p->kind == HC2R)

	    /* real strides must allow for reduction to rdft */
	    && (2 * (p->r1 - p->r0) ==
		(((p->kind == R2HC) ? p->sz->dims[0].is : p->sz->dims[0].os)))

	    && !(fftwf_toobig(p->sz->dims[0].n) && CONSERVE_MEMORYP(plnr))
	  );
}

static int applicable(const problem *p_, const S *ego, const planner *plnr)
{
     const problem_rdft2 *p;

     if (NO_BUFFERINGP(plnr)) return 0;

     if (!applicable0(p_, ego, plnr)) return 0;

     p = (const problem_rdft2 *) p_;
     if (NO_UGLYP(plnr)) {
	  if (p->r0 != p->cr) return 0;
	  if (fftwf_toobig(p->sz->dims[0].n)) return 0;
     }
     return 1;
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     P *pln;
     plan *cld = (plan *) 0;
     plan *cldrest = (plan *) 0;
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     float *bufs = (float *) 0;
     INT nbuf = 0, bufdist, n, vl;
     INT ivs, ovs, rs, id, od;

     static const plan_adt padt = {
	  fftwf_rdft2_solve, awake, print, destroy
     };

     if (!applicable(p_, ego, plnr))
          goto nada;

     n = p->sz->dims[0].n;
     fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs);

     nbuf = fftwf_imax(fftwf_nbuf(n, vl, 0), min_nbuf(p, n, vl));
     bufdist = fftwf_bufdist(n, vl);
     A(nbuf > 0);

     /* initial allocation for the purpose of planning */
     bufs = (float *) MALLOC(sizeof(float) * nbuf * bufdist, BUFFERS);

     id = ivs * (nbuf * (vl / nbuf));
     od = ovs * (nbuf * (vl / nbuf));

     if (p->kind == R2HC) {
	  cld = fftwf_mkplan_f_d(
	       plnr,
	       fftwf_mkproblem_rdft_d(
		    fftwf_mktensor_1d(n, p->sz->dims[0].is/2, 1),
		    fftwf_mktensor_1d(nbuf, ivs, bufdist),
		    TAINT(p->r0, ivs * nbuf), bufs, &p->kind),
	       0, 0, (p->r0 == p->cr) ? NO_DESTROY_INPUT : 0);
	  if (!cld) goto nada;
	  fftwf_ifree(bufs); bufs = 0;

	  cldrest = fftwf_mkplan_d(plnr,
				fftwf_mkproblem_rdft2_d(
				     fftwf_tensor_copy(p->sz),
				     fftwf_mktensor_1d(vl % nbuf, ivs, ovs),
				     p->r0 + id, p->r1 + id,
				     p->cr + od, p->ci + od,
				     p->kind));
	  if (!cldrest) goto nada;

	  pln = MKPLAN_RDFT2(P, &padt, apply_r2hc);
     } else {
	  A(p->kind == HC2R);
	  cld = fftwf_mkplan_f_d(
	       plnr,
	       fftwf_mkproblem_rdft_d(
		    fftwf_mktensor_1d(n, 1, p->sz->dims[0].os/2),
		    fftwf_mktensor_1d(nbuf, bufdist, ovs),
		    bufs, TAINT(p->r0, ovs * nbuf), &p->kind),
	       0, 0, NO_DESTROY_INPUT); /* always ok to destroy bufs */
	  if (!cld) goto nada;
	  fftwf_ifree(bufs); bufs = 0;

	  cldrest = fftwf_mkplan_d(plnr,
				fftwf_mkproblem_rdft2_d(
				     fftwf_tensor_copy(p->sz),
				     fftwf_mktensor_1d(vl % nbuf, ivs, ovs),
				     p->r0 + od, p->r1 + od,
				     p->cr + id, p->ci + id,
				     p->kind));
	  if (!cldrest) goto nada;
	  pln = MKPLAN_RDFT2(P, &padt, apply_hc2r);
     }

     pln->cld = cld;
     pln->cldrest = cldrest;
     pln->n = n;
     pln->vl = vl;
     pln->ivs = ivs;
     pln->ovs = ovs;
     fftwf_rdft2_strides(p->kind, &p->sz->dims[0], &rs, &pln->cs);
     pln->nbuf = nbuf;
     pln->bufdist = bufdist;

     fftwf_ops_madd(vl / nbuf, &cld->ops, &cldrest->ops,
		 &pln->super.super.ops);
     pln->super.super.ops.other += (p->kind == R2HC ? (n + 2) : n) * vl;

     return &(pln->super.super);

 nada:
     fftwf_ifree0(bufs);
     fftwf_plan_destroy_internal(cldrest);
     fftwf_plan_destroy_internal(cld);
     return (plan *) 0;
}

static solver *mksolver(void)
{
     static const solver_adt sadt = { PROBLEM_RDFT2, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     return &(slv->super);
}

void fftwf_rdft2_rdft_register(planner *p)
{
     REGISTER_SOLVER(p, mksolver());
}
