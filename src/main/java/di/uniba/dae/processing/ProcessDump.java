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

import di.uniba.dae.utils.DumpItem;
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

/**
 *
 * @author pierpaolo
 */
public class ProcessDump {

    private static final Logger LOG = Logger.getLogger(ProcessDump.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("d", true, "Input directory")
                .addOption("l", true, "Download log")
                .addOption("o", true, "Output directory")
                .addOption("t", true, "Number of processing thread (default 4)");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            System.setProperty("jdk.xml.totalEntitySizeLimit", String.valueOf(Integer.MAX_VALUE));
            CommandLine cmd = cmdParser.parse(options, args);
            if ((cmd.hasOption("d") || cmd.hasOption("l")) && cmd.hasOption("o")) {
                int nt = Integer.parseInt(cmd.getOptionValue("t", "4"));
                File setProcFile = new File(cmd.getOptionValue("o") + "/dump.processed");
                Set<String> pset = Collections.synchronizedSet(new HashSet<>());
                try {
                    LOG.info("Init...");
                    new File(cmd.getOptionValue("o") + "/csv").mkdirs();
                    if (setProcFile.exists()) {
                        pset.addAll(Utils.loadSetFromFile(setProcFile));
                    }
                    File downDir = new File(cmd.getOptionValue("d"));
                    LOG.log(Level.INFO, "Build dump info from: {0}", downDir.getAbsolutePath());
                    List<DumpItem> dumpList;
                    if (cmd.hasOption("l")) {
                        LOG.log(Level.INFO, "Build dump info from log: {0}", cmd.getOptionValue("l"));
                        dumpList = Utils.createDumpItemListFromLog(downDir.getAbsolutePath(), new File(cmd.getOptionValue("l")));
                    } else {
                        dumpList = Utils.createDumpItemList(downDir);
                    }
                    LOG.log(Level.INFO, "Number of meta files: {0}", dumpList.size());
                    if (!pset.isEmpty()) {
                        LOG.log(Level.INFO, "Remove processed dumps from list...");
                        for (String fs : pset) {
                            dumpList.remove(new DumpItem(fs, -1));
                        }
                        LOG.log(Level.INFO, "Number of meta files: {0}", dumpList.size());
                    }
                    BlockingQueue<File> queue = new LinkedBlockingQueue<>(dumpList.size() + nt);
                    for (DumpItem item : dumpList) {
                        queue.add(new File(item.getDumpName()));
                    }
                    for (int i = 0; i < nt; i++) {
                        queue.offer(new File("dummyfile"));
                    }
                    List<ProcessingThread> tlist = new ArrayList<>();
                    for (int i = 0; i < nt; i++) {
                        ProcessingThread pt = new ProcessingThread(queue, cmd.getOptionValue("o"), pset, false);
                        pt.start();
                        tlist.add(pt);
                    }
                    try {
                        for (Thread t : tlist) {
                            t.join();
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ProcessDump.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        Utils.saveSetInFile(setProcFile, pset);
                    } catch (IOException ex) {
                        Logger.getLogger(ProcessDump.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ProcessMetaDumpMTV3 (multi-thread version) - Build CSV files for entities extracted from a Wikipedia dump", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
