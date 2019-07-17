/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.dae.split;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 *
 * @author pierpaolo
 */
public class DumpSplitWriter {

    public static void splitDump(File inputfile, long bytes, boolean deleteInput) throws IOException {
        if (inputfile.length() > bytes) {
            int part = 1;
            String name = inputfile.getName();
            BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(new FileInputStream(inputfile));
            BufferedReader reader = new BufferedReader(new InputStreamReader(bzIn, "UTF-8"));
            File outputfile = new File(inputfile.getParent() + "/" + name.replace(".bz2", "") + "-" + part + ".bz2");
            BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(new FileOutputStream(outputfile));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bzOut, "UTF-8"));
            StringBuilder header = new StringBuilder();
            boolean headerRead = false;
            String line;
            while ((line = reader.readLine()) !=null) {
                if (!headerRead && !line.trim().equals("<page>")) {
                    header.append(line).append("\n");
                } else {
                    headerRead = true;
                }
                writer.append(line).append("\n");
                if (line.trim().equals("</page>")) {
                    writer.flush();
                    if (outputfile.length() >= bytes) {
                        writer.append("</mediawiki>").append("\n");
                        writer.close();
                        part++;
                        outputfile = new File(inputfile.getParent() + "/" + name.replace(".bz2", "") + "-" + part + ".bz2");
                        bzOut = new BZip2CompressorOutputStream(new FileOutputStream(outputfile));
                        writer = new BufferedWriter(new OutputStreamWriter(bzOut, "UTF-8"));
                        writer.append(header.toString()).append("\n");
                    }
                }
            }
            reader.close();
            writer.close();
            if (deleteInput) {
                inputfile.delete();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            splitDump(new File("/home/pierpaolo/Scaricati/temp/dae/20190701/download/enwiki-20190701-pages-meta-history10.xml-p2534977p2535598.bz2"), 1024*1024, false);
        } catch (IOException ex) {
            Logger.getLogger(DumpSplitWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
