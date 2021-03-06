import org.json.JSONException;
import org.json.JSONObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominik on 01.06.2015.
 *
 * Downloader Class is a abstract class to define the general downloader.
 * This class should be inherited by every sub-type of downloader
 */
public abstract class Downloader {
    private List<DownloadProgressListener> listener = new ArrayList<DownloadProgressListener>();

    // Public Methods

    /**
     * This method determines if a given file exists or not
     * @param f File to check
     * @return True if file exists; False if file doesn't exist.
     */
    public boolean isFileExisting(File f) {
        try {
            if (f.exists())
                return true;
            else
                return false;
        } catch (SecurityException ex){
            // maybe log the error here
            return true;
        }
    }

    /**
     * This method determines the download size in bytes for a specified URL/File
     * @param url URL as String to determine the download size
     * @return Returns the download size of a given url. (Returns -1 if an IOException occured!)
     */
    public int getDownloadSize(String url){
        try {
            URLConnection hUrl = new URL(url).openConnection();
            return hUrl.getContentLength();
        } catch(IOException e) {
            return -1;
        }
    }

    /**
     * This method normalizes a given String which represents a file path on any OS
     * @deprecated Better to use isDirectoryExisting()
     * @param pathToCheck String which needs to be checked
     * @return Normalized Path String
     */
    public String CheckSavePath(String pathToCheck){
        if (CGlobals.CURRENT_OS == OS.Windows) {
            if (!pathToCheck.endsWith("\\")) {
                pathToCheck = pathToCheck + "\\";
            }

            if (!Files.isDirectory(Paths.get(pathToCheck))) {
                try {
                    Files.createDirectory(Paths.get(pathToCheck));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return pathToCheck;
        } else if (CGlobals.CURRENT_OS == OS.Linux) {
            if (!pathToCheck.endsWith("/"))
                pathToCheck = pathToCheck + "/";

            if (!Files.isDirectory(Paths.get(pathToCheck))) {
                try {
                    Files.createDirectory(Paths.get(pathToCheck));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return pathToCheck;
        } else
            return pathToCheck;
    }

    /**
     * Checks whether or not a directory exists.
     * @param path Path to check
     * @return  Returns True if is existing | False if directory doesn't exist
     * @throws InvalidPathException
     */
    public boolean isDirectoryExisting(String path) throws InvalidPathException {
        try{
            if(Files.exists(Paths.get(path))){
                return true;
            } else {
                return false;
            }
        } catch (InvalidPathException e){
            throw e;
        } catch (SecurityException e){
            return true;
        }
    }

    /**
     * This method creates a directory if it doesn't exist already
     * @param path Path to the folder to create
     * @return Return true on success
     * @throws InvalidPathException
     * @throws IOException
     */
    public boolean createDirectory(String path) throws InvalidPathException, IOException {
        try {
            if(isDirectoryExisting(path))
                return true;
            else {
                Files.createDirectory(Paths.get(path));
                return true;
            }
        } catch (InvalidPathException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * This method validates the filename for Windows, Unix and Android systems
     * All unsafe characters are replaced by an underscore "_"
     * @param sFileName
     * @return Returns the modified filename
     */
    public String validateFileName(String sFileName){
        if (sFileName.contains("|"))
            sFileName = sFileName.replace("|", "_");

        if (sFileName.contains(">"))
            sFileName = sFileName.replace(">", "_");

        if (sFileName.contains("<"))
            sFileName = sFileName.replace("<", "_");

        if (sFileName.contains("\""))
            sFileName = sFileName.replace("\"", "_");

        if (sFileName.contains("?"))
            sFileName = sFileName.replace("?", "_");

        if (sFileName.contains("*"))
            sFileName = sFileName.replace("*", "_");

        if (sFileName.contains(":"))
            sFileName = sFileName.replace(":", "_");

        if (sFileName.contains("\\\\"))
            sFileName = sFileName.replace("\\\\", "_");

        if (sFileName.contains("/"))
            sFileName = sFileName.replace("/", "_");

        return sFileName;
    }

    /**
     * This method decodes a given URL using a JavaScript-Engine
     * @param toDecode String URL which needs to be decoded
     * @return Returns the decoded URL as String
     */
    public String decodeJScriptURL(String toDecode) {
        try{
            ScriptEngineManager factory = new ScriptEngineManager();
            return (String) factory.getEngineByName("JavaScript").eval("unescape('" + toDecode + "')");
        } catch (ScriptException e) {
            return "";
        }
    }

    /**
     * This methods interprets the website response as JSON and returns the result in a JSON format
     * @param url String which contains the url
     * @return Returns a JSON Object
     * @throws JSONException Thrown if response is a non proper JSON format
     */
    public JSONObject readJSON(String url) throws JSONException {
        try {
            HTTPAnalyzer httpAnalyzer = new HTTPAnalyzer(url);
            HTTPAnalyzerCode httpCode = httpAnalyzer.parse();

            if(httpCode.getCode() == 200) {
                String obj = httpAnalyzer.getBody();
                return new JSONObject(obj);
            } else {
                throw new HTTPAnalyzerException(httpCode.getCode() + " Execution failed");
            }
        } catch (HTTPAnalyzerException e) {
            // just return an empty json object maybe encode, that an error occured
            return new JSONObject("{}");
        }
    }

    /**
     * This methods interprets the website response as JSON and returns the result in a JSON format
     * @param url String which contains the url
     * @param requestProperties Specific data attached as Request Property (used for eg. VINE)
     * @return Returns a JSON Object
     * @throws JSONException Thrown if response is a non proper JSON format
     */
    public JSONObject readJSON(String url, String[][] requestProperties) throws JSONException {
        try {
            HTTPAnalyzer httpAnalyzer = new HTTPAnalyzer(url, requestProperties);
            HTTPAnalyzerCode httpCode = httpAnalyzer.parse();

            if(httpCode.getCode() == 200) {
                return new JSONObject(httpAnalyzer.getBody());
            } else {
                throw new HTTPAnalyzerException(httpCode.getCode() + " Execution failed");
            }
        } catch (HTTPAnalyzerException e) {
            // return an empty json object maybe encode, that an error occured
            return new JSONObject("{}");
        }
    }

    /**
     * A download method in order to download a given file
     * @param pURL
     * @param strFileName
     */
    public void Download(URLPackage pURL, String strFileName) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(pURL.getURL()).openConnection();
            File outputCacheFile = new File(strFileName);

            long downloadedSize = 0;
            long fileLength = 0;
            BufferedInputStream inputStream = null;
            RandomAccessFile outputFile = null;

            if(outputCacheFile.exists()) {
                urlConnection.setAllowUserInteraction(true);
                urlConnection.setRequestProperty("Range", "bytes=" + outputCacheFile.length() + "-");
            }

            urlConnection.setConnectTimeout(14000);
            urlConnection.setReadTimeout(20000);
            urlConnection.connect();

            // If reponse code is 416 download is most likely finished
            if(urlConnection.getResponseCode() == 416) {
                System.out.println("Response 416 - finished");
            } else if (urlConnection.getResponseCode() / 100 != 2) {
                System.err.println("Unkown Reponse code error");
            } else {
                String connectionField = urlConnection.getHeaderField("content-range");

                if (connectionField != null) {
                    String[] connectionRanges = connectionField.substring("bytes=".length()).split("-");
                    downloadedSize = Long.valueOf(connectionRanges[0]);
                }

                if (connectionField == null && outputCacheFile.exists())
                    outputCacheFile.delete();

                fileLength = urlConnection.getContentLength() + downloadedSize;
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
                outputFile = new RandomAccessFile(outputCacheFile, "rw");
                outputFile.seek(downloadedSize);

                byte data[] = new byte[1024];
                int count = 0;
                int __progress = 0;

                while ((count = inputStream.read(data, 0, 1024)) != -1
                        && __progress != 100) {
                    downloadedSize += count;
                    outputFile.write(data, 0, count);


                    __progress = (int) ((downloadedSize * 100) / fileLength);
                    downloadProgressChanged(__progress);
                }

                outputFile.close();
                inputStream.close();
            }
        } catch (MalformedURLException e){
            System.err.println("Malformed URL");
        } catch (IOException e) {
            System.err.println("I/O Error");
        }
    }

    public void addListener(DownloadProgressListener downloadProgressListener){
        listener.add(downloadProgressListener);
    }

    private void downloadProgressChanged(int progress){
        for (DownloadProgressListener elements : listener)
            elements.downloadProgressChange(progress);
    }
}

interface DownloadProgressListener {
    void downloadProgressChange(int e);
}

abstract class Downloaderv2 {

    // Methods any Downloader need
    public boolean isFileExisting(File fileToCheck) {
        try {
            if (fileToCheck.exists())
                return true;
            else
                return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int getDownloadSize(String urls) {
        URLConnection hUrl;
        try {
            hUrl = new URL(urls).openConnection();
            int size = hUrl.getContentLength();
            return size;

        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String CheckSavePath(String pathToCheck) {
        if (CGlobals.CURRENT_OS == OS.Windows) {
            if (!pathToCheck.endsWith("\\")) {
                pathToCheck = pathToCheck + "\\";
            }

            if (!Files.isDirectory(Paths.get(pathToCheck))) {
                try {
                    Files.createDirectory(Paths.get(pathToCheck));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return pathToCheck;
        } else if (CGlobals.CURRENT_OS == OS.Linux) {
            if (!pathToCheck.endsWith("/"))
                pathToCheck = pathToCheck + "/";

            if (!Files.isDirectory(Paths.get(pathToCheck))) {
                try {
                    Files.createDirectory(Paths.get(pathToCheck));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return pathToCheck;
        } else
            return pathToCheck;
    }

    public String validateFileName(String name) {
        if (name.contains("|"))
            name = name.replace("|", "_");

        if (name.contains(">"))
            name = name.replace(">", "_");

        if (name.contains("<"))
            name = name.replace("<", "_");

        if (name.contains("\""))
            name = name.replace("\"", "_");

        if (name.contains("?"))
            name = name.replace("?", "_");

        if (name.contains("*"))
            name = name.replace("*", "_");

        if (name.contains(":"))
            name = name.replace(":", "_");

        if (name.contains("\\\\"))
            name = name.replace("\\\\", "_");

        if (name.contains("/"))
            name = name.replace("/", "_");

        return name;
    }

    public String decodeJScriptURL(String toDecode) {
        try {
            ScriptEngineManager factory = new ScriptEngineManager();
            ScriptEngine engine = factory.getEngineByName("JavaScript");
            return (String) engine.eval("unescape('" + toDecode + "')");
        } catch (ScriptException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject readJsonFromUrl(String url) throws JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } catch (Exception ex) {
            return null;
        }
    }

    public JSONObject readJSONFromVine(String url) throws JSONException {


        try {
            URL vineurl = new URL(url);
            URLConnection uc = vineurl.openConnection();
            uc.setRequestProperty("x-vine-client", "vinewww/2.0");

            InputStream is = uc.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public void DownloadFile(String dlUrl, String filename, int downloadSize, int i, DefaultTableModel dTableModel) throws Exception {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(dlUrl).openConnection();
            File outputFileCache = new File(filename);
            long downloadedSize = 0;
            long fileLength = 0;
            BufferedInputStream input = null;
            RandomAccessFile output = null;

            if (outputFileCache.exists()) {
                connection.setAllowUserInteraction(true);
                connection.setRequestProperty("Range", "bytes=" + outputFileCache.length() + "-");
            }

            connection.setConnectTimeout(14000);
            connection.setReadTimeout(20000);
            connection.connect();

            // If response code is 416 the download is most likely already finished.
            if (connection.getResponseCode() == 416) {
                System.out.println("Response Code 416");
                dTableModel.setValueAt("100%", i, 2);
            } else if (connection.getResponseCode() / 100 != 2) {
                System.err.println("Unknown response code!");
            } else { // Continue with download
                String connectionField = connection.getHeaderField("content-range");

                if (connectionField != null) {
                    String[] connectionRanges = connectionField.substring("bytes=".length()).split("-");
                    downloadedSize = Long.valueOf(connectionRanges[0]);
                }

                if (connectionField == null && outputFileCache.exists())
                    outputFileCache.delete();

                fileLength = connection.getContentLength() + downloadedSize;
                input = new BufferedInputStream(connection.getInputStream());
                output = new RandomAccessFile(outputFileCache, "rw");
                output.seek(downloadedSize);

                byte data[] = new byte[1024];
                int count = 0;
                int __progress = 0;

                while ((count = input.read(data, 0, 1024)) != -1
                        && __progress != 100) {
                    downloadedSize += count;
                    output.write(data, 0, count);
                    __progress = (int) ((downloadedSize * 100) / fileLength);

                    if (dTableModel != null)
                        dTableModel.setValueAt(__progress + "%", i, 2);
                }

                output.close();
                input.close();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage() + " Error occured while downloading!");
        }
    }

    public void DownloadFile(URLConnection con, String filename, int downloadSize, int i, DefaultTableModel dTableModel) {
        try {
            HttpURLConnection connection = (HttpURLConnection) con;
            File outputFileCache = new File(filename);
            long downloadedSize = 0;
            long fileLength = 0;
            BufferedInputStream input = null;
            RandomAccessFile output = null;

            if (outputFileCache.exists()) {
                connection.setAllowUserInteraction(true);
                connection.setRequestProperty("Range", "bytes=" + outputFileCache.length() + "-");
            }

            connection.setConnectTimeout(14000);
            connection.setReadTimeout(20000);
            connection.connect();

            if (connection.getResponseCode() / 100 != 2)
                System.err.println("Unknown response code!");
            else {
                String connectionField = connection.getHeaderField("content-range");

                if (connectionField != null) {
                    String[] connectionRanges = connectionField.substring("bytes=".length()).split("-");
                    downloadedSize = Long.valueOf(connectionRanges[0]);
                }

                if (connectionField == null && outputFileCache.exists())
                    outputFileCache.delete();

                fileLength = connection.getContentLength() + downloadedSize;
                input = new BufferedInputStream(connection.getInputStream());
                output = new RandomAccessFile(outputFileCache, "rw");
                output.seek(downloadedSize);

                byte data[] = new byte[1024];
                int count = 0;
                int __progress = 0;

                while ((count = input.read(data, 0, 1024)) != -1
                        && __progress != 100) {
                    downloadedSize += count;
                    output.write(data, 0, count);
                    __progress = (int) ((downloadedSize * 100) / fileLength);

                    dTableModel.setValueAt(__progress + "%", i, 2);
                }

                output.close();
                input.close();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage() + " Error occured while downloading!");
        }
    }



        /* Old code block
    public void DownloadFile(String dlUrl, String filename, int downloadSize, int i, DefaultTableModel dTableModel) {
        try {
            URL url = new URL(dlUrl);
            InputStream in = new BufferedInputStream(url.openStream());
            OutputStream out = new BufferedOutputStream(new FileOutputStream(filename + ".mp4"));

            double sum = 0;
            int count;
            byte data[] = new byte[1024];
            // added a quick fix for downloading >= 0 instead of != -1
            while ((count = in.read(data, 0, 1024)) >= 0) {
                out.write(data, 0, count);
                sum += count;

                if (downloadSize > 0 && dTableModel != null) {
                    dTableModel.setValueAt(((int)(sum / downloadSize * 100)) + "%", i, 2);
                }
            }


            in.close();
            out.close();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    } */


}
