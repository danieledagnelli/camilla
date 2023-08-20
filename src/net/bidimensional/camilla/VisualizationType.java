package net.bidimensional.camilla;

/**
 *
 * @author danie
 */


// Enum to represent different visualization types. Only Entity currently implemented.
public enum VisualizationType {
    ENTITY,
    TIMELINE;

    public String getName() {
        return this.name();
    }
}
