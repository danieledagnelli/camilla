/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla;

import java.lang.reflect.InvocationTargetException;
import org.openide.nodes.Node.Property;
import org.openide.util.Exceptions;

/**
 *
 * @author danie
 */
public class CamillaVertex {
    
    private Property[] properties;
    
    public CamillaVertex(Property[] properties) {
        this.properties = properties;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }
    
    @Override
    //TODO: the toString is returned based on the type of the node so that only the relevant informations are displayed
    public String toString() {
        String v = "";
        for (Property p : properties) {
            try {
                v = v + p.getName() + ": " + p.getValue() + "\n";
            } catch (IllegalAccessException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
            
        }
        return v;
        
    }
    
}
