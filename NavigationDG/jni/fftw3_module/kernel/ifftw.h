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


/* FFTW internal header file */
#ifndef __IFFTW_H__
#define __IFFTW_H__

#include "../config.h"

#include <stdlib.h>		/* size_t */
#include <stdarg.h>		/* va_list */
#include <stddef.h>             /* ptrdiff_t */

#if HAVE_SYS_TYPES_H
# include <sys/types.h>
#endif

#if HAVE_STDINT_H
# include <stdint.h>             /* uintptr_t, maybe */
#endif

#if HAVE_INTTYPES_H
# include <inttypes.h>           /* uintptr_t, maybe */
#endif

/* Windows annoyances -- since tests/hook.c uses some internal
   FFTW functions, we need to given them the dllexport attribute
   under Windows when compiling as a DLL (see api/fftw3.h). */
#if defined(FFTW_EXTERN)
#  define IFFTW_EXTERN FFTW_EXTERN
#elif (defined(FFTW_DLL) || defined(DLL_EXPORT)) \
 && (defined(_WIN32) || defined(__WIN32__))
#  define IFFTW_EXTERN extern __declspec(dllexport)
#else
#  define IFFTW_EXTERN extern
#endif

/*
  integral type large enough to contain a stride (what ``int'' should
  have been in the first place.
*/
typedef ptrdiff_t INT;

/* dummy use of unused parameters to silence compiler warnings */
#define UNUSED(x) (void)x

#define NELEM(array) ((int) (sizeof(array) / sizeof((array)[0])))

#define FFT_SIGN (-1)  /* sign convention for forward transforms */
extern void fftwf_extract_reim(int sign, float *c, float **r, float **i);

#define REGISTER_SOLVER(p, s) fftwf_solver_register(p, s)

#define STRINGIZEx(x) #x
#define STRINGIZE(x) STRINGIZEx(x)

#if defined(HAVE_SSE) || defined(HAVE_SSE2) || defined(HAVE_ALTIVEC) || \
    defined(HAVE_MIPS_PS)
#define HAVE_SIMD 1
#else
#define HAVE_SIMD 0
#endif

/* forward declarations */
typedef struct problem_s problem;
typedef struct plan_s plan;
typedef struct solver_s solver;
typedef struct planner_s planner;
typedef struct printer_s printer;
typedef struct scanner_s scanner;

/*-----------------------------------------------------------------------*/
/* alloca: */
#if HAVE_SIMD || HAVE_CELL
#define MIN_ALIGNMENT 16
#endif

#if defined(HAVE_ALLOCA) && defined(FFTW_ENABLE_ALLOCA)
   /* use alloca if available */

#ifndef alloca
#ifdef __GNUC__
# define alloca __builtin_alloca
#else
# ifdef _MSC_VER
#  include <malloc.h>
#  define alloca _alloca
# else
#  if HAVE_ALLOCA_H
#   include <alloca.h>
#  else
#   ifdef _AIX
 #pragma alloca
#   else
#    ifndef alloca /* predefined by HP cc +Olibcalls */
void *alloca(size_t);
#    endif
#   endif
#  endif
# endif
#endif
#endif

#  ifdef MIN_ALIGNMENT
#    define STACK_MALLOC(T, p, x)				\
     {								\
         p = (T)alloca((x) + MIN_ALIGNMENT);			\
         p = (T)(((uintptr_t)p + (MIN_ALIGNMENT - 1)) &	\
               (~(uintptr_t)(MIN_ALIGNMENT - 1)));		\
     }
#    define STACK_FREE(x)
#  else /* HAVE_ALLOCA && !defined(MIN_ALIGNMENT) */
#    define STACK_MALLOC(T, p, x) p = (T)alloca(x)
#    define STACK_FREE(x)
#  endif

#else /* ! HAVE_ALLOCA */
   /* use malloc instead of alloca */
#  define STACK_MALLOC(T, p, x) p = (T)MALLOC(x, OTHER)
#  define STACK_FREE(x) fftwf_ifree(x)
#endif /* ! HAVE_ALLOCA */

/*-----------------------------------------------------------------------*/
/* define uintptr_t if it is not already defined */

#ifndef HAVE_UINTPTR_T
#  if SIZEOF_VOID_P == 0
#    error sizeof void* is unknown!
#  elif SIZEOF_UNSIGNED_INT == SIZEOF_VOID_P
     typedef unsigned int uintptr_t;
#  elif SIZEOF_UNSIGNED_LONG == SIZEOF_VOID_P
     typedef unsigned long uintptr_t;
#  elif SIZEOF_UNSIGNED_LONG_LONG == SIZEOF_VOID_P
     typedef unsigned long long uintptr_t;
#  else
#    error no unsigned integer type matches void* sizeof!
#  endif
#endif

/*-----------------------------------------------------------------------*/
/* We can do an optimization for copying pairs of (aligned) floats
   when in single precision if 2*float = double. */

#define FFTW_2R_IS_DOUBLE (defined(FFTW_SINGLE) \
                           && SIZEOF_FLOAT != 0 \
                           && SIZEOF_DOUBLE == 2*SIZEOF_FLOAT)

#define DOUBLE_ALIGNED(p) ((((uintptr_t)(p)) % sizeof(double)) == 0)

/*-----------------------------------------------------------------------*/
/* assert.c: */
IFFTW_EXTERN void fftwf_assertion_failed(const char *s,
				      int line, const char *file);

/* always check */
#define CK(ex)						 \
      (void)((ex) || (fftwf_assertion_failed(#ex, __LINE__, __FILE__), 0))

#ifdef FFTW_DEBUG
/* check only if debug enabled */
#define A(ex)						 \
      (void)((ex) || (fftwf_assertion_failed(#ex, __LINE__, __FILE__), 0))
#else
#define A(ex) /* nothing */
#endif

extern void fftwf_debug(const char *format, ...);
#define D fftwf_debug

/*-----------------------------------------------------------------------*/
/* kalloc.c: */
extern void *fftwf_kernel_malloc(size_t n);
extern void fftwf_kernel_free(void *p);

/*-----------------------------------------------------------------------*/
/* alloc.c: */

/* objects allocated by malloc, for statistical purposes */
enum malloc_tag {
     EVERYTHING,
     PLANS,
     SOLVERS,
     PROBLEMS,
     BUFFERS,
     HASHT,
     TENSORS,
     PLANNERS,
     SLVDESCS,
     TWIDDLES,
     STRIDES,
     OTHER,
     MALLOC_WHAT_LAST		/* must be last */
};

IFFTW_EXTERN void fftwf_ifree(void *ptr);
extern void fftwf_ifree0(void *ptr);

#ifdef FFTW_DEBUG_MALLOC

IFFTW_EXTERN void *fftwf_malloc_debug(size_t n, enum malloc_tag what,
			     const char *file, int line);
#define MALLOC(n, what) fftwf_malloc_debug(n, what, __FILE__, __LINE__)
#define NATIVE_MALLOC(n, what) MALLOC(n, what)
IFFTW_EXTERN void fftwf_malloc_print_minfo(int vrbose);

#else /* ! FFTW_DEBUG_MALLOC */

IFFTW_EXTERN void *fftwf_malloc_plain(size_t sz);
#define MALLOC(n, what)  fftwf_malloc_plain(n)
#define NATIVE_MALLOC(n, what) malloc(n)

#endif

#if defined(FFTW_DEBUG) && defined(FFTW_DEBUG_MALLOC) && (defined(HAVE_THREADS) || defined(HAVE_OPENMP))
extern int fftwf_in_thread;
#  define IN_THREAD fftwf_in_thread
#  define THREAD_ON { int in_thread_save = fftwf_in_thread; fftwf_in_thread = 1
#  define THREAD_OFF fftwf_in_thread = in_thread_save; }
#else
#  define IN_THREAD 0
#  define THREAD_ON
#  define THREAD_OFF
#endif

/*-----------------------------------------------------------------------*/
/* low-resolution clock */

#ifdef FAKE_CRUDE_TIME
 typedef int crude_time;
#else
# if TIME_WITH_SYS_TIME
#  include <sys/time.h>
#  include <time.h>
# else
#  if HAVE_SYS_TIME_H
#   include <sys/time.h>
#  else
#   include <time.h>
#  endif
# endif

# ifdef HAVE_BSDGETTIMEOFDAY
# ifndef HAVE_GETTIMEOFDAY
# define gettimeofday BSDgettimeofday
# define HAVE_GETTIMEOFDAY 1
# endif
# endif

# if defined(HAVE_GETTIMEOFDAY)
   typedef struct timeval crude_time;
# else
   typedef clock_t crude_time;
# endif
#endif /* else FAKE_CRUDE_TIME */

crude_time fftwf_get_crude_time(void);
double fftwf_elapsed_since(const planner *plnr, const problem *p,
			crude_time t0); /* time in seconds since t0 */

/*-----------------------------------------------------------------------*/
/* ops.c: */
/*
 * ops counter.  The total number of additions is add + fma
 * and the total number of multiplications is mul + fma.
 * Total flops = add + mul + 2 * fma
 */
typedef struct {
     double add;
     double mul;
     double fma;
     double other;
} opcnt;

void fftwf_ops_zero(opcnt *dst);
void fftwf_ops_other(INT o, opcnt *dst);
void fftwf_ops_cpy(const opcnt *src, opcnt *dst);

void fftwf_ops_add(const opcnt *a, const opcnt *b, opcnt *dst);
void fftwf_ops_add2(const opcnt *a, opcnt *dst);

/* dst = m * a + b */
void fftwf_ops_madd(INT m, const opcnt *a, const opcnt *b, opcnt *dst);

/* dst += m * a */
void fftwf_ops_madd2(INT m, const opcnt *a, opcnt *dst);


/*-----------------------------------------------------------------------*/
/* minmax.c: */
INT fftwf_imax(INT a, INT b);
INT fftwf_imin(INT a, INT b);

/*-----------------------------------------------------------------------*/
/* iabs.c: */
INT fftwf_iabs(INT a);

/* inline version */
#define IABS(x) (((x) < 0) ? (0 - (x)) : (x))

/*-----------------------------------------------------------------------*/
/* md5.c */

#if SIZEOF_UNSIGNED_INT >= 4
typedef unsigned int md5uint;
#else
typedef unsigned long md5uint; /* at least 32 bits as per C standard */
#endif

typedef md5uint md5sig[4];

typedef struct {
     md5sig s; /* state and signature */

     /* fields not meant to be used outside md5.c: */
     unsigned char c[64]; /* stuff not yet processed */
     unsigned l;  /* total length.  Should be 64 bits long, but this is
		     good enough for us */
} md5;

void fftwf_md5begin(md5 *p);
void fftwf_md5putb(md5 *p, const void *d_, size_t len);
void fftwf_md5puts(md5 *p, const char *s);
void fftwf_md5putc(md5 *p, unsigned char c);
void fftwf_md5int(md5 *p, int i);
void fftwf_md5INT(md5 *p, INT i);
void fftwf_md5unsigned(md5 *p, unsigned i);
void fftwf_md5end(md5 *p);

/*-----------------------------------------------------------------------*/
/* tensor.c: */
#define STRUCT_HACK_KR
#undef STRUCT_HACK_C99

typedef struct {
     INT n;
     INT is;			/* input stride */
     INT os;			/* output stride */
} iodim;

typedef struct {
     int rnk;
#if defined(STRUCT_HACK_KR)
     iodim dims[1];
#elif defined(STRUCT_HACK_C99)
     iodim dims[];
#else
     iodim *dims;
#endif
} tensor;

/*
  Definition of rank -infinity.
  This definition has the property that if you want rank 0 or 1,
  you can simply test for rank <= 1.  This is a common case.

  A tensor of rank -infinity has size 0.
*/
#define RNK_MINFTY  ((int)(((unsigned) -1) >> 1))
#define FINITE_RNK(rnk) ((rnk) != RNK_MINFTY)

typedef enum { INPLACE_IS, INPLACE_OS } inplace_kind;

tensor *fftwf_mktensor(int rnk);
tensor *fftwf_mktensor_0d(void);
tensor *fftwf_mktensor_1d(INT n, INT is, INT os);
tensor *fftwf_mktensor_2d(INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1);
tensor *fftwf_mktensor_3d(INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1,
		       INT n2, INT is2, INT os2);
tensor *fftwf_mktensor_4d(INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1,
		       INT n2, INT is2, INT os2,
		       INT n3, INT is3, INT os3);
tensor *fftwf_mktensor_5d(INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1,
		       INT n2, INT is2, INT os2,
		       INT n3, INT is3, INT os3,
		       INT n4, INT is4, INT os4);
INT fftwf_tensor_sz(const tensor *sz);
void fftwf_tensor_md5(md5 *p, const tensor *t);
INT fftwf_tensor_max_index(const tensor *sz);
INT fftwf_tensor_min_istride(const tensor *sz);
INT fftwf_tensor_min_ostride(const tensor *sz);
INT fftwf_tensor_min_stride(const tensor *sz);
int fftwf_tensor_inplace_strides(const tensor *sz);
int fftwf_tensor_inplace_strides2(const tensor *a, const tensor *b);
int fftwf_tensor_strides_decrease(const tensor *sz, const tensor *vecsz,
                               inplace_kind k);
tensor *fftwf_tensor_copy(const tensor *sz);
int fftwf_tensor_kosherp(const tensor *x);

tensor *fftwf_tensor_copy_inplace(const tensor *sz, inplace_kind k);
tensor *fftwf_tensor_copy_except(const tensor *sz, int except_dim);
tensor *fftwf_tensor_copy_sub(const tensor *sz, int start_dim, int rnk);
tensor *fftwf_tensor_compress(const tensor *sz);
tensor *fftwf_tensor_compress_contiguous(const tensor *sz);
tensor *fftwf_tensor_append(const tensor *a, const tensor *b);
void fftwf_tensor_split(const tensor *sz, tensor **a, int a_rnk, tensor **b);
int fftwf_tensor_tornk1(const tensor *t, INT *n, INT *is, INT *os);
void fftwf_tensor_destroy(tensor *sz);
void fftwf_tensor_destroy2(tensor *a, tensor *b);
void fftwf_tensor_destroy4(tensor *a, tensor *b, tensor *c, tensor *d);
void fftwf_tensor_print(const tensor *sz, printer *p);
int fftwf_dimcmp(const iodim *a, const iodim *b);
int fftwf_tensor_equal(const tensor *a, const tensor *b);
int fftwf_tensor_inplace_locations(const tensor *sz, const tensor *vecsz);

/*-----------------------------------------------------------------------*/
/* problem.c: */
enum {
     /* a problem that cannot be solved */
     PROBLEM_UNSOLVABLE,

     PROBLEM_DFT,
     PROBLEM_RDFT,
     PROBLEM_RDFT2,

     /* for mpi/ subdirectory */
     PROBLEM_MPI_DFT,
     PROBLEM_MPI_RDFT,
     PROBLEM_MPI_RDFT2,
     PROBLEM_MPI_TRANSPOSE,

     PROBLEM_LAST
};

typedef struct {
     int problem_kind;
     void (*hash) (const problem *ego, md5 *p);
     void (*zero) (const problem *ego);
     void (*print) (const problem *ego, printer *p);
     void (*destroy) (problem *ego);
} problem_adt;

struct problem_s {
     const problem_adt *adt;
};

problem *fftwf_mkproblem(size_t sz, const problem_adt *adt);
void fftwf_problem_destroy(problem *ego);
problem *fftwf_mkproblem_unsolvable(void);

/*-----------------------------------------------------------------------*/
/* print.c */
struct printer_s {
     void (*print)(printer *p, const char *format, ...);
     void (*vprint)(printer *p, const char *format, va_list ap);
     void (*putchr)(printer *p, char c);
     void (*cleanup)(printer *p);
     int indent;
     int indent_incr;
};

printer *fftwf_mkprinter(size_t size,
		      void (*putchr)(printer *p, char c),
		      void (*cleanup)(printer *p));
IFFTW_EXTERN void fftwf_printer_destroy(printer *p);

/*-----------------------------------------------------------------------*/
/* scan.c */
struct scanner_s {
     int (*scan)(scanner *sc, const char *format, ...);
     int (*vscan)(scanner *sc, const char *format, va_list ap);
     int (*getchr)(scanner *sc);
     int ungotc;
};

scanner *fftwf_mkscanner(size_t size, int (*getchr)(scanner *sc));
void fftwf_scanner_destroy(scanner *sc);

/*-----------------------------------------------------------------------*/
/* plan.c: */

enum wakefulness {
     SLEEPY,
     AWAKE_ZERO,
     AWAKE_SQRTN_TABLE,
     AWAKE_SINCOS
};

typedef struct {
     void (*solve)(const plan *ego, const problem *p);
     void (*awake)(plan *ego, enum wakefulness wakefulness);
     void (*print)(const plan *ego, printer *p);
     void (*destroy)(plan *ego);
} plan_adt;

struct plan_s {
     const plan_adt *adt;
     opcnt ops;
     double pcost;
     enum wakefulness wakefulness; /* used for debugging only */
     int could_prune_now_p;
};

plan *fftwf_mkplan(size_t size, const plan_adt *adt);
void fftwf_plan_destroy_internal(plan *ego);
IFFTW_EXTERN void fftwf_plan_awake(plan *ego, enum wakefulness wakefulness);
void fftwf_plan_null_destroy(plan *ego);

/*-----------------------------------------------------------------------*/
/* solver.c: */
typedef struct {
     int problem_kind;
     plan *(*mkplan)(const solver *ego, const problem *p, planner *plnr);
     void (*destroy)(solver *ego);
} solver_adt;

struct solver_s {
     const solver_adt *adt;
     int refcnt;
};

solver *fftwf_mksolver(size_t size, const solver_adt *adt);
void fftwf_solver_use(solver *ego);
void fftwf_solver_destroy(solver *ego);
void fftwf_solver_register(planner *plnr, solver *s);

/* shorthand */
#define MKSOLVER(type, adt) (type *)fftwf_mksolver(sizeof(type), adt)

/*-----------------------------------------------------------------------*/
/* planner.c */

typedef struct slvdesc_s {
     solver *slv;
     const char *reg_nam;
     unsigned nam_hash;
     int reg_id;
     int next_for_same_problem_kind;
} slvdesc;

typedef struct solution_s solution; /* opaque */

/* interpretation of L and U:

   - if it returns a plan, the planner guarantees that all applicable
     plans at least as impatient as U have been tried, and that each
     plan in the solution is at least as impatient as L.

   - if it returns 0, the planner guarantees to have tried all solvers
     at least as impatient as L, and that none of them was applicable.

   The structure is packed to fit into 64 bits.
*/

typedef struct {
     unsigned l:20;
     unsigned hash_info:3;
#    define BITS_FOR_TIMELIMIT 9
     unsigned timelimit_impatience:BITS_FOR_TIMELIMIT;
     unsigned u:20;

     /* abstraction break: we store the solver here to pad the
	structure to 64 bits.  Otherwise, the struct is padded to 64
	bits anyway, and another word is allocated for slvndx. */
#    define BITS_FOR_SLVNDX 12
     unsigned slvndx:BITS_FOR_SLVNDX;
} flags_t;

/* impatience flags  */
enum {
     BELIEVE_PCOST = 0x0001,
     ESTIMATE = 0x0002,
     NO_DFT_R2HC = 0x0004,
     NO_SLOW = 0x0008,
     NO_VRECURSE = 0x0010,
     NO_INDIRECT_OP = 0x0020,
     NO_LARGE_GENERIC = 0x0040,
     NO_RANK_SPLITS = 0x0080,
     NO_VRANK_SPLITS = 0x0100,
     NO_NONTHREADED = 0x0200,
     NO_BUFFERING = 0x0400,
     NO_FIXED_RADIX_LARGE_N = 0x0800,
     NO_DESTROY_INPUT = 0x1000,
     NO_SIMD = 0x2000,
     CONSERVE_MEMORY = 0x4000,
     NO_DHT_R2HC = 0x8000,
     NO_UGLY = 0x10000,
     ALLOW_PRUNING = 0x20000
};

/* hashtable information */
enum {
     BLESSING = 0x1,   /* save this entry */
     H_VALID = 0x2,    /* valid hastable entry */
     H_LIVE = 0x4      /* entry is nonempty, implies H_VALID */
};

#define PLNR_L(plnr) ((plnr)->flags.l)
#define PLNR_U(plnr) ((plnr)->flags.u)
#define PLNR_TIMELIMIT_IMPATIENCE(plnr) ((plnr)->flags.timelimit_impatience)

#define ESTIMATEP(plnr) (PLNR_U(plnr) & ESTIMATE)
#define BELIEVE_PCOSTP(plnr) (PLNR_U(plnr) & BELIEVE_PCOST)
#define ALLOW_PRUNINGP(plnr) (PLNR_U(plnr) & ALLOW_PRUNING)

#define NO_INDIRECT_OP_P(plnr) (PLNR_L(plnr) & NO_INDIRECT_OP)
#define NO_LARGE_GENERICP(plnr) (PLNR_L(plnr) & NO_LARGE_GENERIC)
#define NO_RANK_SPLITSP(plnr) (PLNR_L(plnr) & NO_RANK_SPLITS)
#define NO_VRANK_SPLITSP(plnr) (PLNR_L(plnr) & NO_VRANK_SPLITS)
#define NO_VRECURSEP(plnr) (PLNR_L(plnr) & NO_VRECURSE)
#define NO_DFT_R2HCP(plnr) (PLNR_L(plnr) & NO_DFT_R2HC)
#define NO_SLOWP(plnr) (PLNR_L(plnr) & NO_SLOW)
#define NO_UGLYP(plnr) (PLNR_L(plnr) & NO_UGLY)
#define NO_FIXED_RADIX_LARGE_NP(plnr) \
  (PLNR_L(plnr) & NO_FIXED_RADIX_LARGE_N)
#define NO_NONTHREADEDP(plnr) \
  ((PLNR_L(plnr) & NO_NONTHREADED) && (plnr)->nthr > 1)

#define NO_DESTROY_INPUTP(plnr) (PLNR_L(plnr) & NO_DESTROY_INPUT)
#define NO_SIMDP(plnr) (PLNR_L(plnr) & NO_SIMD)
#define CONSERVE_MEMORYP(plnr) (PLNR_L(plnr) & CONSERVE_MEMORY)
#define NO_DHT_R2HCP(plnr) (PLNR_L(plnr) & NO_DHT_R2HC)
#define NO_BUFFERINGP(plnr) (PLNR_L(plnr) & NO_BUFFERING)

typedef enum { FORGET_ACCURSED, FORGET_EVERYTHING } amnesia;

typedef enum {
     /* WISDOM_NORMAL: planner may or may not use wisdom */
     WISDOM_NORMAL,

     /* WISDOM_ONLY: planner must use wisdom and must avoid searching */
     WISDOM_ONLY,

     /* WISDOM_IS_BOGUS: planner must return 0 as quickly as possible */
     WISDOM_IS_BOGUS,

     /* WISDOM_IGNORE_INFEASIBLE: planner ignores infeasible wisdom */
     WISDOM_IGNORE_INFEASIBLE,

     /* WISDOM_IGNORE_ALL: planner ignores all */
     WISDOM_IGNORE_ALL
} wisdom_state_t;

typedef struct {
     void (*register_solver)(planner *ego, solver *s);
     plan *(*mkplan)(planner *ego, const problem *p);
     void (*forget)(planner *ego, amnesia a);
     void (*exprt)(planner *ego, printer *p); /* ``export'' is a reserved
						 word in C++. */
     int (*imprt)(planner *ego, scanner *sc);
} planner_adt;

/* hash table of solutions */
typedef struct {
     solution *solutions;
     unsigned hashsiz, nelem;

     /* statistics */
     int lookup, succ_lookup, lookup_iter;
     int insert, insert_iter, insert_unknown;
     int nrehash;
} hashtab;

typedef enum { COST_SUM, COST_MAX } cost_kind;

struct planner_s {
     const planner_adt *adt;
     void (*hook)(struct planner_s *plnr, plan *pln,
		  const problem *p, int optimalp);
     double (*cost_hook)(const problem *p, double t, cost_kind k);

     /* solver descriptors */
     slvdesc *slvdescs;
     unsigned nslvdesc, slvdescsiz;
     const char *cur_reg_nam;
     int cur_reg_id;
     int slvdescs_for_problem_kind[PROBLEM_LAST];

     wisdom_state_t wisdom_state;

     hashtab htab_blessed;
     hashtab htab_unblessed;

     int nthr;
     flags_t flags;

     crude_time start_time;
     double timelimit; /* elapsed_since(start_time) at which to bail out */
     int timed_out; /* whether most recent search timed out */
     int need_timeout_check;

     /* various statistics */
     int nplan;    /* number of plans evaluated */
     double pcost, epcost; /* total pcost of measured/estimated plans */
     int nprob;    /* number of problems evaluated */
};

planner *fftwf_mkplanner(void);
void fftwf_planner_destroy(planner *ego);

/*
  Iterate over all solvers.   Read:

  @article{ baker93iterators,
  author = "Henry G. Baker, Jr.",
  title = "Iterators: Signs of Weakness in Object-Oriented Languages",
  journal = "{ACM} {OOPS} Messenger",
  volume = "4",
  number = "3",
  pages = "18--25"
  }
*/
#define FORALL_SOLVERS(ego, s, p, what)			\
{							\
     unsigned _cnt;					\
     for (_cnt = 0; _cnt < ego->nslvdesc; ++_cnt) {	\
	  slvdesc *p = ego->slvdescs + _cnt;		\
	  solver *s = p->slv;				\
	  what;						\
     }							\
}

#define FORALL_SOLVERS_OF_KIND(kind, ego, s, p, what)		\
{								\
     int _cnt = ego->slvdescs_for_problem_kind[kind]; 		\
     while (_cnt >= 0) {					\
	  slvdesc *p = ego->slvdescs + _cnt;			\
	  solver *s = p->slv;					\
	  what;							\
	  _cnt = p->next_for_same_problem_kind;			\
     }								\
}


/* make plan, destroy problem */
plan *fftwf_mkplan_d(planner *ego, problem *p);
plan *fftwf_mkplan_f_d(planner *ego, problem *p,
		    unsigned l_set, unsigned u_set, unsigned u_reset);

/*-----------------------------------------------------------------------*/
/* stride.c: */

/* If PRECOMPUTE_ARRAY_INDICES is defined, precompute all strides. */
#if (defined(__i386__) || defined(__x86_64__) || _M_IX86 >= 500) && !defined(FFTW_LDOUBLE)
#define PRECOMPUTE_ARRAY_INDICES
#endif

extern const INT fftwf_an_INT_guaranteed_to_be_zero;

#ifdef PRECOMPUTE_ARRAY_INDICES
typedef INT *stride;
#define WS(stride, i)  (stride[i])
extern stride fftwf_mkstride(INT n, INT s);
void fftwf_stride_destroy(stride p);
/* hackery to prevent the compiler from copying the strides array
   onto the stack */
#define MAKE_VOLATILE_STRIDE(x) (x) = (x) + fftwf_an_INT_guaranteed_to_be_zero
#else

typedef INT stride;
#define WS(stride, i)  (stride * i)
#define fftwf_mkstride(n, stride) stride
#define fftw_mkstride(n, stride) stride
#define fftwl_mkstride(n, stride) stride
#define fftwf_stride_destroy(p) ((void) p)
#define fftw_stride_destroy(p) ((void) p)
#define fftwl_stride_destroy(p) ((void) p)

/* hackery to prevent the compiler from ``optimizing'' induction
   variables in codelet loops. */
#define MAKE_VOLATILE_STRIDE(x) (x) = (x) ^ fftwf_an_INT_guaranteed_to_be_zero

#endif /* PRECOMPUTE_ARRAY_INDICES */

/*-----------------------------------------------------------------------*/
/* solvtab.c */

struct solvtab_s { void (*reg)(planner *); const char *reg_nam; };
typedef struct solvtab_s solvtab[];
void fftwf_solvtab_exec(const solvtab tbl, planner *p);
#define SOLVTAB(s) { s, STRINGIZE(s) }
#define SOLVTAB_END { 0, 0 }

/*-----------------------------------------------------------------------*/
/* pickdim.c */
int fftwf_pickdim(int which_dim, const int *buddies, int nbuddies,
	       const tensor *sz, int oop, int *dp);

/*-----------------------------------------------------------------------*/
/* twiddle.c */
/* little language to express twiddle factors computation */
enum { TW_COS = 0, TW_SIN = 1, TW_CEXP = 2, TW_NEXT = 3,
       TW_FULL = 4, TW_HALF = 5 };

typedef struct {
     unsigned char op;
     signed char v;
     short i;
} tw_instr;

typedef struct twid_s {
     float *W;                     /* array of twiddle factors */
     INT n, r, m;                /* transform order, radix, # twiddle rows */
     int refcnt;
     const tw_instr *instr;
     struct twid_s *cdr;
     enum wakefulness wakefulness;
} twid;

INT fftwf_twiddle_length(INT r, const tw_instr *p);
void fftwf_twiddle_awake(enum wakefulness wakefulness,
		      twid **pp, const tw_instr *instr, INT n, INT r, INT m);

/*-----------------------------------------------------------------------*/
/* trig.c */
#ifdef TRIGREAL_IS_LONG_DOUBLE
   typedef long double trigreal;
#else
   typedef double trigreal;
#endif

typedef struct triggen_s triggen;

struct triggen_s {
     void (*cexp)(triggen *t, INT m, float *result);
     void (*cexpl)(triggen *t, INT m, trigreal *result);
     void (*rotate)(triggen *p, INT m, float xr, float xi, float *res);

     INT twshft;
     INT twradix;
     INT twmsk;
     trigreal *W0, *W1;
     INT n;
};

triggen *fftwf_mktriggen(enum wakefulness wakefulness, INT n);
void fftwf_triggen_destroy(triggen *p);

/*-----------------------------------------------------------------------*/
/* primes.c: */

#define MULMOD(x, y, p) \
   (((x) <= 92681 - (y)) ? ((x) * (y)) % (p) : fftwf_safe_mulmod(x, y, p))

INT fftwf_safe_mulmod(INT x, INT y, INT p);
INT fftwf_power_mod(INT n, INT m, INT p);
INT fftwf_find_generator(INT p);
INT fftwf_first_divisor(INT n);
int fftwf_is_prime(INT n);
INT fftwf_next_prime(INT n);
int fftwf_factors_into(INT n, const INT *primes);
INT fftwf_choose_radix(INT r, INT n);
INT fftwf_isqrt(INT n);
INT fftwf_modulo(INT a, INT n);

#define GENERIC_MIN_BAD 173 /* min prime for which generic becomes bad */

/*-----------------------------------------------------------------------*/
/* rader.c: */
typedef struct rader_tls rader_tl;

void fftwf_rader_tl_insert(INT k1, INT k2, INT k3, float *W, rader_tl **tl);
float *fftwf_rader_tl_find(INT k1, INT k2, INT k3, rader_tl *t);
void fftwf_rader_tl_delete(float *W, rader_tl **tl);

/*-----------------------------------------------------------------------*/
/* copy/transposition routines */

/* lower bound to the cache size, for tiled routines */
#define CACHESIZE 8192

INT fftwf_compute_tilesz(INT vl, int how_many_tiles_in_cache);

void fftwf_tile2d(INT n0l, INT n0u, INT n1l, INT n1u, INT tilesz,
	       void (*f)(INT n0l, INT n0u, INT n1l, INT n1u, void *args),
	       void *args);
void fftwf_cpy1d(float *I, float *O, INT n0, INT is0, INT os0, INT vl);
void fftwf_cpy2d(float *I, float *O,
	      INT n0, INT is0, INT os0,
	      INT n1, INT is1, INT os1,
	      INT vl);
void fftwf_cpy2d_ci(float *I, float *O,
		 INT n0, INT is0, INT os0,
		 INT n1, INT is1, INT os1,
		 INT vl);
void fftwf_cpy2d_co(float *I, float *O,
		 INT n0, INT is0, INT os0,
		 INT n1, INT is1, INT os1,
		 INT vl);
void fftwf_cpy2d_tiled(float *I, float *O,
		    INT n0, INT is0, INT os0,
		    INT n1, INT is1, INT os1,
		    INT vl);
void fftwf_cpy2d_tiledbuf(float *I, float *O,
		       INT n0, INT is0, INT os0,
		       INT n1, INT is1, INT os1,
		       INT vl);
void fftwf_cpy2d_pair(float *I0, float *I1, float *O0, float *O1,
		   INT n0, INT is0, INT os0,
		   INT n1, INT is1, INT os1);
void fftwf_cpy2d_pair_ci(float *I0, float *I1, float *O0, float *O1,
		      INT n0, INT is0, INT os0,
		      INT n1, INT is1, INT os1);
void fftwf_cpy2d_pair_co(float *I0, float *I1, float *O0, float *O1,
		      INT n0, INT is0, INT os0,
		      INT n1, INT is1, INT os1);

void fftwf_transpose(float *I, INT n, INT s0, INT s1, INT vl);
void fftwf_transpose_tiled(float *I, INT n, INT s0, INT s1, INT vl);
void fftwf_transpose_tiledbuf(float *I, INT n, INT s0, INT s1, INT vl);

typedef void (*transpose_func)(float *I, INT n, INT s0, INT s1, INT vl);
typedef void (*cpy2d_func)(float *I, float *O,
			   INT n0, INT is0, INT os0,
			   INT n1, INT is1, INT os1,
			   INT vl);

#if HAVE_CELL
int fftwf_cell_transpose_applicable)(R *I, const iodim *d, INT vl);
void fftwf_cell_transpose)(R *I, INT n, INT s0, INT s1, INT vl);
int fftwf_cell_copy_applicable)(R *I, R *O, const iodim *n, const iodim *v);
void fftwf_cell_copy)(R *I, R *O, const iodim *n, const iodim *v);
#endif

/*-----------------------------------------------------------------------*/
/* misc stuff */
void fftwf_null_awake(plan *ego, enum wakefulness wakefulness);
double fftwf_iestimate_cost(const planner *, const plan *, const problem *);

double fftwf_measure_execution_time(const planner *plnr,
				 plan *pln, const problem *p);
int fftwf_alignment_of(float *p);
unsigned fftwf_hash(const char *s);
INT fftwf_nbuf(INT n, INT vl, INT maxnbuf);
int fftwf_nbuf_redundant(INT n, INT vl, int which,
		      const INT *maxnbuf, int nmaxnbuf);
INT fftwf_bufdist(INT n, INT vl);
int fftwf_toobig(INT n);
int fftwf_ct_uglyp(INT min_n, INT v, INT n, INT r);

#if HAVE_SIMD || HAVE_CELL
R *fftwf_taint)(R *p, INT s);
R *fftwf_join_taint)(R *p1, R *p2);
#define TAINT(p, s) fftwf_taint)(p, s)
#define UNTAINT(p) ((R *) (((uintptr_t) (p)) & ~(uintptr_t)3))
#define TAINTOF(p) (((uintptr_t)(p)) & 3)
#define JOIN_TAINT(p1, p2) fftwf_join_taint)(p1, p2)
#else
#define TAINT(p, s) (p)
#define UNTAINT(p) (p)
#define TAINTOF(p) 0
#define JOIN_TAINT(p1, p2) p1
#endif

#ifdef FFTW_DEBUG_ALIGNMENT
#  define ASSERT_ALIGNED_DOUBLE {		\
     double __foo;				\
     CK(!(((uintptr_t) &__foo) & 0x7));		\
}
#else
#  define ASSERT_ALIGNED_DOUBLE
#endif /* FFTW_DEBUG_ALIGNMENT */



/*-----------------------------------------------------------------------*/
/* macros used in codelets to reduce source code size */

typedef float E;  /* internal precision of codelets. */

#ifdef FFTW_LDOUBLE
#  define K(x) ((E) x##L)
#else
#  define K(x) ((E) x)
#endif
#define DK(name, value) const E name = K(value)

/* FMA macros */

#if defined(__GNUC__) && (defined(__powerpc__) || defined(__ppc__) || defined(_POWER))
/* The obvious expression a * b + c does not work.  If both x = a * b
   + c and y = a * b - c appear in the source, gcc computes t = a * b,
   x = t + c, y = t - c, thus destroying the fma.

   This peculiar coding seems to do the right thing on all of
   gcc-2.95, gcc-3.1, gcc-3.2, and gcc-3.3.  It does the right thing
   on gcc-3.4 -fno-web (because the ``web'' pass splits the variable
   `x' for the single-assignment form).

   However, gcc-4.0 is a formidable adversary which succeeds in
   pessimizing two fma's into one multiplication and two additions.
   It does it very early in the game---before the optimization passes
   even start.  The only real workaround seems to use fake inline asm
   such as

     asm ("# confuse gcc %0" : "=f"(a) : "0"(a));
     return a * b + c;

   in each of the FMA, FMS, FNMA, and FNMS functions.  However, this
   does not solve the problem either, because two equal asm statements
   count as a common subexpression!  One must use *different* fake asm
   statements:

   in FMA:
     asm ("# confuse gcc for fma %0" : "=f"(a) : "0"(a));

   in FMS:
     asm ("# confuse gcc for fms %0" : "=f"(a) : "0"(a));

   etc.

   After these changes, gcc recalcitrantly generates the fma that was
   in the source to begin with.  However, the extra asm() cruft
   confuses other passes of gcc, notably the instruction scheduler.
   (Of course, one could also generate the fma directly via inline
   asm, but this confuses the scheduler even more.)

   Steven and I have submitted more than one bug report to the gcc
   mailing list over the past few years, to no effect.  Thus, I give
   up.  gcc-4.0 can go to hell.  I'll wait at least until gcc-4.3 is
   out before touching this crap again.
*/
static __inline__ E FMA(E a, E b, E c)
{
     E x = a * b;
     x = x + c;
     return x;
}

static __inline__ E FMS(E a, E b, E c)
{
     E x = a * b;
     x = x - c;
     return x;
}

static __inline__ E FNMA(E a, E b, E c)
{
     E x = a * b;
     x = - (x + c);
     return x;
}

static __inline__ E FNMS(E a, E b, E c)
{
     E x = a * b;
     x = - (x - c);
     return x;
}
#else
#define FMA(a, b, c) (((a) * (b)) + (c))
#define FMS(a, b, c) (((a) * (b)) - (c))
#define FNMA(a, b, c) (- (((a) * (b)) + (c)))
#define FNMS(a, b, c) ((c) - ((a) * (b)))
#endif

/* stack-alignment hackery */
#ifdef __ICC /* Intel's compiler for ia32 */
#define WITH_ALIGNED_STACK(what)				\
{								\
     /*								\
      * Simply calling alloca seems to do the right thing.	\
      * The size of the allocated block seems to be irrelevant.	\
      */							\
     _alloca(16);						\
     what							\
}
#endif

#if defined(__GNUC__) && defined(__i386__) && !defined(WITH_ALIGNED_STACK) \
    && !(defined(__MACOSX__) || defined(__APPLE__)) /* OSX ABI is aligned */
/*
 * horrible hack to align the stack to a 16-byte boundary.
 *
 * We assume a gcc version >= 2.95 so that
 * -mpreferred-stack-boundary works.  Otherwise, all bets are
 * off.  However, -mpreferred-stack-boundary does not create a
 * stack alignment, but it only preserves it.  Unfortunately,
 * many versions of libc on linux call main() with the wrong
 * initial stack alignment, with the result that the code is now
 * pessimally aligned instead of having a 50% chance of being
 * correct.
 */

#define WITH_ALIGNED_STACK(what)				\
{								\
     /*								\
      * Use alloca to allocate some memory on the stack.	\
      * This alerts gcc that something funny is going		\
      * on, so that it does not omit the frame pointer		\
      * etc.							\
      */							\
     (void)__builtin_alloca(16);				\
								\
     /*								\
      * Now align the stack pointer				\
      */							\
     __asm__ __volatile__ ("andl $-16, %esp");			\
								\
     what							\
}
#endif

#ifndef WITH_ALIGNED_STACK
#define WITH_ALIGNED_STACK(what) what
#endif

#endif /* __IFFTW_H__ */

