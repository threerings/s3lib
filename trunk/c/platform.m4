# Copyright (c) 2003 - 2007 Landon Fuller <landonf@bikemonkey.org>
# Copyright (c) 2007 Three Rings Design, Inc.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in the
#    documentation and/or other materials provided with the distribution.
# 3. Neither the name of the copyright owner nor the names of contributors
#    may be used to endorse or promote products derived from this software
#    without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

#--------------------------------------------------------------------
# TR_CONFIG_LIBRARY
#
#	Try to determine the proper flags to pass to the compiler
#	for building plugins.
#
# Arguments:
#	none
#
# Results:
#
#	Defines and substitutes the following vars:
#
#       MAKE_LIBRARY -   Command to execute to build a plugin
#       INSTALL_LIBRARY - Command to execute to install a plugin
#       LIBRARY_CFLAGS -  Flags to pass to cc when compiling the components
#                       of a plugin (may request position-independent
#                       code, among other things).
#       LIBRARY_LD -      Base command to use for combining object files
#                       into a plugin .
#       LIBRARY_LD_FLAGS -Flags to pass when building a plugin. This
#                       differes from the LIBRARY_CFLAGS as it is not used
#                       when building object files or executables.
#       LIBRARY_LD_LIBS - Dependent libraries for the linker to scan when
#                       creating plugins.  This symbol typically
#                       goes at the end of the "ld" commands that build
#                       plugins. The value of the symbol is
#                       "${LIBS}" if all of the dependent libraries should
#                       be specified when creating a plugin.  If
#                       dependent libraries should not be specified (as on
#                       SunOS 4.x, where they cause the link to fail, or in
#                       general if Tcl and Tk aren't themselves shared
#                       libraries), then this symbol has an empty string
#                       as its value.
#       LIBRARY_SUFFIX -  Suffix to use for the names of dynamic libraries
#                       extensions.  An empty string means we don't know how
#                       to build libraries on this platform.
#       AR -            Path to ar binary
#       RANLIB -        Path to ranlib binary
#--------------------------------------------------------------------

AC_DEFUN(TR_CONFIG_LIBRARY, [

    LD_LIBRARY_PATH_VAR="LD_LIBRARY_PATH"
    PLAT_OBJS=""

    case $host_os in
	rhapsody*|darwin*)
	    LIBRARY_CFLAGS="-fno-common"
	    LIBRARY_LD="cc -dynamiclib \${LDFLAGS}"
	    LIBRARY_LD_FLAGS=""
	    LIBRARY_SUFFIX=".dylib"
	    ;;
	*)
	    # A sensible default
	    LIBRARY_CFLAGS="-fPIC"
	    LIBRARY_LD="${CC} -shared"
	    LIBRARY_LD_FLAGS=""
	    LIBRARY_SUFFIX=".so"
	    LDFLAGS="-export-dynamic"
	    ;;
    esac

    if test "x$LIBRARY_SUFFIX" = "x" ; then
	AC_MSG_ERROR([Can't figure out how to link libraries on this system.])
    fi

    LIBRARY_FILE='${LIBRARY_NAME}${LIBRARY_SUFFIX}'
    MAKE_LIBRARY='${LIBRARY_LD} -o [$]@ ${LIBRARY_LD_FLAGS} ${LIBRARY_OBJS} ${LIBS}'
    INSTALL_LIBRARY='$(INSTALL_LIB) $(LIBRARY_FILE) $(LIBRARY_INSTALL_DIR)/$(LIBRARY_FILE)'
    CLEAN_LIBRARY='rm -f ${LIBRARY_FILE}'

    # Check for static library tools, too
    AC_CHECK_TOOL([RANLIB], [ranlib], [no])
    if test x"$RANLIB" = x"no"; then
        AC_MSG_ERROR([Missing ranlib -- required to build static library])
    fi

    AC_CHECK_TOOL([AR], [ar], [no])
    if test x"$AR" = x"no"; then
        AC_MSG_ERROR([Missing ar -- required to build static library])
    fi


    AC_SUBST(LIBRARY_LD)
    AC_SUBST(LIBRARY_LD_FLAGS)
    AC_SUBST(LIBRARY_CFLAGS)
    AC_SUBST(LIBRARY_SUFFIX)
    AC_SUBST(LIBRARY_FILE)

    AC_SUBST(MAKE_LIBRARY)
    AC_SUBST(INSTALL_LIBRARY)
    AC_SUBST(CLEAN_LIBRARY)

    AC_SUBST([AR])
    AC_SUBST([RANLIB])
])

#------------------------------------------------------------------------
# TR_COMPILER_FVISIBILITY
#
#       Determines whether the compiler supports the -fvisiblity flag.
#
# Arguments:
#       None.
#
# Requires:
#       none
#
# Depends:
#       none
#
# Results:
#
#       Defines the following macros:
#               GCC_VISIBILITY_ATTR
#	Substitutes:
#		CFLAGS_VISIBILITY
#
#------------------------------------------------------------------------

AC_DEFUN([TR_COMPILER_FVISIBILITY], [
        AC_MSG_CHECKING([for gcc symbol visibility attribute])
        AC_CACHE_VAL(od_cv_attribute_tr_visibility, [
		CFLAGS_SAVED="$CFLAGS"
		CFLAGS="$CFLAGS -fvisibility=hidden"
                AC_COMPILE_IFELSE([
                        AC_LANG_SOURCE([
                                #if defined(__GNUC__) && defined(__APPLE__) && __GNUC__ < 4
                                # error Darwin does not support the visibility attribute with gcc releases prior to 4
                                #elif defined(WIN32) && __GNUC__ < 4
                                # error MinGW/Cygwin do not support the visibility attribute with gcc releases prior to 4.
                                #endif
                                int a __attribute__ ((visibility("hidden")));
                        ])
                ],[
                        od_cv_attribute_tr_visibility="yes"
                ],[
                        od_cv_attribute_tr_visibility="no"
                ])
		CFLAGS="$CFLAGS_SAVED"
        ])

        AC_MSG_RESULT([$od_cv_attribute_tr_visibility])
        
        if test x"$od_cv_attribute_tr_visibility" = "xyes"; then
		AC_DEFINE(GCC_VISIBILITY_SUPPORT, 1, [GCC supports the visibility attribute])
		CFLAGS_VISIBILITY="-fvisibility=hidden"
	else
		CFLAGS_VISIBILITY=""
        fi

	AC_SUBST(CFLAGS_VISIBILITY)
])
