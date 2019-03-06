import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Main {

    static ByteArrayOutputStream byteArrayOutputStream;
    static DataOutputStream dataOutputStream;
    static ByteArrayInputStream byteArrayInputStream;
    static DataInputStream dataInputStream;
    public static void main(String[] args)throws Exception {

        String domainName="www.google.com";
        //String domainName = args[1];
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

            System.out.println("received packet size : "+receivedPacked.getLength());
            for(int i=0;i<receivedPacked.getLength();i++)
            {
                System.out.print(String.format("%x",answer[i])+" ");
            }

            System.out.println("\n");
            byteArrayInputStream = new ByteArrayInputStream(answer);
            dataInputStream = new DataInputStream(byteArrayInputStream);

            System.out.println("Transaction ID : 0x"+String.format("%x",dataInputStream.readShort()));
            System.out.println("Flags : 0x"+String.format("%x",dataInputStream.readShort()));
            System.out.println("Questions : 0x"+String.format("%x",dataInputStream.readShort()));
            System.out.println("Answer RRs : 0x"+String.format("%x",dataInputStream.readShort()));
            System.out.println("Authority RRs : 0x"+String.format("%x",dataInputStream.readShort()));
            System.out.println("Additional RRs : 0x"+String.format("%x",dataInputStream.readShort()));


            String queryDomainName = getName();
            System.out.println("Queries : ");
            System.out.println("    Name : "+ queryDomainName);
            System.out.println("    Type : "+ getType(dataInputStream.readShort()));
            System.out.println("    Class : IN (0x"+String.format("%x",dataInputStream.readShort())+")");

            dataInputStream.readShort();
            System.out.println("Answer : ");
            System.out.println("    Name : "+ queryDomainName);
            System.out.println("    Type : "+ getType(dataInputStream.readShort()));
            System.out.println("    Class : IN (0x"+String.format("%x",dataInputStream.readShort())+")");
            System.out.println("    Time to live : "+String.format("%d",dataInputStream.readInt()));
            System.out.println("    Data length : "+String.format("%d",dataInputStream.readShort()));
            String nameServer = getName();
            System.out.println("    Name Server : "+nameServer);

        }
    }

    static String getName()throws Exception
    {
        dataInputStream.readByte();
        byte DomainNameByteArray[] = new byte[1024];

        byte b=0;
        int i=0;
        while ((b=dataInputStream.readByte())>0)
        {

            if(!((b>='a' && b<='z')||(b>='A' && b<='Z')||(b=='-')))
            {
                b='.';
            }
            DomainNameByteArray[i]=b;
            i++;
        }
        String DomainName = new String(DomainNameByteArray,0,i);
        return DomainName;
    }

    static String getType(int a)
    {
        if(a==1)
        {
            return "A";
        }
        if(a==2)
        {
            return "NS";
        }
        if(a==5)
        {
            return "CNAME";
        }
        return "Unknown";
    }
}
