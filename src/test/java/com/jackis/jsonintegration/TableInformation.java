package com.jackis.jsonintegration;

public class TableInformation {

    private String table;
    private String schema;
    private String tableSpace;
    private boolean hasIndexes;


    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTableSpace() {
        return tableSpace;
    }

    public void setTableSpace(String tableSpace) {
        this.tableSpace = tableSpace;
    }

    public boolean isHasIndexes() {
        return hasIndexes;
    }

    public void setHasIndexes(boolean hasIndexes) {
        this.hasIndexes = hasIndexes;
    }
}
