import enums.Banners;
import logic.Connector;
import logic.Helper;
import logic.Tools;

import java.io.*;


public class ApplicationRunner {

    private static String destinationDir;
    private static String login = "";
    private static String password = "";
    private static int otp = 0;
    private static final int connectionTimeout = 10000;

    public static void main(String[] args) throws IOException {

        System.out.println(Banners.PAPERLESS_UTILITY_BANNER); //mmmeeehhhh

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Your Paperless.com.ua login:");
            login = login.concat(br.readLine());
            System.out.print("Your Paperless.com.ua password:");
            password = password.concat(br.readLine());
            System.out.print("Google Authentication code for Paperless.com.ua from your mobile app (otherwise type '0'):");
            otp = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            System.err.println("I/O Error");
        }

        Connector connector = Connector.getInstance();
        connector.init(login, password, otp, connectionTimeout);

        //TODO menu for more functions of the Paperless RESTful API

        System.out.println("Connection is open: " + connector.open() + ";");
        System.out.println("Session is created successfully:"
                + connector.checkConnectionAndSession() + ";");

        try {
            Tools.backupSignedDocsToLocalDirectory(Helper.getAvailableDirName
                    .apply(destinationDir = "Paperless_Dump_"), connector);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Request to close the connection sent: " + connector.close() + ";");
        System.out.println("Session killed: " + connector.isSessionEnded() + ";");
    }


}
