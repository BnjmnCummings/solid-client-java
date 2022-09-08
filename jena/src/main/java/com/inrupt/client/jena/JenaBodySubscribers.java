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
package com.inrupt.client.jena;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * {@link HttpResponse.BodySubscriber} implementations for use with Jena types.
 */
public final class JenaBodySubscribers {

    /**
     * Process an HTTP response as a Jena {@link Model}.
     *
     * <p>This method expects the default (TURTLE) serialization of an HTTP response.
     *
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<Supplier<Model>> ofModel() {
        return ofModel(Lang.TURTLE);
    }

    /**
     * Process an HTTP response as a Jena {@link Model}.
     *
     * @param lang the RDF serialization of the HTTP response
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<Supplier<Model>> ofModel(final Lang lang) {
        final var upstream = HttpResponse.BodySubscribers.ofInputStream();
        final HttpResponse.BodySubscriber<Supplier<Model>> downstream = HttpResponse.BodySubscribers.mapping(
            upstream,
            (InputStream is) -> () -> {
                try (var stream = is) {
                    final var model = ModelFactory.createDefaultModel();
                    RDFDataMgr.read(model, stream, lang);
                    return model;
                } catch (IOException ex) {
                    throw new UncheckedIOException(
                        "An I/O error occurred while data was read from the InputStream into a Model",
                        ex
                    );
                }
            }
            );
        return downstream;
    }

    /**
     * Process an HTTP response as a Jena {@link Graph}.
     *
     * <p>This method expects the default (TURTLE) serialization of an HTTP response.
     *
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<Supplier<Graph>> ofGraph() {
        return ofGraph(Lang.TURTLE);
    }

    /**
     * Process an HTTP response as a Jena {@link Graph}.
     *
     * @param lang the RDF serialization of the HTTP response
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<Supplier<Graph>> ofGraph(final Lang lang) {
        final var upstream = HttpResponse.BodySubscribers.ofInputStream();
        final HttpResponse.BodySubscriber<Supplier<Graph>> downstream = HttpResponse.BodySubscribers.mapping(
            upstream,
            (InputStream is) -> () -> {
                try (var stream = is) {
                    final var graph = Factory.createDefaultGraph();
                    RDFDataMgr.read(graph, stream, lang);
                    return graph;
                } catch (IOException ex) {
                    throw new UncheckedIOException(
                        "An I/O error occurred while data was read from the InputStream into a Graph",
                        ex
                    );
                }
            }
        );
        return downstream;
    }

    /**
     * Process an HTTP response as a Jena {@link Dataset}.
     *
     * <p>This method expects the default (TRIG) serialization of an HTTP response.
     *
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<Supplier<Dataset>> ofDataset() {
        return ofDataset(Lang.TRIG);
    }

    /**
     * Process an HTTP response as a Jena {@link Dataset}.
     *
     * @param lang the RDF serialization of the HTTP response
     * @return the body subscriber
     */
    public static HttpResponse.BodySubscriber<Supplier<Dataset>> ofDataset(final Lang lang) {
        final var upstream = HttpResponse.BodySubscribers.ofInputStream();
        final HttpResponse.BodySubscriber<Supplier<Dataset>> downstream = HttpResponse.BodySubscribers.mapping(
            upstream,
            (InputStream is) -> () -> {
                try (var stream = is) {
                    final var dataset = DatasetFactory.create();
                    RDFDataMgr.read(dataset, stream, lang);
                    return dataset;
                } catch (IOException ex) {
                    throw new UncheckedIOException(
                        "An I/O error occurred while data was read from the InputStream into a Dataset",
                        ex
                    );
                }
            });
        return downstream;
    }

    private JenaBodySubscribers() {
        // Prevent instantiation
    }
}
