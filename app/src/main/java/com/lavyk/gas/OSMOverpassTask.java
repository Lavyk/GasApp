package com.lavyk.gas;

import android.os.AsyncTask;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class OSMOverpassTask extends AsyncTask<String, Integer, Document> {
    private Exception exception;

    protected Document doInBackground(String... urls) {
        Document doc = null;
        try {
            String hostname = "http://www.overpass-api.de/api/interpreter";
            String queryString = "[out:\"json\"][timeout:\"30\"];"
                    + "("
                    + "     node[\"amenity\"=\"fuel\"](-7.232252,-35.903320,-7.205643,-35.845428);"
                    + "     way[\"amenity\"=\"fuel\"](-7.232252,-35.903320,-7.205643,-35.845428);"
                    + "     rel[\"amenity\"=\"fuel\"](-7.232252,-35.903320,-7.205643,-35.845428);"
                    + ");"
                    + "out body center qt 100;";

            URL osm = new URL(hostname);
            HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream printout = new DataOutputStream(connection.getOutputStream());
            printout.writeBytes("data=" + URLEncoder.encode(queryString, "utf-8"));
            printout.flush();
            printout.close();

            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            doc = docBuilder.parse(connection.getInputStream());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        }
        return doc;
    }

    protected void onPostExecute() {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}
