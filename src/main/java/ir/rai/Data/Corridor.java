package ir.rai.Data;

public class Corridor {
    private String origin;
    private String realOrigin;
    private String destination;
    private String realDestination;

    public Corridor(String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
        this.realOrigin = origin;
        this.realDestination = destination;
    }

    public Corridor(String origin, String realOrigin, String destination, String realDestination) {
        this.origin = origin;
        this.realOrigin = realOrigin;
        this.destination = destination;
        this.realDestination = realDestination;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getRealOrigin() {
        return realOrigin;
    }

    public void setRealOrigin(String realOrigin) {
        this.realOrigin = realOrigin;
    }

    public String getRealDestination() {
        return realDestination;
    }

    public void setRealDestination(String realDestination) {
        this.realDestination = realDestination;
    }

    @Override
    public String toString() {
        return origin + " - " + destination;
    }
}
