package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Request {
    private final String method;
    private final InputStream body;
    private final String path;


    public Request(String method, InputStream body, String path) {
        this.method = method;
        this.body = body;
        this.path = path;
    }

    public static Request fromInputStream(InputStream in) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(in));//
        final var requestLine = reader.readLine();//читаем входящую строку
        final var parts = requestLine.split(" "); // парсим на три части 0 часть - метод, 1 - запрашиваемый файл

        if (parts.length != 3) {
            throw new IOException("Error!!!");
        }
        var method = parts[0];
        var path = parts[1];
        return new Request(method, in, path);
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
}
