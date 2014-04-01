#include "dft.h"

/*
 * Compute transforms of prime sizes using Rader's trick: turn them
 * into convolutions of size n - 1, which you then perform via a pair
 * of FFTs.
 */

typedef struct {
     solver super;
} S;

typedef struct {
     plan_dft super;

     plan *cld1, *cld2;
     float *omega;
     INT n, g, ginv;
     INT is, os;
     plan *cld_omega;
} P;

static rader_tl *omegas = 0;

static float *mkomega(enum wakefulness wakefulness, plan *p_, INT n, INT ginv)
{
     plan_dft *p = (plan_dft *) p_;
     float *omega;
     INT i, gpower;
     trigreal scale;
     triggen *t;

     if ((omega = fftwf_rader_tl_find(n, n, ginv, omegas)))
	  return omega;

     omega = (float *)MALLOC(sizeof(float) * (n - 1) * 2, TWIDDLES);

     scale = n - 1.0; /* normalization for convolution */

     t = fftwf_mktriggen(wakefulness, n);
     for (i = 0, gpower = 1; i < n-1; ++i, gpower = MULMOD(gpower, ginv, n)) {
	  trigreal w[2];
	  t->cexpl(t, gpower, w);
	  omega[2*i] = w[0] / scale;
	  omega[2*i+1] = FFT_SIGN * w[1] / scale;
     }
     fftwf_triggen_destroy(t);
     A(gpower == 1);

     p->apply(p_, omega, omega + 1, omega, omega + 1);

     fftwf_rader_tl_insert(n, n, ginv, omega, &omegas);
     return omega;
}

static void free_omega(float *omega)
{
     fftwf_rader_tl_delete(omega, &omegas);
}


/***************************************************************************/

/* Below, we extensively use the identity that fft(x*)* = ifft(x) in
   order to share data between forward and backward transforms and to
   obviate the necessity of having separate forward and backward
   plans.  (Although we often compute separate plans these days anyway
   due to the differing strides, etcetera.)

   Of course, since the new FFTW gives us separate pointers to
   the real and imaginary parts, we could have instead used the
   fft(r,i) = ifft(i,r) form of this identity, but it was easier to
   reuse the code from our old version. */

static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io)
{
     const P *ego = (const P *) ego_;
     INT is, os;
     INT k, gpower, g, r;
     float *buf;
     float r0 = ri[0], i0 = ii[0];

     r = ego->n; is = ego->is; os = ego->os; g = ego->g;
     buf = (float *) MALLOC(sizeof(float) * (r - 1) * 2, BUFFERS);

     /* First, permute the input, storing in buf: */
     for (gpower = 1, k = 0; k < r - 1; ++k, gpower = MULMOD(gpower, g, r)) {
	  float rA, iA;
	  rA = ri[gpower * is];
	  iA = ii[gpower * is];
	  buf[2*k] = rA; buf[2*k + 1] = iA;
     }
     /* gpower == g^(r-1) mod r == 1 */;


     /* compute DFT of buf, storing in output (except DC): */
     {
	    plan_dft *cld = (plan_dft *) ego->cld1;
	    cld->apply(ego->cld1, buf, buf+1, ro+os, io+os);
     }

     /* set output DC component: */
     {
	  ro[0] = r0 + ro[os];
	  io[0] = i0 + io[os];
     }

     /* now, multiply by omega: */
     {
	  const float *omega = ego->omega;
	  for (k = 0; k < r - 1; ++k) {
	       E rB, iB, rW, iW;
	       rW = omega[2*k];
	       iW = omega[2*k+1];
	       rB = ro[(k+1)*os];
	       iB = io[(k+1)*os];
	       ro[(k+1)*os] = rW * rB - iW * iB;
	       io[(k+1)*os] = -(rW * iB + iW * rB);
	  }
     }

     /* this will add input[0] to all of the outputs after the ifft */
     ro[os] += r0;
     io[os] -= i0;

     /* inverse FFT: */
     {
	    plan_dft *cld = (plan_dft *) ego->cld2;
	    cld->apply(ego->cld2, ro+os, io+os, buf, buf+1);
     }

     /* finally, do inverse permutation to unshuffle the output: */
     {
	  INT ginv = ego->ginv;
	  gpower = 1;
	  for (k = 0; k < r - 1; ++k, gpower = MULMOD(gpower, ginv, r)) {
	       ro[gpower * os] = buf[2*k];
	       io[gpower * os] = -buf[2*k+1];
	  }
	  A(gpower == 1);
     }


     fftwf_ifree(buf);
}

/***************************************************************************/

static void awake(plan *ego_, enum wakefulness wakefulness)
{
     P *ego = (P *) ego_;

     fftwf_plan_awake(ego->cld1, wakefulness);
     fftwf_plan_awake(ego->cld2, wakefulness);
     fftwf_plan_awake(ego->cld_omega, wakefulness);

     switch (wakefulness) {
	 case SLEEPY:
	      free_omega(ego->omega);
	      ego->omega = 0;
	      break;
	 default:
	      ego->omega = mkomega(wakefulness,
				   ego->cld_omega, ego->n, ego->ginv);
	      break;
     }
}

static void destroy(plan *ego_)
{
     P *ego = (P *) ego_;
     fftwf_plan_destroy_internal(ego->cld_omega);
     fftwf_plan_destroy_internal(ego->cld2);
     fftwf_plan_destroy_internal(ego->cld1);
}

static void print(const plan *ego_, printer *p)
{
     const P *ego = (const P *)ego_;
     p->print(p, "(dft-rader-%D%ois=%oos=%(%p%)",
              ego->n, ego->is, ego->os, ego->cld1);
     if (ego->cld2 != ego->cld1)
          p->print(p, "%(%p%)", ego->cld2);
     if (ego->cld_omega != ego->cld1 && ego->cld_omega != ego->cld2)
          p->print(p, "%(%p%)", ego->cld_omega);
     p->putchr(p, ')');
}

static int applicable0(const solver *ego_, const problem *p_)
{
     const problem_dft *p = (const problem_dft *) p_;
     UNUSED(ego_);
     return (1
	     && p->sz->rnk == 1
	     && p->vecsz->rnk == 0
	     && fftwf_is_prime(p->sz->dims[0].n)
	  );
}

static int applicable(const solver *ego_, const problem *p_,
		      const planner *plnr)
{
     return (!NO_SLOWP(plnr) && applicable0(ego_, p_));
}

static int mkP(P *pln, INT n, INT is, INT os, float *ro, float *io,
	       planner *plnr)
{
     plan *cld1 = (plan *) 0;
     plan *cld2 = (plan *) 0;
     plan *cld_omega = (plan *) 0;
     float *buf = (float *) 0;

     /* initial allocation for the purpose of planning */
     buf = (float *) MALLOC(sizeof(float) * (n - 1) * 2, BUFFERS);

     cld1 = fftwf_mkplan_f_d(plnr,
			  fftwf_mkproblem_dft_d(fftwf_mktensor_1d(n - 1, 2, os),
					     fftwf_mktensor_1d(1, 0, 0),
					     buf, buf + 1, ro + os, io + os),
			  NO_SLOW, 0, 0);
     if (!cld1) goto nada;

     cld2 = fftwf_mkplan_f_d(plnr,
			  fftwf_mkproblem_dft_d(fftwf_mktensor_1d(n - 1, os, 2),
					     fftwf_mktensor_1d(1, 0, 0),
					     ro + os, io + os, buf, buf + 1),
			  NO_SLOW, 0, 0);

     if (!cld2) goto nada;

     /* plan for omega array */
     cld_omega = fftwf_mkplan_f_d(plnr,
			       fftwf_mkproblem_dft_d(fftwf_mktensor_1d(n - 1, 2, 2),
						  fftwf_mktensor_1d(1, 0, 0),
						  buf, buf + 1, buf, buf + 1),
			       NO_SLOW, ESTIMATE, 0);
     if (!cld_omega) goto nada;

     /* deallocate buffers; let awake() or apply() allocate them for real */
     fftwf_ifree(buf);
     buf = 0;

     pln->cld1 = cld1;
     pln->cld2 = cld2;
     pln->cld_omega = cld_omega;
     pln->omega = 0;
     pln->n = n;
     pln->is = is;
     pln->os = os;
     pln->g = fftwf_find_generator(n);
     pln->ginv = fftwf_power_mod(pln->g, n - 2, n);
     A(MULMOD(pln->g, pln->ginv, n) == 1);

     fftwf_ops_add(&cld1->ops, &cld2->ops, &pln->super.super.ops);
     pln->super.super.ops.other += (n - 1) * (4 * 2 + 6) + 6;
     pln->super.super.ops.add += (n - 1) * 2 + 4;
     pln->super.super.ops.mul += (n - 1) * 4;

     return 1;

 nada:
     fftwf_ifree0(buf);
     fftwf_plan_destroy_internal(cld_omega);
     fftwf_plan_destroy_internal(cld2);
     fftwf_plan_destroy_internal(cld1);
     return 0;
}

static plan *mkplan(const solver *ego, const problem *p_, planner *plnr)
{
     const problem_dft *p = (const problem_dft *) p_;
     P *pln;
     INT n;
     INT is, os;

     static const plan_adt padt = {
	  fftwf_dft_solve, awake, print, destroy
     };

     if (!applicable(ego, p_, plnr))
	  return (plan *) 0;

     n = p->sz->dims[0].n;
     is = p->sz->dims[0].is;
     os = p->sz->dims[0].os;

     pln = MKPLAN_DFT(P, &padt, apply);
     if (!mkP(pln, n, is, os, p->ro, p->io, plnr)) {
	  fftwf_ifree(pln);
	  return (plan *) 0;
     }
     return &(pln->super.super);
}

static solver *mksolver(void)
{
     static const solver_adt sadt = { PROBLEM_DFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     return &(slv->super);
}

void fftwf_dft_rader_register(planner *p)
{
     REGISTER_SOLVER(p, mksolver());
}