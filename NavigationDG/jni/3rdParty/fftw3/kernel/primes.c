


#include "ifftw.h"

/***************************************************************************/

/* Rader's algorithm requires lots of modular arithmetic, and if we
   aren't careful we can have errors due to integer overflows. */

/* Compute (x * y) mod p, but watch out for integer overflows; we must
   have 0 <= {x, y} < p.

   If overflow is common, this routine is somewhat slower than
   e.g. using 'long long' arithmetic.  However, it has the advantage
   of working when INT is 64 bits, and is also faster when overflow is
   rare.  FFTW calls this via the MULMOD macro, which further
   optimizes for the case of small integers.
*/

#define ADD_MOD(x, y, p) ((x) >= (p) - (y)) ? ((x) + ((y) - (p))) : ((x) + (y))

INT fftwf_safe_mulmod(INT x, INT y, INT p)
{
     INT r;

     if (y > x)
	  return fftwf_safe_mulmod(y, x, p);

     A(0 <= y && x < p);

     r = 0;
     while (y) {
	  r = ADD_MOD(r, x*(y&1), p); y >>= 1;
	  x = ADD_MOD(x, x, p);
     }

     return r;
}

/***************************************************************************/

/* Compute n^m mod p, where m >= 0 and p > 0.  If we really cared, we
   could make this tail-recursive. */

INT fftwf_power_mod(INT n, INT m, INT p)
{
     A(p > 0);
     if (m == 0)
	  return 1;
     else if (m % 2 == 0) {
	  INT x = fftwf_power_mod(n, m / 2, p);
	  return MULMOD(x, x, p);
     }
     else
	  return MULMOD(n, fftwf_power_mod(n, m - 1, p), p);
}

/* the following two routines were contributed by Greg Dionne. */
static INT get_prime_factors(INT n, INT *primef)
{
     INT i;
     INT size = 0;

     A(n % 2 == 0); /* this routine is designed only for even n */
     primef[size++] = (INT)2;
     do
	  n >>= 1;
     while ((n & 1) == 0);

     if (n == 1)
	  return size;

     for (i = 3; i * i <= n; i += 2)
	  if (!(n % i)) {
	       primef[size++] = i;
	       do
		    n /= i;
	       while (!(n % i));
	  }
     if (n == 1)
	  return size;
     primef[size++] = n;
     return size;
}

INT fftwf_find_generator(INT p)
{
    INT n, i, size;
    INT primef[16];     /* smallest number = 32589158477190044730 > 2^64 */
    INT pm1 = p - 1;

    if (p == 2)
	 return 1;

    size = get_prime_factors(pm1, primef);
    n = 2;
    for (i = 0; i < size; i++)
        if (fftwf_power_mod(n, pm1 / primef[i], p) == 1) {
            i = -1;
            n++;
        }
    return n;
}

/* Return first prime divisor of n  (It would be at best slightly faster to
   search a static table of primes; there are 6542 primes < 2^16.)  */
INT fftwf_first_divisor(INT n)
{
     INT i;
     if (n <= 1)
	  return n;
     if (n % 2 == 0)
	  return 2;
     for (i = 3; i*i <= n; i += 2)
	  if (n % i == 0)
	       return i;
     return n;
}

int fftwf_is_prime(INT n)
{
     return(n > 1 && fftwf_first_divisor(n) == n);
}

INT fftwf_next_prime(INT n)
{
     while (!fftwf_is_prime(n)) ++n;
     return n;
}

int fftwf_factors_into(INT n, const INT *primes)
{
     for (; *primes != 0; ++primes)
	  while ((n % *primes) == 0)
	       n /= *primes;
     return (n == 1);
}

/* integer square root.  Return floor(sqrt(N)) */
INT fftwf_isqrt(INT n)
{
     INT guess, iguess;

     A(n >= 0);
     if (n == 0) return 0;

     guess = n; iguess = 1;

     do {
          guess = (guess + iguess) / 2;
	  iguess = n / guess;
     } while (guess > iguess);

     return guess;
}

static INT isqrt_maybe(INT n)
{
     INT guess = fftwf_isqrt(n);
     return guess * guess == n ? guess : 0;
}

#define divides(a, b) (((b) % (a)) == 0)
INT fftwf_choose_radix(INT r, INT n)
{
     if (r > 0) {
	  if (divides(r, n)) return r;
	  return 0;
     } else if (r == 0) {
	  return fftwf_first_divisor(n);
     } else {
	  /* r is negative.  If n = (-r) * q^2, take q as the radix */
	  r = 0 - r;
	  return (n > r && divides(r, n)) ? isqrt_maybe(n / r) : 0;
     }
}

/* return A mod N, works for all A including A < 0 */
INT fftwf_modulo(INT a, INT n)
{
     A(n > 0);
     if (a >= 0)
	  return a % n;
     else
	  return (n - 1) - ((-(a + (INT)1)) % n);
}

