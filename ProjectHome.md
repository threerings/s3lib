## Description ##

S3Lib is a client library for  Amazon's Simple Storage Service.

### Status ###

#### Java Library ####

S3Lib-Java is complete and in active production use. [Three Rings Design](http://www.threerings.net) uses S3Lib-Java for the backend storage needs of their games.

#### C Library ####

The C implementation, S3Lib-C, is a work-in-progress. Finishing the library is waiting on free time, an external contribution, or sponsorship of the work.

Remaining tasks:
  * Implementation of S3Connection using libcurl.

### S3Pipe ###
Included with S3Lib-Java is S3Pipe -- S3Pipe implements piping of streams to and from S3. This allows us to pipe the encrypted output of 'dump' (and 'restore') directly to and from S3 -- We've used this to implement offsite backups of our servers:

```
landonf:s3lib> echo "hello, world" | s3pipe ... upload --stream helloworld
landonf:s3lib> s3pipe ... download --stream helloworld
hello, world
```