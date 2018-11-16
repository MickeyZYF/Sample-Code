package ca.ubc.cs.cs317.dnslookup;

import java.io.Console;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class DNSLookupService {

    private static final int DEFAULT_DNS_PORT = 53;
    private static final int MAX_INDIRECTION_LEVEL = 10;

    private static InetAddress rootServer;
    private static boolean verboseTracing = false;
    private static DatagramSocket socket;

    private static DNSCache cache = DNSCache.getInstance();

    private static Random random = new Random();

    private static int index = 0; // Used to track where we end up after getting the Domain Name, Don't forget to reset when doing iterative query 
    private static InetAddress NameServer;

    /**
     * Main function, called when program is first invoked.
     *
     * @param args list of arguments specified in the command line.
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Invalid call. Usage:");
            System.err.println("\tjava -jar DNSLookupService.jar rootServer");
            System.err.println("where rootServer is the IP address (in dotted form) of the root DNS server to start the search at.");
            System.exit(1);
        }

        try {
            rootServer = InetAddress.getByName(args[0]);
            System.out.println("Root DNS server is: " + rootServer.getHostAddress());
        } catch (UnknownHostException e) {
            System.err.println("Invalid root server (" + e.getMessage() + ").");
            System.exit(1);
        }

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
        } catch (SocketException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        Console console = System.console();
        do {
            // Use console if one is available, or standard input if not.
            String commandLine;
            if (console != null) {
                System.out.print("DNSLOOKUP> ");
                commandLine = console.readLine();
            } else
                try {
                    System.out.print("DNSLOOKUP> ");
                    commandLine = in.nextLine();
                } catch (NoSuchElementException ex) {
                    break;
                }
            // If reached end-of-file, leave
            if (commandLine == null) break;

            // Ignore leading/trailing spaces and anything beyond a comment character
            commandLine = commandLine.trim().split("#", 2)[0];

            // If no command shown, skip to next command
            if (commandLine.trim().isEmpty()) continue;

            String[] commandArgs = commandLine.split(" ");

            if (commandArgs[0].equalsIgnoreCase("quit") ||
                    commandArgs[0].equalsIgnoreCase("exit"))
                break;
            else if (commandArgs[0].equalsIgnoreCase("server")) {
                // SERVER: Change root nameserver
                if (commandArgs.length == 2) {
                    try {
                        rootServer = InetAddress.getByName(commandArgs[1]);
                        System.out.println("Root DNS server is now: " + rootServer.getHostAddress());
                    } catch (UnknownHostException e) {
                        System.out.println("Invalid root server (" + e.getMessage() + ").");
                        continue;
                    }
                } else {
                    System.out.println("Invalid call. Format:\n\tserver IP");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("trace")) {
                // TRACE: Turn trace setting on or off
                if (commandArgs.length == 2) {
                    if (commandArgs[1].equalsIgnoreCase("on"))
                        verboseTracing = true;
                    else if (commandArgs[1].equalsIgnoreCase("off"))
                        verboseTracing = false;
                    else {
                        System.err.println("Invalid call. Format:\n\ttrace on|off");
                        continue;
                    }
                    System.out.println("Verbose tracing is now: " + (verboseTracing ? "ON" : "OFF"));
                } else {
                    System.err.println("Invalid call. Format:\n\ttrace on|off");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("lookup") ||
                    commandArgs[0].equalsIgnoreCase("l")) {
                // LOOKUP: Find and print all results associated to a name.
                RecordType type;
                if (commandArgs.length == 2)
                    type = RecordType.A;
                else if (commandArgs.length == 3)
                    try {
                        type = RecordType.valueOf(commandArgs[2].toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        System.err.println("Invalid query type. Must be one of:\n\tA, AAAA, NS, MX, CNAME");
                        continue;
                    }
                else {
                    System.err.println("Invalid call. Format:\n\tlookup hostName [type]");
                    continue;
                }
                findAndPrintResults(commandArgs[1], type);
            } else if (commandArgs[0].equalsIgnoreCase("dump")) {
                // DUMP: Print all results still cached
                cache.forEachNode(DNSLookupService::printResults);
            } else {
                System.err.println("Invalid command. Valid commands are:");
                System.err.println("\tlookup fqdn [type]");
                System.err.println("\ttrace on|off");
                System.err.println("\tserver IP");
                System.err.println("\tdump");
                System.err.println("\tquit");
                continue;
            }

        } while (true);

        socket.close();
        System.out.println("Goodbye!");
    }

    /**
     * Finds all results for a host name and type and prints them on the standard output.
     *
     * @param hostName Fully qualified domain name of the host being searched.
     * @param type     Record type for search.
     */
    private static void findAndPrintResults(String hostName, RecordType type) {

        DNSNode node = new DNSNode(hostName, type);
        printResults(node, getResults(node, 0));
    }

    /**
     * Finds all the result for a specific node.
     *
     * @param node             Host and record type to be used for search.
     * @param indirectionLevel Control to limit the number of recursive calls due to CNAME redirection.
     *                         The initial call should be made with 0 (zero), while recursive calls for
     *                         regarding CNAME results should increment this value by 1. Once this value
     *                         reaches MAX_INDIRECTION_LEVEL, the function prints an error message and
     *                         returns an empty set.
     * @return A set of resource records corresponding to the specific query requested.
     */
    private static Set<ResourceRecord> getResults(DNSNode node, int indirectionLevel) {

        if (indirectionLevel > MAX_INDIRECTION_LEVEL) {
            System.err.println("Maximum number of indirection levels reached.");
            return Collections.emptySet();
        }

        // TODO To be completed by the student

        Set<ResourceRecord> results = cache.getCachedResults(node);
        if (!results.isEmpty()){ // Got something in cache, at Authoritative Name Server, I think.
            return results;
        }

        DNSNode CNAMENode = new DNSNode(node.getHostName(), RecordType.getByCode(5)); // RecordType Code 5 is for CNAME

        NameServer = rootServer;

        while (true) {  // Infinite look as it the final name server still has type CNAME
            index = 0;
            results = cache.getCachedResults(CNAMENode); // Should we uncache something?
            if (results.isEmpty()) {
                if (NameServer != null) {
                    retrieveResultsFromServer(node, NameServer); // Update Cache
                    results = cache.getCachedResults(node); // Cached result return empty
                    if (!results.isEmpty()) {
                        return results;
                    }
                }

            } else {
                Set<ResourceRecord> cnameResults = new HashSet<>();
                for (ResourceRecord cname : results) {
                    CNAMENode = new DNSNode(cname.getTextResult(), node.getType());
                    cnameResults.addAll(getResults(CNAMENode, indirectionLevel++));
                }
                return cnameResults;
            }
        }
        // return cache.getCachedResults(node);
    }

    public static int getBit (byte currentByte, int position){ // Get the bit from currentByte at position
        String binary = String.format("%8s", Integer.toBinaryString(currentByte & 0xff)).replace(' ', '0');
        char[] binaryArray = binary.toCharArray();
        int bit = binaryArray[position - 1] - '0';
        return bit;
    }

    public static byte[] encodeQuery(int ID, DNSNode node) { // Encode the query packet into byte[]
        ByteBuffer query = ByteBuffer.allocate(512);
        // Header
        // ID (16 bits)
        // QR (1 bit)
        // OPCODE (4 bits)
        // AA (1 bit)
        // TC (1 bit)
        // RD (1 bit)
        // RA (1 bit)
        // Z (3 bit)
        // RCODE (4 bit)
        // The previous 16 bits should just be zero, we do not want recursion
        // QDCOUNT (16 bits)
        // ANCOUNT (16 bits)
        // NSCOUNT (16 bits)
        // ARCOUNT (16 bits)
        int QROPCODEAATCRD = 0;
        int RAZRCODE = 0;
        int QDCount = 1; // Question count should be 1 in the query
        int ANCOUNT = 0; // Answer count should be 0 in the query
        int NSCOUNT = 0; // Authority records count should be 0 in the query
        int ARCOUNT = 0; // Additional records count should be 0 in the query

        int QIDPart1 = ID & 0xff;
        int QIDPart2 = (ID >> 8) & 0xff;

        query.put(0, (byte) QIDPart1);
        query.put(1, (byte) QIDPart2);
        query.put(2, (byte) QROPCODEAATCRD);
        query.put(3, (byte) RAZRCODE);
        query.put(4, (byte) 0);
        query.put(5, (byte) QDCount);
        query.put(6, (byte) 0);
        query.put(7, (byte) ANCOUNT);
        query.put(8, (byte) 0);
        query.put(9, (byte) NSCOUNT);
        query.put(10, (byte) 0);
        query.put(11, (byte) ARCOUNT);

        int currentIndex = 12;

        // Question
        // QNAME/URL (No padding)
        String[] hostName = node.getHostName().split("\\.");
        for (int i = 0; i < hostName.length; i++){
            String hostPart = hostName[i];
            query.put(currentIndex, (byte) hostPart.length());
            currentIndex++;
            char[] hostParts = hostPart.toCharArray();
            for (char character : hostPart.toCharArray()){
                query.put(currentIndex, (byte) ((int) character));
                currentIndex++;
            }
        }

        query.put(currentIndex, (byte) 0); // 0 byte indicates the end of QNAME
        currentIndex++;

        // QTYPE (16 bits)
        int QTYPE = node.getType().getCode();
        int QTYPEPart1 = QTYPE & 0xff;
        int QTYPEPart2 = (QTYPE >> 8) & 0xff;
        query.put(currentIndex, (byte) QTYPEPart2);
        currentIndex++;
        query.put(currentIndex, (byte) QTYPEPart1);
        currentIndex++;

        // QCLASS (16 bits)
        int QCLASS = 1;
        query.put(currentIndex, (byte) 0);
        currentIndex++;
        query.put(currentIndex, (byte) QCLASS);
        currentIndex++;


        return Arrays.copyOfRange(query.array(), 0, currentIndex); // Returns a byte[] from index 0 to currentIndex, needed as we don't wanna send empty bytes
    }

    public static String  domainNameCompression(ByteBuffer response, int currentIndex){
        // if the first two bits are 1 and 1, then the name is 2 bytes, and the rest of the bits ar a pointer
        // to a prior occurrence of the domain name
        // if the first two bit are two zeros, then it's just a regular a domain name, at most 63 characters
        String domainName = "";
        boolean recursion = true;
        while (recursion){
            int domainNamePartLength = (response.get(currentIndex)) & 0xff;
            currentIndex++;
            if (domainNamePartLength == 0){
                recursion = false;
            }
            if (domainNamePartLength >= 192){ // Binary for of decimal 192 is 11000000, which signifies it is a pointer
                byte newIndexPart1 = (byte) (domainNamePartLength - 192);
                byte newIndexPart2 = response.get(currentIndex);
                currentIndex++;
                int newIndex = ((newIndexPart1 & 0xff) << 8) | (newIndexPart2 & 0xff);
                domainName += domainNameCompression(response, newIndex);
                recursion = false;
            }
            else {
                for (int i = 0; i < domainNamePartLength; i++){
                    char domainNamePart = (char) (response.get(currentIndex) & 0xff);
                    currentIndex++;
                    domainName += domainNamePart;
                }
                domainName += ".";
            }
        }
        while (domainName.length() > 0 && domainName.charAt(domainName.length() - 1) == '.') { // Remove extra period at the end
            domainName = domainName.substring(0, domainName.length() - 1);
        }
        index = currentIndex; // Update index now that we have measured how long the name it
        return domainName;
    }


    private static ResourceRecord decodeResponseRecords(ByteBuffer response){
        ResourceRecord resourceRecord = null;
        String domainName = domainNameCompression(response, index);
        // Type (2 bytes)
        // Class (2 bytes)
        // TTL (4 bytes), if 0 do not cache
        // RDLENGTH (2bytes)
        byte TYPEPart1 = response.get(index++);
        byte TYPEPart2 = response.get(index++);
        byte CLASSPart1 = response.get(index++);
        byte CLASSPart2 = response.get(index++);
        byte TTLPart1 = response.get(index++);
        byte TTLPart2 = response.get(index++);
        byte TTLPart3 = response.get(index++);
        byte TTLPart4 = response.get(index++);
        byte RDLENGTHPart1 = response.get(index++);
        byte RDLENGTHPart2 = response.get(index++);

        int TYPE = ((TYPEPart1 & 0xff) << 8) | (TYPEPart2 & 0xff);
        int CLASS = ((CLASSPart1 & 0xff) << 8) | (CLASSPart2 & 0xff);
        int TTL = ((0xFF & TTLPart1) << 24) | ((0xFF & TTLPart2) << 16) | ((0xFF & TTLPart3) << 8)
                | (0xFF & TTLPart4);
        int RDLENGTH = ((RDLENGTHPart1 & 0xff) << 8) | (RDLENGTHPart2 & 0xff);

        // TYPE: 28 = IPv6

        if (TYPE == 1){ // IPv4
            String address = "";
            for (int i = 0; i < RDLENGTH; i++){
                int addressPart = response.get(index) & 0xff;
                index++;
                address += addressPart + ".";

            }
            address = address.substring(0, address.length() - 1);
            resourceRecord = new ResourceRecord(domainName, RecordType.getByCode(TYPE), TTL, address);
            verbosePrintResourceRecord(resourceRecord, TYPE);
        }

        else if (TYPE == 28){ // *fixed* Bug, not putting together properly. 2620:10a:8053:0:0:0:0:2 turns to 26:20:10:a:80:53:...etc
            String address = "";
            for (int j = 0; j < RDLENGTH / 2; j++){ // RData length has to be divided by two as the IPV6 address is only half as long, as two bytes together form one part
                int addressPartSection1 = response.get(index) & 0xff;
                index++;
                int addressPartSection2 = response.get(index) & 0xff;
                index++;
                int addressPart = ((addressPartSection1 & 0xff) << 8) | (addressPartSection2 & 0xff);
                String IPv6AddressPart = Integer.toHexString(addressPart);
                address += IPv6AddressPart + ":";
            }
            address = address.substring(0, address.length() - 1);
            resourceRecord = new ResourceRecord(domainName, RecordType.getByCode(TYPE), TTL, address);
            verbosePrintResourceRecord(resourceRecord, TYPE);

        }

        else {
            String address = domainNameCompression(response, index);
            resourceRecord = new ResourceRecord(domainName, RecordType.getByCode(TYPE), TTL, address);
            verbosePrintResourceRecord(resourceRecord, TYPE);
        }
        cache.addResult(resourceRecord);
        return resourceRecord;
    }


    public static void decodeResponse(int queryID, byte[] response) {
        ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
        responseBuffer = ByteBuffer.wrap(response); // Turn the byte[] to ByteBuffer for easier parsing
        ResourceRecord resourceRecord = null;

        // Response Header
        byte responseIDPart1 = responseBuffer.get(0);
        byte responseIDPart2 = responseBuffer.get(1);
        int responseID = ((responseIDPart2 & 0xff) << 8) | (responseIDPart1 & 0xff);
        if ((responseID == queryID)) {
            byte QROPCODEAATCRD = responseBuffer.get(2); // OPCODE not used for responses
            int QR = getBit(QROPCODEAATCRD, 1);
            byte RAZRCODE = responseBuffer.get(3);
            byte QDCOUNTPart1 = responseBuffer.get(4);
            byte QDCOUNTPart2 = responseBuffer.get(5);
            int QCOUNT = ((QDCOUNTPart1 & 0xff) << 8) | (QDCOUNTPart2 & 0xff);
            byte ANCOUNTPart1 = responseBuffer.get(6);
            byte ANCOUNTPart2 = responseBuffer.get(7);
            int ANCOUNT = ((ANCOUNTPart1 & 0xff) << 8) | (ANCOUNTPart2 & 0xff);
            byte NSCOUNTPart1 = responseBuffer.get(8);
            byte NSCOUNTPart2 = responseBuffer.get(9);
            int NSCOUNT = ((NSCOUNTPart1 & 0xff) << 8) | (NSCOUNTPart2 & 0xff);
            byte ARCOUNTPart1 = responseBuffer.get(10);
            byte ARCOUNTPart2 = responseBuffer.get(11);
            int ARCOUNT = ((ARCOUNTPart1 & 0xff) << 8) | (ARCOUNTPart2 & 0xff);

            int AA = getBit(QROPCODEAATCRD, 6);

            if (QR == 1) { // QR should be 1 for a response
                if (verboseTracing) {
                    // "Response ID:", space. ID of response, space, "Authoritative", space, "=", " ", then "true"
                    // or "false" indicating if the response is authoritative
                    // response ID is the exact same as the queryID right?
                    System.out.println("Response ID" + " " + queryID + " " + "Authoritative" + " " + "=" + " " + (AA == 1));
                }

                int RCODEPart1 = getBit(RAZRCODE, 5);
                int RCODEPart2 = getBit(RAZRCODE, 6);
                int RCODEPart3 = getBit(RAZRCODE, 7);
                int RCODEPart4 = getBit(RAZRCODE, 8);
                int RCODE = ((0xFF & RCODEPart1) << 24) | ((0xFF & RCODEPart2) << 16) | ((0xFF & RCODEPart3) << 8)
                        | (0xFF & RCODEPart4);
                // Should I print out the meaning of the response code? Yes
                String errorMessage = "";
                switch (RCODE){
                    case 0: // Don't print anything if there's no error
                        break;
                    case 1: errorMessage = "Format error";
                        break;
                    case 2: errorMessage = "Server failure";
                        break;
                    case 3: errorMessage = "Name Error";
                        break;
                    case 4: errorMessage = "Not Implemented";
                        break;
                    case 5: errorMessage = "Refused";
                        break;
                }
                System.out.println(errorMessage);

                // Need to read the question in the response to figure out how it's length
                int currentIndex = 12;
                String QNAME = "";
                while (responseBuffer.get(currentIndex) != (byte) 0) { // 0 byte indicates end of QNAME
                    int length = (responseBuffer.get(currentIndex));
                    currentIndex++;
                    for (int i = 0; i < length; i++){
                        byte QNAMEPart = responseBuffer.get(i);
                        QNAME += (char) (QNAMEPart & 0xff);
                        currentIndex++;
                    }
                }
                currentIndex++; // To pass over the 0 byte that signifies the end of the QNAME
                currentIndex += 4; //QTYPE and QCLASS are 4 bytes in total
                index = currentIndex;


                // Should I even bother with verbose tracing? its always going to be false isn't it not? <- It can be turned on by the command "trace on"
                ArrayList<ResourceRecord> answers = new ArrayList<>();
                if (verboseTracing) {
                    // 2 spaces, "Answers", space, # of response records in answers <- put in parenthesis
                    System.out.println("  " + "Answers" + " " + "(" + ANCOUNT + ")"); // Need # of response records
                }
                // Print response record(s)
                for (int i = 0; i < ANCOUNT; i++) {
                    resourceRecord = decodeResponseRecords(responseBuffer);
                    if (resourceRecord != null){
                        answers.add(resourceRecord);
                    }
                }

                // Repeat above for "Nameservers" and "Additional Information" respectively
                ArrayList<ResourceRecord> nameServers = new ArrayList<>();
                if (verboseTracing) {
                    System.out.println("  " + "Nameservers" + " " + "(" + NSCOUNT + ")");
                }
                // Print response record(s)
                for (int i = 0; i < NSCOUNT; i++) {
                    resourceRecord = decodeResponseRecords(responseBuffer);
                    if (resourceRecord != null){
                        nameServers.add(resourceRecord);
                    }
                }
                ArrayList<ResourceRecord> additionalInformations = new ArrayList<>();
                if (verboseTracing) {
                    System.out.println("  " + "Additional Information" + " " + "(" + ARCOUNT + ")");

                    // Print response record(s)
                    for (int i = 0; i < ARCOUNT; i++) {
                        resourceRecord = decodeResponseRecords(responseBuffer);
                        if (resourceRecord != null){
                            additionalInformations.add(resourceRecord);
                        }
                    }
                }
                if (AA != 1 && RCODE == 0) { // Not authoritative but valid
                    ArrayList<ResourceRecord> authoritiveNameServers = new ArrayList<>();
                    for (ResourceRecord nameServer : nameServers) {
                        String name = nameServer.getTextResult();
                        for (ResourceRecord additionalInformation : additionalInformations) {
                            if (additionalInformation.getHostName().equals(name) && additionalInformation.getType().getCode() == 1) {
                                authoritiveNameServers.add(additionalInformation);
                            }
                        }
                    }
                    if (authoritiveNameServers.isEmpty()) {
                        for (ResourceRecord nameServer : nameServers) {
                            String name = nameServer.getTextResult();
                            DNSNode nameServerNode = new DNSNode(name, RecordType.getByCode(1));
                            Set<ResourceRecord> results = getResults(nameServerNode, 0);
                            if (!results.isEmpty()) {
                                authoritiveNameServers.addAll(results);
                                break; // End Loop
                            }
                        }
                    }
                    if (!authoritiveNameServers.isEmpty()) {
                        String IPAdress = (authoritiveNameServers).get(0).getTextResult();
                        try {
                            NameServer = InetAddress.getByName(IPAdress);
                        } catch (UnknownHostException e) {
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves DNS results from a specified DNS server. Queries are sent in iterative mode,
     * and the query is repeated with a new server if the provided one is non-authoritative.
     * Results are stored in the cache.
     *
     * @param node   Host name and record type to be used for the query.
     * @param server Address of the server to be used for the query.
     */
    private static void retrieveResultsFromServer(DNSNode node, InetAddress server) {

        // TODO To be completed by the student

        // Start query with 2 blank lines, print phrase "Query ID", then 5 spaces, then query ID, a space, the name
        // being looked up, two spaces then, query type, space, "-->", space, then IP address of the queried DNS server
        int queryID = random.nextInt(70000); // Generate the Query ID, is there any worry of two IDs being equal?
        if (verboseTracing) {
            System.out.println(); // First Blank line
            System.out.println(); // Second blank line
            System.out.println("Query ID" + "     " + queryID + " " + node.getHostName() + "  " + node.getType() + " " + "-->" + " " + server.getHostAddress());
        }

        // Implement sending and receiving packets
        // Need to encoode query in helper function
        byte[] buf = encodeQuery(queryID, node);
        DatagramPacket queryPacket = new DatagramPacket(buf, buf.length, server, DEFAULT_DNS_PORT);
        try {
            socket.send(queryPacket); // Make it try twice
        }
        catch (IOException e){
            return;
        }

        byte[] response = new byte[1024]; // Response packet max size = 1024 bytes, turn this to ByteBuffer for parsing
        DatagramPacket responsePacket = new DatagramPacket(response, response.length);
        try {
            socket.receive(responsePacket); // Make it try twice
            decodeResponse(queryID, response);

        }
        catch (SocketTimeoutException e){
            return;
        }
        catch (IOException e){
            return;
        }

    }

    private static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
        if (verboseTracing)
            System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(),
                    record.getTTL(),
                    record.getType() == RecordType.OTHER ? rtype : record.getType(),

                    record.getTextResult());
    }

    /**
     * Prints the result of a DNS query.
     *
     * @param node    Host name and record type used for the query.
     * @param results Set of results to be printed for the node.
     */
    private static void printResults(DNSNode node, Set<ResourceRecord> results) {
        if (results.isEmpty())
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        for (ResourceRecord record : results) {
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), record.getTTL(), record.getTextResult());
        }
    }
}