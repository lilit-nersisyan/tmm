package org.cytoscape.tmm.reports;

import org.cytoscape.tmm.gui.DoubleFormatter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.Array;
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
    private boolean labeled = false;

    private double[] domainRange = new double[]{0, 0};
    private double[] rangeRange = new double[]{0, 0};

    private ArrayList<String> samples;
    private HashMap<String, Integer> seriesIndex = new HashMap<>();
    private HashMap<String, HashMap<String, HashMap<String, Double>>> summaryMap;
    private HashMap<String, ArrayList<String>> seriesLabels = new HashMap<>();

    private final SummaryFileHandler summaryFileHandler;
    private TMMLabels tmmLabels;
    private double accuracy;


    /**
     * Constructor for non-labeled samples.
     *
     * @param summaryFileHandler
     * @throws Exception
     */
    public TwoDPlotFactory(SummaryFileHandler summaryFileHandler) {
        this.summaryFileHandler = summaryFileHandler;
        summaryMap = summaryFileHandler.getSummaryMap();
        samples = summaryFileHandler.getSamples();
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

        JFreeChart tmm2Dchart = ChartFactory.createScatterPlot(
                "TMM scores 2D plot",
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
            return createNonLabeledDataset();
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
    private XYDataset createNonLabeledDataset() throws Exception{
        XYSeriesCollection tmmDataset = null;

        try {
            tmmDataset = new XYSeriesCollection();
            XYSeries tmmSeries = get2DSeries();
            tmmDataset.addSeries(tmmSeries);
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
        XYSeries series = new XYSeries("Uknown TMM");
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
     * Create a single series of points with(telomerase score, ALT score) coordinates for the specified TMM label.
     * Populates the seriesLabels map with the list of sample names for the specified TMM. This map will be used for labeling the points on the 2D plot.
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
                series.add(telomeraseScores.get(s), altScores.get(s));
                labels.add(s);
            }
        }
        seriesLabels.put(seriesKey, labels);
        return series;
    }

    private void renderPlot(XYPlot plot) {
        PlotManager.renderBase(plot);
        if(!labeled)
            PlotManager.setBaseItemLabels(plot, samples);
        else{
            PlotManager.setSeriesItemLabels(plot, seriesLabels, tmmLabels);
        }

        domainRange[1] = summaryFileHandler.getPSFRange(TELOMERASEKEY)[1];
        rangeRange[1] = summaryFileHandler.getPSFRange(ALTKEY)[1];
        plot.getDomainAxis().setRange(domainRange[0] - .25, domainRange[1] + .25);
        plot.getRangeAxis().setRange(rangeRange[0], rangeRange[1] + .25);
    }

    /**
     * Draws a horizontal line at the threshold value of the ALT axis.
     *
     * @param twoDChart
     * @param h - the y axis threshold
     */
    public void setALTThreshold(JFreeChart twoDChart, double h) throws Exception {
        XYPlot plot = (XYPlot) twoDChart.getPlot();
        if( h < plot.getRangeAxis().getRange().getLowerBound())
            System.out.println("the specified threshold of " +
                    h + " is lower than the lower bound of the y axis: " + plot.getRangeAxis().getRange().getLowerBound());
        if( h > plot.getRangeAxis().getRange().getUpperBound())
            System.out.println("the specified threshold of " +
                    h + " is greater than the upper bound of the y axis: " +
                    plot.getRangeAxis().getRange().getUpperBound());

        PlotManager.drawLine(plot, true, h);
    }


    /**
     * Draws a vertical line at the threshold value of the Telomerase axis
     *
     * @param twoDChart
     * @param v - the x axis threshold
     */
    public void setTelomeraseThreshold(JFreeChart twoDChart, double v) throws Exception {
        XYPlot plot = (XYPlot) twoDChart.getPlot();
        if( v < plot.getDomainAxis().getRange().getLowerBound())
            System.out.println("the specified threshold of " +
                    v + " is lower than the lower bound of the y axis: " + plot.getRangeAxis().getRange().getLowerBound());
        if( v > plot.getDomainAxis().getRange().getUpperBound())
            System.out.println("the specified threshold of " +
                    v + " is greater than the upper bound of the y axis: " +
                    plot.getRangeAxis().getRange().getUpperBound());

        PlotManager.drawLine(plot, false, v);
    }

    /**
     * Draws a vertical line at the threshold value of the Telomerase axis
     *
     * @param twoDChart
     * @param v - the accuracy value
     */
    public void setAccuracy(JFreeChart twoDChart, double v) throws Exception {
        TextTitle legendText = new TextTitle("Classification accuracy:   "
                + DoubleFormatter.formatDouble(v) + "\n");

        legendText.setPosition(RectangleEdge.BOTTOM);
        legendText.setTextAlignment(HorizontalAlignment.CENTER);
        legendText.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        twoDChart.addSubtitle(legendText);
    }
}
