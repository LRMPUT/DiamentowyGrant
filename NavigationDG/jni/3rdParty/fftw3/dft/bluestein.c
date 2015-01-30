

#include "dft.h"

typedef struct {
    solver super;
} S;

typedef struct {
    plan_dft super;
    INT n; /* problem size */
    INT nb; /* size of convolution */
    float *w; /* lambda k . exp(2*pi*i*k^2/(2*n)) */
    float *W; /* DFT(w) */
    plan *cldf;
    INT is, os;
} P;

static void bluestein_sequence(enum wakefulness wakefulness, INT n, float *w) {
    INT k, ksq, n2 = 2 * n;
    triggen *t = fftwf_mktriggen(wakefulness, n2);

    ksq = 0;
    for (k = 0; k < n; ++k) {
        t->cexp(t, ksq, w + 2 * k);
        /* careful with overflow */
        ksq += 2 * k + 1;
        while (ksq > n2) ksq -= n2;
    }

    fftwf_triggen_destroy(t);
}

static void mktwiddle(enum wakefulness wakefulness, P *p) {
    INT i;
    INT n = p->n, nb = p->nb;
    float *w, *W;
    E nbf = (E) nb;

    p->w = w = (float *) MALLOC(2 * n * sizeof (float), TWIDDLES);
    p->W = W = (float *) MALLOC(2 * nb * sizeof (float), TWIDDLES);

    bluestein_sequence(wakefulness, n, w);

    for (i = 0; i < nb; ++i)
        W[2 * i] = W[2 * i + 1] = K(0.0);

    W[0] = w[0] / nbf;
    W[1] = w[1] / nbf;

    for (i = 1; i < n; ++i) {
        W[2 * i] = W[2 * (nb - i)] = w[2 * i] / nbf;
        W[2 * i + 1] = W[2 * (nb - i) + 1] = w[2 * i + 1] / nbf;
    }

    {
        plan_dft *cldf = (plan_dft *) p->cldf;
        /* cldf must be awake */
        cldf->apply(p->cldf, W, W + 1, W, W + 1);
    }
}

static void apply(const plan *ego_, float *ri, float *ii, float *ro, float *io) {
    const P *ego = (const P *) ego_;
    INT i, n = ego->n, nb = ego->nb, is = ego->is, os = ego->os;
    float *w = ego->w, *W = ego->W;
    float *b = (float *) MALLOC(2 * nb * sizeof (float), BUFFERS);

    /* multiply input by conjugate bluestein sequence */
    for (i = 0; i < n; ++i) {
        E xr = ri[i * is], xi = ii[i * is];
        E wr = w[2 * i], wi = w[2 * i + 1];
        b[2 * i] = xr * wr + xi * wi;
        b[2 * i + 1] = xi * wr - xr * wi;
    }

    for (; i < nb; ++i) b[2 * i] = b[2 * i + 1] = K(0.0);

    /* convolution: FFT */
    {
        plan_dft *cldf = (plan_dft *) ego->cldf;
        cldf->apply(ego->cldf, b, b + 1, b, b + 1);
    }

    /* convolution: pointwise multiplication */
    for (i = 0; i < nb; ++i) {
        E xr = b[2 * i], xi = b[2 * i + 1];
        E wr = W[2 * i], wi = W[2 * i + 1];
        b[2 * i] = xi * wr + xr * wi;
        b[2 * i + 1] = xr * wr - xi * wi;
    }

    /* convolution: IFFT by FFT with real/imag input/output swapped */
    {
        plan_dft *cldf = (plan_dft *) ego->cldf;
        cldf->apply(ego->cldf, b, b + 1, b, b + 1);
    }

    /* multiply output by conjugate bluestein sequence */
    for (i = 0; i < n; ++i) {
        E xi = b[2 * i], xr = b[2 * i + 1];
        E wr = w[2 * i], wi = w[2 * i + 1];
        ro[i * os] = xr * wr + xi * wi;
        io[i * os] = xi * wr - xr * wi;
    }

    fftwf_ifree(b);
}

static void awake(plan *ego_, enum wakefulness wakefulness) {
    P *ego = (P *) ego_;

    fftwf_plan_awake(ego->cldf, wakefulness);

    switch (wakefulness) {
        case SLEEPY:
            fftwf_ifree0(ego->w);
            ego->w = 0;
            fftwf_ifree0(ego->W);
            ego->W = 0;
            break;
        default:
            A(!ego->w);
            mktwiddle(wakefulness, ego);
            break;
    }
}

static int applicable0(const problem *p_) {
    const problem_dft *p = (const problem_dft *) p_;
    return (1
            && p->sz->rnk == 1
            && p->vecsz->rnk == 0
            /* FIXME: allow other sizes */
            && fftwf_is_prime(p->sz->dims[0].n)

            /* FIXME: infinite recursion of bluestein with itself */
            && p->sz->dims[0].n > 16
            );
}

static int applicable(const solver *ego, const problem *p_,
        const planner *plnr) {
    UNUSED(ego);
    if (NO_SLOWP(plnr)) return 0;
    if (!applicable0(p_)) return 0;
    return 1;
}

static void destroy(plan *ego_) {
    P *ego = (P *) ego_;
    fftwf_plan_destroy_internal(ego->cldf);
}

static void print(const plan *ego_, printer *p) {
    const P *ego = (const P *) ego_;
    p->print(p, "(dft-bluestein-%D/%D%(%p%))",
            ego->n, ego->nb, ego->cldf);
}

static INT choose_transform_size(INT minsz) {
    static const INT primes[] = {2, 3, 5, 0};
    while (!fftwf_factors_into(minsz, primes))
        ++minsz;
    return minsz;
}

static plan *mkplan(const solver *ego, const problem *p_, planner *plnr) {
    const problem_dft *p = (const problem_dft *) p_;
    P *pln;
    INT n, nb;
    plan *cldf = 0;
    float *buf = (float *) 0;

    static const plan_adt padt = {
        fftwf_dft_solve, awake, print, destroy
    };

    if (!applicable(ego, p_, plnr))
        return (plan *) 0;

    n = p->sz->dims[0].n;
    nb = choose_transform_size(2 * n - 1);
    buf = (float*) MALLOC(2 * nb * sizeof (float), BUFFERS);

    cldf = fftwf_mkplan_f_d(plnr,
            fftwf_mkproblem_dft_d(fftwf_mktensor_1d(nb, 2, 2),
            fftwf_mktensor_1d(1, 0, 0),
            buf, buf + 1,
            buf, buf + 1),
            NO_SLOW, 0, 0);
    if (!cldf) goto nada;

    fftwf_ifree(buf);

    pln = MKPLAN_DFT(P, &padt, apply);

    pln->n = n;
    pln->nb = nb;
    pln->w = 0;
    pln->W = 0;
    pln->cldf = cldf;
    pln->is = p->sz->dims[0].is;
    pln->os = p->sz->dims[0].os;

    fftwf_ops_add(&cldf->ops, &cldf->ops, &pln->super.super.ops);
    pln->super.super.ops.add += 4 * n + 2 * nb;
    pln->super.super.ops.mul += 8 * n + 4 * nb;
    pln->super.super.ops.other += 6 * (n + nb);

    return &(pln->super.super);

nada:
    fftwf_ifree0(buf);
    fftwf_plan_destroy_internal(cldf);
    return (plan *) 0;
}

static solver *mksolver(void) {
    static const solver_adt sadt = {PROBLEM_DFT, mkplan, 0};
    S *slv = MKSOLVER(S, &sadt);
    return &(slv->super);
}

void fftwf_dft_bluestein_register(planner *p) {
    REGISTER_SOLVER(p, mksolver());
}

