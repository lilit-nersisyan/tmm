package org.cytoscape.tmm.actions;

import jdk.nashorn.internal.runtime.ECMAException;
import jdk.nashorn.internal.scripts.JO;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.CyManager;
import org.cytoscape.tmm.gui.TMMPanel;
import org.cytoscape.tmm.processing.ExpMatFileHandler;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Lilit Nersisyan on 3/22/2017.
 */
public class RunPipelineAction extends AbstractCyAction {
    private boolean addFC = false;
    private boolean runPSF = false;
    private boolean generateReport = false;
    private boolean cancelled = false;
    private TMMPanel tmmPanel;
    private File parentDir;
    private File iterationDir;
    private File expMatFile;
    private File nodeTableFile;
    private String iterationTitle;
    private ActionEvent e;

    public RunPipelineAction(String name, TMMPanel tmmPanel, boolean addFC, boolean runPSF, boolean generateReport) {
        super(name);
        this.tmmPanel = tmmPanel;
        this.addFC = addFC;
        this.runPSF = runPSF;
        this.generateReport = generateReport;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final RunPipelineTask runPipelineTask = new RunPipelineTask();
        TMMActivator.taskManager.execute(new TaskIterator(runPipelineTask));
        this.e = e;
    }

    private class RunPipelineTask extends AbstractTask {

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("TMM");
            taskMonitor.setStatusMessage("TMM task started");

            // Start of Create directories, write comment file
            try {
                parentDir = tmmPanel.getParentDir();
                if (parentDir == null) {
                    throw new Exception("Parent directory is null");
                }
            } catch (Exception e) {
                throw new Exception("TMM exception: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }

            try {
                expMatFile = tmmPanel.getExpMatFile();
                if (expMatFile == null) {
                    throw new Exception("Expression matrix is null");
                }
            } catch (Exception e) {
                throw new Exception("TMM exception: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
            try {
                iterationTitle = tmmPanel.getIterationTitle();
                iterationDir = new File(parentDir, iterationTitle);
                if (iterationDir.exists()) {
                    if (!iterationDir.isDirectory()) {
                        JOptionPane jOptionPane = new JOptionPane();
                        jOptionPane.showMessageDialog(TMMActivator.cytoscapeDesktopService.getJFrame(),
                                "There is a file with the same name as the iteration directory " + iterationDir.getName()
                                        + ". Please, remove it and try again",
                                "TMM directory file exists", JOptionPane.OK_OPTION);
                        cancel();
                    }
                } else {
                    boolean success = false;
                    try {
                        success = iterationDir.mkdir();
                    } catch (Exception e) {
                        throw new Exception("Unable to create directory "
                                + iterationDir.getAbsolutePath()
                                + " . Reason: " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
                    }
                    if (!success) {
                        throw new Exception("Unable to create directory "
                                + iterationDir.getAbsolutePath());
                    }
                }

            } catch (Exception e) {
                throw new Exception("TMM: problem creating directories for the iteration"
                        + (e.getCause() == null ? e.getCause().getMessage() : e.getMessage()));
            }

            File commentFile = new File(iterationDir, "comment.txt");
            String comment = tmmPanel.getCommentText();
            PrintWriter writer = new PrintWriter(commentFile);
            try {
                writer.write(comment);
                writer.close();
            } catch (Exception e) {
                throw new Exception("Problem writing comment to file : " + commentFile
                        + " .Reason: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }

            //export network
            try {
                File netFile = new File(iterationDir, iterationTitle + ".xgmml");
                new ExportNetworkAction(CyManager.getCurrentNetwork(),
                        netFile.getAbsolutePath()).actionPerformed(e);
            } catch (Exception e1) {
                throw new Exception("Problem exporting network to file. Reason: "
                        + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage()));
            }


            //export nodes table to the netDir
            try {
                String geneIDName = tmmPanel.getGeneIDName();
                if(geneIDName == null)
                    throw new Exception("No proper Gene ID Column was chosen");
                nodeTableFile = new File(iterationDir, "nodes_"
                        + iterationTitle + ".csv");
                CyManager.exportNodeNameEntrezTable(CyManager.getCurrentNetwork(),
                        geneIDName, nodeTableFile);
            } catch (Exception e1) {
                throw new Exception("Problem exporting node table to file. Reason: "
                        + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage()));
            }

            // End of create directories, write comment file

            if (!cancelled) {

                try {
                    if (addFC && !cancelled) {
                        taskMonitor.setStatusMessage("Updating FC values");
                        File fcMatFile = new File(iterationDir,
                                "fc_" + iterationTitle + ".txt");
                        ExpMatFileHandler handler = new ExpMatFileHandler(expMatFile,
                                nodeTableFile, fcMatFile);
                        boolean valid = handler.processExpMat();
                        if(valid)
                            taskMonitor.setStatusMessage("FC values written to file: " + fcMatFile.getAbsolutePath());
                        taskMonitor.setStatusMessage("Mapping FC values to CyTable");
                        for(String sample : handler.getSamples()) {
                            CyManager.setNodeAttributesFromMap(CyManager.getCurrentNetwork(),
                                    handler.getSamplesCyNodeFCValueMap().get(sample), sample, Double.class);
                        }
                        taskMonitor.setStatusMessage("FC values were successfully imported");
                    }
                    if (runPSF && !cancelled) {
                        taskMonitor.setStatusMessage("Running PSF");
                        Map<String, Object> args = new HashMap<>();
                        args.put("edgeTypeColumnName", "type");
                        args.put("nodeDataColumnNames", "SUSM1_ALT,SKLU_ALT");
                        TaskIterator taskIterator = TMMActivator.commandExecutor.createTaskIterator(
                                "psfc", "run psf", args, null);
                        TMMActivator.taskManager.execute(taskIterator);
                    }
                    if (generateReport && !cancelled) {
                        taskMonitor.setStatusMessage("Generating report");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.gc();
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            System.gc();
        }


     /*   private Set<String> addFCActionPerformed(ActionEvent e) throws Exception {

            //make a directory parent.dir/alt-tert/iteration
            File netsParentDir = new File(jl_selectedParentDir.getToolTipText(), "alt-tert"); // alt-tert until set by the user
            NetIteration.setIteration(jtxt_iteration.getText());
            File netDir = new File(netsParentDir, NetIteration.getIteration());
            System.out.println("PSFC:: creating folder " + netDir.getAbsolutePath());
            if (netDir.exists()) {
                int ans = JOptionPane.showConfirmDialog(this, "Directory " + netDir.getAbsolutePath() + " already exists. \n " +
                        "Do you want to replace it? ");
                if (ans == JOptionPane.NO_OPTION || ans == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
            } else {
                boolean success = netDir.mkdir();
                if (!success) {
                    throw new Exception("PSFC:: Net: could not create folder " + netDir.getAbsolutePath());
                }
            }
            NetIteration.setNetDir(netDir);
            //export network
            File netFile = new File(netDir, NetIteration.getIteration() + ".xgmml");
            new ExportNetworkAction(getSelectedNetwork(), netFile.getAbsolutePath()).actionPerformed(e);

            //export nodes table to the netDir
            File nodeFile = new File(netDir, "nodes_" + NetIteration.getIteration() + ".csv");
            psfcpane    NetworkCyManager.exportNodeNameEntrezTable(getSelectedNetwork(), nodeFile);


            NetIteration.processData();
            File fcMat = new File(netDir, "fc_" + NetIteration.getIteration() + ".txt");
            if (fcMat.exists()) {
                System.out.println("PSFC:: Net: Computed fc values and stored in " + fcMat.getAbsolutePath());
            }
            NetIteration.setFcMat(fcMat);
            System.out.println("PSFC:: Net: ran the Rscript preprocess.R");
            Set<String> colnames = NetworkCyManager.setNodeAttributesFromFile(getSelectedNetwork(), fcMat);
            System.out.println("PSFC:: imported fc attributes");
            NetIteration.setColnames(colnames);
            return colnames;
            //import the fc file
        }

        private void jb_runPSFActionPerformed(ActionEvent e)  {
            TaskIterator taskIterator = new TaskIterator();
            taskIterator.append(new runPSFandGenerateReportTask(e));
            PSFCActivator.taskManager.execute(taskIterator);
        }*/

    }
}
