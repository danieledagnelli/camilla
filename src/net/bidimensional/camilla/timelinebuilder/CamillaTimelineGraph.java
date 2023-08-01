/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla.timelinebuilder;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.view.mxGraph;

/**
 *
 * @author danie
 */
public class CamillaTimelineGraph extends mxGraph {

    CamillaTimelineGraph(mxIGraphModel model) {
        super(model);
    }

    CamillaTimelineGraph() {
        super();
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
