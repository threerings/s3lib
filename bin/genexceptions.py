import httplib, re, sys

error = re.compile("<tr><td>(.*?)</td><td>(.*?)</td><td>(\d*).+?</td><td>(Client|Server)</td></tr>",
        re.DOTALL)
whitespace = re.compile('\s+')

template = """/** %(reason)s  */
public static class %(class)s extends %(super)s {
    public %(class)s (String message) {
        this(message, null, null);
    }
    public %(class)s (String message, String requestId, String hostId) {
        super(message, requestId, hostId);
    }
}
"""
codes = set()
names = set() # MalformedACLError is included twice, so only generate the class once
for match in error.finditer(sys.stdin.read()):
    name, reason, superclass, source = match.groups()
    if not name in names:
        if superclass:
            codes.add(superclass)
        print template % {"class":"%sException" % name.strip(),
                "super":"S3Server%sException" % superclass, "reason":whitespace.sub(" ", reason)}
    names.add(name)

for code in sorted(list(codes)):
    codename = httplib.responses[int(code)]
    print template % {"class":"S3Server%sException" % code, "super":"S3ServerException",
            "reason":"S3 returned a status code %s, which means %s" % (code, codename)}
