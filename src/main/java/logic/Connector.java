package logic;

import enums.PaperlessRestApiUrls;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;

import static logic.Helper.*;

//a session-managing class
public class Connector {

    private static volatile Connector instance;

    private String login;
    private String password;
    private int otp; //Google Authenticator, if enabled. //TO DO
    private int connectionTimeout;
    private String sessionId; //sessionId: after a successful authentication this session key will be
    //valid for 24 hours and should be present in every subsequent request

    //A singleton with lazy init
    private Connector() {
        this.login = null;
        this.password = null;
        this.otp = 0;
        this.connectionTimeout = 0;
        this.sessionId = null;
    }

    public void init(String login, String password, int otp, int connectionTimeout) {
        this.login = login;
        this.password = password;
        this.otp = otp;
        this.connectionTimeout = connectionTimeout;
    }

    //singleton type: Double Checked Locking & volatile
    public static Connector getInstance() {
        Connector localInstance = instance;
        if(localInstance == null) {
            synchronized (Connector.class) {
                localInstance = instance;
                if(localInstance == null) {
                    instance = localInstance = new Connector();
                }
            }
        }
        return localInstance;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Starts a session
     *
     * @throws IOException
     * @return sessionId for a connection error
     */
    public boolean open() {
        try {
            sessionId = login(this.login, this.password, this.otp);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Does login with user's credentials and gets a session id
     *
     * @param login
     * @param password
     * @return session id once successfully logged in, but null if got an error
     * @throws IOException if there was a connectivity error
     */
    private String login(String login, String password, int otp) throws IOException {

        URL loginURL = new URL(PaperlessRestApiUrls.HOST.toString()
                + PaperlessRestApiUrls.LOGIN);

        HttpsURLConnection con = (HttpsURLConnection) loginURL.openConnection();

        String content = "{\"login\" : \"" + login + "\", \"password\" : \"" + password + "\", \"otp\" : \"" + otp + "\"}";

        con.setHostnameVerifier(Helper::verifyHostname);
        con.setConnectTimeout(connectionTimeout);
        con.setRequestMethod("POST");
        con.setRequestProperty("content-length", Integer.toString(content.length()));
        con.setRequestProperty("v", "161031");
        con.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(content);
            wr.flush();
        } catch (IOException e) {
            System.err.println("Failed to send login and password data to Paperless. IOException.");
        }

        try {
            if (con.getResponseCode() == 200)
                return (String) parseJsonToMap.apply(
                        fixPaperlessJson.apply(
                                getResponseAsString(con.getInputStream()))).get("sessionId");
        } finally {
            con.disconnect();
        }
        return null;
    }

    /**
     * Checks the connection
     *
     * @return true if the connection is OK and the session is valid, false otherwise
     * @throws IOException
     */
    public boolean checkConnectionAndSession() {
        try {
            URL loginURL = new URL(PaperlessRestApiUrls.HOST.toString()
                    + PaperlessRestApiUrls.LOGIN);
            HttpsURLConnection con = (HttpsURLConnection) loginURL.openConnection();

            con.setHostnameVerifier(Helper::verifyHostname);
            con.setConnectTimeout(connectionTimeout);
            con.setRequestMethod("GET");
            con.setRequestProperty("content-length", "0");
            con.setRequestProperty("sessionid", sessionId);

            try {
                return con.getResponseCode() == 200;
            } catch (Exception e) {
                return false;
            } finally {
                con.disconnect();
            }
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * Checks if the session is finished
     *
     * @return If the session is finished - true, otherwise - false
     * @throws IOException for connectivity problems
     */
    public boolean isSessionEnded() throws IOException {
        URL loginURL = new URL(PaperlessRestApiUrls.HOST.toString()
                + PaperlessRestApiUrls.LOGIN);

        HttpsURLConnection con = (HttpsURLConnection) loginURL.openConnection();

        con.setHostnameVerifier(Helper::verifyHostname);
        con.setConnectTimeout(connectionTimeout);
        con.setRequestMethod("GET");
        con.setRequestProperty("content-length", "0");
        con.setRequestProperty("sessionid", sessionId);

        try {
            return con.getResponseCode() == 403;
        } finally {
            con.disconnect();
        }
    }

    /**
     * closes the session with Paperless
     *
     * @return
     * @throws IOException
     */
    public boolean close() throws IOException {
        URL loginURL = new URL(PaperlessRestApiUrls.HOST.toString()
                + PaperlessRestApiUrls.LOGIN);

        HttpsURLConnection con = (HttpsURLConnection) loginURL.openConnection();

        con.setHostnameVerifier(Helper::verifyHostname);
        con.setRequestMethod("DELETE");
        con.setRequestProperty("sessionid", sessionId);

        try {
            return con.getResponseCode() == 200;
        } finally {
            con.disconnect();
        }
    }

}
