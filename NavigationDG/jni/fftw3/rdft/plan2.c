#include "rdft.h"

plan *fftwf_mkplan_rdft2(size_t size, const plan_adt *adt, rdft2apply apply)
{
     plan_rdft2 *ego;

     ego = (plan_rdft2 *) fftwf_mkplan(size, adt);
     ego->apply = apply;

     return &(ego->super);
}
