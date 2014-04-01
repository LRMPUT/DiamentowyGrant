


#include "dft.h"
#include <stddef.h>

static void destroy(problem *ego_)
{
     problem_dft *ego = (problem_dft *) ego_;
    fftwf_tensor_destroy2(ego->vecsz, ego->sz);
    fftwf_ifree(ego_);
}

static void hash(const problem *p_, md5 *m)
{
     const problem_dft *p = (const problem_dft *) p_;
    fftwf_md5puts(m, "dft");
    fftwf_md5int(m, p->ri == p->ro);
    fftwf_md5INT(m, p->ii - p->ri);
    fftwf_md5INT(m, p->io - p->ro);
    fftwf_md5int(m,fftwf_alignment_of(p->ri));
    fftwf_md5int(m,fftwf_alignment_of(p->ii));
    fftwf_md5int(m,fftwf_alignment_of(p->ro));
    fftwf_md5int(m,fftwf_alignment_of(p->io));
    fftwf_tensor_md5(m, p->sz);
    fftwf_tensor_md5(m, p->vecsz);
}

static void print(const problem *ego_, printer *p)
{
     const problem_dft *ego = (const problem_dft *) ego_;
      p->print(p, "(dft %d %d %d %D %D %T %T)",
	      ego->ri == ego->ro,
	      fftwf_alignment_of(ego->ri),
	      fftwf_alignment_of(ego->ro),
	      (INT)(ego->ii - ego->ri),
	      (INT)(ego->io - ego->ro),
	      ego->sz,
	      ego->vecsz);
}

static void zero(const problem *ego_)
{
     const problem_dft *ego = (const problem_dft *) ego_;
     tensor *sz =fftwf_tensor_append(ego->vecsz, ego->sz);
    fftwf_dft_zerotens(sz, UNTAINT(ego->ri), UNTAINT(ego->ii));
    fftwf_tensor_destroy(sz);
}

static const problem_adt padt =
{
     PROBLEM_DFT,
     hash,
     zero,
     print,
     destroy
};

problem *fftwf_mkproblem_dft(const tensor *sz, const tensor *vecsz,
			 float *ri,float *ii,float *ro,float *io)
{
     problem_dft *ego;

     /* enforce pointer equality if untainted pointers are equal */
     if (UNTAINT(ri) == UNTAINT(ro))
	  ri = ro = JOIN_TAINT(ri, ro);
     if (UNTAINT(ii) == UNTAINT(io))
	  ii = io = JOIN_TAINT(ii, io);

     /* more correctness conditions: */
     A(TAINTOF(ri) == TAINTOF(ii));
     A(TAINTOF(ro) == TAINTOF(io));

     A(fftwf_tensor_kosherp(sz));
     A(fftwf_tensor_kosherp(vecsz));

     if (ri == ro || ii == io) {
	  /* If either real or imag pointers are in place, both must be. */
	  if (ri != ro || ii != io || !fftwf_tensor_inplace_locations(sz, vecsz))
	       return fftwf_mkproblem_unsolvable();
     }

     ego = (problem_dft *)fftwf_mkproblem(sizeof(problem_dft), &padt);

     ego->sz =fftwf_tensor_compress(sz);
     ego->vecsz =fftwf_tensor_compress_contiguous(vecsz);
     ego->ri = ri;
     ego->ii = ii;
     ego->ro = ro;
     ego->io = io;

     A(FINITE_RNK(ego->sz->rnk));
     return &(ego->super);
}

/* Same asfftwf_mkproblem_dft), but also destroy input tensors. */
problem *fftwf_mkproblem_dft_d(tensor *sz, tensor *vecsz,
			    float *ri,float *ii,float *ro,float *io)
{
     problem *p =fftwf_mkproblem_dft(sz, vecsz, ri, ii, ro, io);
    fftwf_tensor_destroy2(vecsz, sz);
     return p;
}
