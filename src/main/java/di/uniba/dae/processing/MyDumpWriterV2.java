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

package di.uniba.dae.processing;

import de.tudarmstadt.ukp.wikipedia.mwdumper.importer.DumpWriter;
import de.tudarmstadt.ukp.wikipedia.mwdumper.importer.Page;
import de.tudarmstadt.ukp.wikipedia.mwdumper.importer.Revision;
import de.tudarmstadt.ukp.wikipedia.mwdumper.importer.Siteinfo;
import org.sweble.wikitext.engine.CompiledPage;
import org.sweble.wikitext.engine.CompilerException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.LinkTargetException;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.List;

import static de.tudarmstadt.ukp.wikipedia.api.WikiConstants.SWEBLE_CONFIG;
import di.uniba.dae.utils.Token;
import di.uniba.dae.utils.Utils;
import di.uniba.dae.utils.AnchorToken;
import di.uniba.dae.utils.InternalPlainTextConverter;
import java.util.Calendar;
import java.util.zip.GZIPOutputStream;

/**
 * Implementa DumpWriter ed è in grado di poter gestire i nodi restituiti dalla
 * navigazione del file xml.
 *
 */
public class MyDumpWriterV2 implements DumpWriter {

    private final BufferedWriter out;
    private final SimpleWikiConfiguration config;

    //WINDOW_OF_CONTEXT è il numero di parole che il parser estrare dalla parte sx e dx dell'internal link
    private int WINDOW_OF_CONTEXT = 10;

    private PageTitle pageTitle;
    private int pageId_sing;
    private PageId pageId;
    private int total_number_of_pages;
    private int analyzed_pages;
    private int numberOfRevision;

    public MyDumpWriterV2(File outputfile, int diff) throws IOException, FileNotFoundException, JAXBException {
        this(outputfile, diff, false);
    }

    /**
     * Costruttore della classe MyDumpWriter che setta a 0 il numero di pagine
     * totali, analizzate e configura i settaggi del file mediawiki.
     *
     */
    public MyDumpWriterV2(File outputfile, int diff, boolean compress) throws IOException, FileNotFoundException, JAXBException {
        if (compress) {
            out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputfile))));
        } else {
            out = new BufferedWriter(new FileWriter(outputfile));
        }
        config = new SimpleWikiConfiguration(SWEBLE_CONFIG);
        numberOfRevision = 0;
        analyzed_pages = 0;
        total_number_of_pages = diff;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void writeStartWiki() throws IOException {
    }

    @Override
    public void writeEndWiki() throws IOException {
    }

    @Override
    public void writeSiteinfo(Siteinfo info) throws IOException {
    }

    /**
     * Metodo che viene chiamato ogni volta che si incontra l'inizio di una
     * nuova pagina. Aumenta di 1 il contatore delle pagine visitate e setta le
     * variabili globali che indicano la pagina che si sta analizzando.
     *
     * @param page page that is starting to analyze
     *
     */
    @Override
    public void writeStartPage(Page page) throws IOException {
        numberOfRevision = 0;
        if (!(page.Title.isSpecial() || page.Title.isTalk())) {
            try {
                pageTitle = PageTitle.make(config, page.Title.Text);
                pageId = new PageId(pageTitle, -1);
                pageId_sing = page.Id;
            } catch (LinkTargetException ex) {
                // System.out.println("Error in retrieving the page");
            }
        }
    }

    /**
     * writeEndPage() viene chiamato quando la pagina che si sta analizzando
     * viene chiusa. Setta a null le variabili della pagina in utilizzo.
     *
     */
    @Override
    public void writeEndPage() throws IOException {
        //System.out.println("Page analyzed: " + pageId_sing + " with " + numberOfRevision + " revisions.");
        pageTitle = null;
        pageId = null;
    }

    /**
     * writeRevision(Revision) è il metodo centrale del parser. Infatti grazie
     * ad esso possiamo analizzare le singole revisioni di ogni pagina.
     * Inizializza un compiler che è in grado di processare il testo restituendo
     * una lista di Anchor (tramite il metodo go(Compiler). La lista ogni Anchor
     * è un Internal Link e da esso possiamo estrarre quindi la surface e il
     * link. Quindi per ogni Internal Link viene create una string contentente
     * le informazioni che andiamo a memorizzare nel file csv:
     * <p>
     * Id della pagina dal quale è stato ritrovato Nome della pagina Id della
     * revisione Anno di pubblicazione della revisione Surface dell'Internal
     * link Target dell'Internal link Sequenza di parole che precedono
     * l'Internal Link dalla parte sinistra Sequenza di parole che precedono
     * l'Internal Link dalla parte destra
     * <p>
     * Gli ultimi due attributi sono restituiti dall'oggetto ContextParser che è
     * in grado di restituire la parte sx e dx della frase nel quale l'internal
     * link è presente. Con il metodo flush andiamo a scrivere nel file
     * temporaneo, ogni riga di quello che poi decompresso sarà il csv.
     * <p>
     * L'esecuzione di questo metodo può richiedere anche ore per tutto il dump
     * di wikipedia. Si consiglia di utilizare i dump current dato che, al
     * contrario di quelli history, hanno una sola revisione per pagina.
     *
     * @param revision revision to analyze
     *
     */
    @Override
    public void writeRevision(Revision revision) {
        try {
            if (!(pageId == null)) {
                // Compile the retrieved page
                org.sweble.wikitext.engine.Compiler compiler = new org.sweble.wikitext.engine.Compiler(config);
                CompiledPage cp = compiler.postprocess(pageId, revision.Text, null);
                //System.out.println(pageId_sing + "\t" + revision.Timestamp.get(Calendar.MONTH) + "\t" + revision.Timestamp.get(Calendar.YEAR));
                String Year_of_revision = String.valueOf(revision.Timestamp.get(Calendar.YEAR));
                String Title = pageTitle.getFullTitle();
                int Id_revision = revision.Id;
                //Initializing InternalLinkExtractor
                InternalPlainTextConverter v = new InternalPlainTextConverter(config);
                //Surfing the xml tree and adding IL to the list anchors.
                List<Token> tokens = (List<Token>) v.go(cp);
                for (int i = 0; i < tokens.size(); i++) {
                    if (tokens.get(i) instanceof AnchorToken) {
                        //String to write in the file.
                        StringBuilder str_to_write = new StringBuilder();
                        str_to_write.append(pageId_sing).append("\t").
                                append(Title).append("\t").
                                append(Integer.toString(Id_revision)).append("\t").
                                append(Year_of_revision).append("\t").
                                append(tokens.get(i).getSurface()).append("\t").
                                append(((AnchorToken) tokens.get(i)).getTarget()).append("\t").
                                append(extractLeft(tokens, i)).append("\t").
                                append(extractRight(tokens, i)).append("\t");
                        out.append(str_to_write);
                        out.newLine();
                    }
                }
                out.flush();
            }
            numberOfRevision += 1;
        } catch (IOException | CompilerException ex) {
            // System.out.println("Error parsing revision \t" + revision.Id + "\t for page\t" + pageId_sing);

        }
    }

    private String extractLeft(List<Token> tokens, int o) throws IOException {
        StringBuilder sb = new StringBuilder();
        int s = Math.max(0, o - WINDOW_OF_CONTEXT);
        for (int i = s; i < o; i++) {
            sb.append(tokens.get(i).getSurface()).append(" ");
        }
        return Utils.stringListToString(Utils.tokenize(sb.toString()));
    }

    private String extractRight(List<Token> tokens, int o) throws IOException {
        StringBuilder sb = new StringBuilder();
        int s = Math.min(tokens.size() - 1, o + WINDOW_OF_CONTEXT);
        for (int i = o + 1; i <= s; i++) {
            sb.append(tokens.get(i).getSurface()).append(" ");
        }
        return Utils.stringListToString(Utils.tokenize(sb.toString()));
    }

}
