package net.bidimensional.camilla;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.bidimensional.camilla.graphbuilder.CamillaEntityGraph;
import net.bidimensional.camilla.graphbuilder.CamillaGraphBuilder;
import org.netbeans.swing.outline.Outline;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.datamodel.FileNode;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.TskCoreException;
import org.w3c.dom.Document;
import org.openide.explorer.view.Visualizer;

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

    /*
    This can be improved, it currently saves a temporary file for each image to be used on the Canvas so that it can be rendered there.
    The improvement is to save one temporary icon for each Artefact type
     */
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

    //NOT IMPLEMENTED: Returns the previous version of the graph stored in the db. To be implemented it needs to maintain a history of the xml graph in the database at every change.
    public static mxGraph undoChanges(VisualizationType type) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    /*
    Export the canvas passed as input on the filesystem. 
     */
    public static void saveGraphToPNG(JPanel canvas) {
        mxGraphComponent graphComponent = ((CamillaCanvas) canvas).getGraphComponent();

        // Create a BufferedImage of the graph
        BufferedImage originalImage = mxCellRenderer.createBufferedImage(graphComponent.getGraph(), null, 1, Color.WHITE, true, null);

        // Create a new image with padding otherwise the image will be cropped exactly at the node image end, which doesn't look great
        int padding = 50;
        BufferedImage imageWithPadding = new BufferedImage(originalImage.getWidth() + 2 * padding, originalImage.getHeight() + 2 * padding, originalImage.getType());
        Graphics2D g = imageWithPadding.createGraphics();
        g.setColor(Color.WHITE); // Set background color
        g.fillRect(0, 0, imageWithPadding.getWidth(), imageWithPadding.getHeight());
        g.drawImage(originalImage, padding, padding, null);
        g.dispose();

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        // Set the file filter to show only PNG files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG files", "png");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();

            // Append .png extension if not present
            if (!filePath.toLowerCase().endsWith(".png")) {
                filePath += ".png";
                fileToSave = new File(filePath);
            }

            try {
                ImageIO.write(imageWithPadding, "png", fileToSave);

                // Open the newly created file
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(fileToSave);
                }
            } catch (IOException ex) {
                // Handle the exception, e.g., log the error or show a message dialog
                System.err.println("Error saving or opening PNG file: " + ex.getMessage());
            }
        }
    }

    /*
    Save the graph passed as input as part of the case. Send the visualization type as parameter, this is for future use in case there are multiple visualizations available.
    TODO: implement maintaining the history of changes.
     */
    synchronized public static void saveVisualization(VisualizationType type, mxGraph graph)  {
        Object parent = graph.getDefaultParent();
        Object[] allVertices = (Object[]) graph.getChildVertices(parent);

        CamillaVertex cv;
        for (Object c : allVertices) {
            mxCell cell = (mxCell) c;
            // Save only the name of the artifact when saving to XML
            if (cell.getValue() instanceof CamillaVertex) {
                cv = (CamillaVertex) cell.getValue();
                String nodeName = cv.getNode().getDisplayName();
                cell.setValue(nodeName);
            }
        }

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
        try {
            stmt.execute("INSERT OR REPLACE INTO " + tablename + "(id, XML) VALUES (1, '" + graphXml + "')");
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }

    }


    /*
    Load the visualization saved as part of the case. Pass the type of visualization as parameter.
    TODO: Once implemented history, return the latest save.
     */
    synchronized public static CamillaEntityGraph loadVisualization(VisualizationType type) {
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
                }
                codec.decode(document.getDocumentElement(), graph.getModel());

                Object parent = graph.getDefaultParent();
                Object[] allVertices = (Object[]) graph.getChildVertices(parent);
                for (Object c : allVertices) {
                    mxCell cell = (mxCell) c;
                    long artefactID = Long.parseLong(cell.getId());
                    BlackboardArtifact bba;

                    try {
                        bba = Case.getCurrentCaseThrows().getSleuthkitCase().getBlackboardArtifact(artefactID);

                        if (bba instanceof BlackboardArtifact) {
                            cell.setValue(new CamillaVertex(new CamillaNode(bba)));
                        }
                    } catch (TskCoreException ex) {
                        System.out.println("it's ok, " + cell.getValue() + " was not a real artifacts");
                    }

                }

//                for (Object c : allVertices) {
//                    mxCell cell = (mxCell) c;
////                    System.out.println(cell.getValue().getClass().getTypeName());
//                }
                return (CamillaEntityGraph) graph;
            }
        } catch (SQLException | NoCurrentCaseException ex) {
            Exceptions.printStackTrace(ex);

        }
        return null;
    }

}
