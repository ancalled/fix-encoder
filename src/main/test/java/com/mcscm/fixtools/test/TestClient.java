package com.mcscm.fixtools.test;

import com.mcscm.fixtools.FIXMessage;
import com.mcscm.fixtools.MessageFactory;
import com.mcscm.fixtools.utils.CodeUtils;
import org.sample.*;
import org.sample.enums.ApplVerID;
import org.sample.enums.DefaultApplVerID;
import org.sample.enums.EncryptMethod;
import org.sample.enums.SecurityListRequestType;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

public class TestClient {


    public static final ByteBuffer SEND_BUFFER = ByteBuffer.allocateDirect(1024);
    public static final ByteBuffer RECEIVE_BUFFER = ByteBuffer.allocateDirect(10240);

    public static final Header SEND_HEADER = new Header();
    public static final Header REC_HEADER = new Header();

    private SocketChannel ch;
    private final String host;
    private final int port;
    public static final Trailer SEND_TRAILER = new Trailer();
    public static final Trailer REC_TRAILER = new Trailer();
    public static final MessageFactory FACTORY = new FIXMessageFactory();
    private int recBufPos = 0;
    private final List<FIXListener> listeners = new CopyOnWriteArrayList<>();

    public TestClient(Properties props) {
        host = props.getProperty("fix.host");
        port = Integer.parseInt(props.getProperty("fix.port"));
        String sender = props.getProperty("fix.sender");
        String target = props.getProperty("fix.target");


        SEND_HEADER.beginString = "FIXT.1.1";
        SEND_HEADER.applVerID = ApplVerID.FIX50;
        SEND_HEADER.msgSeqNum = 1;
        SEND_HEADER.senderCompID = sender;
        SEND_HEADER.targetCompID = target;
    }

    public void connect() throws IOException {
        ch = SocketChannel.open();
//        ch.configureBlocking(true);
        ch.configureBlocking(false);
        ch.connect(new InetSocketAddress(host, port));

        while (!ch.finishConnect()) {
            sleep(10);
        }

        System.out.println("Connected");
    }

    public void startAsyncListener() {
        Executors.newFixedThreadPool(1).submit(() -> {
            while (!Thread.interrupted()) {
                try {
                    int bytesRead = ch.read(RECEIVE_BUFFER);
                    if (bytesRead > 0) {
                        System.out.println("[receive] " + CodeUtils.toString(RECEIVE_BUFFER));
                        RECEIVE_BUFFER.rewind();
                        while (RECEIVE_BUFFER.position() < bytesRead) {
                            final FIXMessage mes = CodeUtils.decodeMessage(RECEIVE_BUFFER,
                                    RECEIVE_BUFFER.position(), bytesRead,
                                    REC_HEADER, REC_TRAILER, FACTORY);

//                            System.out.println("\tpos: " + RECEIVE_BUFFER.position());
                            for (FIXListener l : listeners) {
                                l.onMessage(mes);
                            }


                            sleep(500);

                        }
                        RECEIVE_BUFFER.rewind();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                sleep(100);
            }
        });
    }


    public boolean send(FIXMessage message) throws IOException {
        SEND_HEADER.sendingTime = new Date();

        int offset = 0;
        int length = CodeUtils.encodeMessage(SEND_BUFFER, offset, message, SEND_HEADER, SEND_TRAILER);

        System.out.println("[send] " + CodeUtils.toString(SEND_BUFFER));
        SEND_BUFFER.flip();
        while (SEND_BUFFER.hasRemaining()) {
            int sent = ch.write(SEND_BUFFER);
//            System.out.println("Sent " + sent + " bytes");
            SEND_HEADER.msgSeqNum++;
        }

        return true;
    }

    public FIXMessage poll() throws IOException {
        int bytesRead = ch.read(RECEIVE_BUFFER);
        if (bytesRead > 0) {
            final FIXMessage message = CodeUtils.decodeMessage(RECEIVE_BUFFER, 0, bytesRead, REC_HEADER, REC_TRAILER, FACTORY);
            RECEIVE_BUFFER.rewind();
            return message;
        }
        return null;
    }


    public void logon(String username, String pass) throws IOException {
        Logon logon = new Logon();
        logon.defaultApplVerID = DefaultApplVerID.FIX50;
        logon.username = username;
        logon.password = pass;
        logon.encryptMethod = EncryptMethod.NONE_OTHER;
        logon.heartBtInt = 10;
        logon.resetSeqNumFlag = true;
        send(logon);

        sleep(100);
        Logon response = (Logon) poll();
        System.out.println("Logon");
        System.out.println(response.printParsed("\t"));
        System.out.println("Authorized");
    }


    public interface FIXListener {
        void onMessage(FIXMessage mes);
    }

    public void addListener(FIXListener l) {
        listeners.add(l);
    }

    public static void sleep(long n) {
        try {
            Thread.sleep(n);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String propsFile = "./src/main/test/fix-client.properties";
        Properties props = new Properties();
        props.load(new FileReader(propsFile));
        TestClient client = new TestClient(props);
        client.connect();

        String username = "148b03";
        String pass = "123";
        client.logon(username, pass);

        client.addListener((m) ->
                System.out.println(
                        "<On new message>\n" + m.getClass().getSimpleName() +
                        "\n" + m.printParsed("\t")));

        client.startAsyncListener();

        SecurityListRequest req = new SecurityListRequest();
        req.securityReqID = Integer.toString(1);
        req.securityListRequestType = SecurityListRequestType.ALL_SECURITIES;
        client.send(req);

        sleep(100000);
    }


}
