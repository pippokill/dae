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
package di.uniba.dae.api;

import di.uniba.dae.utils.Utils;
import di.uniba.dae.utils.Entry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author pierpaolo
 */
public class CreateIndex {

    //TEST ATTRIBUTES
    private static int minOccBow = 5;

    private static int maxBowSize = 10000;

    private static int minSurfaceCount = 5;

    private static final Logger LOG = Logger.getLogger(CreateIndex.class.getName());

    static Options options;

    static CommandLineParser cmdParser = new DefaultParser();

    static {
        options = new Options();
        options.addOption("i", true, "Input dateset")
                .addOption("o", true, "Index directory")
                .addOption("mo", true, "Min token occurrences (default 5)")
                .addOption("mb", true, "Max context BoW size (default 1000)")
                .addOption("ms", true, "Min surface count (default 5)");
    }

    private static String buildContextText(String[] split) {
        List<Entry> tokens = new ArrayList();
        for (int i = 4; i < split.length; i++) {
            String[] bv = split[i].split(" ");
            int occ = Integer.parseInt(bv[1]);
            if (occ >= minOccBow) {
                tokens.add(new Entry(bv[0], occ));
            }
        }
        Collections.sort(tokens, Comparator.reverseOrder());
        if (tokens.size() > maxBowSize) {
            tokens = tokens.subList(0, maxBowSize);
        }
        StringBuilder sb = new StringBuilder();
        for (Entry t : tokens) {
            for (int i = 0; i < t.getCount(); i++) {
                sb.append(t.getKey()).append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            CommandLine cmd = cmdParser.parse(options, args);
            if ((cmd.hasOption("i") || cmd.hasOption("o"))) {
                try {
                    File inputfile = new File(cmd.getOptionValue("i"));
                    LOG.log(Level.INFO, "Open dataset: {0}", inputfile.getAbsolutePath());
                    File indexFile = new File(cmd.getOptionValue("o"));
                    LOG.log(Level.INFO, "Index dir: {0}", indexFile.getAbsolutePath());
                    minOccBow = Integer.parseInt(cmd.getOptionValue("mo", "5"));
                    maxBowSize = Integer.parseInt(cmd.getOptionValue("mb", "10000"));
                    minSurfaceCount = Integer.parseInt(cmd.getOptionValue("ms", "5"));
                    LOG.log(Level.INFO, "min-occ-bow {0}, max-bow-size {1}, min-surface-count {2}", new Object[]{minOccBow, maxBowSize, minSurfaceCount});
                    IndexWriterConfig config = new IndexWriterConfig(new WhitespaceAnalyzer());
                    IndexWriter writer = new IndexWriter(FSDirectory.open(indexFile.toPath()), config);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputfile))));
                    String target = "";
                    FieldType tvfieldType = new FieldType(TextField.TYPE_NOT_STORED);
                    tvfieldType.setStoreTermVectors(true);
                    long di = 0;
                    long ei = 0;
                    while (reader.ready()) {
                        String line = reader.readLine();
                        try {
                            if (line.startsWith("#")) {
                                target = line.substring(3);
                                ei++;
                                if (ei % 10000 == 0) {
                                    LOG.log(Level.INFO, "Processed {0} entities and {1} rows", new Object[]{ei, di});
                                }
                            } else {
                                String[] split = line.split("\t");
                                int sc = Integer.parseInt(split[2]);
                                if (sc >= minSurfaceCount) {
                                    Document doc = new Document();
                                    doc.add(new StringField("target", target.replaceAll("\\s+", "_"), Field.Store.YES));
                                    doc.add(new TextField("target_a", target.toLowerCase(), Field.Store.NO));
                                    doc.add(new StringField("year", split[0], Field.Store.YES));
                                    doc.add(new IntPoint("year_int", Integer.parseInt(split[0])));
                                    doc.add(new StringField("surface", split[1], Field.Store.YES));
                                    doc.add(new TextField("surface_a", Utils.stringListToString(Utils.tokenize(split[1])), Field.Store.NO));
                                    doc.add(new StoredField("surface_count_s", sc));
                                    doc.add(new IntPoint("surface_count", sc));
                                    doc.add(new Field("context", buildContextText(split), tvfieldType));
                                    writer.addDocument(doc);
                                    di++;
                                }
                            }
                        } catch (IOException ex) {
                            LOG.log(Level.SEVERE, "Error to process line: " + line.substring(0, Math.min(100, line.length())), ex);
                        }
                    }
                    reader.close();
                    writer.close();
                    LOG.log(Level.INFO, "Stored {0} entities and {1} documents", new Object[]{ei, di});
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("CreateIndex - Creates the index given the input dataset", options);
            }
        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
