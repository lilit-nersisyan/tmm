package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.CyManager;
import org.cytoscape.tmm.gui.TMMPanel;
import org.cytoscape.tmm.processing.ExpMatFileHandler;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private ArrayList<String> samples = null;
    private int bootCycles = 200;

    public RunPipelineAction(String name, TMMPanel tmmPanel, boolean addFC, boolean runPSF, boolean generateReport) {
        super(name);
        this.tmmPanel = tmmPanel;
        this.addFC = addFC;
        this.runPSF = runPSF;
        this.generateReport = generateReport;
    }

    public void setSamples(ArrayList<String> samples) {
        this.samples = samples;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final RunPipelineTask runPipelineTask = new RunPipelineTask();
        TMMActivator.taskManager.execute(new TaskIterator(runPipelineTask));
        this.e = e;
    }

    public void setBootCycles(int bootCycles) {
        this.bootCycles = bootCycles;
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
                if (geneIDName == null)
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
                        if (valid)
                            taskMonitor.setStatusMessage("FC values written to file: " + fcMatFile.getAbsolutePath());
                        taskMonitor.setStatusMessage("Mapping FC values to CyTable");
                        for (String sample : handler.getSamples()) {
                            CyManager.setNodeAttributesFromMap(CyManager.getCurrentNetwork(),
                                    handler.getSamplesCyNodeFCValueMap().get(sample), sample, Double.class);
                        }
                        taskMonitor.setStatusMessage("FC values were successfully imported");
                        tmmPanel.setSamples(handler.getSamples());
                        tmmPanel.setFcFile(handler.getFCFile());
                        tmmPanel.setAddFCDone(true);
                        tmmPanel.enableButtons();
                    }
                    if (runPSF && !cancelled) {
                        if(!tmmPanel.isAddFCDone())
                            throw new Exception("Please, run Add/Update FC values before running Run PSF");
                        if (samples == null)
                            throw new Exception("No samples specified. Run Add/Update FC values before running PSF");
                        taskMonitor.setStatusMessage("Running PSF");
                        Map<String, Object> args = new HashMap<>();
                        args.put("edgeTypeColumnName", "type");
                        String samplesArg = "";
                        for (int i = 0; i < samples.size(); i++) {
                            samplesArg += samples.get(i);
                            if (i != samples.size() - 1)
                                samplesArg += ",";
                        }
                        args.put("nodeDataColumnNames", samplesArg);
                        if (tmmPanel.getFCFile() == null)
                            throw new Exception("FC file is null. Run Add/Update FC values before running PSF");
                        args.put("fcFile", tmmPanel.getFCFile());

                        int bootCycles = tmmPanel.getBootCycles();
                        args.put("bootCyclesArg", bootCycles + "");
                        TaskIterator taskIterator = TMMActivator.commandExecutor.createTaskIterator(
                                "psfc", "run psf", args, null);
                        File psfcDir = new File(TMMActivator.getTMMDir().getParent(), "PSFC");
                        File summaryFile = new File(psfcDir, CyManager.getCurrentNetwork().toString() + "_summary.xls");
                        boolean summaryFileOK = true;
                        if (summaryFile.exists()) {
                            boolean success = summaryFile.delete();
                            if (!success) {
                                JOptionPane.showMessageDialog(TMMActivator.cytoscapeDesktopService.getJFrame(),
                                        "Could not delete file"
                                                + summaryFile.getAbsolutePath() + " maybe the file is in use. " +
                                                "Please, close it and start again.", "TMM error", JOptionPane.ERROR_MESSAGE);
                                summaryFileOK = false;
                            }
                        }
                        if (summaryFileOK) {
                            TMMActivator.taskManager.execute(taskIterator);
                            taskMonitor.setStatusMessage("Collecting results for backup");
                            while (!summaryFile.exists()) {
                                if (cancelled)
                                    break;
                            }
                            File itSummaryFile = new File(new File(tmmPanel.getParentDir(),
                                    tmmPanel.getIterationTitle()),
                                    "psf_summary.xls");
                            boolean itSummaryFileOK = true;
                            if (itSummaryFile.exists()) {
                                itSummaryFileOK = itSummaryFile.delete();
                            }
                            if (itSummaryFileOK) {
                                boolean fileInUse = true;
                                taskMonitor.setStatusMessage("Writing results to summary file");
                                while (fileInUse) {
//                                    System.out.printf("summary file in use\n");
                                    if (cancelled)
                                        break;
                                    fileInUse = !summaryFile.renameTo(itSummaryFile);
                                }
                                if (itSummaryFile.exists()) {
                                    System.out.println("summary file size: " + itSummaryFile.length());
                                    taskMonitor.setStatusMessage("Wrote the resulst to summary file "
                                            + itSummaryFile.getAbsolutePath());
                                }
                                else
                                    throw new Exception("Run PSF was not successful. " +
                                            "Could not find summary file " + itSummaryFile.getAbsolutePath());

                            } else {
                                throw new Exception("File " + itSummaryFile.getAbsolutePath() + " is in use.");
                            }
                        }
                        tmmPanel.setRunPSFDone(true);
                        tmmPanel.enableButtons();
                    }
                    if (generateReport && !cancelled) {
                        taskMonitor.setStatusMessage("Generating report");
                        if (!tmmPanel.isRunPSFDone())
                            throw new Exception("Run PSF task was not completed successfully. Please, rerun it and try again with report generation");

                    }
                } catch (Exception e) {
                    throw new Exception("TMM exception: " + e.getMessage());
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
