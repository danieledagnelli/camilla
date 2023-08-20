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
import javax.swing.tree.TreeModel;
import net.bidimensional.camilla.CamillaCanvas;
import net.bidimensional.camilla.CamillaNode;
import net.bidimensional.camilla.CamillaUtils;
import net.bidimensional.camilla.CamillaVertex;
import net.bidimensional.camilla.NodeArtefactRegistry;
import net.bidimensional.camilla.VisualizationType;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.casemodule.services.Blackboard;
import org.sleuthkit.autopsy.corecomponents.TableFilterNode;
import org.sleuthkit.autopsy.datamodel.FileNode;
import org.sleuthkit.datamodel.AbstractContent;
import org.sleuthkit.datamodel.AnalysisResult;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.BlackboardAttribute.ATTRIBUTE_TYPE;
import org.sleuthkit.datamodel.DataArtifact;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;

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
//            CamillaUtils.loadVisualization(VisualizationType.ENTITY);

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
            if (userObject instanceof BlackboardArtifact) {
                BlackboardArtifact bba = (BlackboardArtifact) userObject;
                selectedVertex = new CamillaVertex(new CamillaNode(bba));
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

                BlackboardArtifact bba;
                if (node.getLookup().lookup(Object.class) instanceof BlackboardArtifact) {
                    bba = ((BlackboardArtifact) node.getLookup().lookup(Object.class));
                    artefactID = bba.getArtifactID();

//                    System.out.println("Artefact (from Node): " + bba.getDisplayName());
//                    System.out.println("Aretfact ID (from Node): " + artefactID);
//
//                    System.out.println("Artefact (from Case): " + Case.getCurrentCaseThrows().getSleuthkitCase().getBlackboardArtifact(artefactID));
////                    System.out.println("ArtefactName (from Case)" + getArtifactName(bba));
//                    printArtifactAttributes(bba);
                }

                String artefactIDstring = null;
                long objID = Case.getCurrentCaseThrows().getSleuthkitCase().getBlackboardArtifact(artefactID).getObjectID();

                if (artefactID != -1L) {
                    artefactIDstring = String.valueOf(artefactID);
                }

                graph.getModel().beginUpdate();
                insertedVertex = (mxCell) graph.insertVertex(graph.getDefaultParent(), artefactIDstring, vertex, dropPoint.getX(), dropPoint.getY(), 80, 30, style);
                graph.getModel().endUpdate();
                graph.refresh();

                CamillaUtils.saveVisualization(VisualizationType.ENTITY, graph);
//                graph.getModel().beginUpdate();
//                graph = CamillaUtils.loadVisualization(VisualizationType.ENTITY);
//                graph.getModel().endUpdate();

//                graph.refresh();

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
