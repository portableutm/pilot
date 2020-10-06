package com.dronfieslabs.portableutmpilot.services.geometry;

import java.util.ArrayList;
import java.util.List;

public class GeometryUtils {

    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //---------------------------------------- PUBLIC METHODS  ----------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    public static Polygon rotate(Polygon polygonToRotate, Point center, double angleInDegrees){
        List<Point> rotatedVertices = new ArrayList<>();
        for(Point v : polygonToRotate.getVertices()){
            rotatedVertices.add(rotate(v, center, angleInDegrees));
        }
        return Polygon.newPolygon(rotatedVertices);
    }

    public static Point rotate(Point pointToRotate, Point center, double angleInDegrees){
        // source: http://danceswithcode.net/engineeringnotes/rotations_in_2d/rotations_in_2d.html#:~:text=Convert%20back%20to%20Cartesian%20coordinates,%3D%20(1.964%2C%204.598).
        double angleInRadians = Math.toRadians(angleInDegrees);
        double x0 = pointToRotate.getX();
        double y0 = pointToRotate.getY();
        double xc = center.getX();
        double yc = center.getY();
        double sin = Math.sin(angleInRadians);
        double cos = Math.cos(angleInRadians);
        return Point.newPoint(
        (x0 - xc)*cos - (y0 - yc)*sin + xc,
        (x0 - xc)*sin + (y0 - yc)*cos + yc
        );
    }

    public static boolean doIntersect(Segment s1, Segment s2){
        // Find the four orientations needed for general and special cases
        Orientation o1 = Point.orientation(s1.getP1(), s1.getP2(), s2.getP1());
        Orientation o2 = Point.orientation(s1.getP1(), s1.getP2(), s2.getP2());
        Orientation o3 = Point.orientation(s2.getP1(), s2.getP2(), s1.getP1());
        Orientation o4 = Point.orientation(s2.getP1(), s2.getP2(), s1.getP2());

        // General case
        if(o1 != o2 && o3 != o4) return true;

        // Special Cases
        // s1 and s2.getP1() are colinear and s2.getP1() lies on s1
        if(o1 == Orientation.COLINEAR && onSegment(s1, s2.getP1())) return true;

        // s1 and s2.getP2() are colinear and s2.getP2() lies on segment s1
        if(o2 == Orientation.COLINEAR && onSegment(s1, s2.getP2())) return true;

        // s2 and s1.getP1() are colinear and s1.getP1() lies on segment s2
        if(o3 == Orientation.COLINEAR && onSegment(s2, s1.getP1())) return true;

        // s2 and s1.getP2() are colinear and s1.getP2() lies on segment s2
        if(o4 == Orientation.COLINEAR && onSegment(s2, s1.getP2())) return true;

        return false; // Doesn't fall in any of the above cases
    }

    // PRE: doIntersect is true
    // return null if segments are parallel
    public static Point intersect(Segment segment1, Segment segment2){
        // calculate intersection between the lines
        // https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
        double x1 = segment1.getP1().getX();
        double x2 = segment1.getP2().getX();
        double x3 = segment2.getP1().getX();
        double x4 = segment2.getP2().getX();
        double y1 = segment1.getP1().getY();
        double y2 = segment1.getP2().getY();
        double y3 = segment2.getP1().getY();
        double y4 = segment2.getP2().getY();
        double denominator = ((x1-x2)*(y3-y4) - (y1-y2)*(x3-x4));
        if(denominator == 0){
            return null;
        }
        double t = ((x1-x3)*(y3-y4) - (y1-y3)*(x3-x4))/denominator;
        return Point.newPoint(x1 + t*(x2-x1), y1 + t*(y2-y1));
    }

    public static List<Point> intersect(Segment segment, Polygon polygon){
        List<Point> ret = new ArrayList<>();
        for(Segment side : polygon.getSides()){
            if(doIntersect(side, segment)){
                Point intersection = intersect(side, segment);
                if(intersection != null){
                    ret.add(intersection);
                }
            }
        }
        return ret;
    }

    // Returns true if the point p lies inside the polygon
    public static boolean isInside(Polygon polygon, Point p)
    {
        int n = polygon.getVerticesCount();

        Segment halfLineFromPoint = Segment.newSegment(p, Point.newPoint(Double.MAX_VALUE, p.getY()));

        List<Segment> polygonSides = polygon.getSides();

        int intersectionsCount = 0;
        for(Segment side : polygonSides){
            // if point is on the side, return true
            if(Point.orientation(side.getP1(), side.getP2(), p) == Orientation.COLINEAR && onSegment(side, p)){
                return true;
            }
            // side intersect, only if one of the extremes is above and the other is below the halfLineFromPoint
            if(side.getP1().getY() >= p.getY() && side.getP2().getY() < p.getY() ||
                side.getP1().getY() < p.getY() && side.getP2().getY() >= p.getY()){
                if(doIntersect(side, halfLineFromPoint)){
                    intersectionsCount++;
                }
            }
        }
        return intersectionsCount%2 == 1;
    }

    public static double distance(Point point1, Point point2){
        double x1 = point1.getX();
        double x2 = point2.getX();
        double y1 = point1.getY();
        double y2 = point2.getY();
        return Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1));
    }

    public static double distance(Point point, Line line){
        double x0 = point.getX();
        double y0 = point.getY();
        double x1 = line.getP1().getX();
        double y1 = line.getP1().getY();
        double x2 = line.getP2().getX();
        double y2 = line.getP2().getY();
        return Math.abs( (y2 - y1)*x0 - (x2 - x1)*y0 + x2*y1 - y2*x1) / Math.sqrt((y2 - y1)*(y2 - y1) + (x2 - x1)*(x2 - x1));
    }

    public static double distance(Point point, Segment segment) {
        double x = point.getX();
        double y = point.getY();
        double x1 = segment.getP1().getX();
        double y1 = segment.getP1().getY();
        double x2 = segment.getP2().getX();
        double y2 = segment.getP2().getY();

        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = -1;
        if (len_sq != 0) //in case of 0 length line
            param = dot / len_sq;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        }
        else if (param > 1) {
            xx = x2;
            yy = y2;
        }
        else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = x - xx;
        double dy = y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }


    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------
    //---------------------------------------- PRIVATE METHODS ----------------------------------------
    //-------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------

    // This method assume the point and the segment are on the same line
    private static boolean onSegment(Segment s, Point p){
        // special case: vertical segment
        if(s.getP1().getX() == s.getP2().getX()){
            double minY = Math.min(s.getP1().getY(), s.getP2().getY());
            double maxY = Math.max(s.getP1().getY(), s.getP2().getY());
            return p.getY() >= minY && p.getY() <= maxY;
        }
        // most common case: not vertical segment
        double minX = Math.min(s.getP1().getX(), s.getP2().getX());
        double maxX = Math.max(s.getP1().getX(), s.getP2().getX());
        return p.getX() >= minX && p.getX() <= maxX;
    }


}
