package org.cytoscape.tmm.processing;

import org.cytoscape.tmm.Enums.ETMMProps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Lilit Nersisyan on 4/3/2017.
 */
public class ExpMatFileHandler {
    private final File expMatFile;
    private final File nodeTableFile;
    private String[] nodeTableHeader;
    private double[][] fcMat;
    private ArrayList<String> nodes;
    private ArrayList<String> genes;
    private int numSamples;
    private String headerLine;
    private File fcMatFile;


    public ExpMatFileHandler(File expMatFile, File nodeTableFile, File fcMatFile) {
        this.expMatFile = expMatFile;
        this.nodeTableFile = nodeTableFile;
        this.fcMatFile = fcMatFile;
    }

    /**
     * Reads expression matrix file; Checks if the file contains data
     * in the following tab delimited format:
     * <p>
     * Header: GeneID   Sample1  Sample2  Sample3 ...
     * Rows: gene   expValue1   expValue2   expValue3...
     * <p>
     * If the file is valid, creates an FC file to
     *
     * @return boolean valid - true if the expMatFile was valid and successfully processed; false - otherwise
     * @throws Exception
     */
    public boolean processExpMat() throws Exception {
        boolean valid = true;
        if (expMatFile == null) {
            throw new Exception("Exp Matrix file not specified.");
        }
        if (!expMatFile.exists()) {
            throw new Exception("Exp Matrix file " + expMatFile.getAbsolutePath() + " does not exist");
        }

        BufferedReader reader = new BufferedReader(new FileReader(expMatFile));
        headerLine = reader.readLine();
        String[] headerTokens = headerLine.split("\t");
        if (headerTokens.length < 2)
            throw new Exception("The exp mat file should contain at least two columns. " +
                    "Only one token found in the header: " + headerTokens[0]
                    + " . ExpMatfile: " + expMatFile.getAbsolutePath());


        String header_geneID = headerTokens[0];
        numSamples = headerTokens.length - 1;
        boolean success = initFCMatFile();
        if (!success) {
            throw new Exception("A problem occured initiating the FC matrix");
        }
        int n = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            n++;
            String[] tokens = line.split("\t");
            if (tokens.length >= 1) {
                String id = tokens[0];
                if (genes.contains(id) && !id.equals("0") && !id.equals("NA") && !id.equals("")) {
                    if (tokens.length != numSamples + 1)
                        throw new Exception("Line " + n + " did not contain "
                                + (numSamples + 1) + " elements. Expmatfile: " + expMatFile.getAbsolutePath());
                    double sum = 0;
                    ArrayList<Integer> indices = indicesOf(id, genes);
                    for (int j = 0; j < numSamples; j++) {
                        String t = tokens[j + 1];
                        double value;
                        try {
                            value = Double.parseDouble(t);
                        } catch (NumberFormatException e) {
                            throw new NumberFormatException("Could not cast "
                                    + t + " to double. File: " + expMatFile.getAbsolutePath()
                                    + " . Line: " + n + ", element at: " + (j - 1));
                        }
                        if (value < 0)
                            throw new Exception("Found a negative value " + value
                                    + " at element" + j + 1 + ", at line: " + n + " in expMatFile: "
                                    + expMatFile.getAbsolutePath());
                        nodes.indexOf(id);
                        for (int i : indices) {
                            fcMat[i][j] = value;
                        }
                        sum += value;
                    }
                    double mean = sum / numSamples;
                    for (int i : indices) {
                        if (mean == 0) // this will be the case when all the values are 0
                            for (int j = 0; j < numSamples; j++)
                                fcMat[i][j] = 1;

                        for (int j = 0; j < numSamples; j++) {
                            fcMat[i][j] = fcMat[i][j] / mean;
                        }
                    }
                }
            }
        }
        try {
            writeFCMatFile();
        } catch (Exception e) {
            throw new Exception("Problem writing fc matrix to file: " + fcMatFile.getAbsolutePath()
                    + ". Reason: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        return true;
    }

    private ArrayList<Integer> indicesOf(String id, ArrayList<String> genes) {
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < genes.size(); i++)
            if (genes.get(i).equals(id))
                indices.add(i);
        return indices;
    }

    private void writeFCMatFile() throws Exception {
        if (fcMatFile.exists())
            if (!fcMatFile.delete())
                throw new Exception("Could not delete " + fcMatFile.getAbsolutePath());

        PrintWriter writer = new PrintWriter(fcMatFile);
        writer.write(headerLine);
        for (int i = 0; i < fcMat.length; i++) {
            writer.append(nodes.get(i));
            for (int j = 0; j < fcMat[0].length; j++) {
                writer.append("\t");
                writer.append(fcMat[i][j] + "");
            }
            writer.append("\n");
        }
        writer.close();
    }


    private boolean initFCMatFile() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(nodeTableFile));
        String line = reader.readLine();
        String[] tokens = line.split(",");
        boolean validHeader = true;
        if (tokens.length != 3)
            validHeader = false;
        else {
            if (!tokens[0].equals("name"))
                validHeader = false;
            if (!tokens[2].equals("network"))
                validHeader = false;
        }
        if (!validHeader)
            throw new Exception("Node table file does not contain three header items. " +
                    "File: " + nodeTableFile.getAbsolutePath());
        nodeTableHeader = tokens;

        int n = 1;
        nodes = new ArrayList<>();
        genes = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            n++;
            tokens = line.split(",");
            if (tokens.length != 3)
                throw new Exception("Line " + n + " in node table file "
                        + nodeTableFile.getAbsolutePath() + " did not have three elements");
            nodes.add(tokens[0]);
            genes.add(tokens[1]);
        }
        fcMat = new double[n - 1][numSamples];
        for (int i = 0; i < fcMat.length; i++) {
            for (int j = 0; j < numSamples; j++) {
                fcMat[i][j] = 1; //missing value
            }
        }
        return true;
    }

}
