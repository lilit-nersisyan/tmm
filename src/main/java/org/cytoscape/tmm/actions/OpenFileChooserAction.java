package org.cytoscape.tmm.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.tmm.TMMActivator;

import javax.swing.*;
import javax.swing.plaf.FileChooserUI;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;

/**
 * Created by Lilit Nersisyan on 3/20/2017.
 */
public class OpenFileChooserAction extends AbstractCyAction{
    private File selectedFile = null;
    private boolean selectDirectory;

    public OpenFileChooserAction(String name, boolean selectDirectory) {
        super(name);
        this.selectDirectory = selectDirectory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame fileLoadFrame = new JFrame(name);
        fileLoadFrame.setLocation(400, 250);
        fileLoadFrame.setSize(400, 200);
        JFileChooser fileChooser = new JFileChooser();
        if(selectDirectory)
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File recentDirectory = TMMActivator.getRecentDirectory();
        fileChooser.setCurrentDirectory(recentDirectory);

        fileChooser.setDialogTitle("TMM File chooser");
        fileChooser.showOpenDialog(fileLoadFrame);
        String selectedFilePath = null;

        if (fileChooser.getSelectedFile() != null) {
            selectedFilePath = fileChooser.getSelectedFile().getPath();
            TMMActivator.writeRecentDirectory(selectedFilePath);
        }
        selectedFile = fileChooser.getSelectedFile();
    }

    public File getSelectedFile() {
        return selectedFile;
    }
}
