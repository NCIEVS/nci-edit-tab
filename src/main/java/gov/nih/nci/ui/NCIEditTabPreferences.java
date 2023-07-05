package gov.nih.nci.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NCIEditTabPreferences {
	private static final Logger logger = LoggerFactory.getLogger(NCIEditTabPreferences.class);

    private static final int ONTOLOGY_IRI_ATTRIBUTE = 0;
    private static final int INDEX_DIRECTORY_ATTRIBUTE = 1;

    public static final String NCIEDITTAB_PREFERENCES_KEY = "NCIEditTabPreferences";

    public static final String BASE_DIR = "BASE_DIR";
    public static final String INDEX_RECORD_KEYS = "INDEX_RECORD_KEYS";
    

    public static final String PREFIX_INDEX_DIR = "ProtegeIndex";

    public static final String FN_SPLIT = "FnSplit";
    public static final String FN_COPY = "FnCopy";
    public static final String FN_MERGE = "FnMerge";
    public static final String FN_DUALEDITS = "FnDualEdits";
    public static final String FN_RETIRE = "FnRetire";

    public static final String PROTEGE_DIR = ".protege";

    public static final String COLLECTOR_DIR = "nciedittab";

    private static String fileSystemSeparator = System.getProperty("file.separator");

    private static Preferences getPreferences() {
        return PreferencesManager.getInstance().getApplicationPreferences(NCIEDITTAB_PREFERENCES_KEY);
    }

    /**
     * Gets the system temporary directory path.
     *
     * @return Returns the system temporary directory path.
     */
    @Nonnull
    public static String getTempDirectory() {
        return normalizePathName(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Gets the user home directory path.
     *
     * @return Returns the user home directory path.
     */
    @Nonnull
    public static String getNCIEditTabBaseDirectory() {
    	return PROTEGE_DIR + fileSystemSeparator + COLLECTOR_DIR;
    }

    public static boolean getFnSplit() {
        return getPreferences().getBoolean(FN_SPLIT, true);
    }

    public static void setFnSplit(boolean isSelected) {
        getPreferences().putBoolean(FN_SPLIT, isSelected);
    }

    public static boolean getFnCopy() {
        return getPreferences().getBoolean(FN_COPY, true);
    }

    public static void setFnCopy(boolean isSelected) {
        getPreferences().putBoolean(FN_COPY, isSelected);
    }
    
    public static boolean getFnMerge() {
        return getPreferences().getBoolean(FN_MERGE, true);
    }

    public static void setFnMerge(boolean isSelected) {
        getPreferences().putBoolean(FN_MERGE, isSelected);
    }
    
    public static boolean getFnDualEdits() {
        return getPreferences().getBoolean(FN_DUALEDITS, true);
    }

    public static void setFnDualEdits(boolean isSelected) {
        getPreferences().putBoolean(FN_DUALEDITS, isSelected);
    }
    
    public static boolean getFnRetire() {
        return getPreferences().getBoolean(FN_RETIRE, true);
    }

    public static void setFnRetire(boolean isSelected) {
        getPreferences().putBoolean(FN_RETIRE, isSelected);
    }
    
    /**
     * Gets the base directory to store the index files. By default, the base
     * directory is the user home directory.
     *
     * @return Returns the base directory location.
     */
    @Nonnull
    public static String getBaseDirectory() {
        return getPreferences().getString(BASE_DIR, getNCIEditTabBaseDirectory());
    }

    /**
     * Sets the base directory to store the index files.
     *
     * @param baseDirectory
     *          A directory location set by the user.
     */
    public static void setBaseDirectory(@Nonnull String baseDirectory) {
        checkNotNull(baseDirectory);
        getPreferences().putString(BASE_DIR, normalizePathName(baseDirectory));
    }

    /**
     * Adds an index record to the index preference given the input ontology.
     * The record will contain the information about the ontology IRI, the index
     * directory location and the index checksum. This method will do nothing if
     * the input is an anonymous ontology.
     *
     * @param ontology
     *            The input ontology to create the index record
     */
    public static void addIndexRecord(String indexDirId) {

    	final String indexRecordKey = createIndexRecordKey(indexDirId);
    	getPreferences().putStringList(indexRecordKey, createIndexAttributes(indexDirId));
    	collectIndexRecordKey(indexRecordKey);

    }

    /**
     * Removes the index record given the ontology IRI. This method will also
     * remove the index directory from the file system.
     *
     * @param indexDirId
     *          The directory id for the index
     */
    public static void removeIndexRecord(String indexDirId) {
        
        final String indexRecordKey = createIndexRecordKey(indexDirId);
        deleteIndexDirectory(indexRecordKey);
        getPreferences().putString(indexRecordKey, null);
        discardIndexRecordKey(indexRecordKey);
    }

    /**
     * Checks if the index preference contains the index record for the given
     * input ontology. When the method finds an index record associated with
     * the input ontology, it will check if the index directory location is
     * still valid. The method will check as well if the index checksum agrees
     * with the input ontology such that the index record is still valid.
     * 
     * This method will always return <code>false</code> if the input is an
     * anonymous ontology.
     *
     * @param indexDirId
     *          The unique id that neames the directory where the index is stored
     * @return Returns <code>true</code> if the index record is still relevant
     * with the given input ontology, or <code>false</code> otherwise.
     */
    public static boolean containsIndexRecord(String indexDirId) {
        
        boolean doesContain = true;
        String indexRecordKey = createIndexRecordKey(indexDirId);
        Optional<List<String>> indexRecord = getIndexRecord(indexRecordKey);
        if (indexRecord.isPresent()) {
            String directoryLocation = getIndexDirectoryLocation(indexDirId);
            if (doesDirectoryExist(directoryLocation)) {
            	
            } else {
                doesContain = false;
            }
        } else {
            doesContain = false;
        }
        return doesContain;
    }

    private static boolean doesDirectoryExist(String directoryLocation) {
        if (directoryLocation == null) {
            return false;
        }
        if (directoryLocation.isEmpty()) {
            return false;
        }
        return new File(directoryLocation).exists();
    }

    /**
     * Returns the index record table. The table consists of the record key, the ontology IRI,
     * the index directory location and the index checksum.
     *
     * @return A index record table.
     */
    @Nonnull
    public static List<List<String>> getIndexRecordTable() {
        List<List<String>> indexRecordTable = new ArrayList<>();
        List<String> indexMapKeys = getPreferences().getStringList(INDEX_RECORD_KEYS, new ArrayList<>());
        for (String indexMapKey : indexMapKeys) {
            List<String> indexRecord = new ArrayList<>();
            indexRecord.add(indexMapKey);
            indexRecord.addAll(getCheckedIndexRecord(indexMapKey));
            indexRecordTable.add(indexRecord);
        }
        return indexRecordTable;
    }

    /**
     * Gets the index directory path given the input ontology IRI.
     *
     * @param indexDirId
     *          The unique integer id that identifies the index directory path.
     * @return A full path location of the index directory
     */
    @Nonnull
    public static String getIndexDirectoryLocation(String indexDirId) {
        String indexMapKey = createIndexRecordKey(indexDirId);
        List<String> indexAttributes = getCheckedIndexRecord(indexMapKey);
        return indexAttributes.get(INDEX_DIRECTORY_ATTRIBUTE);
    }

    /**
     * Clears all ontology versions and the associated index location from the preference.
     * Note that no physical files or folders are deleted from the file system.
     */
    public static void clear() {
        Preferences preferences = getPreferences();
        preferences.clear();
        preferences.putString(BASE_DIR, getBaseDirectory());
    }

    /*
     * Private utility methods
     */

    private static Optional<List<String>> getIndexRecord(String indexRecordKey) {
        return Optional.ofNullable(getPreferences().getStringList(indexRecordKey, null));
    }

    private static List<String> getCheckedIndexRecord(String indexRecordKey) {
        Optional<List<String>> uncheckedIndexRecord = getIndexRecord(indexRecordKey);
        List<String> indexRecord = new ArrayList<>();
        if (!uncheckedIndexRecord.isPresent()) {
            indexRecord.add(ONTOLOGY_IRI_ATTRIBUTE, "");
            indexRecord.add(INDEX_DIRECTORY_ATTRIBUTE, "");
        } else {
            indexRecord.addAll(uncheckedIndexRecord.get());
        }
        return indexRecord;
    }

    private static String createIndexRecordKey(String id) {
        return "KEY:" + id;
    }

    private static List<String> createIndexAttributes(String indexDirId) {
        List<String> indexAttributes = new ArrayList<>();
        indexAttributes.add(ONTOLOGY_IRI_ATTRIBUTE, indexDirId);
        indexAttributes.add(INDEX_DIRECTORY_ATTRIBUTE, createIndexDirectoryLocation(indexDirId));
        return indexAttributes;
    }

    private static String fetchOntologyIri(OWLOntology ontology) {
        return ontology.getOntologyID().getOntologyIRI().get().toString();
    }

    private static String createIndexDirectoryLocation(String indexDirId) {
        StringBuffer directoryLocation = new StringBuffer();
        directoryLocation.append(getBaseDirectory());
        directoryLocation.append(fileSystemSeparator);
        directoryLocation.append(createDirectoryName(indexDirId));
        return directoryLocation.toString();
    }

    private static void collectIndexRecordKey(String indexMapKey) {
        List<String> indexRecordKeys = getPreferences().getStringList(
                INDEX_RECORD_KEYS,
                new ArrayList<>());
        if (!indexRecordKeys.contains(indexMapKey)) { // to prevent duplication in the list
            indexRecordKeys.add(indexMapKey);
        }
        getPreferences().putStringList(INDEX_RECORD_KEYS, indexRecordKeys);
    }

    private static void discardIndexRecordKey(String indexMapKey) {
        List<String> indexMapKeys = getPreferences().getStringList(
                INDEX_RECORD_KEYS,
                new ArrayList<>());
        indexMapKeys.remove(indexMapKey);
        getPreferences().putStringList(INDEX_RECORD_KEYS, indexMapKeys);
    }

    private static String createDirectoryName(String indexDirId) {
        String ontologyIdHex = indexDirId;
        String timestampHex = Integer.toHexString(new Date().hashCode());
        return String.format("%s-%s-%s", PREFIX_INDEX_DIR, ontologyIdHex, timestampHex);
    }

    private static void deleteIndexDirectory(String indexMapKey) {
        List<String> indexAttributes = getCheckedIndexRecord(indexMapKey);
        String directoryLocation = indexAttributes.get(INDEX_DIRECTORY_ATTRIBUTE);
        try {
            File indexDirectory = new File(directoryLocation);
            if (indexDirectory.exists()) {
                FileUtils.deleteDirectory(indexDirectory);
            }
        } catch (IOException e) {
            logger.error("Error while removing index directory: " + directoryLocation, e);
        }
    }

    /**
     * Make sure the path directory contains no file separator at the end of the string
     *
     * @param directory
     *          input path directory
     * @return clean path directory.
     */
    private static String normalizePathName(String directory) {
        if (directory.endsWith(fileSystemSeparator)) {
            directory = directory.substring(0, directory.length()-1);
        }
        return directory;
    }

}
