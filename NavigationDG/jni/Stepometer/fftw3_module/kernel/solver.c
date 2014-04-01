#include "ifftw.h"

solver *fftwf_mksolver(size_t size, const solver_adt *adt)
{
     solver *s = (solver *)MALLOC(size, SOLVERS);

     s->adt = adt;
     s->refcnt = 0;
     return s;
}

void fftwf_solver_use(solver *ego)
{
     ++ego->refcnt;
}

void fftwf_solver_destroy(solver *ego)
{
     if ((--ego->refcnt) == 0) {
	  if (ego->adt->destroy)
	       ego->adt->destroy(ego);
          fftwf_ifree(ego);
     }
}

void fftwf_solver_register(planner *plnr, solver *s)
{
     plnr->adt->register_solver(plnr, s);
}
