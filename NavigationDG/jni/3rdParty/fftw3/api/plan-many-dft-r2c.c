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

#include "api.h"
#include "../rdft/rdft.h"

fftwf_plan fftwf_plan_many_dft_r2c (int rank, const int *n,
			     int howmany,
			     float *in, const int *inembed,
			     int istride, int idist,
			     fftwf_complex *out, const int *onembed,
			     int ostride, int odist, unsigned flags)
{
     float *ro, *io;
     int *nfi, *nfo;
     int inplace;
     fftwf_plan p;

     if (!fftwf_many_kosherp (rank, n, howmany)) return 0;

     EXTRACT_REIM(FFT_SIGN, out, &ro, &io);
     inplace = in == ro;

     p = fftwf_mkapiplan(
	  0, flags,
	  fftwf_mkproblem_rdft2_d_3pointers(
	       fftwf_mktensor_rowmajor(
		    rank, n,
		    fftwf_rdft2_pad(rank, n, inembed, inplace, 0, &nfi),
		    fftwf_rdft2_pad(rank, n, onembed, inplace, 1, &nfo),
		    istride, 2 * ostride),
	       fftwf_mktensor_1d(howmany, idist, 2 * odist),
	       TAINT_UNALIGNED(in, flags),
	       TAINT_UNALIGNED(ro, flags), TAINT_UNALIGNED(io, flags),
	       R2HC));

     fftwf_ifree0 (nfi);
     fftwf_ifree0 (nfo);
     return p;
}
