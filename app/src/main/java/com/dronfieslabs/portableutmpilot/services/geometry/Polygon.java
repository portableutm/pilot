package com.dronfieslabs.portableutmpilot.services.geometry;

import java.util.ArrayList;
import java.util.List;

public class Polygon {

    private List<Point> vertices;

    private Polygon(List<Point> vertices){
        this.vertices = vertices;
    }

    public static Polygon newPolygon(List<Point> vertices){
        if(vertices.size() < 3){
            throw new RuntimeException("Polygon needs at least 3 vertices");
        }
        List<Point> v = new ArrayList<>();
        for(Point vertex : vertices){
            v.add(Point.newPoint(vertex.getX(), vertex.getY()));
        }
        return new Polygon(v);
    }

    public int getVerticesCount(){
        return this.vertices.size();
    }

    public List<Point> getVertices(){
        List<Point> ret = new ArrayList<>();
        for(Point p : vertices){
            ret.add(Point.newPoint(p.getX(), p.getY()));
        }
        return ret;
    }

    public List<Segment> getSides(){
        List<Segment> ret = new ArrayList<>();
        for(int i = 0; i < vertices.size() - 1; i++){
            ret.add(Segment.newSegment(Point.newPoint(vertices.get(i).getX(), vertices.get(i).getY()), Point.newPoint(vertices.get(i+1).getX(), vertices.get(i+1).getY())));
        }
        ret.add(Segment.newSegment(Point.newPoint(vertices.get(vertices.size()-1).getX(), vertices.get(vertices.size()-1).getY()), Point.newPoint(vertices.get(0).getX(), vertices.get(0).getY())));
        return ret;
    }

    public double minX(){
        double min = Double.MAX_VALUE;
        for(Point p : vertices){
            min = Math.min(p.getX(), min);
        }
        return min;
    }

    public double minY(){
        double min = Double.MAX_VALUE;
        for(Point p : vertices){
            min = Math.min(p.getY(), min);
        }
        return min;
    }

    public double maxX(){
        double max = -Double.MAX_VALUE;
        for(Point p : vertices){
            max = Math.max(p.getX(), max);
        }
        return max;
    }

    public double maxY(){
        double max = -Double.MAX_VALUE;
        for(Point p : vertices){
            max = Math.max(p.getY(), max);
        }
        return max;
    }
}
