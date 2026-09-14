package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationImpactTaskMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccPublicationImpactAssessmentSchemaTest {

    @Test
    void migrationDefinesVersionedUniqueTasksAndImmutableAudit() throws Exception {
        Path migration = findProjectDir().resolve("sql/mysql/20260907_dcc_publication_impact_assessment.sql");
        assertTrue(Files.exists(migration), "impact-assessment migration must exist");
        String sql = Files.readString(migration).toLowerCase(Locale.ROOT);
        assertTrue(sql.startsWith("-- release-migration:"));
        assertTrue(sql.contains("dependson=20260907_dcc_publication_followup"));
        assertTrue(sql.contains("`dcc_publication_impact_task`"));
        assertTrue(sql.contains("`dcc_publication_impact_audit`"));
        assertTrue(sql.contains("unique key `uk_dcc_pub_impact_master`"));
        assertTrue(sql.contains("`row_version` int not null default 0"));
        assertTrue(sql.contains("`creation_token` varchar(36) not null"));
        assertFalse(sql.contains("update `dcc_publication_relation_snapshot`"));
    }

    @Test
    void resolveRaceCurrentReadUsesTenantScopedForUpdateQuery() throws Exception {
        Select select = DccPublicationImpactTaskMapper.class
                .getMethod("selectByIdAndTenantForUpdate", Long.class, Long.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("tenant_id = #{tenantid}"));
        assertTrue(sql.contains("id = #{taskid}"));
        assertTrue(sql.contains("for update"));
    }

    private Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("sql/mysql"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("sql/mysql"))) {
            return parent;
        }
        throw new IllegalStateException("Unable to locate IntRuoyiBackend project directory from " + current);
    }
}
