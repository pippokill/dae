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
package di.uniba.dae.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author pierpaolo
 */
public class CreateDirtySampleData {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            Set<String> set = new HashSet<>();
            set.add("Donald Trump");
            set.add("Apple");
            set.add("IBM");
            set.add("Microsoft");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("/media/pierpaolo/82883105-79c9-491a-a404-94ead16bcba2/wikihistory/dataset/wikihistory_dataset_01032019_1M.gz"))));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("/media/pierpaolo/82883105-79c9-491a-a404-94ead16bcba2/wikihistory/dataset/wikihistory_dataset_sample.gz"))));
            boolean w = false;
            boolean end = false;
            while (reader.ready() && !end) {
                String line = reader.readLine();
                if (line.startsWith("#T")) {
                    end = set.isEmpty();
                    String[] split = line.split("\t");
                    if (set.contains(split[1])) {
                        set.remove(split[1]);
                        writer.append(line);
                        writer.newLine();
                        w = true;
                    } else {
                        w = false;
                    }
                } else {
                    if (w) {
                        writer.append(line);
                        writer.newLine();
                    }
                }
            }
            reader.close();
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(CreateDirtySampleData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
