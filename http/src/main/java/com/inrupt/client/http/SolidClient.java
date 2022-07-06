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
package com.inrupt.client.http;

import com.inrupt.client.authentication.SolidAuthenticator;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * An HTTP client for interacting with Solid Resources.
 *
 * <p>This client extends the native Java client by introducing a {@link SolidAuthenticator} class
 * for performing Solid-conforming authentication.
 */
public class SolidClient extends HttpClient {

    private static final int UNAUTHORIZED = 401;

    private final HttpClient client;
    private final SolidAuthenticator solidAuthenticator;

    /**
     * Create a new SolidClient.
     *
     * @param client an HTTP client
     * @param authenticator the Solid authenticator
     */
    protected SolidClient(final HttpClient client, final SolidAuthenticator authenticator) {
        this.client = client;
        this.solidAuthenticator = authenticator;
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return client.authenticator();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return client.connectTimeout();
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return client.cookieHandler();
    }

    @Override
    public Optional<Executor> executor() {
        return client.executor();
    }

    @Override
    public HttpClient.Redirect followRedirects() {
        return client.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return client.proxy();
    }

    @Override
    public HttpClient.Version version() {
        return client.version();
    }

    @Override
    public SSLContext sslContext() {
        return client.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return client.sslParameters();
    }

    @Override
    public <T> HttpResponse<T> send(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        final var res = client.send(request, responseBodyHandler);
        if (res.statusCode() == UNAUTHORIZED) {
            // TODO -- make use of the authenticator

        }
        return res;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler) {
        return client.sendAsync(request, responseBodyHandler)
            .thenCompose(res -> {
                if (res.statusCode() == UNAUTHORIZED) {
                    // TODO -- make use of the authenticator
                }
                return CompletableFuture.completedFuture(res);
            });
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(final HttpRequest request,
            final HttpResponse.BodyHandler<T> responseBodyHandler,
            final HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        return client.sendAsync(request, responseBodyHandler, pushPromiseHandler)
            .thenCompose(res -> {
                if (res.statusCode() == UNAUTHORIZED) {
                    // TODO -- make use of the authenticator

                }
                return CompletableFuture.completedFuture(res);
            });
    }

    /**
     * A builder of Solid HTTP Clients.
     *
     * <p>Builders are created by invoking {@link Builder#newBuilder}. Each of the setter methods modifies the state
     * of the builder and returns the same instance. Builders are not thread-safe and should not be used concurrently
     * from multiple threads without external synchronization.
     */
    public static final class Builder {

        private HttpClient httpClient;
        private SolidAuthenticator solidAuthenticator;

        /**
         * Sets a Solid authenticator to use for HTTP authentication.
         *
         * @param authenticator the Solid authenticator
         * @return this builder instance
         */
        public Builder authenticator(final SolidAuthenticator authenticator) {
            this.solidAuthenticator = authenticator;
            return this;
        }

        /**
         * Sets a configured HTTP client to use for HTTP interactions.
         *
         * @param client the HTTP client
         * @return this builder instance
         */
        public Builder client(final HttpClient client) {
            this.httpClient = client;
            return this;
        }

        /**
         * Returns a new {@link SolidClient} built from the current state of this builder.
         *
         * @return a new Solid client
         */
        public SolidClient build() {
            if (httpClient == null) {
                httpClient = HttpClient.newBuilder().build();
            }
            return new SolidClient(httpClient, solidAuthenticator);
        }

        /**
         * Create a new {@link SolidClient.Builder} instance.
         *
         * @return a new Solid client builder
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        private Builder() {
            // Prevent instantiation
        }
    }
}
