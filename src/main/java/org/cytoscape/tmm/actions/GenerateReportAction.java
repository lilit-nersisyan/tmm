package org.cytoscape.tmm.actions;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.DoubleFormatter;
import org.cytoscape.tmm.processing.ParsedFilesDirectory;
import org.cytoscape.tmm.reports.*;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 4/7/2017.
 */
public class GenerateReportAction extends AbstractCyAction {
    private String iterationTitle;
    private String comment;
    private File tmmLabelsFile;
    private File summaryFile;
    private File reportDir;
    private File pdfFile;
    private File numericFile;
    private SummaryFileHandler summaryFileHandler;
    private TMMLabels tmmLabels;
    private SVM svm;
    private HashMap<String, HashMap<String, Double>> boxplotStats;
    private int bootCycles;
    private ParsedFilesDirectory parsedFilesDirectory;

    /**
     * Standart constructor for the non-labeled samples case.
     *  @param name        the name of the action
     * @param summaryFile the file containing PSF summary values
     * @param reportDir     the output pdf file
     * @param parsedFilesDirectory
     */
    public GenerateReportAction(String name, File summaryFile, File reportDir,
                                String iterationTitle, String comment, int bootCycles, ParsedFilesDirectory parsedFilesDirectory) {
        super(name);
        this.summaryFile = summaryFile;
        this.reportDir = reportDir;
        this.iterationTitle = iterationTitle;
        this.comment = comment;
        this.pdfFile = new File(reportDir, iterationTitle + "_report.pdf");
        this.numericFile = new File(reportDir, iterationTitle + "_scores.txt");
        this.bootCycles = bootCycles;
        this.parsedFilesDirectory = parsedFilesDirectory;
    }

    /**
     * Standart constructor for the case of annotated labeled samples.
     *  @param name          the name of the action
     * @param summaryFile   the file containing SPF summary values
     * @param reportDir       the output pdf file
     * @param tmmLabelsFile the file containing TMM annotations
     * @param parsedFilesDirectory
     */
    public GenerateReportAction(String name, File summaryFile, File reportDir,
                                File tmmLabelsFile, String iterationTitle, String comment, int bootCycles, ParsedFilesDirectory parsedFilesDirectory) {
        super(name);
        this.summaryFile = summaryFile;
        this.reportDir = reportDir;
        this.tmmLabelsFile = tmmLabelsFile;
        this.iterationTitle = iterationTitle;
        this.comment = comment;
        this.pdfFile = new File(reportDir, iterationTitle + "_report.pdf");
        this.numericFile = new File(reportDir, iterationTitle + "_scores.txt");
        this.bootCycles = bootCycles;
        this.parsedFilesDirectory = parsedFilesDirectory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final DrawReportTask drawReportTask = new DrawReportTask();
        TaskIterator taskIterator = new TaskIterator(drawReportTask);
        TMMActivator.taskManager.execute(taskIterator);
    }

    public File getPdfFile() {
        return pdfFile;
    }

    private class DrawReportTask extends AbstractTask {

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("TMM report generation");
            taskMonitor.setStatusMessage("Generating report");
            taskMonitor.setProgress(0.1);

            try {
                //read summaryfile and store in summaryFileHandler
                summaryFileHandler = null;
                try {
                    summaryFileHandler = new SummaryFileHandler(summaryFile);
                } catch (Exception e) {
                    throw new Exception("Problem handling summary file: " + e.getMessage());
                }
                summaryFileHandler.printSummaryMap();

                //If tmmLabelsFile is supplied, read tmm annotations and store in TMMLabels
                tmmLabels = null;
                if (tmmLabelsFile != null) {
                    try {
                        tmmLabels = new TMMLabels(tmmLabelsFile);
                    } catch (Exception e) {
                        throw new Exception("Problem handling TMM labels file: " + e.getMessage());
                    }
                }

                taskMonitor.setProgress(0.2);

                try {
                    writeNumericOutput();
                } catch (FileNotFoundException e) {
                    throw new Exception("Problem writing numeric PSF valus output to file. " +
                            "\n Reason: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                }

                ArrayList<JFreeChart> charts = new ArrayList<>();

                // Create volcano plots
                VolcanoPlotFactory volcanoPlotFactory = null;
                try {
                    if (tmmLabels != null)
                        volcanoPlotFactory = new VolcanoPlotFactory(summaryFileHandler, tmmLabels);
                    else
                        volcanoPlotFactory = new VolcanoPlotFactory(summaryFileHandler);
                    JFreeChart[] volcanoCharts = null;
                    volcanoCharts = volcanoPlotFactory.createVolcanoPlots();
                    for (int i = 0; i < volcanoCharts.length; i++)
                        charts.add(volcanoCharts[i]);
                } catch (Exception e) {
                    throw new Exception("Could not generate volcano plots: " + e.getMessage());
                }

                taskMonitor.setProgress(0.4);
                // finished with volcano plots

                // if labeled, crate boxplots
                if (tmmLabels != null) {
                    BoxPlotFactory boxPlotFactory = new BoxPlotFactory(summaryFileHandler, tmmLabels);

                    JFreeChart[] boxplots = null;
                    try {
                        boxplots = boxPlotFactory.createBoxplots();
                        for (int i = 0; i < boxplots.length; i++)
                            charts.add(boxplots[i]);
                        boxplotStats = boxPlotFactory.getBoxplotStats();
                    } catch (Exception e) {
                        throw new Exception("Could not generate boxplots: " + e.getMessage());
                    }
                }
                // finished with boxplots

                taskMonitor.setProgress(0.6);

                // generate 2D plots
                TwoDPlotFactory twoDPlotFactory = null;

                try {
                    if (tmmLabels != null)
                        twoDPlotFactory = new TwoDPlotFactory(summaryFileHandler, tmmLabels);
                    else
                        twoDPlotFactory = new TwoDPlotFactory(summaryFileHandler);

                    // finished with twoDplots (without svm)
                    JFreeChart twoDChart = null;
                    twoDChart = twoDPlotFactory.create2DPlot();
                    twoDPlotFactory.setDrawPointLabels(false);
                    JFreeChart twoDChart_woLabels = twoDPlotFactory.create2DPlot();
                    if (tmmLabels != null) {
                        svm = new SVM(summaryFileHandler, tmmLabels);
                        taskMonitor.setStatusMessage("Running SVM");
                        svm.runSVM();
                        twoDPlotFactory.setALTThreshold(twoDChart, svm.getH());
                        twoDPlotFactory.setTelomeraseThreshold(twoDChart, svm.getV());
                        twoDPlotFactory.setAccuracy(twoDChart, svm.getAccuracy());

                        twoDPlotFactory.setALTThreshold(twoDChart_woLabels, svm.getH());
                        twoDPlotFactory.setTelomeraseThreshold(twoDChart_woLabels, svm.getV());
                        twoDPlotFactory.setAccuracy(twoDChart_woLabels, svm.getAccuracy());
                    }
                    charts.add(twoDChart);
                    charts.add(twoDChart_woLabels);
                } catch (Exception e) {
                    throw new Exception("Could not generate 2D plots: " + e.getMessage());
                }
                taskMonitor.setProgress(0.8);

                ArrayList<Paragraph> firstPage = null;
                try {
                    firstPage = generateFirstPage();
                } catch (Exception e) {
                    throw new Exception("Problem writing text to first page: " + e.getMessage());
                }

                taskMonitor.setStatusMessage("Writing to PDF");

                //finished with SVM based thresholds
                float width = 500;
                float height = 500;
                try {
                    PlotManager.writeChartAsPDF(pdfFile, firstPage, charts, width, height);
                } catch (Exception e) {
                    throw new Exception("Could not write to PSF: " + e.getMessage());
                }

                taskMonitor.setStatusMessage("Report written to " + pdfFile.getAbsolutePath());

            } catch (Exception e) {
                throw new Exception("Exception during report generation: " +
                        (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            } finally {
                System.gc();
                cancel();
            }
        }

        private void writeNumericOutput() throws FileNotFoundException {
            File numericOutputFile = new File(reportDir, "TMM_psf_summary.xls");
            PrintWriter writer = new PrintWriter(numericOutputFile);
            writer.append("Sample\tALT_PSF\tALT_pValue\tTelomerase_PSF\tTelomerase_pValue\n");
            for(String s : summaryFileHandler.getSamples()){
                String line = s + "\t";
                line += summaryFileHandler.getSummaryMap().get(SummaryFileHandler.ALTKEY).
                        get(SummaryFileHandler.SCORESKEY).get(s) + "\t";
                line += summaryFileHandler.getSummaryMap().get(SummaryFileHandler.ALTKEY).
                        get(SummaryFileHandler.PVALUESKEY).get(s) + "\t";

                line += summaryFileHandler.getSummaryMap().get(SummaryFileHandler.TELOMERASEKEY).
                        get(SummaryFileHandler.SCORESKEY).get(s) + "\t";
                line += summaryFileHandler.getSummaryMap().get(SummaryFileHandler.TELOMERASEKEY).
                        get(SummaryFileHandler.PVALUESKEY).get(s) + "\n";
                writer.append(line);
            }
            writer.close();
        }

        private ArrayList<Paragraph> generateFirstPage() throws Exception {
            ArrayList<Paragraph> firstPage = new ArrayList<Paragraph>();
            Paragraph title = new Paragraph();
            title.setFont(new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 14,
                    com.itextpdf.text.Font.BOLD));
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.add("TMM output for: " + iterationTitle);
            firstPage.add(title);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String dateTime = sdf.format(new Date());

            firstPage.add(content("\nDate:      " + dateTime));

            firstPage.add(header("Comments on iteration:"));
            if (comment.equals(""))
                comment = "No comment supplied";
            firstPage.add(content(comment));

            firstPage.add(header("Inputs:"));
            String inputs = "";
            inputs += "Iteration dir:      " + parsedFilesDirectory.getIterationDir().getAbsolutePath() + "\n";
            inputs += "Gene expression matrix:      " + parsedFilesDirectory.getExpMatFile().getAbsolutePath() + "\n";
            firstPage.add(content(inputs));

            firstPage.add(header("Options:"));

            String options = "";
            options += "Number of samples:      "
                    + summaryFileHandler.getSamples().size() + "\n";
            options += "PSF summary file:      " + summaryFile.getAbsolutePath() + "\n";
            options += "Number of bootstrap cycles:      " + bootCycles + "\n";

            if (tmmLabels != null) {
                options += "Mode:      Validation\n";
                options += "TMM labels file:      " + tmmLabelsFile.getAbsolutePath() + "\n";
            } else {
                options += "Mode:      Prediction";
            }
            firstPage.add(content(options));

            if (tmmLabels != null) {
                firstPage.add(header("Validation results"));
                if (boxplotStats == null) {
                    firstPage.add(content("Not enough classes to perform validation"));
                } else {
                    firstPage.add(content("Classification accuracy:      " + DoubleFormatter.formatDouble(svm.getAccuracy())));
                    firstPage.add(header("Median differences and p values"));

                    String tmmkey = BoxPlotFactory.ALTKEY;
                    HashMap<String, Double> thisBoxplotStats = boxplotStats.get(tmmkey);
                    firstPage.add(content("\nALT PSF: overall p value (Kruskal-Wallis rank sum test):      "
                            + DoubleFormatter.formatDouble(
                                    thisBoxplotStats.get(BoxPlotFactory.KWP),3)));

                    firstPage.add(content("ALT versus norm:      Median difference:      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.MD1), 3)
                            + "      p value      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.p1), 3)));
                    firstPage.add(content("ALT versus Tel-ase:      Median difference:      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.MD2),3)
                            + "      p value      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.p2),3)));


                    tmmkey = BoxPlotFactory.TELOMERASEKEY;
                    thisBoxplotStats = boxplotStats.get(tmmkey);
                    firstPage.add(content("\nTel-ase PSF: overall p value (Kruskal-Wallis rank sum test):      "
                            + DoubleFormatter.formatDouble(
                            thisBoxplotStats.get(BoxPlotFactory.KWP),3)));

                    firstPage.add(content("Tel-ase versus norm:      Median difference:      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.MD1), 3)
                            + "      p value      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.p1), 3)));
                    firstPage.add(content("Tel-ase versus ALT:      Median difference:      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.MD2),3)
                            + "      p value      "
                            + DoubleFormatter.formatDouble(thisBoxplotStats.get(BoxPlotFactory.p2),3)));
                }
            }
            return firstPage;
        }

        private Paragraph content(String s) {
            Paragraph text = new Paragraph();
            text.setFont(new Font(Font.FontFamily.TIMES_ROMAN, 10));
            text.setAlignment(Paragraph.ALIGN_LEFT);
            text.add(s);
            return text;
        }

        private Paragraph header(String s) {
            Paragraph header = new Paragraph();
            header.setFont(new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.ITALIC));
            header.add("\n" + s + "\n");
            return header;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    }
}
