builtin(include,m4/pthread.m4)
builtin(include,m4/platform.m4)
builtin(include,m4/check.m4)
builtin(include,m4/libcurl.m4)
builtin(include,m4/libxml.m4)
builtin(include,m4/safestr.m4)

#------------------------------------------------------------------------
# TR_WERROR --
#
#	Enable -Werror
#
# Arguments:
#	None.
#
# Requires:
#	none
#
# Depends:
#	none
#
# Results:
#	Modifies CFLAGS variable.
#------------------------------------------------------------------------
AC_DEFUN([TR_WERROR],[
	AC_REQUIRE([AC_PROG_CC])
	AC_ARG_ENABLE(werror, AC_HELP_STRING([--enable-werror], [Add -Werror to CFLAGS. Used for development.]), [enable_werror=${enableval}], [enable_werror=no])
	if test x"$enable_werror" != "xno"; then
		CFLAGS="$CFLAGS -Werror"
	fi
])
