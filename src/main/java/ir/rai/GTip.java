package ir.rai;

public class GTip {
    private int maxH;
    private int maxW;

    private float[][] freeSpace;
    private float[][] allowedSpace;


    public int getMaxH() {
        return maxH;
    }

    public void setMaxH(int maxH) {
        this.maxH = maxH;
    }

    public int getMaxW() {
        return maxW;
    }

    public void setMaxW(int maxW) {
        this.maxW = maxW;
    }

    public float[][] getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(float[][] freeSpace) {
        this.freeSpace = freeSpace;
    }

    public float[][] getAllowedSpace() {
        return allowedSpace;
    }

    public void setAllowedSpace(float[][] allowedSpace) {
        this.allowedSpace = allowedSpace;
    }
}
