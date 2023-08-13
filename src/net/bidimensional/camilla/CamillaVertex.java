/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.util.Exceptions;

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
    //TODO: the toString is returned based on the type of the node so that only the relevant informations are displayed
    public String toString() {
        return node.getDisplayName();
//        String v = "";
//        for (Property p : node.getPropertySets()[0].getProperties()) {
//            try {
//                v = v + p.getName() + ": " + p.getValue() + "\n";
//            } catch (IllegalAccessException | InvocationTargetException ex) {
//                Exceptions.printStackTrace(ex);
//            }
//            
//        }
//        // Name is not saved, possible problem with escapes
//        return v.trim();
//
    }

}
