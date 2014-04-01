

#include "dft.h"

typedef struct {
     solver super;
     int vecloop_dim;
     const int *buddies;
     int nbuddies;
} S;

typedef struct {
     plan_dft super;

     plan *cld;
     INT vl;
     INT ivs, ovs;
     const S *solver;
} P;

static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io)
{
     const P *ego = (const P *) ego_;
     INT i, vl = ego->vl;
     INT ivs = ego->ivs, ovs = ego->ovs;
     dftapply cldapply = ((plan_dft *) ego->cld)->apply;

     for (i = 0; i < vl; ++i) {
          cldapply(ego->cld,
                   ri + i * ivs, ii + i * ivs, ro + i * ovs, io + i * ovs);
     }
}

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;
     fftwf_plan_awake(ego->cld, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cld);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     const S *s = ego->solver;
     p->print(p, "(dft-vrank>=1-x%D/%d%(%p%))",
 	      ego->vl, s->vecloop_dim, ego->cld);
}

static int pickdim(const S *ego, const tensor *vecsz, int oop, int *dp)
{
     return fftwf_pickdim(ego->vecloop_dim, ego->buddies, ego->nbuddies,
		       vecsz, oop, dp);
}

static int applicable0(const solver *ego_, const problem *p_, int *dp)
{
     const S *ego = (const S *) ego_;
     const problem_dft *p = (const problem_dft *) p_;

     return (1
	     && FINITE_RNK(p->vecsz->rnk)
	     && p->vecsz->rnk > 0

	     /* do not bother looping over rank-0 problems,
		since they are handled via rdft */
	     && p->sz->rnk > 0

	     && pickdim(ego, p->vecsz, p->ri != p->ro, dp)
	  );
}

static int applicable(const solver *ego_, const problem *p_,
		      const planner *plnr, int *dp)
{
     const S *ego = (const S *)ego_;
     const problem_dft *p;

     if (!applicable0(ego_, p_, dp)) return 0;

     /* fftw2 behavior */
     if (NO_VRANK_SPLITSP(plnr) && (ego->vecloop_dim != ego->buddies[0]))
	  return 0;

     p = (const problem_dft *) p_;

     if (NO_UGLYP(plnr)) {
	  /* Heuristic: if the transform is multi-dimensional, and the
	     vector stride is less than the transform size, then we
	     probably want to use a rank>=2 plan first in order to combine
	     this vector with the transform-dimension vectors. */
	  {
	       iodim *d = p->vecsz->dims + *dp;
	       if (1
		   && p->sz->rnk > 1
		   && fftwf_imin(fftwf_iabs(d->is), fftwf_iabs(d->os))
		   < fftwf_tensor_max_index(p->sz)
		    )
		    return 0;
	  }

	  if (NO_NONTHREADEDP(plnr)) return 0; /* prefer threaded version */
     }

     return 1;
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     const problem_dft *p;
     P *pln;
     plan *cld;
     int vdim;
     iodim *d;

     static const plan_adt padt = {
	  fftwf_dft_solve, awake, print, destroy
     };

     if (!applicable(ego_, p_, plnr, &vdim))
          return (plan *) 0;
     p = (const problem_dft *) p_;

     d = p->vecsz->dims + vdim;

     A(d->n > 1);
     cld = fftwf_mkplan_d(plnr,
		       fftwf_mkproblem_dft_d(
			    fftwf_tensor_copy(p->sz),
			    fftwf_tensor_copy_except(p->vecsz, vdim),
			    TAINT(p->ri, d->is), TAINT(p->ii, d->is),
			    TAINT(p->ro, d->os), TAINT(p->io, d->os)));
     if (!cld) return (plan *) 0;

     pln = MKPLAN_DFT(P, &padt, apply);

     pln->cld = cld;
     pln->vl = d->n;
     pln->ivs = d->is;
     pln->ovs = d->os;

     pln->solver = ego;
     fftwf_ops_zero(&pln->super.super.ops);
     pln->super.super.ops.other = 3.14159; /* magic to prefer codelet loops */
     fftwf_ops_madd2(pln->vl, &cld->ops, &pln->super.super.ops);

     if (p->sz->rnk != 1 || (p->sz->dims[0].n > 64))
	  pln->super.super.pcost = pln->vl * cld->pcost;

     return &(pln->super.super);
}

static solver *mksolver(int vecloop_dim, const int *buddies, int nbuddies)
{
     static const solver_adt sadt = { PROBLEM_DFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->vecloop_dim = vecloop_dim;
     slv->buddies = buddies;
     slv->nbuddies = nbuddies;
     return &(slv->super);
}

void fftwf_dft_vrank_geq1_register(planner *p)
{
     int i;

     /* FIXME: Should we try other vecloop_dim values? */
     static const int buddies[] = { 1, -1 };

     const int nbuddies = (int)(sizeof(buddies) / sizeof(buddies[0]));

     for (i = 0; i < nbuddies; ++i)
          REGISTER_SOLVER(p, mksolver(buddies[i], buddies, nbuddies));
}
