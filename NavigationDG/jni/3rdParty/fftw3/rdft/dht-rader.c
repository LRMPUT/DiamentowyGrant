

#include "rdft.h"

/*
 * Compute DHTs of prime sizes using Rader's trick: turn them
 * into convolutions of size n - 1, which we then perform via a pair
 * of FFTs.   (We can then do prime real FFTs via rdft-dht.c.)
 *
 * Optionally (determined by the "pad" field of the solver), we can
 * perform the (cyclic) convolution by zero-padding to a size
 * >= 2*(n-1) - 1.  This is advantageous if n-1 has large prime factors.
 *
 */

typedef struct {
     solver super;
     int pad;
} S;

typedef struct {
     plan_rdft super;

     plan *cld1, *cld2;
     float *omega;
     INT n, npad, g, ginv;
     INT is, os;
     plan *cld_omega;
} P;

static rader_tl *omegas = 0;

/***************************************************************************/

/* If R2HC_ONLY_CONV is 1, we use a trick to perform the convolution
   purely in terms of R2HC transforms, as opposed to R2HC followed by H2RC.
   This requires a few more operations, but allows us to share the same
   plan/codelets for both Rader children. */
#define R2HC_ONLY_CONV 1

static void apply(const plan *ego_, float *I, float *O)
{
     const P *ego = (const P *) ego_;
     INT n = ego->n; /* prime */
     INT npad = ego->npad; /* == n - 1 for unpadded Rader; always even */
     INT is = ego->is, os;
     INT k, gpower, g;
     float *buf, *omega;
     float r0;

     buf = (float *) MALLOC(sizeof(float) * npad, BUFFERS);

     /* First, permute the input, storing in buf: */
     g = ego->g;
     for (gpower = 1, k = 0; k < n - 1; ++k, gpower = MULMOD(gpower, g, n)) {
	  buf[k] = I[gpower * is];
     }
     /* gpower == g^(n-1) mod n == 1 */;

     A(n - 1 <= npad);
     for (k = n - 1; k < npad; ++k) /* optionally, zero-pad convolution */
	  buf[k] = 0;

     os = ego->os;

     /* compute RDFT of buf, storing in buf (i.e., in-place): */
     {
	    plan_rdft *cld = (plan_rdft *) ego->cld1;
	    cld->apply((plan *) cld, buf, buf);
     }

     /* set output DC component: */
     O[0] = (r0 = I[0]) + buf[0];

     /* now, multiply by omega: */
     omega = ego->omega;
     buf[0] *= omega[0];
     for (k = 1; k < npad/2; ++k) {
	  E rB, iB, rW, iW, a, b;
	  rW = omega[k];
	  iW = omega[npad - k];
	  rB = buf[k];
	  iB = buf[npad - k];
	  a = rW * rB - iW * iB;
	  b = rW * iB + iW * rB;
#if R2HC_ONLY_CONV
	  buf[k] = a + b;
	  buf[npad - k] = a - b;
#else
	  buf[k] = a;
	  buf[npad - k] = b;
#endif
     }
     /* Nyquist component: */
     A(k + k == npad); /* since npad is even */
     buf[k] *= omega[k];

     /* this will add input[0] to all of the outputs after the ifft */
     buf[0] += r0;

     /* inverse FFT: */
     {
	    plan_rdft *cld = (plan_rdft *) ego->cld2;
	    cld->apply((plan *) cld, buf, buf);
     }

     /* do inverse permutation to unshuffle the output: */
     A(gpower == 1);
#if R2HC_ONLY_CONV
     O[os] = buf[0];
     gpower = g = ego->ginv;
     A(npad == n - 1 || npad/2 >= n - 1);
     if (npad == n - 1) {
	  for (k = 1; k < npad/2; ++k, gpower = MULMOD(gpower, g, n)) {
	       O[gpower * os] = buf[k] + buf[npad - k];
	  }
	  O[gpower * os] = buf[k];
	  ++k, gpower = MULMOD(gpower, g, n);
	  for (; k < npad; ++k, gpower = MULMOD(gpower, g, n)) {
	       O[gpower * os] = buf[npad - k] - buf[k];
	  }
     }
     else {
	  for (k = 1; k < n - 1; ++k, gpower = MULMOD(gpower, g, n)) {
	       O[gpower * os] = buf[k] + buf[npad - k];
	  }
     }
#else
     g = ego->ginv;
     for (k = 0; k < n - 1; ++k, gpower = MULMOD(gpower, g, n)) {
	  O[gpower * os] = buf[k];
     }
#endif
     A(gpower == 1);

     fftwf_ifree(buf);
}

static float *mkomega(enum wakefulness wakefulness,
		  plan *p_, INT n, INT npad, INT ginv)
{
     plan_rdft *p = (plan_rdft *) p_;
     float *omega;
     INT i, gpower;
     trigreal scale;
     triggen *t;

     if ((omega = fftwf_rader_tl_find(n, npad + 1, ginv, omegas)))
	  return omega;

     omega = (float *)MALLOC(sizeof(float) * npad, TWIDDLES);

     scale = npad; /* normalization for convolution */

     t = fftwf_mktriggen(wakefulness, n);
     for (i = 0, gpower = 1; i < n-1; ++i, gpower = MULMOD(gpower, ginv, n)) {
	  trigreal w[2];
	  t->cexpl(t, gpower, w);
	  omega[i] = (w[0] + w[1]) / scale;
     }
     fftwf_triggen_destroy(t);
     A(gpower == 1);

     A(npad == n - 1 || npad >= 2*(n - 1) - 1);

     for (; i < npad; ++i)
	  omega[i] = K(0.0);
     if (npad > n - 1)
	  for (i = 1; i < n-1; ++i)
	       omega[npad - i] = omega[n - 1 - i];

     p->apply(p_, omega, omega);

     fftwf_rader_tl_insert(n, npad + 1, ginv, omega, &omegas);
     return omega;
}

static void free_omega(float *omega)
{
     fftwf_rader_tl_delete(omega, &omegas);
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
	      A(!ego->omega);
	      ego->omega = mkomega(wakefulness,
				   ego->cld_omega,ego->n,ego->npad,ego->ginv);
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
     const P *ego = (const P *) ego_;

     p->print(p, "(dht-rader-%D/%D%ois=%oos=%(%p%)",
              ego->n, ego->npad, ego->is, ego->os, ego->cld1);
     if (ego->cld2 != ego->cld1)
          p->print(p, "%(%p%)", ego->cld2);
     if (ego->cld_omega != ego->cld1 && ego->cld_omega != ego->cld2)
          p->print(p, "%(%p%)", ego->cld_omega);
     p->putchr(p, ')');
}

static int applicable0(const problem *p_)
{
     const problem_rdft *p = (const problem_rdft *) p_;
     return (1
	     && p->sz->rnk == 1
	     && p->vecsz->rnk == 0
	     && p->kind[0] == DHT
	     && fftwf_is_prime(p->sz->dims[0].n)
	     && p->sz->dims[0].n > 2
	  );
}

static int applicable(const solver *ego, const problem *p, const planner *plnr)
{
     UNUSED(ego);
     return (!NO_SLOWP(plnr) && applicable0(p));
}

static INT choose_transform_size(INT minsz)
{
     static const INT primes[] = { 2, 3, 5, 0 };
     while (!fftwf_factors_into(minsz, primes) || minsz % 2)
	  ++minsz;
     return minsz;
}

static plan *mkplan(const solver *ego_, const problem *p_, planner *plnr)
{
     const S *ego = (const S *) ego_;
     const problem_rdft *p = (const problem_rdft *) p_;
     P *pln;
     INT n, npad;
     INT is, os;
     plan *cld1 = (plan *) 0;
     plan *cld2 = (plan *) 0;
     plan *cld_omega = (plan *) 0;
     float *buf = (float *) 0;
     problem *cldp;

     static const plan_adt padt = {
	  fftwf_rdft_solve, awake, print, destroy
     };

     if (!applicable(ego_, p_, plnr))
	  return (plan *) 0;

     n = p->sz->dims[0].n;
     is = p->sz->dims[0].is;
     os = p->sz->dims[0].os;

     if (ego->pad)
	  npad = choose_transform_size(2 * (n - 1) - 1);
     else
	  npad = n - 1;

     /* initial allocation for the purpose of planning */
     buf = (float *) MALLOC(sizeof(float) * npad, BUFFERS);

     cld1 = fftwf_mkplan_f_d(plnr,fftwf_mkproblem_rdft_1_d(fftwf_mktensor_1d(npad, 1, 1),
						fftwf_mktensor_1d(1, 0, 0),
						buf, buf,
						R2HC),  NO_SLOW, 0, 0);
     if (!cld1) goto nada;

     cldp =
          fftwf_mkproblem_rdft_1_d(
               fftwf_mktensor_1d(npad, 1, 1),
               fftwf_mktensor_1d(1, 0, 0),
	       buf, buf,
#if R2HC_ONLY_CONV
	       R2HC
#else
	       HC2R
#endif
	       );
     if (!(cld2 = fftwf_mkplan_f_d(plnr, cldp, NO_SLOW, 0, 0)))
	  goto nada;

     /* plan for omega */
     cld_omega = fftwf_mkplan_f_d(plnr,
			       fftwf_mkproblem_rdft_1_d(
				    fftwf_mktensor_1d(npad, 1, 1),
				    fftwf_mktensor_1d(1, 0, 0),
				    buf, buf, R2HC),
			       NO_SLOW, ESTIMATE, 0);
     if (!cld_omega) goto nada;

     /* deallocate buffers; let awake() or apply() allocate them for real */
     fftwf_ifree(buf);
     buf = 0;

     pln = MKPLAN_RDFT(P, &padt, apply);
     pln->cld1 = cld1;
     pln->cld2 = cld2;
     pln->cld_omega = cld_omega;
     pln->omega = 0;
     pln->n = n;
     pln->npad = npad;
     pln->is = is;
     pln->os = os;
     pln->g = fftwf_find_generator(n);
     pln->ginv = fftwf_power_mod(pln->g, n - 2, n);
     A(MULMOD(pln->g, pln->ginv, n) == 1);

     fftwf_ops_add(&cld1->ops, &cld2->ops, &pln->super.super.ops);
     pln->super.super.ops.other += (npad/2-1)*6 + npad + n + (n-1) * ego->pad;
     pln->super.super.ops.add += (npad/2-1)*2 + 2 + (n-1) * ego->pad;
     pln->super.super.ops.mul += (npad/2-1)*4 + 2 + ego->pad;
#if R2HC_ONLY_CONV
     pln->super.super.ops.other += n-2 - ego->pad;
     pln->super.super.ops.add += (npad/2-1)*2 + (n-2) - ego->pad;
#endif

     return &(pln->super.super);

 nada:
     fftwf_ifree0(buf);
     fftwf_plan_destroy_internal(cld_omega);
     fftwf_plan_destroy_internal(cld2);
     fftwf_plan_destroy_internal(cld1);
     return 0;
}

/* constructors */

static solver *mksolver(int pad)
{
     static const solver_adt sadt = { PROBLEM_RDFT, mkplan, 0 };
     S *slv = MKSOLVER(S, &sadt);
     slv->pad = pad;
     return &(slv->super);
}

void fftwf_dht_rader_register(planner *p)
{
     REGISTER_SOLVER(p, mksolver(0));
     REGISTER_SOLVER(p, mksolver(1));
}
