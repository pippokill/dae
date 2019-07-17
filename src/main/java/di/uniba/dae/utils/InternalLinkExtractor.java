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

import de.fau.cs.osr.ptk.common.AstVisitor;
import de.fau.cs.osr.ptk.common.ast.AstNode;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.LinkTargetException;
import org.sweble.wikitext.lazy.parser.InternalLink;
import org.sweble.wikitext.lazy.utils.StringConverterPartial;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author pierpaolo
 */
public class InternalLinkExtractor extends AstVisitor {

    private final SimpleWikiConfiguration config;

    private List<Anchor> anchors;

    private static final Logger LOG = Logger.getLogger(InternalLinkExtractor.class.getName());

    /**
     *
     */
    public InternalLinkExtractor() {
        SimpleWikiConfiguration config = null;
        try {
            config = new SimpleWikiConfiguration(WikiConstants.SWEBLE_CONFIG);
        } catch (IOException | JAXBException e) {
            LOG.log(Level.SEVERE, null, e);
        }
        this.config = config;
    }

    /**
     * Creates a new visitor that extracts anchors of internal links from a
     * parsed Wikipedia article.
     *
     * @param config the Sweble configuration
     */
    public InternalLinkExtractor(SimpleWikiConfiguration config) {
        this.config = config;
    }

    /**
     *
     * @param node
     * @return
     */
    @Override
    protected boolean before(AstNode node) {
        // This method is called by go() before visitation starts
        anchors = new LinkedList<>();
        return super.before(node);
    }

    /**
     *
     * @param node
     * @param result
     * @return
     */
    @Override
    protected Object after(AstNode node, Object result) {
        return anchors;
    }

    // =========================================================================

    /**
     *
     * @param n
     */
    public void visit(AstNode n) {
        iterate(n);
    }

    /**
     *
     * @param inLink
     * @throws IOException
     */
    public void visit(InternalLink inLink) throws IOException {
        if (inLink.getTitle().getContent().size() > 0) {

            //HERE THERE ARE THE INTERNAL LINK WITH THE TARGET!
            //ex. [[anarchism|Anarchism]]
            try {
                String target = inLink.getTarget();
                PageTitle ptitle = PageTitle.make(config, target);
                if (ptitle.getNamespace().getName().length() == 0) {
                    anchors.add(new Anchor(StringConverterPartial.convert(inLink.getTitle().getContent())._1, target));

                }
            } catch (LinkTargetException ex) {
                Logger.getLogger(InternalLinkExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {

            //HERE THERE ARE THE INTERNAL LINK WITHOUT THE TARGET!
            //ex. [[anarchism]]
            try {
                String target = inLink.getTarget();
                PageTitle ptitle = PageTitle.make(config, target);
                if (ptitle.getNamespace().getName().length() == 0) {
                    anchors.add(new Anchor(target, target));
                }
            } catch (LinkTargetException ex) {
                Logger.getLogger(InternalLinkExtractor.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
