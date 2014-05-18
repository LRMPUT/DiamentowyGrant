#include "rdft.h"

void fftwf_kr2c_register(planner *p, kr2c codelet, const kr2c_desc *desc)
{
     REGISTER_SOLVER(p, fftwf_mksolver_rdft_r2c_direct(codelet, desc));
     REGISTER_SOLVER(p, fftwf_mksolver_rdft_r2c_directbuf(codelet, desc));
     REGISTER_SOLVER(p, fftwf_mksolver_rdft2_direct(codelet, desc));
}
