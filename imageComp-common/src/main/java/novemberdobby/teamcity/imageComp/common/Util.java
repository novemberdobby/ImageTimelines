package novemberdobby.teamcity.imageComp.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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

    public static List<String> getCompareTypes(Map<String, String> params) {
        String typesStr = params.get(Constants.FEATURE_SETTING_DIFF_METRIC);
        if(typesStr != null) {
            return Arrays.asList(typesStr.toUpperCase().split(",", 0));
        }

        return Collections.emptyList();
    }
}