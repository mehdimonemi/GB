package ir.rai.Data;

public class Result {
    String section = "";
    String gabariKind = "";
    String allowedSpace = "";
    String inFree = "";
    String freeSpace = "";
    String inStructure = "";
    String structureSpace = "";

    public Result(String section, String gabariKind, String allowedSpace,
                  String inFree, String freeSpace, String inStructure, String structureSpace) {
        this.section = section;
        this.gabariKind = gabariKind;
        this.allowedSpace = allowedSpace;
        this.inFree = inFree;
        this.freeSpace = freeSpace;
        this.inStructure = inStructure;
        this.structureSpace = structureSpace;
    }

    public Result(String section, String gabariKind, String allowedSpace) {
        this.section = section;
        this.gabariKind = gabariKind;
        this.allowedSpace = allowedSpace;
    }

    public Result(String section, String gabariKind, String allowedSpace, String inFree, String freeSpace) {
        this.section = section;
        this.gabariKind = gabariKind;
        this.allowedSpace = allowedSpace;
        this.inFree = inFree;
        this.freeSpace = freeSpace;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getGabariKind() {
        return gabariKind;
    }

    public void setGabariKind(String gabariKind) {
        gabariKind = gabariKind;
    }

    public String getAllowedSpace() {
        return allowedSpace;
    }

    public void setAllowedSpace(String allowedSpace) {
        this.allowedSpace = allowedSpace;
    }

    public String getInFree() {
        return inFree;
    }

    public void setInFree(String inFree) {
        this.inFree = inFree;
    }

    public String getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(String freeSpace) {
        this.freeSpace = freeSpace;
    }

    public String getInStructure() {
        return inStructure;
    }

    public void setInStructure(String inStructure) {
        this.inStructure = inStructure;
    }

    public String getStructureSpace() {
        return structureSpace;
    }

    public void setStructureSpace(String structureSpace) {
        this.structureSpace = structureSpace;
    }
}
