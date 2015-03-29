package gckfilestructure;

/**
 * A Feature is a special Region. It has a name, a direction, and a type. Otherwise it is the same as a Region.
 *
 * @author Matyas Medzihradszky
 * @version 0.1
 */
public class Feature extends Region {

    /**
     * Constructor
     */
    public Feature() {
        this(0,0,(byte)0, (byte)0, (byte)0, true, "", "", Strand.FORWARD, RegionType.GENE);
    }

    /**
     * Constructor
     *
     * @param r
     */
    public Feature(Region r) {
        this(r, "", "", Strand.FORWARD, RegionType.GENE);
    }

    /**
     * Constructor
     *
     * @param r
     * @param name
     * @param comment
     * @param strand
     * @param type
     */
    public Feature(Region r, String name, String comment, Strand strand, RegionType type) {
        this(r.getStart(), r.getEnd(), r.getColourRed(), r.getColourGreen(), r.getColourBlue(), r.isToDisplay(), name, comment, strand, type);
    }

    /**
     * Constructor
     *
     * @param start
     * @param end
     * @param red
     * @param green
     * @param blue
     * @param toDisplay
     * @param name
     * @param comment
     * @param strand
     * @param type
     */
    public Feature(int start, int end, byte red, byte green, byte blue, boolean toDisplay, String name, String comment, Strand strand, RegionType type) {
        super();
        setStart(start);
        setEnd(end);
        setColourRed(red);
        setColourGreen(green);
        setColourBlue(blue);
        setToDisplay(toDisplay);
        setName(name);
        setComment(comment);
        setStrand(strand);
        setType(type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Strand getStrand() {
        return strand;
    }

    public void setStrand(Strand strand) {
        this.strand = strand;
    }
    
    public void setStrand(byte btStrand) {
        switch (btStrand) {
            case 0: this.strand = Strand.NONE; break;
            case 1: this.strand = Strand.REVERSE; break;
            case 2: this.strand = Strand.FORWARD; break;
            case 3: this.strand = Strand.BOTH; break;
            default: this.strand = Strand.FORWARD;
        }
    }

    public Region.RegionType getType() {
        return type;
    }

    public void setType(Region.RegionType type) {
        this.type = type;
    }
    
    public void setType(short btType) {
        if( btType > 0 ) {
            this.type = Region.RegionType.CDS;
        } else {
            this.type = Region.RegionType.GENE;
        }
    }


    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }
    
    public enum Strand {
        FORWARD ((byte)2),
        REVERSE ((byte)1),
        NONE ((byte)0),
        BOTH ((byte)3);
        
        private final byte direction;
        Strand(byte direction) {
            this.direction = direction;
        }
    }
    
    private String name;
    private Strand strand;
    private RegionType type;
    private String comment;
    private boolean automatic = false;
}