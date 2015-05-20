package com.mcscm.fixtools.test;

import com.mcscm.fixtools.FIXMessage;
import com.mcscm.fixtools.MessageFactory;
import com.mcscm.fixtools.utils.CodeUtils;
import org.sample.FIXMessageFactory;
import org.sample.Header;
import org.sample.Logon;
import org.sample.Trailer;
import org.sample.enums.ApplVerID;
import org.sample.enums.DefaultApplVerID;
import org.sample.enums.EncryptMethod;
import sun.rmi.runtime.Log;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Properties;

public class TestClient {


    public static final ByteBuffer SEND_BUFFER = ByteBuffer.allocateDirect(1024);
    public static final ByteBuffer RECEIVE_BUFFER = ByteBuffer.allocateDirect(2048);
    public static final Header SEND_HEADER = new Header();
    public static final Header REC_HEADER = new Header();

    private SocketChannel ch;
    private final String host;
    private final int port;
    public static final Trailer SEND_TRAILER = new Trailer();
    public static final Trailer REC_TRAILER = new Trailer();
    public static final MessageFactory FACTORY = new FIXMessageFactory();

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
        ch.configureBlocking(true);
//        ch.configureBlocking(false);
        ch.connect(new InetSocketAddress(host, port));
    }

    public boolean send(FIXMessage message) throws IOException {
        SEND_HEADER.sendingTime = new Date();

        int offset = 0;
        int length = CodeUtils.encodeMessage(SEND_BUFFER, offset, message, SEND_HEADER, SEND_TRAILER);

        System.out.println(CodeUtils.toString(SEND_BUFFER));
        SEND_BUFFER.flip();
        while (SEND_BUFFER.hasRemaining()) {
            int sent = ch.write(SEND_BUFFER);
            System.out.println("Sent " + sent + " bytes");
        }

        return true;
    }

    public FIXMessage poll() throws IOException {
        int bytesRead = ch.read(RECEIVE_BUFFER);
        System.out.println("bytesRead = " + bytesRead);
        if (bytesRead > 0) {
            return CodeUtils.decodeMessage(RECEIVE_BUFFER, 0, REC_HEADER, REC_TRAILER, FACTORY);
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

        Logon response = (Logon) poll();
        System.out.println(response.heartBtInt);
    }


    public static void main(String[] args) throws IOException {
        System.out.println(System.getProperty("user.dir"));
        String propsFile = "./src/main/test/fix-client.properties";
        Properties props = new Properties();
        props.load(new FileReader(propsFile));
        TestClient client = new TestClient(props);
        client.connect();

        String username = "usr123";
        String pass = "qwerty";
        client.logon(username, pass);

    }



//    public static void main1(String[] args) throws IOException {
//        String host = "localhost";
//        int port = 9885;
//        String sender = "fix-client";
//        String target = "fix-server";
//        String username = "usr123";
//        String pass = "qwerty";
////        String pass = "qwerty1";
//
//
//        SEND_HEADER.sendingTime = new Date();
//
//        SocketChannel ch = SocketChannel.open();
//
//
//        Logon logon = new Logon();
//        logon.defaultApplVerID = DefaultApplVerID.FIX50;
//        logon.username = username;
//        logon.password = pass;
//        logon.encryptMethod = EncryptMethod.NONE_OTHER;
//        logon.heartBtInt = 10;
//        logon.resetSeqNumFlag = true;
//
//        int offset = 0;
//        int length = CodeUtils.encodeMessage(SEND_BUFFER, offset, logon, SEND_HEADER, SEND_TRAILER);
////        ch.write(SEND_BUFFER, offset, length);
//        System.out.println(CodeUtils.toString(SEND_BUFFER));
//        SEND_BUFFER.flip();
//        while (SEND_BUFFER.hasRemaining()) {
//            int sent = ch.write(SEND_BUFFER);
//            System.out.println("Sent " + sent + " bytes");
//        }
//
//        int bytesRead = ch.read(RECEIVE_BUFFER);
//        System.out.println("bytesRead = " + bytesRead);
//        if (bytesRead > 0) {
//            FIXMessage mes = CodeUtils.decodeMessage(RECEIVE_BUFFER, 0, REC_HEADER, REC_TRAILER, FACTORY);
//
//            if (mes != null) {
//                System.out.println("Got mes type: " + mes.getType() + "\t " + mes.getClass().getSimpleName());
//                System.out.println("mes = " + mes);
//            }
//        }
//    }
}
