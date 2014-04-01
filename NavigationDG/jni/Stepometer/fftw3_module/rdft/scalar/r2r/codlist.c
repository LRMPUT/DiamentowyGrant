#include "../../../kernel/ifftw.h"

extern void fftwf_codelet_e01_8(planner *);
extern void fftwf_codelet_e10_8(planner *);


extern const solvtab fftwf_solvtab_rdft_r2r;
const solvtab fftwf_solvtab_rdft_r2r = {
   SOLVTAB(fftwf_codelet_e01_8),
   SOLVTAB(fftwf_codelet_e10_8),
   SOLVTAB_END
};