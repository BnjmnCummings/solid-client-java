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
package com.inrupt.client.uma;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.inrupt.client.api.*;
import com.inrupt.client.spi.HttpProcessor;
import com.inrupt.client.spi.JsonProcessor;
import com.inrupt.client.spi.ServiceProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UmaClient {

    /* HTTP */
    private static final String EQUALS = "=";
    private static final String ETC = "&";
    private static final String ACCEPT = "Accept";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String JSON = "application/json";
    private static final String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final int SUCCESS = 200;

    /* UMA parameter names */
    private static final String CLAIM_TOKEN = "claim_token";
    private static final String CLAIM_TOKEN_FORMAT = "claim_token_format";
    private static final String GRANT_TYPE = "grant_type";
    private static final String PCT = "pct";
    private static final String RPT = "rpt";
    private static final String SCOPE = "scope";
    private static final String TICKET = "ticket";

    /* UMA parameter values */
    private static final String UMA_TICKET = "urn:ietf:params:oauth:grant-type:uma-ticket";

    /* UMA Error Codes */
    private static final String INVALID_GRANT = "invalid_grant";
    private static final String INVALID_SCOPE = "invalid_scope";
    private static final String NEED_INFO = "need_info";
    private static final String REQUEST_DENIED = "request_denied";

    // TODO add metadata cache
    private final HttpProcessor httpClient;
    private final JsonProcessor processor;
    private final int maxIterations;

    /**
     * Create a new UMA client using a default-configured HTTP client.
     */
    public UmaClient() {
        this(5);
    }

    public UmaClient(final int maxIterations) {
        this(ServiceProvider.getHttpProcessor(), maxIterations);
    }

    /**
     * Create an UMA client using an externally-configured HTTP client.
     *
     * @param httpClient the externally configured HTTP client
     * @param maxIterations the maximum number of claims gathering stages
     */
    public UmaClient(final HttpProcessor httpClient, final int maxIterations) {
        this.httpClient = httpClient;
        this.maxIterations = maxIterations;
        this.processor = ServiceProvider.getJsonProcessor();
    }

    /**
     * Fetch the UMA metadata resource.
     *
     * @param authorizationServer the authorization server URI
     * @return the authorization server discovery metadata
     */
    public Metadata metadata(final URI authorizationServer) {
        try {
            return metadataAsync(authorizationServer).toCompletableFuture().join();
        } catch (final CompletionException ex) {
            if (ex.getCause() instanceof UmaException) {
                sneakyThrow(ex.getCause());
            }
            throw new UmaException("Error performing UMA metadata discovery", ex);
        }
    }

    /**
     * Fetch the UMA metadata resource.
     *
     * @param authorizationServer the authorization server URI
     * @return the next stage of completion, containing the authorization server discovery metadata
     */
    public CompletionStage<Metadata> metadataAsync(final URI authorizationServer) {
        final Request req = Request.newBuilder(getMetadataUrl(authorizationServer)).header(ACCEPT, JSON).build();
        return httpClient.sendAsync(req, Response.BodyHandlers.ofInputStream())
            .thenApply(this::processMetadataResponse);
    }

    /**
     * Fetch the UMA token resource.
     *
     * @param tokenEndpoint the token endpoint
     * @param tokenRequest the token request data
     * @param claimMapper a mapping function for interactive claim gathering
     * @return the token response
     */
    public TokenResponse token(final URI tokenEndpoint, final TokenRequest tokenRequest,
            final Function<NeedInfo, ClaimToken> claimMapper) {
        final Function<NeedInfo, ClaimToken> mapper = Objects.requireNonNull(claimMapper);
        try {
            return negotiateToken(Objects.requireNonNull(tokenEndpoint),
                    Objects.requireNonNull(tokenRequest),
                    needInfo -> CompletableFuture.completedFuture(mapper.apply(needInfo)), 1)
                .toCompletableFuture().get();
        } catch (final InterruptedException ex) {
            throw new UmaException("Error processing UMA token negotiation", ex);
        } catch (final ExecutionException ex) {
            // Handle any execution exceptions as runtime errors
            sneakyThrow(ex.getCause());
        }
        throw new UmaException("Unable to negotiate UMA token");
    }

    /**
     * Fetch the UMA token resource.
     *
     * @param tokenEndpoint the token endpoint
     * @param tokenRequest the token request data
     * @param claimMapper a mapping function for interactive claim gathering
     * @return the next stage of completion, containing the token response
     */
    public CompletionStage<TokenResponse> tokenAsync(final URI tokenEndpoint, final TokenRequest tokenRequest,
            final Function<NeedInfo, CompletionStage<ClaimToken>> claimMapper) {
        return negotiateToken(Objects.requireNonNull(tokenEndpoint),
                Objects.requireNonNull(tokenRequest), Objects.requireNonNull(claimMapper), 1);
    }

    private CompletionStage<TokenResponse> negotiateToken(final URI tokenEndpoint, final TokenRequest tokenRequest,
            final Function<NeedInfo, CompletionStage<ClaimToken>> claimMapper, final int count) {

        if (count > maxIterations) {
            throw new UmaException("Claim gathering stages exceeded configured maximum of " + maxIterations);
        }

        final Request req = buildTokenRequest(tokenEndpoint, tokenRequest);
        return httpClient.sendAsync(req, Response.BodyHandlers.ofInputStream())
            .thenCompose(res -> {
                try {
                    // Successful terminal state
                    if (SUCCESS == res.statusCode()) {
                        return CompletableFuture.completedFuture(processor.fromJson(res.body(), TokenResponse.class));
                    }

                    // Everything else is a 4xx response
                    // Attempt to read the error response as JSON
                    final ErrorResponse err = processor.fromJson(res.body(), ErrorResponse.class);

                    if (err.error != null) {
                        switch (err.error) {
                            case NEED_INFO:
                                // recursive claims gathering
                                return NeedInfo.ofErrorResponse(err)
                                    .map(needInfo -> claimMapper.apply(needInfo).thenApply(claimToken -> {
                                        if (claimToken == null) {
                                            throw new RequestDeniedException(
                                                    "The client is unable to negotiate an access token");
                                        }
                                        return new TokenRequest(needInfo.getTicket(), null, null, claimToken,
                                                    tokenRequest.getScopes());
                                    }))
                                    .orElseThrow(() -> new RequestDeniedException("Invalid need_info error response"))
                                    .thenCompose(modifiedTokenRequest ->
                                        negotiateToken(tokenEndpoint, modifiedTokenRequest, claimMapper, count + 1));

                            case REQUEST_DENIED:
                                throw new RequestDeniedException(
                                        "The client is not authorized for the requested permissions");

                            case INVALID_GRANT:
                                throw new InvalidGrantException("Invalid grant provided");

                            case INVALID_SCOPE:
                                throw new InvalidScopeException("Invalid scope provided");
                        }
                    }

                    throw new UmaException(
                            "Unexpected error response while performing token negotiation: " + res.statusCode());

                } catch (final IOException ex) {
                    throw new UmaException("Unexpected I/O Error while performing token negotiation", ex);
                }
            });
    }

    static <T extends Throwable> void sneakyThrow(final Throwable ex) throws T {
        throw (T) ex;
    }

    private Request buildTokenRequest(final URI tokenEndpoint, final TokenRequest request) {
        final Map<String, String> data = new HashMap<>();
        data.put(GRANT_TYPE, UMA_TICKET);
        data.put(TICKET, request.getTicket());
        request.getPersistedClaimToken().ifPresent(pct -> data.put(PCT, pct));
        request.getRequestingPartyToken().ifPresent(rpt -> data.put(RPT, rpt));
        request.getClaimToken().ifPresent(claimToken -> {
            data.put(CLAIM_TOKEN, claimToken.getClaimToken());
            data.put(CLAIM_TOKEN_FORMAT, claimToken.getClaimTokenType());
        });
        if (!request.getScopes().isEmpty()) {
            data.put(SCOPE, String.join(" ", request.getScopes()));
        }

        // TODO add dpop support, if available
        return Request.newBuilder(tokenEndpoint)
            .header(CONTENT_TYPE, X_WWW_FORM_URLENCODED)
            .POST(ofFormData(data))
            .build();
    }

    private static Request.BodyPublisher ofFormData(final Map<String, String> data) {
        final String form = data.entrySet().stream().map(entry -> {
            final String name = URLEncoder.encode(entry.getKey(), UTF_8);
            final String value = URLEncoder.encode(entry.getValue(), UTF_8);
            return String.join(EQUALS, name, value);
        }).collect(Collectors.joining(ETC));

        return Request.BodyPublishers.ofString(form);
    }


    private Metadata processMetadataResponse(final Response<InputStream> response) {
        if (response.statusCode() == SUCCESS) {
            try {
                return processor.fromJson(response.body(), Metadata.class);
            } catch (final IOException ex) {
                throw new UmaException("Error while processing UMA metadata response", ex);
            }
        }
        throw new UmaException("Unexpected response code during UMA discovery: " + response.statusCode());
    }

    private URI getMetadataUrl(final URI authorizationServer) {
        return URIBuilder.newBuilder(authorizationServer).path(".well-known/uma2-configuration").build();
    }
}
