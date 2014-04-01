

#include "dft.h"

void fftwf_kdft_register(planner *p, kdft codelet, const kdft_desc *desc)
{
     REGISTER_SOLVER(p, fftwf_mksolver_dft_direct(codelet, desc));
     REGISTER_SOLVER(p, fftwf_mksolver_dft_directbuf(codelet, desc));
}
