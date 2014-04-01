#include "rdft.h"
#include "../dft/dft.h"

typedef struct {
     solver super;
     int maxnbuf_ndx;
} S;

static const INT maxnbufs[] = { 8, 256 };

typedef struct {
     plan_rdft2 super;

     plan *cld, *cldcpy, *cldrest;
     INT n, vl, nbuf, bufdist;
     INT ivs_by_nbuf, ovs_by_nbuf;
     INT ioffset, roffset;
} P;

/* transform a vector input with the help of bufs */
static void apply_r2hc(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;
     plan_rdft2 *cld = (plan_rdft2 *) ego->cld;
     plan_dft *cldcpy = (plan_dft *) ego->cldcpy;
     INT i, vl = ego->vl, nbuf = ego->nbuf;
     INT ivs_by_nbuf = ego->ivs_by_nbuf, ovs_by_nbuf = ego->ovs_by_nbuf;
     float *bufs = (float *)MALLOC(sizeof(float) * nbuf * ego->bufdist, BUFFERS);
     float *bufr = bufs + ego->roffset;
     float *bufi = bufs + ego->ioffset;
     plan_rdft2 *cldrest;

     for (i = nbuf; i <= vl; i += nbuf) {
          /* transform to bufs: */
          cld->apply((plan *) cld, r0, r1, bufr, bufi);
	  r0 += ivs_by_nbuf; r1 += ivs_by_nbuf;

          /* copy back */
          cldcpy->apply((plan *) cldcpy, bufr, bufi, cr, ci);
	  cr += ovs_by_nbuf; ci += ovs_by_nbuf;
     }

     fftwf_ifree(bufs);

     /* Do the remaining transforms, if any: */
     cldrest = (plan_rdft2 *) ego->cldrest;
     cldrest->apply((plan *) cldrest, r0, r1, cr, ci);
}

/* for hc2r problems, copy the input into buffer, and then
   transform buffer->output, which allows for destruction of the
   buffer */
static void apply_hc2r(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;
     plan_rdft2 *cld = (plan_rdft2 *) ego->cld;
     plan_dft *cldcpy = (plan_dft *) ego->cldcpy;
     INT i, vl = ego->vl, nbuf = ego->nbuf;
     INT ivs_by_nbuf = ego->ivs_by_nbuf, ovs_by_nbuf = ego->ovs_by_nbuf;
     float *bufs = (float *)MALLOC(sizeof(float) * nbuf * ego->bufdist, BUFFERS);
     float *bufr = bufs + ego->roffset;
     float *bufi = bufs + ego->ioffset;
     plan_rdft2 *cldrest;

     for (i = nbuf; i <= vl; i += nbuf) {
          /* copy input into bufs: */
          cldcpy->apply((plan *) cldcpy, cr, ci, bufr, bufi);
	  cr += ivs_by_nbuf; ci += ivs_by_nbuf;

          /* transform to output */
          cld->apply((plan *) cld, r0, r1, bufr, bufi);
	  r0 += ovs_by_nbuf; r1 += ovs_by_nbuf;
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
     fftwf_plan_awake(ego->cldcpy, wakefulness);
     fftwf_plan_awake(ego->cldrest, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cldrest);
     fftwf_plan_destroy_internal(ego->cldcpy);
     fftwf_plan_destroy_internal(ego->cld);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     p->print(p, "(rdft2-buffered-%D%v/%D-%D%(%p%)%(%p%)%(%p%))",
              ego->n, ego->nbuf,
              ego->vl, ego->bufdist % ego->n,
              ego->cld, ego->cldcpy, ego->cldrest);
}

static int applicable0(const S *ego, const problem *p_, const planner *plnr)
{
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     iodim *d = p->sz->dims;

     if (1
	 && p->vecsz->rnk <= 1
	 && p->sz->rnk == 1

	 /* we assume even n throughout */
	 && (d[0].n % 2) == 0

	 /* and we only consider these two cases */
	 && (p->kind == R2HC || p->kind == HC2R)

	  ) {
	  INT vl, ivs, ovs;
	  fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs);

	  if (fftwf_toobig(d[0].n) && CONSERVE_MEMORYP(plnr))
	       return 0;

	  /* if this solver is redundant, in the sense that a solver
	     of lower index generates the same plan, then prune this
	     solver */
	  if (fftwf_nbuf_redundant(d[0].n, vl,
				ego->maxnbuf_ndx,
				maxnbufs, NELEM(maxnbufs)))
	       return 0;

	  if (p->r0 != p->cr) {
	       if (p->kind == HC2R) {
		    /* Allow HC2R problems only if the input is to be
		       preserved.  This solver sets NO_DESTROY_INPUT,
		       which prevents infinite loops */
		    return (NO_DESTROY_INPUTP(plnr));
	       } else {
		    /*
		      In principle, the buffered transforms might be useful
		      when working out of place.  However, in order to
		      prevent infinite loops in the planner, we require
		      that the output stride of the buffered transforms be
		      greater than 2.
		    */
		    return (d[0].os > 2);
	       }
	  }

	  /*
	   * If the problem is in place, the input/output strides must
	   * be the same or the whole thing must fit in the buffer.
	   */
	  if (fftwf_rdft2_inplace_strides(p, RNK_MINFTY))
	       return 1;

	  if (/* fits into buffer: */
	       ((p->vecsz->rnk == 0)
		||
		(fftwf_nbuf(d[0].n, p->vecsz->dims[0].n,
			 maxnbufs[ego->maxnbuf_ndx])
		 == p->vecsz->dims[0].n)))
	       return 1;
     }

     return 0;
}

static int applicable(const S *ego, const problem *p_, const planner *plnr)
{
     const problem_rdft2 *p;

     if (NO_BUFFERINGP(plnr)) return 0;

     if (!applicable0(ego, p_, plnr)) return 0;

     p = (const problem_rdft2 *) p_;
     if (p->kind == HC2R) {
	  if (NO_UGLYP(plnr)) {
	       /* UGLY if in-place and too big, since the problem
		  could be solved via transpositions */
	       if (p->r0 == p->cr && fftwf_toobig(p->sz->dims[0].n))
		    return 0;
	  }
     } else {
	  if (NO_UGLYP(plnr)) {
	       if (p->r0 != p->cr || fftwf_toobig(p->sz->dims[0].n))
		    return 0;
	  }
     }
     return 1;
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     P *pln;
     const S *ego = (const S *)ego_;
     plan *cld = (plan *) 0;
     plan *cldcpy = (plan *) 0;
     plan *cldrest = (plan *) 0;
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     float *bufs = (float *) 0;
     INT nbuf = 0, bufdist, n, vl;
     INT ivs, ovs, ioffset, roffset, id, od;

     static const plan_adt padt = {
	  fftwf_rdft2_solve, awake, print, destroy
     };

     if (!applicable(ego, p_, plnr))
          goto nada;

     n = fftwf_tensor_sz(p->sz);
     fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs);

     nbuf = fftwf_nbuf(n, vl, maxnbufs[ego->maxnbuf_ndx]);
     bufdist = fftwf_bufdist(n + 2, vl); /* complex-side rdft2 stores N+2
					 real numbers */
     A(nbuf > 0);

     /* attempt to keep real and imaginary part in the same order,
	so as to allow optimizations in the the copy plan */
     roffset = (p->cr - p->ci > 0) ? (INT)1 : (INT)0;
     ioffset = 1 - roffset;

     /* initial allocation for the purpose of planning */
     bufs = (float *) MALLOC(sizeof(float) * nbuf * bufdist, BUFFERS);

     id = ivs * (nbuf * (vl / nbuf));
     od = ovs * (nbuf * (vl / nbuf));

     if (p->kind == R2HC) {
	  /* allow destruction of input if problem is in place */
	  cld = fftwf_mkplan_f_d(
	       plnr,
	       fftwf_mkproblem_rdft2_d(
		    fftwf_mktensor_1d(n, p->sz->dims[0].is, 2),
		    fftwf_mktensor_1d(nbuf, ivs, bufdist),
		    TAINT(p->r0, ivs * nbuf), TAINT(p->r1, ivs * nbuf),
		    bufs + roffset, bufs + ioffset, p->kind),
	       0, 0, (p->r0 == p->cr) ? NO_DESTROY_INPUT : 0);
	  if (!cld) goto nada;

	  /* copying back from the buffer is a rank-0 DFT: */
	  cldcpy = fftwf_mkplan_d(
	       plnr,
	       fftwf_mkproblem_dft_d(
		    fftwf_mktensor_0d(),
		    fftwf_mktensor_2d(nbuf, bufdist, ovs,
				   n/2+1, 2, p->sz->dims[0].os),
		    bufs + roffset, bufs + ioffset,
		    TAINT(p->cr, ovs * nbuf), TAINT(p->ci, ovs * nbuf) ));
	  if (!cldcpy) goto nada;

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
	  /* allow destruction of buffer */
	  cld = fftwf_mkplan_f_d(
	       plnr,
	       fftwf_mkproblem_rdft2_d(
		    fftwf_mktensor_1d(n, 2, p->sz->dims[0].os),
		    fftwf_mktensor_1d(nbuf, bufdist, ovs),
		    TAINT(p->r0, ovs * nbuf), TAINT(p->r1, ovs * nbuf),
		    bufs + roffset, bufs + ioffset, p->kind),
	       0, 0, NO_DESTROY_INPUT);
	  if (!cld) goto nada;

	  /* copying input into buffer is a rank-0 DFT: */
	  cldcpy = fftwf_mkplan_d(
	       plnr,
	       fftwf_mkproblem_dft_d(
		    fftwf_mktensor_0d(),
		    fftwf_mktensor_2d(nbuf, ivs, bufdist,
				   n/2+1, p->sz->dims[0].is, 2),
		    TAINT(p->cr, ivs * nbuf), TAINT(p->ci, ivs * nbuf),
		    bufs + roffset, bufs + ioffset));
	  if (!cldcpy) goto nada;

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
     pln->cldcpy = cldcpy;
     pln->cldrest = cldrest;
     pln->n = n;
     pln->vl = vl;
     pln->ivs_by_nbuf = ivs * nbuf;
     pln->ovs_by_nbuf = ovs * nbuf;
     pln->roffset = roffset;
     pln->ioffset = ioffset;

     pln->nbuf = nbuf;
     pln->bufdist = bufdist;

     {
	  opcnt t;
	  fftwf_ops_add(&cld->ops, &cldcpy->ops, &t);
	  fftwf_ops_madd(vl / nbuf, &t, &cldrest->ops, &pln->super.super.ops);
     }

     return &(pln->super.super);

 nada:
     fftwf_ifree0(bufs);
     fftwf_plan_destroy_internal(cldrest);
     fftwf_plan_destroy_internal(cldcpy);
     fftwf_plan_destroy_internal(cld);
     return (plan *) 0;
}

static solver *mksolver(int maxnbuf_ndx)
{
     static const solver_adt sadt = { PROBLEM_RDFT2, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->maxnbuf_ndx = maxnbuf_ndx;
     return &(slv->super);
}

void fftwf_rdft2_buffered_register(planner *p)
{
     size_t i;
     for (i = 0; i < NELEM(maxnbufs); ++i)
	  REGISTER_SOLVER(p, mksolver(i));
}