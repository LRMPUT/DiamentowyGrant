
#include "hc2hc.h"

void fftwf_khc2hc_register(planner *p, khc2hc codelet, const hc2hc_desc *desc)
{
     fftwf_regsolver_hc2hc_direct(p, codelet, desc);
}
