import jdk.internal.dynalink.beans.StaticClass;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

            byte answer[]=new byte[1024];
            DatagramPacket receivedPacked=new DatagramPacket(answer,answer.length);
            datagramSocket.receive(receivedPacked);

            //System.out.println("received packet size : "+receivedPacked.getLength());
            /*for(int i=0;i<receivedPacked.getLength();i++)
            {
                System.out.print(String.format("%x",answer[i])+" ");
            }*/

            //System.out.println("\n");
            byteArrayInputStream = new ByteArrayInputStream(answer);
            dataInputStream = new DataInputStream(byteArrayInputStream);

            //System.out.println("Transaction ID : 0x"+String.format("%x",dataInputStream.readShort()));
            //System.out.println("Flags : 0x"+String.format("%x",dataInputStream.readShort()));
            //System.out.println("Questions : 0x"+String.format("%x",dataInputStream.readShort()));
            //System.out.println("Answer RRs : 0x"+String.format("%x",dataInputStream.readShort()));
            //System.out.println("Authority RRs : 0x"+String.format("%x",dataInputStream.readShort()));
            //System.out.println("Additional RRs : 0x"+String.format("%x",dataInputStream.readShort()));
            dataInputStream.readShort();
            dataInputStream.readShort();
            dataInputStream.readShort();
            dataInputStream.readShort();
            dataInputStream.readShort();
            dataInputStream.readShort();


            String queryDomainName = getName();
//            System.out.println("Queries : ");
//            System.out.println("    Name : "+ queryDomainName);
//            System.out.println("    Type : "+ getType(dataInputStream.readShort()));
//            System.out.println("    Class : IN (0x"+String.format("%x",dataInputStream.readShort())+")");
            dataInputStream.readShort();
            dataInputStream.readShort();

            while (true)
            {

                count++;
                String type="";
                String name = getName();

//                System.out.println("Answer : ");
//                System.out.println("    Name : "+ queryDomainName);
                int t = dataInputStream.readShort();
                type=getType(t);
//                System.out.println("    Type : "+ type);
//                System.out.println("    Class : IN (0x"+String.format("%x",dataInputStream.readShort())+")");
//                System.out.println("    Time to live : "+String.format("%d",dataInputStream.readInt()));
//                System.out.println("    Data length : "+String.format("%d",dataInputStream.readShort()));

                dataInputStream.readShort();
                dataInputStream.readInt();
                dataInputStream.readShort();

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
                    //ipAddressList.add(str);

                    //System.out.println("domain name : "+domainName);
                    //System.out.println("name : "+name);
                    if(count==1)
                    {
                        return str;
                    }
                   return getIP(domainName,str);

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
                else
                {
                    String str =getName();
                    if(count==1)
                    {

                        nameServer = str;
                        //System.out.println("    Name Server : "+nameServer);
                    }

                }

            }
        }catch (EOFException e)
        {
            //ipAddress = new String[ipAddressList.size()];
            //ipAddress = ipAddressList.toArray(ipAddress);
            String ip=getIP(nameServer,rootServer);
           // System.out.println("found missing additional record : "+ip);
            return getIP(domainName,ip);
        }
        catch (Exception e)
        {
            System.out.println("Error ");
            System.out.println(e);
        }
        //ipAddress = new String[ipAddressList.size()];
        //ipAddress = ipAddressList.toArray(ipAddress);
        return getIP(nameServer,rootServer);
    }
    static String getName()throws Exception
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
                dataInputStream.readByte();
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
