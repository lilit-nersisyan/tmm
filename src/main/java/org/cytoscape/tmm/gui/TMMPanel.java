package org.cytoscape.tmm.gui;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.tmm.Enums.ETMMProps;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.actions.OpenFileChooserAction;
import org.cytoscape.tmm.actions.RunPipelineAction;
import sun.management.snmp.jvmmib.EnumJvmThreadCpuTimeMonitoring;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.io.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by Lilit Nersisyan on 3/18/2017.
 */
public class TMMPanel extends JPanel implements CytoPanelComponent {
    private static Properties tmmProps;
    private String title = "TMM";
    private Component psfcPanel = null;
    private File expMatFile = null;
    private File parentDir = null;
    private File tmmLabels = null;
    private String iterationTitle = "Untitled_iteration";


    public TMMPanel() throws Exception {
        if(getPSFCPanel() == null) {
            showMessageDialog("PSFC 1.1.3 not running! Install PSFC 1.1.3 before installing TMM.", JOptionPane.ERROR_MESSAGE);
            throw new Exception("PSFC 1.1.3 not running! Install PSFC 1.1.3 before installing TMM.");
        }
        psfcPanel = getPSFCPanel();
        this.setPreferredSize(new Dimension(380, getHeight()));
        loadProps();
        initComponents();
        setComponentProperties();
        setToolTips();
        setModels();
        addActionListeners();
        enableButtons();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.WEST;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Icon getIcon() {
        return null;
    }


    private void enableButtons() {
        boolean enable = true;
        if (expMatFile == null || !expMatFile.exists())
            enable = false;
        if (parentDir == null || !parentDir.exists())
            enable = false;

        boolean enableValidationButtons = jrb_validationMode.isSelected();
        jb_generateTMMLabels.setEnabled(enableValidationButtons);
        jb_chooseTMMLabels.setEnabled(enableValidationButtons);
        jb_editTMMLabels.setEnabled(enableValidationButtons);
        if (jrb_validationMode.isSelected())
            if (tmmLabels == null || !tmmLabels.exists())
                enable = false;

        if (getCurrentNetwork() == null)
            enable = false;

        jb_addFC.setEnabled(enable);
        jb_runPSF.setEnabled(enable);
        jb_generateReport.setEnabled(enable);
        jb_runAll.setEnabled(enable);

    }


    private void setModels() {
        setjcb_geneIDModel();

    }

    private void setToolTips() {
        jl_expMat.setToolTipText("The expression file (see the manual for the format)");
        jl_parentDir.setToolTipText("The directory where the iteration folder should be placed");
    }

    private void setComponentProperties() {
        String property;
        // Expmat file
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.EXPMATFILE.getName());
        if (property != null && !property.equals("")) {
            try {
                expMatFile = new File(property);
                if (!expMatFile.exists())
                    expMatFile = null;
            } catch (Exception e) {
                System.out.println("TMM::Could not load expmat file " + property + " " + e.getMessage());
            }
        }
        if (expMatFile != null) {
            jl_chosenExpMatFile.setText(expMatFile.getName());
            jl_chosenExpMatFile.setToolTipText(expMatFile.getAbsolutePath());
        }

        // Parent dir
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.PARENTDIR.getName());
        if (property != null && !property.equals("")) {
            try {
                parentDir = new File(property);
                if (!parentDir.exists() || !parentDir.isDirectory())
                    parentDir = null;
            } catch (Exception e) {
                System.out.println("TMM::Could not set parent dir " + property + " " + e.getMessage());
            }
        }
        if (parentDir != null) {
            jl_chosenParentDir.setText(parentDir.getName());
            jl_chosenParentDir.setToolTipText(parentDir.getAbsolutePath());
        }

        // Gene ID is set in the setModels() method

        // iteration title
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.ITERATION.getName());
        if (property != null && !property.equals("") && !property.contains(" ")) {
            iterationTitle = property;
            jtxt_iterationTitle.setText(iterationTitle);
        }

        // comment
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.COMMENT.getName());
        if (property != null && !property.equals("")) {
            jtxt_comment.setText(property);
        }

        // validation
        ButtonGroup modeButtons = new ButtonGroup();
        modeButtons.add(jrb_predictionMode);
        modeButtons.add(jrb_validationMode);
        jrb_predictionMode.setSelected(true); // default

        // tmmlabel
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.TMMLABELSFILE.getName());
        if (property != null && !property.equals("")) {
            try {
                tmmLabels = new File(property);
                if (!tmmLabels.exists())
                    tmmLabels = null;
            } catch (Exception e) {
                System.out.println("TMM::Could not load tmm labels file " + property + " " + e.getMessage());
            }
        }
    }

    public void loadProps() {
        for (ETMMProps property : ETMMProps.values()) {
            property.setOldValue(Boolean.parseBoolean((String) TMMActivator.getTMMProps().get(property.getName())));
            property.setNewValue(Boolean.parseBoolean((String) TMMActivator.getTMMProps().get(property.getName())));
        }
    }

    private void addActionListeners() {
        jb_chooseExpMatFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_chooseExpMatFileActionPerformed(e);
            }
        });

        jb_browseParentDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_browseParentDirActionPerformed(e);
            }
        });

        jtxt_iterationTitle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jtxt_iterationTitleActionPerformed();
            }
        });

        jrb_predictionMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jrb_predictionModeActionPerformed();
            }
        });

        jrb_validationMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jrb_validationModeActionPerformed();
            }
        });

        jb_generateTMMLabels.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_generateTMMLabelsActionPerformed(e);
            }
        });

        jb_chooseTMMLabels.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_chooseTMMLabelsActionPerformed(e);
            }
        });

        jb_editTMMLabels.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_editTMMLabelsActionPerformed(e);
            }
        });

        jb_addFC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_addFCActionPerformed(e);
            }
        });

        jb_runPSF.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_runPSFActionPerformed(e);
            }
        });

        jb_generateReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_generateReportActionPerformed(e);
            }
        });

        jb_runAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_runAllActionPerformed(e);
            }
        });

        jb_saveSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_saveSettingsActionPerformed();
            }
        });

    }


    private void setjcb_geneIDModel() {
        if (getCurrentNetwork() == null)
            jcb_geneID.setModel(new DefaultComboBoxModel());
        else {
            Collection<CyColumn> columns = getCurrentNetwork().getDefaultNodeTable().getColumns();
            String[] attributes = new String[columns.size()];
            int i = 0;
            for (CyColumn column : columns) {
                attributes[i++] = column.getName();
            }
            jcb_geneID.setModel(new DefaultComboBoxModel(attributes));
            //Select item from properties, if valid
            String geneIDAttribute = TMMActivator.getTMMProps().getProperty(ETMMProps.GENEID.getName());
            for (i = 0; i < jcb_geneID.getItemCount(); i++) {
                Object item = jcb_geneID.getItemAt(i);
                if (item.toString().equals(geneIDAttribute))
                    jcb_geneID.setSelectedItem(item);
            }
        }
    }

    private void jb_chooseExpMatFileActionPerformed(ActionEvent e) {
        OpenFileChooserAction openFileChooserAction = new OpenFileChooserAction("Select Expression matrix file", false);
        openFileChooserAction.actionPerformed(e);
        if (openFileChooserAction.getSelectedFile() != null)
            expMatFile = openFileChooserAction.getSelectedFile();
        if (expMatFile != null) {
            if (!expMatFile.exists())
                showMessageDialog("Selected file does not exist!", JOptionPane.WARNING_MESSAGE);
            else {
                jl_chosenExpMatFile.setText(expMatFile.getName());
                jl_chosenExpMatFile.setToolTipText(expMatFile.getAbsolutePath());
            }
        }
        enableButtons();
    }

    private void jb_browseParentDirActionPerformed(ActionEvent e) {
        OpenFileChooserAction openFileChooserAction = new OpenFileChooserAction("Select Expression matrix file", true);
        openFileChooserAction.actionPerformed(e);
        if (openFileChooserAction.getSelectedFile() != null)
            parentDir = openFileChooserAction.getSelectedFile();
        if (parentDir != null) {
            if (!parentDir.exists())
                showMessageDialog("Selected directory does not exist!", JOptionPane.WARNING_MESSAGE);
            else {
                jl_chosenParentDir.setText(parentDir.getName());
                jl_chosenParentDir.setToolTipText(parentDir.getAbsolutePath());
            }
        }
        enableButtons();
    }

    private void jtxt_iterationTitleActionPerformed() {
        String newTitle = jtxt_iterationTitle.getText();
        boolean setNew = false;
        if(newTitle.equals("")) {
            showMessageDialog("The iteration title should not be empty!", JOptionPane.OK_OPTION);
        } else if (newTitle.contains(" ")){
            showMessageDialog("The iteration title should not contain spaces!", JOptionPane.OK_OPTION);
        } else if (newTitle.contains("/") || newTitle.contains("\\") || newTitle.contains(":")){
            showMessageDialog("Invalid character found in iteration title, characters \"/, \\, :\" " +
                    "are not allowed", JOptionPane.OK_OPTION);
        } else {
            setNew = true;
        }

        if(setNew)
            iterationTitle = newTitle;
        else
            jtxt_iterationTitle.setText(iterationTitle);
    }

    private void jrb_predictionModeActionPerformed() {
        enableButtons();
    }

    private void jrb_validationModeActionPerformed() {
        enableButtons();
    }

    private void jb_generateTMMLabelsActionPerformed(ActionEvent e) {

    }

    private void jb_chooseTMMLabelsActionPerformed(ActionEvent e) {

    }

    private void jb_editTMMLabelsActionPerformed(ActionEvent e) {

    }

    private void jb_addFCActionPerformed(ActionEvent e) {
        RunPipelineAction runPipelineAction =
                new RunPipelineAction("Update FC values", this, true, false, false);
        runPipelineAction.actionPerformed(e);
    }

    private void jb_runPSFActionPerformed(ActionEvent e) {
        RunPipelineAction runPipelineAction =
                new RunPipelineAction("Run PSF", this, false, true, false);
        runPipelineAction.actionPerformed(e);
    }

    private void jb_generateReportActionPerformed(ActionEvent e) {
        RunPipelineAction runPipelineAction =
                new RunPipelineAction("Run PSF", this, false, false, true);
        runPipelineAction.actionPerformed(e);
    }

    private void jb_runAllActionPerformed(ActionEvent e) {
        RunPipelineAction runPipelineAction =
                new RunPipelineAction("Run PSF", this, true, true, true);
        runPipelineAction.actionPerformed(e);
    }

    private void jb_saveSettingsActionPerformed() {
        tmmProps = TMMActivator.getTMMProps();
        try {
            if (expMatFile != null)
                tmmProps.setProperty(ETMMProps.EXPMATFILE.getName(), expMatFile.getAbsolutePath());
            if (parentDir != null)
                tmmProps.setProperty(ETMMProps.PARENTDIR.getName(), parentDir.getAbsolutePath());
            if (jcb_geneID.getSelectedItem() != null)
                tmmProps.setProperty(ETMMProps.GENEID.getName(), jcb_geneID.getSelectedItem().toString());
            if (!jtxt_iterationTitle.getText().equals(""))
                tmmProps.setProperty(ETMMProps.ITERATION.getName(), jtxt_iterationTitle.getText());
            if (!jtxt_comment.getText().equals(""))
                tmmProps.setProperty(ETMMProps.COMMENT.getName(), jtxt_comment.getText());
        } catch (Exception e) {
            String message = "Couldn't save the settings. Error: "
                    + e.getMessage() + " Cause: " + e.getCause();
            TMMActivator.getLogger().warn(message);
            System.out.println("TMM:: " + message);
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(TMMActivator.getTMMPropsFile());
            TMMActivator.getTMMProps().store(outputStream, "TMM property file");
            outputStream.close();
        } catch (FileNotFoundException e) {
            TMMActivator.getLogger().error("Could not write to tmm.props file. Reason: " + e.getMessage(), e);
        } catch (IOException e) {
            TMMActivator.getLogger().error("Could not write to tmm.props file. Reason: " + e.getMessage(), e);
        }
    }

    private void showMessageDialog(String message, int option) {
        JOptionPane.showMessageDialog(TMMActivator.cytoscapeDesktopService.getJFrame(), message, "TMM message dialog", option);
    }

    private CyNetwork getCurrentNetwork() {
        return TMMActivator.cyApplicationManager.getCurrentNetwork();
    }

    public Component getPSFCPanel() {

        if (this.psfcPanel != null)
            return psfcPanel;
        CytoPanel westPanel = TMMActivator.cytoscapeDesktopService.getCytoPanel(CytoPanelName.WEST);
        int i = 0;
        for (; i < westPanel.getCytoPanelComponentCount(); i++) {
            Component component = westPanel.getComponentAt(i);
            if (component.getName() != null)
                if (component.getName().equals("PSFC_1.1.3")) {
                    psfcPanel = westPanel.getComponentAt(i);
                    break;
                }
        }


        if (psfcPanel == null)
            System.out.println("PSFC 1.1.3 Panel not found!");
        else
            System.out.println("PSFC 1.1.3 component found: " + psfcPanel.getName());
        return psfcPanel;
    }

    public File getParentDir(){
        return parentDir;
    }

    public File getExpMatFile(){
        return expMatFile;
    }

    public String getIterationTitle(){
        return iterationTitle;
    }

    public String getCommentText() {
        return jtxt_comment.getText();
    }

    public String getGeneIDName(){
        String geneIDName = jcb_geneID.getSelectedItem().toString();
        if(getCurrentNetwork().getDefaultNodeTable().getColumn(geneIDName) == null)
            return null;
        return  geneIDName;
    }

    private void initComponents() {
        jp_filesAndTitles = new javax.swing.JPanel();
        jl_chosenExpMatFile = new javax.swing.JLabel();
        jl_parentDir = new javax.swing.JLabel();
        jl_chosenParentDir = new javax.swing.JLabel();
        jtxt_iterationTitle = new javax.swing.JTextField();
        jl_iterationTitle = new javax.swing.JLabel();
        jb_browseParentDir = new javax.swing.JButton();
        jb_chooseExpMatFile = new javax.swing.JButton();
        jl_expMat = new javax.swing.JLabel();
        jl_comment = new javax.swing.JLabel();
        jsp_comment = new javax.swing.JScrollPane();
        jtxt_comment = new javax.swing.JTextArea();
        jcb_geneID = new javax.swing.JComboBox<>();
        jl_geneID = new javax.swing.JLabel();
        jp_modeAndLabels = new javax.swing.JPanel();
        jrb_validationMode = new javax.swing.JRadioButton();
        jrb_predictionMode = new javax.swing.JRadioButton();
        jl_tmmLabels = new javax.swing.JLabel();
        jb_generateTMMLabels = new javax.swing.JButton();
        jb_chooseTMMLabels = new javax.swing.JButton();
        jb_editTMMLabels = new javax.swing.JButton();
        jp_run = new javax.swing.JPanel();
        jb_runAll = new javax.swing.JButton();
        jb_addFC = new javax.swing.JButton();
        jb_runPSF = new javax.swing.JButton();
        jb_generateReport = new javax.swing.JButton();
        jb_UserGuide = new javax.swing.JButton();
        jb_saveSettings = new javax.swing.JButton();


        jp_filesAndTitles.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jl_chosenExpMatFile.setText("Chosen file");
        jl_chosenExpMatFile.setEnabled(false);

        jl_parentDir.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_parentDir.setForeground(new java.awt.Color(21, 140, 186));
        jl_parentDir.setText("Parent directory");

        jl_chosenParentDir.setText("Chosen folder");
        jl_chosenParentDir.setEnabled(false);

        jtxt_iterationTitle.setText("Untitled_iteration");

        jl_iterationTitle.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_iterationTitle.setForeground(new java.awt.Color(21, 140, 186));
        jl_iterationTitle.setText("Iteration title");

        jb_browseParentDir.setText("Browse");

        jb_chooseExpMatFile.setText("Choose");

        jl_expMat.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_expMat.setForeground(new java.awt.Color(21, 140, 186));
        jl_expMat.setText("Exp matrix");

        jl_comment.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_comment.setForeground(new java.awt.Color(21, 140, 186));
        jl_comment.setText("Add a comment to the iteration");

        jtxt_comment.setColumns(20);
        jtxt_comment.setRows(5);
        jsp_comment.setViewportView(jtxt_comment);

        jl_geneID.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_geneID.setForeground(new java.awt.Color(21, 140, 186));
        jl_geneID.setText("Gene ID");

        javax.swing.GroupLayout jp_filesAndTitlesLayout = new javax.swing.GroupLayout(jp_filesAndTitles);
        jp_filesAndTitles.setLayout(jp_filesAndTitlesLayout);
        jp_filesAndTitlesLayout.setHorizontalGroup(
                jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jsp_comment)
                                        .addComponent(jl_comment)
                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                .addComponent(jl_parentDir)
                                                .addGap(18, 18, 18)
                                                .addComponent(jb_browseParentDir, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(26, 26, 26)
                                                .addComponent(jl_chosenParentDir, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jl_expMat)
                                                        .addComponent(jl_iterationTitle)
                                                        .addComponent(jl_geneID))
                                                .addGap(38, 38, 38)
                                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                                .addComponent(jb_chooseExpMatFile, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(26, 26, 26)
                                                                .addComponent(jl_chosenExpMatFile, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addComponent(jtxt_iterationTitle)
                                                        .addComponent(jcb_geneID, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        jp_filesAndTitlesLayout.setVerticalGroup(
                jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jb_chooseExpMatFile)
                                        .addComponent(jl_chosenExpMatFile)
                                        .addComponent(jl_expMat))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jcb_geneID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jl_geneID))
                                .addGap(18, 18, 18)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jl_parentDir)
                                        .addComponent(jb_browseParentDir)
                                        .addComponent(jl_chosenParentDir))
                                .addGap(18, 18, 18)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jl_iterationTitle)
                                        .addComponent(jtxt_iterationTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jl_comment)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_comment, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                                .addContainerGap())
        );

        jp_modeAndLabels.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jrb_validationMode.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        jrb_validationMode.setForeground(new java.awt.Color(102, 103, 114));
        jrb_validationMode.setText("Validation mode");

        jrb_predictionMode.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        jrb_predictionMode.setForeground(new java.awt.Color(255, 102, 153));
        jrb_predictionMode.setText("Prediction mode");

        jl_tmmLabels.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_tmmLabels.setForeground(new java.awt.Color(102, 103, 114));
        jl_tmmLabels.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jl_tmmLabels.setText("Indicate TMM state labels");

        jb_generateTMMLabels.setText("Generate file");

        jb_chooseTMMLabels.setText("Choose file");
        jb_chooseTMMLabels.setPreferredSize(new java.awt.Dimension(105, 25));

        jb_editTMMLabels.setText("Edit file");
        jb_editTMMLabels.setPreferredSize(new java.awt.Dimension(105, 25));

        javax.swing.GroupLayout jp_modeAndLabelsLayout = new javax.swing.GroupLayout(jp_modeAndLabels);
        jp_modeAndLabels.setLayout(jp_modeAndLabelsLayout);
        jp_modeAndLabelsLayout.setHorizontalGroup(
                jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                                .addGap(78, 78, 78)
                                                .addComponent(jl_tmmLabels, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jb_generateTMMLabels)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jb_chooseTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jb_editTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_modeAndLabelsLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jrb_validationMode)
                                        .addComponent(jrb_predictionMode))
                                .addGap(203, 203, 203))
        );
        jp_modeAndLabelsLayout.setVerticalGroup(
                jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jrb_predictionMode)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jrb_validationMode)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jl_tmmLabels)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jb_generateTMMLabels)
                                        .addComponent(jb_chooseTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jb_editTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jp_run.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jb_runAll.setBackground(new java.awt.Color(255, 102, 153));
        jb_runAll.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jb_runAll.setText("Run all at once");

        jb_addFC.setBackground(new java.awt.Color(21, 140, 186));
        jb_addFC.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jb_addFC.setText("Add/update fold change values");

        jb_runPSF.setBackground(new java.awt.Color(21, 140, 186));
        jb_runPSF.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jb_runPSF.setText("Run PSF");

        jb_generateReport.setBackground(new java.awt.Color(21, 140, 186));
        jb_generateReport.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jb_generateReport.setText("Generate report");

        javax.swing.GroupLayout jp_runLayout = new javax.swing.GroupLayout(jp_run);
        jp_run.setLayout(jp_runLayout);
        jp_runLayout.setHorizontalGroup(
                jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_runLayout.createSequentialGroup()
                                .addGap(52, 52, 52)
                                .addGroup(jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jb_addFC, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                                        .addComponent(jb_runPSF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jb_generateReport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jb_runAll, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_runLayout.setVerticalGroup(
                jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_runLayout.createSequentialGroup()
                                .addContainerGap(22, Short.MAX_VALUE)
                                .addComponent(jb_addFC)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jb_runPSF)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jb_generateReport)
                                .addGap(18, 18, 18)
                                .addComponent(jb_runAll)
                                .addContainerGap())
        );

        jb_UserGuide.setBackground(new java.awt.Color(21, 140, 186));
        jb_UserGuide.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jb_UserGuide.setForeground(new java.awt.Color(102, 103, 114));
        jb_UserGuide.setText("User Guide");
        jb_UserGuide.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(21, 140, 186), 1, true));

        jb_saveSettings.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jb_saveSettings.setText("Save settings");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jp_filesAndTitles, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jp_modeAndLabels, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                        .addComponent(jp_run, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(74, 74, 74)
                                                .addComponent(jb_UserGuide, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(92, 92, 92)
                                                .addComponent(jb_saveSettings, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jp_filesAndTitles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jp_modeAndLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jp_run, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jb_saveSettings)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 57, Short.MAX_VALUE)
                                .addComponent(jb_UserGuide, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
    }

    // Variables declaration - do not modify
    private javax.swing.JButton jb_UserGuide;
    private javax.swing.JButton jb_addFC;
    private javax.swing.JButton jb_browseParentDir;
    private javax.swing.JButton jb_chooseExpMatFile;
    private javax.swing.JButton jb_chooseTMMLabels;
    private javax.swing.JButton jb_editTMMLabels;
    private javax.swing.JButton jb_generateReport;
    private javax.swing.JButton jb_generateTMMLabels;
    private javax.swing.JButton jb_runAll;
    private javax.swing.JButton jb_runPSF;
    private javax.swing.JButton jb_saveSettings;
    private javax.swing.JComboBox<String> jcb_geneID;
    private javax.swing.JLabel jl_chosenExpMatFile;
    private javax.swing.JLabel jl_chosenParentDir;
    private javax.swing.JLabel jl_comment;
    private javax.swing.JLabel jl_expMat;
    private javax.swing.JLabel jl_geneID;
    private javax.swing.JLabel jl_iterationTitle;
    private javax.swing.JLabel jl_parentDir;
    private javax.swing.JLabel jl_tmmLabels;
    private javax.swing.JPanel jp_filesAndTitles;
    private javax.swing.JPanel jp_modeAndLabels;
    private javax.swing.JPanel jp_run;
    private javax.swing.JRadioButton jrb_predictionMode;
    private javax.swing.JRadioButton jrb_validationMode;
    private javax.swing.JScrollPane jsp_comment;
    private javax.swing.JTextArea jtxt_comment;
    private javax.swing.JTextField jtxt_iterationTitle;


    // End of variables declaration
}
