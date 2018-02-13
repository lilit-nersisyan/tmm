package org.cytoscape.tmm.reports;


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 4/11/2017.
 */
public class TMMLabels {
    public static String A = "ALT";
    public static String T = "Telomerase";
    public static String N = "Normal";
    public static String AT = "ALT+/Telomerase+";
    private String[][] categories = new String[][]{{A, AT}, {N, T}};

    private static String HEADERLINE = "Sample\tALT\tTelomerase";

    File tmmLabelsFile;
    //labelSamplesMap: TMM label -> list of samples with that label
    private HashMap<String, ArrayList<String>> labelSamplesMap;
    //labelColorMap: TMM label -> the color used to visualize that label
    private HashMap<String, Paint> labelColorMap = new HashMap<>();
    //sampleTMMLabelMap: sample -> TMM label
    private HashMap<String, String> sampleTMMLabelMap;

    public TMMLabels(File tmmLabelsFile) throws Exception {
        if (!tmmLabelsFile.exists())
            throw new Exception("TMM labels file " + tmmLabelsFile.getAbsolutePath()
                    + " does not exist");

        this.tmmLabelsFile = tmmLabelsFile;

        initializeMaps();

        try {
            readTmmLabels();
        } catch (Exception e) {
            throw new Exception("Problem reading TMM Labels file: "
                    + tmmLabelsFile.getAbsolutePath() + " . Reason: " + e.getMessage());
        }
    }

    private void initializeMaps() {
        labelSamplesMap = new HashMap<>();
        String[] catOrder = new String[]{A, T, N, AT};
        for (String cat : catOrder) {
            labelSamplesMap.put(cat, new ArrayList<String>());
        }
        sampleTMMLabelMap = new HashMap<>();

        labelColorMap.put(A, Color.red);
        labelColorMap.put(T, Color.blue);
        labelColorMap.put(N, Color.green);
        labelColorMap.put(AT, Color.cyan);
    }

    private void readTmmLabels() throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(tmmLabelsFile));
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Exception reading the file " + tmmLabelsFile.getAbsolutePath());
        }

        int lineNum = 1;
        try {
            String header = reader.readLine();
            if (!header.equals(HEADERLINE))
                throw new Exception("Invalid header. Expected: " + HEADERLINE
                        + " . Found: " + header);


            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != 3)
                    throw new Exception("Incorrectly formatted TMMLabels file. " +
                            "Invalid number of columns at line " + lineNum
                            + ". Expected: 3, Found: " + tokens.length);

                String sample = tokens[0];

                if (!tokens[1].equals("+") && !tokens[1].equals("-"))
                    throw new Exception("Incorrectly formatted TMMLabels file. " +
                            "Unexpected symbol at line " + lineNum
                            + ". Expected: + or - , Found: " + tokens[1]);
                boolean isAlt = tokens[1].equals("+");

                if (!tokens[2].equals("+") && !tokens[2].equals("-"))
                    throw new Exception("Incorrectly formatted TMMLabels file. " +
                            "Unexpected symbol at line " + lineNum
                            + ". Expected: + or - , Found: " + tokens[2]);
                boolean isTelomerase = tokens[2].equals("-");


                int i = isAlt ? 0 : 1;
                int j = isTelomerase ? 0 : 1;
                String category = categories[i][j];

                System.out.println("Sample: " + sample + " Category: " + category);

                sampleTMMLabelMap.put(sample, category);
                labelSamplesMap.get(category).add(sample);
                lineNum++;
            }
        } catch (IOException e) {
            throw new Exception("Problem reading line " + lineNum
                    + " . Reason: " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    public HashMap<String, ArrayList<String>> getLabelSamplesMap() {
        return labelSamplesMap;
    }

    public String[][] getCategories() {
        return categories;
    }

    public HashMap<String, String> getSampleTMMLabelMap() {
        return sampleTMMLabelMap;
    }

    public ArrayList<String> getSamples(String label) {
        return labelSamplesMap.get(label);
    }

    /**
     * Returns the color of the TMM label or null if no such label exists.
     *
     * @param seriesKey
     * @return
     */
    public Paint getLabelColor(String seriesKey) {
        if (labelColorMap.containsKey(seriesKey))
            return labelColorMap.get(seriesKey);
        return null;
    }

    public static String isLabelsFileValid(File tmmLabelsFile, ArrayList<String> samples) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(tmmLabelsFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        String line;
        try {
            if ((line = reader.readLine()) == null) {
                return ("The file is empty.");
            }
            if (!line.equals(HEADERLINE)) {
                return ("Error in first line: " +
                        "expected \"Sample\\tALT\\tTelomerase\" " +
                        "found " + line + ". \nFile: " + tmmLabelsFile.getAbsolutePath()
                );
            }
            HashMap<String, String[]> labelsMap = new HashMap<>();
            int count = 2;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != 3)
                    return "Line number " + count + " does not contain 3 columns";
                String sample = tokens[0];
                if (labelsMap.containsKey(sample))
                    return "Duplicate sample found: " + sample;
                if (!tokens[1].equals("+") && !tokens[1].equals("-"))
                    return "Labels should be '+' or '-'. Found: " + tokens[1] + " at line: " + count;
                if (!tokens[2].equals("+") && !tokens[2].equals("-"))
                    return "Labels should be '+' or '-'. Found: " + tokens[1] + " at line: " + count;

                String[] labels = new String[]{tokens[1], tokens[2]};
                labelsMap.put(sample, labels);
                count++;
            }
            for (String sample : samples) {
                if (!labelsMap.containsKey(sample))
                    return "The file did not contain sample " + sample;
            }
        } catch (IOException e) {
            return e.getMessage();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                return (e.getMessage());
            }
        }
        return "true";
    }

    public static void generateFile(File tmmLabelsFile, ArrayList<String> samples) throws Exception {
        if (!tmmLabelsFile.createNewFile())
            throw new Exception("Could not create new File");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(tmmLabelsFile);
        } catch (FileNotFoundException e1) {
            throw new Exception("Could not open TMM labels file "
                    + " for writing. \nReason: "
                    + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage())
                    + "\nFile: " + tmmLabelsFile.getAbsolutePath());
        }
        writer.append(HEADERLINE + System.lineSeparator());
        for (String sample : samples) {
            writer.append(sample + "\t-\t-" + System.lineSeparator());
        }
        writer.close();
    }
}
