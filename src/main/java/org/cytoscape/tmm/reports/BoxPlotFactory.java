package org.cytoscape.tmm.reports;

import jsc.independentsamples.MannWhitneyMedianDifferenceCI;
import jsc.independentsamples.MannWhitneyTest;
import org.cytoscape.tmm.gui.DoubleFormatter;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by Lilit Nersisyan on 4/15/2017.
 * public class BoxPlotFactory
 * <p>
 * Generates boxplots highlighting the difference in PSF values for each pathway between
 * samples with different TMM labels
 */
public class BoxPlotFactory {
    private String ALTKEY = "ALT";
    private String TELOMERASEKEY = "Telomerase";
    private String SCORESKEY = "scores";
    public static String KWP = "kwp";
    public static String MD1 = "MD1";
    public static String p1 = "p1";
    public static String MD2 = "MD2";
    public static String p2 = "p2";

    private ArrayList<String> samples;
    private HashMap<String, HashMap<String, HashMap<String, Double>>> summaryMap;
    private final HashMap<String, ArrayList<String>> labelSamplesMap;

    private SummaryFileHandler summaryFileHandler;
    private TMMLabels tmmLabels;
    private HashMap<String, Double> boxplotStats;

    /**
     * Basic constructor
     *
     * @param summaryFileHandler
     * @param tmmLabels
     */
    public BoxPlotFactory(SummaryFileHandler summaryFileHandler, TMMLabels tmmLabels) {
        this.summaryFileHandler = summaryFileHandler;
        this.tmmLabels = tmmLabels;
        summaryMap = summaryFileHandler.getSummaryMap();
        samples = summaryFileHandler.getSamples();
        labelSamplesMap = tmmLabels.getLabelSamplesMap();
    }

    public HashMap<String, Double> getBoxplotStats() {
        return boxplotStats;
    }

    /**
     * Creates to boxplots for ALT and Telomerase network PSFs.
     *
     * @return returns a JFreeChart[] array containing two boxplots: first is for ALT, second is for Telomerase TMM
     * @throws Exception
     */
    public JFreeChart[] createBoxplots() throws Exception {
        JFreeChart altBoxplot = createBoxplot(ALTKEY);
        JFreeChart telomeraseBoxplot = createBoxplot(TELOMERASEKEY);
        return new JFreeChart[]{altBoxplot, telomeraseBoxplot};
    }

    /**
     * Creates  a boxplot chart for the given TMM.
     *
     * @param tmmKey the TMM for which to draw the boxplot
     * @return returns a JFreeChart boxplot
     * @throws Exception
     */
    public JFreeChart createBoxplot(String tmmKey) throws Exception {
        BoxAndWhiskerCategoryDataset dataset = createSampleDataset(tmmKey);
        CategoryAxis xAxis = new CategoryAxis("Sample TMM annotation");
        NumberAxis yAxis = new NumberAxis("PSF");
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        int n = dataset.getRowCount();
        for (int series = 0; series < n; series++) {
            Paint color = tmmLabels.getLabelColor((String) dataset.getRowKey(series));
            renderer.setSeriesPaint(series, color);
        }

        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        renderer.setMeanVisible(false);

        JFreeChart boxplot = new JFreeChart(
                tmmKey + " PSF boxplot",
                new Font("SansSerif", Font.BOLD, 14),
                plot,
                true
        );

        //If only one sample is present, no need to perform statistics
        if (dataset.getRowCount() < 2)
            return boxplot;

        // Statistics legends
        boxplotStats = new HashMap<>();
        double p = DoubleFormatter.formatDouble(kwt(dataset, tmmKey), 3);
        boxplotStats.put(KWP, p);


        try {
            double[] test1 = medDiff(dataset, tmmKey, TMMLabels.A, TMMLabels.N);
            double[] test2 = medDiff(dataset, tmmKey, TMMLabels.A, TMMLabels.T);
            boxplotStats.put(MD1, test1[0]);
            boxplotStats.put(p1, test1[1]);
            boxplotStats.put(MD2, test2[0]);
            boxplotStats.put(p2, test2[1]);

            double diff1 = DoubleFormatter.formatDouble(test1[0]);
            double p1 = DoubleFormatter.formatDouble(test1[1], 3);
            double diff2 = DoubleFormatter.formatDouble(test2[0]);
            double p2 = DoubleFormatter.formatDouble(test2[1], 3);
            TextTitle legendText = new TextTitle(formatLegend(p, diff1, p1, diff2, p2));
            TextTitle legendTitle = new TextTitle("Statistics");
            legendTitle.setPosition(RectangleEdge.BOTTOM);
            legendTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

            legendText.setPosition(RectangleEdge.BOTTOM);
            legendText.setTextAlignment(HorizontalAlignment.LEFT);
            legendText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));

            boxplot.addSubtitle(legendText);
            boxplot.addSubtitle(legendTitle);

        } catch (Exception e) {
            boxplotStats.put(MD1, Double.NaN);
            boxplotStats.put(p1, Double.NaN);
            boxplotStats.put(MD2, Double.NaN);
            boxplotStats.put(p2, Double.NaN);
            TextTitle legendText = new TextTitle("Could not compute pairwise stats. " +
                    "\nReason: " + e.getMessage());
            TextTitle legendTitle = new TextTitle("Statistics");
            legendTitle.setPosition(RectangleEdge.BOTTOM);
            legendTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

            legendText.setPosition(RectangleEdge.BOTTOM);
            legendText.setTextAlignment(HorizontalAlignment.LEFT);
            legendText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));

            boxplot.addSubtitle(legendText);
            boxplot.addSubtitle(legendTitle);

            e.printStackTrace();
        }

        return boxplot;
    }

    /**
     * Creates a boxandwhiskerCategeoryDataset for the given TMM.
     * The the samples under a specific label are taken from the labelSamplesMap,
     * while the PSF values for each sample are taken from summaryMap.
     * Only the the labels for which PSF values are available will be in the final dataset.
     * The dataset only contains summary statistics for each label.
     *
     * @param tmmKey
     * @return returns BoxAndWhiskerCategoryDataset
     */
    private BoxAndWhiskerCategoryDataset createSampleDataset(String tmmKey) {
        DefaultBoxAndWhiskerCategoryDataset dataset
                = new DefaultBoxAndWhiskerCategoryDataset();
        for (String label : labelSamplesMap.keySet()) {
            if (labelSamplesMap.get(label).size() > 0) {
                List list = new ArrayList<>();
                for (String sample : labelSamplesMap.get(label)) {
                    if (summaryMap.get(tmmKey).get(SCORESKEY).containsKey(sample)) {
                        double psf = summaryMap.get(tmmKey).get(SCORESKEY).get(sample);
                        list.add(psf);
                    }
                }
                if (list.size() > 0)
                    dataset.add(list, label, label);
            }
        }
        return dataset;
    }

    /**
     * Gathers data for the given TMM and calls the Stats.rankSum() test to generate
     * an overall significance value for boxplot differences, using Kruskal-Wallis rank sum test.
     * The categories (labels) are taken from the input dataset,
     * while the respective sample names and PSF values are taken from the labelSamplesMap and summaryMap.
     *
     * @param dataset the BoxAndWhiskerCategoryDataset for which the value is generated
     * @param tmmkey  the TMM value
     * @return returns the p value of Kruskal-Wallis rank sum test
     */
    private double kwt(BoxAndWhiskerCategoryDataset dataset, String tmmkey) {
        ArrayList<Double> dataList = new ArrayList<>();
        ArrayList<String> labelList = new ArrayList<>();
        for (Object row : dataset.getRowKeys()) {
            String label = (String) row;
            ArrayList<String> samples = labelSamplesMap.get(label);
            for (String sample : samples) {
                if (summaryMap.get(tmmkey).get(SCORESKEY).containsKey(sample)) {
                    double psf = summaryMap.get(tmmkey).get(SCORESKEY).get(sample);
                    dataList.add(psf);
                    labelList.add(label);
                }

            }
        }
        double[] data = new double[dataList.size()];
        for (int i = 0; i < dataList.size(); i++)
            data[i] = dataList.get(i);


        String[] labels = new String[labelList.size()];
        labelList.toArray(labels);

        double p = Double.NaN;
        try {
            p = Stats.rankSum(data, labels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    /**
     * Median difference and p value for a pair of categories based on MannWhitney test.
     *
     * @param dataset the dataset to take categories from
     * @param tmmkey  the TMM
     * @param l1      the first category label
     * @param l2      the second category label
     * @return returns a double array of size two, where the first entry is the median difference, and the second entry is the (MannWhitney) p value of this difference
     */
    private double[] medDiff(BoxAndWhiskerCategoryDataset dataset,
                             String tmmkey, String l1, String l2) {
        ArrayList<Double> dataList = new ArrayList<>();
        ArrayList<String> labelList = new ArrayList<>();

        String label = l1;
        ArrayList<String> samples = labelSamplesMap.get(label);
        double[] data1 = new double[samples.size()];
        int i = 0;
        for (String sample : samples) {
            double psf = summaryMap.get(tmmkey).get(SCORESKEY).get(sample);
            dataList.add(psf);
            labelList.add(label);
            data1[i++] = psf;
        }

        label = l2;
        samples = labelSamplesMap.get(label);
        double[] data2 = new double[samples.size()];
        i = 0;
        for (String sample : samples) {
            double psf = summaryMap.get(tmmkey).get(SCORESKEY).get(sample);
            dataList.add(psf);
            labelList.add(label);
            data2[i++] = psf;
        }
        MannWhitneyMedianDifferenceCI mw = new MannWhitneyMedianDifferenceCI(data1, data2, 0.05);
        double diff = mw.getPointEstimate();
        MannWhitneyTest mwt = new MannWhitneyTest(data1, data2);
        double p = mwt.getSP();

        return new double[]{diff, p};
    }

    private String formatLegend(double p, double diff1, double p1, double diff2, double p2) {
        int[] cols = new int[]{20, 15, 10, 10, 5};
        String row1Col1 = "Overall p value:";
        String row1Space2 = "";
        String row1Col2 = p + "";
        int space = cols[0] - row1Col1.length();
        row1Space2 = spaces(space);
        String row1 = row1Col1 + row1Space2 + row1Col2;

        String col1 = "ALT vs normal";
        String col2 = "Median Diff:";
        String col3 = diff1 + "";
        String col4 = "p value:";
        String col5 = p1 + "";
        String space12 = spaces(cols[0] - col1.length());
        String space23 = spaces(cols[1] - col2.length());
        String space34 = spaces(cols[2] - col3.length());
        String space45 = spaces(cols[3] - col4.length());

        String row2 = col1 + space12 + col2 + space23 + col3 + space34 + col4 + space45 + col5;

        col1 = "ALT vs telomerase";
        col2 = "Median Diff:";
        col3 = diff2 + "";
        col4 = "p value:";
        col5 = p2 + "";
        space12 = spaces(cols[0] - col1.length());
        space23 = spaces(cols[1] - col2.length());
        space34 = spaces(cols[2] - col3.length());
        space45 = spaces(cols[3] - col4.length());

        String row3 = col1 + space12 + col2 + space23 + col3 + space34 + col4 + space45 + col5;

        return row1 + "\n" + row2 + "\n" + row3;
    }

    private String spaces(int num) {
        String spaces = "";
        for (int i = 0; i < num; i++)
            spaces += " ";
        return spaces;
    }


}
