package cn.iocoder.yudao.module.srm.service.naslocator;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SrmNasLocatorWildcardTenantSqlRegressionTest {

    private TenantLineInnerInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public LongValue getTenantId() {
                return new LongValue(1L);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return false;
            }
        });
    }

    @Test
    void wildcardCountSql_shouldStayParsableUnderTenantInterceptor() {
        String sql = """
                SELECT COUNT(1)
                FROM srm_nas_locator_entry
                WHERE deleted = 0
                  AND refresh_task_id = ?
                  AND entry_type = 'FILE'
                  AND UPPER(name) LIKE UPPER(?) ESCAPE '\\'
                """;

        String parsed = assertDoesNotThrow(() -> interceptor.parserSingle(sql, null));
        assertTrue(parsed.contains("tenant_id = 1"));
    }

    @Test
    void wildcardPageSql_shouldStayParsableUnderTenantInterceptor() {
        String sql = """
                SELECT id, tenant_id, refresh_task_id, entry_type, name, path, parent_path, size, modified_at
                FROM srm_nas_locator_entry
                WHERE deleted = 0
                  AND refresh_task_id = ?
                  AND entry_type = 'FILE'
                  AND UPPER(name) LIKE UPPER(?) ESCAPE '\\'
                ORDER BY name ASC, parent_path ASC
                LIMIT ? OFFSET ?
                """;

        String parsed = assertDoesNotThrow(() -> interceptor.parserSingle(sql, null));
        assertTrue(parsed.contains("tenant_id = 1"));
    }

    @Test
    void mapperXml_shouldAvoidExplicitEscapeClause() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("mapper/naslocator/SrmNasLocatorEntryMapper.xml")) {
            assertNotNull(inputStream, "missing nas locator mapper xml");
            String xml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(!xml.contains(" ESCAPE "),
                    "wildcard LIKE should rely on default backslash escaping for MySQL/H2 compatibility");
        }
    }
}
