
 


#include "ct.h"

void fftwf_kdft_dif_register(planner *p, kdftw codelet, const ct_desc *desc)
{
     fftwf_regsolver_ct_directw(p, codelet, desc, DECDIF);
}
