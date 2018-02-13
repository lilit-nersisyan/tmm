package org.cytoscape.tmm.reports;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 10/30/2017.
 */
public class GroupLabels {

    private static String GROUPLABELS_HEADERLINE = "Sample\tGroupLabel";
    private static String GROUPCOLORS_HEADERLINE = "GroupLabel\tColor(hex)";
    private static String DEFUALTLABEL = "unknown";
    private static String DEFAULTCOLOR = "#7E9495";

    File groupLabelsFile;
    File groupColorsFile;
    //sampleColorMap: sample ->  color ---> should be reduntant in the end
    private HashMap<String, Color> sampleColorsMap;
    //groupSamplesMap: group label -> list of samples with that label
    private HashMap<String, ArrayList<String>> groupSamplesMap;
    //groupLabelColorMap: group label -> the color used to visualize that label
    private HashMap<String, Color> groupLabelColorMap;
    //sampleGroupLabelMap: sample -> group label
    private HashMap<String, String> sampleGroupLabelMap;
    //List of unique group names
    private static ArrayList<String> groups;


    public GroupLabels(ArrayList<String> samples) {
        initializeMaps();
        groups.add(DEFUALTLABEL);
        groupSamplesMap.put(DEFUALTLABEL, samples);
        groupLabelColorMap.put(DEFUALTLABEL, Color.decode(DEFAULTCOLOR));
    }

    public ArrayList<String> getGroups() {
        return groups;
    }

    public void setGroupLabels(File groupLabelsFile) throws Exception {
        if (!groupLabelsFile.exists())
            throw new Exception("Group labels file " + groupLabelsFile.getAbsolutePath()
                    + " does not exist");

        this.groupLabelsFile = groupLabelsFile;

        initializeMaps();

        try {
            readGroupLabels();
        } catch (Exception e) {
            throw new Exception("Problem with the group labels file: "
                    + groupLabelsFile.getAbsolutePath() + " . Reason: " + e.getMessage());
        }

        try {
            setDefaultPalette();
        } catch (Exception e) {
            throw new Exception("Problem generated default color palette. \nReason: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
            );
        }
    }

    public void setGroupColors(File groupColorsFile) throws Exception {
        if (!groupColorsFile.exists())
            throw new Exception("Group labels file " + groupColorsFile.getAbsolutePath()
                    + " does not exist");

        this.groupColorsFile = groupColorsFile;

        try {
            readGroupColors();
        } catch (Exception e) {
            throw new Exception("Problem reading group colors file: "
                    + groupColorsFile.getAbsolutePath() + " . Reason: " + e.getMessage());
        }

    }

    private void setDefaultPalette() throws Exception {
        String[] palette = ColorPalette.getColorPalette(groups.size(), ColorPalette.SET2);
        if(palette == null)
            throw new Exception("The number of groups " + groups.size() +
                    " is greater than the color range supported by TMM");
        for (int i = 0; i < groups.size(); i++) {
            String g = groups.get(i);
            groupLabelColorMap.put(g, Color.decode(palette[i]));
        }
    }

    private void initializeMaps() {
        sampleColorsMap = new HashMap<>();
        groupSamplesMap = new HashMap<>();
        groupLabelColorMap = new HashMap<>();
        sampleGroupLabelMap = new HashMap<>();
        groups = new ArrayList<>();
    }


    /**
     * Assigns all the samples with a default group name,
     * creates a new file with the specified path and writes the annotation in a
     * sample -> groupLabel tab-delimited format.
     *
     * @param groupLabelsFile the absolute path of the file where the group label annotations should be written
     * @param samples the ArrayList of samples for which to generate the annotation
     * @throws Exception returns exception in case of problems with file IO.
     */
    public static void generateGroupLabelsFile(File groupLabelsFile, ArrayList<String> samples) throws Exception {
        if (!groupLabelsFile.createNewFile())
            throw new Exception("Could not create new File");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(groupLabelsFile);
        } catch (FileNotFoundException e1) {
            throw new Exception("Could not open group labels file "
                    + " for writing. \nReason: "
                    + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage())
                    + "\nFile: " + groupLabelsFile.getAbsolutePath());
        }
        writer.append(GROUPLABELS_HEADERLINE + System.lineSeparator());
        for (String sample : samples) {
            writer.append(sample + "\t" + DEFUALTLABEL + System.lineSeparator());
        }
        writer.close();
    }

    /**
     * Assigns all the group lables with a default color palette,
     * creates a new file with the specified path and writes the annotation in a
     * groupLabel -> hexColor tab-delimited format.
     *
     * @param groupColorsFile the absolute path of the file where the group color annotations should be written
     * @throws Exception returns exception in case of problems with file IO.
     */
    public void generateGroupColorsFile(File groupColorsFile) throws Exception {
        if (groups == null){
            throw new Exception("No group labels specified. Please, set a proper group labels file first");
        }
        if (!groupColorsFile.createNewFile())
            throw new Exception("Could not create new File");
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(groupColorsFile);
        } catch (FileNotFoundException e1) {
            throw new Exception("Could not open group colors file "
                    + " for writing. \nReason: "
                    + (e1.getCause() != null ? e1.getCause().getMessage() : e1.getMessage())
                    + "\nFile: " + groupColorsFile.getAbsolutePath());
        }
        writer.append(GROUPCOLORS_HEADERLINE + System.lineSeparator());
        try {
            setDefaultPalette();
        } catch (Exception e) {
            throw new Exception("Problem generated default color palette. \nReason: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
            );
        }
        for (String g : groupLabelColorMap.keySet()) {
            Formatter f = new Formatter(new StringBuffer("#"));
            f.format("%02X", groupLabelColorMap.get(g).getRed());
            f.format("%02X", groupLabelColorMap.get(g).getGreen());
            f.format("%02X", groupLabelColorMap.get(g).getBlue());
            writer.append(g + "\t" + f.toString()  + System.lineSeparator());
        }
        writer.close();
    }

    private void readGroupLabels() throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(groupLabelsFile));
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Exception reading the file " + groupLabelsFile
                    .getAbsolutePath());
        }

        int lineNum = 1;
        try {
            String header = reader.readLine();
            if (!header.equals(GROUPLABELS_HEADERLINE))
                throw new Exception("Invalid header. Expected: " + GROUPLABELS_HEADERLINE
                        + " . Found: " + header);

            groups = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != GROUPLABELS_HEADERLINE.split("\t").length)
                    throw new Exception("Incorrectly formatted Group labels file. \n"
                            + "Invalid number of columns at line " + lineNum
                            + "\n. Expected: " + GROUPLABELS_HEADERLINE.split("\t").length
                            + ", Found: " + tokens.length);

                String sample = tokens[0];
                String groupLabel = tokens[1];

                if (!groups.contains(groupLabel)) {
                    groups.add(groupLabel);
                    groupSamplesMap.put(groupLabel, new ArrayList<String>());
                }

                groupSamplesMap.get(groupLabel).add(sample);

                sampleGroupLabelMap.put(sample, groupLabel);

                lineNum++;
            }

        } catch (Exception e) {
            throw new Exception("Problem reading line " + lineNum
                    + " . Reason: " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    private void readGroupColors() throws Exception {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(groupColorsFile));
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Exception reading the file " + groupColorsFile
                    .getAbsolutePath());
        }

        int lineNum = 1;
        try {
            String header = reader.readLine();
            if (!header.equals(GROUPCOLORS_HEADERLINE))
                throw new Exception("Invalid header. Expected: " + GROUPCOLORS_HEADERLINE
                        + " . Found: " + header);

            String line;
            groupLabelColorMap = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != GROUPCOLORS_HEADERLINE.split("\t").length)
                    throw new Exception("Incorrectly formatted group colors file. "
                            + "Invalid number of columns at line " + lineNum
                            + ". Expected: " + GROUPCOLORS_HEADERLINE.split("\t").length
                            + ", Found: " + tokens.length);

                String groupLabel = tokens[0];
                Color decodedColor;

                try {
                    decodedColor = Color.decode(tokens[1]);
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("The color of group " + groupLabel
                            + " was not represented as a hex code, found " + tokens[1] + " instead");
                }

                if (!groupLabelColorMap.containsKey(groupLabel))
                    groupLabelColorMap.put(groupLabel, decodedColor);
                else
                    System.out.println("Warning: the group label " + groupLabel
                            + " found in group color file, but not in the group labels file");
                lineNum++;
            }
            for (String g : groups) {
                if (!groupLabelColorMap.containsKey(g))
                    throw new Exception("The file did not contain the group " + g);
            }
        } catch (IOException e) {
            throw new Exception("Problem reading line " + lineNum
                    + " . Reason: " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    public static String isGroupLabelsFileValid(File groupLabelsFile, ArrayList<String> samples) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(groupLabelsFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        String line;
        try {
            if ((line = reader.readLine()) == null) {
                return ("The file is empty.");
            }
            if (!line.equals(GROUPLABELS_HEADERLINE)) {
                return ("Error in first line: " +
                        "expected  " + GROUPLABELS_HEADERLINE +
                        "found " + line + ". \nFile: " + groupLabelsFile.getAbsolutePath()
                );
            }
            HashMap<String, String> sampleGroupLabelMap = new HashMap<>();
            int count = 2;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != GROUPLABELS_HEADERLINE.split("\t").length)
                    return "Line number " + count + " does not contain " +
                            GROUPLABELS_HEADERLINE.split("\t").length + "columns";
                String sample = tokens[0];
                if (sampleGroupLabelMap.containsKey(sample))
                    return "Duplicate sample found: " + sample;
                String groupLabel = tokens[1];

                sampleGroupLabelMap.put(sample, groupLabel);
                count++;
            }
            for (String sample : samples) {
                if (!sampleGroupLabelMap.containsKey(sample))
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


    public String isGroupColorsFileValid(File groupColorsFile) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(groupColorsFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        String line;
        try {
            if ((line = reader.readLine()) == null) {
                return ("The file is empty.");
            }
            if (!line.equals(GROUPCOLORS_HEADERLINE)) {
                return ("Error in first line: " +
                        "expected  " + GROUPCOLORS_HEADERLINE +
                        "found " + line + ". \nFile: " + groupColorsFile.getAbsolutePath()
                );
            }
            HashMap<String, Color> groupLabelColorMap = new HashMap<>();
            int count = 2;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                if (tokens.length != GROUPCOLORS_HEADERLINE.split("\t").length)
                    return "Line number " + count + " does not contain " +
                            GROUPCOLORS_HEADERLINE.split("\t").length + "columns";
                String groupLabel = tokens[0];
                if (groupLabelColorMap.containsKey(groupLabel))
                    return "Duplicate groupLabel found: " + groupLabel;

                Color decodedColor;
                try {
                    decodedColor = Color.decode(tokens[1]);
                } catch (NumberFormatException e) {
                    return "Colors should be represented as hex codes, found "
                            + tokens[1] + "instead";
                }

                groupLabelColorMap.put(groupLabel, decodedColor);
                count++;
            }
            for (String g : groups) {
                if (!groupLabelColorMap.containsKey(g))
                    return "The file did not contain the group" + g;
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

    public HashMap<String, ArrayList<String>> getGroupSamplesMap() {
        return groupSamplesMap;
    }

    public HashMap<String, Color> getGroupColorsMap() {
        return groupLabelColorMap;
    }


    public ArrayList<String> getSamples(String groupLabel) {
        if(!groupSamplesMap.containsKey(groupLabel))
            return null;
        return groupSamplesMap.get(groupLabel);
    }
}
