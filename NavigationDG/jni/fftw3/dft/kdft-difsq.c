

#include "ct.h"

void fftwf_kdft_difsq_register(planner *p, kdftwsq k, const ct_desc *desc)
{
     fftwf_regsolver_ct_directwsq(p, k, desc, DECDIF);
}
