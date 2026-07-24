package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.module.dcc.service.category.DccIntAuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DccIntAuthDirectoryClientImplTest {

    @TempDir
    Path tempDir;

    @Test
    void listBaselineDirectories_missingDbPath_failFast() {
        DccIntAuthProperties properties = new DccIntAuthProperties();
        DccIntAuthDirectoryClientImpl client = new DccIntAuthDirectoryClientImpl(properties);

        assertServiceException(client::listBaselineDirectories, INTAUTH_DIRECTORY_IMPORT_CONFIG_MISSING);
    }

    @Test
    void listBaselineDirectories_missingBaselineSetting_failFast() throws Exception {
        Path sqliteFile = tempDir.resolve("intauth.db");
        createSchema(sqliteFile);

        DccIntAuthProperties properties = new DccIntAuthProperties();
        properties.setDbPath(sqliteFile.toString());
        DccIntAuthDirectoryClientImpl client = new DccIntAuthDirectoryClientImpl(properties);

        assertServiceException(client::listBaselineDirectories, INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID);
    }

    @Test
    void listBaselineDirectories_success() throws Exception {
        Path sqliteFile = tempDir.resolve("intauth.db");
        createSchema(sqliteFile);
        try (Connection connection = connect(sqliteFile)) {
            connection.createStatement().executeUpdate("""
                    INSERT INTO kb_directory_nodes (node_id, name, parent_id, created_at_ms, updated_at_ms)
                    VALUES
                    ('quality-root', '质量体系文件', NULL, 1, 1),
                    ('dmr-root', '3.DMR', 'quality-root', 2, 2),
                    ('child-b', '02.说明书', 'dmr-root', 4, 4),
                    ('child-a', '01.图纸', 'dmr-root', 3, 3),
                    ('grand-a', '设计输入', 'child-a', 5, 5),
                    ('test-root', 'Test Knowledge Root', NULL, 6, 6),
                    ('test-child', 'Ignore Me', 'test-root', 7, 7)
                    """);
            connection.createStatement().executeUpdate("""
                    INSERT INTO operation_approval_settings (setting_key, setting_value, updated_by, updated_at_ms)
                    VALUES ('baseline_directory_id', 'dmr-root', 'seed', 1)
                    """);
        }

        DccIntAuthProperties properties = new DccIntAuthProperties();
        properties.setDbPath(sqliteFile.toString());
        DccIntAuthDirectoryClientImpl client = new DccIntAuthDirectoryClientImpl(properties);

        List<DccIntAuthDirectoryClient.IntAuthDirectoryNode> nodes = client.listBaselineDirectories();

        assertEquals(4, nodes.size());
        assertEquals("dmr-root", nodes.get(0).nodeId());
        assertEquals(null, nodes.get(0).parentNodeId());
        assertEquals("3.DMR", nodes.get(0).name());
        assertEquals("child-a", nodes.get(1).nodeId());
        assertEquals("dmr-root", nodes.get(1).parentNodeId());
        assertEquals("grand-a", nodes.get(2).nodeId());
        assertEquals("child-a", nodes.get(2).parentNodeId());
        assertEquals("child-b", nodes.get(3).nodeId());
        assertEquals("dmr-root", nodes.get(3).parentNodeId());
    }

    private static void createSchema(Path sqliteFile) throws Exception {
        try (Connection connection = connect(sqliteFile)) {
            connection.createStatement().executeUpdate("""
                    CREATE TABLE kb_directory_nodes (
                        node_id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        parent_id TEXT,
                        created_at_ms INTEGER NOT NULL,
                        updated_at_ms INTEGER NOT NULL
                    )
                    """);
            connection.createStatement().executeUpdate("""
                    CREATE TABLE operation_approval_settings (
                        setting_key TEXT PRIMARY KEY,
                        setting_value TEXT,
                        updated_by TEXT,
                        updated_at_ms INTEGER
                    )
                    """);
        }
    }

    private static Connection connect(Path sqliteFile) throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
    }
}
