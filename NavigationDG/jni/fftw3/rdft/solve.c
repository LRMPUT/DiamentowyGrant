#include "rdft.h"

void fftwf_rdft_solve(const plan *ego_, const problem *p_)
{
     const plan_rdft *ego = (const plan_rdft *) ego_;
     const problem_rdft *p = (const problem_rdft *) p_;
     ego->apply(ego_, UNTAINT(p->I), UNTAINT(p->O));
}
