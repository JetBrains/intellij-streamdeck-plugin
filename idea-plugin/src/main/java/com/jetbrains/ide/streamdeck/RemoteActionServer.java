package com.jetbrains.ide.streamdeck;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.ide.streamdeck.settings.ActionServerSettings;
import com.jetbrains.ide.streamdeck.util.ActionExecutor;
import com.jetbrains.ide.streamdeck.util.LocalHostUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * An action server created to serve remote execute actions.
 */
 @Service(Service.Level.APP)
public final class RemoteActionServer implements AutoCloseable {
    private static SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS ");

    public static RemoteActionServer getInstance() {
        return ApplicationManager.getApplication().getService(RemoteActionServer.class);
    }
    private HttpServer server;

    private boolean started = false;

    private static StringBuffer serverLog = new StringBuffer();

    public void start() throws IOException {
        ActionServerSettings myActionServerSettings = ActionServerSettings.getInstance();
        int port = myActionServerSettings.getDefaultPort();
        if(port <= 0) {
            port = 21420;
        }

        server = HttpServer.create(new InetSocketAddress(port), 5);
        server.createContext("/", new HandleHttpRequest());

        // Start the ActionServer on a separate thread from the rest of the framework
        // so that it can respond while debugging the framework itself.
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        started = true;

        log("Stream Deck Remote Action Server listen on host " + String.join(",", LocalHostUtil.getLocalIPs()) + " port:" + server.getAddress().getPort());
        System.out.println("RemoteActionServer listening on port " + server.getAddress().getPort());
    }

    private static void log(String msg) {
        serverLog.append(defaultDateFormat.format(Calendar.getInstance().getTime())).append(msg).append("\n");
        ActionServerListener.fireServerStatusChanged();
    }

    public String getOrigin() {
        InetSocketAddress address = server.getAddress();
        return String.format("http://%s:%d", address.getHostName(), address.getPort());
    }

    public String getServerLog() {
        return serverLog.toString();
    }

    public boolean isStarted() {
        return started;
    }

    public void clearLog() {
        serverLog.setLength(0);
    }

    @Override
    public void close() {
        server.stop(0);
        started = false;
        log("Stopped Stream Deck Remote Action Server");
    }

    static class HandleHttpRequest implements HttpHandler {

        public HandleHttpRequest() {
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if(!ActionServerSettings.getInstance().getEnable()) return; // || !ActionServerSettings.getInstance().getEnableRemote()

            log("RemoteActionServer -> " + exchange.getRequestURI().toString());
            String password = ActionServerSettings.getInstance().getPassword();

            if(StringUtil.isNotEmpty(password)) {
                Headers headers = exchange.getRequestHeaders();
                String passwordHeader = headers.getFirst("Authorization");
                if(!Objects.equals(password, passwordHeader)) {
                    respondWithString(exchange, "Bad password provided", "text/plain", 500);
                    return;
                }
            }

            // http://localhost:21420/api/action/Run
            ActionExecutor.performActionUrl(exchange.getRequestURI().toString(),
                    !ActionServerSettings.getInstance().getFocusOnly());
            respondWithJson(exchange, "<status>ok</status>");
        }

        void responseError(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String response = "404 - file not found from action server<br>" + "Your are visiting: " + path + "<br> JetBrains Stream Deck Http Server";
            respondWithString(exchange, response, "text/plain", 404);
        }

        /**
         * Produces a simple, string-based response to a request.
         */
        private void respondWithString(HttpExchange exchange, String response, String contentType, int httpCode) throws IOException {
            Charset charset = StandardCharsets.UTF_8;
            byte[] bytes = response.getBytes(charset);
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            exchange.sendResponseHeaders(httpCode, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private void respondWithJson(HttpExchange exchange, String response) throws IOException {
            respondWithString(exchange, response, "application/json", 200);
        }
    }
}
