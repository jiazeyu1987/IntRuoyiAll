package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccFormCenterPolicyMigrationTest {

    @Test
    void dccObsoletePolicyMigrationShouldSeedPublishedActionPolicy() throws Exception {
        Path projectDir = findProjectDir();
        Path migrationFile = projectDir.resolve("sql/mysql/20260719_dcc_obsolete_form_policy_seed.sql");

        assertTrue(Files.exists(migrationFile), "DCC obsolete form-center policy migration must exist");

        String sql = Files.readString(migrationFile, StandardCharsets.UTF_8);
        assertTrue(sql.contains("-- release-migration:"),
                "DCC obsolete form-center policy migration must carry release metadata");
        assertTrue(sql.contains("dependsOn=20260717_bpm_form_center"),
                "DCC obsolete form-center policy migration must depend on form-center schema");
        assertTrue(sql.contains("`bpm_business_approval_policy`"),
                "DCC obsolete form-center policy migration must seed bpm_business_approval_policy");
        assertTrue(sql.contains("'DCC'") && sql.contains("'CONTROLLED_FILE'"),
                "DCC obsolete policy must target DCC controlled files");
        assertTrue(sql.contains("'OBSOLETE'") && sql.contains("'ACTIVE'"),
                "DCC obsolete policy must match OBSOLETE action on ACTIVE files");
        assertTrue(sql.contains("'DCC_OBSOLETE'"),
                "DCC obsolete policy must use the registered DCC_OBSOLETE effect executor");
        assertTrue(sql.contains("'[]'"),
                "DCC obsolete policy must not create a second form-template dependency for the reason-only dialog");
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"),
                "DCC obsolete policy migration must fail fast on invalid prerequisites or conflicts");
        assertTrue(sql.contains("DCC obsolete form policy conflict"),
                "DCC obsolete policy migration must not overwrite conflicting existing policies");
        assertTrue(sql.contains("COALESCE(`policy`.`form_policy_type`, '') <> 'NONE'"),
                "DCC obsolete policy migration must reject obsolete policies that still use upload template mode");
        assertTrue(sql.contains("COALESCE(`policy`.`form_slots_json`, '[]') <> '[]'"),
                "DCC obsolete policy migration must reject obsolete policies that still bind upload template slots");
        assertTrue(sql.contains("DCC obsolete form policy duplicate"),
                "DCC obsolete policy migration must reject duplicate published policies");
        assertFalse(Pattern.compile("INSERT\\s+INTO\\s+`?bpm_form_template_version`?", Pattern.CASE_INSENSITIVE)
                        .matcher(sql).find(),
                "DCC obsolete policy migration must reuse the official obsolete dialog instead of creating templates");
    }

    private static Path findProjectDir() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("pom.xml")) && Files.exists(current.resolve("sql/mysql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate project root");
    }
}
