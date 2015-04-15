package gck_convert;

import java.io.PrintWriter;
import java.io.FileNotFoundException;
import gckfilestructure.GCKFile;
import gckfilestructure.Feature;
import gckfilestructure.Region;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.LinkedList;

/**
 *
 * @author Matyas Medzihradszky
 * @version 0.1
 *
 */
public class GbFileWriter {

    /**
     * Standard constructor.
     */
    public GbFileWriter() {
        super();
    }

    /**
     * Writes the full gene bank file pulling information from the supplied GCK File object using the supplied library and parameters.
     *
     * @param gckFile A GCK File object including the needed information on Regions, Features, Header and Sequence.
     * @param outputFile The file to write the information to.
     * @param parseLevel How rigorously the number of included regions should be pruned.
     * @param library A list of names used to find out the Feature types.
     * @param includeApEData Whether extra information for A Plasmid Editor should be included in the gene bank file.
     * @param includeUnnamed Whether unnamed coloured regions in the original GCK file should be included.
     * @throws FileNotFoundException Thrown if the output file is not found.
     */
    public void writeGbFile(GCKFile gckFile, File outputFile, RegionParser.ParseLevel parseLevel, LinkedList<String[]> library, boolean includeApEData, boolean includeUnnamed, boolean includePrimers) throws FileNotFoundException {
        //setup variables
        this.gckFile = gckFile;
        printWriter = new PrintWriter(outputFile);
        calendar = Calendar.getInstance();
        this.parser = new RegionParser();
        this.includeApEData = includeApEData;
        writeGbFileHeader();
        //get the list of features and regions to write from the parser and write them to the file
        writeFeatures(parser.buildFeatureList(gckFile, parseLevel, library, includeUnnamed, includePrimers));
        writeSequence();
    }


    /**
     * Writes the standard GeneBank file header.
     */
    private void writeGbFileHeader() {
        printWriter.write(String.format("%-12s", "LOCUS"));
        printWriter.write(String.format("%-16.15s", gckFile.getConstructName()));
        printWriter.write(String.format("%10d", gckFile.getSequenceLength()));
        if (gckFile.isCircular()) {
            printWriter.write(" bp " + "ds-DNA     " + "circular     " + calendar.get(Calendar.DAY_OF_MONTH) + "-" + calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) + "-" + calendar.get(Calendar.YEAR) + "\n");
        } else {
            printWriter.write(" bp " + "ds-DNA     " + "linear       " + calendar.get(Calendar.DAY_OF_MONTH) + "-" + calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH) + "-" + calendar.get(Calendar.YEAR) + "\n");
        }
        printWriter.println("DEFINITION .");
        printWriter.println("ACCESSION   ");
        printWriter.println("VERSION     ");
        printWriter.println("SOURCE     .");
        printWriter.println("  ORGANISM .");
        printWriter.println("COMMENT");
        if (includeApEData) {
            printWriter.println("COMMENT    ApEinfo:methylated:1");
        }
    }

    /**
     * Write the list of features (sequence annotations) to the GeneBank file. Information about the annotations is
     * stored in the gckfilestructre.Feature object.
     *
     * @param featureList A list of gckfilestructure.Feature objects that contain the annotations to write to the GeneBank file.
     */
    private void writeFeatures(List<Feature> featureList) {
        //Data format: Feature keys start at column 6 and has at most 15 characters.
        //Data format: location and other qualifiers begin on column 22 and can extend to column 80.
        printWriter.write(String.format("%-21s%s\n", "FEATURES", "Location/Qualifiers"));
        for (Feature f : featureList) {
            printWriter.write(String.format("%5s%-16.15s", " ", f.getType().dispName()));
            if (f.getStrand() == Feature.Strand.REVERSE) { printWriter.write("(complement)"); }
            printWriter.write("" + f.getStart() + ".." + f.getEnd() + "\n");
            printWriter.write(String.format("%21s%7s%-2.52s\n", " ", "/label=", f.getName()));
            if (f.hasComment()) {  //TODO: Do we need line breaks?
                printWriter.write(String.format("%21s%9s\"%-2s\"\n", " ", "/comment=", f.getComment()));
            }
            //ApE data includes information for display in the ApE program. Not needed for a simple gb file.
            if (includeApEData) { writeApEData(f); }
        }
    }

    /**
     * Write information about an annotation for the ApE (A Plasmid Editor) program.
     * This information includes display colour and the arrow type and size.
     * The way the information is stored makes it not readily available for other programs.
     *
     * @param f The feature to write information about.
     */
    private void writeApEData(Feature f) {
        if (((int) f.getColourRed() & 0xff) > 0 || ((int) f.getColourGreen() & 0xff) > 0 || ((int) f.getColourBlue() & 0xff) > 0) {
            printWriter.println("                     /ApEinfo_fwdcolor=#" + String.format("%02X", f.getColourRed()) +
                    String.format("%02X", f.getColourGreen()) +
                    String.format("%02X", f.getColourBlue()));
            printWriter.println("                     /ApEinfo_revcolor=#" + String.format("%02X", f.getColourRed()) +
                    String.format("%02X", f.getColourGreen()) +
                    String.format("%02X", f.getColourBlue()));
        } else { //Use these default colours if none are included
            if (f.getType() == Region.RegionType.GENE) {
                printWriter.write("                     /ApEinfo_fwdcolor=#ff0000\n");
                printWriter.write("                     /ApEinfo_revcolor=#ff0000\n");
            } else {
                printWriter.write("                     /ApEinfo_fwdcolor=#00ff00\n");
                printWriter.write("                     /ApEinfo_revcolor=#00ff00\n");
            }
        }
        if (f.getStrand() == Feature.Strand.FORWARD) {
            printWriter.println("                     /ApEinfo_graphicformat=arrow_data {{0 1 2 0 0 -1} {} 0}");
        } else if (f.getStrand() == Feature.Strand.REVERSE) {
            printWriter.println("                     /ApEinfo_graphicformat=arrow_data {{0 1 2 0 0 -1} {} 0}");
        } else if (f.getStrand() == Feature.Strand.BOTH) { //Double headed arrow
            printWriter.println("                     /ApEinfo_graphicformat=arrow_data {{0 1 2 0 0 -1} {0 1 2 0 0 -1} 0}");
        } else { //Blunt arrow (no arrowheads)
            printWriter.println("                     /ApEinfo_graphicformat=arrow_data {{} {} 0}");
        }
        printWriter.println("                     width 5 offset 0");
    }

    /**
     * Writes the sequence extracted from the GCK file to the final GeneBank file and also 'closes' the file.
     * This should be called last when writing a GeneBank file.
     */
    private void writeSequence() {
        printWriter.println("ORIGIN");
        int i=0;
        while (i < gckFile.getSequenceLength()) {
            for (int s=0; s<(9-String.valueOf(i).length()); s++) {
                printWriter.write(" ");
            }
            printWriter.write(String.valueOf(i+1));
            printWriter.write(" ");
            for(int j=0; j<6; j++) {
                String sub;
                try {
                    sub = gckFile.getSequence().substring(i, i+10);
                } catch (IndexOutOfBoundsException ex) {
                    sub = gckFile.getSequence().substring(i);
                }                
                printWriter.write(sub);
                i=i+10;
                if (i < gckFile.getSequenceLength() && j<5) {
                    printWriter.write(" ");                    
                } else {
                    printWriter.write("\n");
                    break;
                }
            }
        }
        printWriter.write("//\n");
        printWriter.close();
    }

    /**
     * Changes whether ApE data should be included in the final file or not.
     *
     * @param state
     */
    public void setApEData(boolean state) {
        includeApEData = state;
    }
    
    private PrintWriter printWriter;
    private GCKFile gckFile;
    private Calendar calendar; //used to include the date in the resulting GCK file
    private RegionParser parser;
    private boolean includeApEData;
}
