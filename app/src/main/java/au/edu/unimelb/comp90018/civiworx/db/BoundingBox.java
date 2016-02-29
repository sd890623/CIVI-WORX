package au.edu.unimelb.comp90018.civiworx.db;

public class BoundingBox {

    public double latNorthEast, lngNorthEast,
        latSouthWest, lngSouthWest;

    public BoundingBox(double latNE, double lngNE, double latSW, double lngSW) {
        this.latNorthEast = latNE;
        this.lngNorthEast = lngNE;
        this.latSouthWest = latSW;
        this.lngSouthWest = lngSW;
    }

}
