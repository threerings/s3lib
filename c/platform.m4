
# This software is copyrighted by the Regents of the University of
# California, Sun Microsystems, Inc., Scriptics Corporation, ActiveState
# Corporation and other parties.  The following terms apply to all files
# associated with the software unless explicitly disclaimed in
# individual files.
# 
# The authors hereby grant permission to use, copy, modify, distribute,
# and license this software and its documentation for any purpose, provided
# that existing copyright notices are retained in all copies and that this
# notice is included verbatim in any distributions. No written agreement,
# license, or royalty fee is required for any of the authorized uses.
# Modifications to this software may be copyrighted by their authors
# and need not follow the licensing terms described here, provided that
# the new terms are clearly indicated on the first page of each file where
# they apply.
# 
# IN NO EVENT SHALL THE AUTHORS OR DISTRIBUTORS BE LIABLE TO ANY PARTY
# FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
# ARISING OUT OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY
# DERIVATIVES THEREOF, EVEN IF THE AUTHORS HAVE BEEN ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# 
# THE AUTHORS AND DISTRIBUTORS SPECIFICALLY DISCLAIM ANY WARRANTIES,
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.  THIS SOFTWARE
# IS PROVIDED ON AN "AS IS" BASIS, AND THE AUTHORS AND DISTRIBUTORS HAVE
# NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
# MODIFICATIONS.
# 
# GOVERNMENT USE: If you are acquiring this software on behalf of the
# U.S. government, the Government shall have only "Restricted Rights"
# in the software and related documentation as defined in the Federal 
# Acquisition Regulations (FARs) in Clause 52.227.19 (c) (2).  If you
# are acquiring the software on behalf of the Department of Defense, the
# software shall be classified as "Commercial Computer Software" and the
# Government shall have only "Restricted Rights" as defined in Clause
# 252.227-7013 (c) (1) of DFARs.  Notwithstanding the foregoing, the
# authors grant the U.S. Government and others acting in its behalf
# permission to use and distribute the software in accordance with the
# terms specified in this license.

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

    AC_SUBST(LIBRARY_LD)
    AC_SUBST(LIBRARY_LD_FLAGS)
    AC_SUBST(LIBRARY_CFLAGS)
    AC_SUBST(LIBRARY_SUFFIX)
    AC_SUBST(LIBRARY_FILE)

    AC_SUBST(MAKE_LIBRARY)
    AC_SUBST(INSTALL_LIBRARY)
    AC_SUBST(CLEAN_LIBRARY)
])
