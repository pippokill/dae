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

import di.uniba.dae.utils.DownloadThread;
import di.uniba.dae.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author pierpaolo
 */
public class ProcessMetaDumpMTV2 {

    private static final Logger LOG = Logger.getLogger(ProcessMetaDumpMTV2.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("d", true, "Dump date (for example 20181101)")
                .addOption("n", true, "Number of download (default 5)")
                .addOption("t", true, "Number of processing thread (default 4)");
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
            System.setProperty("jdk.xml.totalEntitySizeLimit", String.valueOf(Integer.MAX_VALUE));
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("d")) {
                int nd = Integer.parseInt(cmd.getOptionValue("n", "5"));
                int nt = Integer.parseInt(cmd.getOptionValue("t", "4"));
                File setProcFile = new File(cmd.getOptionValue("d") + "/dump.processed");
                Set<String> pset = Collections.synchronizedSet(new HashSet<>());
                try {
                    LOG.info("Init...");
                    new File(cmd.getOptionValue("d") + "/download").mkdirs();
                    new File(cmd.getOptionValue("d") + "/temp").mkdirs();
                    new File(cmd.getOptionValue("d") + "/csv").mkdirs();
                    if (setProcFile.exists()) {
                        pset.addAll(Utils.loadSetFromFile(setProcFile));
                    }
                    String dumpdate = cmd.getOptionValue("d");
                    LOG.log(Level.INFO, "Build dump info for: {0}", dumpdate);
                    List<String> dumpList = Utils.createDumpList(dumpdate);
                    if (!pset.isEmpty()) {
                        LOG.log(Level.INFO, "Remove processed dumps from list...");
                        for (String fs : pset) {
                            dumpList.remove(fs.substring(fs.lastIndexOf("/") + 1));
                        }
                    }
                    LOG.log(Level.INFO, "Number of meta files: {0}", dumpList.size());
                    BlockingQueue<File> queue = new LinkedBlockingQueue<>(nd * 2);
                    DownloadThread dt = new DownloadThread(dumpdate, nd, dumpList, cmd.getOptionValue("d") + "/download", cmd.getOptionValue("d") + "/temp", queue, nt);
                    dt.start();
                    List<ProcessingThread> tlist = new ArrayList<>();
                    for (int i = 0; i < nt; i++) {
                        ProcessingThread pt = new ProcessingThread(queue, cmd.getOptionValue("d"), pset, false);
                        pt.start();
                        tlist.add(pt);
                    }
                    try {
                        dt.join();
                        for (Thread t : tlist) {
                            t.join();
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ProcessMetaDumpMTV2.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        Utils.saveSetInFile(setProcFile, pset);
                    } catch (IOException ex) {
                        Logger.getLogger(ProcessMetaDumpMTV2.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ProcessMetaDumpMTV2 (multi-thread version) - Build CSV files for entities extracted from a Wikipedia dump", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
