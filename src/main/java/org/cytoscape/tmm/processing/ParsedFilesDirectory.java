package org.cytoscape.tmm.processing;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 5/6/2017.
 */
public class ParsedFilesDirectory {
    public static String PARENTDIR = "ParentDir";
    public static String EXPMATFILE = "ExpMatFile";
    public static String GENEID = "GeneID";
    public static String ITERATIONTITLE = "IterationTitle";


    private File parentDir;
    private File expMatFile;
    private ExpMatFileHandler expMatFileHandler;
    private File iterationDir;
    private File reportDir;
    private String iterationTitle;
    private File networkFile;
    private File nodeTableFile;
    private File fcMatFile;
    private HashMap<String, String> errorToolTips = new HashMap<>();
    private boolean allValid = true;

    public File getFcMatFile() {
        return fcMatFile;
    }

    public void setFcMatFile(File fcMatFile) {
        this.fcMatFile = fcMatFile;
    }

    public File getNetworkFile() {
        return networkFile;
    }

    public void setNetworkFile(File networkFile) {
        this.networkFile = networkFile;
    }

    public File getNodeTableFile() {
        return nodeTableFile;
    }

    public void setNodeTableFile(File nodeTableFile) {
        this.nodeTableFile = nodeTableFile;
    }

    public File getParentDir() {
        return parentDir;
    }

    public void setParentDir(File parentDir) {
        this.parentDir = parentDir;
    }

    public File getExpMatFile() {
        return expMatFile;
    }

    public void setExpMatFile(File expMatFile) {
        this.expMatFile = expMatFile;
    }

    public ExpMatFileHandler getExpMatFileHandler() {
        return expMatFileHandler;
    }

    public void setExpMatFileHandler(ExpMatFileHandler expMatFileHandler) {
        this.expMatFileHandler = expMatFileHandler;
    }

    public File getIterationDir() {
        return iterationDir;
    }

    public void setIterationDir(File iterationDir) {
        this.iterationDir = iterationDir;
    }

    public File getReportDir() {
        return reportDir;
    }

    public void setReportDir(File reportDir) {
        this.reportDir = reportDir;
    }

    public String getIterationTitle() {
        return iterationTitle;
    }

    public void setIterationTitle(String iterationTitle) {
        this.iterationTitle = iterationTitle;
    }

    public void setErrorToolTip(String item, String errorMessage) {
        this.allValid = false;
        errorToolTips.put(item, errorMessage);
    }

    public String getErrorToolTip(String item) {
        if (errorToolTips.containsKey(item))
            return errorToolTips.get(item);
        return null;
    }

    public boolean isAllValid() {
        return allValid;
    }
}
