package cn.iocoder.yudao.module.dcc.service.projectcode.folder;

import java.util.*;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.*;

/** key 是模板内稳定键；生成时改为项目自己的数据库目录 ID。 */
public record DccFolderTemplateStructure(List<Node> nodes) {
    public record Node(String key, String parentKey, String name, Integer sortOrder) {}
    public DccFolderTemplateStructure {
        if (nodes != null) nodes = List.copyOf(nodes);
    }
    public DccFolderTemplateStructure validated() {
        if (nodes == null || nodes.isEmpty() || nodes.size() > 500) throw fail(TEMPLATE_INVALID);
        Map<String, Node> byKey = new LinkedHashMap<>();
        Set<List<String>> siblingNames = new HashSet<>();
        for (Node node : nodes) {
            if (node == null || node.key() == null || node.key().isBlank() || node.key().length() > 64
                    || node.name() == null || node.name().isBlank() || node.name().length() > 128
                    || node.sortOrder() == null || node.sortOrder() < 0
                    || (node.parentKey() != null && node.parentKey().isBlank())
                    || byKey.put(node.key(), node) != null
                    || !siblingNames.add(Arrays.asList(node.parentKey(), node.name().trim()))) throw fail(TEMPLATE_INVALID);
        }
        for (Node node : nodes) {
            Set<String> visited = new HashSet<>();
            Node current = node;
            while (current != null) {
                if (!visited.add(current.key())) throw fail(TEMPLATE_INVALID);
                String parent = current.parentKey();
                if (parent != null && !byKey.containsKey(parent)) throw fail(TEMPLATE_INVALID);
                current = parent == null ? null : byKey.get(parent);
            }
        }
        return this;
    }
    public List<Node> parentFirst() {
        validated();
        List<Node> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        while (result.size() < nodes.size()) {
            nodes.stream().sorted(Comparator.comparing(Node::sortOrder).thenComparing(Node::key))
                    .filter(node -> !added.contains(node.key())
                            && (node.parentKey() == null || added.contains(node.parentKey())))
                    .forEach(node -> { result.add(node); added.add(node.key()); });
        }
        return List.copyOf(result);
    }
}
