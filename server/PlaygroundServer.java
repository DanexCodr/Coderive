// PlaygroundServer.java
import static spark.Spark.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class PlaygroundServer {
    public static void main(String[] args) {
        port(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));
        enableCORS();
        
        System.out.println("🚀 Coderive API Server Starting...");
        
        // Load YOUR Coderive.jar from GitHub Pages!
        try {
            // Download the JAR from GitHub Pages
            URL jarUrl = new URL("https://danexcodr.github.io/Coderive/assets/Coderive.jar");
            File jarFile = new File("Coderive.jar");
            
            System.out.println("📥 Downloading Coderive.jar from GitHub Pages...");
            try (InputStream in = jarUrl.openStream()) {
                Files.copy(in, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("✅ Coderive.jar downloaded!");
            
            // Load the JAR
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarFile.toURI().toURL()},
                Thread.currentThread().getContextClassLoader()
            );
            
            // Find your main interpreter class
            Class<?> interpreterClass = classLoader.loadClass("cod.interpreter.Interpreter");
            Method runMethod = interpreterClass.getMethod("run", String.class);
            
            get("/", (req, res) -> "Coderive API is live! v0.7.0");
            
            post("/run", (req, res) -> {
                String code = req.queryParams("code");
                if (code == null || code.isEmpty()) {
                    return "Please provide code!";
                }
                
                // Capture output
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                PrintStream oldErr = System.err;
                
                System.setOut(ps);
                System.setErr(ps);
                
                try {
                    long start = System.nanoTime();
                    
                    // RUN YOUR ACTUAL INTERPRETER!
                    Object result = runMethod.invoke(null, code);
                    
                    long end = System.nanoTime();
                    String output = baos.toString();
                    
                    return output + "\n⏱️ Time: " + ((end - start) / 1_000_000.0) + " ms";
                    
                } catch (Exception e) {
                    e.printStackTrace(ps);
                    return "Error: " + e.getMessage();
                } finally {
                    System.setOut(oldOut);
                    System.setErr(oldErr);
                }
            });
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load Coderive.jar: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to echo mode
            post("/run", (req, res) -> {
                String code = req.queryParams("code");
                return "⚠️ Running in fallback mode. Coderive.jar not loaded.\n\nYou wrote:\n" + code;
            });
        }
        
        System.out.println("✅ Server ready on port " + port());
    }
    
    private static void enableCORS() {
        options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            return "OK";
        });
        
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
        });
    }
}
