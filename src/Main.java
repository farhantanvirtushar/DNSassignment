import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Main {

    static ByteArrayOutputStream byteArrayOutputStream;
    static DataOutputStream dataOutputStream;
    static ByteArrayInputStream byteArrayInputStream;
    static DataInputStream dataInputStream;

    static String rootServer;
    static String ipAddress;
    static String nameServer;
    public static void main(String[] args) {

        if(args.length==0)
        {
            System.out.println("Error");
        }
        String domainName=args[0];
        nameServer=domainName;
        rootServer = "192.5.5.241";
        ipAddress = getIP(domainName,rootServer);
        System.out.println("ip address : "+ipAddress);

    }


    static String getIP(String domainName,String serverAddress)
    {
        //System.out.println("Domain name :"+ domainName+" , "+"Server address : "+serverAddress);
        int count=0;
        //int x;
        //Scanner scanner = new Scanner(System.in);
        //x=scanner.nextInt();

        String ipAddress[];
        List<String> ipAddressList = new ArrayList<String>();
        String domainNameParts[]=domainName.split("\\.");
        byteArrayOutputStream=new ByteArrayOutputStream();
        dataOutputStream=new DataOutputStream(byteArrayOutputStream);
        try{
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


            DatagramSocket datagramSocket = new DatagramSocket();
            DatagramPacket datagramPacket = new DatagramPacket(dnsQueryMessage,msgLen, InetAddress.getByName(serverAddress),53);
            datagramSocket.send(datagramPacket);
            datagramSocket.setSoTimeout(1000);

            byte answer[] = new byte[1024];
            try {

                DatagramPacket receivedPacked = new DatagramPacket(answer, answer.length);
                datagramSocket.receive(receivedPacked);
            }catch (SocketTimeoutException e)
            {
                return "time out";
            }

            byteArrayInputStream = new ByteArrayInputStream(answer);
            dataInputStream = new DataInputStream(byteArrayInputStream);

            dataInputStream.readShort();    //Transaction ID
            dataInputStream.readShort();    //Flags
            dataInputStream.readShort();    //Questions
            dataInputStream.readShort();    //Answer RRs
            dataInputStream.readShort();    //Authority RRs
            dataInputStream.readShort();    //Additional RRs

            //Query section
            String queryDomainName = getName(answer);   //query domain name
            dataInputStream.readShort();    //Query Type
            dataInputStream.readShort();    //Class : IN

            while (true)
            {
                //Answer section

                count++;
                String type="";
                String name = getName(answer);  //Name :

                int t = dataInputStream.readShort();
                type=getType(t);
                dataInputStream.readShort();    //Class : IN
                dataInputStream.readInt();      //Time to live :
                dataInputStream.readShort();    //Data length :

                if(type.equals("A"))
                {
                    int a=dataInputStream.readByte();
                    a= a & 0x000000ff;
                    int b=dataInputStream.readByte();
                    b= b & 0x000000ff;
                    int c=dataInputStream.readByte();
                    c= c & 0x000000ff;
                    int d=dataInputStream.readByte();
                    d= d & 0x000000ff;

                    String str = String.format("%d.%d.%d.%d",a,b,c,d);

                    if(name.equals(domainName))
                    {
                        return str;
                    }
                    String ip = getIP(domainName,str);
                    if(!(ip.equals("time out")))
                    {
                        return ip;
                    }
                }
                else if(type.equals("AAAA"))
                {
                    int a=dataInputStream.readShort();
                    int b=dataInputStream.readShort();
                    int c=dataInputStream.readShort();
                    int d=dataInputStream.readShort();
                    int e=dataInputStream.readShort();
                    int f=dataInputStream.readShort();
                    int g=dataInputStream.readShort();
                    int h=dataInputStream.readShort();
                    String ipv6Address=String.format("%x:%x:%x:%x:%x:%x:%x:%x",a,b,c,d,e,f,g,h);
                    //System.out.println("IPV6 Address : "+ipv6Address);
                }
                else if(type.equals("CNAME"))
                {
                    String cname=getName(answer);
                    return getIP(cname,rootServer);
                }
                else if(type.equals("NS"))
                {
                    String str =getName(answer);

                    if(count==1)
                    {
                        nameServer = str;
                    }

                }

            }
        }catch (EOFException e)
        {
            String ip=getIP(nameServer,rootServer);
            return getIP(domainName,ip);
        }
        catch (Exception e)
        {
            System.out.println("Error ");
            e.printStackTrace();
        }
        return getIP(nameServer,rootServer);
    }
    static String getName(byte answer[])throws Exception
    {
        byte DomainNameByteArray[] = new byte[1024];
        int i=0;
        while (true)
        {
            byte k=dataInputStream.readByte();
            if(k==0)
            {
                break;
            }
            if(String.format("%x",k).equals("c0"))
            {
                int  index = dataInputStream.readByte();
                index= index & 0x000000ff;
                int j=answer[index];
                index++;
                while (j>0)
                {
                    if(i>0)
                    {
                        DomainNameByteArray[i]='.';
                        i++;
                    }

                    for(int l=0;l<j;l++)
                    {
                        DomainNameByteArray[i]=answer[index];
                        i++;
                        index++;
                    }
                    j=answer[index];
                    index++;
                }

                break;
            }
            if(i>0)
            {
                DomainNameByteArray[i]='.';
                i++;
            }

            for(int j=0;j<k;j++)
            {
                byte b=dataInputStream.readByte();
                DomainNameByteArray[i]=b;
                i++;
            }

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
        if(a==28)
        {
            return "AAAA";
        }
        return "Unknown";
    }
}
