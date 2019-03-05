import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {

    static ByteArrayOutputStream byteArrayOutputStream;
    static DataOutputStream dataOutputStream;
    public static void main(String[] args)throws Exception {

        String domainName="www.google.com";
        String domainNameParts[]=domainName.split("\\.");
        byteArrayOutputStream=new ByteArrayOutputStream();
        dataOutputStream=new DataOutputStream(byteArrayOutputStream);
        {
            dataOutputStream.writeShort(0x1234);
            dataOutputStream.writeShort(0x0000);
            dataOutputStream.writeShort(0x0001);
            dataOutputStream.writeShort(0x0000);
            dataOutputStream.writeShort(0x0000);
            dataOutputStream.writeShort(0x0000);


            int len = domainNameParts.length;
            for(int i=0;i<len;i++)
            {
                byte b[]=domainNameParts[i].getBytes();
                dataOutputStream.writeByte(b.length);
                dataOutputStream.write(b);
            }

            dataOutputStream.writeByte(0x00);
            dataOutputStream.writeShort(0x0001);
            dataOutputStream.writeShort(0x0001);

            byte dnsQueryMessage[]=byteArrayOutputStream.toByteArray();
            int msgLen = dnsQueryMessage.length;
            String rootServer = "198.41.0.4";

            DatagramSocket datagramSocket = new DatagramSocket();
            DatagramPacket datagramPacket = new DatagramPacket(dnsQueryMessage,msgLen, InetAddress.getByName(rootServer),53);
            datagramSocket.send(datagramPacket);

            byte answer[]=new byte[1024];
            DatagramPacket receivedPacked=new DatagramPacket(answer,answer.length);
            datagramSocket.receive(receivedPacked);

            System.out.println("received packet size : "+answer.length);
            for(int i=0;i<receivedPacked.getLength();i++)
            {
                System.out.print("0x"+String.format("%x",answer[i])+" ");
            }

        }
    }
}
