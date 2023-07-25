/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.bidimensional.camilla;

/*
 * Autopsy Forensic Browser
 *
 * Copyright 2015-2019 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
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
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.core.RuntimeProperties;
import org.sleuthkit.autopsy.coreutils.Logger;

@ActionID(category = "Tools", id = "net.bidimensional.camilla.OpenAction")
@ActionReferences(value = {
    @ActionReference(path = "Menu/Tools", position = 101),
    @ActionReference(path = "Toolbars/Case", position = 101)})
@ActionRegistration(displayName = "#CTL_OpenAction", lazy = false)
@NbBundle.Messages({"CTL_OpenAction=Camilla",
    "OpenAction.stale.confDlg.msg=The image / video database may be out of date. "
    + "Do you want to update and listen for further ingest results?\n"
    + "Choosing 'yes' will update the database and enable listening to future ingests.\n\n"
    + "Database update status will appear in the lower right corner of the application window.",
    "OpenAction.notAnalyzedDlg.msg=No image/video files available to display yet.\n"
    + "Please run FileType and EXIF ingest modules.",
    "OpenAction.stale.confDlg.title=Camilla"})
public final class OpenAction extends CallableSystemAction {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(OpenAction.class.getName());
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
//        Case openCase;
//        try {
//            openCase = Case.getCurrentCaseThrows();
//        } catch (NoCurrentCaseException ex) {
//            return false;
//        }
//        return super.isEnabled() && Installer.isJavaFxInited() && openCase.hasData();
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
    @NbBundle.Messages({"OpenAction.dialogTitle=Image Gallery",
        "OpenAction.multiUserDialog.Header=Multi-user Image Gallery",
        "OpenAction.multiUserDialog.ContentText=The Image Gallery updates itself differently for multi-user cases than single user cases. Notably:\n\n"
        + "If your computer is analyzing a data source, then you will get real-time Image Gallery updates as files are analyzed (hashed, EXIF, etc.). This is the same behavior as a single-user case.\n\n"
        + "If another computer in your multi-user cluster is analyzing a data source, you will get updates about files on that data source only when you launch Image Gallery, which will cause the local database to be rebuilt based on results from other nodes.",
        "OpenAction.multiUserDialog.checkBox.text=Don't show this message again.",
        "OpenAction.noControllerDialog.header=Cannot open Image Gallery",
        "OpenAction.noControllerDialog.text=An initialization error ocurred.\nPlease see the log for details.",})
    public void performAction() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                JLabel textLabel = new JLabel(
                        "<html><body>"
                                + "<center><h2><b>CAMILLA</b></h2>"
                                + "<p><b>C</b>ontextual <b>A</b>rtefact <b>M</b>apping and <b>I</b>nteractive <b>L</b>inking <b>L</b>ayer <b>A</b>nalyzer</p>"
                                + "<p><i><small>Copyright © 2023</small></i></p>"
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
                return;
            }
        });
//            ImageGalleryController controller;
//            // @@@ This call gets a lock. We shouldn't do this in the UI....
//            controller = ImageGalleryController.getController(currentCase);
//
//            // Display an error if we could not get the controller and return
//            if (controller == null) {
//                Alert errorDIalog = new Alert(Alert.AlertType.ERROR);
//                errorDIalog.initModality(Modality.APPLICATION_MODAL);
//                errorDIalog.setResizable(true);
//                errorDIalog.setTitle(Bundle.OpenAction_dialogTitle());
//                errorDIalog.setHeaderText(Bundle.OpenAction_noControllerDialog_header());
//                Label errorLabel = new Label(Bundle.OpenAction_noControllerDialog_text());
//                errorLabel.setMaxWidth(450);
//                errorLabel.setWrapText(true);
//                errorDIalog.getDialogPane().setContent(new VBox(10, errorLabel));
//                GuiUtils.setDialogIcons(errorDIalog);
//                errorDIalog.showAndWait();
//                logger.log(Level.SEVERE, "No Image Gallery controller for the current case");  
//                return;
//            }
//
//            // Make sure the user is aware of Single vs Multi-user behaviors
//            if (currentCase.getCaseType() == Case.CaseType.MULTI_USER_CASE
//                    && ImageGalleryPreferences.isMultiUserCaseInfoDialogDisabled() == false) {
//                Alert dialog = new Alert(Alert.AlertType.INFORMATION);
//                dialog.initModality(Modality.APPLICATION_MODAL);
//                dialog.setResizable(true);
//                dialog.setTitle(Bundle.OpenAction_dialogTitle());
//                dialog.setHeaderText(Bundle.OpenAction_multiUserDialog_Header());
//
//                Label label = new Label(Bundle.OpenAction_multiUserDialog_ContentText());
//                label.setMaxWidth(450);
//                label.setWrapText(true);
//                CheckBox dontShowAgainCheckBox = new CheckBox(Bundle.OpenAction_multiUserDialog_checkBox_text());
//                dialog.getDialogPane().setContent(new VBox(10, label, dontShowAgainCheckBox));
//                GuiUtils.setDialogIcons(dialog);
//
//                dialog.showAndWait();
//
//                if (dialog.getResult() == ButtonType.OK && dontShowAgainCheckBox.isSelected()) {
//                    ImageGalleryPreferences.setMultiUserCaseInfoDialogDisabled(true);
//                }
//            }
//
//            checkDBStale(controller);
//        });
    }

    @NbBundle.Messages({"OpenAction.openTopComponent.error.message=An error occurred while attempting to open Image Gallery.",
        "OpenAction.openTopComponent.error.title=Failed to open Image Gallery"})
    private void openTopComponent() {
//        SwingUtilities.invokeLater(() -> {
//            try {
//                ImageGalleryTopComponent.openTopComponent();
//            } catch (TskCoreException ex) {
//                logger.log(Level.SEVERE, "Failed to open Image Gallery top component", ex); //NON-NLS}
//                JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), Bundle.OpenAction_openTopComponent_error_message(), Bundle.OpenAction_openTopComponent_error_title(), JOptionPane.PLAIN_MESSAGE);
//            }
//        });
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
