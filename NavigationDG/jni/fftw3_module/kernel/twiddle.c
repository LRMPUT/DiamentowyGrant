




#include "ifftw.h"
#include <math.h>

#define HASHSZ 109

/* hash table of known twiddle factors */
static twid *twlist[HASHSZ];

static INT hash(INT n, INT r)
{
     INT h = n * 17 + r;

     if (h < 0) h = -h;

     return (h % HASHSZ);
}

static int equal_instr(const tw_instr *p, const tw_instr *q)
{
     if (p == q)
          return 1;

     for (;; ++p, ++q) {
          if (p->op != q->op)
	       return 0;

	  switch (p->op) {
	      case TW_NEXT:
		   return (p->v == q->v); /* p->i is ignored */

	      case TW_FULL:
	      case TW_HALF:
		   if (p->v != q->v) return 0; /* p->i is ignored */
		   break;

	      default:
		   if (p->v != q->v || p->i != q->i) return 0;
		   break;
	  }
     }
     A(0 /* can't happen */);
}

static int ok_twid(const twid *t,
		   enum wakefulness wakefulness,
		   const tw_instr *q, INT n, INT r, INT m)
{
     return (wakefulness == t->wakefulness &&
	     n == t->n &&
	     r == t->r &&
	     m <= t->m &&
	     equal_instr(t->instr, q));
}

static twid *lookup(enum wakefulness wakefulness,
		    const tw_instr *q, INT n, INT r, INT m)
{
     twid *p;

     for (p = twlist[hash(n,r)];
	  p && !ok_twid(p, wakefulness, q, n, r, m);
	  p = p->cdr)
          ;
     return p;
}

static INT twlen0(INT r, const tw_instr *p, INT *vl)
{
     INT ntwiddle = 0;

     /* compute length of bytecode program */
     A(r > 0);
     for ( ; p->op != TW_NEXT; ++p) {
	  switch (p->op) {
	      case TW_FULL:
		   ntwiddle += (r - 1) * 2;
		   break;
	      case TW_HALF:
		   ntwiddle += (r - 1);
		   break;
	      case TW_CEXP:
		   ntwiddle += 2;
		   break;
	      case TW_COS:
	      case TW_SIN:
		   ntwiddle += 1;
		   break;
	  }
     }

     *vl = (INT)p->v;
     return ntwiddle;
}

INT fftwf_twiddle_length(INT r, const tw_instr *p)
{
     INT vl;
     return twlen0(r, p, &vl);
}

static float *compute(enum wakefulness wakefulness,
		  const tw_instr *instr, INT n, INT r, INT m)
{
     INT ntwiddle, j, vl;
    float *W, *W0;
     const tw_instr *p;
     triggen *t = fftwf_mktriggen(wakefulness, n);

     p = instr;
     ntwiddle = twlen0(r, p, &vl);

     A(m % vl == 0);

     W0 = W = (float *)MALLOC((ntwiddle * (m / vl)) * sizeof(float), TWIDDLES);

     for (j = 0; j < m; j += vl) {
          for (p = instr; p->op != TW_NEXT; ++p) {
	       switch (p->op) {
		   case TW_FULL: {
			INT i;
			for (i = 1; i < r; ++i) {
			     A((j + (INT)p->v) * i < n);
			     A((j + (INT)p->v) * i > -n);
			     t->cexp(t, (j + (INT)p->v) * i, W);
			     W += 2;
			}
			break;
		   }

		   case TW_HALF: {
			INT i;
			A((r % 2) == 1);
			for (i = 1; i + i < r; ++i) {
			     t->cexp(t, MULMOD(i, (j + (INT)p->v), n), W);
			     W += 2;
			}
			break;
		   }

		   case TW_COS: {
			float d[2];

			A((j + (INT)p->v) * p->i < n);
			A((j + (INT)p->v) * p->i > -n);
			t->cexp(t, (j + (INT)p->v) * (INT)p->i, d);
			*W++ = d[0];
			break;
		   }

		   case TW_SIN: {
			float d[2];

			A((j + (INT)p->v) * p->i < n);
			A((j + (INT)p->v) * p->i > -n);
			t->cexp(t, (j + (INT)p->v) * (INT)p->i, d);
			*W++ = d[1];
			break;
		   }

		   case TW_CEXP:
			A((j + (INT)p->v) * p->i < n);
			A((j + (INT)p->v) * p->i > -n);
			t->cexp(t, (j + (INT)p->v) * (INT)p->i, W);
			W += 2;
			break;
	       }
	  }
     }

     fftwf_triggen_destroy(t);
     return W0;
}

static void mktwiddle(enum wakefulness wakefulness,
		      twid **pp, const tw_instr *instr, INT n, INT r, INT m)
{
     twid *p;
     INT h;

     if ((p = lookup(wakefulness, instr, n, r, m))) {
          ++p->refcnt;
     } else {
	  p = (twid *) MALLOC(sizeof(twid), TWIDDLES);
	  p->n = n;
	  p->r = r;
	  p->m = m;
	  p->instr = instr;
	  p->refcnt = 1;
	  p->wakefulness = wakefulness;
	  p->W = compute(wakefulness, instr, n, r, m);

	  /* cons! onto twlist */
	  h = hash(n, r);
	  p->cdr = twlist[h];
	  twlist[h] = p;
     }

     *pp = p;
}

static void twiddle_destroy(twid **pp)
{
     twid *p = *pp;
     twid **q;

     if ((--p->refcnt) == 0) {
	  /* remove p from twiddle list */
	  for (q = &twlist[hash(p->n, p->r)]; *q; q = &((*q)->cdr)) {
	       if (*q == p) {
		    *q = p->cdr;
		    fftwf_ifree(p->W);
		    fftwf_ifree(p);
		    *pp = 0;
		    return;
	       }
	  }
	  A(0 /* can't happen */ );
     }
}


void fftwf_twiddle_awake(enum wakefulness wakefulness, twid **pp,
		      const tw_instr *instr, INT n, INT r, INT m)
{
     switch (wakefulness) {
	 case SLEEPY:
	      twiddle_destroy(pp);
	      break;
	 default:
	      mktwiddle(wakefulness, pp, instr, n, r, m);
	      break;
     }
}
