// ASTNode.java
package cod.ast.nodes;

import cod.ast.VisitorImpl;

public abstract class ASTNode {

    public abstract <T> T accept(VisitorImpl<T> visitor);

}