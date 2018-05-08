package logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.Tag;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;

//service methods/functions
public class Helper {

    /**
     * Reads server's response as text
     *
     * @param is - InputStream (from the HTTPS connection)
     * @return Response as a string
     * @throws IOException Input stream error
     */
    public static String getResponseAsString(InputStream is) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (IOException e) {
            System.err.println("ERROR while reading Paperless response as string.");
        }
        return response.toString();
    }

    /**
     * verifies the hostname which is on the SSL certificate
     *
     * @param expectedHost - which host we expect to be on the SSL certificate
     * @param sslSession -
     * @return Response as a string
     * @throws IOException Input stream error
     */
    public static Boolean verifyHostname(String expectedHost, SSLSession sslSession) {
        if (sslSession.getPeerHost().toString() == expectedHost) return true;
        else return false;
    }

    /**
     * Checks local filesystem default location for existence of a dump directory.
     * Appends a number of an attempt to the folder's name if the latter does exist
     *
     * @param dir name
     * @return modified dir name or the same one
     */
    public static Function<String, String> getAvailableDirName = (dir) -> {
        dir = new String(dir + DateFormat.getDateInstance(DateFormat.SHORT)
                .format(Calendar.getInstance().getTime()).toString());
        int attemptCounter = 1;
        while (Files.exists(Paths.get(dir))) {
            attemptCounter++;
            dir = dir.concat("(" + attemptCounter + ")");
        }
        try {
            Files.createDirectory(Paths.get(dir));
        } catch (Exception e) {
            System.err.println("ERROR creating a new directory \'" + dir + "\'.");
        }
        return dir;
    };

    /**
     * Fixes the filename of the PDF file received from the paperless server not to make
     * it create embedded folders in unix systems
     *
     * @param filename
     * @return fixed filename
     */
    public static Function<String, String> fixFileNameRegex = (filename) -> {
        filename = filename.replaceAll("[\\p{Cc}\\\\/*\\[\\]\\|]", "");
        return filename;
    };


    /**
     * A function that parses json into a Map
     *
     * @param json
     * @return Map like incoming json
     * @throws IOException Error during json parsing
     */
    public static Function<String, Map<String, Object>> parseJsonToMap = (json) -> {
        try {
            return new ObjectMapper().readValue(json, HashMap.class);
        } catch (IOException e) {
            System.err.println("Parsing json to Map failed. Returning null.\n" + e.getMessage());
        }
        return null;
    };


    /**
     * Chooses a value of a 'description' field from the filled-in json
     *
     * @param mapped
     * @return 'description' field value
     */
    public static Function<Map<String, Object>, Map<String, Object>> getDescriptionFromJson
            = mapped -> (Map<String, Object>) mapped.get("description");

    /**
     * Returns a list of tags from the 'description' field
     *
     * @param description
     * @return
     */
    public static Function<Map<String, Object>, List<Tag>> getTagsFromDescription = (description) -> {
        List<Tag> res = new ArrayList<>();

        for (Map tag : (List<Map>) description.get("tagList")) {
            res.add(new Tag((int) tag.get("id"), (String) tag.get("text"), (int) tag.get("color")));
        }

        return res;
    };

    /**
     * Fixes JSON from Paperless
     * will be redundant once they fix their JSON
     *
     * @param json
     * @return fixed json
     */
    public static Function<String, String> fixPaperlessJson = json -> {
        String[] parts = json.split("\\B(?=\"friends\")");

        return parts[0].replaceAll("(\\\\+)", "").replaceAll("(\"(?=\\{|\\[))|((?<=\\}|\\])\")", "")
                + (parts.length > 1 ? parts[1] : "");
    };

}
