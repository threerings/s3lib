/* 
 * Main vi:ts=4:sw=4:expandtab:
 *
 * Copyright (c) 2007 Three Rings Design, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright owner nor the names of contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.threerings.s3.pipe;

import com.threerings.s3.client.S3Connection;
import com.threerings.s3.client.S3Exception;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.Security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * s3pipe main class.
 */
final public class Main {
    enum PipeCommand {
        /** Read from stdin, write to an S3 stream. */
        UPLOAD {
            public void run (Main app)
                throws S3Exception, RemoteStreamException
            {
                UploadStreamer streamer = new UploadStreamer(app.connection, app.bucketName, app.blockSize);
                streamer.upload(app.streamName, System.in, app.maxRetry);
            }
        },


        /** Read from an S3 stream, write to stdout. */
        DOWNLOAD {
            public void run (Main app)
                throws S3Exception, RemoteStreamException
            {
                DownloadStreamer streamer = new DownloadStreamer(app.connection, app.bucketName);
                streamer.download(app.streamName, System.out, app.maxRetry);
            }
        },

        /** Delete a stream. */
        DELETE {
            public void run (Main app)
                throws S3Exception, RemoteStreamException
            {
                RemoteStream stream = new RemoteStream(app.connection, app.bucketName, app.streamName);
                stream.delete();
            }
        },

        /** List all streams in a bucket. */
        LIST {
            public void run (Main app)
                throws S3Exception, RemoteStreamException
            {
                List<RemoteStreamInfo> list = RemoteStream.getAllStreams(app.connection, app.bucketName);
                for (RemoteStreamInfo info : list) {
                    System.out.println("Stream: '" + info.getName() + "'" + " " +
                        "    Created: " + info.getCreationDate());
                }
            }

            public void validate (Main app) {}
        },

        /** Create a bucket. */
        CREATEBUCKET {
            public void run (Main app)
                throws S3Exception
            {
                app.connection.createBucket(app.bucketName);
            }

            public void validate (Main app) {}
        },


        /** Delete a bucket. */
        DELETEBUCKET {
            public void run (Main app)
                throws S3Exception
            {
                app.connection.deleteBucket(app.bucketName);
            }

            public void validate (Main app) {}
        };


        /** Validate that any (non-args4j required) user-supplied settings are set. Feel
          * free to override this for a given command. */
        public void validate (Main app)
            throws CmdLineException
        {
            if (app.streamName == null) {
                throw new CmdLineException("Option \"--stream\" is required.");
            }
        }

        /** Run the specific pipe command. */
        public abstract void run (Main app) throws RemoteStreamException, S3Exception;
    };


    /**
     * Instantiate and run a Main instance.
     */
    public static void main (String[] args) {
        new Main().doMain(args);
    }


    /**
     * Parse command line arguments and run the specified command.
     */
    public void doMain (String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        PipeCommand command;

        /* Parse command line arguments. */
        try {
            parser.parseArgument(args);

            /* We require exactly one command argument */
            if (arguments.size() < 1) {
                throw new CmdLineException("Missing command argument");
            } else if (arguments.size() > 1) {
                throw new CmdLineException("Too many arguments");
            }

            /* Map the command string to the PipeCommand enum. */
            try {
                command = PipeCommand.valueOf(arguments.get(0).toUpperCase(Locale.US));                
            } catch (IllegalArgumentException e) {
                throw new CmdLineException("Unknown command " + arguments.get(0));
            }

            /* Validate settings. */
            command.validate(this);
        } catch (CmdLineException cle) {
            System.err.println("Error parsing arguments: " + cle.getMessage());
            System.err.println("Usage:\n  s3pipe <options> <command>\n\nOptions:");
            parser.printUsage(System.err);
            System.err.println("\nCommands:");
            System.err.println("  upload\t\t: Read a stream from stdin, and write to S3.");
            System.err.println("  download\t\t: Read a stream from S3, and write to stdout.");
            System.err.println("  list\t\t\t: List all streams within the bucket.");
            System.err.println("  delete\t\t: Delete the specified stream.");
            System.err.println("  createbucket\t\t: Create the specified bucket.");
            System.err.println("  deletebucket\t\t: Delete the specified bucket.");
            System.err.println();
            System.exit(EXIT_FAILURE);
            return;
        }

        /* Don't cache DNS results forever. This will break S3's DNS-based
         * failover support */
        Security.setProperty("networkaddress.cache.ttl" , "30");

        try {
            loadProperties();
            connection = new S3Connection(awsId, awsKey);
            command.run(this);
        } catch (S3Exception e) {
            System.err.println(e.getMessage());
            System.exit(EXIT_FAILURE);
        } catch (RemoteStreamException e) {
            System.err.println("Stream record error: " + e.getMessage());
            System.exit(EXIT_FAILURE);            
        }

        System.exit(EXIT_SUCCESS);
    }


    /**
     * Load AWS Id and Key from the properties file.
     */
    private void loadProperties () {
        Properties awsProps;

        try {
            awsProps = new Properties();
            awsProps.load(new FileInputStream(keyFile));
        } catch (IOException e) {
            System.err.println("Failure reading AWS key file: " + e.getMessage());
            System.exit(EXIT_FAILURE);
            return;
        }

        awsId = awsProps.getProperty(PROP_AWSID);
        awsKey = awsProps.getProperty(PROP_AWSKEY);

        if (awsKey == null) {
            System.err.println("Missing " + PROP_AWSID + " property in '" +
                keyFile.getName() + "'");
            System.exit(EXIT_FAILURE);
        }

        if (awsId == null) {
            System.err.println("Missing " + PROP_AWSKEY + " property in '" +
                keyFile.getName() + "'");
            System.exit(EXIT_FAILURE);
        }
    }

    /**
     * Set the block size, in bytes.
     */
    @SuppressWarnings("unused")
	@Option(name="--blocksize", usage="Specify the in-memory block size, in kilobytes, " +
        "for S3 file uploads. Defaults to 5 megabytes.", metaVar="<size>")
    private void setBlocksize (int newBlockSize) {
        blockSize = newBlockSize * 1024;
    }

    /** S3 Connection. */
    private S3Connection connection;

    /** AWS Id. */
    private String awsId;

    /** AWS secret key */
    private String awsKey;

    /** Path to AWS properties file. */
    @Option(name="--keyfile", usage="Specify the properties file containing the AWS ID and secret key.", metaVar="<file>", required=true)
    private File keyFile;

    /** Remote bucket name. */
    @Option(name="--bucket", usage="Specify the S3 bucket name.", metaVar="<bucket>", required=true)
    private String bucketName = null;

    /** Remote stream name. */
    @Option(name="--stream", usage="Specify the remote stream name.", metaVar="<stream name>")
    private String streamName = null;

    /** Maximum number of retries. */
    @Option(name="--retry", usage="Specify the number of S3 retry attempts before exiting, " +
        "in the event of S3 and/or network failure. Defaults to 30.", metaVar="<count>")
    private int maxRetry = 30;

    /** Block size, in bytes. Default to 5 megabytes. */
    private int blockSize = 5 * 1024 * 1024;

    /** All non-option arguments. */
    @Argument
    private List<String> arguments = new ArrayList<String>();

    /** AWS id property. */
    private static final String PROP_AWSID = "aws.id";

    /** AWS secret key property. */
    private static final String PROP_AWSKEY = "aws.key";

    /** Standard C99 EXIT_SUCCESS value. */
    private static final int EXIT_SUCCESS = 0;
    
    /** Standard C99 EXIT_FAILURE value. */
    private static final int EXIT_FAILURE = 1;
}
