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
import net.bidimensional.camilla.CamillaNode;
import net.bidimensional.camilla.CamillaUtils;
import net.bidimensional.camilla.CamillaVertex;
import net.bidimensional.camilla.VisualizationType;
import org.openide.nodes.Node;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.corecomponents.TableFilterNode;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.TskCoreException;

public class CamillaGraphCanvas extends JPanel implements CamillaCanvas {

    private static CamillaEntityGraph graph;
    private Object parent;
    private mxGraphComponent graphComponent;
    private CamillaVertex selectedVertex;

    public static mxGraph getGraph() {
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
                    System.out.println("Cell on Mouse Release: " + cell.getValue());

                    handleVertexSelection(cell);
//                    CamillaUtils.saveVisualization(VisualizationType.ENTITY, CamillaGraphCanvas.getGraph());

////                    System.out.println("Selected Vertex: " + selectedVertex.getNode().getDisplayName());
//                    if (cell.isVertex()) {
////                        System.out.println("Click: " + ((TableFilterNode) (selectedVertex.getNode())).getDisplayName());
//
//                    }
                }
            }
        });
        new mxRubberband(graphComponent);

        graph.getModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
            @Override
            public void invoke(Object sender, mxEventObject evt) {

//                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
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
//                                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);

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
//                                    CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
//                                    CamillaUtils.loadVisualization(VisualizationType.ENTITY);

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
                                    System.out.println("selected vertex actions: " + selectedVertex.getNode().getActions(true).length);
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

                        if (!(((mxCell) cell).getValue() instanceof String)) {
                            menu.add(showMenu);

                        }

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
            System.out.println("---BEGIN---handleVertexSelection------------------------------------------------");
            System.out.println("userobject Type: " + userObject.getClass().toString());
            System.out.println("User Object: " + userObject);
            System.out.println("userObject instanceof CamillaVertex: " + (userObject instanceof CamillaVertex));
            System.out.println("---END---handleVertexSelection------------------------------------------------");
            try {
                selectedVertex = (CamillaVertex) (userObject);
                System.out.println("handleVertexSelection Selected Vertex: " + selectedVertex.getNode().getDisplayName());

            } catch (ClassCastException | NullPointerException e) {
                System.out.println("handleVertexSelection: probably a string - " + e.toString());

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

//        public void traverseOutline(Outline outline) {
//            OutlineModel outlineModel = (OutlineModel) outline.getModel();
//            Object root = outlineModel.getRoot();
//            traverseNode(outlineModel, root);
//        }
//
//        private void traverseNode(OutlineModel model, Object node) {
//            int childCount = model.getChildCount(node);
//            for (int i = 0; i < childCount; i++) {
//                Object child = model.getChild(node, i);
//                traverseNode(model, child);
//            }
//
//            // Do something with the node here
//            System.out.println(node);
//        }
        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            try {
                Node node = (Node) support.getTransferable().getTransferData(new DataFlavor(Node.class, "Node"));
                System.out.println("Dropped node: " + node.getDisplayName());
                TransferHandler.DropLocation dropLocation = support.getDropLocation();
                Point dropPoint = dropLocation.getDropPoint();
                File imageFile = new File(CamillaUtils.saveImageToTempFile(node));
                String imageUrl = imageFile.toURI().toURL().toString();
                BufferedImage img = ImageIO.read(imageFile);
                super.setDragImage(img);

                long artefactID = -1;
                CamillaVertex vertex = new CamillaVertex(node);

                mxCell insertedVertex;
                String vertexName = vertex.getNode().getDisplayName(); // should be vertex.displayname
                String style = "shape=image;image=" + imageUrl + ";verticalLabelPosition=bottom;verticalAlign=top;movable=1;";
                setSelectedVertex(vertex);
                long objID;
                BlackboardArtifact bba;

                // Not all elements in the outline are artefacts, some of them are Autopsy groupings.
                // This logic allows to get the id only for real investigation artefacts
                if (node.getLookup().lookup(Object.class) instanceof BlackboardArtifact) {
                    bba = ((BlackboardArtifact) node.getLookup().lookup(Object.class));
                    artefactID = bba.getArtifactID();
                    objID = Case.getCurrentCaseThrows().getSleuthkitCase().getBlackboardArtifact(artefactID).getObjectID();
                }

                String artefactIDstring = null;
                if (artefactID != -1L) {
                    artefactIDstring = String.valueOf(artefactID);
                }

                graph.getModel().beginUpdate();
                insertedVertex = (mxCell) graph.insertVertex(graph.getDefaultParent(), artefactIDstring, vertex, dropPoint.getX(), dropPoint.getY(), 80, 30, style);
                graph.getModel().endUpdate();
                graph.refresh();

//                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
                handleVertexSelection(insertedVertex);
                return true;
            } catch (UnsupportedFlavorException | IOException | NoCurrentCaseException | TskCoreException ex) {
                System.out.println("importData Exception");
                return false;
            }
        }
    }

//    public void printArtifactAttributes(BlackboardArtifact artifact) {
//        try {
//            // Iterate over all attributes associated with the artifact
//            for (BlackboardAttribute attribute : artifact.getAttributes()) {
//                // Print the attribute type and its display value
//                System.out.println(attribute.getAttributeType().getTypeName() + ": " + attribute.getDisplayString());
//            }
//        } catch (TskCoreException e) {
//            e.printStackTrace();
//        }
//    }
//    public String getArtifactName(BlackboardArtifact artifact) {
//        try {
//            for (BlackboardAttribute attribute : artifact.getAttributes()) {
//                // Depending on your case, you might be looking for a specific attribute type
//                // For example, if the name is stored in the TSK_NAME attribute:
//                if (attribute.getAttributeType().getTypeID() == ATTRIBUTE_TYPE.TSK_NAME.getTypeID()) {
//                    return attribute.getDisplayString();
//                }
//            }
//        } catch (TskCoreException e) {
//            e.printStackTrace();
//        }
//        return null;  // or return a default value
//    }
//    public BlackboardArtifact getArtifactById(long artifactId) {
//        try {
//            // Obtain the current case
//            Case currentCase = Case.getCurrentCase();
//
//            // Fetch the SleuthkitCase object
//            SleuthkitCase sleuthkitCase = currentCase.getSleuthkitCase();
//
//            // Retrieve the artifact using its ID
//            BlackboardArtifact artifact = sleuthkitCase.getBlackboardArtifact(artifactId);
//
//            return artifact;
//        } catch (TskCoreException e) {
//            // Handle exceptions
//            e.printStackTrace();
//            return null;
//        }
//    }
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
