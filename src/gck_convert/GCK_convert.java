package gck_convert;

import java.awt.EventQueue;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.FileHandler;
import gckfilestructure.GCKFile;
import gckfilestructure.Region;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JComboBox;
import javax.swing.filechooser.FileFilter;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import java.util.LinkedList;
import java.util.Scanner;
import javax.swing.JLabel;


/**
 *
 * @author Matyas Medzihradszky
 * @version 0.1
 */
public class GCK_convert extends JPanel implements ActionListener, ItemListener {

    /**
     * Constructor that initializes variables and sets up the display area for the program.
     */
    public GCK_convert() {
        super(new GridBagLayout());
        
//        setupLogging();
        
        //Initialize booleans
        outputDirectorySet = false;
        includeApEData = false;
        includeUnnamed = false;
        includePrimers = false;
        withoutErrors = true;
        parseLevel = RegionParser.ParseLevel.MEDIUM;

        logger.setLevel(Level.INFO);
        
        
        //General display for files and information.
        disp = new JTextArea(10, 40);
        disp.setMargin(new Insets(5,5,5,5));
        disp.setEditable(false);
        JScrollPane dispScrollPane = new JScrollPane(disp);

        //Displayes the selected destination directory
        dirDisp = new JTextArea(1, 40);
        dirDisp.setEditable(false);
        dirDisp.setMargin(new Insets(5,5,5,5));
        dirDisp.setBorder(BorderFactory.createLineBorder(Color.black));

        //File filter for file selection
        filter = new GCKFilter();

        //File selection dialog
        selector = new JFileChooser();
        selector.setAcceptAllFileFilterUsed(true);
        selector.setFileFilter(filter);
        selector.setMultiSelectionEnabled(true);

        //Directory selection dialog
        dirSelector = new JFileChooser();
        dirSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirSelector.setMultiSelectionEnabled(false);

        fileSelect = new JButton("Select files");
        fileSelect.addActionListener(this);
        fileSelect.setToolTipText("Select the files to convert. The files will not be modified themselves, but a copy made.");
        
        convert = new JButton("Convert files");
        convert.addActionListener(this);
        convert.setToolTipText("Start conversion of the files.");
        
        dirSelect = new JButton("Set destination");
        dirSelect.addActionListener(this);
        dirSelect.setToolTipText("Select the destination directory where the converted files will be located. The default directory is where the files to convert are located.");
        
        ape = new JCheckBox("Include ApE data");
        ape.setSelected(false); //Changed default behavior to not be included
        ape.addItemListener(this);
        ape.setToolTipText("Select whether to include information that ApE uses for colours and arrows.");

        unnamed = new JCheckBox("Include unnamed regions");
        unnamed.setSelected(false);
        unnamed.addItemListener(this);
        unnamed.setToolTipText("Include unnamed coloured regions in the final file.");

        primers = new JCheckBox("Include primers");
        primers.setSelected(false);
        primers.addItemListener(this);
        primers.setToolTipText("Include primer binding sites in the final file.");
        
        
        //Add components
        GridBagConstraints c = new GridBagConstraints();
        //Buttons
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(3,3,3,3);
        add(fileSelect, c);
        c.gridx = 2;
        add(unnamed, c);
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        add(ape, c);
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        add(dirSelect, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1.0;
        add(new JLabel(""), c);
        c.gridx = 2;
        c.weightx = 0.0;
        c.anchor = GridBagConstraints.WEST;
        add(primers, c);
        c.gridx = 2;
        c.gridy = 5;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(convert, c);
        
        //Destination directory
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        add(new JLabel("Destination directory: "), c);
        c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        add(dirDisp, c);
        
        //Add display area
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        add(dispScrollPane, c);
        
        //Setup, prepare, and read library of feature definitions.
        library = new LinkedList<String[]>();
        readLibrary();
    }

    /**
     * Listener for button and item events.
     * Does not handle Checkboxes, those are separate.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == fileSelect) {
            int returnVal = selector.showDialog(GCK_convert.this, "Select");

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                disp.setText("");
                fileListToProcess = selector.getSelectedFiles();
                disp.append("Files selected:\n");
                for (int i = 0; i < fileListToProcess.length; i++) {
                    disp.append(fileListToProcess[i].getName() + "\n");
                }
                if (!outputDirectorySet) {
                    directoryPath = fileListToProcess[0].getParent();
                    dirDisp.setText(directoryPath);
                }
                
            } else {
            }
            disp.setCaretPosition(disp.getDocument().getLength());

        } else if (e.getSource() == convert) {
            Thread beginProcessingThread = new Thread() {
                public void run() {
                    disp.append("\n\nFile conversion STARTED\n");
                    disp.setCaretPosition(disp.getDocument().getLength());
                }
            };
            beginProcessingThread.start();
            counter = fileListToProcess.length;
            logger.severe("Found " + counter + " files to process.");
            for (int i = 0; i < fileListToProcess.length; i++) {
                File outputFile = new File(directoryPath, getNameWOExt(fileListToProcess[i]) + ".gb");
                Thread t = new Thread(new convertThread(fileListToProcess[i], outputFile));
                t.start();
            }
            Thread testIfDoneThread = new Thread() {
                public void run() {
                    while(counter > 0) {
                        try { sleep(200); }
                        catch (InterruptedException e1) {}
                    }
                    disp.append("\n\nFile conversion DONE!\n");
                    disp.setCaretPosition(disp.getDocument().getLength());
                }
            };
            testIfDoneThread.start();
        } else if (e.getSource() == dirSelect) {
            int returnVal = dirSelector.showDialog(GCK_convert.this, "Destination");
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                if (!dirSelector.getSelectedFile().isDirectory()) {
                    directoryPath = dirSelector.getCurrentDirectory().getPath();
                } else {
                    directoryPath = dirSelector.getSelectedFile().getPath();
                }              
                dirDisp.setText(directoryPath);
                outputDirectorySet = true;
            }
        }
    }

    private class convertThread implements Runnable {
        convertThread(File input, File output) {
            super();
            inputFile = input;
            outputFile = output;
        }
        public void run() {
            convertFile(inputFile, outputFile);
        }

        File inputFile;
        File outputFile;
    }

    private synchronized void convertFile(File inputFile, File outputFile) {
        GCKFile gckFile;
        boolean success = true;
        if (getExt(inputFile).equalsIgnoreCase("gcc")) {
            gckFile = new GCKFile(inputFile, GCKFile.FileType.GCC);
        } else {
            gckFile = new GCKFile(inputFile, GCKFile.FileType.GCS);
        }
        disp.append("\nFile: " + inputFile.getName() + " is being converted ... ");
        int currentPosition = disp.getCaretPosition();
        try {
            readFile(gckFile);
            writeFile(gckFile, outputFile);
        } catch (Exception e) {
            success = false;
            withoutErrors = false;
        }
        disp.setCaretPosition(currentPosition);
        if (success) {
            disp.append("Finished!");
        } else {
            disp.append("Failed!");
        }
        disp.setCaretPosition(disp.getDocument().getLength());
        counter = counter - 1;
    }

    /**
     * Listener for checkbox events.
     *
     * @param e
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == ape ) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                includeApEData = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                includeApEData = true;
            }
        } else if (e.getSource() == unnamed ) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                includeUnnamed = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                includeUnnamed = true;
            }
        } else if (e.getSource() == primers ) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                includePrimers = false;
            } else if (e.getStateChange() == ItemEvent.SELECTED) {
                includePrimers = true;
            }
        }
    }

    /**
     * File filter for selection of GCK files only.
     */
    private class GCKFilter extends FileFilter {
        @Override
        public String getDescription() {
            return "GCK files";
        }
        
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            
            String ext = getExt(f);
            if (ext == null) {
                return false;
            } else if (ext.equalsIgnoreCase("gcc") || ext.equalsIgnoreCase("gcs")) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                displayMainFrame();
            }
        });
    }

    /**
     * Displays the GUI.
     */
    private static void displayMainFrame() {
        JFrame mainFrame = new JFrame("GCK file converter");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        mainFrame.add(new GCK_convert());
        
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    /**
     * Reads the current GCK file into a GCKFile object.
     */
    private void readFile(GCKFile gckFile) throws Exception {
        try {
            GCKFileAnalyzer fileAnalyzer = new GCKFileAnalyzer(gckFile);
            fileAnalyzer.readGCKFile();
        } catch (IOException ex) {
            disp.append("ERROR: Failed to open " + gckFile.getFile().getName() + " for reading.\n");
            logger.severe("Failed to open file for reading");
            throw ex;
        } catch (Exception ex) {  //TODO: Check what kinds of exceptions it can throw.
            throw ex;
        }
    }

    /**
     * Writes the current GCK File into a GeneBank file with the current settings.
     */
    private void writeFile(GCKFile gckFile, File outputFile) throws Exception {
        try {
            GbFileWriter gbFileWriter = new GbFileWriter();
            gbFileWriter.setApEData(includeApEData);
            gbFileWriter.writeGbFile(gckFile, outputFile, parseLevel, library, includeApEData, includeUnnamed, includePrimers);
        } catch (FileNotFoundException ex) {
            disp.append("ERROR: Failed to open " + outputFile.getName() + " for writing.\n");
            logger.severe("Failed to open file for writing.");
            throw ex;
        } catch (Exception ex) { //TODO: Check what kinds of exceptions it can throw.
            throw ex;
        }
    }

    /**
     * Finds the extension of a file and returns it.
     * Basically returns everything after the last dot in the file name.
     *
     * @param f File to examine.
     * @return Extension
     */
    private String getExt(File f) {
        String ext = "";
        String fileName = f.getName();
        int i = fileName.lastIndexOf('.');

        if (i > 0 &&  i < fileName.length() - 1) {
            ext = fileName.substring(i+1);
        }
        return ext;
    }

    /**
     * Finds the name of a file without the extension.
     * Basically removes the extension of the file, returning the rest of the file name. Without the dot.
     *
     * @param f File to examine.
     * @return Name without the extension
     */
    private String getNameWOExt(File f) {
        String name;
        String fullName = f.getName();
        int i = fullName.lastIndexOf('.');
        
        if (i > 0 && i < fullName.length() - 1) {
            name = fullName.substring(0, i);
            return name;
        }
        
        return fullName;
    }

    /**
     * Reads the library file, reporting if it has not been found. Calls the library parser to store the information
     * in the file internally in the library variable.
     */
    private void readLibrary() {
        try {
            libraryFile = new File("DefaultLibrary.lb");
            if (libraryFile.exists()) {
                logger.info("Library found.");
                Scanner libraryReader = new Scanner(libraryFile);
                parseLibrary(libraryReader);
            } else {
                disp.append("No library file found.\n");
                logger.severe("No library found.");
                libraryFile = null;
            }
        } catch (Exception ex) {
            
        }
    }

    /**
     * Parses the library file reading out the values and storing them in the internal library variable.
     *
     * @param reader A scanner attached to the library file to read and parse.
     */
    private void parseLibrary(Scanner reader) {
        String actualValue = null;
        String line;
        while (reader.hasNextLine()) {
            line = reader.nextLine();
            line = line.toLowerCase();
            if (!line.startsWith("//")) {
                //if the line contains a comment we ditch that part.
                //Otherwise we just remove any leading or trailing spaces.
                if (line.contains("//")) {
                    line = line.split("//", 2)[0];
                    line = line.trim();
//                    logger.info("Line found: " + line);
                } else {
                    line = line.trim();
//                    logger.info("Line found: " + line);
                }
                if (line.startsWith("group:")) {
                    actualValue = line.split(":", 2)[1];
                } else if (actualValue != null) {
                    if (line.startsWith("c:")) {
                        line = unEscape(line);
                        line = line.split(":", 2)[1];
                        library.add(new String[] {line, actualValue});                        
                    } else if (line.startsWith("p:")) {
                        line = line.split(":", 2)[1];
                        //line = line.replace("\\" , "\\\\");
                        library.add(new String[] {line, actualValue}); 
                    } else {
                        line = unEscape(line);
                        StringBuilder sb = new StringBuilder("^");
                        sb.append(line);
                        sb.append("$");
                        library.add(new String[] {sb.toString(), actualValue});    
                    }
                }
            }
        }
        for (String[] sa : library) {
            logger.info("Key: " + sa[0] + " ; " + "Value: " + sa[1]);
        }
//        logger.info(library.toString());
    }

    /**
     * Converts a simple string into one that can be parsed as a regular expression.
     *
     * @param line The string to convert.
     * @return The supplied string converted to a regular expression matching the supplied string.
     */
    private String unEscape(String line) {
        line = line.replace("[", "\\[");
        line = line.replace("]", "\\]");
        line = line.replace("}", "\\}");
        line = line.replace("{", "\\{");
        line = line.replace(")", "\\)");
        line = line.replace("(", "\\(");
        line = line.replace(".", "\\.");
        line = line.replace("*", "\\*");
        line = line.replace("+", "\\+");
        line = line.replace("$", "\\$");
        line = line.replace("^", "\\^");
        line = line.replace("?", "\\?");
        return line;
    }

    private File libraryFile;
    private static final Logger logger = Logger.getLogger("GCK_Converter_logger");
//    private static Handler logHandler;
    private File[] fileListToProcess;
    private JButton fileSelect, convert, dirSelect;
    private JCheckBox ape;
    private JCheckBox unnamed;
    private JCheckBox primers;
    private JFileChooser selector, dirSelector;
    private JTextArea disp, dirDisp;
    private GCKFilter filter;
    private String directoryPath;
    private boolean outputDirectorySet;
    private boolean includeApEData;
    private boolean includeUnnamed;
    private boolean includePrimers;
    private RegionParser.ParseLevel parseLevel;
    private LinkedList<String[]> library;

    private int counter;
    private boolean withoutErrors;
}
