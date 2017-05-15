package org.cytoscape.tmm.gui;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.tmm.Enums.ETMMProps;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.actions.*;
import org.cytoscape.tmm.processing.ExpMatFileHandler;
import org.cytoscape.tmm.processing.ParsedFilesDirectory;
import org.cytoscape.tmm.reports.TMMLabels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
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
    private String iterationTitle = "Untitled_iteration";
    private ArrayList<String> samples;
    private File fcFile;
    private int bootCycles;
    private boolean runPSFDone;
    private boolean addFCDone;
    private boolean generateReportDone;
    private File summaryFile;
    private File reportFile;
    private File tmmLabelsFile;
    private ExpMatFileHandler expMatFileHandler;
    private boolean expMatFileValid = false;
    private ParsedFilesDirectory parsedFilesDirectory;
    private boolean editingInput = true;
    private ImageIcon refreshIcon;
    private String refreshIconName = "refresh_button.png";

    public TMMPanel() throws Exception {
        if (getPSFCPanel() == null) {
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

    private void showMessageDialog(String message, int option) {
        JOptionPane.showMessageDialog(TMMActivator.cytoscapeDesktopService.getJFrame(),
                message, "TMM message dialog", option);
    }

    private int showConfirmDialog(String message, int option) {
        int answer = JOptionPane.showConfirmDialog(TMMActivator.cytoscapeDesktopService.getJFrame(),
                message, "TMM message dialog", option);
        return answer;
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

    public File getParentDir() {
        return parentDir;
    }

    public File getExpMatFile() {
        return expMatFile;
    }

    public ParsedFilesDirectory getParsedFilesDirectory() {
        return parsedFilesDirectory;
    }

    public void setParsedFilesDirectory(ParsedFilesDirectory parsedFilesDirectory) {
        this.parsedFilesDirectory = parsedFilesDirectory;
    }


    public File getTmmLabelsFile() {
        return tmmLabelsFile;
    }

    public boolean isValidationMode() {
        return jrb_validationMode.isSelected();
    }

    public String getIterationTitle() {
        return jtxt_iterationTitle.getText();
    }

    public String getCommentText() {
        return jtxt_comment.getText();
    }


    public void setExpMatFileValid(boolean expMatFileValid) {
        this.expMatFileValid = expMatFileValid;
    }

    public String getGeneIDName() {
        String geneIDName = jcb_geneID.getSelectedItem() != null ? jcb_geneID.getSelectedItem().toString() : "";
        if (getCurrentNetwork().getDefaultNodeTable().getColumn(geneIDName) == null)
            return null;
        return geneIDName;
    }

    public void setSamples(ArrayList<String> samples) {
        this.samples = samples;
        jcb_samples.setModel(new DefaultComboBoxModel(samples.toArray()));
        enableButtons();
    }

    public void setSummaryFile(File summaryFile) {
        this.summaryFile = summaryFile;
    }

    public File getSummaryFile() {
        return summaryFile;
    }

    public void setReportFile(File reportFile) {
        this.reportFile = reportFile;
    }

    public File getReportFile() {
        return reportFile;
    }

    public void setFcFile(File fcFile) {
        this.fcFile = fcFile;
    }

    public File getFCFile() {
        return fcFile;
    }

    public int getBootCycles() {
        return Integer.parseInt(jtxt_bootCycles.getText());
    }

    public void setRunPSFDone(boolean runPSFDone) {
        this.runPSFDone = runPSFDone;
        if (runPSFDone)
            jb_runPSF.setBackground(new Color(0, 188, 49));
        else
            jb_runPSF.setBackground(new Color(21, 140, 186));
    }

    public void setAddFCDone(boolean addFCDone) {
        this.addFCDone = addFCDone;
        if (addFCDone)
            jb_addFC.setBackground(new Color(0, 188, 49));
        else
            jb_addFC.setBackground(new Color(21, 140, 186));
    }

    public boolean isGenerateReportDone() {
        return generateReportDone;
    }

    public void setGenerateReportDone(boolean generateReportDone) {
        this.generateReportDone = generateReportDone;
        if (generateReportDone) {
            jb_generateReport.setBackground(new Color(0, 188, 49));
            jb_runAll.setBackground(new Color(0, 188, 49));
        } else {
            jb_generateReport.setBackground(new Color(21, 140, 186));
            jb_runAll.setBackground(new Color(21, 140, 186));
        }
    }

    public boolean isRunPSFDone() {
        return runPSFDone;
    }

    public boolean isAddFCDone() {
        return addFCDone;
    }

    public void enableButtons() {
        boolean enable = true;
        boolean inputsValid = true;

        if (editingInput && parsedFilesDirectory != null) {
            inputsValid = false;
            //loop throgh tooltiperrors in parsefilesdirectory and set tooltips and colors
            if (expMatFile == null || !expMatFile.exists()) {
                enable = false;
                jb_chooseExpMatFile.setBackground(new java.awt.Color(255, 0, 0));
            } else {
                String error = parsedFilesDirectory.getErrorToolTip(ParsedFilesDirectory.EXPMATFILE);
                if (error == null) {
                    jb_chooseExpMatFile.setBackground(new Color(0, 188, 49));
                    jl_chosenExpMatFile.setToolTipText(expMatFile.getAbsolutePath());
                } else {
                    jb_chooseExpMatFile.setBackground(new java.awt.Color(255, 0, 0));
                    jb_chooseExpMatFile.setToolTipText(error);
                }
                String geneIdValid = parsedFilesDirectory.getErrorToolTip(ParsedFilesDirectory.GENEID);

                if (geneIdValid == null) {
                    jcb_geneID.setBorder(javax.swing.BorderFactory.createEtchedBorder(new Color(0, 188, 49), null));
                    jcb_geneID.setToolTipText("");
                } else {
                    enable = false;
                    jcb_geneID.setToolTipText(geneIdValid);
                    jcb_geneID.setBorder(javax.swing.BorderFactory.createEtchedBorder(new Color(255, 0, 0), null));
                }
            }
            if (parentDir == null || !parentDir.exists()) {
                enable = false;
                jb_browseParentDir.setBackground(new Color(255, 0, 0));
            } else {
                String error = parsedFilesDirectory.getErrorToolTip(ParsedFilesDirectory.PARENTDIR);
                if (error == null) {
                    jb_browseParentDir.setBackground(new Color(0, 188, 49));
                    jl_parentDir.setToolTipText(parentDir.getAbsolutePath());
                } else {
                    jb_browseParentDir.setBackground(new Color(255, 0, 0));
                    jb_browseParentDir.setToolTipText(error);
                }
            }
            String error = parsedFilesDirectory.getErrorToolTip(ParsedFilesDirectory.ITERATIONTITLE);
            if (error == null) {
                jtxt_iterationTitle.setBorder(BorderFactory.createLineBorder(new Color(0, 188, 49)));
                jtxt_iterationTitle.setToolTipText("");
            } else {
                jtxt_iterationTitle.setBorder(BorderFactory.createLineBorder(new Color(255, 0, 0)));
                jtxt_iterationTitle.setToolTipText(error);
            }
        } else {
            jb_chooseExpMatFile.setBackground(new Color(0, 188, 49));
            jb_chooseExpMatFile.setToolTipText(expMatFile.getAbsolutePath());
            jcb_geneID.setBorder(javax.swing.BorderFactory.createEtchedBorder(new Color(0, 188, 49), null));
            jcb_geneID.setToolTipText("");
            jb_browseParentDir.setBackground(new Color(0, 188, 49));
            jb_browseParentDir.setToolTipText(parentDir.getAbsolutePath());
            jtxt_iterationTitle.setBorder(BorderFactory.createLineBorder(new Color(0, 188, 49)));
            jtxt_iterationTitle.setToolTipText("");
        }


        jb_browseParentDir.setEnabled(editingInput);
        jb_chooseExpMatFile.setEnabled(editingInput);
        jcb_geneID.setEnabled(editingInput);
        jb_refresh.setEnabled(editingInput);
        jtxt_iterationTitle.setEnabled(editingInput);
        jtxt_comment.setEnabled(editingInput);
        jb_edit.setEnabled(!editingInput);
        jb_done.setEnabled(editingInput);
        jrb_predictionMode.setEnabled(!editingInput);
        jrb_validationMode.setEnabled(!editingInput);
        jb_generateTMMLabels.setEnabled(!editingInput);
        jb_chooseTMMLabels.setEnabled(!editingInput);
        jb_editTMMLabels.setEnabled(!editingInput);
        jb_saveSettings.setEnabled(!editingInput);
        jb_addFC.setEnabled(!editingInput);
        jb_runPSF.setEnabled(!editingInput);
        jb_generateReport.setEnabled(!editingInput);
        jb_openReport.setEnabled(!editingInput);
        jb_runAll.setEnabled(!editingInput);

        if (!editingInput) {

            boolean enableValidationButtons = jrb_validationMode.isSelected();
            jb_generateTMMLabels.setEnabled(enableValidationButtons);
            jb_chooseTMMLabels.setEnabled(enableValidationButtons);
            jb_editTMMLabels.setEnabled(enableValidationButtons
                    && tmmLabelsFile != null && tmmLabelsFile.exists());

            jb_addFC.setEnabled(enable);
            jb_runPSF.setEnabled(enable);
            if (jrb_validationMode.isSelected() && tmmLabelsFile == null) {
                jb_generateReport.setEnabled(false);
                jb_runAll.setEnabled(false);
            } else {
                jb_generateReport.setEnabled(enable);
                jb_runAll.setEnabled(enable);
            }

            jb_runPSF.setEnabled(addFCDone);
            jb_generateReport.setEnabled(runPSFDone);
            jb_openReport.setEnabled(generateReportDone);

            boolean enablesamples = (samples != null);
            jcb_samples.setEnabled(enablesamples);
            jb_viz.setEnabled(enablesamples);
        } else {
            jrb_predictionMode.setEnabled(false);
            jrb_validationMode.setEnabled(false);
            jb_generateTMMLabels.setEnabled(false);
            jb_chooseTMMLabels.setEnabled(false);
            jb_editTMMLabels.setEnabled(false);
            jb_saveSettings.setEnabled(false);
            jb_addFC.setEnabled(false);
            jb_runPSF.setEnabled(false);
            jb_generateReport.setEnabled(false);
            jb_openReport.setEnabled(false);
            jb_runAll.setEnabled(false);
            jcb_samples.setEnabled(false);
            jb_viz.setEnabled(false);
        }
    }


    private void setModels() {
        setjcb_geneIDModel();

    }

    private void setToolTips() {
        jl_expMat.setToolTipText("The expression file (see the manual for the format)");
        jl_parentDir.setToolTipText("The directory where the iteration folder should be placed");
        jb_refresh.setToolTipText("Click to refresh network and table attributes");
        jb_viz.setToolTipText("Click to visualize PSF values of selected sample. " +
                "\nIf nothing happens try refreshing network view, or restarting Cytoscape. " +
                "\nWe are sorry if this appears annoying, but life is life.");
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
            jtxt_iterationTitle.setText(getIterationTitle());
        }
        //jb_refreshbutton
        jb_refresh.setIcon(getRefreshIcon());

        // comment
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.COMMENT.getName());
        if (property != null && !property.equals("")) {
            jtxt_comment.setText(property);
        }

        jb_done.setEnabled(true);
        jb_edit.setEnabled(false);
        // validation
        ButtonGroup modeButtons = new ButtonGroup();
        modeButtons.add(jrb_predictionMode);
        modeButtons.add(jrb_validationMode);

        property = (String) TMMActivator.getTMMProps().get(ETMMProps.VALIDATIONMODE.getName());
        if (property != null && property.equals("true")) {
            jrb_validationMode.setSelected(true);
            property = (String) TMMActivator.getTMMProps().get(ETMMProps.TMMLABELSFILE.getName());
            if (property != null && new File(property).exists())
                this.tmmLabelsFile = new File(property);
        } else
            jrb_predictionMode.setSelected(true);

        // tmmlabel
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.TMMLABELSFILE.getName());
        if (property != null && !property.equals("")) {
            try {
                tmmLabelsFile = new File(property);
                if (!tmmLabelsFile.exists()) {
                    setTmmLabelsFile(null);
                    tmmLabelsFile = null;
                } else {
                    setTmmLabelsFile(tmmLabelsFile);
                }
            } catch (Exception e) {
                System.out.println("TMM::Could not load tmm labels file " + property + " " + e.getMessage());
            }
        }

        //boot cycles
        property = (String) TMMActivator.getTMMProps().get(ETMMProps.BOOTCYCLES.getName());
        try {
            bootCycles = Integer.parseInt(property);
        } catch (NumberFormatException e) {
            bootCycles = 200;
        }
        jtxt_bootCycles.setText(bootCycles + "");
    }

    private ImageIcon getRefreshIcon() {
        if (refreshIcon == null) {
            ClassLoader cl = TMMActivator.class.getClassLoader();
            refreshIcon = new ImageIcon(cl.getResource(refreshIconName));
        }
        return refreshIcon;
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

        jcb_geneID.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableButtons();
            }
        });

        jb_refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_refreshActionPerformed(e);
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

        jb_edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_editActionPerformed(e);
            }
        });

        jb_done.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_doneActionPerformed(e);
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

        jtxt_bootCycles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jtxt_bootCyclesActionPerformed();
            }
        });

        jb_generateReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_generateReportActionPerformed(e);
            }
        });

        jb_openReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_openReportActionPerformed(e);
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

        jb_viz.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_vizActionPerformed(e);
            }
        });

        jb_webpage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jb_webpageActionPerformed(e);
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
        File selectedFile = null;
        if (openFileChooserAction.getSelectedFile() != null)
            selectedFile = openFileChooserAction.getSelectedFile();
        if (selectedFile != null) {
            if (!selectedFile.exists())
                showMessageDialog("Selected file does not exist!",
                        JOptionPane.ERROR_MESSAGE);
            else {
                expMatFile = selectedFile;
                jl_chosenExpMatFile.setText(expMatFile.getName());
                jl_chosenExpMatFile.setToolTipText(expMatFile.getAbsolutePath());
            }
        }
        enableButtons();
    }

    private void jb_refreshActionPerformed(ActionEvent e){
        setjcb_geneIDModel();
        enableButtons();
    }

    private void jb_browseParentDirActionPerformed(ActionEvent e) {
        File prevDir = null;
        if (parentDir != null)
            prevDir = parentDir;
        OpenFileChooserAction openFileChooserAction =
                new OpenFileChooserAction("Select Expression matrix file", true);
        openFileChooserAction.actionPerformed(e);
        File selectedDir = null;
        if (openFileChooserAction.getSelectedFile() != null)
            selectedDir = openFileChooserAction.getSelectedFile();
        if (selectedDir != null) {
            if (!selectedDir.exists())
                showMessageDialog("Selected directory does not exist!", JOptionPane.WARNING_MESSAGE);
            else {
                parentDir = selectedDir;
                jl_chosenParentDir.setText(parentDir.getName());
                jl_chosenParentDir.setToolTipText(parentDir.getAbsolutePath());
                if (!parentDir.getAbsolutePath().equals(prevDir.getAbsolutePath())) {
                    addFCDone = runPSFDone = generateReportDone = false;
                }
            }
        }
        enableButtons();
    }

    private void jtxt_iterationTitleActionPerformed() {
        String newTitle = jtxt_iterationTitle.getText();
        boolean setNew = false;
        if (newTitle.equals("")) {
            showMessageDialog("The iteration title should not be empty!", JOptionPane.OK_OPTION);
        } else if (newTitle.contains(" ")) {
            showMessageDialog("The iteration title should not contain spaces!", JOptionPane.OK_OPTION);
        } else if (newTitle.contains("/") || newTitle.contains("\\") || newTitle.contains(":")) {
            showMessageDialog("Invalid character found in iteration title, characters \"/, \\, :\" " +
                    "are not allowed", JOptionPane.OK_OPTION);
        } else {
            setNew = true;
        }

        if (setNew)
            iterationTitle = newTitle;
        else
            jtxt_iterationTitle.setText(iterationTitle);
    }


    private void jb_editActionPerformed(ActionEvent e) {
        editingInput = true;
        enableButtons();
    }

    private void jb_doneActionPerformed(ActionEvent e) {
        if (getCurrentNetwork() == null)
            showMessageDialog("No network loaded. Please, import a TMM network first. " +
                    "\n You may download it from the project webpage. ", JOptionPane.ERROR_MESSAGE);
        ParseInputFilesAction parseInputFilesAction =
                new ParseInputFilesAction("Parsing input files", this, e);
        parseInputFilesAction.actionPerformed(e);
        while (!parseInputFilesAction.isFinished()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        if (!parseInputFilesAction.isAllValid()) {
            editingInput = true;
            showMessageDialog("One or more inputs (highlighted in red) were not valid\n\n" +
                    "Hover over the buttons/labels to see the problem.", JOptionPane.ERROR_MESSAGE);
        } else
            editingInput = false;
        enableButtons();

    }

    private void jrb_predictionModeActionPerformed() {
        enableButtons();
    }

    private void jrb_validationModeActionPerformed() {
        String property = (String) TMMActivator.getTMMProps().get(ETMMProps.TMMLABELSFILE.getName());
        if (property != null && new File(property).exists()) {
            this.tmmLabelsFile = new File(property);
            setTmmLabelsFile(tmmLabelsFile);
        } else {
            setTmmLabelsFile(null);
        }
        enableButtons();
    }

    private void setTmmLabelsFile(File tmmLabelsFile) {
        if (tmmLabelsFile != null) {
            jl_tmmLabelsFile.setText(tmmLabelsFile.getName());
            jl_tmmLabelsFile.setToolTipText(tmmLabelsFile.getAbsolutePath());
            jl_tmmLabelsFile.setForeground(new Color(0, 188, 49));
        } else {
            jl_tmmLabelsFile.setText("No file chosen");
            jl_tmmLabelsFile.setForeground(new Color(255, 0, 0));
        }

    }

    private void jb_generateTMMLabelsActionPerformed(ActionEvent e) {
        if (samples == null) {
            samples = parsedFilesDirectory.getExpMatFileHandler().getSamples();
            System.out.println(" samples was null: replaced from parsedFilesDirecotyr. " +
                    "this should not have occcured though.");
        }
        OpenFileChooserAction openFileChooserAction =
                new OpenFileChooserAction("Choose TMM labels filename", false);
        openFileChooserAction.actionPerformed(e);
        tmmLabelsFile = openFileChooserAction.getSelectedFile();

        if (tmmLabelsFile.exists()) {
            String valid = TMMLabels.isLabelsFileValid(tmmLabelsFile, samples);
            int answer;
            if (valid.equals("true")) {
                answer = showConfirmDialog("The TMM Labels file " + tmmLabelsFile.getAbsolutePath()
                                + " is already specified. \nDo you want to remove it?",
                        JOptionPane.YES_NO_OPTION);
            } else {
                answer = showConfirmDialog("The file "
                                + tmmLabelsFile.getAbsolutePath()
                                + " already exists, but is not valid. Reason: " +
                                valid + "Do you want to remove it? ",
                        JOptionPane.YES_NO_OPTION);
            }
            if (answer == JOptionPane.YES_OPTION) {
                boolean success = tmmLabelsFile.delete();
                if (!success) {
                    showMessageDialog("Cannot delete the file " + tmmLabelsFile.getAbsolutePath()
                            + ". Maybe the file is in use.", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                return;
            }
        }
        try {
            TMMLabels.generateFile(tmmLabelsFile, samples);
            setTmmLabelsFile(tmmLabelsFile);
            enableButtons();
        } catch (Exception e1) {
            tmmLabelsFile.delete();
            showMessageDialog("Could not generate TMM Labels file: " +
                    e1.getMessage(), JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            jb_editTMMLabelsActionPerformed(e);
        } catch (Exception e2) {
            showMessageDialog("Could not open file " + tmmLabelsFile.getAbsolutePath()
                    + " for editing. Reason: " + e2.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }


    private void jb_chooseTMMLabelsActionPerformed(ActionEvent e) {
        if (samples == null) {
            RunPipelineAction runPipelineAction = new RunPipelineAction("Parsing input files",
                    this, false, false, false);
            runPipelineAction.actionPerformed(e);
            while (runPipelineAction.parsing()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        OpenFileChooserAction openFileChooserAction =
                new OpenFileChooserAction("Select TMM Labels file", false);
        openFileChooserAction.actionPerformed(e);
        File chosenFile = openFileChooserAction.getSelectedFile();
        if (chosenFile == null)
            return;
        String valid = TMMLabels.isLabelsFileValid(chosenFile, samples);
        if (valid.equals("true")) {
            tmmLabelsFile = chosenFile;
            setTmmLabelsFile(tmmLabelsFile);
            enableButtons();
        } else {
            showMessageDialog("TMM Labels file is not valid: \n"
                            + valid + "\nFile:" + chosenFile.getAbsolutePath(),
                    JOptionPane.ERROR_MESSAGE);
            setTmmLabelsFile(null);
            jl_tmmLabelsFile.setToolTipText("File was not valid: " + valid);
        }
    }

    private void jb_editTMMLabelsActionPerformed(ActionEvent e) {
        if (!tmmLabelsFile.exists()) {
            showMessageDialog("TMM Labels file is not specified", JOptionPane.ERROR_MESSAGE);
            return;
        }
        OpenFileAction openFileAction = new OpenFileAction(tmmLabelsFile.getAbsolutePath());
        openFileAction.actionPerformed(e);
    }

    private void jb_addFCActionPerformed(ActionEvent e) {
        RunPipelineAction runPipelineAction =
                new RunPipelineAction("Update FC values", this, true, false, false);
        runPipelineAction.actionPerformed(e);
        enableButtons();
    }

    private void jb_runPSFActionPerformed(ActionEvent e) {
        if (samples == null)
            JOptionPane.showMessageDialog(TMMActivator.cytoscapeDesktopService.getJFrame(),
                    "Cannot run PSF: run Add/update FC first");
        else {
            RunPipelineAction runPipelineAction =
                    new RunPipelineAction("Run PSF", this, false, true, false);
            runPipelineAction.setSamples(samples);
            int bootCycles = Integer.parseInt(jtxt_bootCycles.getText());
            runPipelineAction.setBootCycles(bootCycles);
            runPipelineAction.actionPerformed(e);
        }
    }

    private void jtxt_bootCyclesActionPerformed() {
        int bootCycles;
        int bootCyclesDefault = 200;
        try {
            bootCycles = Integer.parseInt(jtxt_bootCycles.getText());
            if (bootCycles < 1)
                bootCycles = bootCyclesDefault;
        } catch (NumberFormatException e) {
            bootCycles = bootCyclesDefault;
        }
        jtxt_bootCycles.setText(bootCycles + "");
    }

    private void jb_generateReportActionPerformed(ActionEvent e) {
        RunPipelineAction runPipelineAction =
                new RunPipelineAction("Run PSF", this, false, false, true);
        runPipelineAction.actionPerformed(e);
    }

    private void jb_openReportActionPerformed(ActionEvent e) {
        OpenFileAction openReportAction = new OpenFileAction(reportFile.getAbsolutePath());
        openReportAction.actionPerformed(e);

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
            if (jrb_validationMode.isSelected()) {
                tmmProps.setProperty(ETMMProps.VALIDATIONMODE.getName(), "true");
                if (tmmLabelsFile != null && tmmLabelsFile.exists())
                    tmmProps.setProperty(ETMMProps.TMMLABELSFILE.getName(), tmmLabelsFile.getAbsolutePath());
            } else {
                tmmProps.setProperty(ETMMProps.VALIDATIONMODE.getName(), "false");
            }
            tmmProps.setProperty(ETMMProps.BOOTCYCLES.getName(), jtxt_bootCycles.getText());
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

    private void jb_vizActionPerformed(ActionEvent e) {
        String sample = jcb_samples.getSelectedItem().toString();
        VizSampleAction vizSampleAction = new VizSampleAction(
                "Visualise sample PSF task", this, sample);
        vizSampleAction.actionPerformed(e);
    }

    private void jb_webpageActionPerformed(ActionEvent e){
        WebLoadAction webLoadAction = new WebLoadAction(TMMActivator.getWebPageUrl());
        webLoadAction.actionPerformed(e);
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
        jb_done = new javax.swing.JButton();
        jb_edit = new javax.swing.JButton();
        jb_refresh = new javax.swing.JButton();
        jp_modeAndLabels = new javax.swing.JPanel();
        jrb_validationMode = new javax.swing.JRadioButton();
        jrb_predictionMode = new javax.swing.JRadioButton();
        jl_tmmLabels = new javax.swing.JLabel();
        jb_generateTMMLabels = new javax.swing.JButton();
        jb_chooseTMMLabels = new javax.swing.JButton();
        jb_editTMMLabels = new javax.swing.JButton();
        jl_tmmLabelsFile = new javax.swing.JLabel();
        jp_run = new javax.swing.JPanel();
        jb_runAll = new javax.swing.JButton();
        jb_addFC = new javax.swing.JButton();
        jb_runPSF = new javax.swing.JButton();
        jb_generateReport = new javax.swing.JButton();
        jtxt_bootCycles = new javax.swing.JTextField();
        jl_bootstrap = new javax.swing.JLabel();
        jb_openReport = new javax.swing.JButton();
        jb_webpage = new javax.swing.JButton();
        jp_visualization = new javax.swing.JPanel();
        jl_samples = new javax.swing.JLabel();
        jcb_samples = new javax.swing.JComboBox<>();
        jb_viz = new javax.swing.JButton();
        jb_saveSettings = new javax.swing.JButton();


        jp_filesAndTitles.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jl_chosenExpMatFile.setText("No file chosen");
        jl_chosenExpMatFile.setEnabled(false);

        jl_parentDir.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_parentDir.setForeground(new java.awt.Color(21, 140, 186));
        jl_parentDir.setText("Parent directory");

        jl_chosenParentDir.setText("No folder chosen");
        jl_chosenParentDir.setEnabled(false);

        jtxt_iterationTitle.setText("Untitled_iteration");
        jtxt_iterationTitle.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));

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

        jcb_geneID.setMinimumSize(new java.awt.Dimension(31, 20));
        jcb_geneID.setPreferredSize(new java.awt.Dimension(31, 20));

        jl_geneID.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_geneID.setForeground(new java.awt.Color(21, 140, 186));
        jl_geneID.setText("Gene ID");

        jb_done.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jb_done.setForeground(new java.awt.Color(51, 153, 0));
        jb_done.setText("Done");
        jb_done.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jb_done.setPreferredSize(new java.awt.Dimension(35, 25));

        jb_edit.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jb_edit.setForeground(new java.awt.Color(21, 140, 186));
        jb_edit.setText("Edit");
        jb_edit.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jb_edit.setPreferredSize(new java.awt.Dimension(27, 25));

        jb_refresh.setMaximumSize(new java.awt.Dimension(20, 20));
        jb_refresh.setMinimumSize(new java.awt.Dimension(20, 20));
        jb_refresh.setPreferredSize(new java.awt.Dimension(20, 20));


        javax.swing.GroupLayout jp_filesAndTitlesLayout = new javax.swing.GroupLayout(jp_filesAndTitles);
        jp_filesAndTitles.setLayout(jp_filesAndTitlesLayout);
        jp_filesAndTitlesLayout.setHorizontalGroup(
                jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jl_expMat)
                                                        .addComponent(jl_iterationTitle)
                                                        .addComponent(jl_geneID))
                                                .addGap(38, 38, 38)
                                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                                .addComponent(jb_chooseExpMatFile, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jl_chosenExpMatFile, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                        .addComponent(jtxt_iterationTitle)
                                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                                .addComponent(jcb_geneID, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jb_refresh, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addComponent(jsp_comment)
                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                .addComponent(jb_edit, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jb_done, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                .addComponent(jl_comment)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                                .addComponent(jl_parentDir)
                                                .addGap(18, 18, 18)
                                                .addComponent(jb_browseParentDir, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jl_chosenParentDir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jp_filesAndTitlesLayout.setVerticalGroup(
                jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_filesAndTitlesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jl_parentDir)
                                        .addComponent(jb_browseParentDir)
                                        .addComponent(jl_chosenParentDir))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jb_chooseExpMatFile)
                                        .addComponent(jl_chosenExpMatFile)
                                        .addComponent(jl_expMat))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jl_geneID)
                                        .addComponent(jb_refresh, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jcb_geneID, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(11, 11, 11)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jtxt_iterationTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jl_iterationTitle))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jl_comment)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsp_comment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jp_filesAndTitlesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jb_done, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jb_edit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        jp_modeAndLabels.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jrb_validationMode.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        jrb_validationMode.setForeground(new java.awt.Color(0, 188, 49));
        jrb_validationMode.setText("Validation mode");

        jrb_predictionMode.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        jrb_predictionMode.setForeground(new java.awt.Color(21, 140, 186));
        jrb_predictionMode.setText("Prediction mode");

        jl_tmmLabels.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_tmmLabels.setForeground(new java.awt.Color(102, 103, 114));
        jl_tmmLabels.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jl_tmmLabels.setText("TMM state labels");

        jb_generateTMMLabels.setText("Generate file");

        jb_chooseTMMLabels.setText("Choose file");
        jb_chooseTMMLabels.setPreferredSize(new java.awt.Dimension(105, 25));

        jb_editTMMLabels.setText("Edit file");
        jb_editTMMLabels.setPreferredSize(new java.awt.Dimension(105, 25));

        jl_tmmLabelsFile.setForeground(new java.awt.Color(102, 103, 114));
        jl_tmmLabelsFile.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jl_tmmLabelsFile.setText("No file chosen");

        javax.swing.GroupLayout jp_modeAndLabelsLayout = new javax.swing.GroupLayout(jp_modeAndLabels);
        jp_modeAndLabels.setLayout(jp_modeAndLabelsLayout);
        jp_modeAndLabelsLayout.setHorizontalGroup(
                jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_modeAndLabelsLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jrb_validationMode)
                                        .addComponent(jrb_predictionMode))
                                .addGap(203, 203, 203))
                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                                .addComponent(jl_tmmLabels, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(29, 29, 29)
                                                .addComponent(jl_tmmLabelsFile, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                                .addComponent(jb_generateTMMLabels)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jb_chooseTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(13, 13, 13)
                                                .addComponent(jb_editTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_modeAndLabelsLayout.setVerticalGroup(
                jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_modeAndLabelsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jrb_predictionMode)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jrb_validationMode)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jl_tmmLabels)
                                        .addComponent(jl_tmmLabelsFile))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jp_modeAndLabelsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jb_generateTMMLabels)
                                        .addComponent(jb_chooseTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jb_editTMMLabels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jp_run.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jb_runAll.setBackground(new java.awt.Color(0, 188, 49));
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

        jtxt_bootCycles.setText("200");

        jl_bootstrap.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_bootstrap.setForeground(new java.awt.Color(21, 140, 186));
        jl_bootstrap.setText("Bootstrap");

        jb_openReport.setBackground(new java.awt.Color(0, 188, 49));
        jb_openReport.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jb_openReport.setText("Open");

        javax.swing.GroupLayout jp_runLayout = new javax.swing.GroupLayout(jp_run);
        jp_run.setLayout(jp_runLayout);
        jp_runLayout.setHorizontalGroup(
                jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_runLayout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addGroup(jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jb_addFC, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jp_runLayout.createSequentialGroup()
                                                .addComponent(jb_runPSF, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jl_bootstrap)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jtxt_bootCycles, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jp_runLayout.createSequentialGroup()
                                                .addComponent(jb_generateReport, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(jb_openReport, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jb_runAll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_runLayout.setVerticalGroup(
                jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jp_runLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jb_addFC)
                                .addGap(18, 18, 18)
                                .addGroup(jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jb_runPSF)
                                        .addComponent(jtxt_bootCycles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jl_bootstrap))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jp_runLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jb_generateReport)
                                        .addComponent(jb_openReport))
                                .addGap(18, 18, 18)
                                .addComponent(jb_runAll)
                                .addContainerGap())
        );

        jb_webpage.setBackground(new java.awt.Color(255, 204, 0));
        jb_webpage.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jb_webpage.setForeground(new java.awt.Color(102, 103, 114));
        jb_webpage.setText("Project webpage");
        jb_webpage.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 204, 0)));

        jp_visualization.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jl_samples.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        jl_samples.setForeground(new java.awt.Color(21, 140, 186));
        jl_samples.setText("Samples");

        jb_viz.setBackground(new java.awt.Color(21, 140, 186));
        jb_viz.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jb_viz.setForeground(new java.awt.Color(0, 188, 49));
        jb_viz.setText("Viz");

        javax.swing.GroupLayout jp_visualizationLayout = new javax.swing.GroupLayout(jp_visualization);
        jp_visualization.setLayout(jp_visualizationLayout);
        jp_visualizationLayout.setHorizontalGroup(
                jp_visualizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_visualizationLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jl_samples)
                                .addGap(18, 18, 18)
                                .addComponent(jcb_samples, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jb_viz, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jp_visualizationLayout.setVerticalGroup(
                jp_visualizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jp_visualizationLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jp_visualizationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jl_samples)
                                        .addComponent(jcb_samples, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jb_viz))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jb_saveSettings.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jb_saveSettings.setText("Save settings");
        jb_saveSettings.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 102, 103)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jb_saveSettings, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jp_visualization, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addGap(72, 72, 72)
                                                        .addComponent(jb_webpage, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addComponent(jp_filesAndTitles, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jp_modeAndLabels, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 354, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jp_run, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jp_filesAndTitles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jp_modeAndLabels, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jb_saveSettings, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jp_run, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jp_visualization, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jb_webpage, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    // Variables declaration - do not modify
    private javax.swing.JButton jb_webpage;
    private javax.swing.JButton jb_addFC;
    private javax.swing.JButton jb_browseParentDir;
    private javax.swing.JButton jb_chooseExpMatFile;
    private javax.swing.JButton jb_chooseTMMLabels;
    private javax.swing.JButton jb_done;
    private javax.swing.JButton jb_edit;
    private javax.swing.JButton jb_editTMMLabels;
    private javax.swing.JButton jb_generateReport;
    private javax.swing.JButton jb_generateTMMLabels;
    private javax.swing.JButton jb_openReport;
    private javax.swing.JButton jb_runAll;
    private javax.swing.JButton jb_runPSF;
    private javax.swing.JButton jb_saveSettings;
    private javax.swing.JButton jb_refresh;
    private javax.swing.JButton jb_viz;
    private javax.swing.JComboBox<String> jcb_geneID;
    private javax.swing.JComboBox<String> jcb_samples;
    private javax.swing.JLabel jl_bootstrap;
    private javax.swing.JLabel jl_chosenExpMatFile;
    private javax.swing.JLabel jl_chosenParentDir;
    private javax.swing.JLabel jl_comment;
    private javax.swing.JLabel jl_expMat;
    private javax.swing.JLabel jl_geneID;
    private javax.swing.JLabel jl_iterationTitle;
    private javax.swing.JLabel jl_parentDir;
    private javax.swing.JLabel jl_samples;
    private javax.swing.JLabel jl_tmmLabels;
    private javax.swing.JLabel jl_tmmLabelsFile;
    private javax.swing.JPanel jp_filesAndTitles;
    private javax.swing.JPanel jp_modeAndLabels;
    private javax.swing.JPanel jp_run;
    private javax.swing.JPanel jp_visualization;
    private javax.swing.JRadioButton jrb_predictionMode;
    private javax.swing.JRadioButton jrb_validationMode;
    private javax.swing.JScrollPane jsp_comment;
    private javax.swing.JTextField jtxt_bootCycles;
    private javax.swing.JTextArea jtxt_comment;
    private javax.swing.JTextField jtxt_iterationTitle;
    // End of variables declaration
}
