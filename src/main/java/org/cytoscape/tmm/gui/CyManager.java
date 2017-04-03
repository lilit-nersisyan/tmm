package org.cytoscape.tmm.gui;

import org.cytoscape.model.*;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.view.model.CyNetworkView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    /**
     * Populate the CyTable with attributes of given attribute name from given
     * <code>CyNode</code> : attribute map.
     * <p>
     * If an attribute column with such name
     * does not exist in the <code>CyTable</code>, it will be created. If it exists and its type
     * does not match the attribute type given, an Exception will be returned.
     * If the key type of the map is not <code>CyNode</code>, an Exception is returned.
     * Otherwise, rows for each of the <code>CyNode</code>in the map will be populated with
     * values from the map. If a CyNode does not exist in the given CyNetwork this node will be skipped.
     * </p>
     *
     * @param cyNetwork          CyNetwork containing the CyNodes to be mapped.
     * @param cyNodeAttributeMap Map containing the CyNodes and their attribute values.
     * @param attrName           the name of the attribute column
     * @param attrType           the type of the attribute
     */
    public static void setNodeAttributesFromMap(CyNetwork cyNetwork,
                                                Map cyNodeAttributeMap,
                                                String attrName, Class<?> attrType) throws Exception {
        if (cyNodeAttributeMap.isEmpty())
            throw new Exception("Could not populate the nodeTable : the nodeAttributeMap was empty");
        CyTable nodeTable = cyNetwork.getDefaultNodeTable();

        getOrCreateAttributeColumn(nodeTable, attrName, attrType);

        for (Object obj : cyNodeAttributeMap.keySet()) {
            if (obj instanceof CyNode)
                break;
            else
                throw new Exception("The key of the map is not of type CyNode");
        }
        CyNode cyNode;

        if (attrType.equals(Double.class) || attrType.equals(double.class)) {
            for (Object obj : cyNodeAttributeMap.keySet()) {
                cyNode = (CyNode) obj;
                double value = (Double) cyNodeAttributeMap.get(cyNode);
                value = DoubleFormatter.formatDouble(value);
                CyRow row = nodeTable.getRow(cyNode.getSUID());
                row.set(attrName, value);
            }

        } else {
            for (Object obj : cyNodeAttributeMap.keySet()) {
                cyNode = (CyNode) obj;
                CyRow row = nodeTable.getRow(cyNode.getSUID());
                row.set(attrName, cyNodeAttributeMap.get(cyNode));
            }
        }
    }

    /**
     * Returns a CyColumn from the given CyTable with the given name.
     * If such a column does not exist, it is created.
     * If a CyColumn with given name exists, but does not match the attribute type given,
     * and exception is thrown.
     *
     * @param table    CyTable where the CyColumn should be
     * @param attrName name of the attribute column
     * @param attrType type of the attribute
     * @return CyColumn
     * @throws Exception thrown if the attribute type does not match the existing type in the existing CyColumn.
     */
    public static CyColumn getOrCreateAttributeColumn(CyTable table,
                                                      String attrName, Class attrType) throws Exception {
        Iterator<CyColumn> iterator = table.getColumns().iterator();

        while (iterator.hasNext()) {
            CyColumn column = iterator.next();
            if (attrName.equals(column.getName()))
                if (column.getType().equals(attrType))
                    return column;
                else {
                    throw new Exception("The argument type conflicts with the type of column: "
                            + column.getName());
                }
        }
        table.createColumn(attrName, attrType, false);
        return table.getColumn(attrName);
    }


    public static CyNode getCyNodeFromName(String name, CyNetwork network) {
        CyTable nodeTable = network.getDefaultNodeTable();
        for (CyNode cyNode : network.getNodeList()) {
            if (nodeTable.getRow(cyNode.getSUID()).get("name", String.class).equals(name))
                return cyNode;
        }
        return null;
    }
}
