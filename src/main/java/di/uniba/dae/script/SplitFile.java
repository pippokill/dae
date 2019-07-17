/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.dae.script;

import di.uniba.dae.split.DumpSplitWriter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pierpaolo
 */
public class SplitFile {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 1) {
            File dir = new File(args[0]);
            File[] listFiles = dir.listFiles();
            long size = Long.parseLong(args[1]);
            boolean delete = (args.length > 2 && args[2].equals("-d"));
            for (File file : listFiles) {
                if (file.isFile()) {
                    if (file.length() > size) {
                        try {
                            Logger.getLogger(SplitFile.class.getName()).log(Level.INFO, "Split file: {0}, original size: {1}", new Object[]{file.getName(), file.length()});
                            DumpSplitWriter.splitDump(file, size, delete);
                        } catch (IOException ex) {
                            Logger.getLogger(SplitFile.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        } else {
            Logger.getLogger(SplitFile.class.getName()).log(Level.SEVERE, "Illegal arguments");
        }
    }

}
