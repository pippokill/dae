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
public class SplitThread extends Thread {

    private final File file;

    private final long size;

    private final boolean delete;

    public SplitThread(File file, long size, boolean delete) {
        this.file = file;
        this.size = size;
        this.delete = delete;
    }

    @Override
    public void run() {
        try {
            Logger.getLogger(SplitFile.class.getName()).log(Level.INFO, "Split file: {0}, original size: {1}", new Object[]{file.getName(), file.length()});
            DumpSplitWriter.splitDump(file, size, delete);
        } catch (IOException ex) {
            Logger.getLogger(SplitFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
