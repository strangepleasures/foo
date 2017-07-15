package foo.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IfNode extends Node implements ListLike {
    private List<Node> items;

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return null;
    }
}
