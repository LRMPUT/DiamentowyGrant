#include "rdft.h"

static void apply(const plan *ego_, float *r0, float *r1, float *cr, float *ci) {
    UNUSED(ego_);
    UNUSED(r0);
    UNUSED(r1);
    UNUSED(cr);
    UNUSED(ci);
}

static int applicable(const solver *ego_, const problem *p_) {
    const problem_rdft2 *p = (const problem_rdft2 *) p_;
    UNUSED(ego_);

    return (0
            /* case 1 : -infty vector rank */
            || (p->vecsz->rnk == RNK_MINFTY)

            /* case 2 : rank-0 in-place rdft, except that
               R2HC is not a no-op because it sets the imaginary
               part to 0 */
            || (1
            && p->kind != R2HC
            && p->sz->rnk == 0
            && FINITE_RNK(p->vecsz->rnk)
            && (p->r0 == p->cr)
            && fftwf_rdft2_inplace_strides(p, RNK_MINFTY)
            ));
}

static void print(const plan *ego, printer *p) {
    UNUSED(ego);
    p->print(p, "(rdft2-nop)");
}

static plan *mkplan(const solver *ego, const problem *p, planner *plnr) {
    static const plan_adt padt = {
        fftwf_rdft2_solve, fftwf_null_awake, print, fftwf_plan_null_destroy
    };
    plan_rdft2 *pln;

    UNUSED(plnr);

    if (!applicable(ego, p))
        return (plan *) 0;
    pln = MKPLAN_RDFT2(plan_rdft2, &padt, apply);
    fftwf_ops_zero(&pln->super.ops);

    return &(pln->super);
}

static solver *mksolver(void) {
    static const solver_adt sadt = {PROBLEM_RDFT2, mkplan, 0};
    return MKSOLVER(solver, &sadt);
}

void fftwf_rdft2_nop_register(planner *p) {
    REGISTER_SOLVER(p, mksolver());
}
