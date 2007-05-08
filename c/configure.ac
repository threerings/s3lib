AC_PREREQ(2.57)
AC_INIT(s3lib, [1.0], landonf@threerings.net)
AC_CONFIG_SRCDIR(${srcdir}/src)
AC_CONFIG_HEADERS(config.h)

AC_CANONICAL_SYSTEM

# Compiler
AC_PROG_CC
TR_WERROR
AC_CACHE_SAVE

# Programs
AC_PROG_INSTALL
AC_PROG_LN_S
AC_PROG_MAKE_SET

AC_PATH_PROG([DOXYGEN], [doxygen], [no])
if test x"$DOXYGEN" = x"no"; then
	DOXYGEN="false"
fi

AC_PATH_PROG([DOT], [dot], [no])
if test x"$DOT" = x"no"; then
	DOTDIR=""
else
	DOTDIR=`AS_DIRNAME([$DOT])`
fi
AC_SUBST([DOTDIR])

AC_CACHE_SAVE

# Headers
AC_HEADER_STDC
AC_CACHE_SAVE

# Threads
ACX_PTHREAD
AC_CACHE_SAVE

# Platform
TR_CONFIG_LIBRARY
AC_CACHE_SAVE

# Functions
# AC_CACHE_SAVE

# Libraries
LIBCURL_CHECK_CONFIG([yes], [], [], [
	AC_MSG_ERROR([s3lib requires libcurl (http://curl.haxx.se/).])
])
OD_LIBCHECK

# Output
AC_CONFIG_FILES([Makefile src/Makefile tests/Makefile Mk/autoconf.mk Mk/compile.mk Mk/subdir.mk doxyfile])

AC_OUTPUT
