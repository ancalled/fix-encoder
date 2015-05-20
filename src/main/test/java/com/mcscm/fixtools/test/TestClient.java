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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

public class TestClient {


    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 9885;
        String sender = "fix-client";
        String target = "fix-server";
        String username = "usr123";
        String pass = "qwerty";
//        String pass = "qwerty1";

        ByteBuffer SEND_BUFFER = ByteBuffer.allocateDirect(1024);
        ByteBuffer RECEIVE_BUFFER = ByteBuffer.allocateDirect(2048);
        Header SEND_HEADER = new Header();
        SEND_HEADER.beginString = "FIXT.1.1";
        SEND_HEADER.applVerID = ApplVerID.FIX50;
        SEND_HEADER.msgSeqNum = 1;
        SEND_HEADER.senderCompID = sender;
        SEND_HEADER.targetCompID = target;
        SEND_HEADER.sendingTime = new Date();
        Trailer SEND_TRAILER = new Trailer();

        Header REC_HEADER = new Header();
        Trailer REC_TRAILER = new Trailer();
        MessageFactory FACTORY = new FIXMessageFactory();

        SocketChannel ch = SocketChannel.open();
        ch.configureBlocking(true);
//        ch.configureBlocking(false);
        ch.connect(new InetSocketAddress(host, port));

        Logon logon = new Logon();
        logon.defaultApplVerID = DefaultApplVerID.FIX50;
        logon.username = username;
        logon.password = pass;
        logon.encryptMethod = EncryptMethod.NONE_OTHER;
        logon.heartBtInt = 10;
        logon.resetSeqNumFlag = true;

        int offset = 0;
        int length = CodeUtils.encodeMessage(SEND_BUFFER, offset, logon, SEND_HEADER, SEND_TRAILER);
//        ch.write(SEND_BUFFER, offset, length);
        System.out.println(CodeUtils.toString(SEND_BUFFER));
        SEND_BUFFER.flip();
        while (SEND_BUFFER.hasRemaining()) {
            int sent = ch.write(SEND_BUFFER);
            System.out.println("Sent " + sent + " bytes");
        }

        int bytesRead = ch.read(RECEIVE_BUFFER);
        System.out.println("bytesRead = " + bytesRead);
        if (bytesRead > 0) {
            FIXMessage mes = CodeUtils.decodeMessage(RECEIVE_BUFFER, 0, REC_HEADER, REC_TRAILER, FACTORY);

            if (mes != null) {
                System.out.println("Got mes type: " + mes.getType() + "\t " + mes.getClass().getSimpleName());
                System.out.println("mes = " + mes);
            }
        }
    }
}
