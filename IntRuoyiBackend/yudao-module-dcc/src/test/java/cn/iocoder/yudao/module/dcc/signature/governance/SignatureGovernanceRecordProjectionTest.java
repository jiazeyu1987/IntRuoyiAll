package cn.iocoder.yudao.module.dcc.signature.governance;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.dal.mysql.signature.governance.SignatureGovernanceRecordMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Query structure and H2 scalar-expression tests, not full MySQL JSON query execution. */
class SignatureGovernanceRecordProjectionTest {
    private static final String NAMES = "COALESCE(NULLIF(CONCAT_WS(' · ', NULLIF(approval_process.NAME_, ''), NULLIF(approval_task.NAME_, '')), ''), '审批任务（未记录业务名称）')";

    @Test
    void actualBoundQueriesKeepSpecificBranchesAndTenantScopedSnapshotIdentity() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/signature/governance/SignatureGovernanceRecordMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        for (String method : List.of("selectSignatureRecordPage", "selectSignatureRecordByGlobalId")) {
            String sql = configuration.getMappedStatement(SignatureGovernanceRecordMapper.class.getName() + "." + method)
                    .getBoundSql(Map.of("reqVO", new SignatureGovernanceRecordPageReqVO(), "globalId", "UNIFIED-1"))
                    .getSql().replaceAll("\\s+", " ");
            assertNotNull(CCJSqlParserUtil.parse(sql), "Actual CASE/JOIN query must parse");
            String unified = sql.substring(sql.indexOf("CONVERT(CONCAT('UNIFIED-', sig.id)"));
            assertTrue(unified.contains("WHEN sig.module_code = 'BPM' AND sig.subject_type = 'BPM_APPROVAL_TASK' THEN " + NAMES + " ELSE sig.subject_type END"));
            assertTrue(unified.contains("WHEN sig.module_code = 'BPM' AND sig.subject_type = 'BPM_APPROVAL_TASK' THEN NULL ELSE sig.subject_id END"));
            assertTrue(unified.indexOf("THEN 'eDHR上市放行'") < unified.indexOf(NAMES));
            assertTrue(unified.contains("approval_process.TENANT_ID_ = CAST(sig.tenant_id AS CHAR)"));
            assertTrue(unified.contains("approval_task.TENANT_ID_ = approval_process.TENANT_ID_"));
            assertTrue(unified.contains("CAST(approval_process.PROC_INST_ID_ AS BINARY) = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(sig.canonical_content_json, '$.processInstanceId')), 'null') AS BINARY)"));
            assertTrue(unified.contains("CAST(approval_task.ID_ AS BINARY) = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(sig.canonical_content_json, '$.sourceTaskId')), 'null') AS BINARY)"));
            assertFalse(unified.contains("$.businessKey"));
        }
    }

    @Test
    void exactNameProjectionKeepsBothNamesAndReportsMissingValues() throws Exception {
        String xml = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/mapper/signature/governance/SignatureGovernanceRecordMapper.xml"));
        int start = xml.indexOf(NAMES);
        assertTrue(start >= 0, "Use actual projection expression from the mapper");
        String expression = xml.substring(start, start + NAMES.length()).replace("approval_process.NAME_", "?").replace("approval_task.NAME_", "?");
        // No tables or stored records: only the scalar expression in H2's MySQL compatibility mode.
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:signature_projection;MODE=MySQL")) {
            for (String[] values : new String[][] {
                    {"采购申请A", "审批", "采购申请A · 审批"},
                    {"采购申请B", "审批", "采购申请B · 审批"},
                    {"采购申请A", null, "采购申请A"},
                    {null, "审批", "审批"},
                    {null, null, "审批任务（未记录业务名称）"},
                    {"", "", "审批任务（未记录业务名称）"}}) {
                try (var query = connection.prepareStatement("SELECT " + expression)) {
                    query.setString(1, values[0]); query.setString(2, values[1]);
                    try (var rows = query.executeQuery()) {
                        assertTrue(rows.next()); assertEquals(values[2], rows.getString(1));
                    }
                }
            }
        }
    }
}
