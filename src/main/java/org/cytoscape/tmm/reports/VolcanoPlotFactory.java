package org.cytoscape.tmm.reports;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 4/7/2017.
 * <p>
 * Public class VolcanoPlotFactory
 * <p>
 * Creates volcano plots from PSF scores and p values
 */
public class VolcanoPlotFactory {

    private String SCORESKEY = "scores";
    private String PVALUESKEY = "pvalues";
    private String ALTKEY = "ALT";
    private String TELOMERASEKEY = "Telomerase";

    private boolean labeled = false;

    private ArrayList<String> samples;
    private HashMap<String, HashMap<String, HashMap<String, Double>>> summaryMap;
    private HashMap<String, ArrayList<String>> seriesLabels = new HashMap<>();

    private SummaryFileHandler summaryFileHandler;
    private TMMLabels tmmLabels;
    private GroupLabels groupLabels;

    /**
     * Constructor for estimation mode
     *
     * @param summaryFileHandler
     * @param groupLabels        GroupLabels class object containing group labels and colors
     */
    public VolcanoPlotFactory(SummaryFileHandler summaryFileHandler, GroupLabels groupLabels) {
        this.summaryFileHandler = summaryFileHandler;
        summaryMap = summaryFileHandler.getSummaryMap();
        samples = summaryFileHandler.getSamples();
        this.groupLabels = groupLabels;
    }

    /**
     * Constructor for TMM labeled samples
     *
     * @param summaryFileHandler
     * @param tmmLabels
     */
    public VolcanoPlotFactory(SummaryFileHandler summaryFileHandler,
                              TMMLabels tmmLabels) {

        this.summaryFileHandler = summaryFileHandler;
        summaryMap = summaryFileHandler.getSummaryMap();
        samples = summaryFileHandler.getSamples();
        this.tmmLabels = tmmLabels;
        labeled = true;
    }

    /**
     * Creates two volcano plots from PSF scores and p values stored summaryMap from the summaryHanlder.
     *
     * @return returns an array of two charts: one for ALT, the other for Telomerase TMMs
     * @throws Exception
     */
    public JFreeChart[] createVolcanoPlots() throws Exception {

        XYDataset[] datasets = null;
        try {
            datasets = createDatasets();
        } catch (Exception e) {
            throw new Exception("Problem creating datasets for volcano plots: " + e.getMessage());
        }

        JFreeChart altChart = ChartFactory.createScatterPlot(
                "ALT volcano plot",
                "PSF score", "-log2 pvalue", datasets[0],
                PlotOrientation.VERTICAL, true, false, false);

        JFreeChart telomeraseChart = ChartFactory.createScatterPlot(
                "Telomerase volcano plot",
                "PSF score", "-log2 pvalue", datasets[1],
                PlotOrientation.VERTICAL, true, false, false);

        XYPlot altPlot = (XYPlot) altChart.getPlot();
        renderPlot(altPlot, ALTKEY);

        XYPlot telomerasePlot = (XYPlot) telomeraseChart.getPlot();
        renderPlot(telomerasePlot, TELOMERASEKEY);

        return new JFreeChart[]{altChart, telomeraseChart};
    }

    /**
     * Checks if samples are TMM labeled and returns datasets respectively.
     *
     * @return
     * @throws Exception
     */
    private XYDataset[] createDatasets() throws Exception {
        try {
            if (!labeled) {
                return createGroupLabeledDatasets();
            } else {
                return createTMMLabeledDatasets();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Creates ALT and telomerase datasets, each containing a single series of non-labeled sample points.
     *
     * @return an array of XYDatasets, the first being the ALT and the second being the Telomerase dataset
     * @throws Exception
     */
    private XYDataset[] createNonLabeledDatasets() throws Exception {
        XYSeriesCollection altDataset = null;
        XYSeriesCollection telomeraseDataset = null;

        try {
            altDataset = new XYSeriesCollection();
            telomeraseDataset = new XYSeriesCollection();

            XYSeries altSeries = getTMMLabeledSeries(ALTKEY);
            XYSeries telomeraseSeries = getTMMLabeledSeries(TELOMERASEKEY);

            altDataset.addSeries(altSeries);
            telomeraseDataset.addSeries(telomeraseSeries);
        } catch (Exception e) {
            throw new Exception("Problem generating the datasets: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        return new XYSeriesCollection[]{altDataset, telomeraseDataset};
    }

    /**
     * Creates ALT and telomerase datasets, each containing a series of group-labeled sample points.
     *
     * @return an array of XYDatasets, the first being the ALT and the second being the Telomerase dataset
     * @throws Exception
     */
    private XYDataset[] createGroupLabeledDatasets() throws Exception {
        XYSeriesCollection altDataset = null;
        XYSeriesCollection telomeraseDataset = null;
        try {

            altDataset = new XYSeriesCollection();
            for (String groupLabel : groupLabels.getGroups()) {
                XYSeries series = getGroupLabeledSeries(ALTKEY, groupLabel);
                altDataset.addSeries(series);
            }

            telomeraseDataset = new XYSeriesCollection();
            for (String groupLabel : groupLabels.getGroups()) {
                XYSeries series = getGroupLabeledSeries(TELOMERASEKEY, groupLabel);
                telomeraseDataset.addSeries(series);
            }

        } catch (Exception e) {
            throw new Exception("Problem generating the 2D Group labeled TMM dataset: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        return new XYSeriesCollection[]{altDataset, telomeraseDataset};

    }

    /**
     * Returns ALT and Telomerase datasets, each containing multiple series of points, one serie per each TMM label
     *
     * @return
     * @throws Exception
     */
    private XYDataset[] createTMMLabeledDatasets() throws Exception {
        XYSeriesCollection altDataset = null;
        XYSeriesCollection telomeraseDataset = null;
        try {
            String[] seriesKeys = new String[]{TMMLabels.A, TMMLabels.T, TMMLabels.N, TMMLabels.AT};

            altDataset = new XYSeriesCollection();
            for (String seriesKey : seriesKeys) {
                XYSeries series = getTMMLabeledSeries(ALTKEY, seriesKey);
                altDataset.addSeries(series);
            }

            telomeraseDataset = new XYSeriesCollection();
            for (String seriesKey : seriesKeys) {
                XYSeries series = getTMMLabeledSeries(TELOMERASEKEY, seriesKey);
                telomeraseDataset.addSeries(series);
            }

        } catch (Exception e) {
            throw new Exception("Problem generating the 2D TMM dataset: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
        return new XYSeriesCollection[]{altDataset, telomeraseDataset};
    }


    /**
     * Reads the summary map and returns a single series of psf scores and p values.
     *
     * @param tmmKey
     * @return
     * @throws Exception
     */
    private XYSeries getTMMLabeledSeries(String tmmKey) throws Exception {
        XYSeries series = new XYSeries("Unknown TMM", false);
        HashMap<String, Double> scores = getScores(tmmKey);
        HashMap<String, Double> pvalues = getPvalues(tmmKey);

        for (String s : samples) {
            double score = scores.get(s);
            double pvalue = pvalues.get(s);
            double y = getLog2Pvalue(pvalue);
            series.add(score, y);
        }
        return series;
    }

    /**
     * Returns a series of points with psf scores and p values for the specified tmm
     * and the samples with the specified TMM annotation.
     *
     * @param tmmKey    the TMM network key
     * @param seriesKey the TMM annotation of the samples
     * @return XYSeries containing samples with the specified TMM annotation, each sample with psf scores and p values from the specified TMM network
     * @throws Exception
     */
    private XYSeries getTMMLabeledSeries(String tmmKey, String seriesKey) throws Exception {
        XYSeries series = new XYSeries(seriesKey, false);
        HashMap<String, Double> scores = getScores(tmmKey);
        HashMap<String, Double> pvalues = getPvalues(tmmKey);

        ArrayList<String> labels = new ArrayList<>();

        for (String s : samples) {
            if (tmmLabels.getSamples(seriesKey).contains(s)) {
                double score = scores.get(s);
                double pvalue = pvalues.get(s);
                double y = getLog2Pvalue(pvalue);
                series.add(score, y);
                labels.add(s);
            }
        }
        seriesLabels.put(seriesKey, labels);
        return series;
    }

    /**
     * Returns a series of points with psf scores and p values for the specified tmm
     * and the samples with the specified Group label annotation.
     *
     * @param tmmKey     the TMM network key
     * @param groupLabel the group label annotation of the samples
     * @return XYSeries containing samples from the specified group label annotation,
     * each sample with psf scores and p values from the specified TMM network
     * @throws Exception
     */
    private XYSeries getGroupLabeledSeries(String tmmKey, String groupLabel) throws Exception {
        XYSeries series = new XYSeries(groupLabel, false);
        HashMap<String, Double> scores = getScores(tmmKey);
        HashMap<String, Double> pvalues = getPvalues(tmmKey);

        ArrayList<String> labels = new ArrayList<>();

        for (String s : groupLabels.getSamples(groupLabel)) {
            double score = scores.get(s);
            double pvalue = pvalues.get(s);
            double y = getLog2Pvalue(pvalue);
            series.add(score, y);
            labels.add(s);
        }
        seriesLabels.put(groupLabel, labels);
        return series;
    }

    /**
     * Retrieves psf scores for the specified TMM network from the summaryMap.
     *
     * @param tmmKey
     * @return
     * @throws Exception
     */
    private HashMap<String, Double> getScores(String tmmKey) throws Exception {
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

    /**
     * Retrieves psf score p values for the specified TMM network from the summaryMap.
     *
     * @param tmmKey
     * @return
     * @throws Exception
     */
    private HashMap<String, Double> getPvalues(String tmmKey) throws Exception {
        HashMap<String, Double> pvalues = null;
        try {
            pvalues = summaryMap.get(tmmKey).get(PVALUESKEY);
        } catch (Exception e) {
            throw new Exception("Summary map did not contain key " + tmmKey);
        }
        if (pvalues.size() != samples.size())
            throw new Exception("p values for " + tmmKey + " did not contain " + samples.size()
                    + " elements. Actual size: " + pvalues.size());
        return pvalues;
    }


    private void renderPlot(XYPlot plot, String tmmkey) {
        PlotManager.renderBase(plot);
        if (!labeled)
//            PlotManager.setBaseItemLabels(plot, samples);
            PlotManager.setSeriesItemLabels(plot,
                    groupLabels.getGroupSamplesMap(),
                    groupLabels.getGroupColorsMap());
        else {
            PlotManager.setSeriesItemLabels(plot, seriesLabels, tmmLabels);
        }

        double[] domainRange = summaryFileHandler.getPSFRange(tmmkey);
        plot.getDomainAxis().setRange(domainRange[0] - 0.5, domainRange[1] + 0.5);
        plot.getRangeAxis().setRange(0, 11);

        PlotManager.drawLine(plot, true, getLog2Pvalue(0.05), Color.red, "p = 0.05");
        PlotManager.drawLine(plot, true, getLog2Pvalue(0.2), Color.gray, "p = 0.2");
    }

    private class MyLabelGenerator implements XYItemLabelGenerator {
        private ArrayList<String> seriesLabels;

        public MyLabelGenerator(ArrayList<String> seriesLabels) {
            this.seriesLabels = seriesLabels;
        }

        @Override
        public String generateLabel(XYDataset dataset, int series, int item) {
            return seriesLabels.get(item);
        }

    }

    private double getLog2Pvalue(double pvalue) {
        double y = 0;
        try {
            if (pvalue < 0.001)
                pvalue = 0.001;
            y = -1 * Math.log(pvalue) / Math.log(2);
        } catch (Exception e) {
            System.out.println("Could not convert p value " + pvalue + " to log2" + e.getMessage());
        }
        return y;
    }

}
