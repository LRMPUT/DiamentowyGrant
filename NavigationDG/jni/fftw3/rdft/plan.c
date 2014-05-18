#include "rdft.h"

plan *fftwf_mkplan_rdft(size_t size, const plan_adt *adt, rdftapply apply)
{
     plan_rdft *ego;

     ego = (plan_rdft *) fftwf_mkplan(size, adt);
     ego->apply = apply;

     return &(ego->super);
}