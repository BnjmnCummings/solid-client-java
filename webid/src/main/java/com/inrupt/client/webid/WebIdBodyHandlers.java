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

import java.net.URI;
import java.net.http.HttpResponse;

/**
 * Body handlers for WebID Profiles.
 */
public final class WebIdBodyHandlers {

    /**
     * Transform an HTTP response into a WebID Profile object.
     *
     * @param webid the WebID URI
     * @return an HTTP body handler
     */
    public static HttpResponse.BodyHandler<WebIdProfile> ofWebIdProfile(final URI webid) {
        return new HttpResponse.BodyHandler<WebIdProfile>() {
            @Override
            public HttpResponse.BodySubscriber<WebIdProfile> apply(final HttpResponse.ResponseInfo responseInfo) {
                return WebIdBodySubscribers.ofWebIdProfile(webid);
            }
        };
    }

    private WebIdBodyHandlers() {
        // Prevent instantiation
    }
}