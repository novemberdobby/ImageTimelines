package novemberdobby.teamcity.imageComp.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Util {
    public static Document getRESTdocument(String url, String user, String pass)
            throws IOException, ParserConfigurationException, SAXException {
        URLConnection connection = webRequest(url, user, pass);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(true);

        return builderFactory.newDocumentBuilder().parse(connection.getInputStream());
    }

    public static URLConnection webRequest(String url, String user, String pass) throws IOException {
        URL urlObj = new URL(url);

        String creds = String.format("%s:%s", user, pass);
        String authStr = String.format("Basic %s", DatatypeConverter.printBase64Binary(creds.getBytes()));
        
        URLConnection connection = urlObj.openConnection();
        connection.setRequestProperty("Authorization", authStr);
        return connection;
    }

    public static List<String> webRequestLines(String url, String user, String pass) throws IOException {
        URLConnection connection = webRequest(url, user, pass);

        List<String> result = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while(reader.ready()) {
                result.add(reader.readLine());
            }
        }

        return result;
    }

    public static void downloadFile(URLConnection connection, File saveToFile) throws IOException {
        try (
            InputStream inStream = connection.getInputStream(); //in from connection
            FileOutputStream outStream = new FileOutputStream(saveToFile); //out to file
        ) {
            int read;
            byte[] buffer = new byte[2048];
            while((read = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, read);
            }
        }
    }

    public static List<String> getCompareMetrics(Map<String, String> params) {
        String typesStr = params.get(Constants.FEATURE_SETTING_DIFF_METRIC);
        if(typesStr != null) {
            return Arrays.asList(typesStr.toLowerCase().split(",", 0));
        }

        return Collections.emptyList();
    }

	public static String EscapeServiceMessageText(String input) {
        return input
        .replace("|", "||")
        .replace("\r", "|r")
        .replace("\n", "|n")
        .replace("'", "|'")
        .replace("[", "|[")
        .replace("]", "|]");
	}

	public static boolean writeStrToFile(File file, String str) {
        FileWriter fWriter = null;

        try {
            fWriter = new FileWriter(file, false);
            fWriter.write(str);
            return true;
        } catch (Exception e) {

        } finally {
            try {
                if(fWriter != null) {
                    fWriter.close();
                }
            } catch (Exception e) {
                
            }
        }

        return false;
	}
}