/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla;

import java.io.Serializable;

/**
 *
 * @author danie
 */
public class CamillaVertex implements Serializable {

    private String serializedSupport;
    private String displayName;

    public CamillaVertex(String serializedSupport, String displayName) {
        this.serializedSupport = serializedSupport;
        this.displayName = displayName;
    }

    public String getSerializedSupport() {
        return serializedSupport;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
