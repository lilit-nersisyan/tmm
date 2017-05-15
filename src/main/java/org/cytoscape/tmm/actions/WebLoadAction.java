package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import java.awt.event.ActionEvent;

/**
 * Created by User on 6/10/2015.
 */
public class WebLoadAction extends AbstractCyAction{

    private final String url;

    public WebLoadAction(String url){
        super("TMM: Web load action");
        this.url = url;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        WebLoadActionTask task = new WebLoadActionTask();
        TMMActivator.taskManager.execute(new TaskIterator(task));
    }

    private class WebLoadActionTask extends AbstractTask {

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("TMM: loading the link: " + url);

            taskMonitor.setProgress(0.1);
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            }
            catch (java.io.IOException e) {
                throw new Exception("TMM::Exception " + "Problems loading " + url +
                        "\n" + e.getMessage()+
                        "\n Try opening it manually.");
            } finally {
                taskMonitor.setProgress(1);
                System.gc();
            }
        }
    }
}
