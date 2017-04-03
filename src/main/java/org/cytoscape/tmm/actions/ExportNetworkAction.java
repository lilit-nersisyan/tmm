package org.cytoscape.tmm.actions;

/**
 * Created by Lilit Nersisyan on 4/1/2017.
 */

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.tmm.gui.CyManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;

public class ExportNetworkAction extends AbstractCyAction {


    private final String fileName;
    private final CyNetwork network;

    public ExportNetworkAction(CyNetwork network, String fileName) {
        super("TMM: Exporting network to file " + fileName);
        this.fileName = fileName;
        this.network = network;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TaskIterator taskIterator = TMMActivator.exportNetworkViewTaskFactory.createTaskIterator(
                CyManager.getNetworkView(network),
                new File(fileName));

        TMMActivator.taskManager.execute(taskIterator);
    }
}



