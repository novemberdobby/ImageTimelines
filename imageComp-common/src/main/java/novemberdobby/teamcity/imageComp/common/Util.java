package novemberdobby.teamcity.imageComp.common;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

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
}