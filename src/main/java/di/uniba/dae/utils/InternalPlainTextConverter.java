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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.sweble.wikitext.engine.Page;
import org.sweble.wikitext.engine.utils.EntityReferences;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.encval.IllegalCodePoint;
import org.sweble.wikitext.lazy.parser.Bold;
import org.sweble.wikitext.lazy.parser.ExternalLink;
import org.sweble.wikitext.lazy.parser.HorizontalRule;
import org.sweble.wikitext.lazy.parser.ImageLink;
import org.sweble.wikitext.lazy.parser.InternalLink;
import org.sweble.wikitext.lazy.parser.Italics;
import org.sweble.wikitext.lazy.parser.Itemization;
import org.sweble.wikitext.lazy.parser.ItemizationItem;
import org.sweble.wikitext.lazy.parser.MagicWord;
import org.sweble.wikitext.lazy.parser.Paragraph;
import org.sweble.wikitext.lazy.parser.Section;
import org.sweble.wikitext.lazy.parser.Url;
import org.sweble.wikitext.lazy.parser.Whitespace;
import org.sweble.wikitext.lazy.parser.XmlElement;
import org.sweble.wikitext.lazy.preprocessor.TagExtension;
import org.sweble.wikitext.lazy.preprocessor.Template;
import org.sweble.wikitext.lazy.preprocessor.TemplateArgument;
import org.sweble.wikitext.lazy.preprocessor.TemplateParameter;
import org.sweble.wikitext.lazy.preprocessor.XmlComment;
import org.sweble.wikitext.lazy.utils.StringConverterPartial;
import org.sweble.wikitext.lazy.utils.XmlCharRef;
import org.sweble.wikitext.lazy.utils.XmlEntityRef;

import de.fau.cs.osr.ptk.common.AstVisitor;
import de.fau.cs.osr.ptk.common.ast.AstNode;
import de.fau.cs.osr.ptk.common.ast.NodeList;
import de.fau.cs.osr.ptk.common.ast.Text;
import de.fau.cs.osr.utils.StringUtils;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.lazy.LinkTargetException;

public class InternalPlainTextConverter extends AstVisitor {

    private final SimpleWikiConfiguration config;

    private static final Logger LOG = Logger.getLogger(InternalPlainTextConverter.class.getName());

    private List<Token> tokens = new ArrayList<>();

    private StringBuilder sb;

    // =========================================================================
    /**
     * Creates a new visitor that produces a plain text String representation of
     * a parsed Wikipedia article s
     */
    public InternalPlainTextConverter() {
        SimpleWikiConfiguration config = null;
        try {
            config = new SimpleWikiConfiguration(WikiConstants.SWEBLE_CONFIG);
        } catch (IOException | JAXBException e) {
            LOG.log(Level.SEVERE, null, e);
        }
        this.config = config;
    }

    public InternalPlainTextConverter(SimpleWikiConfiguration config) {
        this.config = config;
    }

    @Override
    protected boolean before(AstNode node) {
        // This method is called by go() before visitation starts
        sb = new StringBuilder();
        return super.before(node);
    }

    @Override
    protected Object after(AstNode node, Object result) {
        finishLine();

        // This method is called by go() after visitation has finished
        // The return value will be passed to go() which passes it to the caller
        return tokens;
    }

    // =========================================================================
    public void visit(AstNode n) {
        // Fallback for all nodes that are not explicitly handled below
        //		write("<");
        //		write(n.getNodeName());
        //		write(" />");
        iterate(n);
    }

    public void visit(NodeList n) {
        iterate(n);
    }

    public void visit(Page p) {
        iterate(p.getContent());
    }

    public void visit(Text text) {
        write(text.getContent());
    }

    public void visit(Whitespace w) {
        write(" ");
    }

    public void visit(Bold b) {
        //write("**");
        iterate(b.getContent());
        //write("**");
    }

    public void visit(Italics i) {
        //write("//");
        iterate(i.getContent());
        //write("//");
    }

    public void visit(XmlCharRef cr) {
        write(Character.toChars(cr.getCodePoint()));
    }

    public void visit(XmlEntityRef er) {
        String ch = EntityReferences.resolve(er.getName());
        if (ch == null) {
            write('&');
            write(er.getName());
            write(';');
        } else {
            write(ch);
        }
    }

    public void visit(Url url) {
        write(url.getProtocol());
        write(':');
        write(url.getPath());
    }

    public void visit(ExternalLink link) {
        //TODO How should we represent external links in the plain text output?
        write('[');
        iterate(link.getTitle());
        write(']');
    }

    public void visit(InternalLink link) {
        if (link.getTitle().getContent().size() > 0) {

            //HERE THERE ARE THE INTERNAL LINK WITH THE TARGET!
            //ex. [[anarchism|Anarchism]]
            try {
                String target = link.getTarget();
                PageTitle ptitle = PageTitle.make(config, target);
                if (ptitle.getNamespace().getName().length() == 0) {
                    addAnchor(new AnchorToken(StringConverterPartial.convert(link.getTitle().getContent())._1, target));

                }
            } catch (LinkTargetException ex) {
                Logger.getLogger(InternalLinkExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {

            //HERE THERE ARE THE INTERNAL LINK WITHOUT THE TARGET!
            //ex. [[anarchism]]
            try {
                String target = link.getTarget();
                PageTitle ptitle = PageTitle.make(config, target);
                if (ptitle.getNamespace().getName().length() == 0) {
                    addAnchor(new AnchorToken(target, target));
                }
            } catch (LinkTargetException ex) {
                Logger.getLogger(InternalLinkExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public void visit(Section s) {
        finishLine();
        iterate(s.getTitle());
        finishLine();
        newline(1);
        iterate(s.getBody());
    }

    public void visit(Paragraph p) {
        iterate(p.getContent());
        newline(1);
    }

    public void visit(HorizontalRule hr) {
        newline(1);
        //		write(StringUtils.strrep('-', wrapCol));
        //		newline(1);
    }

    public void visit(XmlElement e) {
        if (e.getName().equalsIgnoreCase("br")) {
            newline(1);
        } else {
            iterate(e.getBody());
        }
    }

    public void visit(Itemization n) {
        iterate(n.getContent());
    }

    public void visit(ItemizationItem n) {
        iterate(n.getContent());
        newline(1);
    }

    // =========================================================================
    // Stuff we want to hide
    public void visit(ImageLink n) {
    }

    public void visit(IllegalCodePoint n) {
    }

    public void visit(XmlComment n) {
    }

    public void visit(Template n) {
    }

    public void visit(TemplateArgument n) {
    }

    public void visit(TemplateParameter n) {
    }

    public void visit(TagExtension n) {
    }

    public void visit(MagicWord n) {
    }

    // =========================================================================
    private void addAnchor(AnchorToken anchor) {
        try {
            Analyzer a = new StandardAnalyzer();
            TokenStream tokenStream = a.tokenStream("wiki", sb.toString());
            tokenStream.reset();
            CharTermAttribute cattr = tokenStream.addAttribute(CharTermAttribute.class);
            while (tokenStream.incrementToken()) {
                String token = cattr.toString();
                tokens.add(new Token(token));
            }
            tokenStream.end();
            tokens.add(anchor);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            sb = new StringBuilder();
        }

    }

    private void newline(int num) {
        sb.append(StringUtils.strrep('\n', num));
    }

    private void finishLine() {
        sb.append("\n");
    }

    private void writeWord(String s) {
        write(s);
    }

    private void write(String s) {
        if (s.isEmpty()) {
            return;
        }
        sb.append(StringEscapeUtils.unescapeXml(s)).append(" ");
    }

    private void write(char[] cs) {
        write(String.valueOf(cs));
    }

    private void write(char ch) {
        writeWord(String.valueOf(ch));
    }

}
