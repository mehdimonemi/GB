package ir.rai.Data;

public class Corridor {
    private String origin;
    private String realOrigin;
    private String destination;
    private String realDestination;
    private boolean singleBlock = false;

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

    public Corridor(String origin, String destination, String singleBlock) {
        this.origin = origin;
        this.destination = destination;
        if (singleBlock.equals("یک بلاک"))
            this.singleBlock = true;
        this.realOrigin = origin;
        this.realDestination = destination;
    }

    public boolean isSingleBlock() {
        return singleBlock;
    }

    public void setSingleBlock(boolean singleBlock) {
        this.singleBlock = singleBlock;
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
