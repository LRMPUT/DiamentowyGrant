


#include "dft.h"

typedef struct {
     solver super;
     int maxnbuf_ndx;
} S;

static const INT maxnbufs[] = { 8, 256 };

typedef struct {
     plan_dft super;

     plan *cld, *cldcpy, *cldrest;
     INT n, vl, nbuf, bufdist;
     INT ivs_by_nbuf, ovs_by_nbuf;
     INT roffset, ioffset;
} P;

/* transform a vector input with the help of bufs */
static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io)
{
     const P *ego = (const P *) ego_;
     INT nbuf = ego->nbuf;
     float *bufs = (float *)MALLOC(sizeof(float) * nbuf * ego->bufdist * 2, BUFFERS);

     plan_dft *cld = (plan_dft *) ego->cld;
     plan_dft *cldcpy = (plan_dft *) ego->cldcpy;
     plan_dft *cldrest;
     INT i, vl = ego->vl;
     INT ivs_by_nbuf = ego->ivs_by_nbuf, ovs_by_nbuf = ego->ovs_by_nbuf;
     INT roffset = ego->roffset, ioffset = ego->ioffset;

     for (i = nbuf; i <= vl; i += nbuf) {
          /* transform to bufs: */
          cld->apply((plan *) cld, ri, ii, bufs + roffset, bufs + ioffset);
	  ri += ivs_by_nbuf; ii += ivs_by_nbuf;

          /* copy back */
          cldcpy->apply((plan *) cldcpy, bufs+roffset, bufs+ioffset, ro, io);
	  ro += ovs_by_nbuf; io += ovs_by_nbuf;
     }

     fftwf_ifree(bufs);

     /* Do the remaining transforms, if any: */
     cldrest = (plan_dft *) ego->cldrest;
     cldrest->apply((plan *) cldrest, ri, ii, ro, io);
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
     p->print(p, "(dft-buffered-%D%v/%D-%D%(%p%)%(%p%)%(%p%))",
              ego->n, ego->nbuf,
              ego->vl, ego->bufdist % ego->n,
              ego->cld, ego->cldcpy, ego->cldrest);
}

static int applicable0(const S *ego, const problem *p_, const planner *plnr)
{
     const problem_dft *p = (const problem_dft *) p_;
     const iodim *d = p->sz->dims;

     if (1
	 && p->vecsz->rnk <= 1
	 && p->sz->rnk == 1
	  ) {
	  INT vl, ivs, ovs;
	  fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs);

	  if (fftwf_toobig(p->sz->dims[0].n) && CONSERVE_MEMORYP(plnr))
	       return 0;

	  /* if this solver is redundant, in the sense that a solver
	     of lower index generates the same plan, then prune this
	     solver */
	  if (fftwf_nbuf_redundant(d[0].n, vl,
				ego->maxnbuf_ndx,
				maxnbufs, NELEM(maxnbufs)))
	       return 0;

	  /*
	    In principle, the buffered transforms might be useful
	    when working out of place.  However, in order to
	    prevent infinite loops in the planner, we require
	    that the output stride of the buffered transforms be
	    greater than 2.
	  */
	  if (p->ri != p->ro)
	       return (d[0].os > 2);

	  /*
	   * If the problem is in place, the input/output strides must
	   * be the same or the whole thing must fit in the buffer.
	   */
	  if (fftwf_tensor_inplace_strides2(p->sz, p->vecsz))
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
     if (NO_BUFFERINGP(plnr)) return 0;
     if (!applicable0(ego, p_, plnr)) return 0;

     if (NO_UGLYP(plnr)) {
	  const problem_dft *p = (const problem_dft *) p_;
	  if (p->ri != p->ro) return 0;
	  if (fftwf_toobig(p->sz->dims[0].n)) return 0;
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
     const problem_dft *p = (const problem_dft *) p_;
     float *bufs = (float *) 0;
     INT nbuf = 0, bufdist, n, vl;
     INT ivs, ovs, roffset, ioffset;

     static const plan_adt padt = {
	  fftwf_dft_solve, awake, print, destroy
     };

     if (!applicable(ego, p_, plnr))
          goto nada;

     n = fftwf_tensor_sz(p->sz);

     fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs);

     nbuf = fftwf_nbuf(n, vl, maxnbufs[ego->maxnbuf_ndx]);
     bufdist = fftwf_bufdist(n, vl);
     A(nbuf > 0);

     /* attempt to keep real and imaginary part in the same order,
	so as to allow optimizations in the the copy plan */
     roffset = (p->ri - p->ii > 0) ? (INT)1 : (INT)0;
     ioffset = 1 - roffset;

     /* initial allocation for the purpose of planning */
     bufs = (float *) MALLOC(sizeof(float) * nbuf * bufdist * 2, BUFFERS);

     /* allow destruction of input if problem is in place */
     cld = fftwf_mkplan_f_d(plnr,
			 fftwf_mkproblem_dft_d(
			      fftwf_mktensor_1d(n, p->sz->dims[0].is, 2),
			      fftwf_mktensor_1d(nbuf, ivs, bufdist * 2),
			      TAINT(p->ri, ivs * nbuf),
			      TAINT(p->ii, ivs * nbuf),
			      bufs + roffset,
			      bufs + ioffset),
			 0, 0, (p->ri == p->ro) ? NO_DESTROY_INPUT : 0);
     if (!cld)
          goto nada;

     /* copying back from the buffer is a rank-0 transform: */
     cldcpy = fftwf_mkplan_d(plnr,
			  fftwf_mkproblem_dft_d(
			       fftwf_mktensor_0d(),
			       fftwf_mktensor_2d(nbuf, bufdist * 2, ovs,
					      n, 2, p->sz->dims[0].os),
			       bufs + roffset,
			       bufs + ioffset,
			       TAINT(p->ro, ovs * nbuf),
			       TAINT(p->io, ovs * nbuf)));
     if (!cldcpy)
          goto nada;

     /* deallocate buffers, let apply() allocate them for real */
     fftwf_ifree(bufs);
     bufs = 0;

     /* plan the leftover transforms (cldrest): */
     {
	  INT id = ivs * (nbuf * (vl / nbuf));
	  INT od = ovs * (nbuf * (vl / nbuf));
	  cldrest = fftwf_mkplan_d(plnr,
				fftwf_mkproblem_dft_d(
				     fftwf_tensor_copy(p->sz),
				     fftwf_mktensor_1d(vl % nbuf, ivs, ovs),
				     p->ri+id, p->ii+id, p->ro+od, p->io+od));
     }
     if (!cldrest)
          goto nada;

     pln = MKPLAN_DFT(P, &padt, apply);
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
     static const solver_adt sadt = { PROBLEM_DFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->maxnbuf_ndx = maxnbuf_ndx;
     return &(slv->super);
}

void fftwf_dft_buffered_register(planner *p)
{
     size_t i;
     for (i = 0; i < NELEM(maxnbufs); ++i)
	  REGISTER_SOLVER(p, mksolver(i));
}
