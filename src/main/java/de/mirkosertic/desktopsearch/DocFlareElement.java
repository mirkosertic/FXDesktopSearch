package de.mirkosertic.desktopsearch;

import java.util.ArrayList;
import java.util.List;

public class DocFlareElement {

    private String name;
    private Integer size;
    private List<DocFlareElement> children;

    public DocFlareElement(String aName, Integer aSize) {
        name = aName;
        size = aSize;
        children = new ArrayList<>();
    }

    public DocFlareElement(String aName) {
        this(aName, null);
    }

    public String getName() {
        return name;
    }

    public Integer getSize() {
        return size;
    }

    public void incrementWeight() {
        if (size == null) {
            size = 1;
        } else {
            size = size + 1;
        }
    }

    public List<DocFlareElement> getChildren() {
        return children;
    }
}