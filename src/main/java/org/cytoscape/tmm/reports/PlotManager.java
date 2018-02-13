package org.cytoscape.tmm.reports;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.*;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;

import java.awt.*;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Lilit Nersisyan on 4/7/2017.
 */
public class PlotManager {
    public static void writeChartAsPDF(File out, ArrayList<Paragraph> firstPage,
                                       ArrayList<JFreeChart> charts,
                                       float width,
                                       float height) throws IOException {
        Rectangle pagesize = new Rectangle(PageSize.A4);
        Document document = new Document(pagesize, 50, 50, 50, 50);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(out));
            document.addAuthor("JFreeChart");
            document.addSubject("Demonstration");
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(PageSize.A4.getWidth(), PageSize.A4.getHeight());
            double y = 0;

           document.setPageSize(new Rectangle(0,0,width, height));
            document.newPage();
            for(Paragraph paragraph : firstPage){
                document.add(paragraph);
            }


            for (JFreeChart chart : charts) {
                document.setPageSize(new Rectangle(0, 0, width, height));
                document.newPage();
                Graphics2D g2 = new PdfGraphics2D(cb, width, height);
                double margin = 20;
                Rectangle2D r2D = new Rectangle2D.Double(margin, margin, width - 2 * margin, height - 2 * margin);
                chart.draw(g2, r2D);
                g2.dispose();
            }

        } catch (DocumentException de) {
            System.err.println(de.getMessage());
        }
        document.close();
    }

    public static void renderBase(XYPlot plot) {
        plot.getRenderer().setSeriesShape(0, new Ellipse2D.Double(0, 0, 10, 10));
        plot.getRenderer().setSeriesPaint(0, new Color(0, 151, 172));
        plot.setBackgroundPaint(Color.white);
        plot.getRenderer().setBaseItemLabelsVisible(true);
        plot.getRenderer().setBaseItemLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));

    }

    public static void setBaseItemLabels(XYPlot plot, ArrayList<String> samples) {
        MyLabelGenerator labelGenerator = new MyLabelGenerator(samples);
        plot.getRenderer().setBaseItemLabelGenerator(labelGenerator);
    }

    public static void setSeriesItemLabels(XYPlot plot,
                                           HashMap<String, ArrayList<String>> seriesLabels,
                                           TMMLabels tmmLabels) {
        int n = plot.getSeriesCount();
        for (int s = 0; s < n; s++) {
            String seriesKey = (String) plot.getDataset().getSeriesKey(s);
            MyLabelGenerator labelGenerator = new MyLabelGenerator(seriesLabels.get(seriesKey));
            plot.getRenderer().setSeriesItemLabelGenerator(s, labelGenerator);
            plot.getRenderer().setSeriesPaint(s, tmmLabels.getLabelColor(seriesKey));
            plot.getRenderer().setSeriesShape(s, new Ellipse2D.Double(0, 0, 10, 10));
        }
    }

    public static void setSeriesItemLabels(XYPlot plot,
                                           HashMap<String, ArrayList<String>> seriesLabels,
                                           GroupLabels groupLabels) {
        int n = plot.getSeriesCount();
        for (int s = 0; s < n; s++) {
            String seriesKey = (String) plot.getDataset().getSeriesKey(s);
            MyLabelGenerator labelGenerator = new MyLabelGenerator(seriesLabels.get(seriesKey));
            plot.getRenderer().setSeriesItemLabelGenerator(s, labelGenerator);
            plot.getRenderer().setSeriesPaint(s, groupLabels.getGroupColorsMap().get(seriesKey));
            plot.getRenderer().setSeriesShape(s, new Ellipse2D.Double(0, 0, 10, 10));
        }
    }

    public static void setSeriesItemLabels(XYPlot plot,
                                           HashMap<String, ArrayList<String>> groupSamplesMap,
                                           HashMap<String, Color> groupColorsMap) {
        int n = plot.getSeriesCount();
        for (int s = 0; s < n; s++) {
            String seriesKey = (String) plot.getDataset().getSeriesKey(s);
            MyLabelGenerator labelGenerator = new MyLabelGenerator(groupSamplesMap.get(seriesKey));
            plot.getRenderer().setSeriesItemLabelGenerator(s, labelGenerator);
            plot.getRenderer().setSeriesPaint(s, groupColorsMap.get(seriesKey));
            plot.getRenderer().setSeriesShape(s, new Ellipse2D.Double(0, 0, 10, 10));
        }
    }



    /**
     * Draws a gray line at the specified position on the y axis (if horizontal is true)
     * or the x axis (if horizontal is false)
     *
     * @param plot
     * @param horizontal a logical indicating if the line should be horizontal or vertical
     * @param position the axis value at which to draw the line
     */
    public static void drawLine(XYPlot plot, boolean horizontal, double position) {
        drawLine(plot, horizontal, position, Color.gray);
    }

    /**
     * Draws a line at the specified position on the y axis (if horizontal is true)
     * or the x axis (if horizontal is false) with the specified color
     *
     * @param plot
     * @param horizontal a logical indicating if the line should be horizontal or vertical
     * @param position the axis value at which to draw the line
     */
    public static void drawLine(XYPlot plot, boolean horizontal, double position, Color color) {
        ValueMarker marker = new ValueMarker(position);  // position is the value on the axis
        marker.setPaint(color);
        marker.setStroke(new BasicStroke(.5f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, new float[]{10.0f}, 0.0f));
        if(horizontal)
            plot.addRangeMarker(marker);
        else
            plot.addDomainMarker(marker);
    }

    /**
     * Draws a line at the specified position on the y axis (if horizontal is true)
     * or the x axis (if horizontal is false) with the specified color
     *
     * @param plot
     * @param horizontal a logical indicating if the line should be horizontal or vertical
     * @param position the axis value at which to draw the line
     */
    public static void drawLine(XYPlot plot, boolean horizontal, double position, Color color, String label) {
        ValueMarker marker = new ValueMarker(position);  // position is the value on the axis
        marker.setPaint(color);
        marker.setStroke(new BasicStroke(.5f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, new float[]{10.0f}, 0.0f));

        marker.setLabel(label);
        marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        marker.setLabelOffset(new RectangleInsets(8, 10, 0, 50));
        marker.setLabelFont(new Font(Font.SANS_SERIF, Font.ITALIC, 8));

        if(horizontal)
            plot.addRangeMarker(marker);
        else
            plot.addDomainMarker(marker);

    }

    static class MyLabelGenerator implements XYItemLabelGenerator {
        private ArrayList<String> seriesLabels;

        public MyLabelGenerator(ArrayList<String> seriesLabels) {
            this.seriesLabels = seriesLabels;
        }

        @Override
        public String generateLabel(XYDataset dataset, int series, int item) {
            return seriesLabels.get(item);
        }

    }

}
