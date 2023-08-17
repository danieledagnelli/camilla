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
import java.awt.Component;
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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import net.bidimensional.camilla.CamillaCanvas;
import net.bidimensional.camilla.CamillaUtils;
import net.bidimensional.camilla.CamillaVertex;
import net.bidimensional.camilla.VisualizationType;
import org.openide.nodes.Node;
import org.sleuthkit.autopsy.corecomponents.TableFilterNode;
import org.sleuthkit.autopsy.datamodel.FileNode;
import org.sleuthkit.datamodel.AbstractContent;
import org.sleuthkit.datamodel.AnalysisResult;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.DataArtifact;

public class CamillaGraphCanvas extends JPanel implements CamillaCanvas {

    private CamillaEntityGraph graph;
    private Object parent;
    private mxGraphComponent graphComponent;
    private CamillaVertex selectedVertex;

    public mxGraph getGraph() {
        return graph;
    }

    public void setSelectedVertex(CamillaVertex vertex) {
        this.selectedVertex = vertex;
    }

    public CamillaGraphCanvas() {
        super();
        setLayout(new BorderLayout());  // Set the layout to BorderLayout

        graph = (CamillaEntityGraph) CamillaUtils.loadVisualization(VisualizationType.ENTITY);
        if (graph == null) {
            graph = new CamillaEntityGraph(new mxGraphModel()) {
//                @Override
//                public boolean isCellMovable(Object cell) {
//                    if (cell instanceof mxCell) {
//                        mxCell mxCell = (mxCell) cell;
//                        // Add your conditions here based on the mxCell object
//                        // For example, to make a specific vertex immovable, you could do:
//                        // if (mxCell.getValue().equals("My Vertex")) return false;
//                    }
////                return super.isCellMovable(cell);
//                    return true;
//                }
            };
            CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
            CamillaUtils.loadVisualization(VisualizationType.ENTITY);

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
        // Create and add the mouse listener
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Object cellObject = graphComponent.getCellAt(e.getX(), e.getY());
                if (cellObject instanceof mxCell) {
                    mxCell cell = (mxCell) cellObject;
                    handleVertexSelection(cell);
                    System.out.println("Cell: " + cell);

//                    System.out.println("Selected Vertex: " + selectedVertex.getNode().getDisplayName());
                    if (cell.isVertex()) {
//                        System.out.println("Click: " + ((TableFilterNode) (selectedVertex.getNode())).getDisplayName());

                    }
                }
            }
        });
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
                                CamillaUtils.loadVisualization(VisualizationType.ENTITY);

                            }
                        });

                        menu.add(addItem);

                        // If the click was on a vertex or an edge, offer to delete
                    } else if (cell instanceof mxCell && (((mxCell) cell).isVertex() || ((mxCell) cell).isEdge())) {
                        JMenuItem deleteItem = new JMenuItem("Delete Node");
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
                                    CamillaUtils.loadVisualization(VisualizationType.ENTITY);

                                }
                            }
                        });

                        menu.add(deleteItem);

                        JMenu showMenu = new JMenu("Actions");
                        showMenu.addMenuListener(new MenuListener() {
                            @Override
                            public void menuSelected(MenuEvent e) {
                                System.out.println("menu selected"); // Debugging
                                showMenu.removeAll();

                                if (selectedVertex != null) {
                                    System.out.println("selectedVertex is not null"); // Debugging
                                    JPopupMenu contextMenu = selectedVertex.getNode().getContextMenu();
                                    int count = contextMenu.getComponentCount();
                                    System.out.println("contextMenu count: " + count); // Debugging
                                    for (Component component : contextMenu.getComponents()) {
                                        if (component instanceof JMenuItem) {
                                            showMenu.add((JMenuItem) component);
                                        }
                                    }

                                    // Force the menu to revalidate and repaint
                                    showMenu.revalidate();
                                    showMenu.repaint();
                                } else {
                                    System.out.println("selectedVertex is null"); // Debugging
                                }
                            }

                            // ... other methods ...
                            @Override
                            public void menuDeselected(MenuEvent e) {
//                                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
                            }

                            @Override
                            public void menuCanceled(MenuEvent e) {
//                                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
                            }
                        });
                        menu.add(showMenu);

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

    public void handleVertexSelection(Object cellObject) {
        if (cellObject instanceof mxCell) {
            mxCell cell = (mxCell) cellObject;
            Object userObject = cell.getValue();
            System.out.println("------------------------------------------------");
            System.out.println("userobject Type: " + userObject.getClass().toString());
            System.out.println("User Object: " + userObject);
            System.out.println("userObject instanceof CamillaVertex: " + (userObject instanceof CamillaVertex));
            if (userObject instanceof CamillaVertex) {
                selectedVertex = (CamillaVertex) userObject;
            }
        }
    }

    //This custom handler is disabled within the canvas to allow the jgraph drag and drop
    private class CamillaTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            // Only accept the drop if the data being transferred is a Node
            // or if the dragged component is the graph component
            return (support.isDataFlavorSupported(new DataFlavor(Node.class, "Node")) || support.isDataFlavorSupported(new DataFlavor(TableFilterNode.class, "TableFilterNode")));
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            try {
                // Get the Node
//                TableFilterNode; -> this is the actual type, check here if i can enable selection
                Node node = (Node) support.getTransferable().getTransferData(new DataFlavor(Node.class, "Node"));
//                TableFilterNode tfnode = (TableFilterNode) support.getTransferable().getTransferData(new DataFlavor(Node.class, "Node"));
                System.out.println("Dropped node: " + node.getDisplayName());
                TransferHandler.DropLocation dropLocation = support.getDropLocation();
                Point dropPoint = dropLocation.getDropPoint();
                File imageFile = new File(CamillaUtils.saveImageToTempFile(node));
                String imageUrl = imageFile.toURI().toURL().toString();
                BufferedImage img = ImageIO.read(imageFile);
                super.setDragImage(img);

                AbstractContent ac;
                DataArtifact da;
                long artefactID = -1;
                CamillaVertex vertex = new CamillaVertex(node);
                Object insertedVertex;
                String vertexName = vertex.getNode().getDisplayName(); // should be vertex.displayname
                String style = "shape=image;image=" + imageUrl + ";verticalLabelPosition=bottom;verticalAlign=top;movable=1;";
                setSelectedVertex(vertex);
                if (node.getLookup().lookup(Object.class) instanceof AbstractContent) {
                    ac = (AbstractContent) node.getLookup().lookup(Object.class);
                    artefactID = ac.getId();

                }
                if (node.getLookup().lookup(Object.class) instanceof DataArtifact) {
                    da = (DataArtifact) node.getLookup().lookup(Object.class);
                    artefactID = da.getId();
                    System.out.println("DA ID:" + artefactID);
                }
                String artefactIDstring = null;

                if (artefactID != -1L) {
                    artefactIDstring = String.valueOf(artefactID);
                }
                System.out.println("Artefact ID: " + artefactIDstring);
                graph.getModel().beginUpdate();
                insertedVertex = graph.insertVertex(graph.getDefaultParent(), artefactIDstring, vertexName, dropPoint.getX(), dropPoint.getY(), 80, 30, style);
                graph.getModel().endUpdate();
                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
//                ((mxCell) insertedVertex).setValue(vertex);
                CamillaUtils.loadVisualization(VisualizationType.ENTITY);
//                if (node.getLookup().lookup(Object.class) instanceof AbstractContent) {
//                    ac = (AbstractContent) node.getLookup().lookup(Object.class);
//                    artefactID = ac.getId();
//                    artefactNode = (new FileNode(Case.getCurrentCaseThrows().getSleuthkitCase().getAbstractFileById(artefactID)));
//                    insertedVertex = graph.insertVertex(graph.getDefaultParent(), String.valueOf(artefactID), vertexName, dropPoint.getX(), dropPoint.getY(), 80, 30, style);
//                    graph.getModel().endUpdate();
//                    CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
//                    ((mxCell) insertedVertex).setValue(vertex);
//                } else {
//                    graph.getModel().beginUpdate();
//                    insertedVertex = graph.insertVertex(graph.getDefaultParent(), "", vertexName, dropPoint.getX(), dropPoint.getY(), 80, 30, style);
//                    graph.getModel().endUpdate();
//                    ((mxCell) insertedVertex).setValue(vertex);
//                    CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
//                }
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
