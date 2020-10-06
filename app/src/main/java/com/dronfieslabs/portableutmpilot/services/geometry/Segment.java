package com.dronfieslabs.portableutmpilot.services.geometry;

public class Segment {

    private Point p1;
    private Point p2;

    private Segment(Point p1, Point p2){
        this.p1 = p1;
        this.p2 = p2;
    }

    public static Segment newSegment(Point p1, Point p2){
        return new Segment(Point.newPoint(p1.getX(), p1.getY()), Point.newPoint(p2.getX(), p2.getY()));
    }

    public Point getP1(){
        return this.p1;
    }

    public Point getP2(){
        return this.p2;
    }

    public double getLength(){
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

}
