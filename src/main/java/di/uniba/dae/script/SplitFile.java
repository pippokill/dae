/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.dae.script;

import di.uniba.dae.split.DumpSplitWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
public class SplitFile {

    private static final Logger LOG = Logger.getLogger(SplitFile.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("d", true, "Directory")
                .addOption("s", true, "Split size")
                .addOption("e", false, "Delete files")
                .addOption("t", true, "Number of threads");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("d") && cmd.hasOption("s")) {
                try {
                    File dir = new File(cmd.getOptionValue("d"));
                    File[] listFiles = dir.listFiles();
                    long size = Long.parseLong(cmd.getOptionValue("s"));
                    boolean delete = cmd.hasOption("e");
                    int tn = Integer.parseInt(cmd.getOptionValue("t", "4"));
                    List<Thread> threads = new LinkedList();
                    List<File> files = new LinkedList<>();
                    for (File file : listFiles) {
                        if (file.isFile()) {
                            if (file.length() > size) {
                                files.add(file);
                            }
                        }
                    }
                    for (int i = 0; i < tn; i++) {
                        if (!files.isEmpty()) {
                            SplitThread t = new SplitThread(files.remove(0), size, delete);
                            threads.add(t);
                            t.start();
                        }
                    }
                    while (!files.isEmpty()) {
                        for (int i = threads.size() - 1; i >= 0; i--) {
                            if (!threads.get(i).isAlive()) {
                                threads.remove(i);
                                SplitThread t = new SplitThread(files.remove(0), size, delete);
                                threads.add(t);
                                t.start();
                            }
                        }
                        Thread.sleep(5000);
                    }
                    for (Thread t : threads) {
                        t.join();
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Split file - Split large files", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
