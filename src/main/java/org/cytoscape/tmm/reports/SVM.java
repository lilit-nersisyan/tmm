package org.cytoscape.tmm.reports;

import jsat.ARFFLoader;
import jsat.DataSet;
import jsat.classifiers.*;
import jsat.classifiers.bayesian.NaiveBayes;
import jsat.classifiers.linear.ALMA2;
import jsat.classifiers.svm.DCD;
import jsat.classifiers.svm.LSSVM;
import jsat.datatransform.DataTransform;
import jsat.datatransform.PCA;
import jsat.datatransform.ZeroMeanTransform;
import jsat.linear.DenseVector;
import jsat.linear.Vec;

import javax.swing.*;
import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Lilit Nersisyan on 4/8/2017.
 */
public class SVM {

    public static String ALTOPTION = "alt";
    public static String NONALTOPTION = "non-alt";
    public static String TELOMERASEOPTION = "telomerase";
    public static String NONTELOMERASEOPTION = "non-telomerase";

    private LSSVM classifier = new LSSVM();
    private double[][] confusionMatrix;
    private boolean[][] predictionTable; // predicted phenotypes: colnames=isALT, isTelomerase
    private int classCount;
    private double h;
    private double v;

    private SummaryFileHandler summaryFileHandler;
    private TMMLabels tmmLabels;
    private Double accuracy = null;


    public SVM(SummaryFileHandler summaryFileHandler, TMMLabels tmmLabels) {
        this.summaryFileHandler = summaryFileHandler;
        this.tmmLabels = tmmLabels;
        this.predictionTable = new boolean[summaryFileHandler.getSamples().size()][2];
    }

    public double getH() {
        return h;
    }

    public double getV() {
        return v;
    }

    public double[][] getConfusionMatrix() {
        return confusionMatrix;
    }

    public void runSVM() throws Exception {
        ClassificationDataSet altDataSet = null;
        try {
            altDataSet = generateDataSet(summaryFileHandler, TMMLabels.A);
        } catch (Exception e) {
            throw new Exception("Problem generating the ALT dataset. " + e.getMessage());
        }
        printDataSet(altDataSet);
        classify(altDataSet);
        printConfusionMatrix(altDataSet);

        ClassificationDataSet telomeraseDataset = null;
        try {
            telomeraseDataset = generateDataSet(summaryFileHandler, TMMLabels.T);
        } catch (Exception e) {
            throw new Exception("Problem generating the ALT dataset. " + e.getMessage());
        }
        printDataSet(telomeraseDataset);
        classify(telomeraseDataset);
        printConfusionMatrix(telomeraseDataset);
        printPredictionTable();
    }

    /**
     * Reads the data from summaryFileHandler and the sample labels from tmmLabels.
     * Generates a one-coordinate classification dataset for the specified tmmReference
     * (ALT or Telomerase)
     *
     * @param summaryFileHandler
     * @param tmmReference       the key specifying which TMM to separate: the ALT versus non-ALT cases, or Telomerase versus non-telomerase cases
     * @return returns the classification dataset
     * @throws Exception
     */
    public ClassificationDataSet generateDataSet(SummaryFileHandler summaryFileHandler,
                                                 String tmmReference) throws Exception {

        HashMap<String, HashMap<String, HashMap<String, Double>>> summaryMap =
                summaryFileHandler.getSummaryMap();
        HashMap<String, Double> altScores =
                summaryMap.get(SummaryFileHandler.ALTKEY).get(SummaryFileHandler.SCORESKEY);
        HashMap<String, Double> telomeraseScores =
                summaryMap.get(SummaryFileHandler.TELOMERASEKEY).get(SummaryFileHandler.SCORESKEY);

        // 1 - means one dimension; new CategoricalData(2) means two categories
        CategoricalData[] categoricalDatas = new CategoricalData[1];
        CategoricalData categoricalData = new CategoricalData(2);
        categoricalData.setCategoryName("tmmclass");

        if (tmmReference.equals(TMMLabels.A)) {
            categoricalData.setOptionName(ALTOPTION, 1);
            categoricalData.setOptionName(NONALTOPTION, 0);
        } else {
            categoricalData.setOptionName(TELOMERASEOPTION, 1);
            categoricalData.setOptionName(NONTELOMERASEOPTION, 0);
        }
        categoricalDatas[0] = categoricalData;


        ClassificationDataSet tmmDataSet = new ClassificationDataSet(1,
                categoricalDatas, categoricalData);

        int ind = 0;
        for (String s : summaryFileHandler.getSamples()) {
            double score;
            if (tmmReference.equals(TMMLabels.A))
                score = altScores.get(s);
            else
                score = telomeraseScores.get(s);
            Vec vec = DenseVector.toDenseVec(score);
            String tmm = tmmLabels.getSampleTMMLabelMap().get(s);
            int cat;
            if (tmmReference.equals(TMMLabels.A))
                cat = (tmm.equals(TMMLabels.A) || tmm.equals(TMMLabels.AT)) ? 1 : 0;
            else
                cat = (tmm.equals(TMMLabels.T) || tmm.equals(TMMLabels.AT)) ? 1 : 0;
            DataPoint dp = new DataPoint(vec, new int[]{cat}, categoricalDatas);
            try {
                tmmDataSet.addDataPoint(dp, cat);
            } catch (Exception e) {
                throw new Exception("Could not add datapoint to the dataset: "
                        + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
        }
        return tmmDataSet;
    }

    /**
     * Performs classification on the dataset with the default linear classifier
     * pre-initialized for the SVM object.
     *
     * @param dataSet - A one dimensional dataset containing PSF scores for either of the TMM networks
     */
    public void classify(ClassificationDataSet dataSet) throws Exception {
        int errors = 0;
        classifier.trainC(dataSet);
        CategoricalData predicting = dataSet.getPredicting();

        int numOfClasses = dataSet.getClassSize();
        classCount = predicting.getNumOfCategories();
        confusionMatrix = new double[numOfClasses][numOfClasses];

        ArrayList<Double> values0 = new ArrayList<>(); // keep all the values with 0 prediction
        ArrayList<Double> values1 = new ArrayList<>(); // keep all the values with 1 prediction


        for (int i = 0; i < dataSet.getSampleSize(); i++) {
            DataPoint dataPoint = dataSet.getDataPoint(i);
            int truth = dataSet.getDataPointCategory(i);

            CategoricalResults predictionResults = classifier.classify(dataPoint);
            int predicted = predictionResults.mostLikely();
            if (dataSet.getPredicting().getOptionName(1).equals(ALTOPTION))
                predictionTable[i][0] = (predicted != 0);
            else
                predictionTable[i][1] = (predicted != 0);
            if (predicted == 0)
                values0.add(dataPoint.getNumericalValues().get(0));
            else
                values1.add(dataPoint.getNumericalValues().get(0));
            if (predicted != truth) {
                errors++;
                confusionMatrix[truth][1 - truth] += 1;
            } else {
                confusionMatrix[truth][truth] += 1;
            }
            System.out.println(i + "| True Class: " + truth + ", Predicted: " + predicted + ", Confidence: " + predictionResults.getProb(predicted));
        }


        double low = 0;
        if (values0.size() > 0) {
            for (double value : values0)
                if (value > low)
                    low = value;
        }

        double up = 0;
        if (values1.size() > 0) {
            up = Double.MAX_VALUE;
            for (double value : values1)
                if (value < up)
                    up = value;
        }

        if (dataSet.getPredicting().getOptionName(1).equals(ALTOPTION))
            h = (up + low) / 2;
        else
            v = (up + low) / 2;

        //After counting the h  and v, we should re-iterate the classification,
        // since sometimes the SVM will classify points with low ALT values as ALT positive
        // and those with high ALT values as ALT negative. So we will switch in the following lines.
        for (int i = 0; i < dataSet.getSampleSize(); i++) {
            DataPoint dataPoint = dataSet.getDataPoint(i);
            int truth = dataSet.getDataPointCategory(i);
            int predicted;
            if (dataSet.getPredicting().getOptionName(1).equals(ALTOPTION))
                predicted = classify(dataPoint, h);
            else
                predicted = classify(dataPoint, v);
            if (dataSet.getPredicting().getOptionName(1).equals(ALTOPTION))
                predictionTable[i][0] = (predicted != 0);
            else
                predictionTable[i][1] = (predicted != 0);
            if (predicted == 0)
                values0.add(dataPoint.getNumericalValues().get(0));
            else
                values1.add(dataPoint.getNumericalValues().get(0));
            if (predicted != truth) {
                errors++;
                confusionMatrix[truth][1 - truth] += 1;
            } else {
                confusionMatrix[truth][truth] += 1;
            }
            System.out.println(i + "| True Class: " + truth + ", Predicted: " + predicted );
        }


        System.out.println(errors + " errors were made, " + 100.0 * errors / dataSet.getSampleSize() + "% error rate");

// I don't know what this is: useless staff
//        if (dataSet.getPredicting().getOptionName(1).equals(ALTOPTION)) {
//            classifier.supportsWeightedData();
//        } else
//            classifier.supportsWeightedData();
    }

    private int classify(DataPoint datapoint, double threshold){
        return datapoint.getNumericalValues().get(0) >= threshold? 1 : 0;
    }

    /**
     * Used to get access to the bias field in the classifier through reflection.
     *
     * @param clazz
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     */
    private static Field getField(Class clazz, String fieldName)
            throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }

    /**
     * Prints the dataset
     *
     * @param dataSet
     */
    public void printDataSet(ClassificationDataSet dataSet) {
        System.out.println("There are " + dataSet.getNumFeatures() + " features for this data set.");
        System.out.println(dataSet.getNumCategoricalVars() + " categorical features");
        System.out.println("They are:");
        for (int i = 0; i < dataSet.getNumCategoricalVars(); i++)
            System.out.println("\t" + dataSet.getCategoryName(i));
        System.out.println(dataSet.getNumNumericalVars() + " numerical features");
        System.out.println("They are:");
        for (int i = 0; i < dataSet.getNumNumericalVars(); i++)
            System.out.println("\t" + dataSet.getNumericName(i));

        System.out.println("\nThe whole data set");
        for (int i = 0; i < dataSet.getSampleSize(); i++) {
            DataPoint dataPoint = dataSet.getDataPoint(i);
            System.out.println(dataPoint);
            System.out.println("DataPointCategory: " + dataSet.getDataPointCategory(i));
        }
    }

    /**
     * Prints the confusion matrix. The rows represent true categories, while the columns - predictions.
     *
     * @param dataSet
     */
    public void printConfusionMatrix(ClassificationDataSet dataSet) {
        if (confusionMatrix == null)
            System.out.println("Confusion matrix not initialized");

        CategoricalData predicting = dataSet.getPredicting();
        int nameLength = 10;

        for (int pfx = 0; pfx < classCount; ++pfx) {
            nameLength = Math.max(nameLength, predicting.getOptionName(pfx).length() + 2);
        }
        String var7 = "%-" + nameLength;
        System.out.printf(var7 + "s ", "Matrix");
        for (int c = classCount; c > 0; c--) {
            System.out.printf(var7 + "s\t", predicting.getOptionName(classCount - c).toUpperCase());
        }
        System.out.println();

        for (int i = 0; i < confusionMatrix.length; ++i) {
            System.out.printf(var7 + "s ", predicting.getOptionName(i).toUpperCase());

            for (int j = 0; j < classCount - 1; ++j) {
                System.out.printf(var7 + "f ", confusionMatrix[i][j]);
            }

            System.out.printf(var7 + "f\n", confusionMatrix[i][classCount - 1]);
        }
    }

    public void printPredictionTable() {
        System.out.println("Sample\tTrue TMM\tALT pred\tTelomerase pred\n");
        for (int i = 0; i < summaryFileHandler.getSamples().size(); i++) {
            String sample = summaryFileHandler.getSamples().get(i);
            String tmm = tmmLabels.getSampleTMMLabelMap().get(sample);
            String isALT = predictionTable[i][0] ? "+" : "-";
            String isTelomerase = predictionTable[i][1] ? "+" : "-";
            System.out.println(sample + "\t" + tmm + "\t" + isALT + "\t" + isTelomerase + "\n");
        }
    }


    public static void main(String[] args) {

        File summaryFile = new File("c:\\Dropbox\\Bioinformatics_Group\\The_telomere_project\\telomere_network\\alt-tert-networks\\p9.cl.av\\alt-tert\\Untitled_iteration\\psf_summary.xls");
        SummaryFileHandler summaryFileHandler = null;
        try {
            summaryFileHandler = new SummaryFileHandler(summaryFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        File tmmLabelsFile = new File("c:\\Dropbox\\Bioinformatics_Group\\The_telomere_project\\telomere_network\\alt-tert-networks\\p9.cl.av\\tmm_labels.txt");
        TMMLabels tmmLabels = null;
        try {
            tmmLabels = new TMMLabels(tmmLabelsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            SVM svm = new SVM(summaryFileHandler, tmmLabels);
            ClassificationDataSet dataSet = svm.generateDataSet(summaryFileHandler, TMMLabels.A);
            svm.printDataSet(dataSet);
            svm.classify(dataSet);
            svm.printConfusionMatrix(dataSet);
            dataSet = svm.generateDataSet(summaryFileHandler, TMMLabels.T);
            svm.printDataSet(dataSet);
            svm.classify(dataSet);
            svm.printConfusionMatrix(dataSet);
            System.out.println("h: " + svm.getH() + " v: " + svm.getV());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public double getAccuracy() {
        if(accuracy != null)
            return accuracy;
        int correct = 0;
        int wrong = 0;
        // A, T, N, AT
        //truth\prediction
        double[][] allConfusionMatrix = new double[4][4];

        for (int i = 0; i < summaryFileHandler.getSamples().size(); i++) {
            String sample = summaryFileHandler.getSamples().get(i);
            String tmm = tmmLabels.getSampleTMMLabelMap().get(sample);
            String pred;
            if (predictionTable[i][0])
                if (predictionTable[i][1])
                    pred = TMMLabels.AT;
                else
                    pred = TMMLabels.A;
            else if (predictionTable[i][1])
                pred = TMMLabels.T;
            else
                pred = TMMLabels.N;
            if (tmm.equals(pred)){
                correct++;
            } else {
                wrong++;
            }
        }


        accuracy = ((double) correct) / (wrong + correct);
        return accuracy;
    }
}
