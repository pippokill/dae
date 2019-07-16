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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
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
public class CountTarget {

    private static final Logger LOG = Logger.getLogger(CountTarget.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("i", true, "Input dir")
                .addOption("o", true, "Output file")
                .addOption("m", true, "Min occurrences");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("o")) {
                int min = Integer.parseInt(cmd.getOptionValue("m", "0"));
                try {
                    Map<String, Integer> map = new Object2IntOpenHashMap<>();
                    int c = 0;
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
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
                            try {
                                while (reader.ready()) {
                                    String line = reader.readLine();
                                    if (line.startsWith("#T")) {
                                        if (target != null && !target.matches("[a-zA-Z\\s]+:[^\\s].+")) {
                                            Integer v = map.get(target);
                                            if (v == null) {
                                                map.put(target, c);
                                            } else {
                                                map.put(target, v + c);
                                            }
                                        }
                                        c = 0;
                                        target = line.substring(3);
                                    } else {
                                        String[] split = line.split("\t");
                                        c += Integer.parseInt(split[2]);
                                    }
                                }
                            } catch (Exception ex) {
                                LOG.log(Level.SEVERE, "Error to read file (skip)", ex);
                            } finally {
                                reader.close();
                            }
                            if (target != null && !target.matches("[a-zA-Z]+:[^\\s].+")) {
                                Integer v = map.get(target);
                                if (v == null) {
                                    map.put(target, 1);
                                } else {
                                    map.put(target, v + c);
                                }
                            }
                            procSize += file.length();
                            LOG.log(Level.INFO, "Processed file: {0}, progress {1}%", new Object[]{file.getName(), String.format("%1$.2f", procSize * 100d / totSize)});
                        }
                    }
                    List<Entry> list = new ArrayList<>();
                    for (Map.Entry<String, Integer> e : map.entrySet()) {
                        if (e.getValue() >= min) {
                            list.add(new Entry(e.getKey(), e.getValue()));
                        }
                    }
                    Collections.sort(list, Comparator.reverseOrder());
                    File outfile = new File(cmd.getOptionValue("o"));
                    BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
                    for (Entry e : list) {
                        writer.append(e.getKey()).append("\t").append(String.valueOf(e.getCount()));
                        writer.newLine();
                    }
                    writer.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Count target - count all targets in the dataset", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
