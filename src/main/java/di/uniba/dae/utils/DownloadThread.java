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
package di.uniba.dae.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author pierpaolo
 */
public class DownloadThread extends Thread {

    private final String dumpDate;

    private final int nd;

    private final List<String> dumpList;

    private final String downloadDir;

    private final String tempDir;

    private final BlockingQueue<File> fileReadyQueue;

    private final int nt;

    private boolean run = true;

    private static final Logger LOG = Logger.getLogger(DownloadThread.class.getName());

    /**
     *
     * @param dumpDate
     * @param nd
     * @param dumpList
     * @param downloadDir
     * @param tempDir
     * @param fileReadyQueue
     * @param nt
     */
    public DownloadThread(String dumpDate, int nd, List<String> dumpList, String downloadDir, String tempDir, BlockingQueue<File> fileReadyQueue, int nt) {
        this.dumpDate = dumpDate;
        this.nd = nd;
        this.dumpList = dumpList;
        this.downloadDir = downloadDir;
        this.tempDir = tempDir;
        this.fileReadyQueue = fileReadyQueue;
        this.nt = nt;
    }

    @Override
    public void run() {
        while (run) {
            if (fileReadyQueue.size() >= nd) {
                try {
                    sleep(2000);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                if (!dumpList.isEmpty()) {
                    try {
                        LOG.log(Level.INFO, "File in queue: {0}", dumpList.size());
                        LOG.log(Level.INFO, "Download dump: {0}", dumpList.get(0));
                        Utils.downloadFile(new URL("https://dumps.wikimedia.org/enwiki/" + dumpDate + "/" + dumpList.get(0)), new File(tempDir + "/" + dumpList.get(0)));
                        LOG.log(Level.INFO, "Copy dump: {0}", dumpList.get(0));
                        FileUtils.copyFile(new File(tempDir + "/" + dumpList.get(0)), new File(downloadDir + "/" + dumpList.get(0)));
                        new File(tempDir + "/" + dumpList.get(0)).delete();
                        LOG.log(Level.INFO, "Dump ready: {0}", dumpList.get(0));
                        fileReadyQueue.offer(new File(downloadDir + "/" + dumpList.get(0)));
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, "IO Error for dump: " + dumpList.get(0), ex);
                    } finally {
                        dumpList.remove(0);
                    }
                } else {
                    for (int i = 0; i < nt; i++) {
                        fileReadyQueue.offer(new File("dummyfile"));
                    }
                    run = false;
                }
            }
        }
    }

}
