package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccThreeWorkflowMatrixMultiInstanceMigrationTest {

    private static final String MIGRATION_FILE =
            "sql/mysql/20260926_dcc_three_workflow_matrix_multi_instance_fix.sql";

    @Test
    void migrationMustPublishMultiInstanceMatrixReviewForAllThreeKeys() throws Exception {
        String sql = Files.readString(findProjectDir().resolve(MIGRATION_FILE), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(sql.startsWith("-- release-migration:"));
        assertTrue(sql.contains("dependsOn=20260923_dcc_three_workflow_candidate_strategy_fix"));
        assertTrue(sql.contains("-deploy-v3-tenant-"));
        assertTrue(sql.contains("VERSION_ = 2"));
        assertTrue(sql.contains("VERSION_,"));
        assertTrue(sql.contains("<userTask id=\"MATRIX_REVIEW\" name=\"会签\">\n"
                + "      <extensionElements>\n"
                + "        <flowable:candidateStrategy>35</flowable:candidateStrategy>\n"
                + "        <flowable:approveMethod>2</flowable:approveMethod>\n"
                + "        <flowable:approveRatio>100</flowable:approveRatio>\n"
                + "      </extensionElements>\n"
                + "      <multiInstanceLoopCharacteristics isSequential=\"false\">\n"
                + "        <completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>\n"
                + "      </multiInstanceLoopCharacteristics>\n"
                + "    </userTask>"),
                "the v3 BPMN replacement must add an all-obligations multi-instance MATRIX_REVIEW");
        assertTrue(sql.contains("required_bpmn_count INT DEFAULT 6"),
                "the migration must validate all three process keys for both supported tenants");
        assertTrue(sql.contains("CREATE PROCEDURE validate_dcc_three_workflow_matrix_multi_instance_v3"),
                "migration must validate the version-three BPMN source before publishing it");
        assertTrue(sql.contains("CALL validate_dcc_three_workflow_matrix_multi_instance_v3()"),
                "migration must execute the BPMN completeness guard before publishing it");
        assertTrue(sql.contains("missing exact v2 MATRIX_REVIEW source definition"),
                "a changed source BPMN must fail instead of publishing an unmodified v3 definition");
        assertTrue(sql.contains("v3 BPMN source is partially deployed"),
                "a partial prior v3 deployment must fail instead of being silently mixed with a retry");
        assertTrue(sql.contains("published v3 BPMN is missing MATRIX_REVIEW multi-instance"),
                "the migration must verify the deployed BPMN has the required multi-instance declaration");
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"),
                "BPMN integrity violations must terminate the migration explicitly");
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
