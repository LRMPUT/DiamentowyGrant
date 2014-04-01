#include "rdft.h"
#include "../dft/dft.h"

typedef struct {
     solver super;
} S;

typedef struct {
     plan_dft super;
     plan *cld;
     INT ishift, oshift;
     INT os;
     INT n;
} P;

static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io)
{
     const P *ego = (const P *) ego_;
     INT n;

     UNUSED(ii);

     { /* transform vector of real & imag parts: */
	  plan_rdft *cld = (plan_rdft *) ego->cld;
	  cld->apply((plan *) cld, ri + ego->ishift, ro + ego->oshift);
     }

     n = ego->n;
     if (n > 1) {
	  INT i, os = ego->os;
	  for (i = 1; i < (n + 1)/2; ++i) {
	       E rop, iop, iom, rom;
	       rop = ro[os * i];
	       iop = io[os * i];
	       rom = ro[os * (n - i)];
	       iom = io[os * (n - i)];
	       ro[os * i] = rop - iom;
	       io[os * i] = iop + rom;
	       ro[os * (n - i)] = rop + iom;
	       io[os * (n - i)] = iop - rom;
	  }
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
     p->print(p, "(dft-r2hc-%D%(%p%))", ego->n, ego->cld);
}

static int applicable0(const problem *p_)
{
     const problem_dft *p = (const problem_dft *) p_;
     return ((p->sz->rnk == 1 && p->vecsz->rnk == 0)
	     || (p->sz->rnk == 0 && FINITE_RNK(p->vecsz->rnk))
	  );
}


static int splitp(float *r, float *i, INT n, INT s)
{
     return ((r > i ? (r - i) : (i - r)) >= n * (s > 0 ? s : 0-s));
}

static int applicable(const problem *p_, const planner *plnr)
{
     if (!applicable0(p_)) return 0;

     {
	  const problem_dft *p = (const problem_dft *) p_;

	  /* rank-0 problems are always OK */
	  if (p->sz->rnk == 0) return 1;

	  /* this solver is ok for split arrays */
	  if (p->sz->rnk == 1 &&
	      splitp(p->ri, p->ii, p->sz->dims[0].n, p->sz->dims[0].is) &&
	      splitp(p->ro, p->io, p->sz->dims[0].n, p->sz->dims[0].os))
	       return 1;

	  return !(NO_DFT_R2HCP(plnr));
     }
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     P *pln;
     const problem_dft *p;
     plan *cld;
     INT ishift = 0, oshift = 0;

     static const plan_adt padt = {
	  fftwf_dft_solve, awake, print, destroy
     };

     UNUSED(ego_);
     if (!applicable(p_, plnr))
          return (plan *)0;

     p = (const problem_dft *) p_;

     {
	  tensor *ri_vec = fftwf_mktensor_1d(2, p->ii - p->ri, p->io - p->ro);
	  tensor *cld_vec = fftwf_tensor_append(ri_vec, p->vecsz);
	  int i;
	  for (i = 0; i < cld_vec->rnk; ++i) { /* make all istrides > 0 */
	       if (cld_vec->dims[i].is < 0) {
		    INT nm1 = cld_vec->dims[i].n - 1;
		    ishift -= nm1 * (cld_vec->dims[i].is *= -1);
		    oshift -= nm1 * (cld_vec->dims[i].os *= -1);
	       }
	  }
	  cld = fftwf_mkplan_d(plnr,
			    fftwf_mkproblem_rdft_1(p->sz, cld_vec,
						p->ri + ishift,
						p->ro + oshift, R2HC));
	  fftwf_tensor_destroy2(ri_vec, cld_vec);
     }
     if (!cld) return (plan *)0;

     pln = MKPLAN_DFT(P, &padt, apply);

     if (p->sz->rnk == 0) {
	  pln->n = 1;
	  pln->os = 0;
     }
     else {
	  pln->n = p->sz->dims[0].n;
	  pln->os = p->sz->dims[0].os;
     }
     pln->ishift = ishift;
     pln->oshift = oshift;

     pln->cld = cld;

     pln->super.super.ops = cld->ops;
     pln->super.super.ops.other += 8 * ((pln->n - 1)/2);
     pln->super.super.ops.add += 4 * ((pln->n - 1)/2);
     pln->super.super.ops.other += 1; /* estimator hack for nop plans */

     return &(pln->super.super);
}

static solver *mksolver(void)
{
     static const solver_adt sadt = { PROBLEM_DFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     return &(slv->super);
}

void fftwf_dft_r2hc_register(planner *p)
{
     REGISTER_SOLVER(p, mksolver());
}

