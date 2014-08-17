package de.mirkosertic.desktopsearch;

public class Facet {

    private final String name;
    private final int number;
    private final String link;

    public Facet(String name, int number, String link) {
        this.name = name;
        this.number = number;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public String getLink() {
        return link;
    }
}
