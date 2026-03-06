import static spark.Spark.*;
import cod.runner.REPLRunner;
import java.io.*;

public class PlaygroundServer {
    public static void main(String[] args) {
        port(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));
        enableCORS();
        
        get("/", (req, res) -> "Coderive REPL Server");
        
        post("/eval", (req, res) -> {
            String code = req.queryParams("code");
            if (code == null || code.isEmpty()) {
                return "No code provided";
            }
            return REPLRunner.eval(code);
        });
    }
    
    private static void enableCORS() {
        options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "POST, OPTIONS");
            return "OK";
        });
        
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
        });
    }
}
