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

#ifndef __REODFT_H__
#define __REODFT_H__

#include "../kernel/ifftw.h"
#include "../rdft/rdft.h"

#define REODFT_KINDP(k) ((k) >= REDFT00 && (k) <= RODFT11)

void fftwf_redft00e_r2hc_register(planner *p);
void fftwf_redft00e_r2hc_pad_register(planner *p);
void fftwf_rodft00e_r2hc_register(planner *p);
void fftwf_rodft00e_r2hc_pad_register(planner *p);
void fftwf_reodft00e_splitradix_register(planner *p);
void fftwf_reodft010e_r2hc_register(planner *p);
void fftwf_reodft11e_r2hc_register(planner *p);
void fftwf_reodft11e_radix2_r2hc_register(planner *p);
void fftwf_reodft11e_r2hc_odd_register(planner *p);

/* configurations */
void fftwf_reodft_conf_standard(planner *p);

#endif /* __REODFT_H__ */