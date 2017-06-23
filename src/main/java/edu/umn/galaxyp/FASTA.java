package edu.umn.galaxyp;

//import com.compomics.util.protein.Header;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * The main data contained in FASTA is a HashMap of FASTA headers (in String form) and sequences. *
 *
 * @author caleb
 *
 *
 *
 *
 */
public class FASTA {
    private static final Logger logger = Logger.getLogger(FASTA.class.getName());

    // map of all headers (key) with sequences (value)
    private Map<String, String> fastaHeaderMap;

    // map of FASTA entries that caused Header.parseFromFasta() to fail
    private Map<String, String> badFastaHeaderMap;

    // map of FASTA entries that passed Header.parseFromFasta()
    private Map<String, String> goodFastaHeaderMap;

    // Multiset of databases (i.e., UniProt) assigned by Header.parseFromFasta()
    private MultiSet<Header.DatabaseType> databaseTypes;

    /**
     * constructor that takes the path of the FASTA file
     *
     *  @param path  path to FASTA file to be read in(NIO Path object)
     *  @param crash_if_invalid  if true, a badly formatted header (the first) will immediately cause a System.exit(1)
     */
    public FASTA(Path path, boolean crash_if_invalid){
        // initialize variables
        goodFastaHeaderMap = new LinkedHashMap<>();
        badFastaHeaderMap = new LinkedHashMap<>();
        databaseTypes = new MultiSet<>();
        // LinkedHashMap will (probably) result in the same ordering of the FASTA file
        fastaHeaderMap = new LinkedHashMap<>();
        StringBuilder sequence = new StringBuilder(); // allows us to append all sequences of line

        // automatically closes reader if IOException
        try (BufferedReader br = Files.newBufferedReader(path)){
            String line = br.readLine();

            // while there are still lines in the file
            while (line != null) {
                // indicates FASTA header line
                if (line.startsWith(">")){
                    String header = line + "\n";
//                    logger.info("Header: " + header);

                    // while there are still lines in the file and the next line is not a header
                    while ((line = br.readLine()) != null && !line.startsWith(">")){
                        sequence.append(line).append("\n");
                    }

                    // add header and sequence to HashMap
                    fastaHeaderMap.put(header, sequence.toString());

                    // empty the sequence builder to allow for appending the next sequence
                    sequence.setLength(0);
                }
            }

        } catch(IOException e) {
            logger.severe("FASTA file not found: " + e.toString());
        }

        // fill good and bad Maps
        sortFastaByHeader(crash_if_invalid);
    }

    // GETTERS AND SETTERS
    public Map<String, String> getFastaHeaderMap() {
        return fastaHeaderMap;
    }

    public void setFastaHeaderMap(Map<String, String> fastaHeaderMap) {
        this.fastaHeaderMap = fastaHeaderMap;
    }

    public MultiSet<Header.DatabaseType> getDatabaseTypes(){
        return this.databaseTypes;
    }

    public Map<String, String> getBadFastaHeaderMap() {
        return badFastaHeaderMap;
    }

    public Map<String, String> getGoodFastaHeaderMap() {
        return goodFastaHeaderMap;
    }

    /**
     * Sorts loaded FASTA database into well- and poorly- formatted entries,
     * which are stored in this.goodFastaHeaderMap and this.badFastaHeaderMap, respectively.
     *
     * @param crash_if_invalid  if yes, a poorly formatted header immediately triggers a System.exit(1)
     */
    public void sortFastaByHeader(boolean crash_if_invalid){
        Iterator fastaMapIterator = this.fastaHeaderMap.entrySet().iterator();
        while (fastaMapIterator.hasNext()) {
            Map.Entry pair = (Map.Entry) fastaMapIterator.next();
            String header = (String) pair.getKey();
            String sequence = (String) pair.getValue();
            if (isValidFastaHeader(header, crash_if_invalid)) {
                this.goodFastaHeaderMap.put(header, sequence);
            } else {
                this.badFastaHeaderMap.put(header,sequence);
            }
        }
    }

    /** write out good and bad FASTA sequences into different text files
     *
     * @param goodFASTA  an NIO Path object for the FASTA entries that are well formatted
     * @param badFASTA  Path object for poorly formatted FASTA entries
     */
    public void writeFilteredFastaToFile(Path goodFASTA, Path badFASTA){
        try (BufferedWriter bwGood = Files.newBufferedWriter(goodFASTA);
             BufferedWriter bwBad = Files.newBufferedWriter(badFASTA)){
                for (Map.Entry<String, String> entry : goodFastaHeaderMap.entrySet()){
                    String header = entry.getKey();
                    String sequence = entry.getValue();
                    bwGood.write(header, 0, header.length());
                    bwGood.write(sequence, 0, sequence.length());
                }
                for (Map.Entry<String, String> entry : badFastaHeaderMap.entrySet()){
                    String header = entry.getKey();
                    String sequence = entry.getValue();
                    bwBad.write(header, 0, header.length());
                    bwBad.write(sequence, 0, sequence.length());
                    }
            } catch(IOException e) {
                     e.printStackTrace();
        }
    }

    /***
     *
     *hide system output from compomics methods
     *(for very large FASTA, compomics output may overflow
     *standard output in Galaxy)
     *idea from https://stackoverflow.com/questions/8363493/hiding-system-out-print-calls-of-a-class
    */
    private static PrintStream originalStream = System.out;
    private static PrintStream dummyStream = new PrintStream(new OutputStream() {
        @Override
        public void write(int i) throws IOException {
        }
    });

    /**
     * checks FASTA header for validity, using compomics method `Header.parseFromFASTA()`
     *
     * @param aFastaHeader  the first line of a FASTA entry (starts with ``>``)
     * @param crash_if_invalid  if true, a poorly formatted FASTA header (the first) causes an immediate System.exit(1)
     * @return  boolean true if header is valid, false otherwise
     */

    private boolean isValidFastaHeader(String aFastaHeader, boolean crash_if_invalid){
        Header header = null;
        try {


            // attempt to parse the header
            // set out to dummy stream for Header method
            System.setOut(dummyStream);
            header = Header.parseFromFASTA(aFastaHeader);
            Header.DatabaseType dbType = header.getDatabaseType();
            databaseTypes.add(dbType);
            // return to regular system out
            System.setOut(originalStream);
        } catch(IllegalArgumentException iae){
            // return exit code of 1 (abnormal termination)
            if (crash_if_invalid) {
                System.err.println("Invalid FASTA headers detected. Exit requested by user. ");
                System.exit(1);
            }
            // else, print nothing
            iae.getMessage();
        }
        if (header == null){
            return false;
        } else {
            return true;
        }
    }
}