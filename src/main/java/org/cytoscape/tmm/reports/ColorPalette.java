package org.cytoscape.tmm.reports;

import java.util.Arrays;

/**
 * This class stores and provides a set of distinct colors
 */
public class ColorPalette {
    public static final Integer SET1 = 1;
    public static final Integer SET2 = 2;

    private static String[] set1 = new String[]{
            "#0057E7",
            "#D62D20",
            "#FFA700",
            "#008744",
            "#5C94EF",
            "#E47971",
            "#FFC75C",
            "#5CB288",
            "#003893",
            "#891D15",
            "#A36B00",
            "#00562C",
            "#989898",
            "#A2C1F6",
            "#F0B2AD",
            "#FFDFA2",
            "#A2D3BB",
            "#D8E2DC",
            "#FFBEA3",
            "#E8767F",
            "#E36397",
            "#75DBCD",
            "#002869",
            "#62150F",
            "#744C00",
            "#003E1F",
            "#6D6D6D",
            "#FFDBCC",
            "#F2B4B9",
            "#EFA9C6",
            "#B3EBE3",
            "#A37968",
            "#944C51",
            "#914061",
            "#4B8C83"
    };

    private static String[] set2 = new String[]{
            "#3cb44b",
            "#e6194b",
            "#ffe119",
            "#0082c8",
            "#f58231",
            "#911eb4",
            "#46f0f0",
            "#f032e6",
            "#d2f53c",
            "#fabebe",
            "#008080",
            "#e6beff",
            "#aa6e28",
            "#fffac8",
            "#800000",
            "#aaffc3",
            "#808000",
            "#ffd8b1",
            "#000080",
            "#808080",
            "#FFFFFF",
            "#000000"
    };

    // set2 + set 1
    private static String[] set3 = new String[]{
            "#3cb44b",
            "#e6194b",
            "#ffe119",
            "#0082c8",
            "#f58231",
            "#911eb4",
            "#46f0f0",
            "#f032e6",
            "#d2f53c",
            "#fabebe",
            "#008080",
            "#e6beff",
            "#aa6e28",
            "#fffac8",
            "#800000",
            "#aaffc3",
            "#808000",
            "#ffd8b1",
            "#000080",
            "#808080",
            "#FFFFFF",
            "#000000",
            "#0057E7",
            "#D62D20",
            "#FFA700",
            "#008744",
            "#5C94EF",
            "#E47971",
            "#FFC75C",
            "#5CB288",
            "#003893",
            "#891D15",
            "#A36B00",
            "#00562C",
            "#989898",
            "#A2C1F6",
            "#F0B2AD",
            "#FFDFA2",
            "#A2D3BB",
            "#D8E2DC",
            "#FFBEA3",
            "#E8767F",
            "#E36397",
            "#75DBCD",
            "#002869",
            "#62150F",
            "#744C00",
            "#003E1F",
            "#6D6D6D",
            "#FFDBCC",
            "#F2B4B9",
            "#EFA9C6",
            "#B3EBE3",
            "#A37968",
            "#944C51",
            "#914061",
            "#4B8C83"
    };


    public static String[] getColorPalette(int n, int set) {
        switch (set) {
            case 1: {
                if (n > set1.length)
                    if (n + 1 <= set3.length)
                        return Arrays.copyOfRange(set3, 1, n + 1);
                    else
                        return null;
                else
                    return Arrays.copyOfRange(set1, 1, n + 1);
            }
            case 2:{
                if (n > set2.length)
                    if (n + 1 <= set3.length)
                        return Arrays.copyOfRange(set3, 1, n + 1);
                    else
                        return null;
                else
                    return Arrays.copyOfRange(set2, 1, n + 1);
            }
            default:{
                if (n > set2.length)
                    if (n + 1 <= set3.length)
                        return Arrays.copyOfRange(set3, 1, n + 1);
                    else
                        return null;
                else
                    return Arrays.copyOfRange(set2, 1, n + 1);
            }
        }
    }

    public static String[] getColorPalette(int n) {
        return Arrays.copyOfRange(set1, 1, n + 1);
    }
}
