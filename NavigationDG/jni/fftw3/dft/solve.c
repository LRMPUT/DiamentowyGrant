


#include "dft.h"

/* use the apply() operation for DFT problems */
void fftwf_dft_solve(const plan *ego_, const problem *p_)
{
     const plan_dft *ego = (const plan_dft *) ego_;
     const problem_dft *p = (const problem_dft *) p_;
     ego->apply(ego_,
		UNTAINT(p->ri), UNTAINT(p->ii),
		UNTAINT(p->ro), UNTAINT(p->io));
}
