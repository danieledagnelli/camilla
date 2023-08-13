package net.bidimensional.camilla.graphbuilder;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
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
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import net.bidimensional.camilla.CamillaCanvas;
import net.bidimensional.camilla.CamillaUtils;
import net.bidimensional.camilla.CamillaVertex;
import net.bidimensional.camilla.VisualizationType;
import org.openide.nodes.Node;

public class CamillaGraphCanvas extends JPanel implements CamillaCanvas {

    private CamillaEntityGraph graph;
    private Object parent;
    private mxGraphComponent graphComponent;

    public mxGraph getGraph() {
        return graph;
    }

    public CamillaGraphCanvas() {
        super();
        setLayout(new BorderLayout());  // Set the layout to BorderLayout

        graph = (CamillaEntityGraph) CamillaUtils.loadVisualization(VisualizationType.ENTITY);
        if (graph == null) {
            graph = new CamillaEntityGraph(new mxGraphModel()) {
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
            CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
        }
        graph.setCellsMovable(true);
        parent = graph.getDefaultParent();
        graphComponent = new mxGraphComponent(graph) {
            @Override
            protected JViewport createViewport() {
                JViewport viewport = super.createViewport();
                viewport.setOpaque(true);
                viewport.setBackground(Color.WHITE);
                viewport.setTransferHandler(new CamillaTransferHandler());
                return viewport;
            }
        };
        new mxRubberband(graphComponent);

        graph.getModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
            @Override
            public void invoke(Object sender, mxEventObject evt) {
                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);

            }
        });
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseExited(MouseEvent e) {
                // Re-enable the TransferHandler when the mouse exits the graphComponent
                graphComponent.setTransferHandler(new CamillaTransferHandler());
            }

            @Override
            public void mousePressed(MouseEvent e) {
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
                                graph.getModel().beginUpdate();
                                graph.insertVertex(parent, null, "", e.getX(), e.getY(), 80, 30, "shape=rectangle;strokeColor=black;fillColor=white;");
                                graph.getModel().endUpdate();

                                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
                            }
                        });

                        menu.add(addItem);

                        // If the click was on a vertex or an edge, offer to delete
                    } else if (cell instanceof mxCell && (((mxCell) cell).isVertex() || ((mxCell) cell).isEdge())) {
                        JMenuItem deleteItem = new JMenuItem("Delete");
                        deleteItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                // Confirm deletion
                                int result = JOptionPane.showConfirmDialog(graphComponent,
                                        "Are you sure you want to delete this element?", "Delete Element",
                                        JOptionPane.YES_NO_OPTION);

                                // If the user confirmed, remove the cell
                                if (result == JOptionPane.YES_OPTION) {
//                                    graphComponent.getGraph().removeCells(new Object[]{cell});
                                    deleteSelectedItems();
                                    CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);

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
        graphComponent.setDragEnabled(true);
        this.add(graphComponent, BorderLayout.CENTER);  // Add the component to the center of the layout
        this.setBackground(Color.BLACK); // Set the panel background to black
        this.setAutoscrolls(true);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

    }

    @Override
    public mxGraphComponent getGraphComponent() {
        return graphComponent;
    }

    //This custom handler is disabled within the canvas to allow the jgraph drag and drop
    private class CamillaTransferHandler extends TransferHandler {

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
                TransferHandler.DropLocation dropLocation = support.getDropLocation();
                Point dropPoint = dropLocation.getDropPoint();
                File imageFile = new File(CamillaUtils.saveImageToTempFile(node));
                String imageUrl = imageFile.toURI().toURL().toString();
                BufferedImage img = ImageIO.read(imageFile);
                super.setDragImage(img);

                String style = "shape=image;image=" + imageUrl + ";verticalLabelPosition=bottom;verticalAlign=top;movable=1;";

                CamillaVertex vertex = new CamillaVertex(node);

                graph.getModel().beginUpdate();
                graph.insertVertex(graph.getDefaultParent(), "", vertex.toString(), dropPoint.getX(), dropPoint.getY(), 80, 30, style);
                graph.getModel().endUpdate();

                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);

                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                System.out.println("importData Exception");
                return false;
            }
        }
    }

    public void deleteSelectedItems() {
        Object[] selectedCells = graph.getSelectionCells();
        if (selectedCells != null && selectedCells.length > 0) {
            graph.getModel().beginUpdate();
            try {
                graph.removeCells(selectedCells);
            } finally {
                graph.getModel().endUpdate();
            }
        }
    }
}
