package ir.rai;

public class GTip {
//    public static int maxH = 580;
//    public static int maxW = 500;
    private String Name;

    private float[][] freeSpace;
    private float[][] allowedSpace;


//    public int getMaxH() {
//        return maxH;
//    }
//
//    public void setMaxH(int maxH) {
//        this.maxH = maxH;
//    }

//    public int getMaxW() {
//        return maxW;
//    }
//
//    public void setMaxW(int maxW) {
//        this.maxW = maxW;
//    }

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

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}
