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
package di.uniba.dae.analysis;

import di.uniba.dae.utils.Entry;
import di.uniba.dae.utils.Utils;
import di.uniba.dae.post.SurfaceContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
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
public class BuildFinalDataset {

    private static final Logger LOG = Logger.getLogger(BuildFinalDataset.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("i", true, "Input dir")
                .addOption("d", true, "Dictionary")
                .addOption("o", true, "Output file")
                .addOption("m", true, "Min occurrences")
                .addOption("b", true, "Batch size");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("d") && cmd.hasOption("o")) {
                int min = Integer.parseInt(cmd.getOptionValue("m", "0"));
                int batchSize = Integer.parseInt(cmd.getOptionValue("b", "10000"));
                try {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(cmd.getOptionValue("o"))), "UTF-8"));
                    List<Entry> dict = Utils.loadDict(new File(cmd.getOptionValue("d")), min);
                    Collections.shuffle(dict);
                    for (int k = 0; k < dict.size(); k += batchSize) {
                        Map<String, Map<Integer, Map<String, SurfaceContext>>> memory = new Object2ObjectOpenHashMap<>();
                        LOG.log(Level.INFO, "Batch: from {0} to {1}", new Object[]{k, k + batchSize - 1});
                        for (int j = k; j < (k + batchSize); j++) {
                            memory.put(dict.get(j).getKey(), new Int2ObjectOpenHashMap<>());
                        }
                        LOG.log(Level.INFO, "Batch dict size: {0}", memory.size());
                        String target = null;
                        File indir = new File(cmd.getOptionValue("i"));
                        File[] listFiles = indir.listFiles();
                        double totSize = 0;
                        double procSize = 0;
                        for (File file : listFiles) {
                            totSize += file.length();
                        }
                        for (File file : listFiles) {
                            if (file.getName().endsWith(".gz")) {
                                LOG.log(Level.INFO, "Load file: {0}", file.getName());
                                BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
                                try {
                                    int year = 0;
                                    while (reader.ready()) {
                                        String line = reader.readLine();
                                        if (line.startsWith("#T")) {
                                            target = line.substring(3);
                                            if (!memory.containsKey(target)) {
                                                target = null;
                                            }
                                        } else {
                                            if (target != null) {
                                                Map<Integer, Map<String, SurfaceContext>> mapTarget = memory.get(target);
                                                if (mapTarget != null) {
                                                    int offsetCorrection = 0;
                                                    String[] split = line.split("\t");
                                                    if (split[0].matches("^[0-9]{4,4}$") && split[2].matches("^[0-9]+$") && split[3].matches("^[0-9]+$")) { //!IMPORTANT fix first revision of the dataset 
                                                        year = Integer.parseInt(split[0]); //year
                                                    } else {
                                                        offsetCorrection = 1;
                                                    }
                                                    Map<String, SurfaceContext> yearMap = mapTarget.get(year);
                                                    if (yearMap == null) {
                                                        yearMap = new Object2ObjectOpenHashMap<>();
                                                        mapTarget.put(year, yearMap);
                                                    }
                                                    SurfaceContext sc = yearMap.get(split[1 - offsetCorrection]); //surface
                                                    if (sc == null) {
                                                        sc = new SurfaceContext(Integer.parseInt(split[2 - offsetCorrection])); //surface_counter
                                                        yearMap.put(split[1 - offsetCorrection], sc);
                                                    } else {
                                                        sc.incrementCounter(Integer.parseInt(split[2 - offsetCorrection]));
                                                    }
                                                    for (int x = (4 - offsetCorrection); x < split.length; x++) {
                                                        String[] sv = split[x].split(" ");
                                                        sc.addWord(sv[0], Integer.parseInt(sv[1]));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    LOG.log(Level.SEVERE, "Error to read file (skip)", ex);
                                } finally {
                                    reader.close();
                                }
                                procSize += file.length();
                                LOG.log(Level.INFO, "Processed file: {0}, progress {1}%", new Object[]{file.getName(), String.format("%1$.2f", procSize * 100d / totSize)});
                            }
                        }
                        //save
                        for (String targetKey : memory.keySet()) {
                            out.append("#T\t").append(targetKey);
                            out.newLine();
                            Map<Integer, Map<String, SurfaceContext>> yearMap = memory.get(targetKey);
                            for (int year : yearMap.keySet()) {
                                Map<String, SurfaceContext> surfaceMap = yearMap.get(year);
                                for (String surface : surfaceMap.keySet()) {
                                    out.append(String.valueOf(year)).append("\t");
                                    out.append(surface).append("\t");
                                    SurfaceContext ctx = surfaceMap.get(surface);
                                    out.append(String.valueOf(ctx.getCount())).append("\t");
                                    Map<String, Integer> bow = ctx.getBow();
                                    out.append(String.valueOf(bow.size()));
                                    for (Map.Entry<String, Integer> entry : bow.entrySet()) {
                                        out.append("\t").append(entry.getKey()).append(" ").append(entry.getValue().toString());
                                    }
                                    out.newLine();
                                }
                            }
                        }
                        out.flush();
                        memory.clear();
                        memory = null;
                        System.gc();
                    }
                    out.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Build final dataset", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
