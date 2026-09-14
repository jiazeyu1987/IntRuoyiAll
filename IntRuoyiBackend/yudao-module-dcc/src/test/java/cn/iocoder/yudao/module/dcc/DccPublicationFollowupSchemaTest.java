package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRelatedFileMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccPublicationFollowupSchemaTest {

    @Test
    void migrationDefinesStructuredImmutablePublicationFollowupLedger() throws Exception {
        Path migrationPath = findProjectDir().resolve("sql/mysql/20260907_dcc_publication_followup.sql");
        assertTrue(Files.exists(migrationPath), "publication follow-up migration must exist");
        String migration = Files.readString(migrationPath).toLowerCase(Locale.ROOT);

        assertTrue(migration.startsWith("-- release-migration:"));
        assertTrue(migration.contains("dependson=20260903_dcc_controlled_file_related_file,20260906_dcc_new_file_lifecycle_p4"));
        assertTrue(migration.contains("`dcc_publication_followup_batch`"));
        assertTrue(migration.contains("`dcc_publication_visibility_rule_snapshot`"));
        assertTrue(migration.contains("`dcc_publication_visibility_user_snapshot`"));
        assertTrue(migration.contains("`dcc_publication_notification_candidate`"));
        assertTrue(migration.contains("`dcc_publication_notification_candidate_reason`"));
        assertTrue(migration.contains("`dcc_publication_relation_snapshot`"));
        assertTrue(migration.contains("`dcc_publication_relation_direction_snapshot`"));
        assertTrue(migration.contains("unique key `uk_dcc_pub_followup_file` (`tenant_id`, `published_controlled_file_id`, `deleted`)"));
        assertTrue(migration.contains("unique key `uk_dcc_pub_relation_master` (`tenant_id`, `batch_id`, `related_master_id`, `deleted`)"));
        assertTrue(migration.contains("unique key `uk_dcc_pub_candidate_user` (`tenant_id`, `batch_id`, `user_id`, `deleted`)"));
        assertFalse(migration.contains("user_ids_json"));
        assertFalse(migration.contains("recipient_ids_json"));
        assertFalse(migration.contains("update `dcc_controlled_file`"));
    }

    @Test
    void reverseRelationQueryOnlyUsesOtherMastersCurrentFormalVersion() throws Exception {
        Select select = DccControlledFileRelatedFileMapper.class
                .getMethod("selectReverseCurrentActiveRelations", Long.class, Long.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("source_master.current_active_controlled_file_id = source_file.id"));
        assertTrue(sql.contains("relation.related_master_id = #{relatedmasterid}"));
        assertTrue(sql.contains("source_master.id <> #{relatedmasterid}"));
        assertTrue(sql.contains("relation.tenant_id = #{tenantid}"));
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
