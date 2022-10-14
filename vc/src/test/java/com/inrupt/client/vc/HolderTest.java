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
package com.inrupt.client.vc;

import static org.junit.jupiter.api.Assertions.*;

import com.inrupt.client.spi.VerifiableCredential;
import com.inrupt.client.spi.VerifiablePresentation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HolderTest {

    private static final VerifiableCredentialMockService vcMockService = new VerifiableCredentialMockService();
    private static final HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    private static final Map<String, String> config = new HashMap<>();
    private static Holder holder;

    @BeforeAll
    static void setup() {
        config.putAll(vcMockService.start());
        holder = new Holder(URI.create(config.get("vc_uri")), client);
    }

    @AfterAll
    static void teardown() {
        vcMockService.stop();
    }

    @Test
    void listCredentialsTest() {
        final var vcList = holder.listCredentials(List.of(
                    URI.create("VerifiableCredential"), URI.create("UniversityDegreeCredential")));

        assertEquals(2, vcList.size());
    }

    @Test
    void listCredentialsAsyncTest() {
        final var vcList = holder.listCredentialsAsync(List.of(
                    URI.create("VerifiableCredential"), URI.create("UniversityDegreeCredential")))
            .toCompletableFuture().join();

        assertEquals(2, vcList.size());
    }

    @Test
    void getCredentialTest() {
        final var vc = holder.getCredential("http://example.test/credentials/1872");

        assertEquals("http://example.test/credentials/1872", vc.id);
    }

    @Test
    void getCredentialAsyncTest() {
        final var vc = holder.getCredentialAsync("http://example.test/credentials/1872")
                .toCompletableFuture().join();

        assertEquals("http://example.test/credentials/1872", vc.id);
    }

    @Test
    void getCredentialStatusCodesTest() {
        assertAll("Bad request because of non existent credentialID",
            () -> {
                final CompletionException exception = assertThrows(CompletionException.class,
                    () -> holder.getCredential("http://example.test/credentials/0000")
                );
                assertTrue(exception.getCause() instanceof VerifiableCredentialException);
                final var cause = (VerifiableCredentialException) exception.getCause();
                assertEquals("Unexpected error response when handling a verifiable credential.", cause.getMessage());
                assertEquals(Optional.of(400), cause.getStatus());
            });
    }

    @Test
    void getDeleteCredentialTest() {
        holder.deleteCredential("http://example.test/credentials/1872");
    }

    @Test
    void getDeleteCredentialAsyncTest() {
        holder.deleteCredentialAsync("http://example.test/credentials/1872");
    }

    @Test
    void getDeriveTest() {

        final var derivationReq = new Holder.DerivationRequest();
        derivationReq.verifiableCredential = VCtestData.VC;
        derivationReq.frame = Collections.emptyMap();
        derivationReq.options = Map.of("nonce",
                "lEixQKDQvRecCifKl789TQj+Ii6YWDLSwn3AxR0VpPJ1QV5htod/0VCchVf1zVM0y2E=");

        final var vc = holder.derive(derivationReq);

        assertEquals("http://example.test/credentials/1872", vc.id);
    }

    @Test
    void deriveStatusCodesTest() {
        final var derivationReq = new Holder.DerivationRequest();
        derivationReq.verifiableCredential = new VerifiableCredential();
        derivationReq.frame = Collections.emptyMap();
        derivationReq.options = Collections.emptyMap();
        assertAll("Invalid request because of empty request",
            () -> {
                final CompletionException exception = assertThrows(CompletionException.class,
                    () -> holder.derive(derivationReq)
                );
                assertTrue(exception.getCause() instanceof VerifiableCredentialException);
                final var cause = (VerifiableCredentialException) exception.getCause();
                assertEquals("Unexpected error response when handling a verifiable credential.", cause.getMessage());
                assertEquals(Optional.of(400), cause.getStatus());
            });
    }

    @Test
    void getDeriveAsyncTest() {

        final var derivationReq = new Holder.DerivationRequest();
        derivationReq.verifiableCredential = VCtestData.VC;
        derivationReq.frame = Collections.emptyMap();
        derivationReq.options = Map.of("nonce",
                "lEixQKDQvRecCifKl789TQj+Ii6YWDLSwn3AxR0VpPJ1QV5htod/0VCchVf1zVM0y2E=");

        final var vc = holder.deriveAsync(derivationReq).toCompletableFuture().join();

        assertEquals("http://example.test/credentials/1872", vc.id);
    }


    //----- Presentation Tests -----

    @Test
    void listPresentationsTest() {
        final var vcList = holder.listPresentations(List.of(URI.create("VerifiablePresentation")));

        assertEquals(1, vcList.size());
    }

    @Test
    void listPresentationsAsyncTest() {
        final var vcList = holder.listPresentationsAsync(List.of(URI.create("VerifiablePresentation")))
            .toCompletableFuture().join();

        assertEquals(1, vcList.size());
    }

    @Test
    void getPresentationTest() {
        final var vp = holder.getPresentation("http://example.test/credentials/1872");

        assertEquals(VCtestData.VP.context, vp.context);
        assertEquals(VCtestData.VP.id, vp.id);
    }

    @Test
    void getPresentationAsyncTest() {
        final var vp = holder.getPresentationAsync("http://example.test/credentials/1872")
            .toCompletableFuture().join();

        assertEquals(VCtestData.VP.context, vp.context);
        assertEquals(VCtestData.VP.id, vp.id);
    }

    @Test
    void deletePresentationTest() {
        holder.deletePresentation("http://example.test/credentials/1872");
    }

    @Test
    void deletePresentationAsyncTest() {
        holder.deletePresentationAsync("http://example.test/credentials/1872");
    }

    @Test
    void proveTest() {

        final var derivationReq = new Holder.ProveRequest();
        derivationReq.presentation = VCtestData.VP;
        derivationReq.options = Map.of("nonce",
                "lEixQKDQvRecCifKl789TQj+Ii6YWDLSwn3AxR0VpPJ1QV5htod/0VCchVf1zVM0y2E=");

        final var vp = holder.prove(derivationReq);

        assertEquals(VCtestData.VP.context, vp.context);
        assertEquals(VCtestData.VP.id, vp.id);
    }

    @Test
    void proveAsyncTest() {

        final var proveReq = new Holder.ProveRequest();
        proveReq.presentation = VCtestData.VP;
        proveReq.options = Map.of("nonce",
                "lEixQKDQvRecCifKl789TQj+Ii6YWDLSwn3AxR0VpPJ1QV5htod/0VCchVf1zVM0y2E=");

        final var vp = holder.proveAsync(proveReq).toCompletableFuture().join();

        assertEquals(VCtestData.VP.context, vp.context);
        assertEquals(VCtestData.VP.id, vp.id);
    }

    @Test
    void proveAsyncStatusCodesTest() {
        final var proveReq = new Holder.ProveRequest();
        proveReq.presentation = new VerifiablePresentation();
        proveReq.options = Collections.emptyMap();

        assertAll("Invalid input becuase of empty request",
            () -> {
                final CompletionException exception = assertThrows(
                        CompletionException.class,
                        () -> holder.prove(proveReq)
                    );
                assertTrue(exception.getCause() instanceof VerifiableCredentialException);
                final var cause = (VerifiableCredentialException) exception.getCause();
                assertEquals("Unexpected error response when handling a verifiable presentation.", cause.getMessage());
                assertEquals(Optional.of(400), cause.getStatus());
            });
    }

    @Test
    void initiateExchangeTest() {

        final var exchangeReq = new Holder.ExchangeRequest();
        exchangeReq.query = new Holder.Query();
        exchangeReq.query.type = URI.create("QueryByExample");
        exchangeReq.query.credentialQuery = Map.of(
                "reason", "We need to see your existing University Degree credential.",
                "example", Map.of(
                    "@context", List.of(
                                "https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"),
                    "type", "UniversityDegreeCredential"));

        final var vpr = holder.initiateExchange("credential-refresh", exchangeReq);

        assertEquals("edu.example", vpr.domain);
        assertEquals("3182bdea-63d9-11ea-b6de-3b7c1404d57f", vpr.challenge);
    }

    @Test
    void initiateExchangeAsyncTest() {

        final var exchangeReq = new Holder.ExchangeRequest();
        exchangeReq.query = new Holder.Query();
        exchangeReq.query.type = URI.create("QueryByExample");
        exchangeReq.query.credentialQuery = Map.of("reason",
                "We need to see your existing University Degree credential.", "example",
                Map.of("@context",
                        List.of("https://www.w3.org/2018/credentials/v1",
                                "https://www.w3.org/2018/credentials/examples/v1"),
                        "type", "UniversityDegreeCredential"));

        final var vpr = holder.initiateExchangeAsync("credential-refresh", exchangeReq)
                .toCompletableFuture().join();

        assertEquals("edu.example", vpr.domain);
        assertEquals("3182bdea-63d9-11ea-b6de-3b7c1404d57f", vpr.challenge);
    }

    @Test
    void initiateExchangeAsyncStatusCodesTest() {
        final var exchangeReq = new Holder.ExchangeRequest();
        exchangeReq.query = new Holder.Query();
        exchangeReq.query.type = URI.create("QueryByExample");
        exchangeReq.query.credentialQuery = Collections.emptyMap();

        assertAll("Request is malformed because of empty credential query",
            () -> {
                final CompletionException exception = assertThrows(CompletionException.class,
                    () -> holder.initiateExchangeAsync("credential-refresh", exchangeReq)
                    .toCompletableFuture().join()
                );
                assertTrue(exception.getCause() instanceof VerifiableCredentialException);
                final var cause = (VerifiableCredentialException) exception.getCause();
                assertEquals("Unexpected error response when handling a verifiable presentation.", cause.getMessage());
                assertEquals(Optional.of(400), cause.getStatus());
            });
    }

    @Test
    void continueExchangeTest() {

        // TODO - implement
    }

    @Test
    void continueExchangeAsyncTest() {

        // TODO - implement
    }

}
