package org.cytoscape.tmm.Enums;

/**
 * Created by Lilit Nersisyan on 3/20/2017.
 */
public enum ETMMProps {
    GENEID("GeneID"),
    EXPMATFILE("ExpMatFile"),
    PARENTDIR("ParentDir"),
    ITERATION("Iteration"),
    COMMENT("Comment"),
    TMMLABELSFILE("TMMLabelsFile");

    private String name;
    private boolean oldValue = true;
    private boolean newValue = true;

    private ETMMProps(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isOldValue() {
        return oldValue;
    }

    public void setOldValue(boolean oldValue) {
        this.oldValue = oldValue;
    }

    public boolean isNewValue() {
        return newValue;
    }

    public void setNewValue(boolean newValue) {
        this.newValue = newValue;
    }
}
