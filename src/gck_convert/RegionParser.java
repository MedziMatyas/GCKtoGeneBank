package gck_convert;

import gckfilestructure.GCKFile;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.LinkedList;

import gckfilestructure.Region;
import gckfilestructure.Feature;

/**
 * RegionParser is responsible for the pruning, ordering, and sorting of the 
 * regions and features extracted from a GCK file.
 * 
 * It has a number of methods to validate and modify the extracted list of 
 * regions and features.
 *
 * Note: Sorting is not implemented at the moment.
 * 
 * @author Matyas Medzihradszky
 * @version 0.1
 */
public class RegionParser {

    /**
     * Standard, default constructor.
     */
    public RegionParser() {
        super();
    }

    /**
     * Creates the list of Features (sequence annotations) to write to the final GeneBank file. The list is built
     * depending on the provided parameters, library and parsing level.
     *
     * @param gckFile Contains all the information about the found Features and Regions.
     * @param parseLevel The level of scrutiny to use in which Regions and Features to keep.
     * @param library Contains information about how to annotate Features.
     * @param includeUnnamed Whether unnamed Regions are to be included.
     * @param includePrimers Whether primer annotations are to be included.
     * @return Returns a list of Features containing all the annotations to be included in the final GeneBank file.
     */
    public List<Feature> buildFeatureList(GCKFile gckFile, ParseLevel parseLevel, LinkedList<String[]> library, boolean includeUnnamed, boolean includePrimers) {
        this.gckFile = gckFile;
        this.parseLevel = parseLevel;
        LinkedList<Feature> featureList = new LinkedList();
        pairRegionsWithFeatures();
        idFeatures(library);
        parseRegions();
        validateNames();
        //Determine which of the Features to include in the final file.
        for (int i = 0; i < gckFile.getNumFeatures(); i++) {
            Feature f = gckFile.getFeature(i);
            if (f.isToDisplay()) {
                idFeature(f, library); //redundant
                if(f.getType() != Region.RegionType.EXCLUDE && !f.isAutomatic()) {
                    if (f.getType() != Region.RegionType.PRIMER_BIND || includePrimers) {
                        featureList.add(f);
                    }
                }
            }
        }
        //Add the Regions to be displayed to the final Feature list. Only called if includeUnnamed is TRUE.
        if (includeUnnamed) {
            for (int i = 0; i < gckFile.getNumRegions(); i++) {
                if (gckFile.getRegion(i).isToDisplay()) {
                    Region r = gckFile.getRegion(i);
                    Feature f = new Feature(r, "", "", Feature.Strand.BOTH, Region.RegionType.MISC_FEATURE);
                    f.setName("region" + i); //This also needs to change.
                    f.setType(Region.RegionType.MISC_FEATURE); //Regions do not have names so library not usable.
                    featureList.add(f);
                }
            }
        }
        return featureList;
    }

    /**
     * Pairs a region with the corresponding feature if it exists.
     * 
     * This is needed because GCK files store coloured regions of the sequence and
     * a corresponding defined sequence separately, without a clear way to connect
     * the two as even starting and ending points can be different.
     */
    private void pairRegionsWithFeatures() {
        //Pair regions to features if it is possible.
        //Regions are not always the same length as the corresponding feature because
        //features are only the protein sequence without stops or leading bases.
        //Thus allow +- 5 bp difference on both sides (this is arbitrary).
        for (int i = 0; i < gckFile.getNumRegions(); i++) {
            for (int j = 0; j < gckFile.getNumFeatures(); j++) {
                if(     gckFile.getRegion(i).getStart() - gckFile.getFeature(j).getStart() <= 5 &&
                        gckFile.getRegion(i).getStart() - gckFile.getFeature(j).getStart() >= -5 &&
                        gckFile.getRegion(i).getEnd() - gckFile.getFeature(j).getEnd() <= 5 && 
                        gckFile.getRegion(i).getEnd() - gckFile.getFeature(j).getEnd() >= -5) {
                    //If a match has been found copy the colour data from the region. Only actually matters for APE.
                    gckFile.getFeature(j).setColourRed(gckFile.getRegion(i).getColourRed());
                    gckFile.getFeature(j).setColourGreen(gckFile.getRegion(i).getColourGreen());
                    gckFile.getFeature(j).setColourBlue(gckFile.getRegion(i).getColourBlue());
                    //Set the region's display attribute to false.
                    gckFile.getRegion(i).setToDisplay(false);
                }
            }
        }
    }

    /**
     * Remove duplicate features keeping only the CDS one or if both are genes, the second one.
     * Make sure the direction is also the same.
     */
    private void removeDuplicates() {
        for (int i = 0; i < gckFile.getNumFeatures()-1; i++) {
            for (int j = i + 1; j < gckFile.getNumFeatures(); j++) {
                //Because of the way things are stored in GCK files it often
                //+happens that the same feature is stored with slightly
                //+different end and start points. Such as when a feature has
                //+an arrow as well as a protein sequence.
                if (gckFile.getFeature(i).getStart() - gckFile.getFeature(j).getStart() <= 5 &&
                        gckFile.getFeature(i).getStart() - gckFile.getFeature(j).getStart() >= -5 &&
                        gckFile.getFeature(i).getEnd() - gckFile.getFeature(j).getEnd() <= 5 &&
                        gckFile.getFeature(i).getEnd() - gckFile.getFeature(j).getEnd() >= -5) {
                    if (gckFile.getFeature(i).getStrand() == gckFile.getFeature(j).getStrand()) {
                        if (gckFile.getFeature(j).getType() == Region.RegionType.CDS) {
                            gckFile.getFeature(i).setToDisplay(false);
                        } else {
                            gckFile.getFeature(j).setToDisplay(false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes regions smaller than 10 bp.
     * These are usually automatic annotations by GCK.
     * If the inclusion of unnamed regions is not selected by the user, this is redundant.
     */
    private void removeSmallRegions() {
        for (int i = 0; i < gckFile.getNumRegions(); i++) {
            if ((gckFile.getRegion(i).getEnd()-gckFile.getRegion(i).getStart()) <= 10) {
                gckFile.getRegion(i).setToDisplay(false);
            }
        }
    }

    /**
     * Mark features that have been marked for exclusion.
     */
    private void markExcluded() {
        for (int i = 0; i < gckFile.getNumFeatures(); i++) {
            if (gckFile.getFeature(i).getType() == Region.RegionType.EXCLUDE) {
                gckFile.getFeature(i).setToDisplay(false);
            }
        }
    }

    /**
     * Will remove any Region within a Feature. Unless you have the option of keeping unnamed Regions, this is redundant.
     * Only removes regions withing Features set to be displayed.
     */
    private void removeRegionsInFeatures() {
        for (int i = 0; i < gckFile.getNumFeatures(); i++) {
            Feature f = gckFile.getFeature(i);
            for (int j = 0; j < gckFile.getNumRegions(); j++) {
                Region r = gckFile.getRegion(j);
                if (r.getStart() >= f.getStart() && r.getEnd() <= f.getEnd() && f.isToDisplay()) {
                    r.setToDisplay(false);
                }
            }
        }
    }

    /**
     * Use this if you only want to keep the largest Features in the final file.
     * This is usually not a good idea as named features will contain a lot of information.
     * It is better to use the library to prune what is kept and what is thrown away.
     */
    private void removeFeaturesInFeatures() {
        for (int i = 0; i < gckFile.getNumFeatures() - 1; i++) {
            for (int j = i + 1; j < gckFile.getNumFeatures(); j++) {
                if(gckFile.getFeature(i).getStart() <= gckFile.getFeature(j).getStart() && gckFile.getFeature(i).getEnd() >= gckFile.getFeature(j).getEnd()) {
                    gckFile.getFeature(j).setToDisplay(false);
                }
                if(gckFile.getFeature(j).getStart() <= gckFile.getFeature(i).getStart() && gckFile.getFeature(j).getEnd() >= gckFile.getFeature(i).getEnd()) {
                    gckFile.getFeature(i).setToDisplay(false);
                }
            }
        }
    }

    /**
     * Determines which features and regions to keep.
     * 
     * Dependent on a given ParseLevel tries to reduce the number of features and
     * regions to be included in the final GeneBank file.
     * This is needed because GCK includes a lot of automatic annotations that only
     * make the final file cluttered.
     */
    private void parseRegions() {
        switch (parseLevel) {
            case HIGHEST: removeFeaturesInFeatures();
            case HIGH: markExcluded(); removeRegionsInFeatures();
            case MEDIUM: removeSmallRegions();
            case LOW: removeDuplicates();
        }
    }
    
    /**
     * Assigns types to the features bases upon their names if possible.
     * 
     * Uses defined patterns for determining if a feature is part of a group or
     * not. The patterns are defined in the class definition or in a separate
     * library file.
     *
     * @param library Definitions of region types depending on names.
     */
    private void idFeatures(LinkedList<String[]> library) {
        //Put back any features containing primer binding sites
        for (int i = 0; i < gckFile.getNumFeatures(); i++) {
            for (String[] s: library) {
                currentPattern = Pattern.compile(s[0]);
                matcher = currentPattern.matcher(gckFile.getFeature(i).getName().toLowerCase());
                if (matcher.find()) {
                    gckFile.getFeature(i).setType(Region.RegionType.valueOf(s[1].toUpperCase()));
                }
            }
        }
    }

    /**
     * Assigns a type to a feature bases upon its names if possible.
     *
     * Uses defined patterns for determining if a feature is part of a group or
     * not. The patterns are defined in the class definition or in a separate
     * library file.
     *
     * @param f The feature to examine.
     * @param library Definitions of region types depending on names.
     */
    private void idFeature(Feature f, LinkedList<String[]> library) {
        for (String[] s: library) {
            currentPattern = Pattern.compile(s[0]);
            matcher = currentPattern.matcher(f.getName().toLowerCase());
            if (matcher.find()) {
                f.setType(Region.RegionType.valueOf(s[1].toUpperCase()));
            }
        }
    }
    
    /**
     * Modifies feature names to be acceptable GeneBank names if needed.
     */
    private void validateNames() {
        for (int i = 0; i < gckFile.getNumFeatures(); i++) {
            String name = gckFile.getFeature(i).getName();
            if (name.contains("=")) {
                gckFile.getFeature(i).setName(name.replace("=", "_"));
            }
        }
    }

    public static enum ParseLevel {
        NONE(0),
        LOW(10),
        MEDIUM(100),
        HIGH(200),
        HIGHEST(300);

        private int level;
        ParseLevel(int level) {
            this.level = level;
        }

        public int getNumLevel() {
            return level;
        }

        public boolean greater(ParseLevel compLevel) {
            return (this.level > compLevel.getNumLevel());
        }
    }
    
    private GCKFile gckFile;
    private ParseLevel parseLevel;
    private Pattern currentPattern;
    private Matcher matcher;
}
