package net.bidimensional.camilla.timelinebuilder;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
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
import java.io.File;
import java.io.IOException;
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
import net.bidimensional.camilla.VisualizationType;
import org.openide.nodes.Node;

public class CamillaTimelineCanvas extends JPanel implements CamillaCanvas {

    private CamillaTimelineGraph timeline;
    private Object parent;
    private mxGraphComponent graphComponent;
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
        timeline = (CamillaTimelineGraph) CamillaUtils.loadVisualization(VisualizationType.TIMELINE);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                graphComponent.repaint();
                graphComponent.revalidate();
            }
        });

        if (timeline == null) {
            timeline = new CamillaTimelineGraph(new mxGraphModel());
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);

                    double yPosition = getHeight() * 0.8; // 80% from the bottom
                    double width = getWidth() * 0.6; // 60% of screen width
                    double xPosition = (getWidth() - width) / 2; // centering the line
                    double length = xPosition + width;

                    System.out.println(yPosition);
                    System.out.println(width);
                    System.out.println(xPosition);
                    System.out.println(length);

                    if (!lineAdded) {

                        timeline.getModel().beginUpdate();
//                        arrowStartVertex = timeline.insertVertex(parent, "arrowStartVertex", "", xPosition, yPosition, 0, 0, "strokeColor=black;fillColor=black;");
                        arrowStartVertex = timeline.insertVertex(parent, "arrowStartVertex", "", 1, 2, 0, 0, "strokeColor=black;fillColor=black;");
                        timeline.getModel().endUpdate();
                        CamillaUtils.saveVisualization(VisualizationType.TIMELINE, timeline);

                        timeline.getModel().beginUpdate();
//                        arrowEndVertex = timeline.insertVertex(parent, "arrowEndVertex", "", length, yPosition, 0, 0, "strokeColor=black;fillColor=black;");
                        arrowEndVertex = timeline.insertVertex(parent, "arrowEndVertex", "", 3, 4, 0, 0, "strokeColor=black;fillColor=black;");
                        timeline.getModel().endUpdate();
                        CamillaUtils.saveVisualization(VisualizationType.TIMELINE, timeline);

                        timeline.getModel().beginUpdate();
                        arrowEdge = timeline.insertEdge(parent, "arrowEdge", "", arrowStartVertex, arrowEndVertex, "endArrow=classic;endFill=1;strokeColor=black;");
                        timeline.getModel().endUpdate();
                        CamillaUtils.saveVisualization(VisualizationType.TIMELINE, timeline);

                        lineAdded = true;

                    }

                }
            });

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
        timeline.getModel()
                .addListener(mxEvent.CHANGE, new mxIEventListener() {
                    @Override
                    public void invoke(Object sender, mxEventObject evt
                    ) {
                        CamillaUtils.saveVisualization(VisualizationType.TIMELINE, timeline);
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
                                        timeline.insertVertex(parent, null, "", e.getX(), e.getY(), 80, 30,
                                                "shape=rectangle;strokeColor=black;fillColor=white;");
                                        timeline.getModel().endUpdate();
                                        CamillaUtils.saveVisualization(VisualizationType.TIMELINE, timeline);

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

    public mxGraphComponent getGraphComponent() {
        return graphComponent;
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

                File imageFile = new File(CamillaUtils.saveImageToTempFile(node));
                String imageUrl = imageFile.toURI().toURL().toString();
                String style = "shape=image;image=" + imageUrl + ";verticalLabelPosition=bottom;verticalAlign=top;movable=1;";
                timeline.getModel().beginUpdate();
                timeline.insertVertex(timeline.getDefaultParent(), null, node.getDisplayName(), dropPoint.getX(), dropPoint.getY(), 80, 30, style);
                timeline.getModel().endUpdate();
                CamillaUtils.saveVisualization(VisualizationType.TIMELINE, timeline);

                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                System.out.println("importData Exception");
                return false;
            }
        }

    }
}
