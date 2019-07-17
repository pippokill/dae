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
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pierpaolo
 */
public class SingleDownloadThread extends Thread {

    /**
     *
     */
    public static final String END_QUEUE = "###END###";

    private final String dumpdate;

    private final BlockingQueue<String> queue;

    private final String downloaddir;

    private boolean run = true;

    private static final Logger LOG = Logger.getLogger(SingleDownloadThread.class.getName());

    /**
     *
     * @param dumpdate
     * @param queue
     * @param downloaddir
     */
    public SingleDownloadThread(String dumpdate, BlockingQueue<String> queue, String downloaddir) {
        this.dumpdate = dumpdate;
        this.queue = queue;
        this.downloaddir = downloaddir;
    }

    @Override
    public void run() {
        while (run) {
            String filename = queue.poll();
            if (filename == null) {
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                if (filename.equals(END_QUEUE)) {
                    run = false;
                } else {
                    try {
                        File outFile = new File(downloaddir + "/" + filename);
                        if (outFile.exists()) {
                            LOG.log(Level.INFO, "Skip: {0}", filename);
                        } else {
                            LOG.log(Level.INFO, "Download: {0}", filename);
                            Utils.downloadFile(new URL("https://dumps.wikimedia.org/enwiki/" + dumpdate + "/" + filename), outFile);
                            LOG.log(Level.INFO, "Downloaded {0}, size: {1}", new Object[]{outFile.getName(), outFile.length()});
                        }
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

}
