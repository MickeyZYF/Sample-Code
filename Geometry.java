package ca.ubc.cs.cpsc210.translink.util;


import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Compute relationships between points, lines, and rectangles represented by LatLon objects
 */
public class Geometry {
    /**
     * Return true if the point is inside of, or on the boundary of, the rectangle formed by northWest and southeast
     * @param northWest         the coordinate of the north west corner of the rectangle
     * @param southEast         the coordinate of the south east corner of the rectangle
     * @param point             the point in question
     * @return                  true if the point is on the boundary or inside the rectangle
     */
    public static boolean rectangleContainsPoint(LatLon northWest, LatLon southEast, LatLon point) {
        Double nwY = northWest.getLatitude();
        Double nwX = northWest.getLongitude();
        Double seY = southEast.getLatitude();
        Double seX = southEast.getLongitude();
        Double pY = point.getLatitude();
        Double pX = point.getLongitude();

        if (between(nwX, seX, pX)) {
            if (between(seY, nwY, pY)){
                return true;
            }
        }
        return false;
    }


    /**
     * Return true if the rectangle intersects the line
     * @param northWest         the coordinate of the north west corner of the rectangle
     * @param southEast         the coordinate of the south east corner of the rectangle
     * @param src               one end of the line in question
     * @param dst               the other end of the line in question
     * @return                  true if any point on the line is on the boundary or inside the rectangle
     */
    public static boolean rectangleIntersectsLine(LatLon northWest, LatLon southEast, LatLon src, LatLon dst) {
        Double nwY = northWest.getLatitude();
        Double nwX = northWest.getLongitude();
        Double seY = southEast.getLatitude();
        Double seX = southEast.getLongitude();
        Double srcY = src.getLatitude();
        Double srcX = src.getLongitude();
        Double dstY = dst.getLatitude();
        Double dstX = dst.getLongitude();
        Double slope = (dstY - srcY)/(dstX - srcX);
        Double b = srcY - (slope * srcX);

        LatLon rightEdgeIntersect = new LatLon(slope * seX + b, seX);
        LatLon leftEdgeIntersect = new LatLon(slope * nwX + b, nwX);
        LatLon topEdgeIntersect = new LatLon(nwY, (nwY - b) /slope);
        LatLon bottomEdgeIntersect = new LatLon(seY, (seY - b)/slope);

        Double topOfLine = srcY;
        Double bottomOfLine = dstY;

        if (dstY > srcY){
            topOfLine = dstY;
            bottomOfLine = srcY;
        }

        if (rectangleContainsPoint(northWest, southEast, src)){
            return true;
        }
        if (rectangleContainsPoint(northWest, southEast, dst)){
            return true;
        }

        if (between(seY, nwY, rightEdgeIntersect.getLatitude())){
            if (between(bottomOfLine, topOfLine, rightEdgeIntersect.getLatitude())){
                return true;
            }
        }

        if (between(seY, nwY, leftEdgeIntersect.getLatitude())){
            if (between(bottomOfLine, topOfLine, leftEdgeIntersect.getLatitude())){
                return true;
            }
        }

        if (between(nwX, seX, topEdgeIntersect.getLongitude())){
            if (between(bottomOfLine, topOfLine, topEdgeIntersect.getLatitude())){
                return true;
            }
        }

        if (between(nwX, seX, bottomEdgeIntersect.getLongitude())){
            if (between(bottomOfLine, topOfLine, topEdgeIntersect.getLatitude())){
                return true;
            }
        }



        return false;
    }


    /**
     * A utility method that you might find helpful in implementing the two previous methods
     * Return true if x is >= lwb and <= upb
     * @param lwb      the lower boundary
     * @param upb      the upper boundary
     * @param x         the value in question
     * @return          true if x is >= lwb and <= upb
     */
    private static boolean between(double lwb, double upb, double x) {
        return lwb <= x && x <= upb;
    }
}
