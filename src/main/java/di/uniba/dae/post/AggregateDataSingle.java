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
package di.uniba.dae.post;

import di.uniba.dae.processing.ProcessMetaDumpMTV2;
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
public class AggregateDataSingle {

    private static final Logger LOG = Logger.getLogger(ProcessMetaDumpMTV2.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("i", true, "Input dir")
                .addOption("o", true, "Output directory")
                .addOption("w", false, "Overwrite files")
                .addOption("d", false, "Delete CSV file");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if (cmd.hasOption("i") && cmd.hasOption("o")) {
                try {
                    boolean overwrite = cmd.hasOption("w");
                    boolean delete = cmd.hasOption("d");
                    File inputDir = new File(cmd.getOptionValue("i"));
                    LOG.log(Level.INFO, "Input dir {0}", inputDir.getName());
                    File outputDir = new File(cmd.getOptionValue("o"));
                    LOG.log(Level.INFO, "Output dir {0}", outputDir.getName());
                    outputDir.mkdirs();
                    for (File inputFile : inputDir.listFiles()) {
                        File outputFile = new File(cmd.getOptionValue("o") + "/" + inputFile.getName());
                        if (overwrite || !outputFile.exists()) {
                            process(inputFile, outputFile);
                            if (delete) {
                                inputFile.delete();
                            }
                        } else {
                            LOG.log(Level.INFO, "Skip file {0}", inputFile.getName());
                        }
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Aggregate data - Build final dataset files aggregating data from CSVs", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static void process(File inputFile, File outputFile) throws IOException {
        long ok = 0;
        long ko = 0;
        LOG.log(Level.INFO, "Processing file: {0}", inputFile.getName());
        Map<String, Map<Integer, Map<String, SurfaceContext>>> map = new Object2ObjectOpenHashMap<>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFile))));
        try {
            while (in.ready()) {
                String line = in.readLine();
                String[] split = line.split("\t");
                if (split.length == 5) {
                    try {
                        Map<Integer, Map<String, SurfaceContext>> mapTarget = map.get(split[1]); //target
                        if (mapTarget == null) {
                            mapTarget = new Int2ObjectOpenHashMap<>();
                            map.put(split[1], mapTarget);
                        }
                        int year = Integer.parseInt(split[0]); //year
                        Map<String, SurfaceContext> mapSurface = mapTarget.get(year);
                        if (mapSurface == null) {
                            mapSurface = new Object2ObjectOpenHashMap<>();
                            mapTarget.put(year, mapSurface);
                        }
                        SurfaceContext ctx = mapSurface.get(split[2]); //surface
                        if (ctx == null) {
                            ctx = new SurfaceContext();
                            mapSurface.put(split[2], ctx);
                        }
                        //add info to context
                        ctx.incrementCounter();
                        String[] cs = split[3].split("\\s+");
                        for (String w : cs) {
                            ctx.addWord(w);
                        }
                        cs = split[4].split("\\s+");
                        for (String w : cs) {
                            ctx.addWord(w);
                        }
                        ok++;
                    } catch (Exception ex) {
                        LOG.log(Level.SEVERE, "Error to process line: " + line, ex);
                    }
                } else {
                    //LOG.log(Level.FINER, "No valid line: {0}", line);
                    ko++;
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error to process file: " + inputFile.getName(), ex);
        } finally {
            in.close();
        }
        LOG.log(Level.INFO, "Processed file: {0}, OK = {1}, KO = {2}", new Object[]{outputFile.getName(), ok, ko});
        LOG.log(Level.INFO, "Save file: {0}", outputFile.getName());
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile))));
        for (String target : map.keySet()) {
            out.append("#T\t").append(target);
            out.newLine();
            Map<Integer, Map<String, SurfaceContext>> yearMap = map.get(target);
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
        out.close();
    }

}
