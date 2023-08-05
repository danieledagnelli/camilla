/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla.timelinebuilder;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.view.mxGraph;

/**
 *
 * @author danie
 */
public class CamillaTimelineGraph extends mxGraph {

    public CamillaTimelineGraph(mxIGraphModel model) {
        super(model);
    }

    public CamillaTimelineGraph() {
        super();
    }

    @Override
    public Object createEdge(Object parent, String id, Object value, Object source, Object target, String style) {
        // If the target is not null
        if (target != null) {
            // Get the source and target cells
            mxCell sourceCell = (mxCell) source;
            mxCell targetCell = (mxCell) target;

            // Get the geometry of the source and target cells
            mxGeometry sourceGeometry = sourceCell.getGeometry();
            mxGeometry targetGeometry = targetCell.getGeometry();

            // Set the x-coordinate of the target cell to the x-coordinate of the source cell
            targetGeometry.setX(sourceGeometry.getX());
            targetCell.setGeometry(targetGeometry);
        }

        // Call the superclass's createEdge method to create the edge
        return super.createEdge(parent, id, value, source, target, style);
    }

    @Override
    public void cellsMoved(Object[] cells, double dx, double dy, boolean disconnect, boolean constrain) {
        // Call the superclass's cellsMoved method
        super.cellsMoved(cells, dx, dy, disconnect, constrain);

        // For each moved cell
        for (Object cell : cells) {
            // If the cell is a vertex
            if (getModel().isVertex(cell)) {
                // Get all edges connected to the vertex
                Object[] edges = getEdges(cell);

                // For each edge
                for (Object edge : edges) {
                    // Get the source and target cells of the edge
                    mxCell sourceCell = (mxCell) getModel().getTerminal(edge, true);
                    mxCell targetCell = (mxCell) getModel().getTerminal(edge, false);

                    // Get the geometry of the source and target cells
                    mxGeometry sourceGeometry = sourceCell.getGeometry();
                    mxGeometry targetGeometry = targetCell.getGeometry();

                    // Set the x-coordinate of the target cell to the x-coordinate of the source cell
                    targetGeometry.setX(sourceGeometry.getX());
                    getModel().setGeometry(targetCell, targetGeometry);
                }
            }
        }
    }

    private boolean isBackgroundArrow(Object cell) {
        if (cell instanceof mxCell) {
            mxCell mxCell = (mxCell) cell;
            // Add your conditions here based on the mxCell object
            // For example, to make a specific vertex immovable, you could do:
            if (mxCell.getId().equals("arrowEdge")) {
                return true;
            }
        }
        return false;
    }

    private boolean isBackgroundArrowVertex(Object cell) {
        if (cell instanceof mxCell) {
            mxCell mxCell = (mxCell) cell;
            // Add your conditions here based on the mxCell object
            // For example, to make a specific vertex immovable, you could do:
            if (mxCell.getId().equals("arrowStartVertex") || mxCell.getId().equals("arrowEndVertex")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCellResizable(Object cell) {
        //if is background arrow, then can't be changed
        return !(isBackgroundArrow(cell) || isBackgroundArrowVertex(cell));
    }

    @Override
    public boolean isCellSelectable(Object cell) {
        //if is background arrow, then can't be changed
        return !(isBackgroundArrow(cell) || isBackgroundArrowVertex(cell));
    }

    @Override
    public boolean isCellLocked(Object cell) {
        //if is background arrow, then can't be changed
        return isBackgroundArrowVertex(cell);
    }

    @Override
    public boolean isCellMovable(Object cell) {
        //if is background arrow, then can't be changed
//        return isBackgroundArrow(cell) || isBackgroundArrowVertex(cell);
        return !isBackgroundArrowVertex(cell);

    }

    @Override
    public boolean isCellDeletable(Object cell) {
        //if is background arrow, then can't be changed
        return !(isBackgroundArrow(cell) || isBackgroundArrowVertex(cell));
    }
};
