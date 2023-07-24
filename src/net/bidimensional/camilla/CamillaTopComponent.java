/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.RetainLocation;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sleuthkit.autopsy.actions.AddBookmarkTagAction;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataResult;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataResultViewer;
import org.sleuthkit.autopsy.corecomponents.DataContentTopComponent;
import org.sleuthkit.autopsy.corecomponents.DataResultTopComponent;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.coreutils.ThreadConfined;
import org.sleuthkit.autopsy.directorytree.ExternalViewerShortcutAction;
import org.sleuthkit.datamodel.TskCoreException;

/**
 *
 * @author danie
 */
@TopComponent.Description(
        preferredID = "CamillaTopComponent",
        //iconBase = "org/sleuthkit/autopsy/imagegallery/images/lightbulb.png", /*use this to put icon in window title area*/
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@RetainLocation("editor")
@TopComponent.Registration(mode = "editor", openAtStartup = true)
//@ActionID(category = "Tools", id = "net.bidimensional.camilla.OpenAction")
//@ActionReferences(value = {
//    @ActionReference(path = "Menu/Tools", position = 102)
//    ,
//    @ActionReference(path = "Toolbars/Case", position = 102)})
@Messages({
    //    "CTL_CamillaAction=Camilla",
    "HINT_CamillaTopComponent=This is a Camilla window",
    "CTL_CamillaAction=Camilla Graph Builder",
    "CTL_CamillaTopComponent=Camilla Graph Builder"
})
public final class CamillaTopComponent extends TopComponent implements DataResult, ExplorerManager.Provider, Lookup.Provider {

    private static final long serialVersionUID = 1L;
    private static final String PREFERRED_ID = "CamillaTopComponent"; // NON-NLS

    private static final Logger logger = Logger.getLogger(CamillaTopComponent.class.getName());
    private static final List<String> activeComponentIds = Collections.synchronizedList(new ArrayList<String>());
    private final boolean isMain;
    private final String customModeName;
    private final ExplorerManager em;
    private final CamillaResultPanel camillaResultPanel;
    private Camilla camilla;

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

    public TopComponent findParentTopComponent(TopComponent child) {
        WindowManager wm = WindowManager.getDefault();
        Mode editorMode = wm.findMode("editor");
        TopComponent[] topComponents = WindowManager.getDefault().getOpenedTopComponents(editorMode);

        for (TopComponent tc : topComponents) {
            if (tc.isAncestorOf(child)) {
                return tc;
            }
        }

        return null;
    }

    public CamillaTopComponent() {
        setName(Bundle.CTL_CamillaTopComponent());
        camilla = new Camilla();
        initComponents();
        isMain = false;
        customModeName = "Camilla";
        em = new ExplorerManager();
        this.camillaResultPanel = new CamillaResultPanel("Camilla", isMain, Collections.emptyList(), DataContentTopComponent.findInstance());
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ExternalViewerShortcutAction.EXTERNAL_VIEWER_SHORTCUT, "useExternalViewer"); //NON-NLS 
        getActionMap().put("useExternalViewer", ExternalViewerShortcutAction.getInstance()); //NON-NLS
        createInstance("Camilla");
    }

    public static CamillaTopComponent createInstance(String title, String description, Node node, int childNodeCount) {
        CamillaTopComponent resultViewTopComponent = new CamillaTopComponent(false, title, null, Collections.emptyList(), DataContentTopComponent.findInstance());
        initInstance(description, node, childNodeCount, resultViewTopComponent);
        return resultViewTopComponent;
    }

    public static CamillaTopComponent createInstance(String title, String description, Node node, int childNodeCount, Collection<DataResultViewer> viewers) {
        CamillaTopComponent resultViewTopComponent = new CamillaTopComponent(false, title, null, viewers, DataContentTopComponent.findInstance());
        initInstance(description, node, childNodeCount, resultViewTopComponent);
        return resultViewTopComponent;
    }

    public static CamillaTopComponent createInstance(String title) {
        CamillaTopComponent resultViewTopComponent = new CamillaTopComponent(false, title, null, Collections.emptyList(), DataContentTopComponent.findInstance());
        return resultViewTopComponent;
    }



    public static CamillaTopComponent getTopComponent() {
        return (CamillaTopComponent) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
    }

    public static void initInstance(String description, Node node, int childNodeCount, CamillaTopComponent resultViewTopComponent) {
        resultViewTopComponent.setNumberOfChildNodes(childNodeCount);
        resultViewTopComponent.open();
        resultViewTopComponent.setNode(node);
        resultViewTopComponent.setPath(description);
        resultViewTopComponent.requestActive();
    }

    public static CamillaTopComponent createInstance(String title, String mode, String description, Node node, int childNodeCount, DataContentTopComponent contentViewTopComponent) {
        CamillaTopComponent newDataResult = new CamillaTopComponent(false, title, mode, Collections.emptyList(), contentViewTopComponent);
        initInstance(description, node, childNodeCount, newDataResult);
        return newDataResult;
    }

    public CamillaTopComponent(String title) {
        this(true, title, null, Collections.emptyList(), DataContentTopComponent.findInstance());
    }

    private CamillaTopComponent(boolean isMain, String title, String mode, Collection<DataResultViewer> viewers, DataContentTopComponent contentViewTopComponent) {

        this.isMain = isMain;
        this.em = new ExplorerManager();
        associateLookup(ExplorerUtils.createLookup(em, getActionMap()));
        this.customModeName = mode;
        this.camillaResultPanel = new CamillaResultPanel(title, isMain, viewers, contentViewTopComponent);
        initComponents();
        customizeComponent(title);
    }

    /**
     * Sets the cardinality of the current node's children
     *
     * @param childNodeCount The cardinality of the node's children.
     */
    private void setNumberOfChildNodes(int childNodeCount) {
        this.camillaResultPanel.setNumberOfChildNodes(childNodeCount);
    }

    private void customizeComponent(String title) {
        setToolTipText(NbBundle.getMessage(DataResultTopComponent.class, "HINT_NodeTableTopComponent"));  //NON-NLS
        setTitle(title);
        setName(title);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(AddBookmarkTagAction.BOOKMARK_SHORTCUT, "addBookmarkTag"); //NON-NLS
        getActionMap().put("addBookmarkTag", new AddBookmarkTagAction()); //NON-NLS
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ExternalViewerShortcutAction.EXTERNAL_VIEWER_SHORTCUT, "useExternalViewer"); //NON-NLS 
        getActionMap().put("useExternalViewer", ExternalViewerShortcutAction.getInstance()); //NON-NLS
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, isMain);
        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, true);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, true);
        activeComponentIds.add(title);
    }

    private void initComponents() {

        CamillaResultPanel camillaResultPanelLocal = camillaResultPanel;

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(camillaResultPanelLocal, javax.swing.GroupLayout.DEFAULT_SIZE, 967, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(camillaResultPanelLocal, javax.swing.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
        );
    }

    @Override
    public List<DataResultViewer> getViewers() {
        return camillaResultPanel.getViewers();
    }


    @Override
    public void componentOpened() {
        WindowManager.getDefault().setTopComponentFloating(this, true);

        this.camillaResultPanel.open();
    }

    @Override
    public void componentActivated() {
        super.componentActivated();

        /*
         * Determine which node the content viewer should be using. If multiple
         * results are selected, the node used by the content viewer should be
         * null so no content gets displayed.
         */
        final DataContentTopComponent dataContentTopComponent = DataContentTopComponent.findInstance();
        final Node[] nodeList = em.getSelectedNodes();

        Node selectedNode;
        if (nodeList.length == 1) {
            selectedNode = nodeList[0];
        } else {
            selectedNode = null;
        }

        /*
         * If the selected node of the content viewer is different than that of
         * the result viewer, the content viewer needs to be updated. Otherwise,
         * don't perform the update. This check will ensure that clicking the
         * column headers and scroll bars of the DataResultTopComponent will not
         * needlessly refresh the content view and cause the tab selection to
         * change to the default.
         */
//        if (selectedNode != dataContentTopComponent.getNode()) {
//            dataContentTopComponent.setNode(selectedNode);
//        }
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        activeComponentIds.remove(this.getName());
        camillaResultPanel.close();
    }

    @Override
    protected String preferredID() {
        return getName();
    }

    @Override
    public String getPreferredID() {
        return getName();
    }

    @Override
    public void setNode(Node selectedNode) {
        camillaResultPanel.setNode(selectedNode);
    }

    @Override
    public void setTitle(String title) {
        setName(title);
    }

    @Override
    public void setPath(String pathText) {
        camillaResultPanel.setPath(pathText);
    }

    @Override
    public boolean isMain() {
        return isMain;
    }

    @Override
    public boolean canClose() {
        Case openCase;
        try {
            openCase = Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException unused) {
            return true;
        }
        return (!this.isMain) || openCase.hasData() == false;
    }

    public void setSelectedNodes(Node[] selected) {
        camillaResultPanel.setSelectedNodes(selected);
    }

    public Node getRootNode() {
        return camillaResultPanel.getRootNode();
    }

    /**
     * Sets the cardinality of the current node's children
     *
     * @param childNodeCount The cardinality of the node's children.
     */
}
