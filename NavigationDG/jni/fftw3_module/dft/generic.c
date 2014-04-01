

#include "dft.h"

typedef struct {
     solver super;
} S;

typedef struct {
     plan_dft super;
     twid *td;
     INT n, is, os;
} P;


static void cdot(INT n, const E *x, const float *w,
		 float *or0, float *oi0, float *or1, float *oi1)
{
     INT i;

     E rr = x[0], ri = 0, ir = x[1], ii = 0;
     x += 2;
     for (i = 1; i + i < n; ++i) {
	  rr += x[0] * w[0];
	  ir += x[1] * w[0];
	  ri += x[2] * w[1];
	  ii += x[3] * w[1];
	  x += 4; w += 2;
     }
     *or0 = rr + ii;
     *oi0 = ir - ri;
     *or1 = rr - ii;
     *oi1 = ir + ri;
}

static void hartley(INT n, const float *xr, const float *xi, INT xs, E *o,
		    float *pr, float *pi)
{
     INT i;
     E sr, si;
     o[0] = sr = xr[0]; o[1] = si = xi[0]; o += 2;
     for (i = 1; i + i < n; ++i) {
	  sr += (o[0] = xr[i * xs] + xr[(n - i) * xs]);
	  si += (o[1] = xi[i * xs] + xi[(n - i) * xs]);
	  o[2] = xr[i * xs] - xr[(n - i) * xs];
	  o[3] = xi[i * xs] - xi[(n - i) * xs];
	  o += 4;
     }
     *pr = sr;
     *pi = si;
}

static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io)
{
     const P *ego = (const P *) ego_;
     INT i;
     INT n = ego->n, is = ego->is, os = ego->os;
     const float *W = ego->td->W;
     E *buf;

     STACK_MALLOC(E *, buf, n * 2 * sizeof(E));
     hartley(n, ri, ii, is, buf, ro, io);

     for (i = 1; i + i < n; ++i) {
	  cdot(n, buf, W,
	       ro + i * os, io + i * os,
	       ro + (n - i) * os, io + (n - i) * os);
	  W += n - 1;
     }

     STACK_FREE(buf);
}

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;
     static const tw_instr half_tw[] = {
	  { TW_HALF, 1, 0 },
	  { TW_NEXT, 1, 0 }
     };

     fftwf_twiddle_awake(wakefulness, &ego->td, half_tw, ego->n, ego->n,
		      (ego->n - 1) / 2);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *) ego_;

     p->print(p, "(dft-generic-%D)", ego->n);
}

static int applicable0(const problem *p_)
{
     const problem_dft *p = (const problem_dft *) p_;
     return (1
	     && p->sz->rnk == 1
	     && p->vecsz->rnk == 0
	     && (p->sz->dims[0].n % 2) == 1
	     && fftwf_is_prime(p->sz->dims[0].n)
	  );
}

static int applicable(const solver *ego, const problem *p_,
		      const planner *plnr)
{
     UNUSED(ego);
     if (NO_SLOWP(plnr)) return 0;
     if (!applicable0(p_)) return 0;

     if (NO_LARGE_GENERICP(plnr)) {
          const problem_dft *p = (const problem_dft *) p_;
	  if (p->sz->dims[0].n >= GENERIC_MIN_BAD) return 0;
     }
     return 1;
}

static plan *mkplan(const solver *ego, const problem *p_, planner *plnr)
{
     const problem_dft *p;
     P *pln;
     INT n;

     static const plan_adt padt = {
	  fftwf_dft_solve, awake, print, fftwf_plan_null_destroy
     };

     if (!applicable(ego, p_, plnr))
          return (plan *)0;

     pln = MKPLAN_DFT(P, &padt, apply);

     p = (const problem_dft *) p_;
     pln->n = n = p->sz->dims[0].n;
     pln->is = p->sz->dims[0].is;
     pln->os = p->sz->dims[0].os;
     pln->td = 0;

     pln->super.super.ops.add = (n-1) * 5;
     pln->super.super.ops.mul = 0;
     pln->super.super.ops.fma = (n-1) * (n-1) ;
#if 0 /* these are nice pipelined sequential loads and should cost nothing */
     pln->super.super.ops.other = (n-1)*(4 + 1 + 2 * (n-1));  /* approximate */
#endif

     return &(pln->super.super);
}

static solver *mksolver(void)
{
     static const solver_adt sadt = { PROBLEM_DFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     return &(slv->super);
}

void fftwf_dft_generic_register(planner *p)
{
     REGISTER_SOLVER(p, mksolver());
}
