package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileMapperTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DataSource dataSource;

    @Test
    void insert_assignsIdForControlledFileAndSnapshot() {
        executeUpdate("""
                INSERT INTO dcc_controlled_file_master
                (id, category_id, file_name, file_number, current_active_controlled_file_id, status,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 700L, 10L, "SOP-001", "FI-001", null, "ACTIVE_CHAIN", 0L, "1", "1", 0);

        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(10L)
                .directoryId(20L)
                .originalFileId(30L)
                .title("SOP-001")
                .versionNo("V1.0")
                .effectiveDate(LocalDate.of(2026, 5, 13))
                .remark("mapper-id-check")
                .status("PENDING_DOC_CONTROL_REVIEW")
                .requesterId(99L)
                .processDefinitionKey("dcc-controlled-file-approval")
                .build();
        setField(file, "masterId", 700L);
        setField(file, "fileName", "SOP-001");
        setField(file, "fileNumber", "FI-001");
        setField(file, "sourceFileId", 30L);
        setField(file, "submitterId", 99L);

        controlledFileMapper.insert(file);
        assertNotNull(file.getId());
        assertEquals(700L, queryLong(
                "SELECT master_id FROM dcc_controlled_file WHERE id = ?",
                file.getId()));

        DccControlledFileRouteSnapshotDO snapshot = DccControlledFileRouteSnapshotDO.builder()
                .controlledFileId(file.getId())
                .routeVersionNo(1)
                .stageNo(1)
                .candidateSourceType("POSITION")
                .candidateSourceId(50L)
                .resolvedUserIds("200")
                .approveMethod("ALL")
                .approveRatio(100)
                .build();
        setField(snapshot, "stageCode", "DOC_CONTROL_REVIEW");
        setField(snapshot, "stageOrder", 1);
        setField(snapshot, "requireAllApprovals", Boolean.TRUE);

        routeSnapshotMapper.insert(snapshot);
        assertNotNull(snapshot.getId());
        assertEquals("DOC_CONTROL_REVIEW", queryString(
                "SELECT stage_code FROM dcc_controlled_file_route_snapshot WHERE id = ?",
                snapshot.getId()));
    }

    @Test
    void insert_supportingLifecycleTables() {
        executeUpdate("""
                INSERT INTO dcc_controlled_file_master
                (id, category_id, file_name, file_number, current_active_controlled_file_id, status,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 701L, 10L, "SOP-002", "FI-002", null, "ACTIVE_CHAIN", 0L, "1", "1", 0);

        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(10L)
                .directoryId(20L)
                .originalFileId(31L)
                .title("SOP-002")
                .versionNo("V2.0")
                .effectiveDate(LocalDate.of(2026, 5, 14))
                .remark("supporting-entities-check")
                .status("FINALIZATION_FAILED")
                .requesterId(100L)
                .processDefinitionKey("dcc-controlled-file-approval")
                .build();
        setField(file, "masterId", 701L);
        setField(file, "fileName", "SOP-002");
        setField(file, "fileNumber", "FI-002");
        setField(file, "sourceFileId", 31L);
        setField(file, "submitterId", 100L);
        setField(file, "finalizationError", "message persistence failed");
        controlledFileMapper.insert(file);

        executeUpdate("""
                INSERT INTO dcc_controlled_file_message_job
                (id, business_type, business_id, template_code, recipient_user_id, status, error_message, sent_at,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                        ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 810L, "DISTRIBUTION", file.getId(), "dcc_distribution", 300L, "PENDING", null, 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_signature
                (id, controlled_file_id, task_id, actor_id, action_type, signature_mode, password_verified, comment, signed_at,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                        ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 820L, file.getId(), "task-1", 100L, "APPROVE", "PASSWORD", true, "approved", 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_distribution
                (id, controlled_file_id, department_id, status, tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 830L, file.getId(), 400L, "PENDING", 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_distribution_recipient
                (id, distribution_id, user_id, message_job_id, read_at, acknowledged_at,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                        ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 831L, 830L, 300L, 810L, 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_training
                (id, controlled_file_id, department_id, status, tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 840L, file.getId(), 500L, "PENDING", 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_training_assignment
                (id, training_id, user_id, message_job_id, status, acknowledged_at,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                        ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 841L, 840L, 301L, 810L, "ACKNOWLEDGED", 0L, "1", "1", 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_obsolete_audit
                (id, controlled_file_id, operator_id, obsolete_reason, status_before, status_after,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 850L, file.getId(), 100L, "superseded by newer revision", "ACTIVE", "OBSOLETE", 0L, "1", "1", 0);

        assertEquals(1, countById("dcc_controlled_file_message_job", 810L));
        assertEquals(1, countById("dcc_controlled_file_signature", 820L));
        assertEquals(1, countById("dcc_controlled_file_distribution", 830L));
        assertEquals(1, countById("dcc_controlled_file_distribution_recipient", 831L));
        assertEquals(1, countById("dcc_controlled_file_training", 840L));
        assertEquals(1, countById("dcc_controlled_file_training_assignment", 841L));
        assertEquals(1, countById("dcc_controlled_file_obsolete_audit", 850L));
    }

    @Test
    void selectWorkflowList_keywordMatchesTitleFileNameAndFileNumber() {
        DccControlledFileDO titleMatch = insertControlledFile(702L, "BROWSER-SEARCH-20260617-标题", "WI-001.pdf", "DCC-001");
        DccControlledFileDO fileNameMatch = insertControlledFile(703L, "作业指导书", "BROWSER-SEARCH-20260617-source.pdf", "DCC-002");
        DccControlledFileDO numberMatch = insertControlledFile(704L, "检验规范", "inspection.pdf", "BROWSER-SEARCH-20260617-NO");
        insertControlledFile(705L, "检验规范", "inspection.pdf", "DCC-003");

        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setKeyword("BROWSER-SEARCH-20260617");

        List<Long> ids = controlledFileMapper.selectWorkflowList(reqVO).stream()
                .map(DccControlledFileDO::getId)
                .toList();

        assertEquals(3, ids.size());
        assertTrue(ids.contains(titleMatch.getId()));
        assertTrue(ids.contains(fileNameMatch.getId()));
        assertTrue(ids.contains(numberMatch.getId()));
    }

    @Test
    void selectAssociatedFileCounts_mapsGroupedCountRows() {
        DccControlledFileDO first = insertControlledFile(706L, "项目A文件1", "project-a-1.pdf", "PA-001");
        DccControlledFileDO second = insertControlledFile(707L, "项目A文件2", "project-a-2.pdf", "PA-002");
        insertControlledFile(708L, "未关联文件", "unmatched.pdf", "UM-001");
        executeUpdate("UPDATE dcc_controlled_file SET dcc_project_code_id = ? WHERE id IN (?, ?)",
                1001L, first.getId(), second.getId());

        List<DccControlledFileMapper.ProjectCodeFileCount> counts =
                controlledFileMapper.selectAssociatedFileCountsByProjectCodeIds(List.of(1001L, 1002L));

        assertEquals(1, counts.size());
        assertEquals(1001L, counts.get(0).getProjectCodeId());
        assertEquals(2L, counts.get(0).getFileCount());
    }

    @Test
    void selectAssociatedFileCounts_includesSuccessfulRecognitionLedgerWhenFileFieldMissing() {
        DccControlledFileDO ledgerOnly = insertControlledFile(709L, "账本关联文件", "ledger-only.pdf", "LG-001");
        DccControlledFileDO direct = insertControlledFile(710L, "直接关联文件", "direct.pdf", "DR-001");
        executeUpdate("UPDATE dcc_controlled_file SET dcc_project_code_id = ? WHERE id = ?",
                1002L, direct.getId());
        executeUpdate("""
                INSERT INTO dcc_controlled_file_recognition_record
                (controlled_file_id, recognition_scope, recognition_method, recognition_version, status,
                 matched_project_code_id, recognized_time, tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, ledgerOnly.getId(), "BASIC_INFO", "FILE_NAME_ALIAS", "project-code-v1", "SUCCESS",
                1001L, 0L, "1", "1", 0);

        List<DccControlledFileMapper.ProjectCodeFileCount> counts =
                controlledFileMapper.selectAssociatedFileCountsByProjectCodeIds(List.of(1001L, 1002L));

        assertEquals(List.of(1001L, 1002L), counts.stream()
                .map(DccControlledFileMapper.ProjectCodeFileCount::getProjectCodeId)
                .toList());
        assertEquals(List.of(1L, 1L), counts.stream()
                .map(DccControlledFileMapper.ProjectCodeFileCount::getFileCount)
                .toList());
    }

    @Test
    void selectCurrentApprovedFilesByIds_keepsExternalProjectFileAndDropsOldRevision() {
        DccControlledFileDO oldRevision = insertControlledFile(720L, "旧修订", "revision.pdf", "REV-001");
        DccControlledFileDO currentRevision = DccControlledFileDO.builder()
                .categoryId(10L)
                .directoryId(20L)
                .originalFileId(30L)
                .title("新修订")
                .versionNo("V2.0")
                .effectiveDate(LocalDate.of(2026, 6, 18))
                .remark("current-approved-selected")
                .status("ACTIVE")
                .requesterId(99L)
                .processDefinitionKey("dcc-controlled-file-approval")
                .build();
        setField(currentRevision, "masterId", 720L);
        setField(currentRevision, "fileName", "revision.pdf");
        setField(currentRevision, "fileNumber", "REV-001");
        setField(currentRevision, "sourceFileId", 30L);
        setField(currentRevision, "submitterId", 99L);
        controlledFileMapper.insert(currentRevision);
        DccControlledFileDO externalProjectFile = insertControlledFile(721L, "外部项目文件", "external-project.pdf", "EXT-001");
        executeUpdate("UPDATE dcc_controlled_file SET dcc_project_code_id = ? WHERE id = ?",
                129L, externalProjectFile.getId());

        List<Long> ids = controlledFileMapper.selectCurrentApprovedFilesByIds(List.of(
                        oldRevision.getId(), currentRevision.getId(), externalProjectFile.getId()))
                .stream()
                .map(DccControlledFileDO::getId)
                .toList();

        assertEquals(List.of(currentRevision.getId(), externalProjectFile.getId()), ids);
    }

    @Test
    void selectWorkflowList_filtersProjectCodeByLatestSuccessfulRecognitionWhenDirectFieldMissing() {
        DccControlledFileDO ledgerOnly = insertControlledFile(711L, "账本详情文件", "ledger-detail.pdf", "LD-001");
        DccControlledFileDO direct = insertControlledFile(712L, "直接详情文件", "direct-detail.pdf", "DD-001");
        DccControlledFileDO other = insertControlledFile(713L, "其他项目文件", "other-project.pdf", "OP-001");
        executeUpdate("UPDATE dcc_controlled_file SET dcc_project_code_id = ? WHERE id = ?",
                1002L, direct.getId());
        executeUpdate("UPDATE dcc_controlled_file SET dcc_project_code_id = ? WHERE id = ?",
                1003L, other.getId());
        executeUpdate("""
                INSERT INTO dcc_controlled_file_recognition_record
                (controlled_file_id, recognition_scope, recognition_method, recognition_version, status,
                 matched_project_code_id, recognized_time, tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, ledgerOnly.getId(), "BASIC_INFO", "FILE_NAME_ALIAS", "project-code-v1", "SUCCESS",
                1002L, 0L, "1", "1", 0);

        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setDccProjectCodeId(1002L);

        List<Long> ids = controlledFileMapper.selectWorkflowList(reqVO).stream()
                .map(DccControlledFileDO::getId)
                .toList();

        assertEquals(List.of(direct.getId(), ledgerOnly.getId()), ids);
    }

    @Test
    void selectWorkflowList_fileTypeTaxonomyPathsIncludeLegacyNameOnlyRows() {
        DccControlledFileDO idMatched = insertControlledFile(714L, "分类ID文件", "taxonomy-id.pdf", "TX-ID");
        DccControlledFileDO pathMatched = insertControlledFile(715L, "历史路径文件", "taxonomy-path.pdf", "TX-PATH");
        DccControlledFileDO childPathMatched = insertControlledFile(716L, "历史子路径文件", "taxonomy-child.pdf", "TX-CHILD");
        DccControlledFileDO inactiveChildPath = insertControlledFile(717L, "停用子路径文件", "taxonomy-inactive.pdf", "TX-INACTIVE");
        executeUpdate("UPDATE dcc_controlled_file SET file_type_taxonomy_id = ? WHERE id = ?",
                8803L, idMatched.getId());
        executeUpdate("""
                UPDATE dcc_controlled_file
                   SET file_type_taxonomy_id = NULL,
                       file_type_level1 = ?, file_type_level2 = ?, file_type_level3 = ?, file_type_level4 = NULL,
                       file_type_level5 = NULL
                 WHERE id = ?
                """, "一级", "二级", "三级", pathMatched.getId());
        executeUpdate("""
                UPDATE dcc_controlled_file
                   SET file_type_taxonomy_id = NULL,
                       file_type_level1 = ?, file_type_level2 = ?, file_type_level3 = ?, file_type_level4 = ?,
                       file_type_level5 = NULL
                 WHERE id = ?
                """, "一级", "二级", "三级", "四级", childPathMatched.getId());
        executeUpdate("""
                UPDATE dcc_controlled_file
                   SET file_type_taxonomy_id = NULL,
                       file_type_level1 = ?, file_type_level2 = ?, file_type_level3 = ?, file_type_level4 = ?,
                       file_type_level5 = NULL
                 WHERE id = ?
                """, "一级", "二级", "三级", "停用四级", inactiveChildPath.getId());

        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setFileTypeTaxonomyIds(List.of(8803L, 8804L));
        reqVO.setFileTypeTaxonomyPaths(List.of(
                new DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter("一级", "二级", "三级", null, null),
                new DccControlledFilePageReqVO.FileTypeTaxonomyPathFilter("一级", "二级", "三级", "四级", null)));

        List<Long> ids = controlledFileMapper.selectWorkflowList(reqVO).stream()
                .map(DccControlledFileDO::getId)
                .toList();

        assertEquals(List.of(childPathMatched.getId(), pathMatched.getId(), idMatched.getId()), ids);
    }

    @Test
    void sourceOwnershipMigrationQueries_includeDeletedHistoryAndExcludeOwnedRecords() {
        insertSourceReference(9901L, 9700L, 0);
        insertSourceReference(9902L, 9700L, 1);
        insertSourceReference(9903L, 9701L, 0);
        executeUpdate("""
                INSERT INTO dcc_controlled_file_source_ownership
                (id, controlled_file_id, source_file_id, origin_source_file_id, source_sha256, ownership_type,
                 claimed_by, claimed_time, tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, 9801L, 9901L, 9700L, 9700L, "owner-sha", "HISTORICAL_MIGRATION",
                120L, 0L, "120", "120", 0);

        List<DccControlledFileDO> unowned = controlledFileMapper.selectUnownedSourceReferences(0L, 10);

        assertEquals(List.of(9902L, 9903L), unowned.stream().map(DccControlledFileDO::getId).toList());
        assertEquals(3L, controlledFileMapper.countAllSourceReferences(0L));
        assertEquals(2L, controlledFileMapper.countUnownedSourceReferences(0L));
        assertEquals(1L, controlledFileMapper.countSharedSourceGroups(0L));
        assertEquals(2L, controlledFileMapper.countSharedSourceRecords(0L));
        assertNotNull(controlledFileMapper.selectByIdAndTenantIncludingDeleted(0L, 9902L));

        assertEquals(1, controlledFileMapper.updateSourceFileIdIncludingDeleted(
                0L, 9902L, 9700L, 19700L, 120L));
        assertEquals(19700L, queryLong("SELECT source_file_id FROM dcc_controlled_file WHERE id = ?", 9902L));
    }

    private DccControlledFileDO insertControlledFile(Long masterId, String title, String fileName, String fileNumber) {
        executeUpdate("""
                INSERT INTO dcc_controlled_file_master
                (id, category_id, file_name, file_number, current_active_controlled_file_id, status,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, masterId, 10L, fileName, fileNumber, null, "ACTIVE_CHAIN", 0L, "1", "1", 0);

        DccControlledFileDO file = DccControlledFileDO.builder()
                .categoryId(10L)
                .directoryId(20L)
                .originalFileId(30L)
                .title(title)
                .versionNo("V1.0")
                .effectiveDate(LocalDate.of(2026, 6, 17))
                .remark("browser-search-keyword")
                .status("ACTIVE")
                .requesterId(99L)
                .processDefinitionKey("dcc-controlled-file-approval")
                .build();
        setField(file, "masterId", masterId);
        setField(file, "fileName", fileName);
        setField(file, "fileNumber", fileNumber);
        setField(file, "sourceFileId", 30L);
        setField(file, "submitterId", 99L);
        controlledFileMapper.insert(file);
        return file;
    }

    private void insertSourceReference(Long id, Long sourceFileId, int deleted) {
        executeUpdate("""
                INSERT INTO dcc_controlled_file
                (id, master_id, category_id, directory_id, source_file_id, original_file_id,
                 file_name, title, file_number, version_no, status, submitter_id, requester_id,
                 tenant_id, create_time, update_time, creator, updater, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?)
                """, id, id + 1000, 10L, 20L, sourceFileId, sourceFileId,
                "source-" + id + ".docx", "source-" + id, "SRC-" + id,
                "V1.0", "ACTIVE", 120L, 120L, 0L, "120", "120", deleted);
    }

    private int countById(String tableName, Long id) {
        return queryInt("SELECT COUNT(1) FROM " + tableName + " WHERE id = ?", id);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Missing field " + target.getClass().getSimpleName() + "." + fieldName, ex);
        }
    }

    private void executeUpdate(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, params);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed SQL update", ex);
        }
    }

    private Long queryLong(String sql, Object... params) {
        Object value = querySingleValue(sql, params);
        return value == null ? null : ((Number) value).longValue();
    }

    private Integer queryInt(String sql, Object... params) {
        Object value = querySingleValue(sql, params);
        return value == null ? null : ((Number) value).intValue();
    }

    private String queryString(String sql, Object... params) {
        Object value = querySingleValue(sql, params);
        return value == null ? null : value.toString();
    }

    private Object querySingleValue(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getObject(1);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed SQL query", ex);
        }
    }

    private void bindParameters(PreparedStatement statement, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
