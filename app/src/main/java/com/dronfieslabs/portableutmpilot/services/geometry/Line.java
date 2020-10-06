package com.dronfieslabs.portableutmpilot.services.geometry;

public class Line {

    private Point p1;
    private Point p2;

    private Line(Point p1, Point p2){
        this.p1 = p1;
        this.p2 = p2;
    }

    public static Line newLine(Point p1, Point p2){
        if(p1.equals(p2)){
            throw new RuntimeException("The points have to be different");
        }
        return new Line(Point.newPoint(p1.getX(), p1.getY()), Point.newPoint(p2.getX(), p2.getY()));
    }

    public boolean isVertical(){
        return this.p1.getX() == this.p2.getX();
    }

    public boolean isHorizontal(){
        return this.p1.getY() == this.p2.getY();
    }

    // PRE: line is vertical
    public double getX(){
        if(!isVertical()){
            throw new RuntimeException("Line has to be vertical to call getX()");
        }
        return p1.getX();
    }

    // PRE: line is not vertical
    public double getSlope(){
        if(isVertical()){
            throw new RuntimeException("Line has not to be vertical to call getSlope()");
        }
        return (p2.getY() - p1.getY())/(p2.getX() - p1.getX());
    }

    // PRE: line is not vertical
    public double getYIntercept(){
        if(isVertical()){
            throw new RuntimeException("Line has not to be vertical to call getSlope()");
        }
        return p1.getY() - getSlope()*p1.getX();
    }

    public Point getP1(){
        return this.p1;
    }

    public Point getP2(){
        return this.p2;
    }
}
