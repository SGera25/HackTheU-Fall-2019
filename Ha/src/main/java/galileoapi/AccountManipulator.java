package galileoapi;


import okhttp3.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Random;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class AccountManipulator {
	public static final String ACCOUNT_NUM_NODE = "pmt_ref_no";
	public static final String BALANCE_NODE = "balance";
	public static final String TRANSACTION_HISTORY_NODE = "details_formatted";
	public static final String TRANSACTION_HISTORY_AMOUNT_NODE = "amount";
	public static final String TRANSACTION_HISTORY_TIME_NODE = "timestamp";
    private String serverAccessToken;
    private OkHttpClient client;
    public AccountManipulator() {
        this.client = new OkHttpClient();
    }

    /**
     * Grabs an accessToken for current client connection
     */
    public void authenticate() {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n    \"username\": \"GCzbbA-0139\",\n    \"password\": \"VzkYQwwtmoQHwBwB\"\n}");
        Request request = new Request.Builder()
                .url("https://sandbox.galileo-ft.com/intserv/4.0/login")
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            serverAccessToken = response.body().string();
            this.serverAccessToken = parseToken(serverAccessToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * sends create account request to Galileo API 
     * @param first
     * @param last
     * @param prodID
     * @return accountNumber
     */
    public String createAccount(String first, String last, int prodID){
    	authenticate();
    	String xmlData = "";
        authenticate();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n    \"transactionId\": \""+generateTransactionId() + "\",\n    \"prodId\": \""+prodID+"\",\n    \"firstName\": \""+first+"\",\n    \"lastName\": \""+last+"\"\n}");
        Request request = new Request.Builder()
                .url("https://sandbox.galileo-ft.com/intserv/4.0/createAccount")
                .post(body)
                .addHeader("Authorization", "Bearer " + this.getToken())
                .build();

        try {
            Response response = client.newCall(request).execute();
            xmlData = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parseXML(xmlData,ACCOUNT_NUM_NODE);
    }

    /**
     * Transfer between accounts through Galileo API 
     * @param amount
     * @param senderAccountNumber
     * @param recieverAccountNumber
     */
    public void moneyTransfer(String amount, String senderAccount, String recieverAccount){
        authenticate();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n\t\"transactionId\": \""+generateTransactionId() + "\",\n\t\"accountNo\": \""+ senderAccount+"\",\n\t\"amount\": "+amount+",\n\t\"transferToAccountNo\": \""+recieverAccount+"\"\n}");
        Request request = new Request.Builder()
                .url("https://sandbox.galileo-ft.com/intserv/4.0/createAccountTransfer")
                .post(body)
                .addHeader("Authorization", "Bearer " + this.getToken())
                .build();

        try {
            Response response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Grabs current balance of virtual account through Galileo API 
     * @param accountNumber
     * @return balance
     */
    public String getBalance(String accountNumber){
        authenticate();
        String balance = "";
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n    \"transactionId\": \""+generateTransactionId() +"\",\n    \"accountNo\": \""+accountNumber+"\"\n}");
        Request request = new Request.Builder()
                .url("https://sandbox.galileo-ft.com/intserv/4.0/getBalance")
                .post(body)
                .addHeader("Authorization", "Bearer " + this.getToken())
                .build();

        try {
            Response response = client.newCall(request).execute();
            balance = response.body().string();
         //   System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parseXML(balance, BALANCE_NODE);
    }

    /**
     * Simulate Charges from an account via GalileoAPI
     * @param accountNumber
     * @param amount
     * @param merchantName
     */
    public void simulateCharge(String accountNumber,String amount, String merchantName){
        authenticate();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n\t\"transactionId\": \""+generateTransactionId()+"\",\n\t\"accountNo\": \""+accountNumber+"\",\n\t\"amount\": "+amount+",\n\t\"merchantName\": \""+merchantName+"\",\n\t\"association\": \"mc_auth\"\n}");
        Request request = new Request.Builder()
                .url("https://sandbox.galileo-ft.com/intserv/4.0/createSimulatedCardAuth")
                .post(body)
                .addHeader("Authorization", "Bearer " + this.getToken())
                .build();
        try {
            Response response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Views current complete transaction History tied to Galileo virtual account 
     * @param accountNumber
     * @return transactionHistory (String of complete transaction history on this account)
     */
    public String viewTransactionHistory(String accountNumber){
    	authenticate();
    	String XMLhistory = "";
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n    \"transactionId\": \""+generateTransactionId() + "\",\n    \"accountNo\": \""+ accountNumber +"\"\n}");
        Request request = new Request.Builder()
                .url("https://sandbox.galileo-ft.com/intserv/4.0/getAuthHistory")
                .post(body)
                .addHeader("Authorization", "Bearer " + this.getToken())
                .build();

        try {
            Response response = client.newCall(request).execute();
            XMLhistory = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
       	DocumentBuilder builder = null;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
    	InputSource src = new InputSource();
    	src.setCharacterStream(new StringReader(XMLhistory));

    	Document doc = null;
		try {
			doc = builder.parse(src);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		NodeList listDetails = doc.getElementsByTagName(TRANSACTION_HISTORY_NODE);
		NodeList listAmounts = doc.getElementsByTagName(TRANSACTION_HISTORY_AMOUNT_NODE);
		NodeList listTimestamps = doc.getElementsByTagName(TRANSACTION_HISTORY_TIME_NODE);
		String transactionHistory = "";
		for(int i = 0; i < listDetails.getLength(); i++) {
			transactionHistory += listDetails.item(i).getTextContent() + "\n" +
								  listAmounts.item(i).getTextContent() + "\n" +
								  listTimestamps.item(i).getTextContent()   + "\n\n";
		}
        return transactionHistory; 
    }

    /**
     * Use to parse access token 
     * @param token
     * @return currentToken
     */
    private String parseToken(String token) {
        Scanner s = new Scanner(token);
        s.next();
        String currentToken = s.next();
        currentToken = currentToken.substring(1, currentToken.length()-2);
        s.close();
        return currentToken;

    }
    /**
     * Generates a random transaction for transactions on the Galileo API
     * @return id 
     */

    private String generateTransactionId() {
        String id = "";
        String elements = "abcdefghijklmnopqrstuvwxyz"+
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"+
                "1234567890";
        Random rand = new Random();
        for(int i = 0; i < 16; i++) {
            id = id + elements.charAt(rand.nextInt(elements.length()));
        }
        return id;
    }
    /**
     * Use to parse XML string for values at node 
     * @param xmlFile
     * @param node
     * @return parse
     */
    @SuppressWarnings("unused")
	private static String parseXML(String xmlFile, String node) {
    	DocumentBuilder builder = null;
    	String parse = "";
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
    	InputSource src = new InputSource();
    	src.setCharacterStream(new StringReader(xmlFile));

    	Document doc;
		try {
			doc = builder.parse(src);
			doc.getElementsByTagName(node);
	    	parse = doc.getElementsByTagName(node).item(0).getTextContent();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return parse; 
    }
    
    /**
     * returns current serverAccessToken
     * @return serverAccessToken
     */
    private String getToken() {
        return serverAccessToken;
    }
    public static void main(String args[]) {
    	AccountManipulator a = new AccountManipulator();
    	String accountNo = a.createAccount("Sam", "Ger", 9634);
    	System.out.println("Balance: " + a.getBalance(accountNo));
    	a.moneyTransfer("15", "283101000729", accountNo);
    	System.out.println("new balance: " + a.getBalance(accountNo));
    	a.simulateCharge(accountNo, "10", "Sam Campbells Pastries");
    	a.simulateCharge(accountNo, "5", "HackTheU");
    	System.out.println("new balance: " + a.getBalance(accountNo));
    	System.out.println("TransactionHistory");
    	System.out.println("==================");
    	System.out.println(a.viewTransactionHistory(accountNo));
    	
    }
}

