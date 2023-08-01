package net.bidimensional.camilla;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.data.json.impl.JSONArray;
import org.jfree.data.json.impl.JSONObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;

public class CamillaUtils {

    private static final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "TBD";

    public static String getResponseFromGpt4(String xmlInput) throws IOException {
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "This is an XML representing graph of relationships for a Digital Forensic investigation case. Write a report based on it:");

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", xmlInput);

        JSONArray messages = new JSONArray();
        messages.add(systemMessage.toString());
        messages.add(userMessage.toString());

        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("messages", messages.toString());
        jsonPayload.put("max_tokens", 200);
        jsonPayload.put("model", "gpt-4");

        URL url = new URL(GPT_API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);

        try ( DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
            writer.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
            writer.flush();
        }

        int status = connection.getResponseCode();

        BufferedReader reader;
        if (status > 299) {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }

        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (status > 299) {
            throw new RuntimeException("HTTP response error: " + status + " - " + response.toString());
        }

        return response.toString();
    }

    public static String saveImageToTempFile(Node n) throws IOException {
        String imageClass;
        BufferedImage outputImage;
        File tempFile = null;
        try {

            imageClass = n.getParentNode().getPropertySets()[0].getProperties()[0].getValue().toString().replaceAll(" ", "").toLowerCase();
            tempFile = File.createTempFile(imageClass, ".png");

            // Cast Image to BufferedImage
            outputImage = (BufferedImage) n.getIcon(BeanInfo.ICON_COLOR_32x32);

            // Write the image to the temporary file
            ImageIO.write(outputImage, "png", tempFile);

            // Return the path of the temporary file
            return tempFile.getAbsolutePath();
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
        return tempFile.getAbsolutePath();

    }

    public static void saveGraphToPNG(JPanel canvas) {
        mxGraphComponent graphComponent = ((CamillaCanvas) canvas).getGraphComponent();
        // Create a BufferedImage of the graph
        BufferedImage image = mxCellRenderer.createBufferedImage(graphComponent.getGraph(), null, 1, Color.WHITE, true, null);

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");
        // Set the file filter to show only PNG files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");
        fileChooser.setFileFilter(filter);

        // Show save dialog; this method does not return until the dialog is closed
        int userSelection = fileChooser.showSaveDialog(graphComponent);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            // Append .png extension if not present
            if (!filePath.toLowerCase().endsWith(".png")) {
                filePath += ".png";
                fileToSave = new File(filePath);
            }

            // Write the BufferedImage to a file
            try {
                ImageIO.write(image, "PNG", fileToSave);

                // Open the save directory and highlight the file in Windows
                Runtime.getRuntime().exec("explorer.exe /select," + filePath.replace("/", "\\"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveGraphXml(mxGraph graph, String className) {
        mxCodec codec = new mxCodec();

        String tablename = null;

        if (className.contains("Graph")) {
            tablename = "graph";
        };
        if (className.contains("Timeline")) {
            tablename = "timeline";
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

        String graphXml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
        // Get the current case
        Case currentCase;
        try {
            currentCase = Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException e) {
            System.err.println("No current case: " + e.getMessage());
            return;
        }

//        // Get the case database path
        String caseDatabasePath = currentCase.getCaseDirectory();
        String autopsyDbPath = caseDatabasePath + "\\autopsy.db";
//
        String url = "jdbc:sqlite:" + autopsyDbPath;
        try ( Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();

                // Create a new table to store your graphXml if it doesn't already exist
                stmt.execute("CREATE TABLE IF NOT EXISTS " + tablename + "_XmlData (id INTEGER PRIMARY KEY, graphXml TEXT)");

                // Replace '1' with a suitable ID for the graphXml
                stmt.execute("INSERT OR REPLACE INTO " + tablename + "_XmlData (id, graphXml) VALUES (1, '" + graphXml + "')");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static mxCell getCellByName(mxGraph graph, String name) {
        Object parent = graph.getDefaultParent();
        int childCount = graph.getModel().getChildCount(parent);

        for (int i = 0; i < childCount; i++) {
            Object childCell = graph.getModel().getChildAt(parent, i);

            if (childCell instanceof mxCell) {
                mxCell cell = (mxCell) childCell;

                if (name.equals(cell.getId())) {
                    return cell;
                }
            }
        }

        return null;
    }
}
