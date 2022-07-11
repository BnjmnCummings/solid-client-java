/*
 * Copyright 2022 Inrupt Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.inrupt.client.webid;

import com.inrupt.client.rdf.*;
import com.inrupt.client.spi.RdfProcessor;
import com.inrupt.client.spi.ServiceLoadingException;
import com.inrupt.client.vocabulary.PIM;
import com.inrupt.client.vocabulary.RDF;
import com.inrupt.client.vocabulary.RDFS;
import com.inrupt.client.vocabulary.Solid;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ServiceLoader;

/**
 * Classes for reading HTTP responses as WebID Profile objects.
 */
public final class WebIdBodySubscribers {

    private static final RdfProcessor processor = loadRdfProcessor();

    /**
     * Process an HTTP response as a WebID Profile.
     *
     * <p>This method expects to read a TURTLE serialization of an HTTP response.
     *
     * @param webid the WebID URI
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<WebIdProfile> ofWebIdProfile(final URI webid) {
        final var upstream = HttpResponse.BodySubscribers.ofInputStream();
        return HttpResponse.BodySubscribers.mapping(upstream, (InputStream input) -> {

            final var graph = processor.toGraph(Syntax.TURTLE, input);

            final var builder = WebIdProfile.newBuilder();
            graph.stream(RDFNode.namedNode(webid), RDFNode.namedNode(Solid.oidcIssuer), null)
                .map(Triple::getObject)
                .filter(RDFNode::isNamedNode)
                .map(RDFNode::getURI)
                .forEach(builder::oidcIssuer);

            graph.stream(RDFNode.namedNode(webid), RDFNode.namedNode(RDFS.seeAlso), null)
                .map(Triple::getObject)
                .filter(RDFNode::isNamedNode)
                .map(RDFNode::getURI)
                .forEach(builder::seeAlso);

            graph.stream(RDFNode.namedNode(webid), RDFNode.namedNode(PIM.storage), null)
                .map(Triple::getObject)
                .filter(RDFNode::isNamedNode)
                .map(RDFNode::getURI)
                .forEach(builder::storage);

            graph.stream(RDFNode.namedNode(webid), RDFNode.namedNode(RDF.type), null)
                .map(Triple::getObject)
                .filter(RDFNode::isNamedNode)
                .map(RDFNode::getURI)
                .forEach(builder::type);

            return builder.build(webid);
        });
    }

    static RdfProcessor loadRdfProcessor() {
        return ServiceLoader.load(RdfProcessor.class).findFirst()
            .orElseThrow(() -> new ServiceLoadingException(
                        "Unable to load RDF processor. " +
                        "Please ensure that an RDF processor module is available on the classpath"));
    }

    private WebIdBodySubscribers() {
        // Prevent instantiation
    }
}
