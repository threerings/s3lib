#------------------------------------------------------------------------
# TR_SAFESTR --
#
#       Decipher the correct ldflags and cflags to link against libsafestr
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
#               --with-safestr switch to configure.
#
#       Result is cached.
#
#	Defines and substitutes the following vars:
#               SAFESTR_CPPFLAGS
#               SAFESTR_LIBS
#
#------------------------------------------------------------------------

dnl Test for check, and define SAFESTR_CFLAGS and SAFESTR_LIBS
dnl

AC_DEFUN(TR_SAFESTR, [
	AC_ARG_WITH(safestr, AC_HELP_STRING([--with-safestr=PATH], [Prefix where safestr library is installed. Defaults to auto-detection]), [with_safestr_prefix=${withval}])

	if test x"${with_safestr_prefix}" != x; then
		SAFESTR_CFLAGS="-I$with_safestr_prefix/include"
		SAFESTR_LIBS="-L$with_safestr_prefix/lib -lsafestr"
	else
		SAFESTR_CFLAGS=""
		SAFESTR_LIBS="-lsafestr"
	fi

	AC_MSG_CHECKING([for libsafestr])

	AC_CACHE_VAL(od_cv_safestr, [

		ac_save_CFLAGS="$CFLAGS"
		ac_save_LIBS="$LIBS"

		dnl safestr uses pthreads
		CFLAGS="$CFLAGS $SAFESTR_CFLAGS $PTHREAD_CFLAGS"
		LIBS="$SAFESTR_LIBS $LIBS $PTHREAD_LIBS"
		
		AC_LINK_IFELSE([
			AC_LANG_PROGRAM([
					#include <safestr.h>
				], [
					safestr_alloc(10, SAFESTR_IMMUTABLE);
			])
			], [
				od_cv_safestr="yes"
			], [
				od_cv_safestr="no"
			]
		)

		CFLAGS="$ac_save_CFLAGS"
		LIBS="$ac_save_LIBS"

	])

	AC_MSG_RESULT(${od_cv_safestr})

	if test x"${od_cv_safestr}" != "xyes"; then
		AC_MSG_ERROR([s3lib requires the safestr library (http://www.zork.org/safestr/safestr.html).])
	fi
			

	AC_SUBST(SAFESTR_CPPFLAGS)
	AC_SUBST(SAFESTR_LIBS)
])
