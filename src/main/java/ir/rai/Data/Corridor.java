package ir.rai.Data;

public class Corridor {
    private String origin;
    private String Destination;

    public Corridor(String origin, String destination) {
        this.origin = origin;
        this.Destination = destination;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return Destination;
    }

    public void setDestination(String destination) {
        Destination = destination;
    }

    @Override
    public String toString() {
        return origin + " - " + Destination;
    }
}
