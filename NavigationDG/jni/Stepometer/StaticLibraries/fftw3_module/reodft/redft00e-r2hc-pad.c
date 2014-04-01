

#include "reodft.h"

typedef struct {
     solver super;
} S;

typedef struct {
     plan_rdft super;
     plan *cld, *cldcpy;
     INT is;
     INT n;
     INT vl;
     INT ivs, ovs;
} P;

static void apply(const plan *ego_, float *I, float *O)
{
     const P *ego = (const P *) ego_;
     INT is = ego->is;
     INT i, n = ego->n;
     INT iv, vl = ego->vl;
     INT ivs = ego->ivs, ovs = ego->ovs;
     float *buf;

     buf = (float *) MALLOC(sizeof (float) * (2*n), BUFFERS);

     for (iv = 0; iv < vl; ++iv, I += ivs, O += ovs) {
	  buf[0] = I[0];
	  for (i = 1; i < n; ++i) {
	       float a = I[i * is];
	       buf[i] = a;
	       buf[2*n - i] = a;
	  }
	  buf[i] = I[i * is]; /* i == n, Nyquist */

	  /* r2hc transform of size 2*n */
	  {
	       plan_rdft *cld = (plan_rdft *) ego->cld;
	       cld->apply((plan *) cld, buf, buf);
	  }

	  /* copy n+1 real numbers (real parts of hc array) from buf to O */
	  {
	       plan_rdft *cldcpy = (plan_rdft *) ego->cldcpy;
	       cldcpy->apply((plan *) cldcpy, buf, O);
	  }
     }

     fftwf_ifree(buf);
}

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;
     fftwf_plan_awake(ego->cld, wakefulness);
     fftwf_plan_awake(ego->cldcpy, wakefulness);
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cldcpy);
     fftwf_plan_destroy_internal(ego->cld);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;
     p->print(p, "(redft00e-r2hc-pad-%D%v%(%p%)%(%p%))",
	      ego->n + 1, ego->vl, ego->cld, ego->cldcpy);
}

static int applicable0(const solver *ego_, const problem *p_)
{
     const problem_rdft *p = (const problem_rdft *) p_;
     UNUSED(ego_);

     return (1
	     && p->sz->rnk == 1
	     && p->vecsz->rnk <= 1
	     && p->kind[0] == REDFT00
	     && p->sz->dims[0].n > 1  /* n == 1 is not well-defined */
	  );
}

static int applicable(const solver *ego, const problem *p, const planner *plnr)
{
     return (!NO_SLOWP(plnr) && applicable0(ego, p));
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     P *pln;
     const problem_rdft *p;
     plan *cld = (plan *) 0, *cldcpy;
     float *buf = (float *) 0;
     INT n;
     INT vl, ivs, ovs;
     opcnt ops;

     static const plan_adt padt = {
	 fftwf_rdft_solve, awake, print, destroy
     };

     if (!applicable(ego_, p_, plnr))
	  goto nada;

     p = (const problem_rdft *) p_;

     n = p->sz->dims[0].n - 1;
     A(n > 0);
     buf = (float *) MALLOC(sizeof (float) * (2*n), BUFFERS);

     cld = fftwf_mkplan_d(plnr,fftwf_mkproblem_rdft_1_d(fftwf_mktensor_1d(2*n,1,1),
						  fftwf_mktensor_0d(),
						  buf, buf, R2HC));
     if (!cld)
	  goto nada;

     fftwf_tensor_tornk1(p->vecsz, &vl, &ivs, &ovs);
     cldcpy =
	  fftwf_mkplan_d(plnr,
		      fftwf_mkproblem_rdft_1_d(fftwf_mktensor_0d(),
					    fftwf_mktensor_1d(n+1,1,
							   p->sz->dims[0].os),
					    buf, TAINT(p->O, ovs), R2HC));
     if (!cldcpy)
	  goto nada;

     fftwf_ifree(buf);

     pln = MKPLAN_RDFT(P, &padt, apply);

     pln->n = n;
     pln->is = p->sz->dims[0].is;
     pln->cld = cld;
     pln->cldcpy = cldcpy;
     pln->vl = vl;
     pln->ivs = ivs;
     pln->ovs = ovs;

     fftwf_ops_zero(&ops);
     ops.other = n + 2*n; /* loads + stores (input -> buf) */

     fftwf_ops_zero(&pln->super.super.ops);
     fftwf_ops_madd2(pln->vl, &ops, &pln->super.super.ops);
     fftwf_ops_madd2(pln->vl, &cld->ops, &pln->super.super.ops);
     fftwf_ops_madd2(pln->vl, &cldcpy->ops, &pln->super.super.ops);

     return &(pln->super.super);

 nada:
     fftwf_ifree0(buf);
     if (cld)
	  fftwf_plan_destroy_internal(cld);
     return (plan *)0;
}

/* constructor */
static solver *mksolver(void)
{
     static const solver_adt sadt = { PROBLEM_RDFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     return &(slv->super);
}

void fftwf_redft00e_r2hc_pad_register(planner *p)
{
     REGISTER_SOLVER(p, mksolver());
}


