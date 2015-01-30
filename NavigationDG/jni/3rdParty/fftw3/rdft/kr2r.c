#include "rdft.h"

void fftwf_kr2r_register(planner *p, kr2r codelet, const kr2r_desc *desc)
{
     REGISTER_SOLVER(p, fftwf_mksolver_rdft_r2r_direct(codelet, desc));
}