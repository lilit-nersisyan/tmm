package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyColumn;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.CyManager;
import org.cytoscape.tmm.gui.TMMPanel;
import org.cytoscape.tmm.processing.ExpMatFileHandler;
import org.cytoscape.tmm.processing.ParsedFilesDirectory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

/**
 * This class possesses functionality that parses the input files supplied to TMM.
 * It creates and keeps files and directories in an object of class ParsedFilesDirectory
 * for further reference. This object also holds error messages for invalid inputs.
 */
public class ParseInputFilesAction extends AbstractCyAction {
    private TMMPanel tmmPanel;
    private boolean finished = false;
    private boolean allValid = false;
    private ActionEvent e;
    private ParsedFilesDirectory parsedFilesDirectory = new ParsedFilesDirectory();

    public ParseInputFilesAction(String name, TMMPanel tmmPanel, ActionEvent e) {
        super(name);
        this.tmmPanel = tmmPanel;
        this.e = e;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isAllValid() {
        return allValid;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ParseInputFilesTask parseInputFilesTask = new ParseInputFilesTask();
        TMMActivator.taskManager.execute(new TaskIterator(parseInputFilesTask));
    }

    private class ParseInputFilesTask extends AbstractTask {

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            finished = false;
            taskMonitor.setTitle("TMM");
            taskMonitor.setStatusMessage("TMM task started");
            tmmPanel.setExpMatFileValid(false);
            // Start of Create directories, write comment file
            File parentDir;
            try {
                try {
                    parentDir = tmmPanel.getParentDir();
                    if (parentDir == null) {
                        finished = true;
                        String error = "Parent directory is null";
                        parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.PARENTDIR, error);
                        throw new Exception(error);
                    } else {
                        parsedFilesDirectory.setParentDir(parentDir);
                    }
                } catch (Exception e) {
                    String error = (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.PARENTDIR, error);
                    throw new Exception("TMM exception: " + error);
                }

                File expMatFile;
                try {
                    expMatFile = tmmPanel.getExpMatFile();
                    if (expMatFile == null) {
                        finished = true;
                        String error = "Expression matrix is null";
                        parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.EXPMATFILE, error);
                        throw new Exception(error);
                    } else {
                        parsedFilesDirectory.setExpMatFile(expMatFile);
                    }
                } catch (Exception e) {
                    finished = true;
                    String error = (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.EXPMATFILE, error);
                    throw new Exception("TMM exception: " + error);
                }
                File iterationDir;
                String iterationTitle;
                try {
                    iterationTitle = tmmPanel.getIterationTitle();
                    parsedFilesDirectory.setIterationTitle(iterationTitle);
                    iterationDir = new File(parentDir, iterationTitle);
                    parsedFilesDirectory.setIterationDir(iterationDir);
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
                            String error  = (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
                            parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.ITERATIONTITLE,
                                    error);
                            throw new Exception("Unable to create directory "
                                    + iterationDir.getAbsolutePath()
                                    + " . Reason: " + error);
                        }
                        if (!success) {
                            String error = "Unable to create directory "
                                    + iterationDir.getAbsolutePath();
                            parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.ITERATIONTITLE,
                                    error);
                            throw new Exception(error);
                        } else {
                            parsedFilesDirectory.setIterationDir(iterationDir);
                        }
                    }

                } catch (Exception e) {
                    finished = true;
                    String error = (e.getCause() == null ? e.getCause().getMessage() : e.getMessage());
                    parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.ITERATIONTITLE,
                            error);
                    throw new Exception("TMM: problem creating directories for the iteration"
                            + (e.getCause() == null ? e.getCause().getMessage() : e.getMessage()));
                }

                //export network
                try {
                    File netFile = new File(iterationDir, iterationTitle + "_network.xgmml");
                    new ExportNetworkAction(CyManager.getCurrentNetwork(),
                            netFile.getAbsolutePath()).actionPerformed(e);
                    parsedFilesDirectory.setNetworkFile(netFile);
                } catch (Exception e1) {
                    finished = true;
                    throw new Exception("Problem exporting network to file. Reason: "
                            + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage()));
                }


                //export nodes table to the netDir
                String geneIDName;
                try {
                    geneIDName = tmmPanel.getGeneIDName();
                    if (geneIDName == null) {
                        String error = "No proper Gene ID Column was chosen";
                        parsedFilesDirectory.setErrorToolTip(parsedFilesDirectory.GENEID,
                                error);
                        throw new Exception(error);
                    }
                    if (geneIDName.equals("")) {
                        String error = "Choose a column containing gene IDs of expMat file";
                        parsedFilesDirectory.setErrorToolTip(parsedFilesDirectory.GENEID, error);
                        throw new Exception(error);
                    }

                    CyColumn column = CyManager.getCurrentNetwork().getDefaultNodeTable().getColumn(geneIDName);
                    if (!column.getType().equals(String.class)) {
                        String error  = "The Gene ID column should be of type String. \nChoose a column containing gene IDs of expMat file";
                        parsedFilesDirectory.setErrorToolTip(parsedFilesDirectory.GENEID, error);
                        throw new Exception(error);
                    }
                    try {
                        boolean valid = false;
                        ArrayList<String> samples = ExpMatFileHandler.getFirstColumn(expMatFile);
                        for (String value : column.getValues(String.class)) {
                            if (samples.contains(value)) {
                                valid = true;
                                break;
                            }
                        }
                        if(!valid){
                            String error = "The values in the column are not present in expMat file. \nChoose a column containing gene IDs of expMat file or change the expMat file";
                            parsedFilesDirectory.setErrorToolTip(parsedFilesDirectory.GENEID, error);
                        }
                    } catch (Exception e) {
                        String error = "error validating the gene ID column or expMat file: \n" + e.getMessage();
                        parsedFilesDirectory.setErrorToolTip(parsedFilesDirectory.GENEID, error);
                    }


                } catch (Exception e1) {
                    finished = true;
                    throw new Exception("Gene ID column not valid: "
                            + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage()));
                }

                File nodeTableFile;
                try{
                    nodeTableFile = new File(iterationDir, "nodes_"
                            + iterationTitle + ".csv");
                    CyManager.exportNodeNameEntrezTable(CyManager.getCurrentNetwork(),
                            geneIDName, nodeTableFile);
                    parsedFilesDirectory.setNodeTableFile(nodeTableFile);
                } catch (Exception e){
                    finished = true;
                    throw new Exception("Problem exporting node table to file. Reason: "
                            + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                }

                // End of create directories, write comment file

                taskMonitor.setStatusMessage("Parsing expression mat file");
                ExpMatFileHandler handler;
                try {
                    File fcMatFile = new File(iterationDir,
                            "fc_" + iterationTitle + ".txt");
                    handler = new ExpMatFileHandler(expMatFile,
                            nodeTableFile, fcMatFile);
                    try{
                        handler.processExpMat();
                        taskMonitor.setStatusMessage("FC values written to file: " + fcMatFile.getAbsolutePath());
                        tmmPanel.setExpMatFileValid(true);
                        parsedFilesDirectory.setExpMatFileHandler(handler);
                        parsedFilesDirectory.setFcMatFile(fcMatFile);
                    } catch (Exception e){
                        parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.EXPMATFILE, e.getMessage());
                        throw new Exception("Expression matrix file was not valid:\n " + e.getMessage());
                    }

                    tmmPanel.setSamples(handler.getSamples());
                    allValid = true;
                } catch (Exception e) {
                    parsedFilesDirectory.setErrorToolTip(ParsedFilesDirectory.EXPMATFILE, e.getMessage());
                    throw new Exception("Exception parsing expression matrix file: " + e.getMessage());
                }
            } catch (Exception e1) {
                throw new Exception(e1.getMessage());
            } finally {
                tmmPanel.setParsedFilesDirectory(parsedFilesDirectory);
                finished = true;
                System.gc();
            }

        }

        @Override
        public void cancel(){
            cancelled = true;
            finished = true;
        }
    }


}
