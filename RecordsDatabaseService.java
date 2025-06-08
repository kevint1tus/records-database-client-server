/*
 * RecordsDatabaseService.java
 *
 * The service threads for the records database server.
 * This class implements the database access service, i.e. opens a JDBC connection
 * to the database, makes and retrieves the query, and sends back the result.
 *
 * 
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
//import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.Socket;

import java.util.StringTokenizer;

import java.sql.*;
import javax.sql.rowset.*;
    //Direct import of the classes CachedRowSet and CachedRowSetImpl will fail becuase
    //these clasess are not exported by the module. Instead, one needs to impor
    //javax.sql.rowset.* as above.



public class RecordsDatabaseService extends Thread{

    private Socket serviceSocket = null;
    private String[] requestStr  = new String[2]; //One slot for artist's name and one for recordshop's name.
    private ResultSet outcome   = null;

    private CachedRowSet crs = null; // Global CachedRowSet object

	//JDBC connection
    private String USERNAME = Credentials.USERNAME;
    private String PASSWORD = Credentials.PASSWORD;
    private String URL      = Credentials.URL;



    //Class constructor
    public RecordsDatabaseService(Socket aSocket){
		//TODO (TO BE COMPLETED)
        serviceSocket = aSocket;
    }


    //Retrieve the request from the socket
    public String[] retrieveRequest()
    {
        this.requestStr[0] = ""; //For artist
        this.requestStr[1] = ""; //For recordshop

		String tmp = "";
        try {

            InputStream socketStream = this.serviceSocket.getInputStream();
            InputStreamReader socketReader = new InputStreamReader(socketStream);

            StringBuilder stringBuffer = new StringBuilder();
            int charAsInt;
            char ch;

            while ((charAsInt = socketReader.read()) != -1) {
                ch = (char) charAsInt;
                if (ch == '#') {
                    break;
                }
                stringBuffer.append(ch);
            }

            tmp = stringBuffer.toString();
            if (!tmp.isEmpty()) {
                String[] messageComponents = tmp.split(";");
                this.requestStr[0] = messageComponents.length > 0 ? messageComponents[0] : "";
                if (messageComponents.length > 1) {
                    this.requestStr[1] = messageComponents[1];
                }
            }

        }catch(Exception e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        return this.requestStr;
    }


    //Parse the request command and execute the query
    public boolean attendRequest()
    {
        boolean flagRequestAttended = true;
		
		this.outcome = null;

		String sql = "SELECT r.title, r.label, g.name AS genre, r.rrp, COUNT(rc.copyid) " +
                "FROM artist a, record r, genre g, recordcopy rc, recordshop rs " +
                "WHERE a.artistid = r.artistid AND r.genre = g.name AND r.recordid = rc.recordid AND rc.recordshopid = rs.recordshopid " +
                "AND a.lastname = ? AND rs.city = ? " +
                "GROUP BY r.title, r.label, g.name, r.rrp " +
                "HAVING COUNT(rc.copyid) > 0";

		try {
			//Connect to the database
            Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);

            //Make the query
            PreparedStatement prepstmt = conn.prepareStatement(sql);
            prepstmt.setString(1, this.requestStr[0]);
            prepstmt.setString(2, this.requestStr[1]);
            ResultSet rs = prepstmt.executeQuery();

            this.outcome = rs;
			
			//Process query
            RowSetFactory factory = RowSetProvider.newFactory();
            this.crs = factory.createCachedRowSet();
            this.crs.populate(rs);
            this.crs.beforeFirst();
            while (this.crs.next()) {
                String title = this.crs.getString("title");
                String label = this.crs.getString("label");
                String genre = this.crs.getString("genre");
                double rrp = this.crs.getDouble("rrp");
                int copies = this.crs.getInt("count");

                System.out.println(title + " | " +  label + " | " + genre + " | " + rrp + " | " + copies);
            }

			//Clean up
            conn.close();
			
		} catch (Exception e)
		{ System.out.println(e);flagRequestAttended = false;}


        return flagRequestAttended;
    }


    //Wrap and return service outcome
    public void returnServiceOutcome(){
        try {
			//Return outcome
            OutputStream outcomeStream = this.serviceSocket.getOutputStream();
            ObjectOutputStream outcomeStreamWriter = new ObjectOutputStream(outcomeStream);
            outcomeStreamWriter.writeObject(this.crs);
			
            System.out.println("Service thread " + this.getId() + ": Service outcome returned; " + this.crs);
            
			//Terminating connection of the service socket
            this.serviceSocket.close();
			
        }catch (IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
    }

    //The service thread run() method
    public void run()
    {
		try {
			System.out.println("\n============================================\n");
            //Retrieve the service request from the socket
            this.retrieveRequest();
            System.out.println("Service thread " + this.getId() + ": Request retrieved: "
						+ "artist->" + this.requestStr[0] + "; recordshop->" + this.requestStr[1]);
            //Attend the request
            boolean tmp = this.attendRequest();

            //Send back the outcome of the request
            if (!tmp)
                System.out.println("Service thread " + this.getId() + ": Unable to provide service.");
            this.returnServiceOutcome();

        }catch (Exception e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        //Terminate service thread (by exiting run() method)
        System.out.println("Service thread " + this.getId() + ": Finished service.");
    }

}
