/**
 * Copyright (c) 2019, the Diachronic Analysis of Entities by Exploiting Wikipedia Page revisions AUTHORS.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the University of Bari nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
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
 *
 * GNU GENERAL PUBLIC LICENSE - Version 3, 29 June 2007
 *
 */
package di.uniba.dae.processing;

import di.uniba.dae.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author pierpaolo
 */
public class Downloader {

    private static final Logger LOG = Logger.getLogger(Downloader.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("d", true, "Dump date (for example 20181101)")
                .addOption("n", true, "Number of download thread (default 3)");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        }
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("d")) {
                String dumpdate = cmd.getOptionValue("d");
                LOG.log(Level.INFO, "Build dump info for: {0}", dumpdate);
                List<String> dumpList = Utils.createDumpList(dumpdate);
                LOG.log(Level.INFO, "Total dumps: {0}", dumpList.size());
                LOG.info("Init...");
                File downloadDir = new File(cmd.getOptionValue("d") + "/download");
                downloadDir.mkdirs();
                int n = Integer.parseInt(cmd.getOptionValue("n", "3"));
                BlockingQueue<String> queue = new LinkedBlockingDeque<>(dumpList);
                for (int i = 0; i < n; i++) {
                    queue.offer(SingleDownloadThread.END_QUEUE);
                }
                LOG.log(Level.INFO, "Elements in queue: {0}", queue.size());
                List<Thread> threads = new ArrayList();
                for (int i = 0; i < n; i++) {
                    threads.add(new SingleDownloadThread(dumpdate, queue, downloadDir.getAbsolutePath()));
                    threads.get(i).start();
                }
                for (int i = 0; i < n; i++) {
                    try {
                        threads.get(i).join();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Downloader - Download the whole history dump", options);
            }
        } catch (ParseException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
