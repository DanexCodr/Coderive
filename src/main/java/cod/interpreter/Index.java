package cod.interpreter;

import cod.error.ProgramError;
import cod.lexer.*;
import static cod.lexer.TokenType.*;
import static cod.syntax.Symbol.*;
import static cod.syntax.Keyword.*;

import java.io.*;
import java.util.*;

/**
 * Index file for Coderive units.
 * Stores classname → filename mappings for O(1) import resolution.
 * 
 * File format: {projectRoot}/src/idx/{unit}.toml
 * 
 * Example:
 * # unit sample
 * timestamp = "1700000000000"
 * generator = "Coderive 1.0"
 * 
 * [classes]
 * Imported = "Imported.cod"
 * Helper = "Imported.cod"
 * Database = "Core.cod"
 * 
 * @since 1.0
 */
public final class Index {
    
    private static final String IDX_DIR_NAME = "idx";
    private static final String SRC_DIR_NAME = "src";
    private static final String FILE_EXTENSION = ".toml";
    private static final String CLASSES_SECTION = "classes";
    private static final String DEFAULT_GENERATOR = "Coderive 1.0";
    
    private final String unit;
    private long timestamp;
    private String generator;
    private final Map<String, String> classes;
    
    // Project root - set once when we have srcMainRoot
    private static String projectRoot;
    
    /**
     * Sets the project root directory (where src/ is located).
     * This must be called before using load/save.
     * 
     * @param srcMainRoot the src/main/ root path
     */
    public static void setProjectRoot(String srcMainRoot) {
        if (srcMainRoot == null || srcMainRoot.isEmpty()) {
            return;
        }
        
        try {
            File srcMain = new File(srcMainRoot);
            File src = srcMain.getParentFile();
            
            if (src != null && SRC_DIR_NAME.equals(src.getName())) {
                projectRoot = src.getParentFile().getAbsolutePath();
            } else {
                File parent = srcMain.getParentFile();
                if (parent != null) {
                    projectRoot = parent.getParentFile().getAbsolutePath();
                } else {
                    projectRoot = srcMain.getAbsolutePath();
                }
            }
            
            System.err.println("[INDEX] Project root set to: " + projectRoot);
        } catch (Exception e) {
            System.err.println("[INDEX] Failed to set project root: " + e.getMessage());
        }
    }
    
    /**
     * Gets the project root directory.
     */
    public static String getProjectRoot() {
        return projectRoot;
    }
    
    /**
     * Gets the index file path for a unit.
     */
    private static File getIndexFile(String unitName) {
        if (projectRoot == null) {
            return new File("src/" + IDX_DIR_NAME + "/" + unitName + FILE_EXTENSION);
        }
        return new File(projectRoot + File.separator + SRC_DIR_NAME + 
                        File.separator + IDX_DIR_NAME + File.separator + unitName + FILE_EXTENSION);
    }
    
    /**
     * Creates a new index for the specified unit.
     * 
     * @param unit the unit name (cannot be null or empty)
     * @throws IllegalArgumentException if unit is null or empty
     */
    public Index(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit name cannot be null or empty");
        }
        this.unit = unit;
        this.classes = new HashMap<String, String>();
        this.timestamp = System.currentTimeMillis();
        this.generator = DEFAULT_GENERATOR;
    }
    
    /**
     * Creates a new index with explicit timestamp and generator.
     * 
     * @param unit the unit name
     * @param timestamp the timestamp
     * @param generator the generator string
     */
    public Index(String unit, long timestamp, String generator) {
        this(unit);
        this.timestamp = timestamp;
        if (generator != null && !generator.isEmpty()) {
            this.generator = generator;
        }
    }
    
    /**
     * Loads an index from disk.
     * 
     * @param unitName the unit name
     * @return the loaded index, or null if not found or invalid
     */
    public static Index load(String unitName) {
        if (unitName == null || unitName.trim().isEmpty()) {
            return null;
        }
        
        File file = getIndexFile(unitName);
        if (!file.exists()) {
            return null;
        }
        
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            Index index = new Index(unitName);
            String currentSection = "";
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    continue;
                }
                
                int eq = line.indexOf('=');
                if (eq == -1) {
                    continue;
                }
                
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                if (currentSection.isEmpty()) {
                    if ("timestamp".equals(key)) {
                        try {
                            index.timestamp = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            // Keep existing timestamp
                        }
                    } else if ("generator".equals(key)) {
                        index.generator = value;
                    }
                } else if (CLASSES_SECTION.equals(currentSection)) {
                    index.classes.put(key, value);
                }
            }
            
            return index;
            
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Saves this index to disk.
     * 
     * @return true if saved successfully, false otherwise
     */
    public boolean save() {
        File file = getIndexFile(unit);
        
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                return false;
            }
        }
        
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file));
            
            writer.println("# unit " + unit);
            writer.println("timestamp = \"" + timestamp + "\"");
            writer.println("generator = \"" + generator + "\"");
            writer.println();
            writer.println("[" + CLASSES_SECTION + "]");
            
            List<String> sorted = new ArrayList<String>(classes.keySet());
            Collections.sort(sorted);
            
            for (String className : sorted) {
                String fileName = classes.get(className);
                writer.println(className + " = \"" + fileName + "\"");
            }
            
            writer.flush();
            return true;
            
        } catch (IOException e) {
            return false;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    /**
     * Adds a class mapping.
     */
    public Index add(String className, String fileName) {
        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        classes.put(className, fileName);
        return this;
    }
    
    /**
     * Adds multiple class mappings from a map.
     */
    public Index addAll(Map<String, String> mappings) {
        if (mappings != null) {
            classes.putAll(mappings);
        }
        return this;
    }
    
    /**
     * Gets the file name for a class.
     */
    public String getFile(String className) {
        if (className == null) {
            return null;
        }
        return classes.get(className);
    }
    
    /**
     * Checks if a class exists in this index.
     */
    public boolean contains(String className) {
        if (className == null) {
            return false;
        }
        return classes.containsKey(className);
    }
    
    /**
     * Removes a class from the index.
     */
    public boolean remove(String className) {
        if (className == null) {
            return false;
        }
        return classes.remove(className) != null;
    }
    
    /**
     * Returns all class names in this index.
     */
    public Set<String> getClassNames() {
        return Collections.unmodifiableSet(new HashSet<String>(classes.keySet()));
    }
    
    /**
     * Returns the number of classes in this index.
     */
    public int size() {
        return classes.size();
    }
    
    /**
     * Checks if this index is empty.
     */
    public boolean isEmpty() {
        return classes.isEmpty();
    }
    
    /**
     * Clears all class mappings from this index.
     */
    public void clear() {
        classes.clear();
        timestamp = System.currentTimeMillis();
    }
    
    /**
     * Updates the timestamp to the current time.
     */
    public void touch() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Checks if this index is stale relative to the unit directory.
     */
    public boolean isStale(String unitPath) {
        if (unitPath == null || unitPath.isEmpty()) {
            return true;
        }
        
        File dir = new File(unitPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return true;
        }
        
        // Check if all indexed files still exist
        for (String fileName : classes.values()) {
            File classFile = new File(dir, fileName);
            if (!classFile.exists()) {
                return true;
            }
        }
        
        long lastModified = 0;
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cod");
            }
        });
        
        if (files != null) {
            for (File file : files) {
                long mod = file.lastModified();
                if (mod > lastModified) {
                    lastModified = mod;
                }
            }
        }
        
        // Check if number of files changed
        if (files != null && files.length != classes.size()) {
            return true;
        }
        
        return timestamp < lastModified;
    }
    
    /**
     * Extracts class names from a .cod file using the lexer.
     */
    private static List<String> extractClassNames(File file) {
        List<String> classNames = new ArrayList<String>();
        
        try {
            String content = readFileToString(file);
            MainLexer lexer = new MainLexer(content);
            List<Token> tokens = lexer.tokenize();
            
            int braceDepth = 0;
            
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                
                // Track brace depth
                if (t.isSymbol(LBRACE)) {
                    braceDepth++;
                } else if (t.isSymbol(RBRACE)) {
                    braceDepth--;
                }
                
                // Look for class definitions at depth 0
                if (braceDepth == 0 && t.type == ID) {
                    // Look ahead for {
                    int nextPos = i + 1;
                    // Skip whitespace
                    while (nextPos < tokens.size() && 
                           tokens.get(nextPos).type == WS) {
                        nextPos++;
                    }
                    
                    // Also skip visibility modifiers (share, local)
                    while (nextPos < tokens.size() && 
                           (tokens.get(nextPos).isKeyword(SHARE) || tokens.get(nextPos).isKeyword(LOCAL))) {
                        nextPos++;
                        // Skip whitespace after modifier
                        while (nextPos < tokens.size() && 
                               tokens.get(nextPos).type == WS) {
                            nextPos++;
                        }
                    }
                    
                    // Check if next token is {
                    if (nextPos < tokens.size() && 
                        tokens.get(nextPos).isSymbol(LBRACE)) {
                        String className = t.getText();
                        
                        // Class names start with uppercase
                        if (!className.isEmpty() && 
                            Character.isUpperCase(className.charAt(0))) {
                            if (!classNames.contains(className)) {
                                classNames.add(className);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Fallback to filename
            String fileName = file.getName();
            String fallback = fileName.substring(0, fileName.length() - 4);
            classNames.add(fallback);
        }
        
        return classNames;
    }
    
    /**
     * Refreshes this index by scanning the unit directory.
     * Validates that no duplicate class names exist within the unit.
     * 
     * @return true if the index was updated, false otherwise
     * @throws IllegalStateException if duplicate class names are found
     */
    public boolean refresh(String unitPath) {
        if (unitPath == null || unitPath.isEmpty()) {
            return false;
        }
        
        File dir = new File(unitPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        
        Map<String, String> newMappings = new HashMap<String, String>();
        Map<String, List<String>> duplicateTracker = new HashMap<String, List<String>>();
        
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cod");
            }
        });
        
        if (files == null) {
            return false;
        }
        
        // First pass: collect all classes and detect duplicates
        for (File file : files) {
            String fileName = file.getName();
            
            // Skip index file itself
            String baseName = fileName.substring(0, fileName.length() - 4);
            if ("index".equals(baseName)) {
                continue;
            }
            
            // Extract class names using lexer
            List<String> classNames = extractClassNames(file);
            
            for (String className : classNames) {
                if (newMappings.containsKey(className)) {
                    // Duplicate found!
                    List<String> duplicates = duplicateTracker.get(className);
                    if (duplicates == null) {
                        duplicates = new ArrayList<String>();
                        duplicates.add(newMappings.get(className));
                        duplicateTracker.put(className, duplicates);
                    }
                    duplicates.add(fileName);
                } else {
                    newMappings.put(className, fileName);
                }
            }
        }
        
        // If duplicates found, throw error with details
        if (!duplicateTracker.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Duplicate class names found in unit '").append(unit).append("':\n");
            errorMsg.append("Each class name must be unique within a unit.\n\n");
            
            for (Map.Entry<String, List<String>> entry : duplicateTracker.entrySet()) {
                String className = entry.getKey();
                List<String> filesWithClass = entry.getValue();
                errorMsg.append("  Class '").append(className).append("' appears in:\n");
                for (String fileName : filesWithClass) {
                    errorMsg.append("    - ").append(fileName).append("\n");
                }
            }
            
            errorMsg.append("\nRename or remove duplicate classes to continue.");
            
            throw new ProgramError(errorMsg.toString());
        }
        
        if (newMappings.isEmpty()) {
            return false;
        }
        
        classes.clear();
        classes.putAll(newMappings);
        timestamp = System.currentTimeMillis();
        
        return true;
    }
    
    // ========== Getters ==========
    
    public String getUnit() {
        return unit;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getGenerator() {
        return generator;
    }
    
    public Map<String, String> getMappings() {
        return Collections.unmodifiableMap(classes);
    }
    
    // ========== Private Helpers ==========
    
    private static String readFileToString(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } finally {
            reader.close();
        }
        return content.toString();
    }
    
    // ========== Object Methods ==========
    
    @Override
    public String toString() {
        return String.format("Index{unit='%s', classes=%d, timestamp=%d}", 
                             unit, classes.size(), timestamp);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Index index = (Index) o;
        
        if (timestamp != index.timestamp) return false;
        if (!unit.equals(index.unit)) return false;
        if (!generator.equals(index.generator)) return false;
        return classes.equals(index.classes);
    }
    
    @Override
    public int hashCode() {
        int result = unit.hashCode();
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + generator.hashCode();
        result = 31 * result + classes.hashCode();
        return result;
    }
    
    // ========== Builder ==========
    
    public static final class Builder {
        private final String unit;
        private long timestamp;
        private String generator;
        private final Map<String, String> mappings;
        
        public Builder(String unit) {
            this.unit = unit;
            this.timestamp = System.currentTimeMillis();
            this.generator = DEFAULT_GENERATOR;
            this.mappings = new HashMap<String, String>();
        }
        
        public Builder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder withGenerator(String generator) {
            this.generator = generator;
            return this;
        }
        
        public Builder add(String className, String fileName) {
            this.mappings.put(className, fileName);
            return this;
        }
        
        public Builder addAll(Map<String, String> mappings) {
            this.mappings.putAll(mappings);
            return this;
        }
        
        public Index build() {
            Index index = new Index(unit, timestamp, generator);
            index.addAll(mappings);
            return index;
        }
    }
}