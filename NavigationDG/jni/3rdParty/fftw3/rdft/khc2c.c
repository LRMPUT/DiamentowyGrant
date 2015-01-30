
#include "ct-hc2c.h"

void fftwf_khc2c_register(planner *p, khc2c codelet, const hc2c_desc *desc,
		       hc2c_kind hc2ckind)
{
     fftwf_regsolver_hc2c_direct(p, codelet, desc, hc2ckind);
}