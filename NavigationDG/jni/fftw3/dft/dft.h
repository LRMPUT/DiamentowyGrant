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


#ifndef __DFT_H__
#define __DFT_H__

#include "../kernel/ifftw.h"
#include "codelet-dft.h"

/* problem.c: */
typedef struct {
     problem super;
     tensor *sz, *vecsz;
     float *ri, *ii, *ro, *io;
} problem_dft;

void fftwf_dft_zerotens(tensor *sz, float *ri, float *ii);
problem *fftwf_mkproblem_dft(const tensor *sz, const tensor *vecsz,
				float *ri, float *ii, float *ro, float *io);
problem *fftwf_mkproblem_dft_d(tensor *sz, tensor *vecsz,
			    float *ri, float *ii, float *ro, float *io);

/* solve.c: */
void fftwf_dft_solve(const plan *ego_, const problem *p_);

/* plan.c: */
typedef void (*dftapply) (const plan *ego, float *ri, float *ii, float *ro, float *io);

typedef struct {
     plan super;
     dftapply apply;
} plan_dft;

plan *fftwf_mkplan_dft(size_t size, const plan_adt *adt, dftapply apply);

#define MKPLAN_DFT(type, adt, apply) \
  (type *)fftwf_mkplan_dft(sizeof(type), adt, apply)

/* various solvers */
//solver *fftwf_mksolver_dft_direct(kdft k, const kdft_desc *desc);  todo
//solver *fftwf_mksolver_dft_directbuf(kdft k, const kdft_desc *desc);   todo

void fftwf_dft_rank0_register(planner *p);
void fftwf_dft_rank_geq2_register(planner *p);
void fftwf_dft_indirect_register(planner *p);
void fftwf_dft_indirect_transpose_register(planner *p);
void fftwf_dft_vrank_geq1_register(planner *p);
void fftwf_dft_vrank2_transpose_register(planner *p);
void fftwf_dft_vrank3_transpose_register(planner *p);
void fftwf_dft_buffered_register(planner *p);
void fftwf_dft_generic_register(planner *p);
void fftwf_dft_rader_register(planner *p);
void fftwf_dft_bluestein_register(planner *p);
void fftwf_dft_nop_register(planner *p);
void fftwf_ct_generic_register(planner *p);
void fftwf_ct_genericbuf_register(planner *p);

/* configurations */
//void fftwf_dft_conf_standard(planner *p);       todo

#if HAVE_CELL
  void fftwf_dft_conf_cell(planner *p);
#endif

#endif /* __DFT_H__ */
