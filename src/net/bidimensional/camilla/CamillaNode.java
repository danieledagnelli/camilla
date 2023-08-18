/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.sleuthkit.datamodel.BlackboardArtifact;

/**
 *
 * @author danie
 */
public class CamillaNode extends AbstractNode {
    public final BlackboardArtifact artifact;


    public CamillaNode (BlackboardArtifact artifact) {
        super(Children.LEAF);
        this.artifact = artifact;
        setDisplayName(artifact.getDisplayName());
    }

    public BlackboardArtifact getArtifact() {
        return this.artifact;
    }

}