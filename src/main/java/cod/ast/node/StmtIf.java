package cod.ast.node;

import cod.ast.VisitorImpl;

public class StmtIf extends Stmt {
    public Expr condition;
    public Block thenBlock = new Block();
    public Block elseBlock = new Block();

    public StmtIf() {}
    
    public StmtIf(Expr condition) {
        this.condition = condition;
    }
    
               @Override
        public final <T> T accept(VisitorImpl<T> visitor) {
           return visitor.visit(this);
        }
    
}