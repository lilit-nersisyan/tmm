package org.cytoscape.tmm.reports;

import org.cytoscape.tmm.gui.DoubleFormatter;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 4/10/2017.
 * Public class SummaryFileHandler
 * <p>
 * Reads psf summary file and stores the scores for TMM networks in a map.
 * The sample names are kept in a separate list (samples),
 * and their order is the same as in the score and pvalue maps of summaryMap.
 */
public class SummaryFileHandler {

    public static String SCOREPATTERN = "score.";
    public static String SCORESKEY = "scores";
    public static String PVALUESKEY = "pvalues";
    public static String ALTKEY = "ALT";
    public static String TELOMERASEKEY = "Telomerase";


    private double[] altPSFRange = new double[]{Double.MAX_VALUE, 0};
    private double[] altPvalRange = new double[]{1, 0};
    private double[] telomerasePSFRange = new double[]{Double.MAX_VALUE, 0};
    private double[] telomerasePvalRange = new double[]{1, 0};

    private File summaryFile;
    private ArrayList<String> samples;

    //Summary map: tmmkey (ALT/Telomerase) -> scores/pval key -> sample name -> value
    HashMap<String, HashMap<String, HashMap<String, Double>>> summaryMap;


    public SummaryFileHandler(File summaryFile) throws Exception {
        this.summaryFile = summaryFile;
        try {
            generateSummaryMap();
        } catch (Exception e) {
            throw new Exception("Could not generate summary map: " + e.getMessage());
        }
    }

    /**
     * Returns the map storing the PSF and p values of TMM networks in the form:
     * tmmkey (ALT/Telomerase) -> scores/pval key -> sample name -> value
     * @return
     */
    public HashMap<String, HashMap<String, HashMap<String, Double>>> getSummaryMap() {
        return summaryMap;
    }

    public ArrayList<String> getSamples(){
        return samples;
    }

    private void generateSummaryMap() throws Exception {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(summaryFile));
        } catch (FileNotFoundException e) {
            throw new Exception("Cannot open the summary file " + summaryFile.getAbsolutePath()
                    + " for reading: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        String line;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            throw new Exception("Problem reading line 1 of file " + summaryFile.getAbsolutePath()
                    + ". Reason: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        if (line == null) {
            throw new Exception("Summary file " + summaryFile.getAbsolutePath() + " is empty");
        }

        // read in samples -
        // NOTE: the samples list generated here will later be sorted to agree with the order of samples in maps
        String[] header = line.split("\t");
        samples = new ArrayList<>();
        for (int i = 2; i < header.length; i += 2) {
            if (!header[i].contains(SCOREPATTERN))
                throw new Exception("The header at column " + i + 1
                        + " did not contain pattern \"" + SCOREPATTERN
                        + "\" in file " + summaryFile.getAbsolutePath());
            String sample = header[i].substring(SCOREPATTERN.length());
            samples.add(sample);
        }

        // read the scores
        int lineNum = 1;
        summaryMap = new HashMap<>();

        HashMap<String, HashMap<String, Double>> altMap = new HashMap<>();
        altMap.put(SCORESKEY, new HashMap<String, Double>());
        altMap.put(PVALUESKEY, new HashMap<String, Double>());
        summaryMap.put(ALTKEY, altMap);

        HashMap<String, HashMap<String, Double>> telomeraseMap = new HashMap<>();
        telomeraseMap.put(SCORESKEY, new HashMap<String, Double>());
        telomeraseMap.put(PVALUESKEY, new HashMap<String, Double>());
        summaryMap.put(TELOMERASEKEY, telomeraseMap);

        boolean altFound = false;
        boolean telomeraseFound = false;

        try {
            while ((line = reader.readLine()) != null && (!telomeraseFound || !altFound)) {
                lineNum++;
                String[] tokens = line.split("\t");
                if (tokens.length != 2 * samples.size() + 2)
                    throw new Exception("Column number mismatch: line " + lineNum + " in file " +
                            summaryFile.getAbsolutePath() + " did not contain "
                            + 2 * samples.size() + 2 + "(2xsamples + 2) columns. Found "
                            + tokens.length + " instead.");
                String gene = tokens[1];
                if (gene.equals(ALTKEY)) {
                    try {
                        populateScores(tokens, samples, lineNum, altMap,
                                altPSFRange, altPvalRange);
                    } catch (Exception e) {
                        throw new Exception("Could not retrieve ALT scores: " + e.getMessage());
                    }
                    altFound = true;
                }
                if (gene.equals(TELOMERASEKEY)) {
                    try {
                        populateScores(tokens, samples, lineNum, telomeraseMap,
                                telomerasePSFRange, telomerasePvalRange);
                    } catch (Exception e) {
                        throw new Exception("Could not retrieve Telomerase scores: " + e.getMessage());
                    }
                    telomeraseFound = true;
                }
            }
        } catch (Exception e) {
            throw new Exception("Problem reading line " + lineNum + " from file "
                    + summaryFile.getAbsolutePath() + ": "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }


        if (!altFound) {
            throw new Exception("There was no node labeled \"" + ALTKEY + "\". Please, add it as the target of ALT network.");
        }
        if (!telomeraseFound)
            throw new Exception("There was no node labeled \"" + TELOMERASEKEY + "\". Please, add it as the target of Telomerase network.");

        // to remove the following rows: there's no way the maps are empty if no exceptions were thrown
        if (summaryMap.get(ALTKEY).get(SCORESKEY).size() == 0)
            throw new Exception("Could not retrieve scores for the node " + ALTKEY + " . I have no idea why.");
        if (summaryMap.get(ALTKEY).get(PVALUESKEY).size() == 0)
            throw new Exception("Could not retrieve p values for the node " + ALTKEY + " . I have no idea why.");
        if (summaryMap.get(TELOMERASEKEY).get(SCORESKEY).size() == 0)
            throw new Exception("Could not retrieve scores for the node " + TELOMERASEKEY + " . I have no idea why.");
        if (summaryMap.get(TELOMERASEKEY).get(PVALUESKEY).size() == 0)
            throw new Exception("Could not retrieve p values for the node " + TELOMERASEKEY + " . I have no idea why.");


        /// resort samples to have the same order as in the hashmap
        samples = new ArrayList<>();
        samples.addAll(summaryMap.get(ALTKEY).get(SCORESKEY).keySet());

    }

    private void populateScores(String[] tokens, ArrayList<String> samples,
                                int lineNum,
                                HashMap<String, HashMap<String, Double>> map,
                                double[] psfRange, double[] pvalRange) throws Exception {

        HashMap<String, Double> scoresList = new HashMap<String, Double>();
        HashMap<String, Double> pvaluesList = new HashMap<String, Double>();

        for (int i = 0; i < samples.size(); i++) {
            int j = i * 2 + 2;

            try {
                double score = Double.parseDouble(tokens[j]);
                if (score < psfRange[0])
                    psfRange[0] = score;
                if (score > psfRange[1])
                    psfRange[1] = score;
                scoresList.put(samples.get(i), score);
                if (score < 0)
                    throw new Exception("psf scores should be >= 0. Found: "
                            + score + " in line " + lineNum
                            + " of file " + summaryFile.getAbsolutePath()
                            + ". Please, make sure you don't have negative FC values.");
            } catch (NumberFormatException e) {
                throw new Exception("Could not convert " + tokens[j]
                        + " to double. Line: " + lineNum + " in file: "
                        + summaryFile.getAbsolutePath());
            }

            try {
                double pvalue = Double.parseDouble(tokens[j + 1]);
                if (pvalue < pvalRange[0])
                    pvalRange[0] = pvalue;
                if (pvalue > pvalRange[1])
                    pvalRange[1] = pvalue;
                pvaluesList.put(samples.get(i), pvalue);
                if (pvalue < 0 || pvalue > 1)
                    throw new Exception("p value should be in the range [0,1]. Found: "
                            + pvalue + " in line " + lineNum + " of file " + summaryFile.getAbsolutePath());
            } catch (NumberFormatException e) {
                throw new Exception("Could not convert " + tokens[j]
                        + " to double. Line: " + lineNum + " in file: "
                        + summaryFile.getAbsolutePath());
            }

        }
        map.put(SCORESKEY, scoresList);
        map.put(PVALUESKEY, pvaluesList);
    }

    /**
     * Returns the min-max range of PSF values for the specified TMM, or null if no such TMM exists.
     * @param tmmKey
     * @return
     */
    public double[] getPSFRange(String tmmKey) {
        if (tmmKey.equals(ALTKEY))
            return altPSFRange;
        else if (tmmKey.equals(TELOMERASEKEY))
            return telomerasePSFRange;
        else
            return null;
    }

    /**
     * Retrieves psf scores for the specified TMM network from the summaryMap.
     *
     * @param tmmKey
     * @return
     * @throws Exception
     */
    public HashMap<String, Double> getScores(String tmmKey) throws Exception {
        HashMap<String, Double> scores = null;
        try {
            scores = summaryMap.get(tmmKey).get(SCORESKEY);
        } catch (Exception e) {
            throw new Exception("Summary map did not contain key " + tmmKey);
        }
        if (scores.size() != samples.size())
            throw new Exception("scores for " + tmmKey + " did not contain " + samples.size()
                    + " elements" + scores.size());
        return scores;
    }

    public void printSummaryMap() {
        System.out.println("Summary map\n");
        for (String tmm : summaryMap.keySet()) {
            System.out.println("TMM: " + tmm);
            for (String scoreType : summaryMap.get(tmm).keySet()) {
                System.out.println("Value: " + scoreType);
                for (String sample : summaryMap.get(tmm).get(scoreType).keySet()) {
                    System.out.println("Sample: " + sample + " Value: "
                            + summaryMap.get(tmm).get(scoreType).get(sample));
                }
            }
        }

        System.out.println("\nSamples\n");
        for (String s : samples) {
            System.out.println(s);
        }
    }

    public String niceSummaryMap(){
        String niceMap = "";
        String header = "Sample\tALT_PSF\tALT_pValue\tTelomerase_PSF\tTelomerase_pValue" + System.lineSeparator();
        niceMap += header;
        for(String sample : samples){
            niceMap += sample + "\t";
            niceMap += summaryMap.get(ALTKEY).get(SCORESKEY).get(sample) + "\t";
            niceMap += summaryMap.get(ALTKEY).get(PVALUESKEY).get(sample) + "\t";
            niceMap += summaryMap.get(TELOMERASEKEY).get(SCORESKEY).get(sample) + "\t";
            niceMap += summaryMap.get(TELOMERASEKEY).get(PVALUESKEY).get(sample) + System.lineSeparator();
        }
        return niceMap;
    }
}
