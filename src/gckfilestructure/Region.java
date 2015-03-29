package gckfilestructure;

/**
 * Contains information on a DNA region (sequence) marked somehow in a GCK file.
 *+Regions can be anything from a differently coloured sequence region to
 *+binding sites to protein sequences. This base class only contains the start,
 *+end, display font and colour for a region.
 *
 * @author Matyas Medzihradszky
 * @version 0.1
 */
public class Region extends Site {
    public Region() {
        start = 0;
        end = 0;
        colourRed = 0;
        colourGreen = 0;
        colourBlue = 0;
        grouping = 1;
        toDisplay = false;
    }

    public Region(int start, int end, byte red, byte green, byte blue, short grouping, boolean toDisplay) {
        this.start = start;
        this.end = end;
        this.colourRed = red;
        this.colourGreen = green;
        this.colourBlue = blue;
        this.grouping = grouping;
        this.toDisplay = toDisplay;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

//    public int getFont() {
//        return font;
//    }
//
//    public void setFont(int font) {
//        this.font = font;
//    }
//
    public byte getFontType() {
        return fontType;
    }

    public void setFontType(byte fontType) {
        this.fontType = fontType;
    }
//
//    public byte getFontCase() {
//        return fontCase;
//    }
//
//    public void setFontCase(byte fontCase) {
//        this.fontCase = fontCase;
//    }
//
//    public short getFontSize() {
//        return fontSize;
//    }
//
//    public void setFontSize(short fontSize) {
//        this.fontSize = fontSize;
//    }

    public byte getColourRed() {
        return colourRed;
    }

    public void setColourRed(byte colourRed) {
        this.colourRed = colourRed;
    }

    public byte getColourGreen() {
        return colourGreen;
    }

    public void setColourGreen(byte colourGreen) {
        this.colourGreen = colourGreen;
    }

    public byte getColourBlue() {
        return colourBlue;
    }

    public void setColourBlue(byte colourBlue) {
        this.colourBlue = colourBlue;
    }

    public short getGrouping() {
        return grouping;
    }

    public void setGrouping(short grouping) {
        this.grouping = grouping;
    }

    public boolean isToDisplay() {
        return toDisplay;
    }

    public void setToDisplay(boolean toDisplay) {
        this.toDisplay = toDisplay;
    }

    public static enum RegionType {
        GENE((byte)0, "gene"),
        CDS((byte)1, "CDS"),
        MISC_BINDING((byte)2, "misc_binding"),
        MISC_FEATURE((byte)3, "misc_feature"),
        MISC_RECOMB((byte)4, "misc_recomb"),
        MISC_RNA((byte)5, "misc_rna"),
        MISC_SIGNAL((byte)6, "misc_signal"),
        PRIMER((byte)7, "primer"),
        PRIMER_BIND((byte)8, "primer_bind"),
        REP_ORIGIN((byte)9, "rep_origin"),
        SIG_PEPTIDE((byte)10, "sig_peptide"),
        TERMINATOR((byte)11, "terminator"),
        EXCLUDE((byte)12, "exclude"),
        PROMOTER((byte)13, "promoter");

        private final byte type;
        private final String name;

        RegionType (Byte type, String name) {
            this.type = type;
            this.name = name;
        }

        public byte byteValue() {
            return type;
        }

        public String dispName() {
            return name;
        }
    }
    
    
    private boolean toDisplay; //whether it is shown on the graphical GCK display
    private int start; //first base in the sequence
    private int end; //last base in the sequence
    //font used to display the sequence
    //private int font;
    private byte fontType;
    //private byte fontCase;
    //private short fontSize;
    //colour for the sequence defined in rgb space
    private byte colourRed;
    private byte colourGreen;
    private byte colourBlue;
    private short grouping;
}
