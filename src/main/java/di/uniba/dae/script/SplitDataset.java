/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.dae.script;

import static di.uniba.dae.script.SplitDump.cmdParser;
import di.uniba.dae.split.FileSplitUtils;
import java.io.File;
import java.io.IOException;
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
public class SplitDataset {
    
    private static final Logger LOG = Logger.getLogger(SplitDataset.class.getName());
    
    static Options options;
    
    static CommandLineParser cmdParser = new DefaultParser();
    
    static {
        options = new Options();
        options.addOption("i", true, "Input dataset")
                .addOption("s", true, "Split size")
                .addOption("e", false, "Delete files");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("s")) {
                try {
                    boolean deleteFile = cmd.hasOption("e");
                    FileSplitUtils.splitMainDataset(new File(cmd.getOptionValue("i")), Long.parseLong(cmd.getOptionValue("s")), deleteFile);
                } catch (IOException | NumberFormatException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Split dataset - Split a large dataset", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
    
}
