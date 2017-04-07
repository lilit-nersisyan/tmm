package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.CyManager;
import org.cytoscape.tmm.gui.TMMPanel;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lilit Nersisyan on 4/6/2017.
 */
public class VizSampleAction extends AbstractCyAction {

    private TMMPanel tmmPanel;
    private final String sample;

    public VizSampleAction(String name, TMMPanel tmmPanel,
                           String sample) {
        super(name);
        this.sample = sample;
        this.tmmPanel = tmmPanel;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        //read summary file
        RunSingleSamplePSFTask runSingleSamplePSFTask = new RunSingleSamplePSFTask();
        TMMActivator.taskManager.execute(new TaskIterator(runSingleSamplePSFTask));
    }

    private class RunSingleSamplePSFTask extends AbstractTask{

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            if(!tmmPanel.isAddFCDone())
                throw new Exception("Please, run Add/Update FC values before running Run PSF");
            taskMonitor.setStatusMessage("Running PSF to visualize sample " + sample);
            Map<String, Object> args = new HashMap<>();
            args.put("edgeTypeColumnName", "type");
            String samplesArg = sample + "";
            args.put("nodeDataColumnNames", samplesArg);
            if (tmmPanel.getFCFile() == null)
                throw new Exception("FC file is null. Run Add/Update FC values before running PSF");
            args.put("fcFile", tmmPanel.getFCFile());

            args.put("bootCyclesArg", 0 + "");

            args.put("backupDir", null);

            TaskIterator taskIterator = TMMActivator.commandExecutor.createTaskIterator(
                    "psfc", "run psf", args, null);
            TMMActivator.taskManager.execute(taskIterator);
        }
    }

    private class ReadFileTask extends AbstractTask{
        File summaryFile;

        public ReadFileTask(File summaryFile) {
            this.summaryFile = summaryFile;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("readSummaryFileTask");
            taskMonitor.setStatusMessage("Opening summary file");
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(summaryFile));
            } catch (FileNotFoundException e1) {
                throw new Exception("Problem reading summary file " + summaryFile.getAbsolutePath());
            }
            String line;
            while ((line = reader.readLine()) != null){

            }
        }
    }
}
