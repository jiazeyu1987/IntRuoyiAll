package cn.iocoder.yudao.module.dcc.service.directory;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.service.category.DccIntAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID;

@Component
@RequiredArgsConstructor
public class DccIntAuthDirectoryClientImpl implements DccIntAuthDirectoryClient {

    private static final String BASELINE_DIRECTORY_SETTING_KEY = "baseline_directory_id";

    private final DccIntAuthProperties properties;

    @Override
    public List<IntAuthDirectoryNode> listBaselineDirectories() {
        properties.validateDirectoryImportConfig();
        Path dbPath = Path.of(properties.getDbPath().trim());
        if (!Files.isRegularFile(dbPath)) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, dbPath.toString());
        }

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String baselineDirectoryId = loadBaselineDirectoryId(connection);
            Map<String, RawDirectoryNode> nodesById = loadNodes(connection);
            RawDirectoryNode baselineNode = nodesById.get(baselineDirectoryId);
            if (baselineNode == null) {
                throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, baselineDirectoryId);
            }
            Map<String, List<RawDirectoryNode>> childrenByParentId = buildChildrenByParentId(nodesById.values());
            List<IntAuthDirectoryNode> result = new ArrayList<>();
            appendSubtree(result, childrenByParentId, baselineNode, null, new HashSet<>());
            return result;
        } catch (SQLException ex) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, ex.getMessage());
        }
    }

    private String loadBaselineDirectoryId(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT setting_value
                FROM operation_approval_settings
                WHERE setting_key = ?
                """)) {
            statement.setString(1, BASELINE_DIRECTORY_SETTING_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, BASELINE_DIRECTORY_SETTING_KEY);
                }
                String baselineDirectoryId = StrUtil.trimToEmpty(resultSet.getString("setting_value"));
                if (StrUtil.isBlank(baselineDirectoryId)) {
                    throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, BASELINE_DIRECTORY_SETTING_KEY);
                }
                return baselineDirectoryId;
            }
        }
    }

    private Map<String, RawDirectoryNode> loadNodes(Connection connection) throws SQLException {
        Map<String, RawDirectoryNode> nodesById = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT node_id, name, parent_id, created_at_ms, updated_at_ms
                     FROM kb_directory_nodes
                     """)) {
            while (resultSet.next()) {
                String nodeId = StrUtil.trimToEmpty(resultSet.getString("node_id"));
                String name = StrUtil.trimToEmpty(resultSet.getString("name"));
                if (StrUtil.isBlank(nodeId) || StrUtil.isBlank(name)) {
                    throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, "kb_directory_nodes");
                }
                nodesById.put(nodeId, new RawDirectoryNode(
                        nodeId,
                        StrUtil.trimToNull(resultSet.getString("parent_id")),
                        name,
                        resultSet.getLong("created_at_ms"),
                        resultSet.getLong("updated_at_ms")
                ));
            }
        }
        if (nodesById.isEmpty()) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, "kb_directory_nodes");
        }
        return nodesById;
    }

    private Map<String, List<RawDirectoryNode>> buildChildrenByParentId(Iterable<RawDirectoryNode> nodes) {
        Map<String, List<RawDirectoryNode>> childrenByParentId = new LinkedHashMap<>();
        for (RawDirectoryNode node : nodes) {
            childrenByParentId.computeIfAbsent(node.parentId(), key -> new ArrayList<>()).add(node);
        }
        Comparator<RawDirectoryNode> comparator = Comparator
                .comparing(RawDirectoryNode::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(RawDirectoryNode::createdAtMs)
                .thenComparingLong(RawDirectoryNode::updatedAtMs)
                .thenComparing(RawDirectoryNode::nodeId);
        childrenByParentId.values().forEach(children -> children.sort(comparator));
        return childrenByParentId;
    }

    private void appendSubtree(List<IntAuthDirectoryNode> target,
                               Map<String, List<RawDirectoryNode>> childrenByParentId,
                               RawDirectoryNode currentNode,
                               String importedParentNodeId,
                               Set<String> visitingNodeIds) {
        if (!visitingNodeIds.add(currentNode.nodeId())) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, currentNode.nodeId());
        }
        target.add(new IntAuthDirectoryNode(currentNode.nodeId(), importedParentNodeId, currentNode.name()));
        for (RawDirectoryNode child : childrenByParentId.getOrDefault(currentNode.nodeId(), List.of())) {
            appendSubtree(target, childrenByParentId, child, currentNode.nodeId(), visitingNodeIds);
        }
        visitingNodeIds.remove(currentNode.nodeId());
    }

    private record RawDirectoryNode(String nodeId, String parentId, String name,
                                    long createdAtMs, long updatedAtMs) {
    }
}
