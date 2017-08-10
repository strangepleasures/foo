package ludwig.utils;

import ludwig.model.*;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NodeUtils {

    public static Object parseLiteral(String s) {
        switch (s) {
            case "true":
                return true;
            case "false":
                return false;
            case "null":
                return null;
            default:
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e1) {
                    try {
                        return Double.parseDouble(s);
                    } catch (NumberFormatException e2) {
                        return StringEscapeUtils.unescapeJavaScript(s.substring(1, s.length() - 1));
                    }
                }
        }
    }

    public static String formatLiteral(Object o) {
        if (o instanceof String) {
            return '\'' + StringEscapeUtils.escapeJavaScript(o.toString()) + '\'';
        }
        return String.valueOf(o);
    }

    public static List<Node> expandNode(Node node) {
        List<Node> nodes = new ArrayList<>();
        expandNode(node, true, nodes);
        return nodes;
    }

    private static void expandNode(Node<?> node, boolean onlyChildren, List<Node> nodes) {
        if (!onlyChildren) {
            nodes.add(node);
        }
        for (Node child: node.children()) {
            expandNode(child, false, nodes);
        }
    }

    public static String signature(NamedNode node) {
        if (node instanceof ArgumentList) {
            return ((ArgumentList) node).arguments().stream().collect(Collectors.joining(" ", node.getName() + " ", ""));
        }
        return node.getName();
    }
}
