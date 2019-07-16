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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 *
 * @author pierpaolo
 */
public class Utils {

    public static void downloadFile(URL url, File file) throws IOException {
        FileUtils.copyURLToFile(url, file);
    }

    public static List<String> createDumpList(String dumpdate) throws IOException {
        URL dumpStatusURL = new URL("https://dumps.wikimedia.org/enwiki/" + dumpdate + "/dumpstatus.json");
        String dumpStatusJson = IOUtils.toString(dumpStatusURL);
        JsonElement jelement = new JsonParser().parse(dumpStatusJson);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.get("jobs").getAsJsonObject().get("metahistorybz2dump").getAsJsonObject();
        if (jobject.get("status").getAsString().equals("done")) {
            jobject = jobject.get("files").getAsJsonObject();
            List<String> list = new ArrayList<>(jobject.keySet());
            long bytes = 0;
            for (String s : list) {
                JsonObject elem = jobject.getAsJsonObject(s);
                bytes += elem.get("size").getAsLong();
            }
            Logger.getLogger(Utils.class.getName()).log(Level.INFO, "Total size: {0}", bytes);
            Collections.sort(list);
            return list;
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DumpItem> createDumpItemList(String dumpdate) throws IOException {
        URL dumpStatusURL = new URL("https://dumps.wikimedia.org/enwiki/" + dumpdate + "/dumpstatus.json");
        String dumpStatusJson = IOUtils.toString(dumpStatusURL);
        JsonElement jelement = new JsonParser().parse(dumpStatusJson);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.get("jobs").getAsJsonObject().get("metahistorybz2dump").getAsJsonObject();
        if (jobject.get("status").getAsString().equals("done")) {
            jobject = jobject.get("files").getAsJsonObject();
            List<DumpItem> list = new ArrayList<>();
            long bytes = 0;
            for (String s : jobject.keySet()) {
                JsonObject elem = jobject.getAsJsonObject(s);
                long size = elem.get("size").getAsLong();
                bytes += size;
                list.add(new DumpItem(s, size));
            }
            Logger.getLogger(Utils.class.getName()).log(Level.INFO, "Total size: {0}", bytes);
            Collections.sort(list);
            return list;
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DumpItem> createDumpItemList(File dir) throws IOException {
        if (dir.isDirectory()) {
            List<DumpItem> list = new ArrayList<>();
            File[] listFiles = dir.listFiles();
            for (File file : listFiles) {
                if (file.isFile() && file.getName().endsWith(".bz2")) {
                    list.add(new DumpItem(file.getAbsolutePath(), file.length()));
                }
            }
            Collections.sort(list);
            return list;
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DumpItem> createDumpItemListFromLog(String dir, File logfile) throws IOException {
        List<File> files = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(logfile));
        while (reader.ready()) {
            String line = reader.readLine();
            if (line.startsWith("INFO: Downloaded")) {
                File file = new File(dir + "/" + line.substring(17, line.indexOf(",")));
                files.add(file);
            }
        }
        reader.close();
        List<DumpItem> list = new ArrayList<>(files.size());
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".bz2")) {
                list.add(new DumpItem(file.getAbsolutePath(), file.length()));
            }
        }
        Collections.sort(list);
        return list;
    }

    public static Set<String> loadSetFromFile(File file) throws IOException {
        Set<String> set = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            set.add(reader.readLine());
        }
        reader.close();
        return set;
    }

    public static synchronized void saveSetInFile(File file, Set<String> set) throws IOException {
        if (set == null) {
            return;
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String s : set) {
            writer.write(s);
            writer.newLine();
        }
        writer.close();
    }

    public static int getPagesNumbers(File dumpfile) {
        String[] pages = dumpfile.getName().split("p");
        int page1 = Integer.valueOf(pages[2]);
        int page2 = Integer.valueOf(pages[3].split(".bz2")[0]);
        int diff = page2 - page1;
        System.out.println("Analyzing " + diff + " pages... \b");
        return diff;
    }

    public static String readTextFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            sb.append(reader.readLine()).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static List<String> tokenize(String text) throws IOException {
        List<String> list = new ArrayList<>();
        Analyzer a = new StandardAnalyzer(CharArraySet.EMPTY_SET);
        TokenStream tokenStream = a.tokenStream("wiki", text);
        tokenStream.reset();
        CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
        while (tokenStream.incrementToken()) {
            String token = cattr.toString();
            list.add(token);
        }
        tokenStream.end();
        return list;
    }

    public static List<String> tokenize(String text, Analyzer a) throws IOException {
        List<String> list = new ArrayList<>();
        TokenStream tokenStream = a.tokenStream("wiki", text);
        tokenStream.reset();
        CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
        while (tokenStream.incrementToken()) {
            String token = cattr.toString();
            list.add(token);
        }
        tokenStream.end();
        return list;
    }

    public static String stringListToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        if (list.size() > 0) {
            sb.append(list.get(0));
        }
        for (int i = 1; i < list.size(); i++) {
            sb.append(" ").append(list.get(i));
        }
        return sb.toString();
    }

    public static List<Entry> loadDict(File file) throws IOException {
        return loadDict(file, 0);
    }

    public static List<Entry> loadDict(File file, int minocc) throws IOException {
        List<Entry> dict = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            String[] split = reader.readLine().split("\t");
            int occ = Integer.parseInt(split[1]);
            if (occ >= minocc) {
                dict.add(new Entry(split[0], occ));
            }
        }
        return dict;
    }

    public static double simBow(Map<String, Integer> b1, Map<String, Integer> b2) {
        double sim = 0;
        double n1 = 0;
        for (String k1 : b1.keySet()) {
            Integer v2 = b2.get(k1);
            if (v2 != null) {
                sim += b1.get(k1).doubleValue() * v2.doubleValue();
            }
            n1 += Math.pow(b1.get(k1).doubleValue(), 2);
        }
        double n2 = 0;
        for (Integer v2 : b2.values()) {
            n2 += Math.pow(v2.doubleValue(), 2);
        }
        if (n1 != 0 && n2 != 0) {
            return sim / (Math.sqrt(n1) * Math.sqrt(n2));
        } else {
            return 0;
        }
    }

    public static Map<String, Integer> mergeBow(Map<String, Integer> b1, Map<String, Integer> b2) {
        Map<String, Integer> b = new HashMap<>(b1);
        for (Map.Entry<String, Integer> e2 : b2.entrySet()) {
            Integer v = b.get(e2.getKey());
            if (v == null) {
                b.put(e2.getKey(), e2.getValue());
            } else {
                b.put(e2.getKey(), e2.getValue() + v);
            }
        }
        return b;
    }

}
