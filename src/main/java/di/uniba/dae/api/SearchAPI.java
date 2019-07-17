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
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author pierpaolo
 */
public class SearchAPI {

    private final File indexdir;

    private IndexSearcher searcher;

    /**
     *
     * @param indexdir
     */
    public SearchAPI(File indexdir) {
        this.indexdir = indexdir;
    }

    /**
     *
     * @throws IOException
     */
    public void open() throws IOException {
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(indexdir.toPath())));
    }

    /**
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (searcher != null) {
            searcher.getIndexReader().close();
        }
    }

    private Map<String, Integer> topdocs2contextMap(TopDocs topdocs) throws IOException {
        Map<String, Integer> bow = new Object2IntOpenHashMap();
        for (ScoreDoc scoreDoc : topdocs.scoreDocs) {
            Terms termVector = searcher.getIndexReader().getTermVector(scoreDoc.doc, "context");
            if (termVector != null) {
                TermsEnum te = termVector.iterator();
                while (te.next() != null) {
                    String termText = te.term().utf8ToString();
                    PostingsEnum pe = te.postings(null, PostingsEnum.FREQS);
                    pe.nextDoc();
                    int freq = pe.freq();
                    bow.put(termText, freq);
                }
            }
        }
        return bow;
    }

    private Map<String, Integer> topdocs2surfaceMap(TopDocs topdocs) throws IOException {
        Map<String, Integer> bow = new Object2IntOpenHashMap();
        for (ScoreDoc scoreDoc : topdocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String surface = doc.get("surface");
            int c = doc.getField("surface_count_s").numericValue().intValue();
            Integer v = bow.get(surface);
            if (v == null) {
                bow.put(surface, c);
            } else {
                bow.put(surface, v + c);
            }
        }
        return bow;
    }

    private Map<String, Integer> topdocs2targetMap(TopDocs topdocs) throws IOException {
        Map<String, Integer> bow = new Object2IntOpenHashMap();
        for (ScoreDoc scoreDoc : topdocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String surface = doc.get("target");
            int c = doc.getField("surface_count_s").numericValue().intValue();
            Integer v = bow.get(surface);
            if (v == null) {
                bow.put(surface, c);
            } else {
                bow.put(surface, v + c);
            }
        }
        return bow;
    }

    /**
     *
     * @param target
     * @param year
     * @return
     * @throws IOException
     */
    public Map<String, Integer> getBowByTargetAndYear(String target, int year) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new TermQuery(new Term("target", target.replaceAll("\\s+", "_"))), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newExactQuery("year_int", year), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2contextMap(topdocs);
    }

    /**
     *
     * @param target
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public Map<String, Integer> getBowByTargetAndYear(String target, int lyear, int uyear) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new TermQuery(new Term("target", target.replaceAll("\\s+", "_"))), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newRangeQuery("year_int", lyear, uyear), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2contextMap(topdocs);
    }

    /**
     *
     * @param target
     * @param year
     * @return
     * @throws IOException
     */
    public Map<String, Integer> getSurfaceByTargetAndYear(String target, int year) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new TermQuery(new Term("target", target.replaceAll("\\s+", "_"))), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newExactQuery("year_int", year), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2surfaceMap(topdocs);
    }

    /**
     *
     * @param target
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public Map<String, Integer> getSurfaceByTargetAndYear(String target, int lyear, int uyear) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new TermQuery(new Term("target", target.replaceAll("\\s+", "_"))), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newRangeQuery("year_int", lyear, uyear), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2surfaceMap(topdocs);
    }

    /**
     *
     * @param surface
     * @param year
     * @return
     * @throws IOException
     */
    public Map<String, Integer> getTargetBySurfaceAndYear(String surface, int year) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new TermQuery(new Term("surface", surface)), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newExactQuery("year_int", year), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2targetMap(topdocs);
    }

    /**
     *
     * @param surface
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public Map<String, Integer> getTargetBySurfaceAndYear(String surface, int lyear, int uyear) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        qb.add(new TermQuery(new Term("surface", surface)), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newRangeQuery("year_int", lyear, uyear), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2targetMap(topdocs);
    }

    private List<String> getSurfaceTokens(String surface) throws IOException {
        return Utils.tokenize(surface);
    }

    private List<String> getTargetTokens(String target) throws IOException {
        return Utils.tokenize(target, new WhitespaceAnalyzer());
    }

    /**
     *
     * @param target
     * @param and
     * @param year
     * @return
     * @throws IOException
     */
    public Map<String, Integer> searchSurfaceByTargetAndYear(String target, boolean and, int year) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        BooleanQuery.Builder tqb = new BooleanQuery.Builder();
        List<String> tokens = getTargetTokens(target);
        for (String token : tokens) {
            if (and) {
                tqb.add(new TermQuery(new Term("target_a", token)), BooleanClause.Occur.MUST);
            } else {
                tqb.add(new TermQuery(new Term("target_a", token)), BooleanClause.Occur.SHOULD);
            }
        }
        qb.add(tqb.build(), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newExactQuery("year_int", year), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2surfaceMap(topdocs);
    }

    /**
     *
     * @param target
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public Map<String, Integer> searchSurfaceByTargetAndYear(String target, boolean and, int lyear, int uyear) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        BooleanQuery.Builder tqb = new BooleanQuery.Builder();
        List<String> tokens = getTargetTokens(target);
        for (String token : tokens) {
            if (and) {
                tqb.add(new TermQuery(new Term("target_a", token)), BooleanClause.Occur.MUST);
            } else {
                tqb.add(new TermQuery(new Term("target_a", token)), BooleanClause.Occur.SHOULD);
            }
        }
        qb.add(tqb.build(), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newRangeQuery("year_int", lyear, uyear), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2surfaceMap(topdocs);
    }

    /**
     *
     * @param surface
     * @param and
     * @param year
     * @return
     * @throws IOException
     */
    public Map<String, Integer> searchTargetBySurfaceAndYear(String surface, boolean and, int year) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        BooleanQuery.Builder tqb = new BooleanQuery.Builder();
        List<String> tokens = getSurfaceTokens(surface);
        for (String token : tokens) {
            if (and) {
                tqb.add(new TermQuery(new Term("surface_a", token)), BooleanClause.Occur.MUST);
            } else {
                tqb.add(new TermQuery(new Term("surface_a", token)), BooleanClause.Occur.SHOULD);
            }
        }
        qb.add(tqb.build(), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newExactQuery("year_int", year), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2targetMap(topdocs);
    }

    /**
     *
     * @param surface
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public Map<String, Integer> searchTargetBySurfaceAndYear(String surface, boolean and, int lyear, int uyear) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        BooleanQuery.Builder tqb = new BooleanQuery.Builder();
        List<String> tokens = getSurfaceTokens(surface);
        for (String token : tokens) {
            if (and) {
                tqb.add(new TermQuery(new Term("surface_a", token)), BooleanClause.Occur.MUST);
            } else {
                tqb.add(new TermQuery(new Term("surface_a", token)), BooleanClause.Occur.SHOULD);
            }
        }
        qb.add(tqb.build(), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newRangeQuery("year_int", lyear, uyear), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2targetMap(topdocs);
    }

    /**
     *
     * @param target
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> targetBowTimeSeries(String target, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBow = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bow = getBowByTargetAndYear(target, year);
            Map<String, Integer> newBow = Utils.mergeBow(preBow, bow);
            results.add(Utils.simBow(preBow, newBow));
            preBow = newBow;
        }
        return results;
    }

    /**
     *
     * @param target
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> targetSurfaceTimeSeries(String target, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBow = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bow = getSurfaceByTargetAndYear(target, year);
            Map<String, Integer> newBow = Utils.mergeBow(preBow, bow);
            results.add(Utils.simBow(preBow, newBow));
            preBow = newBow;
        }
        return results;
    }

    /**
     *
     * @param target
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> searchTargetSurfaceTimeSeries(String target, boolean and, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBow = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bow = searchSurfaceByTargetAndYear(target, and, year);
            Map<String, Integer> newBow = Utils.mergeBow(preBow, bow);
            results.add(Utils.simBow(preBow, newBow));
            preBow = newBow;
        }
        return results;
    }

    /**
     *
     * @param surface
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> searchSurfaceTargetTimeSeries(String surface, boolean and, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBow = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bow = searchTargetBySurfaceAndYear(surface, and, year);
            Map<String, Integer> newBow = Utils.mergeBow(preBow, bow);
            results.add(Utils.simBow(preBow, newBow));
            preBow = newBow;
        }
        return results;
    }

    /**
     *
     * @param surface
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> surfaceTargetTimeSeries(String surface, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBow = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bow = getTargetBySurfaceAndYear(surface, year);
            Map<String, Integer> newBow = Utils.mergeBow(preBow, bow);
            results.add(Utils.simBow(preBow, newBow));
            preBow = newBow;
        }
        return results;
    }

    /**
     *
     * @param target1
     * @param target2
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> compareTarget(String target1, String target2, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBowT1 = new Object2IntOpenHashMap();
        Map<String, Integer> preBowT2 = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bowT1 = getBowByTargetAndYear(target1, year);
            Map<String, Integer> bowT2 = getBowByTargetAndYear(target2, year);
            Map<String, Integer> newBowT1 = Utils.mergeBow(preBowT1, bowT1);
            Map<String, Integer> newBowT2 = Utils.mergeBow(preBowT2, bowT2);
            results.add(Utils.simBow(newBowT1, newBowT2));
            preBowT1 = newBowT1;
            preBowT2 = newBowT2;
        }
        return results;
    }
    
    /**
     *
     * @param target1
     * @param target2
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> compareTargetPoint(String target1, String target2, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bowT1 = getBowByTargetAndYear(target1, year);
            Map<String, Integer> bowT2 = getBowByTargetAndYear(target2, year);
            results.add(Utils.simBow(bowT1, bowT2));
        }
        return results;
    }

    /**
     *
     * @param surface1
     * @param surface2
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> compareSurface(String surface1, String surface2, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBowT1 = new Object2IntOpenHashMap();
        Map<String, Integer> preBowT2 = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bowT1 = getTargetBySurfaceAndYear(surface1, year);
            Map<String, Integer> bowT2 = getTargetBySurfaceAndYear(surface2, year);
            Map<String, Integer> newBowT1 = Utils.mergeBow(preBowT1, bowT1);
            Map<String, Integer> newBowT2 = Utils.mergeBow(preBowT2, bowT2);
            results.add(Utils.simBow(newBowT1, newBowT2));
            preBowT1 = newBowT1;
            preBowT2 = newBowT2;
        }
        return results;
    }

    /**
     *
     * @param surface1
     * @param surface2
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> compareSearchSurface(String surface1, String surface2, boolean and, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBowT1 = new Object2IntOpenHashMap();
        Map<String, Integer> preBowT2 = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bowT1 = searchTargetBySurfaceAndYear(surface1, and, year);
            Map<String, Integer> bowT2 = searchTargetBySurfaceAndYear(surface2, and, year);
            Map<String, Integer> newBowT1 = Utils.mergeBow(preBowT1, bowT1);
            Map<String, Integer> newBowT2 = Utils.mergeBow(preBowT2, bowT2);
            results.add(Utils.simBow(newBowT1, newBowT2));
            preBowT1 = newBowT1;
            preBowT2 = newBowT2;
        }
        return results;
    }

    /**
     *
     * @param context
     * @param and
     * @param year
     * @return
     * @throws IOException
     */
    public Map<String, Integer> searchTargetFromContext(String context, boolean and, int year) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        BooleanQuery.Builder tqb = new BooleanQuery.Builder();
        List<String> tokens = getSurfaceTokens(context);
        for (String token : tokens) {
            if (and) {
                tqb.add(new TermQuery(new Term("context", token)), BooleanClause.Occur.MUST);
            } else {
                tqb.add(new TermQuery(new Term("context", token)), BooleanClause.Occur.SHOULD);
            }
        }
        qb.add(tqb.build(), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newExactQuery("year_int", year), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2targetMap(topdocs);
    }

    /**
     *
     * @param context
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public Map<String, Integer> searchTargetFromContext(String context, boolean and, int lyear, int uyear) throws IOException {
        BooleanQuery.Builder qb = new BooleanQuery.Builder();
        BooleanQuery.Builder tqb = new BooleanQuery.Builder();
        List<String> tokens = getSurfaceTokens(context);
        for (String token : tokens) {
            if (and) {
                tqb.add(new TermQuery(new Term("context", token)), BooleanClause.Occur.MUST);
            } else {
                tqb.add(new TermQuery(new Term("context", token)), BooleanClause.Occur.SHOULD);
            }
        }
        qb.add(tqb.build(), BooleanClause.Occur.MUST);
        qb.add(IntPoint.newRangeQuery("year_int", lyear, uyear), BooleanClause.Occur.MUST);
        TopDocs topdocs = searcher.search(qb.build(), Integer.MAX_VALUE);
        return topdocs2targetMap(topdocs);
    }
    
    /**
     *
     * @param context
     * @param and
     * @param lyear
     * @param uyear
     * @return
     * @throws IOException
     */
    public List<Double> contextTimeSeries(String context, boolean and, int lyear, int uyear) throws IOException {
        List<Double> results = new ArrayList<>(uyear - lyear + 1);
        Map<String, Integer> preBow = new Object2IntOpenHashMap();
        for (int year = lyear; year <= uyear; year++) {
            Map<String, Integer> bow = searchTargetFromContext(context, and, year);
            Map<String, Integer> newBow = Utils.mergeBow(preBow, bow);
            results.add(Utils.simBow(preBow, newBow));
            preBow = newBow;
        }
        return results;
    }

}
