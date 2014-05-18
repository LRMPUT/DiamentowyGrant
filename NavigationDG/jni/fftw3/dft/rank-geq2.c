

#include "dft.h"

typedef struct {
     solver super;
     int spltrnk;
     const int *buddies;
     int nbuddies;
} S;

typedef struct {
     plan_dft super;

     plan *cld1, *cld2;
     const S *solver;
} P;

/* Compute multi-dimensional DFT by applying the two cld plans
   (lower-rnk DFTs). */
static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io)
{
     const P *ego = (const P *) ego_;
     plan_dft *cld1, *cld2;

     cld1 = (plan_dft *) ego->cld1;
     cld1->apply(ego->cld1, ri, ii, ro, io);

     cld2 = (plan_dft *) ego->cld2;
     cld2->apply(ego->cld2, ro, io, ro, io);
}


static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;
     fftwf_plan_awake(ego->cld1, wakefulness);
     fftwf_plan_awake(ego->cld2, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cld2);
     fftwf_plan_destroy_internal(ego->cld1);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     const S *s = ego->solver;
     p->print(p, "(dft-rank>=2/%d%(%p%)%(%p%))",
	      s->spltrnk, ego->cld1, ego->cld2);
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

static int applicable0(const solver *ego_, const problem *p_, int *rp)
{
     const problem_dft *p = (const problem_dft *) p_;
     const S *ego = (const S *)ego_;
     return (1
	     && FINITE_RNK(p->sz->rnk) && FINITE_RNK(p->vecsz->rnk)
	     && p->sz->rnk >= 2
	     && picksplit(ego, p->sz, rp)
	  );
}

/* TODO: revise this. */
static int applicable(const solver *ego_, const problem *p_,
		      const planner *plnr, int *rp)
{
     const S *ego = (const S *)ego_;
     const problem_dft *p = (const problem_dft *) p_;

     if (!applicable0(ego_, p_, rp)) return 0;

     if (NO_RANK_SPLITSP(plnr) && (ego->spltrnk != ego->buddies[0])) return 0;

     /* FIXME: this heuristic is broken on Cell, where vrank-geq1
	is slow */
#ifndef HAVE_CELL
     /* Heuristic: if the vector stride is greater than the transform
        sz, don't use (prefer to do the vector loop first with a
        vrank-geq1 plan). */
     if (NO_UGLYP(plnr))
	  if (p->vecsz->rnk > 0 &&
	      fftwf_tensor_min_stride(p->vecsz) > fftwf_tensor_max_index(p->sz))
	       return 0;
#else
     UNUSED(p);
#endif

     return 1;
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     const problem_dft *p;
     P *pln;
     plan *cld1 = 0, *cld2 = 0;
     tensor *sz1, *sz2, *vecszi, *sz2i;
     int spltrnk;

     static const plan_adt padt = {
	  fftwf_dft_solve, awake, print, destroy
     };

     if (!applicable(ego_, p_, plnr, &spltrnk))
          return (plan *) 0;

     p = (const problem_dft *) p_;
     fftwf_tensor_split(p->sz, &sz1, spltrnk, &sz2);
     vecszi = fftwf_tensor_copy_inplace(p->vecsz, INPLACE_OS);
     sz2i = fftwf_tensor_copy_inplace(sz2, INPLACE_OS);

     cld1 = fftwf_mkplan_d(plnr,
			fftwf_mkproblem_dft_d(fftwf_tensor_copy(sz2),
					   fftwf_tensor_append(p->vecsz, sz1),
					   p->ri, p->ii, p->ro, p->io));
     if (!cld1) goto nada;

     cld2 = fftwf_mkplan_d(plnr,
			fftwf_mkproblem_dft_d(
			     fftwf_tensor_copy_inplace(sz1, INPLACE_OS),
			     fftwf_tensor_append(vecszi, sz2i),
			     p->ro, p->io, p->ro, p->io));
     if (!cld2) goto nada;

     pln = MKPLAN_DFT(P, &padt, apply);

     pln->cld1 = cld1;
     pln->cld2 = cld2;

     pln->solver = ego;
     fftwf_ops_add(&cld1->ops, &cld2->ops, &pln->super.super.ops);

     fftwf_tensor_destroy4(sz1, sz2, vecszi, sz2i);

     return &(pln->super.super);

 nada:
     fftwf_plan_destroy_internal(cld2);
     fftwf_plan_destroy_internal(cld1);
     fftwf_tensor_destroy4(sz1, sz2, vecszi, sz2i);
     return (plan *) 0;
}

static solver *mksolver(int spltrnk, const int *buddies, int nbuddies)
{
     static const solver_adt sadt = { PROBLEM_DFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->spltrnk = spltrnk;
     slv->buddies = buddies;
     slv->nbuddies = nbuddies;
     return &(slv->super);
}

void fftwf_dft_rank_geq2_register(planner *p)
{
     int i;
     static const int buddies[] = { 1, 0, -2 };

     const int nbuddies = (int)(sizeof(buddies) / sizeof(buddies[0]));

     for (i = 0; i < nbuddies; ++i)
          REGISTER_SOLVER(p, mksolver(buddies[i], buddies, nbuddies));

    
}
