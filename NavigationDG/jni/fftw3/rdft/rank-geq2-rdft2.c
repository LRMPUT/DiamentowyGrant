#include "rdft.h"
#include "../dft/dft.h"

typedef struct {
     solver super;
     int spltrnk;
     const int *buddies;
     int nbuddies;
} S;

typedef struct {
     plan_dft super;
     plan *cldr, *cldc;
     const S *solver;
} P;

static void apply_r2hc(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;

     {
	  plan_rdft2 *cldr = (plan_rdft2 *) ego->cldr;
	  cldr->apply((plan *) cldr, r0, r1, cr, ci);
     }

     {
	  plan_dft *cldc = (plan_dft *) ego->cldc;
	  cldc->apply((plan *) cldc, cr, ci, cr, ci);
     }
}

static void apply_hc2r(const plan *ego_, float *r0, float *r1, float *cr, float *ci)
{
     const P *ego = (const P *) ego_;

     {
	  plan_dft *cldc = (plan_dft *) ego->cldc;
	  cldc->apply((plan *) cldc, ci, cr, ci, cr);
     }

     {
	  plan_rdft2 *cldr = (plan_rdft2 *) ego->cldr;
	  cldr->apply((plan *) cldr, r0, r1, cr, ci);
     }

}

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;
     fftwf_plan_awake(ego->cldr, wakefulness);
     fftwf_plan_awake(ego->cldc, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cldr);
     fftwf_plan_destroy_internal(ego->cldc);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     const S *s = ego->solver;
     p->print(p, "(rdft2-rank>=2/%d%(%p%)%(%p%))",
	      s->spltrnk, ego->cldr, ego->cldc);
}

static int picksplit(const S *ego, const tensor *sz, int *rp)
{
     A(sz->rnk > 1); /* cannot split rnk <= 1 */
     if (!fftwf_pickdim(ego->spltrnk, ego->buddies, ego->nbuddies, sz, 1, rp))
          return 0;
     *rp += 1; /* convert from dim. index to rank */
     if (*rp >= sz->rnk) /* split must reduce rank */
          return 0;
     return 1;
}

static int applicable0(const solver *ego_, const problem *p_, int *rp,
		       const planner *plnr)
{
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     const S *ego = (const S *)ego_;
     return (1
	     && FINITE_RNK(p->sz->rnk) && FINITE_RNK(p->vecsz->rnk)

	     /* FIXME: multidimensional R2HCII ? */
	     && (p->kind == R2HC || p->kind == HC2R)

	     && p->sz->rnk >= 2
	     && picksplit(ego, p->sz, rp)
	     && (0

		 /* can work out-of-place, but HC2R destroys input */
		 || (p->r0 != p->cr &&
		     (p->kind == R2HC || !NO_DESTROY_INPUTP(plnr)))

		 /* FIXME: what are sufficient conditions for inplace? */
		 || (p->r0 == p->cr))
	  );
}

/* TODO: revise this. */
static int applicable(const solver *ego_, const problem *p_,
		      const planner *plnr, int *rp)
{
     const S *ego = (const S *)ego_;

     if (!applicable0(ego_, p_, rp, plnr)) return 0;

     if (NO_RANK_SPLITSP(plnr) && (ego->spltrnk != ego->buddies[0]))
          return 0;

     if (NO_UGLYP(plnr)) {
	  const problem_rdft2 *p = (const problem_rdft2 *) p_;

	  /* Heuristic: if the vector stride is greater than the transform
	     size, don't use (prefer to do the vector loop first with a
	     vrank-geq1 plan). */
	  if (p->vecsz->rnk > 0 &&
	      fftwf_tensor_min_stride(p->vecsz)
	      > fftwf_rdft2_tensor_max_index(p->sz, p->kind))
	       return 0;
     }

     return 1;
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     const problem_rdft2 *p;
     P *pln;
     plan *cldr = 0, *cldc = 0;
     tensor *sz1, *sz2, *vecszi, *sz2i;
     int spltrnk;
     inplace_kind k;
     problem *cldp;

     static const plan_adt padt = {
	  fftwf_rdft2_solve, awake, print, destroy
     };

     if (!applicable(ego_, p_, plnr, &spltrnk))
          return (plan *) 0;

     p = (const problem_rdft2 *) p_;
     fftwf_tensor_split(p->sz, &sz1, spltrnk, &sz2);

     k = p->kind == R2HC ? INPLACE_OS : INPLACE_IS;
     vecszi = fftwf_tensor_copy_inplace(p->vecsz, k);
     sz2i = fftwf_tensor_copy_inplace(sz2, k);

     /* complex data is ~half of real */
     sz2i->dims[sz2i->rnk - 1].n = sz2i->dims[sz2i->rnk - 1].n/2 + 1;

     cldr = fftwf_mkplan_d(plnr,
		       fftwf_mkproblem_rdft2_d(fftwf_tensor_copy(sz2),
					    fftwf_tensor_append(p->vecsz, sz1),
					    p->r0, p->r1,
					    p->cr, p->ci, p->kind));
     if (!cldr) goto nada;

     if (p->kind == R2HC)
	  cldp = fftwf_mkproblem_dft_d(fftwf_tensor_copy_inplace(sz1, k),
				    fftwf_tensor_append(vecszi, sz2i),
				    p->cr, p->ci, p->cr, p->ci);
     else /* HC2R must swap re/im parts to get IDFT */
	  cldp = fftwf_mkproblem_dft_d(fftwf_tensor_copy_inplace(sz1, k),
				    fftwf_tensor_append(vecszi, sz2i),
				    p->ci, p->cr, p->ci, p->cr);
     if (!cldc) goto nada;

     pln = MKPLAN_RDFT2(P, &padt, p->kind == R2HC ? apply_r2hc : apply_hc2r);

     pln->cldr = cldr;
     pln->cldc = cldc;

     pln->solver = ego;
     fftwf_ops_add(&cldr->ops, &cldc->ops, &pln->super.super.ops);

     fftwf_tensor_destroy4(sz2i, vecszi, sz2, sz1);

     return &(pln->super.super);

 nada:
     fftwf_plan_destroy_internal(cldr);
     fftwf_plan_destroy_internal(cldc);
     fftwf_tensor_destroy4(sz2i, vecszi, sz2, sz1);
     return (plan *) 0;
}

static solver *mksolver(int spltrnk, const int *buddies, int nbuddies)
{
     static const solver_adt sadt = { PROBLEM_RDFT2, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->spltrnk = spltrnk;
     slv->buddies = buddies;
     slv->nbuddies = nbuddies;
     return &(slv->super);
}

void fftwf_rdft2_rank_geq2_register(planner *p)
{
     int i;
     static const int buddies[] = { 1, 0, -2 };

     const int nbuddies = (int)(sizeof(buddies) / sizeof(buddies[0]));

     for (i = 0; i < nbuddies; ++i)
          REGISTER_SOLVER(p, mksolver(buddies[i], buddies, nbuddies));

     
}
