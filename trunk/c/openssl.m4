#------------------------------------------------------------------------
# TR_OPENSSL --
#
#       Decipher the correct ldflags and cflags to link against libopenssl
#
# Arguments:
#       None.
#
# Requires:
#       None.
#
# Depends:
#	AC_PROG_CC
#
# Results:
#
#       Adds the following arguments to configure:
#               --with-openssl switch to configure.
#
#       Result is cached.
#
#	Defines and substitutes the following vars:
#               OPENSSL_CFLAGS
#               OPENSSL_LIBS
#
#------------------------------------------------------------------------

dnl Test for check, and define OPENSSL_CFLAGS and OPENSSL_LIBS
dnl

AC_DEFUN(TR_OPENSSL, [
	AC_ARG_WITH(openssl, AC_HELP_STRING([--with-openssl=PATH], [Prefix where openssl library is installed. Defaults to auto-detection]), [with_openssl_prefix=${withval}])

	if test x"${with_openssl_prefix}" != x; then
		OPENSSL_CFLAGS="-I$with_openssl_prefix/include"
		OPENSSL_LIBS="-L$with_openssl_prefix/lib -lssl -lcrypto"
	else
		OPENSSL_CFLAGS=""
		OPENSSL_LIBS="-lssl -lcrypto"
	fi

	AC_MSG_CHECKING([for openssl])

	AC_CACHE_VAL(od_cv_openssl, [

		ac_save_CFLAGS="$CFLAGS"
		ac_save_LIBS="$LIBS"

		dnl openssl uses pthreads
		CFLAGS="$CFLAGS $OPENSSL_CFLAGS"
		LIBS="$OPENSSL_LIBS $LIBS"
		
		AC_LINK_IFELSE([
			AC_LANG_PROGRAM([
					#include <openssl/hmac.h>
				], [
					HMAC_CTX hmac;
					HMAC_CTX_init(&hmac);
			])
			], [
				od_cv_openssl="yes"
			], [
				od_cv_openssl="no"
			]
		)

		CFLAGS="$ac_save_CFLAGS"
		LIBS="$ac_save_LIBS"

	])

	AC_MSG_RESULT(${od_cv_openssl})

	if test x"${od_cv_openssl}" != "xyes"; then
		AC_MSG_ERROR([s3lib requires the openssl library (http://www.openssl.org)])
	fi
			

	AC_SUBST(OPENSSL_CFLAGS)
	AC_SUBST(OPENSSL_LIBS)
])
