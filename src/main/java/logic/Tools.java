package logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import enums.PaperlessRestApiUrls;
import models.Tag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static logic.Helper.*;

public class Tools {

    /**
     * Uploads the PDF document to Paperless.
     * Json is ok here, no need to fix it
     *
     * @param name
     * @param file
     * @return id of the uploaded file, if OK, -1 if an error occured
     * @throws IOException
     */
    public static int uploadPDF(String name, byte[] file, Connector connector) throws IOException {
        //TODO
        return -1;
    }

    /**
     * Shares the doc to multiple accounts/emails
     *
     * @param id
     * @param comment
     * @param mails
     * @return true if OK, false if didn't share
     * @throws IOException for connectivity problems
     */
    public static boolean shareDocument(int id, String comment, Set<String> mails, Connector connector) throws IOException {
        //TODO
        return false;
    }

    /**
     * Shares the doc to a single account
     *
     * @param id
     * @param comment
     * @param mail
     * @return true if everything is OK, false if didn't share
     * @throws IOException for connectivity problems
     */
    public static boolean shareDocument(int id, String comment, String mail, Connector connector) throws IOException {
        //TODO
        return false;
    }

    /**
     * Creates a new tag for the current Paperless user account
     *
     * @param name new tag's name
     * @return id of the new tag if everything is OK; id if the tag is already exists; -1 if something went wrong
     * @throws IOException for connectivity problems
     */
    public static int addNewTag(String name, Connector connector) throws IOException {
        //TODO
        return -1;
    }

    /**
     * Attaches a tag to a doc
     *
     * @param docId document identifier
     * @param tagId tag identifier
     * @return true - once has attached, false - if the tag was attached already
     * @throws IOException for connectivity problems
     */
    public static boolean assignTagToDoc(int docId, int tagId, Connector connector) throws IOException {
       //TODO
       return false;
    }

    /**
     * Looks up for a list of available tags
     * @param connector - Connector is a session-serving class
     *
     * @return a list of tags available for this user
     * @throws IOException for connectivity problems
     */
    public static List<Tag> getListOfAvailableTags(Connector connector) throws IOException {

        URL loginURL = new URL(PaperlessRestApiUrls.HOST.toString()
                + PaperlessRestApiUrls.LOGIN);

        HttpsURLConnection conlog = (HttpsURLConnection) loginURL.openConnection();

        conlog.setHostnameVerifier(Helper::verifyHostname);
        conlog.setConnectTimeout(connector.getConnectionTimeout());
        conlog.setRequestMethod("GET");
        conlog.setRequestProperty("content-length", "0");
        conlog.setRequestProperty("sessionid", connector.getSessionId());

        List<Tag> tagslist = getTagsFromDescription.apply(
                getDescriptionFromJson.apply(
                        parseJsonToMap.apply(
                                fixPaperlessJson.apply(
                                        getResponseAsString(conlog.getInputStream())))));

        conlog.disconnect();

        for (Tag tag : tagslist) System.out.println("Tag available: " + tag.getText());

        return  tagslist;
    }

    //According to the Paperless client API, they use bit positions instead of integer numbers for Tag IDs.
    // I.e. 0x0001 is the first tag, 0x0002 is the second tag, 0x0004 is the third tag, e.t.c.
    /**
     * Maps folder names to be created in the local filesystem to bitmasks, represented by Integers
     *
     * @return a map of tag folder names mapped by bitmasks as Integers
     * @throws IOException
     */
    private static Map<Integer, String> mapTagBitsToFolders(List<String> folderNames) throws IOException {
        Map<Integer, String> tagToFolderMapper = new HashMap<>();
        tagToFolderMapper.put(0x0000, "ALL");
        if (folderNames.size() > 0) {
            int bits = 0x0001;
            for (String folderName : folderNames) {
                tagToFolderMapper.put(bits, folderName);
                bits = bits << 1;
            }
        }
        return tagToFolderMapper;
    }


    /**
     * Creates directories for tags in the local filesystem
     *
     * @param path where to create those folders
     * @param tagslist a list of Tag objects
     * @throws IOException for filesystem permissions problems
     * @return a list of names of created folders
     */
    private static List<String> makeDirectoriesForTags (String path, List <Tag> tagslist){
        List<String> createdFolders = new ArrayList<>();
        try (PrintWriter tl = new PrintWriter(new File(path + "/tagsInfo"))) {
            for (Tag tag : tagslist) {
                Files.createDirectory(Paths.get(path + "/" + tag.getText())); //creating directories one per tag
                createdFolders.add(tag.getText());
                tl.println(tag);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        return createdFolders;
    }


    /**
     * Backs-up all the files from paperless server user's folder to the local dir.
     * Files are being placed in folders by their status: uploaded, in thrash, or deleted
     *
     * @param path to the local folder where to store the dump
     * @throws IOException for connectivity problems
     */
    public static void backupSignedDocsToLocalDirectory(String path, Connector connector) throws IOException, InterruptedException {

        URL searchURL = new URL(PaperlessRestApiUrls.HOST.toString()
                + PaperlessRestApiUrls.RESOURCE + "search");

        HttpsURLConnection con = (HttpsURLConnection) searchURL.openConnection();

        con.setHostnameVerifier(Helper::verifyHostname);
        con.setConnectTimeout(connector.getConnectionTimeout());
        con.setRequestMethod("POST");
        con.setRequestProperty("sessionid", connector.getSessionId());

        String content = "{}"; //Filters for documents to download

        con.setRequestProperty("content-length", 2 + "");
        con.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(content); //will be flushed since FilterOutputStream does flush() on close()
        } catch (IOException e) {
            System.err.println("Failed to send data to Paperless via connection to " + searchURL.toString());
        }

        String res = getResponseAsString(con.getInputStream());

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> l = mapper.readValue(res, ArrayList.class);

        int norm = 0, tr = 0, del = 0;

        File docInfo = new File(path + "/docInfo.txt");

        Map<Integer, String> mappedBitsToFolders =
                mapTagBitsToFolders(makeDirectoriesForTags(path, getListOfAvailableTags(connector)));

        for (Map<String, Object> json : l) {

            Integer id = (Integer) json.get("id");
            String src = (String) json.get("src");
            String hash = (String) json.get("hash");
            Integer tags = (Integer) json.get("tags");//i.e. tags==0x0005 means that the first
                               //and the third tags are applied to this doc.
            //Go and ask paperless.com.ua why they did it, on your own. If you do want to.
            Integer status = (Integer) json.get("status");
            String name = fixFileNameRegex.apply((String) json.get("name"));
            List<String> listOfTagFoldersWhereToPutTheDoc = new ArrayList<>();

            for(Map.Entry<Integer, String> theFolder : mappedBitsToFolders.entrySet()) {
                if ((tags & theFolder.getKey()) == theFolder.getKey()) {
                    //the bitmask helps to determine if the bit-tag of the document
                    //is in the document's 'tags' Integer
                    listOfTagFoldersWhereToPutTheDoc.add(theFolder.getValue());
                }
            }

            if (status / 10 == 0) norm++;
            if (status / 10 == 1) tr++;
            if (status / 10 == 2) del++;

            try (PrintWriter dl = new PrintWriter(new BufferedWriter(new FileWriter(docInfo, true)))) {

                System.out.println(src + "|" + hash + "|" + tags);
                dl.println(id + "|" + src + "|" + hash + "|" + status + "|" + tags + "|" + name);

                URL cdnURL = new URL(PaperlessRestApiUrls.HOST.toString()
                        + PaperlessRestApiUrls.WITHSIGN + id); //signed docs only

                HttpsURLConnection cdncon = (HttpsURLConnection) cdnURL.openConnection();

                cdncon.setHostnameVerifier(Helper::verifyHostname);
                cdncon.setConnectTimeout(connector.getConnectionTimeout());
                cdncon.setRequestMethod("GET");
                cdncon.setRequestProperty("sessionid", connector.getSessionId());

                byte[] fi = IOUtils.toByteArray(cdncon.getInputStream());

                for(String folder : listOfTagFoldersWhereToPutTheDoc)
                    FileUtils.writeByteArrayToFile
                            (new File(path + (status / 10 == 0 ? "/" + folder +"/docs/" : "/" + folder + "/trash/") + id + "_" + name + ".pdf"), fi);
            } catch (MalformedURLException e) {
                System.err.println(e);
            } catch (IOException e) {
                System.err.println(e);
            }

            //According to the Paperless client API, if requests are being sent with
            //frequency of more than 10/sec, all incoming requests may be
            //blocked for an arbitrary period of time.
            TimeUnit.MILLISECONDS.sleep(100);
        }
        System.out.println("norm: " + norm + "; trash: " + tr + "; deleted: " + del);

    }
}
