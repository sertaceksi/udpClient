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
            address = InetAddress.getByName(Constant.HOSTNAME);
            clientId = generateRandomIntegerInRange();
            sequenceId = generateRandomIntegerInRange();
            value = generateRandomIntegerInRange();
            nackCount = Constant.RAND_MIN_LIMIT;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            value = generateRandomIntegerInRange();
            sendEcho();
        }
    }

    private void sendEcho() {
        try {
            sendPacketToServer();
            DatagramPacket packet = receiveNotificationFromServer();
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
        nackCount++;
        if (nackCount.equals(Constant.MAX_TRY)) {
            nackCount = 0;
            generateLostFile();
            sequenceId++;
        }
    }

    private void analyzeNotification(DatagramPacket packet) throws InterruptedException {
        ByteBuffer wrapped = ByteBuffer.wrap(packet.getData());
        if (wrapped.getInt(Constant.CLIENT_INDEX) == clientId && wrapped.getInt(Constant.SEQUENCE_INDEX) == sequenceId) {
            if (wrapped.get(Constant.NOTIFICATION_INDEX) == 0) {
                if (nackCount < Constant.MAX_TRY) {
                    nackCount++;
                } else {
                    nackCount = 0;
                    generateLostFile();
                    sequenceId++;
                    sleep(Constant.SLEEP_TIME);
                }
            } else {
                sequenceId++;
                sleep(Constant.SLEEP_TIME);
            }
        }
    }

    private void generateLostFile() {
        try {
            String fileName = clientId + Constant.LOST_FILE_EXTENSION;
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
        return "\n" + sequenceId + "," + value;
    }

    private void sendPacketToServer() throws IOException {
        ByteBuffer byteBuffer = generateClientPacket(clientId, sequenceId, value);
        byte[] senderBuf = byteBuffer.array();
        DatagramPacket packet = new DatagramPacket(senderBuf, senderBuf.length, address, Constant.PORT);
        socket.send(packet);
    }

    private DatagramPacket receiveNotificationFromServer() throws IOException {
        byte[] receiverBuf = new byte[Constant.NOTIFY_MESSAGE_SIZE];
        DatagramPacket packet = new DatagramPacket(receiverBuf, receiverBuf.length);
        socket.receive(packet);
        return packet;
    }

    private ByteBuffer generateClientPacket(int clientId, int sequenceId, int value) {
        byte[] clientIdBytes = ByteBuffer.allocate(Constant.CLIENT_SIZE).putInt(clientId).array();
        byte[] sequenceIdBytes = ByteBuffer.allocate(Constant.SEQUENCE_SIZE).putInt(sequenceId).array();
        byte[] valueBytes = ByteBuffer.allocate(Constant.VALUE_SIZE).putInt(value).array();
        byte[] checkSumBytes = calculateChecksum(clientIdBytes, sequenceIdBytes, valueBytes);

        ByteBuffer byteBuffer = ByteBuffer.allocate(Constant.MESSAGE_SIZE);
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
        return ByteBuffer.allocate(Constant.CHECKSUM_SIZE).putLong(checksum.getValue()).array();
    }


    private static int generateRandomIntegerInRange() {
        if (Constant.RAND_MIN_LIMIT >= Constant.RAND_MAX_LIMIT) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((Constant.RAND_MAX_LIMIT - Constant.RAND_MIN_LIMIT) + 1) + Constant.RAND_MIN_LIMIT;
    }


}