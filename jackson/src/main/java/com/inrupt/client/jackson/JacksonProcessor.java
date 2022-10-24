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
package com.inrupt.client.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.inrupt.client.VerifiableCredential;
import com.inrupt.client.VerifiablePresentation;
import com.inrupt.client.spi.JsonProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

/**
 * A {@link JsonProcessor} using the Jackson JSON library.
 */
public class JacksonProcessor implements JsonProcessor {

    private final ObjectMapper mapper;

    /**
     * Create a Jackson processor.
     */
    public JacksonProcessor() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final SimpleModule vcDeserializer = new SimpleModule();
        vcDeserializer.addDeserializer(VerifiableCredential.class, new VCDeserializer());
        mapper.registerModule(vcDeserializer);
        final SimpleModule vcSerializer = new SimpleModule();
        vcSerializer.addSerializer(VerifiableCredential.class, new VCSerializer());
        mapper.registerModule(vcSerializer);

        final SimpleModule vpDeserializer = new SimpleModule();
        vpDeserializer.addDeserializer(VerifiablePresentation.class, new VPDeserializer());
        mapper.registerModule(vpDeserializer);
        final SimpleModule vpSerializer = new SimpleModule();
        vpSerializer.addSerializer(VerifiablePresentation.class, new VPSerializer());
        mapper.registerModule(vpSerializer);
    }

    @Override
    public <T> void toJson(final T object, final OutputStream output) throws IOException {
        mapper.writeValue(output, object);
    }

    @Override
    public <T> T fromJson(final InputStream input, final Class<T> clazz) throws IOException {
        return mapper.readValue(input, clazz);
    }

    @Override
    public <T> T fromJson(final InputStream input, final Type type) throws IOException {
        return mapper.readValue(input, mapper.getTypeFactory().constructType(type));
    }
}
