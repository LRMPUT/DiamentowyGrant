#include "ifftw.h"

/* "Plan: To bother about the best method of accomplishing an
   accidental result."  (Ambrose Bierce, The Enlarged Devil's
   Dictionary). */

plan *fftwf_mkplan(size_t size, const plan_adt *adt)
{
     plan *p = (plan *)MALLOC(size, PLANS);

     A(adt->destroy);
     p->adt = adt;
     fftwf_ops_zero(&p->ops);
     p->pcost = 0.0;
     p->wakefulness = SLEEPY;
     p->could_prune_now_p = 0;

     return p;
}

/*
 * destroy a plan
 */
void fftwf_plan_destroy_internal(plan *ego)
{
     if (ego) {
	  A(ego->wakefulness == SLEEPY);
          ego->adt->destroy(ego);
	  fftwf_ifree(ego);
     }
}

/* dummy destroy routine for plans with no local state */
void fftwf_plan_null_destroy(plan *ego)
{
     UNUSED(ego);
     /* nothing */
}

void fftwf_plan_awake(plan *ego, enum wakefulness wakefulness)
{
     if (ego) {
	  A(((wakefulness == SLEEPY) ^ (ego->wakefulness == SLEEPY)));

	  ego->adt->awake(ego, wakefulness);
	  ego->wakefulness = wakefulness;
     }
}