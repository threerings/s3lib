function printClassDeclaration(errorName, reason) {
	exceptionName = errorName "Exception"
	print "/**", reason, "*/"
	print "public static class", exceptionName, "extends S3Exception {"
	print "    public", exceptionName, "(String message) {"
	print "        this(message, null, null);"
	print "    }\n"
	print "    public", exceptionName, "(String message, String requestId, String hostId) {"
	print "        super(message, requestId, hostId);"
	print "    }"
	print "}\n"
}

printClassDeclaration(substr($1, 1, length($1) - 1), $2)
