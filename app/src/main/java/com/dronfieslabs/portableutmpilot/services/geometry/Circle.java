package com.dronfieslabs.portableutmpilot.services.geometry;

import java.util.ArrayList;
import java.util.List;

public class Circle {

    private Point center;
    private double radius;

    private Circle(Point center, double radius){
        this.center = center;
        this.radius = radius;
    }

    public static Circle newCircle(Point center, double radius){
        return new Circle(Point.newPoint(center.getX(), center.getY()), radius);
    }

    public double getRadius(){
        return radius;
    }

    public Point getCenter(){
        return Point.newPoint(center.getX(), center.getY());
    }

    public List<Point> intersection(Line line){
        // circle equation (x - h)(x - h) + (y - k)(y - k) = r.r
        // r = radius, center (h, k)
        double h = center.getX();
        double k = center.getY();
        double r = radius;
        if(line.isVertical()){
            // line equation is x = n
            // substituting -> y^2 - 2ky + n^2 - 2nh + h^2 + k^2 - r^2 = 0;
            double n = line.getX();
            double coefA = 1;
            double coefB = -2*k;
            double coefC = n*n -2*n*h + h*h + k*k - r*r;
            double discriminant = coefB*coefB - 4*coefA*coefC;
            if(discriminant < 0){
                return new ArrayList<>();
            }else if(discriminant == 0){
                List<Point> ret = new ArrayList<>();
                ret.add(Point.newPoint(n, -coefB/(2*coefA)));
                return ret;
            }else{
                List<Point> ret = new ArrayList<>();
                ret.add(Point.newPoint(n, (-coefB+Math.sqrt(discriminant))/(2*coefA)));
                ret.add(Point.newPoint(n, (-coefB-Math.sqrt(discriminant))/(2*coefA)));
                return ret;
            }
        }else{
            // line equation y = ax + b
            double a = line.getSlope();
            double b = line.getYIntercept();
            double coefA = 1 + a*a;
            double coefB = -2*h + 2*a*b - 2*k*a;
            double coefC = h*h + b*b - 2*k*b + k*k - r*r;
            double discriminant = coefB*coefB - 4*coefA*coefC;
            if(discriminant < 0){
                return new ArrayList<>();
            }else if(discriminant == 0){
                List<Point> ret = new ArrayList<>();
                double x = -coefB/(2*coefA);
                ret.add(Point.newPoint(x, a*x + b));
                return ret;
            }else{
                List<Point> ret = new ArrayList<>();
                double x1 = (-coefB + Math.sqrt(discriminant))/(2*coefA);
                double x2 = (-coefB - Math.sqrt(discriminant))/(2*coefA);
                ret.add(Point.newPoint(x1, a*x1 + b));
                ret.add(Point.newPoint(x2, a*x2 + b));
                return ret;
            }
        }
    }
}
