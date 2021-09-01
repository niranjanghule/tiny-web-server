package com.sng;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketServer {

    private final int port;
    private final SocketService service;
    private boolean isRunning = false;
    private ServerSocket socketServer;
    private final ExecutorService executor;

    public SocketServer( int port, SocketService service) throws IOException {
        this.port = port;
        this.service = service;
        socketServer = new ServerSocket(port);
        executor = Executors.newFixedThreadPool(4);
    }

    public int getPort() {
        return port;
    }

    public SocketService getService() {
        return service;
    }

    public void start() {
        try {
            Runnable connectionHandler = new Runnable() {
                @Override
                public void run() {
                    try {
                        while(isRunning) {
                            Socket socket = socketServer.accept();
                            executor.execute(() -> service.serve(socket));
                        }
                    } catch (IOException e) {
                        if(isRunning)
                            e.printStackTrace();
                    }
                }
            };

            executor.execute(connectionHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void stop() throws Exception {
        socketServer.close();
        executor.shutdown();
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

        isRunning = false;
    }
}
