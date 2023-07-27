package net.bidimensional.camilla;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskDataException;
import org.w3c.dom.Document;

public class CamillaCanvas extends JPanel {

    private mxGraph graph;
    private Object parent;
    private mxGraphComponent graphComponent;
    private Case currentCase;
    private SleuthkitCase skCase;
    private BlackboardArtifact.Type graphType;
    private Content dataSource;
    private mxCodec codec;
    private String graphXml;
    BlackboardArtifact artifact;

    public mxGraph getGraph() {
        return graph;
    }

    public CamillaCanvas() {
        super();
        setLayout(new BorderLayout());  // Set the layout to BorderLayout

        currentCase = Case.getCurrentCase();
        skCase = currentCase.getSleuthkitCase();
        try {
//            TODO: FIX
            graphType = currentCase.getSleuthkitCase().addBlackboardArtifactType("TSK_GRAPH", "Graph");

            dataSource = currentCase.getDataSources().get(0);  // Assuming you have only one data source
            artifact = dataSource.newArtifact(graphType.getTypeID());

        } catch (TskCoreException ex) {
            Exceptions.printStackTrace(ex);
        } catch (TskDataException ex) {
            Exceptions.printStackTrace(ex);
        }

        codec = new mxCodec();

        graph = loadGraph();

        if (graph == null) {
            graph = new mxGraph() {
                @Override
                public boolean isCellMovable(Object cell) {
                    if (cell instanceof mxCell) {
                        mxCell mxCell = (mxCell) cell;
                        // Add your conditions here based on the mxCell object
                        // For example, to make a specific vertex immovable, you could do:
                        // if (mxCell.getValue().equals("My Vertex")) return false;
                    }
//                return super.isCellMovable(cell);
                    return true;
                }
            };
        }
        graph.setCellsMovable(true);
        parent = graph.getDefaultParent();

        graphComponent = new mxGraphComponent(graph) {
            @Override
            protected JViewport createViewport() {
                JViewport viewport = super.createViewport();
                viewport.setOpaque(true);
                viewport.setBackground(Color.WHITE);
                viewport.setTransferHandler(new TransferHandler() {
                    @Override
                    public boolean canImport(TransferHandler.TransferSupport support) {
                        // Only accept the drop if the data being transferred is a Node
                        return support.isDataFlavorSupported(new DataFlavor(Node.class, "Node"));
                    }

                    @Override
                    public boolean importData(TransferHandler.TransferSupport support) {
                        try {
                            // Get the Node
                            Node node = (Node) support.getTransferable().getTransferData(new DataFlavor(Node.class, "Node"));

                            System.out.println("Dropped node: " + node.getDisplayName());
                            TransferHandler.DropLocation dropLocation = support.getDropLocation();
                            Point dropPoint = dropLocation.getDropPoint();

                            graph.getModel().beginUpdate();
                            try {
                                File imageFile = new File(saveImageToTempFile(node));
                                String imageUrl = imageFile.toURI().toURL().toString();
                                String style = "shape=image;image=" + imageUrl + ";verticalLabelPosition=bottom;verticalAlign=top;movable=1;";
                                graphXml = mxXmlUtils.getXml(codec.encode(graph.getModel()));

//                                System.out.println(CamillaUtils.getResponseFromGpt4(graphXml));
                                saveGraphXml(graphXml);
                                graph.insertVertex(graph.getDefaultParent(), null, node.getDisplayName(), dropPoint.getX(), dropPoint.getY(), 80, 30, style);

                            } finally {
                                graph.getModel().endUpdate();
                            }

                            return true;
                        } catch (UnsupportedFlavorException | IOException ex) {
                            System.out.println("importData Exception");
                            return false;
                        }
                    }
                });
                return viewport;
            }
        };
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Check for right-click
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Get the cell at the mouse location
                    final Object cell = graphComponent.getCellAt(e.getX(), e.getY());

                    // Check if the cell is a vertex or an edge
                    if (cell instanceof mxCell && (((mxCell) cell).isVertex() || ((mxCell) cell).isEdge())) {
                        // Create a popup menu
                        JPopupMenu menu = new JPopupMenu();

                        // Create a menu item for deleting the cell
                        JMenuItem deleteItem = new JMenuItem("Delete");
                        deleteItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                // Confirm deletion
                                int result = JOptionPane.showConfirmDialog(graphComponent,
                                        "Are you sure you want to delete this element?", "Delete Element",
                                        JOptionPane.YES_NO_OPTION);

                                // If the user confirmed, remove the cell
                                if (result == JOptionPane.YES_OPTION) {
                                    graphComponent.getGraph().removeCells(new Object[]{cell});
                                }
                            }
                        });

                        // Add the menu item to the popup menu
                        menu.add(deleteItem);

                        // Show the popup menu
                        menu.show(graphComponent.getGraphControl(), e.getX(), e.getY());
                    }
                }
            }
        });
        graphComponent.setDragEnabled(true);

        this.add(graphComponent, BorderLayout.CENTER);  // Add the component to the center of the layout

        this.setBackground(Color.BLACK); // Set the panel background to black
        this.setAutoscrolls(true);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    private mxGraph loadGraph() {
        // Get the current case
        Case currentCase;
        try {
            currentCase = Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException e) {
            System.err.println("No current case: " + e.getMessage());
            return null;
        }

        // Get the case directory path and append the relative path to the autopsy.db
        String caseDatabasePath = currentCase.getCaseDirectory();
        String autopsyDbPath = caseDatabasePath + "\\autopsy.db";

        String url = "jdbc:sqlite:" + autopsyDbPath;
        try ( Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();

                // Replace '1' with the ID used to save the graphXml
                ResultSet resultSet = stmt.executeQuery("SELECT graphXml FROM graphXmlData WHERE id = 1");

                if (resultSet.next()) {
                    String graphXml = resultSet.getString("graphXml");

                    Document document = mxXmlUtils.parseXml(graphXml);
                    mxCodec codec = new mxCodec(document);
                    mxGraph graph = new mxGraph();
                    codec.decode(document.getDocumentElement(), graph.getModel());

                    return graph;
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    private void saveGraphXml(String graphXML) {
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
                stmt.execute("CREATE TABLE IF NOT EXISTS graphXmlData (id INTEGER, graphXml TEXT)");

                // Replace '1' with a suitable ID for the graphXml
                stmt.execute("INSERT INTO graphXmlData (id, graphXml) VALUES (1, '" + graphXml + "')");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public mxGraphComponent getGraphComponent() {
        return graphComponent;
    }

    public String saveImageToTempFile(Node n) throws IOException {
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

}
