package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.exception.DictConnectionException;
import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;
import ca.ubc.cs317.dict.util.DictStringParser;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private Map<String, Database> databaseMap = new LinkedHashMap<String, Database>();

    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        // TODO Add your code here
        try {
            this.socket = new Socket(host, port);
            this.input = new BufferedReader (new InputStreamReader(socket.getInputStream()));
            this.output = new PrintWriter (socket.getOutputStream(), true);
            String welcomeMsg = this.input.readLine(); //welcome message
            if (!welcomeMsg.startsWith("220")){ //code 220 = allowed to connect
                if (welcomeMsg.startsWith("530")){ // code 530 = access denied
                    System.out.println("530 Access Denied");
                }
                if (welcomeMsg.startsWith("420")){ // code 420 = server temporarily unavailable
                    System.out.println("420 Server Temporarily Unavailable");
                }
                if (welcomeMsg.startsWith("421")){ // code 421 = server shut down
                    System.out.println("421 Server Shutting Down At Operator Request");
                }
                throw new DictConnectionException();
            }
            else {
                System.out.println("Connection Established");
            }
        }
        catch (IOException ex){
        System.out.println (ex.toString());
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {
        // TODO Add your code here
        try {
            String command = "quit";
            output.write(command);
            this.input.close();
            this.output.close();
            this.socket.close();
            System.out.println("Connection Closed");
        }
        catch (IOException ex){
            System.out.println (ex.toString());
        }
    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();
        getDatabaseList(); // Ensure the list of databases has been populated
        // TODO Add your code here
        try {
            this.output.println("Define " + database.getName() + " " + word); //(Command: Define database word)
            String definitionLine = this.input.readLine();

            if (definitionLine.startsWith("150")){
                definitionLine = this.input.readLine();
                while (!definitionLine.startsWith("250")){ // 250 ok (optional timing information here)
                    String[] line = DictStringParser.splitAtoms(definitionLine);
                    String defWord = line[1];
                    String dataName = line[2];
                    String dataDesc = line[3];
                    Database currDatabase = new Database(dataName, dataDesc);
                    Definition definition = new Definition(defWord, currDatabase);
                    definitionLine = this.input.readLine();
                    while (!definitionLine.startsWith(".")){
                        definition.appendDefinition(definitionLine);
                        definitionLine = this.input.readLine();
                    }
                    set.add(definition);
                    definitionLine = this.input.readLine();
                }
            }
            if (definitionLine.startsWith("550")){ // 550 Invalid database, use "SHOW DB" for list of databases
                System.out.println("550 Invalid Database");
                throw new DictConnectionException();
            }
            if (definitionLine.startsWith("552")){ // 552 No match
                System.out.println("552 No Match");
                throw new DictConnectionException();
            }
        }
        catch (IOException ex){
            System.out.println (ex.toString());
        }

        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {

        Set<String> set = new LinkedHashSet<>(); // no quotes
        // TODO Add your code here
        try {
            this.output.println("MATCH " + database.getName() + " " + strategy.getName() + " " + word);
            String matchingLine = this.input.readLine();
            if (matchingLine.startsWith("152")) { // 152 n matches found - text follows
                matchingLine = this.input.readLine();
                while (!matchingLine.equals(".")) {
                    String[] line = matchingLine.split(" ", 2);
                    set.add(line[1].replace("\"", ""));
                    matchingLine = this.input.readLine();
                }
            }
            if (matchingLine.startsWith("550")){ // 550 Invalid database, use "SHOW DB" for list of databases
                System.out.println("Invalid Database");
                throw new DictConnectionException();
            }
            if (matchingLine.startsWith("551")){ // 551 Invalid strategy, use "SHOW STRAT" for a list of strategies
                System.out.println("Invalid Strategy");
                throw new DictConnectionException();
            }
            if (matchingLine.startsWith("552")){ // 552 No match
                System.out.println("No Match");
                throw new DictConnectionException();
            }
           this.input.readLine(); // Flush 250 ok (optional timing information here)
       }
        catch (IOException ex){
            System.out.println (ex.toString());
        }
        return set;
    }

    /** Requests and retrieves a list of all valid databases used in the server. In addition to returning the list, this
     * method also updates the local databaseMap field, which contains a mapping from database name to Database object,
     * to be used by other methods (e.g., getDefinitionMap) to return a Database object based on the name.
     *
     * @return A collection of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Database> getDatabaseList() throws DictConnectionException {
        if (!databaseMap.isEmpty()) return databaseMap.values();
        // TODO Add your code here
        try {
            String command = "show DB";
            this.output.println(command);
            String currentLine = this.input.readLine(); // Status Code
            if (currentLine.startsWith("110")) { // 110 n databases present - text follows
                currentLine = this.input.readLine();
                while (!currentLine.equals(".")) {
                    String[] line = currentLine.split(" ", 2);
                    Database database = new Database(line[0].replace("\"", ""), line[1].replace("\"", ""));
                    databaseMap.put(line[0], database);
                    currentLine = this.input.readLine();
                }
            }
            if (currentLine.startsWith("554")){ // 554 No databases present
                System.out.println("554 No Database Present");
                throw new DictConnectionException();
            }
            this.input.readLine(); // flush end
        }
        catch (IOException ex){
            System.out.println (ex.toString());
        }
        return databaseMap.values();
    }


    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();
        // TODO Add your code here
        try {
            String command = "show STRAT";
            output.println(command);
            String currentLine = input.readLine(); // Status Line
            if (currentLine.startsWith("111")) { // 111 n strategies available - text follows
                currentLine = input.readLine();
                while (!currentLine.equals(".")) {
                    String[] line = currentLine.split(" ", 2);
                    MatchingStrategy strategy = new MatchingStrategy(line[0].replace("\"", ""), line[1].replace("\"", ""));
                    set.add(strategy);
                    currentLine = input.readLine();
                }
            }
            if (currentLine.startsWith("555")){ // 555 No strategies available
                System.out.println("555 No Strategies Present");
                throw new DictConnectionException();
            }
            input.readLine(); // flush end
        }
        catch (IOException ex){
            System.out.println (ex.toString());
        }
        return set;
    }

}
