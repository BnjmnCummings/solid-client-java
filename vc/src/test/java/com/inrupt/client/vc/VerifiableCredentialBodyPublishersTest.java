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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VerifiableCredentialBodyPublishersTest {

    private static final VerifiableCredentialMockService vcMockService = new VerifiableCredentialMockService();
    private static final Map<String, String> config = new HashMap<>();
    private static final HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();

    @BeforeAll
    static void setup() {
        config.putAll(vcMockService.start());
    }

    @AfterAll
    static void teardown() {
        vcMockService.stop();
    }

    @Test
    void ofVerifiableCredentialPublisherTest() throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder()
            .uri(URI.create(config.get("vc_uri") + "/postVc"))
            .header("Content-Type", "application/json")
            .POST(VerifiableCredentialBodyPublishers.ofVerifiableCredential(VCtestData.VC))
            .build();

        final var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(201, response.statusCode());
    }

    @Test
    void ofVerifiablePresentationPublisherTest() throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder()
            .uri(URI.create(config.get("vc_uri") + "/postVp"))
            .header("Content-Type", "application/json")
            .POST(VerifiableCredentialBodyPublishers.ofVerifiablePresentation(VCtestData.VP))
            .build();

        final var response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertEquals(201, response.statusCode());
    }

}