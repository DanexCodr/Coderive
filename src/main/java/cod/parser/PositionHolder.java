package cod.parser;

/**
 * Simple holder for shared position across parsers.
 * This allows multiple parser instances to share and update 
 * the same position in the token stream.
 */
public class PositionHolder {
    public int value;
    
    public PositionHolder(int initial) {
        this.value = initial;
    }
    
    public void up() {
        value++;
    }
    
    public int get() {
        return value;
    }
    
    public void set(int newValue) {
        value = newValue;
    }
}