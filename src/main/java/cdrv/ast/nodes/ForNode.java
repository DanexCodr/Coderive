package cdrv.ast.nodes;

import java.util.*;

public class ForNode extends StatementNode {
    public String iterator;
    public RangeNode range;
    public BlockNode body = new BlockNode();

    public ForNode() {}

    public ForNode(String iterator, RangeNode range) {
        this.iterator = iterator;
        this.range = range;
    }

}
