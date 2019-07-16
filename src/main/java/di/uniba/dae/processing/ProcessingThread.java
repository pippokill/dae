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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 *
 * @author pierpaolo
 */
public class ProcessingThread extends Thread {

    private final BlockingQueue<File> queue;

    private final String outDir;

    private final Set<String> pset;

    private boolean run = true;

    private static final Logger LOG = Logger.getLogger(ProcessingThread.class.getName());

    private final File setProcFile;

    private final boolean delete;

    public ProcessingThread(BlockingQueue<File> queue, String outDir, Set<String> pset, boolean delete) {
        this.queue = queue;
        this.outDir = outDir;
        this.pset = pset;
        this.setProcFile = new File(this.outDir + "/dump.processed");
        this.delete = delete;
    }

    public ProcessingThread(BlockingQueue<File> queue, String outDir, Set<String> pset) {
        this.queue = queue;
        this.outDir = outDir;
        this.pset = pset;
        this.setProcFile = new File(this.outDir + "/dump.processed");
        this.delete = true;
    }

    @Override
    public void run() {
        while (run) {
            File pfile = queue.poll();
            if (pfile != null) {
                if (pfile.getName().equals("dummyfile")) {
                    run = false;
                } else if (!pset.contains(pfile.getAbsolutePath())) {
                    MyDumpWriterV3 writer = null;
                    try {
                        LOG.log(Level.INFO, "Processing dump: {0}", pfile.getName());
                        File output = new File(outDir + "/csv/" + pfile.getName() + ".csv.gz");
                        if (output.exists()) {
                            LOG.log(Level.INFO, "Dump exists: {0}", pfile.getName());
                        } else {
                            BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(new FileInputStream(pfile));
                            int diff = Utils.getPagesNumbers(pfile);
                            writer = new MyDumpWriterV3(output, diff, true);
                            MyXmlDumpReader reader = new MyXmlDumpReader(bzIn, writer);
                            reader.readDump();
                            bzIn.close();
                            LOG.log(Level.INFO, "Done dump: {0}", pfile.getName());
                            //pset.add(pfile.getAbsolutePath());
                            //Utils.saveSetInFile(setProcFile, pset);
                        }
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, "Dump fail: " + pfile.getName(), ex);
                        try {
                            writer.close();
                        } catch (IOException ex1) {
                            LOG.log(Level.SEVERE, null, ex1);
                        }
                    } finally {
                        pset.add(pfile.getAbsolutePath());
                        try {
                            Utils.saveSetInFile(setProcFile, pset);
                        } catch (IOException ex) {
                            Logger.getLogger(ProcessingThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        if (delete) {
                            pfile.delete();
                        }
                    }
                } else {
                    LOG.log(Level.INFO, "Skip (already processed) dump: {0}", pfile.getName());
                }
            } else {
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ProcessingThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
