package gckfilestructure;

import java.io.File;

/**
 * Data construct holding all the information extracted from a GCK file.
 * Includes setter and getter methods to the information.
 *
 * TODO: Find a way to determine circularity.
 *
 * @author Matyas Medzihradszy
 * @version 0.1
 */
public class GCKFile {
    public GCKFile() {
        super();
    }
    
    public GCKFile(File file, FileType type) {
        super();
        this.file = file;
        this.type = type;
        sequence = "";
    }

    public File getFile() {
        return file;
    }

    public String getSequence() {
        return sequence;
    }

    public int getLengthRegions() {
        return lengthRegions;
    }

    public void setLengthRegions(int lengthRegions) {
        this.lengthRegions = lengthRegions;
    }

    public short getNumRegions() {
        return numRegions;
    }

    public void setNumRegions(short numRegions) {
        this.numRegions = numRegions;
    }

    public int getLengthFeatures() {
        return lengthFeatures;
    }

    public void setLengthFeatures(int lengthFeatures) {
        this.lengthFeatures = lengthFeatures;
    }

    public short getNumFeatures() {
        return numFeatures;
    }

    public void setNumFeatures(short numFeatures) {
        this.numFeatures = numFeatures;
    }

    public boolean isCircular() {
        return isCircular;
    }

    public void setCircular(boolean isCircular) {
        this.isCircular = isCircular;
    }

    public int getLengthSites() {
        return lengthSites;
    }

    public void setLengthSites(int lengthSites) {
        this.lengthSites = lengthSites;
    }

    public short getNumSites() {
        return numSites;
    }

    public void setNumSites(short numSites) {
        this.numSites = numSites;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public int getSequenceLength() {
        return sequenceLength;
    }

    public void setSequenceLength(int sequenceLength) {
        this.sequenceLength = sequenceLength;
    }

    public void allocateRegions() {
        regions = new Region[numRegions];
        for (int i = 0; i < regions.length; i++) {
            regions[i] = new Region();
        }
    }
    
    public Region getRegion(int i) {
        return regions[i];
    }
    
    public void allocateFeatures() {
        features = new Feature[numFeatures];
        for (int i = 0; i < features.length; i++) {
            features[i] = new Feature();
        }
    }
    
    public Feature getFeature(int i) {
        return features[i];
    }

    public Feature[] getFeatures() {
        return features;
    }

    public String getConstructName() {
        return constructName;
    }

    public void setConstructName(String constructName) {
        this.constructName = constructName;
    }

    public void setFileType(FileType type) {
        this.type = type;
    }

    public FileType getFileType() {
        return type;
    }


    //Fields
    public static enum FileType {
        GCC, GCS;
    }

    private File file; //handle to the physical file
    private int lengthRegions; //total length of region definitions in bytes
    private short numRegions; //number of defined regions found
    private int lengthFeatures; //total length of feature definitions in bytes
    private short numFeatures; //number of defined features found
    private int lengthSites;
    private short numSites;
    private Region[] regions;
    private Feature[] features;
    private boolean isCircular;
    private String constructName;
    private String sequence;
    private int sequenceLength;
    private FileType type;

    /***** CONSTANTS *****/
    public final static int REGION_DEF_LENGTH = 0x2C; //length of a single region definition
    public final static int FEATURE_DEF_LENGTH = 0x5C; //length of a single feature definition
    public final static int HEADER_LENGTH = 0x20; //Header size in bytes
    public final static int GENERATION_DEF_LENGTH = 260;
    public final static int CONSTRUCT_NAME_OFFSET = 706;
}
