package org.cytoscape.tmm.reports;
import jsc.datastructures.GroupedData;
import jsc.independentsamples.KruskalWallisTest;

/**
 * Created by Lilit Nersisyan on 4/26/2017.
 */
public class Stats {

    public static Double rankSum(double[] data, String[] labels){
        GroupedData groupedData = new GroupedData(data, labels);
        KruskalWallisTest kwt = new KruskalWallisTest(groupedData);
        return kwt.getSP();
    }

    public static void main(String[] args) {
        double[] data = new double[]{3.2, 3.3, 3.5, 2.5, 2.7, 1.0, 0.8, 1.1, 1.2, 1.3};
        String[] labels = new String[]{"g1", "g1", "g1", "g1", "g1", "g2", "g2", "g2","g2", "g2"};
        GroupedData groupedData = new GroupedData(data, labels);
        KruskalWallisTest kwt = new KruskalWallisTest(groupedData);
        System.out.println(kwt.getSP());

    }

}
