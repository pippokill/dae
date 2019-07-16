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

import static de.tudarmstadt.ukp.wikipedia.api.WikiConstants.SWEBLE_CONFIG;
import di.uniba.dae.utils.Token;
import di.uniba.dae.utils.Utils;
import di.uniba.dae.utils.AnchorToken;
import di.uniba.dae.utils.InternalPlainTextConverter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.sweble.wikitext.engine.CompiledPage;
import org.sweble.wikitext.engine.CompilerException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.LinkTargetException;

/**
 *
 * @author pierpaolo
 */
public class TestParsing {

    private static final int WINDOW_OF_CONTEXT = 5;

    private static String extractLeft(List<Token> tokens, int o) throws IOException {
        StringBuilder sb = new StringBuilder();
        int s = Math.max(0, o - WINDOW_OF_CONTEXT);
        for (int i = s; i < o; i++) {
            sb.append(tokens.get(i).getSurface()).append(" ");
        }
        return Utils.stringListToString(Utils.tokenize(sb.toString()));
    }

    private static String extractRight(List<Token> tokens, int o) throws IOException {
        StringBuilder sb = new StringBuilder();
        int s = Math.min(tokens.size() - 1, o + WINDOW_OF_CONTEXT);
        for (int i = o + 1; i <= s; i++) {
            sb.append(tokens.get(i).getSurface()).append(" ");
        }
        return Utils.stringListToString(Utils.tokenize(sb.toString()));
    }

    private static void parsingPage(String text) throws IOException, JAXBException, LinkTargetException, CompilerException {
        SimpleWikiConfiguration config = new SimpleWikiConfiguration(SWEBLE_CONFIG);
        PageTitle pageTitle = PageTitle.make(config, "TARGET");
        InternalPlainTextConverter v = new InternalPlainTextConverter(config);
        org.sweble.wikitext.engine.Compiler compiler = new org.sweble.wikitext.engine.Compiler(config);
        CompiledPage cp = compiler.postprocess(new PageId(pageTitle, 0), text, null);
        List<Token> tokens = (List<Token>) v.go(cp);
        for (int i = 0; i < 100; i++) {
            String extractLeft = extractLeft(tokens, i);
            String extractRight = extractRight(tokens, i);
            System.out.print(tokens.get(i));
            if (tokens.get(i) instanceof AnchorToken) {
                System.out.print(" LEFT <- " + extractLeft);
                System.out.print(" RIGHT -> " + extractRight);
            }
            System.out.println();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            String text = Utils.readTextFile(new File("/home/pierpaolo/Scaricati/temp/Italy.mwiki"));
            parsingPage(text);
        } catch (IOException | JAXBException | LinkTargetException | CompilerException ex) {
            Logger.getLogger(TestParsing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
