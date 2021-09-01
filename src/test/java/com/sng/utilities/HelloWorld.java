package com.sng.utilities;

import com.sng.SocketServer;
import com.sng.SocketService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HelloWorld implements SocketService {

    public static void main(String[] args) throws IOException {
        SocketServer server = new SocketServer(8080, new HelloWorld());
        server.start();
    }

    @Override
    public void serve(Socket socket) {
        try {

            String response = "HTTP/1.1 200 OK\n" +
                    "Content-length : 21\n"+
                    "\n"+
                    "<h1>Hello World!</h1>";

            OutputStream os = socket.getOutputStream();
            os.write(response.getBytes());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
