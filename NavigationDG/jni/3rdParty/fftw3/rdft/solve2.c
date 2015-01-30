#include "rdft.h"

void fftwf_rdft2_solve(const plan *ego_, const problem *p_)
{
     const plan_rdft2 *ego = (const plan_rdft2 *) ego_;
     const problem_rdft2 *p = (const problem_rdft2 *) p_;
     ego->apply(ego_,
		UNTAINT(p->r0), UNTAINT(p->r1),
		UNTAINT(p->cr), UNTAINT(p->ci));
}
