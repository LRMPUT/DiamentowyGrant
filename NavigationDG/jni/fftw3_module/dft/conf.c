/*
 * Copyright (c) 2003, 2007-8 Matteo Frigo
 * Copyright (c) 2003, 2007-8 Massachusetts Institute of Technology
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */


#include "dft.h"

static const solvtab s =
{
     SOLVTAB(fftwf_dft_indirect_register),
     SOLVTAB(fftwf_dft_indirect_transpose_register),
     SOLVTAB(fftwf_dft_rank_geq2_register),
     SOLVTAB(fftwf_dft_vrank_geq1_register),
     SOLVTAB(fftwf_dft_buffered_register),
     SOLVTAB(fftwf_dft_generic_register),
     SOLVTAB(fftwf_dft_rader_register),
     SOLVTAB(fftwf_dft_bluestein_register),
     SOLVTAB(fftwf_dft_nop_register),
     SOLVTAB(fftwf_ct_generic_register),
     SOLVTAB(fftwf_ct_genericbuf_register),
     SOLVTAB_END
};

void fftwf_dft_conf_standard(planner *p)
{
     fftwf_solvtab_exec(s, p);
     fftwf_solvtab_exec(fftwf_solvtab_dft_standard, p);

}
