/*
 * Created by Bogdan Tirca on 4/19/17.
 * Copyright (c) 2017 Giphy Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.giphy.sdk.core.network.engine;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.giphy.sdk.core.models.json.BooleanDeserializer;
import com.giphy.sdk.core.models.json.DateDeserializer;
import com.giphy.sdk.core.models.json.DateSerializer;
import com.giphy.sdk.core.models.json.IntDeserializer;
import com.giphy.sdk.core.models.json.MainAdapterFactory;
import com.giphy.sdk.core.network.response.ErrorResponse;
import com.giphy.sdk.core.network.response.GenericResponse;
import com.giphy.sdk.core.threading.ApiTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Does the low level GET requests.
 */
public class DefaultNetworkSession implements NetworkSession {
    public static final Gson GSON_INSTANCE = new GsonBuilder().registerTypeHierarchyAdapter(Date.class, new DateDeserializer())
            .registerTypeHierarchyAdapter(Date.class, new DateSerializer())
            .registerTypeHierarchyAdapter(boolean.class, new BooleanDeserializer())
            .registerTypeHierarchyAdapter(int.class, new IntDeserializer())
            .registerTypeAdapterFactory(new MainAdapterFactory())
            .create();

    @Override
    public <T extends GenericResponse> ApiTask<T> queryStringConnection(@NonNull final Uri serverUrl, @NonNull final String path,
                                                                        @NonNull final String method, @NonNull final Class<T> responseClass, @Nullable final Map<String, String> queryStrings,
                                                                        @Nullable final Map<String, String> headers) {
        return new ApiTask<>(new Callable<T>() {
            @Override
            public T call() throws Exception {
                HttpURLConnection connection = null;
                URL url = null;
                try {
                    Uri.Builder uriBuilder = serverUrl.buildUpon().appendEncodedPath(path);

                    if (queryStrings != null) {
                        for (Map.Entry<String, String> query : queryStrings.entrySet()) {
                            uriBuilder.appendQueryParameter(query.getKey(), query.getValue());
                        }
                    }

                    url = new URL(uriBuilder.build().toString());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod(method);

                    if (headers != null) {
                        for (Map.Entry<String, String> header : headers.entrySet()) {
                            connection.setRequestProperty(header.getKey(), header.getValue());
                        }
                    }

                    connection.connect();

                    return readJsonResponse(url, connection, responseClass);
                } catch (Throwable t) {
                    Log.e(NetworkSession.class.getName(), "Unable to perform network request", t);
                    throw t;
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    private <T extends GenericResponse> T readJsonResponse(URL url, @NonNull HttpURLConnection connection, @NonNull Class<T> responseClass)
            throws IOException, ApiException {

        int responseCode = connection.getResponseCode();
        boolean succeeded = responseCode == HttpURLConnection.HTTP_OK
                || responseCode == HttpURLConnection.HTTP_CREATED
                || responseCode == HttpURLConnection.HTTP_ACCEPTED;
        BufferedReader inputReader;
        if (succeeded) {
            inputReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            inputReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }
        StringWriter stringWriter = new StringWriter();

        String line;
        while ((line = inputReader.readLine()) != null) {
            stringWriter.append(line);
        }
        String contents = stringWriter.toString();
        if (succeeded) {
            return GSON_INSTANCE.fromJson(contents, responseClass);
        } else {
            switch (responseCode) {
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    throw new ApiException("503 Exception : URL : " + url + ": Response Code :" + responseCode, new ErrorResponse(responseCode, null));
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    // Report if an invalid api key is used
                    Log.e(getClass().toString(), "Api key invalid!");
                default:
                    try {
                        throw new ApiException(GSON_INSTANCE.fromJson(contents, ErrorResponse.class));
                    } catch (JsonParseException e) {
                        throw new ApiException("Unable to parse server error response : " + url + " : " + contents + " : " + e.getMessage(),
                                new ErrorResponse(responseCode, contents));
                    }
            }
        }
    }
}
