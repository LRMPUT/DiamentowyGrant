#include "hc2hc.h"

hc2hc_solver *(*fftwf_mksolver_hc2hc_hook)(size_t, INT, hc2hc_mkinferior) = 0;

typedef struct {
     plan_rdft super;
     plan *cld;
     plan *cldw;
     INT r;
} P;

static void apply_dit(const plan *ego_, float *I, float *O)
{
     const P *ego = (const P *) ego_;
     plan_rdft *cld;
     plan_hc2hc *cldw;

     cld = (plan_rdft *) ego->cld;
     cld->apply(ego->cld, I, O);

     cldw = (plan_hc2hc *) ego->cldw;
     cldw->apply(ego->cldw, O);
}

static void apply_dif(const plan *ego_, float *I, float *O)
{
     const P *ego = (const P *) ego_;
     plan_rdft *cld;
     plan_hc2hc *cldw;

     cldw = (plan_hc2hc *) ego->cldw;
     cldw->apply(ego->cldw, I);

     cld = (plan_rdft *) ego->cld;
     cld->apply(ego->cld, I, O);
}

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;
     fftwf_plan_awake(ego->cld, wakefulness);
     fftwf_plan_awake(ego->cldw, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cldw);
     fftwf_plan_destroy_internal(ego->cld);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     p->print(p, "(rdft-ct-%s/%D%(%p%)%(%p%))",
	      ego->super.apply == apply_dit ? "dit" : "dif",
	      ego->r, ego->cldw, ego->cld);
}

static int applicable0(const hc2hc_solver *ego, const problem *p_, planner *plnr)
{
     const problem_rdft *p = (const problem_rdft *) p_;
     INT r;

     return (1
	     && p->sz->rnk == 1
	     && p->vecsz->rnk <= 1

	     && (/* either the problem is R2HC, which is solved by DIT */
		  (p->kind[0] == R2HC)
		  ||
		  /* or the problem is HC2R, in which case it is solved
		     by DIF, which destroys the input */
		  (p->kind[0] == HC2R &&
		   (p->I == p->O || !NO_DESTROY_INPUTP(plnr))))

	     && ((r = fftwf_choose_radix(ego->r, p->sz->dims[0].n)) > 0)
	     && p->sz->dims[0].n > r);
}

int fftwf_hc2hc_applicable(const hc2hc_solver *ego, const problem *p_, planner *plnr)
{
     const problem_rdft *p;

     if (!applicable0(ego, p_, plnr))
          return 0;

     p = (const problem_rdft *) p_;

     return (0
	     || p->vecsz->rnk == 0
	     || !NO_VRECURSEP(plnr)
	  );
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const hc2hc_solver *ego = (const hc2hc_solver *) ego_;
     const problem_rdft *p;
     P *pln = 0;
     plan *cld = 0, *cldw = 0;
     INT n, r, m, v, ivs, ovs;
     iodim *d;

     static const plan_adt padt = {
	  fftwf_rdft_solve, awake, print, destroy
     };

     if (NO_NONTHREADEDP(plnr) || !fftwf_hc2hc_applicable(ego, p_, plnr))
          return (plan *) 0;

     p = (const problem_rdft *) p_;
     d = p->sz->dims;
     n = d[0].n;
     r = fftwf_choose_radix(ego->r, n);
     m = n / r;

     fftwf_tensor_tornk1(p->vecsz, &v, &ivs, &ovs);

     switch (p->kind[0]) {
	 case R2HC:
	      cldw = ego->mkcldw(ego,
				 R2HC, r, m, d[0].os, v, ovs, 0, (m+2)/2,
				 p->O, plnr);
	      if (!cldw) goto nada;

	      cld = fftwf_mkplan_d(plnr,
				fftwf_mkproblem_rdft_d(
				     fftwf_mktensor_1d(m, r * d[0].is, d[0].os),
				     fftwf_mktensor_2d(r, d[0].is, m * d[0].os,
						    v, ivs, ovs),
				     p->I, p->O, p->kind)
		   );
	      if (!cld) goto nada;

	      pln = MKPLAN_RDFT(P, &padt, apply_dit);
	      break;

	 case HC2R:
	      cldw = ego->mkcldw(ego,
				 HC2R, r, m, d[0].is, v, ivs, 0, (m+2)/2,
				 p->I, plnr);
	      if (!cldw) goto nada;

	      cld = fftwf_mkplan_d(plnr,
				fftwf_mkproblem_rdft_d(
				     fftwf_mktensor_1d(m, d[0].is, r * d[0].os),
				     fftwf_mktensor_2d(r, m * d[0].is, d[0].os,
						    v, ivs, ovs),
				     p->I, p->O, p->kind)
		   );
	      if (!cld) goto nada;

	      pln = MKPLAN_RDFT(P, &padt, apply_dif);
	      break;

	 default:
	      A(0);
     }

     pln->cld = cld;
     pln->cldw = cldw;
     pln->r = r;
     fftwf_ops_add(&cld->ops, &cldw->ops, &pln->super.super.ops);

     /* inherit could_prune_now_p attribute from cldw */
     pln->super.super.could_prune_now_p = cldw->could_prune_now_p;

     return &(pln->super.super);

 nada:
     fftwf_plan_destroy_internal(cldw);
     fftwf_plan_destroy_internal(cld);
     return (plan *) 0;
}

hc2hc_solver *fftwf_mksolver_hc2hc(size_t size, INT r, hc2hc_mkinferior mkcldw)
{
     static const solver_adt sadt = { PROBLEM_RDFT, mkplan, 0 };
     hc2hc_solver *slv = (hc2hc_solver *)fftwf_mksolver(size, &sadt);
     slv->r = r;
     slv->mkcldw = mkcldw;
     return slv;
}

plan *fftwf_mkplan_hc2hc(size_t size, const plan_adt *adt, hc2hcapply apply)
{
     plan_hc2hc *ego;

     ego = (plan_hc2hc *) fftwf_mkplan(size, adt);
     ego->apply = apply;

     return &(ego->super);
}
