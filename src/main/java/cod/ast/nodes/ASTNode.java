// ASTNode.java
package cod.ast.nodes;

import cod.ast.ASTVisitor;

public abstract class ASTNode {

    public abstract <T> T accept(ASTVisitor<T> visitor);

}