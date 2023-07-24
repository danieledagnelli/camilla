package net.bidimensional.camilla;

import java.awt.datatransfer.DataFlavor;
import org.openide.explorer.view.OutlineView;
import javax.swing.*;
import java.awt.datatransfer.Transferable;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import org.netbeans.swing.outline.Outline;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;

public class CamillaOutlineView extends OutlineView {

    public CamillaOutlineView(String nodesColumnLabel) {
        super(nodesColumnLabel);
        this.getOutline().setTransferHandler(new CamillaTransferHandler());
    }

    class CamillaTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof Outline) {
                Outline outline = (Outline) c;
                int selectedRow = outline.getSelectedRow();

                if (selectedRow >= 0) {
                    // Get the node at the selected row
                    Object valueAtRow = outline.getValueAt(selectedRow, 0);
                    Node selectedNode = Visualizer.findNode(valueAtRow);

                    // Create and return a Transferable that encapsulates the selected Node
                    return new NodeTransferable(selectedNode);
                }
            }
            System.out.println("Drag started! - ");

            return null;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }
    }

    private static class NodeTransferable implements Transferable {

        private Node node;

        public NodeTransferable(Node node) {
            this.node = node;
        }

        public Object getTransferData(DataFlavor flavor) {
            if (isDataFlavorSupported(flavor)) {
                return node;
            } else {
                return null;
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{new DataFlavor(Node.class, "Node")};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(getTransferDataFlavors()[0]);
        }
    }
}
