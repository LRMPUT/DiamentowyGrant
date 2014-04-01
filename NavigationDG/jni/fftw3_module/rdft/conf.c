#include "rdft.h"

static const solvtab s =
{
     SOLVTAB(fftwf_rdft_indirect_register),
     SOLVTAB(fftwf_rdft_rank0_register),
     SOLVTAB(fftwf_rdft_vrank3_transpose_register),
     SOLVTAB(fftwf_rdft_vrank_geq1_register),

     SOLVTAB(fftwf_rdft_nop_register),
     SOLVTAB(fftwf_rdft_buffered_register),
     SOLVTAB(fftwf_rdft_generic_register),
     SOLVTAB(fftwf_rdft_rank_geq2_register),

     SOLVTAB(fftwf_dft_r2hc_register),

     SOLVTAB(fftwf_rdft_dht_register),
     SOLVTAB(fftwf_dht_r2hc_register),
     SOLVTAB(fftwf_dht_rader_register),

     SOLVTAB(fftwf_rdft2_vrank_geq1_register),
     SOLVTAB(fftwf_rdft2_nop_register),
     SOLVTAB(fftwf_rdft2_rank0_register),
     SOLVTAB(fftwf_rdft2_buffered_register),
     SOLVTAB(fftwf_rdft2_rank_geq2_register),
     SOLVTAB(fftwf_rdft2_rdft_register),

     SOLVTAB(fftwf_hc2hc_generic_register),

     SOLVTAB_END
};

void fftwf_rdft_conf_standard(planner *p)
{
     fftwf_solvtab_exec(s, p);
     fftwf_solvtab_exec(fftwf_solvtab_rdft_r2cf, p);
     fftwf_solvtab_exec(fftwf_solvtab_rdft_r2cb, p);
     fftwf_solvtab_exec(fftwf_solvtab_rdft_r2r, p);
}
