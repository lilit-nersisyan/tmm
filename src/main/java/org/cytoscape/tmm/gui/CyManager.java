package org.cytoscape.tmm.gui;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.view.model.CyNetworkView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

/**
 * Created by Lilit Nersisyan on 4/1/2017.
 */
public class CyManager {
    public static CyNetwork getCurrentNetwork() {
        return TMMActivator.cyApplicationManager.getCurrentNetwork();
    }

    /**
     * Return first of all the views of the given network, or create one if no view for the network exists.
     *
     * @param network the network
     * @return the view of the network
     */
    public static CyNetworkView getNetworkView(CyNetwork network) {
        CyNetworkView networkView;
        Collection<CyNetworkView> networkViews = TMMActivator.networkViewManager.getNetworkViews(network);
        if (networkViews.isEmpty()) {
            networkView = TMMActivator.networkViewFactory.createNetworkView(network);
            TMMActivator.networkViewManager.addNetworkView(networkView);
        } else
            networkView = networkViews.iterator().next();
        return networkView;
    }

    public static void exportNodeNameEntrezTable(CyNetwork selectedNetwork,
                                                 String geneIDName, File file) throws Exception {
        CyTable cyTable = selectedNetwork.getDefaultNodeTable();
        List<CyRow> rows = cyTable.getAllRows();
        try {
            PrintWriter writer = new PrintWriter(file);
            String header = "name,entrez,network";
            writer.append(header + "\n");

            for (CyNode node : selectedNetwork.getNodeList()) {
                String name = cyTable.getRow(node.getSUID()).get("name", String.class);
                String geneID = cyTable.getRow(node.getSUID()).get(geneIDName, String.class);
                String network = cyTable.getRow(node.getSUID()).get("network", String.class);
                writer.append(name + "," + geneID + "," + network + "\n");
            }

            writer.close();
        } catch (FileNotFoundException e) {
            throw new Exception("Problem exporting node table to file. Reason: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }

    }
}
