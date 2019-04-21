import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;

class Client extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
    private Integer clientId;
    private Integer sequenceId;
    private Integer value;
    private Integer nackCount;

    Client() throws UnknownHostException {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            address = InetAddress.getByName("localhost");
            clientId = generateRandomIntegerInRange(0, 65535);
            sequenceId = generateRandomIntegerInRange(0, 65535);
            value = generateRandomIntegerInRange(0, 65535);
            nackCount = 0;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            value = generateRandomIntegerInRange(0, 65535);
            sendEcho();
        }
    }

    private void sendEcho() {
        try {
            DatagramPacket packet = sendPacketToServer();
            packet = receiveNotificationFromServer(packet);
            analyzeNotification(packet);
        } catch (SocketTimeoutException e) {
            handleSocketTimeOut();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSocketTimeOut() {
        if (nackCount < 3) {
            nackCount++;
        } else {
            nackCount = 0;
            generateLostFile();
            sequenceId++;
            sendEcho();
        }
    }

    private void analyzeNotification(DatagramPacket packet) throws InterruptedException {
        ByteBuffer wrapped = ByteBuffer.wrap(packet.getData());
        if (wrapped.getInt(0) == clientId && wrapped.getInt(4) == sequenceId) {
            if (wrapped.get(8) == 0) {
                if (nackCount < 3) {
                    nackCount++;
                    sendEcho();
                } else {
                    nackCount = 0;
                    generateLostFile();
                    sequenceId++;
                    sleep(100);
                    sendEcho();
                }
            } else {
                sequenceId++;
                sleep(100);
                sendEcho();
            }
        }
    }

    private void generateLostFile() {
        try {
            String fileName = clientId + ".lost.txt";
            FileWriter fileWriter;
            fileWriter = new FileWriter(fileName, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(newLine());
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String newLine() {
        return sequenceId + "," + value + "\n";
    }

    private DatagramPacket sendPacketToServer() throws IOException {
        ByteBuffer byteBuffer = generateClientPacket(clientId, sequenceId, value);
        byte[] senderBuf = byteBuffer.array();
        DatagramPacket packet = new DatagramPacket(senderBuf, senderBuf.length, address, 4445);
        socket.send(packet);
        return packet;
    }

    private DatagramPacket receiveNotificationFromServer(DatagramPacket packet) throws IOException {
        byte[] receiverBuf = new byte[9];
        packet = new DatagramPacket(receiverBuf, receiverBuf.length);
        socket.receive(packet);
        return packet;
    }

    private ByteBuffer generateClientPacket(int clientId, int sequenceId, int value) {
        byte[] clientIdBytes = ByteBuffer.allocate(4).putInt(clientId).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(4).putInt(sequenceId).array();
        byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
        byte[] checkSumBytes = calculateChecksum(clientIdBytes, sequenceIdBytes, valueBytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(20);
        byteBuffer.put(clientIdBytes);
        byteBuffer.put(sequenceIdBytes);
        byteBuffer.put(valueBytes);
        byteBuffer.put(checkSumBytes);

        return byteBuffer;
    }

    private byte[] calculateChecksum(byte[] clientIdBytes, byte[] sequenceIdBytes, byte[] valueBytes) {
        CRC32 checksum = new CRC32();
        checksum.update(clientIdBytes);
        checksum.update(sequenceIdBytes);
        checksum.update(valueBytes);
        return ByteBuffer.allocate(8).putLong(checksum.getValue()).array();
    }

    void close() {
        socket.close();
    }

    private static int generateRandomIntegerInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }


}