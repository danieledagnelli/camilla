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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.bidimensional.camilla.graphbuilder.CamillaEntityGraph;
import net.bidimensional.camilla.timelinebuilder.CamillaTimelineGraph;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.w3c.dom.Document;

@SuppressWarnings("StaticNonFinalUsedInInitialization")
public class CamillaUtils {

    private static Case currentCase;
    private static String caseDatabasePath;
    private static String autopsyDbPath;
    private static mxCodec codec = new mxCodec();
    private static String graphXml;
    private static String url;
    private static Connection conn;
    private static mxGraph graph;
    private static CamillaTimelineGraph timelineGraph;

    private static Statement stmt;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            currentCase = Case.getCurrentCaseThrows();
            caseDatabasePath = currentCase.getCaseDirectory();
            autopsyDbPath = caseDatabasePath + "\\autopsy.db";
            url = "jdbc:sqlite:" + autopsyDbPath;
            conn = DriverManager.getConnection(url);

            stmt = conn.createStatement();
            // Create a new table to store your graphXml if it doesn't already exist
            stmt.execute("CREATE TABLE IF NOT EXISTS graphXMLdata (id INTEGER PRIMARY KEY, XML TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS timelineXMLdata (id INTEGER PRIMARY KEY, XML TEXT)");

        } catch (ClassNotFoundException | NoCurrentCaseException | SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public CamillaUtils() {

    }

    public static String saveImageToTempFile(Node n) {
        String imageClass;
        BufferedImage outputImage;
        File tempFile = null;
        try {

            imageClass = n.getDisplayName();
            outputImage = (BufferedImage) n.getOpenedIcon(BeanInfo.ICON_COLOR_32x32);
            tempFile = File.createTempFile(imageClass, ".png");
            ImageIO.write(outputImage, "png", tempFile);
            // Return the path of the temporary file
            return tempFile.getAbsolutePath();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return tempFile.getAbsolutePath();

    }

    //TODO: return the previous version of the graph stored in the db
    public static mxGraph undoChanges (VisualizationType type) {
        return null;
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

            try {
                // Write the BufferedImage to a file
                ImageIO.write(image, "PNG", fileToSave);
                Runtime.getRuntime().exec("explorer.exe /select," + filePath.replace("/", "\\"));

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            // Open the save directory and highlight the file in Windows
        }
    }
    
//TODO: maintain the history of saves in the DB
    synchronized public static void saveVisualization(VisualizationType type, mxGraph graph) {

        String tablename;
        if (null == type) {
            // throw an exception or handle default case
            throw new IllegalArgumentException("Invalid type: " + type);
        } else {
            switch (type) {
                case ENTITY:
                    tablename = "graphXMLdata";
                    break;
                case TIMELINE:
                    tablename = "timelineXMLdata";
                    break;
                default:
                    // throw an exception or handle default case
                    throw new IllegalArgumentException("Invalid type: " + type);
            }
        }
        graphXml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
        System.out.println("Saving: " + graphXml);
        try {
            stmt.execute("INSERT OR REPLACE INTO " + tablename + "(id, XML) VALUES (1, '" + graphXml + "')");
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
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

    //TODO: load the latest saved visualization
    public static mxGraph loadVisualization(VisualizationType type) {
        String tablename;
        if (null == type) {
            // throw an exception or handle default case
            throw new IllegalArgumentException("Invalid type: " + type);
        } else {
            switch (type) {
                case ENTITY:
                    tablename = "graphXMLdata";
                    break;
                case TIMELINE:
                    tablename = "timelineXMLdata";
                    break;
                default:
                    // throw an exception or handle default case
                    throw new IllegalArgumentException("Invalid type: " + type);
            }
        }

        String selectStatement = "SELECT XML FROM " + tablename + " WHERE id = 1";
        try {
            stmt = conn.createStatement();
            // Replace '1' with the ID used to save the graphXml
            ResultSet resultSet = stmt.executeQuery(selectStatement);
            if (resultSet.next()) {
                graphXml = resultSet.getString("XML");
                System.out.println("Load: " + graphXml);

                Document document = mxXmlUtils.parseXml(graphXml);
                codec = new mxCodec(document);
                switch (type) {
                    case ENTITY:
                        graph = new CamillaEntityGraph();
                        break;
                    case TIMELINE:
                        graph = new CamillaTimelineGraph();
                        break;
                }
                codec.decode(document.getDocumentElement(), graph.getModel());
                return graph;
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

}
