package net.bidimensional.camilla.timelinebuilder;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxXmlUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import net.bidimensional.camilla.CamillaCanvas;
import net.bidimensional.camilla.CamillaUtils;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskDataException;
import org.w3c.dom.Document;

public class CamillaTimelineCanvas extends JPanel implements CamillaCanvas {

    private CamillaTimelineGraph timeline;
    private Object parent;
    private mxGraphComponent graphComponent;
    private Case currentCase;
    private SleuthkitCase skCase;
    private BlackboardArtifact.Type graphType;
    private Content dataSource;
    private mxCodec codec;
    private String graphXml;
    BlackboardArtifact artifact;
    private int lastButtonPressed = -1;
    private mxRubberband rubberband;
    private boolean lineAdded = false;  // Add a flag to indicate whether the line has been added
    private Object arrowStartVertex;  // Add this line
    private Object arrowEndVertex;  // Add this line
    private Object arrowEdge;  // Add this line

    public CamillaTimelineGraph getGraph() {
        return timeline;
    }

    public CamillaTimelineCanvas() {
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

        timeline = loadTimeline();

        if (timeline == null) {
            timeline = new CamillaTimelineGraph(new CamillaTimelineModel());

            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);

                    double yPosition = getHeight() * 0.8; // 80% from the bottom
                    double width = getWidth() * 0.6; // 60% of screen width
                    double xPosition = (getWidth() - width) / 2; // centering the line
                    timeline.getModel().beginUpdate();
                    try {
                        if (!lineAdded) {
                            arrowStartVertex = timeline.insertVertex(parent, "arrowStartVertex", "", xPosition, yPosition, 0, 0, "strokeColor=black;fillColor=black;");
                            arrowEndVertex = timeline.insertVertex(parent, "arrowEndVertex", "", xPosition + width, yPosition, 0, 0, "strokeColor=black;fillColor=black;");
                            arrowEdge = timeline.insertEdge(parent, "arrowEdge", "", arrowStartVertex, arrowEndVertex, "endArrow=classic;endFill=1;strokeColor=black;");
                            lineAdded = true;
                        }
                    } finally {
                        timeline.getModel().endUpdate();
                    }

                }
            });
            CamillaUtils.saveGraphXml(timeline, this.getClass().getName());
            timeline = loadTimeline();

        }

        timeline.setCellsMovable(
                true);
        parent = timeline.getDefaultParent();

        graphComponent = new mxGraphComponent(timeline) {
            @Override
            protected JViewport createViewport() {
                JViewport viewport = super.createViewport();
                viewport.setOpaque(true);
                viewport.setBackground(Color.WHITE);
                viewport.setTransferHandler(new CamillaTransferHandler());
                return viewport;
            }
        };
        rubberband = new mxRubberband(graphComponent);

        timeline.getModel()
                .addListener(mxEvent.CHANGE, new mxIEventListener() {
                    @Override
                    public void invoke(Object sender, mxEventObject evt
                    ) {
                        CamillaUtils.saveGraphXml(timeline, this.getClass().getName());
                    }
                }
                );

        graphComponent.getGraphControl()
                .addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseExited(MouseEvent e
                    ) {
                        // Re-enable the TransferHandler when the mouse exits the graphComponent
                        graphComponent.setTransferHandler(new CamillaTransferHandler());
                    }

                    @Override
                    public void mousePressed(MouseEvent e
                    ) {
                        Object cell = graphComponent.getCellAt(e.getX(), e.getY());

                        // Check for right-click
                        if (SwingUtilities.isRightMouseButton(e)) {
                            // Create a popup menu
                            JPopupMenu menu = new JPopupMenu();

                            // If the click was in an empty area, offer to add a note
                            if (cell == null) {
                                JMenuItem addItem = new JMenuItem("Add Note");
                                addItem.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent ae) {
                                        timeline.getModel().beginUpdate();
                                        try {
                                            // Add a new vertex at the right-click location with a black border and a white background
                                            timeline.insertVertex(parent, null, "", e.getX(), e.getY(), 80, 30,
                                                    "shape=rectangle;strokeColor=black;fillColor=white;");
                                        } finally {
                                            timeline.getModel().endUpdate();
                                        }
                                    }
                                });

                                menu.add(addItem);

                                // If the click was on a vertex or an edge, offer to delete
                            } else if (cell instanceof mxCell && (((mxCell) cell).isVertex() || ((mxCell) cell).isEdge())) {
                                JMenuItem deleteItem = new JMenuItem("Delete");
                                deleteItem.addActionListener(new ActionListener() {
                                    public void actionPerformed(ActionEvent ae) {
                                        if (cell instanceof mxCell) {
                                            mxCell mxCell = (mxCell) cell;
                                            if (mxCell.getValue() instanceof String) {
                                                String cellValue = (String) mxCell.getValue();
                                                // If the cell is one of the ones we want to keep undeletable, do not proceed with deletion
                                                if (cellValue.equals("arrowStartVertex") || cellValue.equals("arrowEndVertex") || cellValue.equals("arrowEdge")) {
                                                    return;
                                                }
                                            }
                                        }
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

                                menu.add(deleteItem);
                            }

                            // Show the popup menu
                            menu.show(graphComponent.getGraphControl(), e.getX(), e.getY());
                        }
                    }
                }
                );
        graphComponent.setDragEnabled(
                true);

        this.add(graphComponent, BorderLayout.CENTER);  // Add the component to the center of the layout

        this.setBackground(Color.BLACK); // Set the panel background to black

        this.setAutoscrolls(
                true);

        this.setCursor(
                new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    private CamillaTimelineGraph loadTimeline() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

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
                ResultSet resultSet = stmt.executeQuery("SELECT graphXml FROM timeline_XmlData WHERE id = 1");

                if (resultSet.next()) {
                    graphXml = resultSet.getString("graphXml");

                    Document document = mxXmlUtils.parseXml(graphXml);
                    codec = new mxCodec(document);
                    timeline = new CamillaTimelineGraph();
                    codec.decode(document.getDocumentElement(), timeline.getModel());

                    return timeline;
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    public mxGraphComponent getGraphComponent() {
        return graphComponent;
    }

    //TODO: bring into CamillaUtils
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

    //This custom handler is disabled within the canvas to allow the jgraph drag and drop
    private class CamillaTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            // Get the cell at the mouse location
            mxCell cell = (mxCell) graphComponent.getCellAt(graphComponent.getMousePosition().x, graphComponent.getMousePosition().y);

            // If the cell is a vertex or an edge, don't start a drag operation
            if (cell != null && (cell.isVertex() || cell.isEdge())) {
                return null;
            }

            // Otherwise, let the superclass handle the creation of the Transferable
            return super.createTransferable(c);
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            // Only accept the drop if the data being transferred is a Node
            // or if the dragged component is the graph component
            return support.isDataFlavorSupported(new DataFlavor(Node.class, "Node"));
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            try {
                // Get the Node
                Node node = (Node) support.getTransferable().getTransferData(new DataFlavor(Node.class, "Node"));
                System.out.println("Dropped node: " + node.getDisplayName());

                // Get the drop point from the TransferSupport object
                Point dropPoint = support.getDropLocation().getDropPoint();

                // Cast arrowEdge to mxCell and get its geometry
                mxCell edgeCell = CamillaUtils.getCellByName(timeline, "arrowEdge");
                mxGeometry edgeGeometry = edgeCell.getGeometry();

                // Generate a random x position along the arrow edge for the edge control point
                Random rand = new Random();
                double minX = edgeGeometry.getX();
                double maxX = minX + edgeGeometry.getWidth();
                double controlPointX = minX + (maxX - minX) * rand.nextDouble();

                timeline.getModel().beginUpdate();
                try {
                    File imageFile = new File(saveImageToTempFile(node));
                    String imageUrl = imageFile.toURI().toURL().toString();
                    String style = "shape=image;image=" + imageUrl + ";verticalLabelPosition=bottom;verticalAlign=top;movable=0;";
                    // Create a new vertex at the drop point
                    Object newVertex = timeline.insertVertex(timeline.getDefaultParent(), null, node.getDisplayName(), dropPoint.getX(), dropPoint.getY(), 80, 30, style);

                    // Create a new edge from the new vertex to the arrowEdge
                    Object newEdge = timeline.insertEdge(timeline.getDefaultParent(), null, "", newVertex, arrowEdge);

                    // Set the control point for the new edge
                    mxGeometry edgeGeometryForNewEdge = timeline.getModel().getGeometry(newEdge);
                    edgeGeometryForNewEdge.setTerminalPoint(new mxPoint(controlPointX, edgeGeometry.getY()), false);
                    timeline.getModel().setGeometry(newEdge, edgeGeometryForNewEdge);
                } finally {
                    timeline.getModel().endUpdate();
                }

                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                System.out.println("importData Exception");
                return false;
            }
        }

    }
}
