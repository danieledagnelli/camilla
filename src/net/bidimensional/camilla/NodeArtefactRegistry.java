package net.bidimensional.camilla;

import java.util.HashMap;
import java.util.Map;
import org.openide.nodes.Node;
import org.sleuthkit.datamodel.BlackboardArtifact;

/**
 *
 * @author danie
 */

public class NodeArtefactRegistry {
    private final Map<BlackboardArtifact, Node> registry = new HashMap<>();

    public void register(BlackboardArtifact artefact, Node node) {
        registry.put(artefact, node);
    }

    public Node getNode(BlackboardArtifact artefact) {
        return registry.get(artefact);
    }

    // Singleton pattern to ensure only one instance of the registry
    private static final NodeArtefactRegistry INSTANCE = new NodeArtefactRegistry();

    public static NodeArtefactRegistry getInstance() {
        return INSTANCE;
    }

    private NodeArtefactRegistry() {
    }
}
