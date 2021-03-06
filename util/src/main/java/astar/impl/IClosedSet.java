package astar.impl;

public interface IClosedSet<X> {

    boolean contains(X node);

    void add(X node);

    X min();

}
