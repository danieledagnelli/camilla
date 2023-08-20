package net.bidimensional.camilla;

import java.io.Serializable;
import org.openide.nodes.Node;

/**
 *
 * @author danie
 */
public class CamillaVertex implements Serializable {

    private final Node node;

    public CamillaVertex(Node n) {
        this.node = n;
    }

    public Node getNode() {
        return this.node;
    }

    @Override
    //Return the name of the node as the name of the vertex
    public String toString() {
        return node.getDisplayName();

    }

}
