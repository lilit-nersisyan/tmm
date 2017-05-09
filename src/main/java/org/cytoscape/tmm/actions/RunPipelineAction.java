package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.CyManager;
import org.cytoscape.tmm.gui.TMMPanel;
import org.cytoscape.tmm.processing.ExpMatFileHandler;
import org.cytoscape.tmm.processing.ParsedFilesDirectory;
import org.cytoscape.work.*;

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
    private ActionEvent e;
    private ArrayList<String> samples = null;
    private int bootCycles = 200;
    private boolean isParsing = false;

    public RunPipelineAction(String name, TMMPanel tmmPanel,
                             boolean addFC, boolean runPSF, boolean generateReport) {
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

    public boolean parsing() {
        return isParsing;
    }

    private class RunPipelineTask extends AbstractTask {

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("TMM");
            taskMonitor.setStatusMessage("TMM task started");

            ParsedFilesDirectory parsedFilesDirectory = tmmPanel.getParsedFilesDirectory();
            File iterationDir = parsedFilesDirectory.getIterationDir();
            String iterationTitle = parsedFilesDirectory.getIterationTitle();
            File expMatFile = parsedFilesDirectory.getExpMatFile();
            File nodeTableFile = parsedFilesDirectory.getNodeTableFile();
            ExpMatFileHandler handler = parsedFilesDirectory.getExpMatFileHandler();
            File fcMatFile = parsedFilesDirectory.getFcMatFile();
            // End of create directories, write comment file

            if (!cancelled) {
                try {
                    taskMonitor.setStatusMessage("Parsing input files");
                    isParsing = false;
                    if (addFC && !cancelled) {
                        taskMonitor.setStatusMessage("Mapping FC values to CyTable");
                        for (String sample : handler.getSamples()) {
                            CyManager.setNodeAttributesFromMap(CyManager.getCurrentNetwork(),
                                    handler.getSamplesCyNodeFCValueMap().get(sample), sample, Double.class);
                        }
                        taskMonitor.setStatusMessage("FC values were successfully imported");

                        setSamples(handler.getSamples());
                        tmmPanel.setFcFile(handler.getFCFile());
                        tmmPanel.setAddFCDone(true);
                        tmmPanel.enableButtons();
                    }
                    if (runPSF && !cancelled) {
                        if (!tmmPanel.isAddFCDone())
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

                        args.put("backupDir", iterationDir);
                        MyTaskObserver taskObserver = new MyTaskObserver();
                        TaskIterator taskIterator = TMMActivator.commandExecutor.createTaskIterator(
                                "psfc", "run psf", args, taskObserver);

                        if (samples.size() >= 2 && !cancelled) {
                            File tmmDir = new File(TMMActivator.getTMMDir().getParent(), "PSFC");
                            File summaryFile = new File(tmmDir, CyManager.getCurrentNetwork().toString() + "_summary.xls");
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
                                while (!summaryFile.exists()) {
                                    if (cancelled)
                                        break;
                                }
                                taskMonitor.setStatusMessage("Collecting results for backup");
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
                                        tmmPanel.setSummaryFile(itSummaryFile);
                                        System.out.println("summary file size: " + itSummaryFile.length());
                                        taskMonitor.setStatusMessage("Wrote the resulst to summary file "
                                                + itSummaryFile.getAbsolutePath());
                                    } else
                                        throw new Exception("Run PSF was not successful. " +
                                                "Could not find summary file " + itSummaryFile.getAbsolutePath());

                                } else {
                                    throw new Exception("File " + itSummaryFile.getAbsolutePath() + " is in use.");
                                }
                            } else{
                                throw new Exception("TMM Run PSF task not successful: problem with summary file");
                            }
                        }
                        tmmPanel.setRunPSFDone(true);
                        tmmPanel.enableButtons();
                    }
                    if (generateReport && !cancelled) {
                        taskMonitor.setStatusMessage("Generating report");
                        if (!tmmPanel.isRunPSFDone())
                            throw new Exception("Run PSF task was not completed successfully. Please, rerun it and try again with report generation");

                        File reportDir = new File(iterationDir, iterationTitle + "_report");
                        while (reportDir.exists()) {
                            reportDir = new File(reportDir.getAbsolutePath() + "+");
                        }
                        reportDir.mkdir();

                        GenerateReportAction generateReportAction;
                        if (tmmPanel.isValidationMode()) {
                            generateReportAction = new GenerateReportAction(
                                    "Generating report",
                                    tmmPanel.getSummaryFile(), reportDir,
                                    tmmPanel.getTmmLabelsFile(),
                                    tmmPanel.getIterationTitle(),
                                    tmmPanel.getCommentText(), tmmPanel.getBootCycles());
                        } else
                            generateReportAction = new GenerateReportAction(
                                    "Generate report action",
                                    tmmPanel.getSummaryFile(),
                                    reportDir, tmmPanel.getIterationTitle(),
                                    tmmPanel.getCommentText(), tmmPanel.getBootCycles());
                        generateReportAction.actionPerformed(e);
                        tmmPanel.setReportFile(generateReportAction.getPdfFile());
                        tmmPanel.setGenerateReportDone(true);
                        tmmPanel.enableButtons();
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


    }
}
