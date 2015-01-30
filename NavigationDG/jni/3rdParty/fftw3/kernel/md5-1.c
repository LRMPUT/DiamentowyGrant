#include "ifftw.h"


void fftwf_md5putb(md5 *p, const void *d_, size_t len)
{
     size_t i;
     const unsigned char *d = (const unsigned char *)d_;
     for (i = 0; i < len; ++i)
	  fftwf_md5putc(p, d[i]);
}

void fftwf_md5puts(md5 *p, const char *s)
{
     /* also hash final '\0' */
     do {
	  fftwf_md5putc(p, *s);
     } while(*s++);
}

void fftwf_md5int(md5 *p, int i)
{
     fftwf_md5putb(p, &i, sizeof(i));
}

void fftwf_md5INT(md5 *p, INT i)
{
     fftwf_md5putb(p, &i, sizeof(i));
}

void fftwf_md5unsigned(md5 *p, unsigned i)
{
     fftwf_md5putb(p, &i, sizeof(i));
}
