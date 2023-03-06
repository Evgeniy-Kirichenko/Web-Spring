package org.example;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final ExecutorService executorService;
    private final Map<String, Map<String, Handler>> handlers;
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
            "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html",
            "/classic.html", "/events.html", "/events.js");

    public Server() {
        executorService = Executors.newFixedThreadPool(64);
        handlers = new ConcurrentHashMap<>();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                executorService.execute(() -> connection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

    private void connection(Socket socket) {
        try (socket;
             final var in = socket.getInputStream();
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var request = Request.fromInputStream(in);//обрабатываем строку запроса
            final String method = request.getMethod(); //выделяем Method
            if (method == null) {//если метода нет, то выводим сообщение об ошибке
                notFound.handle(request, out);
                return;//и выходим из соединения
            }

            final String path = request.getPath(); // выделяем имя файла
            if (path == null && !validPaths.contains(path)) { //если файла нет, то выводим сообщение об ошибке
                System.out.println("Зашли в  if (path == null && validPaths.contains(path)");
                notFound.handle(request, out);
                return; //и выходим из соединения
            }
            /*
            Проверяем, если в мапе обработчиков есть ключ, соответствующий method и по этому ключу
            есть ключ с именем файла, то тогда вызываем юто обработчик. Если нет, то обработчик по умолчанию
             */
            if (handlers.containsKey(method) && handlers.get(method).containsKey(path)) {
                handlers.get(method).get(path).handle(request, out);

            } else {
                defaultHandler.handle(request, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if (handlers.get(method) == null) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    private final Handler notFound = (request, out) -> {
        /*
        обрабочик при ошибках
         */
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };
    private final Handler defaultHandler = ((request, out) -> {
        /*
        обработчик по умолчанию
         */
        try {
            final var filePath = Path.of(".", "http-server/public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
}


