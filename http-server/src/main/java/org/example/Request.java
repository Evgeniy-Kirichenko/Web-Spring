package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Request {
    private final String method;
    private final InputStream body;
    private final String path;
    private List<NameValuePair> query;
    private List<String> headers;

    public static final String GET = "GET";
    public static final String POST = "POST";


    public Request(String method, InputStream body, String path, List<NameValuePair> query, List<String> headers) {
        this.method = method;
        this.body = body;
        this.path = path;
        this.query = query;
        this.headers = headers;
    }

    public static Request fromInputStream(BufferedInputStream in, BufferedOutputStream out) throws IOException {

        final var allowedMethods = List.of(GET, POST);
        var reader = new BufferedReader(new InputStreamReader(in));//
        // лимит на request line + заголовки
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);// получаем количество считанных байт

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }
        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }
        final String path;
        var pathQuery = requestLine[1];
        if (!pathQuery.startsWith("/")) {
            return null;
        }
        List<NameValuePair> parse = new ArrayList<>();

        var pathQuerySplit = pathQuery.split("\\?");
        if (pathQuerySplit.length > 1) {
            path = pathQuerySplit[0];
            var queryString = pathQuerySplit[1];
            parse.addAll(URLEncodedUtils.parse(queryString, Charset.defaultCharset()));
        } else path = pathQuery;

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null;
        }
        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));//


        return new Request(method, in, path, parse, headers);
    }

    public String getMethod() {
        return method;
    }

    public InputStream getBody() {
        return body;
    }

    public String getPath() {
        return path;
    }

    public List<NameValuePair> getQuery() {
        return query;
    }

    public NameValuePair getQueryParams(String name) {
        for (int i = 0; i < query.size(); i++) {
            if (query.get(i).getName().equals(name)) return query.get(i);
        }
        return null;
    }


    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}
