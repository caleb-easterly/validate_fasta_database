package edu.umn.galaxyp;

//import com.compomics.util.protein.Header;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

//import org.apache.log4j.*;

public class ValidateFasta {

    private static Logger logger = Logger.getLogger(ValidateFasta.class.getName());

    public static void main(String[] args) {

        // input path
        Path fastaPath = Paths.get(args[0]);

        // if true, the presence of any invalid sequences triggers an exit code of 1
        boolean crash_if_invalid = false;

        // optional boolean
        if (args.length == 4){
            crash_if_invalid = Boolean.valueOf(args[3]);
        }

        // load fasta file
        FASTA fasta = new FASTA(fastaPath, crash_if_invalid);

        // performs filtering, I/O, and returns a count of good and bad sequences
        fasta.writeFilteredFastaToFile(Paths.get(args[1]), Paths.get(args[2]));

        MultiSet<Header.DatabaseType> databaseTypes = fasta.getDatabaseTypes();
        System.out.println("Database Types");
        System.out.println(databaseTypes.toString());
    }
}