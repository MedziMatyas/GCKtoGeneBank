package gck_convert;

import java.io.FileInputStream;

import gckfilestructure.*;
import java.io.IOException;
import java.util.logging.Logger;
import java.nio.ByteBuffer;

/**
 * Reads and extracts data from a gck sequence file.
 * 
 * The extracted data is stored in an associated GCKFile object.
 * Contains methods to extract information about regions and features.
 * Marks or enzyme cleavage sites cannot be extracted at this time.
 * 
 * @author Matyas Medzihradszky
 */
public class GCKFileAnalyzer {
    
    public GCKFileAnalyzer(GCKFile gckFile) throws IOException{
        super();
        this.gckFile = gckFile;
        fileInputStream = new FileInputStream(gckFile.getFile());
    }

    public void readGCKFile() {
        readFileHeader();
        readSequence();
        readRegions();
        readFeatures();
        readFeatureNamesAndComments();
        try {
            findCircularity();
        } catch (IndexOutOfBoundsException e) {
            gckFile.setCircular(true);
            logger.severe("Failed to determine circularity, assuming circular as fallback.");
        }
    }

    /**
     * Reads basic information about the file to be analyzed.
     * 
     * Reads the stored sequence length, the length of the feature definitions,
     * and the length of region definitions. These are later used to move around
     * in the file to the relevant positions.<br />
     * 
     * <b>IMPORTANT: Must be called before any other methods of the class can be 
     * used reliably.</b>
     */
    private void readFileHeader() {
        logger.info("Reading file header.");
        buffer = ByteBuffer.allocate(gckFile.HEADER_LENGTH);
        try {
            //Set read position to the start of the file.
            fileInputStream.getChannel().position(0);
            
            //Read sequence length data.
            fileInputStream.read(buffer.array(), 0, gckFile.HEADER_LENGTH);
            gckFile.setSequenceLength(buffer.getInt(buffer.capacity()-(Integer.SIZE/8)));
            
            //Read length of region definitions.
            fileInputStream.skip(gckFile.getSequenceLength());
            fileInputStream.read(buffer.array(), 0, 4);
            gckFile.setLengthRegions(buffer.getInt(0));
            
            //Read length of feature definitions.
            fileInputStream.skip(gckFile.getLengthRegions());
            fileInputStream.read(buffer.array(), 0, 4);
            gckFile.setLengthFeatures(buffer.getInt(0));

            
            logger.info("Region definitions length =" + gckFile.getLengthRegions() + "\n Feature definitions length =" + gckFile.getLengthFeatures());
        } catch (IOException ex) {
            logger.severe("Failed reading file header.");
        }
        logger.info("File header read successfully.");
    }
    
    /**
     * Extracts and stores region information.
     * 
     * Regions are basically how the sequence data is formatted in the gck file.
     * These can contain colours, fonts, font-sizes, and types, but we are only
     * interested in the colours at the moment.
     */
    private void readRegions() {
        logger.info("Reading region information.");
        buffer = ByteBuffer.allocate(GCKFile.REGION_DEF_LENGTH);
        try {
            //Set read position to the end of the sequence listing where region
            //definitions start.
            fileInputStream.getChannel().position(gckFile.HEADER_LENGTH + gckFile.getSequenceLength());
            
            //Skip the offset and the sequence length entry.
            fileInputStream.skip(8);
            
            //Read the number of regions specified and allocate the needed memory.
            fileInputStream.read(buffer.array(), 0, 2);
            gckFile.setNumRegions(buffer.getShort(0));
            logger.info("Found " + gckFile.getNumRegions() + " regions.");
            gckFile.allocateRegions();
            
            //Iterate through all the regions extracting and storing data.
            for (int i = 0; i < gckFile.getNumRegions(); i++) {
                //Read a full region record into the buffer.
                fileInputStream.read(buffer.array(), 0, buffer.capacity());
                gckFile.getRegion(i).setStart(buffer.getInt(0) + 1);
                gckFile.getRegion(i).setEnd(buffer.getInt(4));
                gckFile.getRegion(i).setFontType(buffer.get(12));
                gckFile.getRegion(i).setColourRed(buffer.get(16));
                gckFile.getRegion(i).setColourGreen(buffer.get(18));
                gckFile.getRegion(i).setColourBlue(buffer.get(20));

                //If a region is black we do not display it. We assume that black is the base colour
                //thus all of these are just the normal un-annotated sequence regions.
                if (((int)gckFile.getRegion(i).getColourRed() & 0xff) > 0 || ((int)gckFile.getRegion(i).getColourGreen() & 0xff) > 0 || ((int)gckFile.getRegion(i).getColourBlue() & 0xff) > 0) {
                    gckFile.getRegion(i).setToDisplay(true);
                }
                logger.info("Region " + i + " = " + gckFile.getRegion(i).getStart()+ "-" + gckFile.getRegion(i).getEnd());
            }           
        } catch (IOException ex) {
            logger.severe("Failed reading sequence regions.");
        }
        logger.info("Region information read successfully.");
    }
    
    /** 
     * Features are the various defined elements in the sequence.
     * 
     * These have direction as well as names.
     */
    private void readFeatures() {
        logger.info("Reading feature definitions.");
        try {
            //Set read position to the end of the regions, where the feature definitions start.
            fileInputStream.getChannel().position(gckFile.HEADER_LENGTH + gckFile.getSequenceLength() + gckFile.getLengthRegions() + 4);

            //Skip the offset and the sequence length entry.
            fileInputStream.skip(8);
            
            //Read the number of features specified.
            buffer = ByteBuffer.allocate(2);
            fileInputStream.read(buffer.array(), 0, 2);
            gckFile.setNumFeatures(buffer.getShort(0));
            logger.info("Found " + gckFile.getNumFeatures() + " features.");
            //Make sure we have at least one Feature.
            if (gckFile.getNumFeatures() > 0) {
                //Calculate the size of each record for a feature as these can be variable.
                //Usually either 0x5C or 0x5E.
                //6 bytes are subtracted as they are the sequence length and the number of features.
                buffer = ByteBuffer.allocate((gckFile.getLengthFeatures()-6)/gckFile.getNumFeatures());
                gckFile.allocateFeatures();
                
                //Iterate through the features extracting and storing data.
                for (int i = 0; i < gckFile.getNumFeatures(); i++) {
                    
                    //Read a full feature record into the buffer. 
                    //IMPORTANT: This does not contain the name of the Feature, or any associated comments.
                    fileInputStream.read(buffer.array(), 0, buffer.capacity());

                    gckFile.getFeature(i).setStart(buffer.getInt(0) + 1);
                    gckFile.getFeature(i).setEnd(buffer.getInt(4));
                    gckFile.getFeature(i).setType(buffer.getShort(14)); //Not sure how long and what the CDS/GENE definition contains, but it is 0 if it is but a GENE and larger otherwise.
                    gckFile.getFeature(i).setStrand(buffer.get(30));
                    gckFile.getFeature(i).setColourRed(buffer.get(42));
                    gckFile.getFeature(i).setColourGreen(buffer.get(44));
                    gckFile.getFeature(i).setColourBlue(buffer.get(46));
                    
                    //If the feature does not have a unique identifier it will
                    //also not have a name entry, thus we have to set it explicitly.
                    if (buffer.getInt(48) == 0) {
                        gckFile.getFeature(i).hasName(false);
                    } else {
                        gckFile.getFeature(i).hasName(true);
                    }

                    if (buffer.getInt(52) == 0) {
                        gckFile.getFeature(i).hasComment(false);
                    } else {
                        gckFile.getFeature(i).hasComment(true);
                    }

                    if (buffer.getShort(56) == (short)0x0115) { //This is not 100% sure, but was like this for all found files.
                        gckFile.getFeature(i).setAutomatic(true);
                    }
                    logger.info("Feature " + gckFile.getFeature(i).getStart() + ".." + gckFile.getFeature(i).getEnd() + " Type = " + gckFile.getFeature(i).getType().dispName());
                }
            }
        } catch (IOException ex) {
            logger.severe("Failed reading features.");
        }
        logger.info("Feature information read successfully.");
    }
    
    /**
     * Read and connect the stored names with the features.
     * 
     * Should be called after the features have been read from the file.
     */
    private void readFeatureNamesAndComments() {
        logger.info("Reading feature names and comments.");
        buffer = ByteBuffer.allocate(0xffff);
        try {
            //Set the reading position to the end of the feature definitions.
            fileInputStream.getChannel().position(gckFile.HEADER_LENGTH + gckFile.getSequenceLength() + gckFile.getLengthRegions() + gckFile.getLengthFeatures() + 8);
            
            //Iterate through the Features adding the names.
            for (int i = 0; i < gckFile.getNumFeatures(); i++) {
                if(gckFile.getFeature(i).hasName()) {
                    //Names are short and their length is stored in a byte variable. It is unsigned.
                    fileInputStream.read(buffer.array(), 0, 1);
                    int nameLength = ((int)buffer.get(0) & 0xff);
                    fileInputStream.read(buffer.array(), 1, nameLength);
                        
                    //Need to convert all the bytes to chars so that we can
                    //make a String, as GCK files use 1 byte chars.
                    char[] c = new char[nameLength];
                    for (int j = 0; j < c.length; j++) {
                        c[j] = (char)(buffer.get(j+1));
                    }
                    gckFile.getFeature(i).setName(String.valueOf(c));
                    logger.info(gckFile.getFeature(i).getName() + " = name of " + i + ". feature");
                } else {
                    gckFile.getFeature(i).setName("NONE");
                }
                if(gckFile.getFeature(i).hasComment()) {
                    fileInputStream.read(buffer.array(), 0, 4);
                    int commentLength = buffer.getInt(0);
                    logger.info("Needed buffer length: " + commentLength);
                    fileInputStream.read(buffer.array(), 4, commentLength);
                    char[] c = new char[buffer.getInt(0)];
                    for (int j = 0; j < c.length; j++) {
                        c[j] = (char)(buffer.get(j+4));
                    }
                    gckFile.getFeature(i).setComment(String.valueOf(c));
                }
            }
        } catch (IOException ex) {
            logger.severe("Failed reading feature names and comments");
        }
    }

    /**
     * Finds the length of a section containing names and comments.
     * In a normal GCK file there are two such sections. One for the Features and one for the Sites.
     *
     * @param startPosition Position in the file where the section starts.
     * @param sites A list of sites for which the names and comments are stored. Needed as not all Features/Sites have names and/or comments.
     *
     * @return The length of the section in number of bytes.
     */
    private int getNameAndCommentsLength(long startPosition, Site[] sites) {
        ByteBuffer localBuffer = ByteBuffer.allocate(4);
        int sectionLength = 0;
        try {
            fileInputStream.getChannel().position(startPosition);
            for (Site site : sites) {
                if (site.hasName()) {
                    sectionLength += 1; //We have a name and its length is stored in a single byte.
                    fileInputStream.read(localBuffer.array(), 0, 1);
                    sectionLength += ((int) localBuffer.get(0) & 0xff); //This is the length of the name itself.
                    fileInputStream.skip(((int) localBuffer.get(0) & 0xff)); //We skip both the length definition and the name itself.
                }
                if (site.hasComment()) {
                    sectionLength += 4; //The length of the comment size definition.
                    fileInputStream.read(localBuffer.array(), 0, 4); //We read the length (automatically skips ahead).
                    sectionLength += localBuffer.getInt(0);
                    fileInputStream.skip(localBuffer.getInt(0)); //We skip the comment length.
                }
            }
            fileInputStream.getChannel().position(startPosition); //Put the reader back to where we were. TODO: these should also be local.
        } catch (IOException e) {
            logger.severe("Failed name and comment length calculation.");
            return -1;
        }
        return sectionLength;
    }
    
    /**
     * Reads and stores the DNA sequence stored in the gck file.
     */
    private void readSequence() {
        buffer = ByteBuffer.allocate(gckFile.getSequenceLength());
        logger.info("Sequence reading started.");
        if (buffer.capacity() > 0) {
            try {
                fileInputStream.getChannel().position(gckFile.HEADER_LENGTH);
                fileInputStream.read(buffer.array(), 0, gckFile.getSequenceLength());
                char[] c = new char[gckFile.getSequenceLength()];
                for (int i = 0; i < gckFile.getSequenceLength(); i++) {
                    c[i] = (char)(buffer.get());
                }
                gckFile.setSequence(String.valueOf(c));
//                logger.info(sequence.getSequence());
            } catch (IOException ex) {
                logger.severe("Failed reading sequence.");
            }
        }
    }

    /**
     * Navigates in the file to the position where the byte marking circularity is stored.
     * In the process it also extracts the name of the construct.
     */
    private void findCircularity() throws IndexOutOfBoundsException {
    	//Need to skip: header, sequence, regions, features, feature names
    	//puts mark at end of feature definitions: 
    	//gckFile.getHeader().getHEADER_SIZE()+gckFile.getSequence().getLength()+gckFile.getLengthRegions()+gckFile.getLengthFeatures()
    	//gckFile.
        try {
            fileInputStream.getChannel().position(gckFile.HEADER_LENGTH + gckFile.getSequenceLength() + gckFile.getLengthRegions() + gckFile.getLengthFeatures() + 8);
            logger.info("Start position for Circularity = " + fileInputStream.getChannel().position());
            int offset = getNameAndCommentsLength(fileInputStream.getChannel().position(), gckFile.getFeatures());
            logger.info("Offset = " + offset);
            fileInputStream.skip(offset);
            logger.info("Current position = " + fileInputStream.getChannel().position());
            buffer = ByteBuffer.allocate(88);
            fileInputStream.read(buffer.array(), 0, 10); //Reads the length of definitions (total), the sequence length and the number of sites.
            gckFile.setLengthSites(buffer.getInt(0));
            logger.info("Site length = " + buffer.getInt(0) + " Current position = " + fileInputStream.getChannel().position());
            int sitesLength = buffer.getInt(0) - 6; //6 bytes contain the sequence length and the number of features (int.size + short.size)
            int sequenceLength = buffer.getInt(4);
            gckFile.setNumSites(buffer.getShort(8));
            logger.info("Number of sites = " + gckFile.getNumSites());
            allocateSites(gckFile.getNumSites());
            if (gckFile.getNumSites() > 0) {
                int definitionsLength = sitesLength / gckFile.getNumSites(); //this should be 88, but just to make sure we calculate it
                for (Site site: gckSites) {
                    fileInputStream.read(buffer.array(), 0, definitionsLength);
                    if (buffer.getInt(32) > 0) {
                        site.hasName(true);
                    } else {
                        site.hasName(false);
                    }
                    if (buffer.getInt(36) > 0) {
                        site.hasComment(true);
                    } else {
                        site.hasComment(false);
                    }
                }
            }
            fileInputStream.skip(getNameAndCommentsLength(fileInputStream.getChannel().position(), gckSites)); //We should be at the beginning of the names, so we just need to skip that.
            fileInputStream.read(buffer.array(), 0, 4);
            fileInputStream.skip(buffer.getInt(0)); //Not sure what this section is, but it is between the sites and the generations.
            fileInputStream.read(buffer.array(), 0, 2);
            short numGenerations = buffer.getShort(0);
            fileInputStream.skip(GCKFile.GENERATION_DEF_LENGTH * numGenerations); //Each generation information is stored on 260 bytes.
            if (gckFile.getFileType() == GCKFile.FileType.GCS && numGenerations > 0) { //TODO: check if this is really something that can only happen with GCS files and not GCC files.
                fileInputStream.read(buffer.array(), 0, 4);
                int tempLength = buffer.getInt(0);
                fileInputStream.skip(tempLength);
                if (tempLength == 0) {
                    fileInputStream.getChannel().position(fileInputStream.getChannel().position() - 4);
                }
            }
            fileInputStream.skip(GCKFile.CONSTRUCT_NAME_OFFSET); //Not sure what is stored here, but this puts us just before the construct name.
            fileInputStream.read(buffer.array(), 0, 1); //The length of the construct name.
            int constructNameLength = ((int) buffer.get(0) & 0xff);
            logger.info("ConstructNameLength = " + constructNameLength);
            try {
                fileInputStream.read(buffer.array(), 0, constructNameLength);
            } catch (IndexOutOfBoundsException e) {
                throw e;
            }

            //Convert char sequence to string, through character array.
            char[] c = new char[constructNameLength];
            for (int i = 0; i < constructNameLength; i++) {
                c[i] = (char)(buffer.get());
            }
            gckFile.setConstructName(String.valueOf(c)); //Set the name of the construct.

            fileInputStream.skip(16); //Not sure what is stored here, but this is the offset between the name and the flags (probably flags).
            fileInputStream.read(buffer.array(), 0, 1); //This is the byte we want, the one that stores whether the construct is linear or circular. 0 if linear, 1 if circular.
            if (buffer.get(0) == 0x00) {
                gckFile.setCircular(false);
            } else if (buffer.get(0) == 0x01) {
                gckFile.setCircular(true);
            }
            logger.info("File is circular: " + gckFile.isCircular());
        } catch (IOException e) {
            logger.severe("Cannot determine if circular.");
        }
    }

    private void allocateSites(int size) {
        gckSites = new Site[size];
        for (int i = 0; i < gckSites.length; i++) {
            gckSites[i] = new Site(true, false);
        }
    }
    
    private FileInputStream fileInputStream; //This is global, but it can cause problems. Might not be the best, or at least needs a local one.
    private GCKFile gckFile;
    private final static Logger logger = Logger.getLogger("GCK_Converter_logger");
    private ByteBuffer buffer; //Do we need a global one at all?
    private Site[] gckSites;
}
