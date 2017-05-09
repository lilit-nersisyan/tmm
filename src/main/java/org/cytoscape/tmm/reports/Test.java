package org.cytoscape.tmm.reports;

import com.itextpdf.text.Paragraph;
import org.jfree.chart.JFreeChart;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Lilit Nersisyan on 4/7/2017.
 */
public class Test {
    public static void main(String[] args) {
        File summaryFile = new File("c:\\Dropbox\\Bioinformatics_Group\\The_telomere_project\\telomere_network\\alt-tert-networks\\p9.cl.av\\alt-tert\\Untitled_iteration\\psf_summary.xls");
        File pdfFile = new File("c:\\Dropbox\\Bioinformatics_Group\\The_telomere_project\\telomere_network\\alt-tert-networks\\p9.cl.av\\alt-tert\\Untitled_iteration\\report.pdf");
        File tmmLabelsFile = new File("c:\\Dropbox\\Bioinformatics_Group\\The_telomere_project\\telomere_network\\alt-tert-networks\\p9.cl.av\\tmm_labels.txt");
        SummaryFileHandler summaryFileHandler = null;
        try {
            summaryFileHandler = new SummaryFileHandler(summaryFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TMMLabels tmmLabels = null;
        try {
            tmmLabels = new TMMLabels(tmmLabelsFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        summaryFileHandler.printSummaryMap();
        VolcanoPlotFactory volcanoPlotFactory = null;
        try {
            volcanoPlotFactory = new VolcanoPlotFactory(summaryFileHandler, new TMMLabels(tmmLabelsFile));
        } catch (Exception e) {
            e.printStackTrace();
        }


        JFreeChart[] volcanoCharts = null;
        try {
            volcanoCharts = volcanoPlotFactory.createVolcanoPlots();
        } catch (Exception e) {
            e.printStackTrace();
        }

        BoxPlotFactory boxPlotFactory = new BoxPlotFactory(summaryFileHandler, tmmLabels);

        JFreeChart[] boxplots = null;
        try {
            boxplots = boxPlotFactory.createBoxplots();
        } catch (Exception e) {
            e.printStackTrace();
        }


        float width = 500;
        float height = 500;

        TwoDPlotFactory twoDPlotFactory = null;

        try {
            twoDPlotFactory = new TwoDPlotFactory(summaryFileHandler, tmmLabels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFreeChart twoDChart = null;
        try {
            twoDChart = twoDPlotFactory.create2DPlot();
            SVM svm = new SVM(summaryFileHandler, tmmLabels);
            svm.runSVM();
            twoDPlotFactory.setALTThreshold(twoDChart, svm.getH());
            twoDPlotFactory.setTelomeraseThreshold(twoDChart, svm.getV());
            twoDPlotFactory.setAccuracy(twoDChart, svm.getAccuracy());

        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<JFreeChart> charts = new ArrayList<JFreeChart>();
        charts.add(volcanoCharts[0]);
        charts.add(volcanoCharts[1]);
        charts.add(boxplots[0]);
        charts.add(boxplots[1]);
        charts.add(twoDChart);

        try {
            PlotManager.writeChartAsPDF(pdfFile, new ArrayList<Paragraph>(), charts, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
