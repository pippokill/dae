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

import di.uniba.dae.utils.Entry;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pierpaolo
 */
public class TestSearch {

    private static void printBow(Map<String, Integer> bow) {
        for (Map.Entry<String, Integer> e : bow.entrySet()) {
            System.out.println(e.getKey() + "\t" + e.getValue());
        }
    }

    private static void printSortedBow(Map<String, Integer> bow) {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : bow.entrySet()) {
            list.add(new Entry(e.getKey(), e.getValue()));
        }
        Collections.sort(list, Comparator.reverseOrder());
        for (Entry e : list) {
            System.out.println(e.getKey() + "\t" + e.getCount());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            SearchAPI api = new SearchAPI(new File("/media/pierpaolo/82883105-79c9-491a-a404-94ead16bcba2/wikihistory/dataset/index_01032019_1M_5_10000_2"));
            api.open();
            /*
            System.out.println("====2005=====");
            Map<String, Integer> bow = api.getTargetBySurfaceAndYear("cell",  2005);
            printSortedBow(bow);
            System.out.println("=====2006=====");
            bow = api.getTargetBySurfaceAndYear("cell", 2006);
            printSortedBow(bow);
            System.out.println("======2011====");
            bow = api.getTargetBySurfaceAndYear("cell", 2011);
            printSortedBow(bow);*/

            List<Double> ts = api.compareTargetPoint("United States", "Donald Trump", 2004, 2019);
            System.out.println();
            for (Double d : ts) {
                System.out.print(d + " ");
            }
            System.out.println();
            System.out.println("===============================");
            ts = api.compareTargetPoint("United States", "President of the United States", 2004, 2019);
            System.out.println();
            for (Double d : ts) {
                System.out.print(d + " ");
            }
            System.out.println();
            System.out.println("===============================");
            ts = api.compareTargetPoint("Donald Trump", "President of the United States", 2004, 2019);
            System.out.println();
            for (Double d : ts) {
                System.out.print(d + " ");
            }
            /*
            System.out.println();
            ts = api.compareSearchSurface("microsoft", "Apple",true, 2007, 2019);
            System.out.println();
            for (Double d : ts) {
                System.out.print(d + " ");
            }
            System.out.println();
            ts = api.compareSearchSurface("Microsoft", "IBM",true, 2007, 2019);
            System.out.println();
            for (Double d : ts) {
                System.out.print(d + " ");
            }
            System.out.println();*/
            
            /*List<Double> ts = api.surfaceTargetTimeSeries("tweet", 2001, 2019);
            System.out.println();
            for (Double d:ts) {
                System.out.print(d+" ");
            }
            System.out.println();
            /*ts = api.searchSurfaceTargetTimeSeries("Italy", 2006, 2018);
            System.out.println();
            for (Double d:ts) {
                System.out.print(d+" ");
            }
            System.out.println();*/
            api.close();
        } catch (IOException ex) {
            Logger.getLogger(TestSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
