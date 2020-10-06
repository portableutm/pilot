package com.dronfieslabs.portableutmpilot.services.geometry;

import java.util.Objects;

public class Point implements Comparable<Point>{

    private double x;
    private double y;

    private Point(double x, double y){
        this.x = x;
        this.y = y;
    }

    public static Point newPoint(double x, double y){
        return new Point(x, y);
    }

    public double getX(){
        return this.x;
    }

    public double getY(){
        return this.y;
    }

    public static Orientation orientation(Point p1, Point p2, Point p3){
        double val = (p2.getY() - p1.getY()) * (p3.getX() - p2.getX()) - (p2.getX() - p1.getX()) * (p3.getY() - p2.getY());
        if (val == 0) return Orientation.COLINEAR;
        return (val > 0) ? Orientation.CLOCKWISE: Orientation.COUNTERCLOCKWISE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 &&
                Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString(){
        return "("+x+","+y+")";
    }

    @Override
    public int compareTo(Point o) {
        double ret = getX() - o.getX();
        if(ret == 0){
            ret = getY() - o.getY();
        }
        return (int)Math.signum(ret);
    }
}
