

#include "api.h"

static planner *plnr = 0;

/* create the planner for the rest of the API */
planner *fftwf_the_planner (void)
{
     if (!plnr) {
          plnr = fftwf_mkplanner();
          fftwf_configure_planner (plnr);
     }

     return plnr;
}

void fftwf_cleanup(void)
{
     if (plnr) {
          fftwf_planner_destroy(plnr);
          plnr = 0;
     }
}

void fftwf_set_timelimit(double tlim) 
{
     /* PLNR is not necessarily initialized when this function is
	called, so use X(the_planner)() */
     fftwf_the_planner()->timelimit = tlim; 
}
