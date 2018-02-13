package org.cytoscape.tmm.reports;

import org.cytoscape.tmm.gui.DoubleFormatter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.HashMap;

import static org.cytoscape.tmm.reports.SummaryFileHandler.ALTKEY;
import static org.cytoscape.tmm.reports.SummaryFileHandler.TELOMERASEKEY;

/**
 * Created by Lilit Nersisyan on 4/11/2017.
 * <p>
 * Public class TwoDPlotFactory
 * <p>
 * Creates a 2D map of Telomerase and ALT PSF values.
 */

public class TwoDPlotFactory {

    private String SCORESKEY = "scores";
    public Double LOGINCREMENT = 0.01;
    private boolean labeled = false;
    private boolean logScale = false;

    private double[] domainRange = new double[]{0, 0};
    private double[] rangeRange = new double[]{0, 0};

    private ArrayList<String> samples;
    private HashMap<String, Integer> seriesIndex = new HashMap<>();
    private HashMap<String, HashMap<String, HashMap<String, Double>>> summaryMap;
    private HashMap<String, ArrayList<String>> seriesLabels = new HashMap<>();
    private ArrayList<String> baseLabels = new ArrayList<>();

    private final SummaryFileHandler summaryFileHandler;
    private TMMLabels tmmLabels;
    private GroupLabels groupLabels;
    private double accuracy;
    private boolean drawPointLabels = true;


    /**
     * Constructor for estimation mode.
     *
     * @param summaryFileHandler
     * @param groupLabels        Grouplabels class object
     * @throws Exception
     */
    public TwoDPlotFactory(SummaryFileHandler summaryFileHandler, GroupLabels groupLabels) {
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
     * @throws Exception
     */
    public TwoDPlotFactory(SummaryFileHandler summaryFileHandler, TMMLabels tmmLabels) {
        this.summaryFileHandler = summaryFileHandler;
        this.tmmLabels = tmmLabels;
        labeled = true;

        summaryMap = summaryFileHandler.getSummaryMap();
        samples = summaryFileHandler.getSamples();
    }


    /**
     * If drawPointLables is set to true (default), the points will have labels on the plot,
     * and not otherwise.
     *
     * @param drawPointLabels
     */
    public void setDrawPointLabels(boolean drawPointLabels) {
        this.drawPointLabels = drawPointLabels;
    }

    /**
     * Creates a 2D scatter plot.
     * The X axis represents Telomerase PSF scores, the Y axis - ALT PSF scores
     *
     * @return JFreeChart
     * @throws Exception
     */
    public JFreeChart create2DPlot() throws Exception {

        XYDataset dataset = null;
        try {
            dataset = createDataset();
        } catch (Exception e) {
            throw new Exception("Problem creating dataset: " + e.getMessage());
        }

        String chartTitle = "TMM scores 2D plot";
        if(drawPointLabels){
            chartTitle = chartTitle + ": labeled";
        }

        if(logScale)
            chartTitle = chartTitle + " (log2 scale)";

        JFreeChart tmm2Dchart = ChartFactory.createScatterPlot(
                chartTitle,
                "Telomerase PSF score", "ALT PSF score", dataset,
                PlotOrientation.VERTICAL, true, false, false);

        XYPlot plot = (XYPlot) tmm2Dchart.getPlot();
        renderPlot(plot);

        return tmm2Dchart;
    }


    /**
     * Reads the PSF scores from the summarfilehandler. If labeled, reads the TMM labels from tmmLabels.
     * Creates an XYDataset with single series in the non-labeled case,
     * and with multiple series in case of TMM labels (one serie for each TMM).
     *
     * @return
     * @throws Exception
     */
    private XYDataset createDataset() throws Exception {
        try {
            if (labeled)
                return createLabeledDataset();
            return createGroupLabeledDataset();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Reads the PSF scores from the summarfilehandler.
     * Creates an XYDataset with single series for all samples.
     *
     * @return
     * @throws Exception
     */
    private XYDataset createGroupLabeledDataset() throws Exception {
        XYSeriesCollection tmmDataset = null;

        try {
            tmmDataset = new XYSeriesCollection();
            int i = 0;
            for (String group : groupLabels.getGroups()) {
                XYSeries series = getGroup2DSeries(group);
                tmmDataset.addSeries(series);
                seriesIndex.put(group, i++);
            }
        } catch (Exception e) {
            throw new Exception("Problem generating the 2D TMM dataset: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

        return tmmDataset;
    }

    /**
     * Reads the PSF scores from the summarfilehandler and TMM labels from tmmLabels.
     * Creates an XYDataset with multiple series - one for each TMM label.
     *
     * @return
     * @throws Exception
     */
    private XYDataset createLabeledDataset() throws Exception {
        XYSeriesCollection tmmDataset = null;

        try {
            int i = 0;
            tmmDataset = new XYSeriesCollection();
            XYSeries ASeries = get2DSeries(TMMLabels.A);
            tmmDataset.addSeries(ASeries);
            seriesIndex.put(TMMLabels.A, i++);

            XYSeries TSeries = get2DSeries(TMMLabels.T);
            tmmDataset.addSeries(TSeries);
            seriesIndex.put(TMMLabels.T, i++);

            XYSeries NSeries = get2DSeries(TMMLabels.N);
            tmmDataset.addSeries(NSeries);
            seriesIndex.put(TMMLabels.N, i++);

            XYSeries ATSeries = get2DSeries(TMMLabels.AT);
            tmmDataset.addSeries(ATSeries);
            seriesIndex.put(TMMLabels.AT, i);
        } catch (Exception e) {
            throw new Exception("Problem generating the 2D TMM dataset: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
        return tmmDataset;
    }


    /**
     * Create a single series of points with(telomerase score, ALT score) coordinates.
     *
     * @return
     * @throws Exception
     */
    private XYSeries get2DSeries() throws Exception {
        XYSeries series = new XYSeries("Uknown TMM", false);
        HashMap<String, Double> altScores = summaryMap.get(ALTKEY).get(SCORESKEY);
        HashMap<String, Double> telomeraseScores = summaryMap.get(TELOMERASEKEY).get(SCORESKEY);

        if (altScores.size() != samples.size())
            throw new Exception("scores for " + ALTKEY + " did not contain " + samples.size()
                    + " elements" + altScores.size());
        if (telomeraseScores.size() != samples.size())
            throw new Exception("p values for " + TELOMERASEKEY + " did not contain " + samples.size()
                    + " elements. Actual size: " + telomeraseScores.size());

        for (String s : samples) {
            double altScore = altScores.get(s);
            double telomeraseScore = telomeraseScores.get(s);
            series.add(telomeraseScore, altScore);
        }
        return series;
    }


    /**
     * Create a single series of points with(telomerase score, ALT score) coordinates
     * for samples belonging to the specified group label.
     *
     * @return
     * @throws Exception
     */
    private XYSeries getGroup2DSeries(String groupLabel) throws Exception {
        XYSeries series = new XYSeries(groupLabel, false);
        HashMap<String, Double> altScores = summaryMap.get(ALTKEY).get(SCORESKEY);
        HashMap<String, Double> telomeraseScores = summaryMap.get(TELOMERASEKEY).get(SCORESKEY);

        if (altScores.size() != samples.size())
            throw new Exception("scores for " + ALTKEY + " did not contain " + samples.size()
                    + " elements" + altScores.size());
        if (telomeraseScores.size() != samples.size())
            throw new Exception("p values for " + TELOMERASEKEY + " did not contain " + samples.size()
                    + " elements. Actual size: " + telomeraseScores.size());

        ArrayList<String> labels = new ArrayList<>();

        for (String s : groupLabels.getSamples(groupLabel)) {
            series.add(logScale ? Math.log(telomeraseScores.get(s) + LOGINCREMENT) : telomeraseScores.get(s),
                    logScale ? Math.log(altScores.get(s) + LOGINCREMENT) : altScores.get(s));
            labels.add(s);
        }
        seriesLabels.put(groupLabel, labels);
        return series;
    }

    /**
     * Create a single series of points with(telomerase score, ALT score) coordinates for the specified TMM label.
     * Populates the seriesLabels map with the list of sample names for the specified TMM.
     * This map will be used for labeling the points on the 2D plot.
     *
     * @param seriesKey the TMM label
     * @return
     * @throws Exception
     */
    private XYSeries get2DSeries(String seriesKey) throws Exception {
        XYSeries series = new XYSeries(seriesKey, false);
        HashMap<String, Double> altScores = summaryMap.get(ALTKEY).get(SCORESKEY);
        HashMap<String, Double> telomeraseScores = summaryMap.get(TELOMERASEKEY).get(SCORESKEY);

        if (altScores.size() != samples.size())
            throw new Exception("scores for " + ALTKEY + " did not contain " + samples.size()
                    + " elements" + altScores.size());
        if (telomeraseScores.size() != samples.size())
            throw new Exception("p values for " + TELOMERASEKEY + " did not contain " + samples.size()
                    + " elements. Actual size: " + telomeraseScores.size());

        ArrayList<String> labels = new ArrayList<>();

        for (String s : samples) {
            if (tmmLabels.getSamples(seriesKey).contains(s)) {
                series.add(logScale ? Math.log(telomeraseScores.get(s) + LOGINCREMENT) : telomeraseScores.get(s),
                        logScale ? Math.log(altScores.get(s) + LOGINCREMENT) : altScores.get(s));
                labels.add(s);
            }
        }
        seriesLabels.put(seriesKey, labels);
        return series;
    }

    private void renderPlot(XYPlot plot) {
        PlotManager.renderBase(plot);
        if (drawPointLabels) {
            if (!labeled)
//                PlotManager.setBaseItemLabels(plot, samples);
                PlotManager.setSeriesItemLabels(plot,
                        groupLabels.getGroupSamplesMap(),
                        groupLabels.getGroupColorsMap());
            else {
                PlotManager.setSeriesItemLabels(plot, seriesLabels, tmmLabels);
            }
        } else {
//            if (!labeled) {
            if (false) { // will be removed from tmm_0.5 if works
                ArrayList<String> noSamples = new ArrayList<>();
                for (String s : samples)
                    noSamples.add("");
                PlotManager.setBaseItemLabels(plot, noSamples);

            } else {
                HashMap<String, ArrayList<String>> seriesNoLabels = new HashMap<>();
                for (String tmmk : seriesLabels.keySet()) {
                    ArrayList<String> nolabarray = new ArrayList<>();
                    ArrayList<String> labarray = seriesLabels.get(tmmk);
                    for (String v : labarray) {
                        nolabarray.add("");
                    }
                    seriesNoLabels.put(tmmk, nolabarray);
                }
                if (tmmLabels == null)
                    PlotManager.setSeriesItemLabels(plot, seriesNoLabels, groupLabels);
                else
                    PlotManager.setSeriesItemLabels(plot, seriesNoLabels, tmmLabels);
            }
        }

//        domainRange[1] = summaryFileHandler.getPSFRange(TELOMERASEKEY)[1];
//        rangeRange[1] = summaryFileHandler.getPSFRange(ALTKEY)[1];
        //tmm0.2
        domainRange = summaryFileHandler.getPSFRange(TELOMERASEKEY);
        rangeRange = summaryFileHandler.getPSFRange(ALTKEY);

        double xmin, xmax, ymin, ymax;

        if(logScale){
            xmin = Math.log(domainRange[0] + LOGINCREMENT);
            ymin = Math.log(rangeRange[0] + LOGINCREMENT);
            xmax = Math.log(domainRange[1] + LOGINCREMENT);
            ymax = Math.log(rangeRange[1] + LOGINCREMENT);
        } else {
            xmin = domainRange[0];
            xmax = domainRange[1];
            ymin = rangeRange[0];
            ymax = rangeRange[1];
        }
        double xMargin = (domainRange[1] - domainRange[0]) / 10;
        if (xMargin < 0.1)
            xMargin = 0.1;
        double yMargin = (rangeRange[1] - rangeRange[0]) / 10;
        if (yMargin < 0.1)
            yMargin = 0.1;
        xmin = xmin - xMargin;
        xmax = xmax + xMargin;
        ymin = ymin - yMargin;
        ymax = ymax + yMargin;


        plot.getDomainAxis().setRange(xmin, xmax);
        plot.getRangeAxis().setRange(ymin , ymax);
    }

    /**
     * Draws a horizontal line at the threshold value of the ALT axis.
     *
     * @param twoDChart
     * @param h         - the y axis threshold
     */
    public void setALTThreshold(JFreeChart twoDChart, double h) throws Exception {
        XYPlot plot = (XYPlot) twoDChart.getPlot();
        if (h < plot.getRangeAxis().getRange().getLowerBound())
            System.out.println("the specified threshold of " +
                    h + " is lower than the lower bound of the y axis: " + plot.getRangeAxis().getRange().getLowerBound());
        if (h > plot.getRangeAxis().getRange().getUpperBound())
            System.out.println("the specified threshold of " +
                    h + " is greater than the upper bound of the y axis: " +
                    plot.getRangeAxis().getRange().getUpperBound());

        PlotManager.drawLine(plot, true, h);
    }


    /**
     * Draws a vertical line at the threshold value of the Telomerase axis
     *
     * @param twoDChart
     * @param v         - the x axis threshold
     */
    public void setTelomeraseThreshold(JFreeChart twoDChart, double v) throws Exception {
        XYPlot plot = (XYPlot) twoDChart.getPlot();
        if (v < plot.getDomainAxis().getRange().getLowerBound())
            System.out.println("the specified threshold of " +
                    v + " is lower than the lower bound of the y axis: " + plot.getRangeAxis().getRange().getLowerBound());
        if (v > plot.getDomainAxis().getRange().getUpperBound())
            System.out.println("the specified threshold of " +
                    v + " is greater than the upper bound of the y axis: " +
                    plot.getRangeAxis().getRange().getUpperBound());

        PlotManager.drawLine(plot, false, v);
    }

    /**
     * Draws a vertical line at the threshold value of the Telomerase axis
     *
     * @param twoDChart
     * @param v         - the accuracy value
     */
    public void setAccuracy(JFreeChart twoDChart, double v) throws Exception {
        TextTitle legendText = new TextTitle("Classification accuracy:   "
                + DoubleFormatter.formatDouble(v) + "\n");

        legendText.setPosition(RectangleEdge.BOTTOM);
        legendText.setTextAlignment(HorizontalAlignment.CENTER);
        legendText.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        twoDChart.addSubtitle(legendText);
    }

    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }
}
