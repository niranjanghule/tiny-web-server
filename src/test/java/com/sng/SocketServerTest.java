package com.sng;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Unit test for socket server
 */
@RunWith(HierarchicalContextRunner.class)
public class SocketServerTest {

    private ClosingSocketService service;
    private int port;
    private SocketServer server;

    public abstract static class TestSocketService implements SocketService {
        @Override
        public void serve(Socket s) {
            try {
                doService(s);
                synchronized (this) {
                    notify();
                };
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        protected abstract void doService(Socket s) throws IOException;
    }

    @Before
    public void setup(){
        port = 8082;
    }

    public static class ClosingSocketService extends TestSocketService {
        public int connections;

        @Override
        public void serve(Socket s) {
            try {
                doService(s);
                synchronized(this){ notify(); }
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        protected void doService(Socket S) {
            connections++;
        }
    }

    public class WithClosingSocketService {

        @Before
        public void setup() throws IOException {
            service = new ClosingSocketService();
            server = new SocketServer(port, service);
        }

        @After
        public void teardown() throws Exception {
            server.stop();
        }

        @Test
        public void shouldInstaniate() {
            assertEquals(port, server.getPort());
            assertEquals(service, server.getService());
        }

        @Test
        public void canStopAndStartServer() throws Exception {
            server.start();
            assertTrue(server.isRunning());
            server.stop();
            assertFalse(server.isRunning());
        }

        @Test
        public void acceptAnIncomingConnection() throws Exception {
            server.start();
            new Socket("localhost", port);
            synchronized (service) {
                service.wait();
            }
            server.stop();
            assertEquals(1, service.connections);

        }

        @Test
        public void acceptMultipleIncomingConnections() throws Exception {
            server.start();
            new Socket("localhost", port);
            synchronized (service) {
                service.wait();
            }
            new Socket("localhost", port);
            synchronized (service) {
                service.wait();
            }
            server.stop();
            assertEquals(2, service.connections);
        }
    }

    public static class ReadingSocketService extends TestSocketService {
        public String message;

        @Override
        protected void doService(Socket s) throws IOException {
            InputStream is = s.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            message = br.readLine();
        }
    }

    public class WithReadingSocketService {

        private ReadingSocketService readingService;

        @Before
        public void setup(){
            readingService = new ReadingSocketService();
        }

        @After
        public void teardown() throws Exception {
            server.stop();
        }

        @Test
        public void canWriteAndReceiveData() throws Exception {
            server = new SocketServer(port, readingService);
            server.start();
            Socket socket = new Socket("localhost", port);
            OutputStream os = socket.getOutputStream();
            os.write("Hello\n".getBytes());
            synchronized (readingService) {
                readingService.wait();
            };
            server.stop();
            assertEquals("Hello", readingService.message);
        }
    }



    public static class EchoSocketService extends TestSocketService {

        @Override
        protected void doService(Socket s) throws IOException {
            InputStream is = s.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String response = br.readLine();
            OutputStream os = s.getOutputStream();
            os.write(response.getBytes());
            os.flush();
        }
    }

    public class WithEchoSocketService {

        private EchoSocketService echoService;

        @Before
        public void setup(){
            echoService = new EchoSocketService();
        }

        @After
        public void teardown() throws Exception {
            server.stop();
        }

        @Test
        public void canEchoData() throws Exception {
            server = new SocketServer(port, echoService);
            server.start();
            Socket s = new Socket("localhost", port);
            OutputStream os = s.getOutputStream();
            os.write("Hello\n".getBytes());
            synchronized (echoService) {
                echoService.wait();
            };
            InputStream is = s.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String response = br.readLine();
            server.stop();
            assertEquals("Hello",response);
        }
    }


}
