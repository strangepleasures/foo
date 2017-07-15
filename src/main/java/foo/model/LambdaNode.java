package foo.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LambdaNode extends Node {
    private final List<ParameterNode> parameters = new ArrayList<>();
    private final List<Node> body = new ArrayList<>();

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.visitLambda(this);
    }
}
