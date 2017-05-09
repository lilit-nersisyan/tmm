package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by User on 6/10/2015.
 */
public class OpenFileAction extends AbstractCyAction {


    private final String fileName;

    public OpenFileAction(String fileName) {
        super("TMM: Open " + fileName + " action");
        this.fileName = fileName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenFileActionTask task = new OpenFileActionTask();
        TMMActivator.taskManager.execute(new TaskIterator(task));

    }

    private class OpenFileActionTask extends AbstractTask{
        File file;

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("TMM: Opening " + fileName);


            try {
                File file = new File(fileName);
                if (file!= null & file.exists()) {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (IOException e1) {
                        try {
                            Desktop.getDesktop().open(file.getParentFile());
                        } catch (IOException e2) {
                            throw  new Exception("TMM:: cannot open the folder: "
                                    + file.getParent());
                        }
                        throw new Exception("TMM:: cannot open the file: "
                                + file + " for editing");
                    }
                } else {
                    throw new Exception("The file " + file + " does not exist. " +
                            "Try generating the report again");
                }
            } catch (Exception e) {
                throw new Exception("TMM::Exception " + "Problems opening " + file.getAbsolutePath() +
                        "\n" + e.getMessage()+
                        "\n Try opening it manually.");
            } finally {
                taskMonitor.setProgress(1);
                System.gc();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            super.cancel();
        }
    }
}
