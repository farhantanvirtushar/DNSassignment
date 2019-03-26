import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Main {

    static ByteArrayOutputStream byteArrayOutputStream;
    static DataOutputStream dataOutputStream;
    static ByteArrayInputStream byteArrayInputStream;
    static DataInputStream dataInputStream;

    //all root server ip addresses
    static String rootServer[] = {"192.5.5.241","198.41.0.4","199.9.14.201","192.33.4.12","199.7.91.13","192.203.230.10","192.112.36.4","198.97.190.53","192.36.148.17","192.58.128.30","193.0.14.129","199.7.83.42","202.12.27.33"};

    static String ipAddress;
    static String nameServer;

    //DOMAIN_NAME == host name that is asked for ip
    static String DOMAIN_NAME;
    public static void main(String[] args) {


        Scanner scanner = new Scanner(System.in);

        //get the domain name from first argument of command line
        DOMAIN_NAME=scanner.nextLine();
        nameServer=DOMAIN_NAME;

        //print the query domain name
        System.out.println("\n\n;; QUESTION SECTION:");
        printAnswer(DOMAIN_NAME,0,"A"," ");

        //print the answers
        System.out.println("\n\n;; ANSWER SECTION:");

        //loop through all root server addresses. If any one of them is active , then dns query is sent to the root address.
        for(int i=0;i<rootServer.length;i++)
        {
            ipAddress = getIP(DOMAIN_NAME,rootServer[i],i);     //getIP() method does the iterative query for DOMAIN_NAME
            if(!(ipAddress.equals("time out")))
            {
                break;
            }
        }
    }


    static String getIP(String domainName,String serverAddress,int serverNo)
    {
        //System.out.println("Domain name :"+ domainName+" , "+"Server address : "+serverAddress);
        //int x;
        //Scanner scanner = new Scanner(System.in);
        //x=scanner.nextInt();


        //split the domain name into domainNameParts[] array.
        String domainNameParts[]=domainName.split("\\.");

        byteArrayOutputStream=new ByteArrayOutputStream();
        dataOutputStream=new DataOutputStream(byteArrayOutputStream);

        try{

            //Header section
            dataOutputStream.writeShort(0x1234);    //Transaction ID
            dataOutputStream.writeShort(0x0000);    //Flags. Flag = 0x0000 means , "do query iteratively"
            dataOutputStream.writeShort(0x0001);    //Number of questions
            dataOutputStream.writeShort(0x0000);    //Number of answers
            dataOutputStream.writeShort(0x0000);    //Number of authoritative RRs
            dataOutputStream.writeShort(0x0000);    //Number of additional RRs


            //get a byte array from domainNameParts[]
            int len = domainNameParts.length;
            for(int i=0;i<len;i++)
            {
                byte b[]=domainNameParts[i].getBytes();
                dataOutputStream.writeByte(b.length);
                dataOutputStream.write(b);
            }

            //Header section
            dataOutputStream.writeByte(0x00);
            dataOutputStream.writeShort(0x0001);
            dataOutputStream.writeShort(0x0001);

            byte dnsQueryMessage[]=byteArrayOutputStream.toByteArray();
            int msgLen = dnsQueryMessage.length;


            //Create a datagram socket and a datagram packet at port 53 and root server address
            DatagramSocket datagramSocket = new DatagramSocket();
            DatagramPacket datagramPacket = new DatagramPacket(dnsQueryMessage,msgLen, InetAddress.getByName(serverAddress),53);
            //send the packet
            datagramSocket.send(datagramPacket);
            //set time out 1s (1000 ms)
            datagramSocket.setSoTimeout(1000);

            byte answer[] = new byte[1024];
            try {

                DatagramPacket receivedPacked = new DatagramPacket(answer, answer.length);
                datagramSocket.receive(receivedPacked);
            }catch (SocketTimeoutException e)   //if time is out , then the getIP() method will return a message "time out"
            {
                return "time out";
            }

            //the DNS reply message is stored in answer[] array
            byteArrayInputStream = new ByteArrayInputStream(answer);
            dataInputStream = new DataInputStream(byteArrayInputStream);

            dataInputStream.readShort();    //Transaction ID
            dataInputStream.readShort();    //Flags
            dataInputStream.readShort();    //Questions
            short answerRRs = dataInputStream.readShort();    //Answer RRs
            short authorityRRs = dataInputStream.readShort();    //Authority RRs
            short additionalRRs = dataInputStream.readShort();    //Additional RRs

            //Query section
            String queryDomainName = getName(answer);   //query domain name
            dataInputStream.readShort();    //Query Type
            dataInputStream.readShort();    //Class : IN


            //loop through all answer RRs
            for(int i=1;i<=answerRRs;i++)
            {
                String type="";
                String ip="";
                String name = getName(answer);  //Name :

                int t = dataInputStream.readShort();
                type=getType(t);
                dataInputStream.readShort();    //Class : IN
                int timeToLive = dataInputStream.readInt();      //Time to live :
                dataInputStream.readShort();    //Data length :

                if(type.equals("A"))
                {
                    //read four parts of IPv4 address
                    int a=dataInputStream.readByte();
                    a= a & 0x000000ff;          // to make sure "a" is unsigned during type cast from byte to int
                    int b=dataInputStream.readByte();
                    b= b & 0x000000ff;
                    int c=dataInputStream.readByte();
                    c= c & 0x000000ff;
                    int d=dataInputStream.readByte();
                    d= d & 0x000000ff;

                    ip = String.format("%d.%d.%d.%d",a,b,c,d); //ip address

                    if(name.equals(DOMAIN_NAME))
                    {
                        printAnswer(DOMAIN_NAME,timeToLive,type,ip); //if ip address belongs to DOMAIN_NAME , then it will be printed
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
                    if(name.equals(DOMAIN_NAME))
                    {
                        printAnswer(DOMAIN_NAME,timeToLive,type,ipv6Address);
                    }
                }
                else if(type.equals("CNAME"))
                {
                    String cname=getName(answer);
                    printAnswer(DOMAIN_NAME,timeToLive,type,cname);
                    if(name.equals(DOMAIN_NAME))
                    {
                        DOMAIN_NAME=cname;
                        return getIP(DOMAIN_NAME,rootServer[serverNo],serverNo);    //CNAME will be printed
                    }
                }
                else if(type.equals("SOA"))
                {
                    System.out.println("        "+domainName+"  :  Does Not Exist");    //SOA means the domain name dose not exist.
                    return "Does Not Exist";
                }
                if(i==answerRRs)
                {
                    return ip;
                }

            }

            //loop through all authority RRs
            for(int i=0;i<authorityRRs;i++)
            {
                String type="";
                String name = getName(answer);  //Name :

                int t = dataInputStream.readShort();
                type=getType(t);
                dataInputStream.readShort();    //Class : IN
                dataInputStream.readInt();      //Time to live :
                dataInputStream.readShort();    //Data length :

                if(type.equals("CNAME"))
                {
                    String cname=getName(answer);
                    return getIP(cname,rootServer[serverNo],serverNo);
                }
                else if(type.equals("NS"))
                {
                    String str =getName(answer);
                    nameServer=str;
                    if(additionalRRs==0)
                    {
                        String nameServerIp=getIP(nameServer,rootServer[serverNo],serverNo);
                        String ip= getIP(domainName,nameServerIp,serverNo);
                        if(!(ip.equals("time out")))
                        {
                            return ip;
                        }
                    }
                }
                else if(type.equals("SOA"))
                {
                    System.out.println("        "+domainName+"  :  Does Not Exist");
                    return "Does Not Exist";
                }
            }

            //loop through all additional RRs
            for(int i=0;i<additionalRRs;i++)
            {
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
                    String ip = getIP(domainName,str,serverNo);
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

            }
        }
        catch (Exception e)
        {
            System.out.println("Error ");
            e.printStackTrace();
        }
        return getIP(nameServer,rootServer[serverNo],serverNo);
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
        if(a==6)
        {
            return "SOA";
        }
        if(a==28)
        {
            return "AAAA";
        }
        return "Unknown";
    }
    static  void printAnswer(String domainName,int timeToLive,String type,String address)
    {
        System.out.println(String.format("%30s",domainName)+ "  "+String.format("%10s",timeToLive)+"    IN  "+String.format("%10s",type)+"    "+address);
    }
}
