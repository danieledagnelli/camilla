package net.bidimensional.camilla;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javafx.application.Platform;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.core.RuntimeProperties;
import org.sleuthkit.autopsy.coreutils.Logger;

@ActionID(category = "Tools", id = "net.bidimensional.camilla.OpenAction")
@ActionReferences(value = {
    @ActionReference(path = "Menu/Tools", position = 101),
    @ActionReference(path = "Toolbars/Case", position = 101)})
@ActionRegistration(displayName = "#CTL_OpenAction", lazy = false)
@NbBundle.Messages({"CTL_OpenAction=Camilla",
    "OpenAction.stale.confDlg.title=Camilla"})
public final class OpenAction extends CallableSystemAction {

    private static final long serialVersionUID = 1L;
//    private static final Logger logger = Logger.getLogger(OpenAction.class.getName());
    private static final String VIEW_IMAGES_VIDEOS = Bundle.CTL_OpenAction();

    private final PropertyChangeListener pcl;
    private final JMenuItem menuItem;
    private final JButton toolbarButton = new JButton(this.getName(),
            new ImageIcon(getClass().getResource("camilla_26.png")));

    public OpenAction() {
        super();
        toolbarButton.addActionListener(actionEvent -> performAction());
        menuItem = super.getMenuPresenter();
        pcl = (PropertyChangeEvent evt) -> {
            if (evt.getPropertyName().equals(Case.Events.CURRENT_CASE.toString())) {
                setEnabled(RuntimeProperties.runningWithGUI() && evt.getNewValue() != null);
            }
        };
        Case.addPropertyChangeListener(pcl);
        this.setEnabled(false);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Returns the toolbar component of this action
     *
     * @return component the toolbar button
     */
    @Override
    public Component getToolbarPresenter() {

        return toolbarButton;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return menuItem;
    }

    /**
     * Set this action to be enabled/disabled
     *
     * @param value whether to enable this action or not
     */
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        menuItem.setEnabled(value);
        toolbarButton.setEnabled(value);
    }

    @Override
    // Show an info Dialog when clicking on Camilla icon on the toolbar
    public void performAction() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                JLabel textLabel = new JLabel(
                        "<html><body>"
                                + "<center><h2><b>CAMILLA</b></h2>"
                                + "<p><b>C</b>ontextual <b>A</b>utopsy <b>M</b>apping and <b>I</b>ntegrated <b>L</b>inking <b>L</b>ayer <b>A</b>dd-on</p>"
                                + "<p><i><small>© 2023 Daniele D'Agnelli</small></i></p>"
                                + "</center></body></html>"
                );      // Load the image and create a label for it
                ImageIcon icon = new ImageIcon(OpenAction.this.getClass().getResource("camilla_26.png"));
                JLabel iconLabel = new JLabel(icon);
                iconLabel.setHorizontalAlignment(JLabel.CENTER);  // Center the icon
                // Create a panel and add the icon label and text label to it
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(iconLabel, BorderLayout.NORTH);
                panel.add(textLabel, BorderLayout.CENTER);
                // Display the panel in an info dialog
                JOptionPane.showMessageDialog(null, panel, "About", JOptionPane.PLAIN_MESSAGE);
            }
        });

    }


    @Override
    public String getName() {
        return VIEW_IMAGES_VIDEOS;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public boolean asynchronous() {
        return true; // run off edt
    }
}
