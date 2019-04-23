import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class ClientTest {

    private int clientId;
    private int sequenceId;
    private int value;

    @Test
    public void sequenceIdIncrementTest() throws IOException {
        DatagramSocket socket = new DatagramSocket(Constant.PORT);
        DatagramPacket packet = receiveAndInitializeData(socket);
        ByteBuffer wrapped = ByteBuffer.wrap(packet.getData());
        clientId = wrapped.getInt(Constant.CLIENT_INDEX);
        sequenceId = wrapped.getInt(Constant.SEQUENCE_INDEX);
        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        ByteBuffer byteBuffer = generateResponseBuffer(Constant.ACK_VALUE);
        byte[] senderBuf = byteBuffer.array();
        DatagramPacket responsePacket = new DatagramPacket(senderBuf, senderBuf.length, address, port);
        socket.send(responsePacket);
        DatagramPacket secondPacket = receiveAndInitializeData(socket);
        ByteBuffer secondWrapped = ByteBuffer.wrap(secondPacket.getData());
        int secondClientId = secondWrapped.getInt(Constant.CLIENT_INDEX);
        int secondSequenceId = secondWrapped.getInt(Constant.SEQUENCE_INDEX);
        Assert.assertEquals(secondClientId, clientId);
        Assert.assertEquals(secondSequenceId, sequenceId + 1);
        socket.close();
    }

    @Test
    public void lostFileGeneratingTest() throws IOException {
        DatagramSocket socket = new DatagramSocket(Constant.PORT);
        ByteBuffer wrapped = null;

        for (int i = 0; i < Constant.MAX_TRY; i++) {
            DatagramPacket packet = receiveAndInitializeData(socket);
            wrapped = ByteBuffer.wrap(packet.getData());
            clientId = wrapped.getInt(Constant.CLIENT_INDEX);
            sequenceId = wrapped.getInt(Constant.SEQUENCE_INDEX);
            value = wrapped.getInt(Constant.VALUE_INDEX);
        }
        String fileName = wrapped.getInt(Constant.CLIENT_INDEX) + Constant.LOST_FILE_EXTENSION;
        File f = new File(fileName);
        Assert.assertTrue(f.exists());

        isValueWrittenInFile(fileName);
    }

    private void isValueWrittenInFile(String fileName) throws IOException {
        BufferedReader b = new BufferedReader(new FileReader(fileName));
        String strLine = b.readLine();

        while (strLine != null) {
            String[] values = strLine.split(",");
            if (!strLine.equals("") && Integer.parseInt(values[0]) == sequenceId) {
                Assert.assertEquals(Integer.parseInt(values[1]), value);
                break;
            }
            strLine = b.readLine();
        }
    }

    private DatagramPacket receiveAndInitializeData(DatagramSocket socket) throws IOException {
        byte[] receiverBuf = new byte[Constant.MESSAGE_SIZE];
        DatagramPacket packet = new DatagramPacket(receiverBuf, receiverBuf.length);
        socket.receive(packet);
        return packet;
    }

    private ByteBuffer generateResponseBuffer(byte ack) {

        byte[] clientIdBytes = ByteBuffer.allocate(Constant.CLIENT_SIZE).putInt(clientId).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(Constant.SEQUENCE_SIZE).putInt(sequenceId).array();
        byte[] ackNumBytes = ByteBuffer.allocate(Constant.NOTIFICATION_SIZE).put(ack).array();

        ByteBuffer byteBuffer = ByteBuffer.allocate(Constant.NOTIFY_MESSAGE_SIZE);
        byteBuffer.put(clientIdBytes);
        byteBuffer.put(sequenceIdBytes);
        byteBuffer.put(ackNumBytes);

        return byteBuffer;
    }
}
