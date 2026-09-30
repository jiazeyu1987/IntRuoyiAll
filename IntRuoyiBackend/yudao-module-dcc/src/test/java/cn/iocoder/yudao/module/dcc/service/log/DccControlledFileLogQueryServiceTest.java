package cn.iocoder.yudao.module.dcc.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileCheckoutDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(DccControlledFileLogQueryServiceImpl.class)
class DccControlledFileLogQueryServiceTest extends BaseDbUnitTest {

    @Resource
    private DccControlledFileLogQueryService logQueryService;
    @Resource
    private DataSource dataSource;
    @MockitoSpyBean
    private DccControlledFileMapper controlledFileMapper;
    @MockitoSpyBean
    private DccControlledFileCheckoutMapper checkoutMapper;
    @MockitoSpyBean
    private DccControlledFileDistributionMapper distributionMapper;
    @MockitoSpyBean
    private DccControlledFileAccessEventMapper accessEventMapper;
    @MockitoSpyBean
    private DccControlledFileAccessLogMapper accessLogMapper;
    @MockitoSpyBean
    private DccProjectCodeMapper projectCodeMapper;
    @MockitoSpyBean
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @MockitoSpyBean
    private DccControlledFileMetadataChangeMapper changeMapper;
    @MockitoSpyBean
    private DccControlledFileMetadataChangeItemMapper changeItemMapper;
    @MockitoSpyBean
    private DccProjectCodeAssignmentFileMapper assignmentFileMapper;
    @MockitoSpyBean
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;

    @BeforeEach
    void setUpUsers() {
        new JdbcTemplate(dataSource).execute(
                "ALTER TABLE dcc_controlled_file_access_log ALTER COLUMN user_id DROP NOT NULL");
        when(adminUserApi.getUserList(ArgumentMatchers.<Collection<Long>>any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .map(id -> new AdminUserRespDTO().setId(id).setNickname("用户" + id))
                    .toList();
        });
    }

    @Test
    void getLogPage_sortsMultipleSourcesAndFiltersByTypeKeywordAndTime() {
        DccControlledFileDO file = insertControlledFile(1001L, "DOC-1001", "离心泵SOP", "A");
        insertAccessAudit(file, 2001L, "PREVIEW", "SUCCESS",
                LocalDateTime.of(2026, 7, 14, 9, 0));
        DccProjectCodeDO projectCode = insertProjectCode(3001L, "IR-PTC", "PTC-A");
        DccProjectCodeAssignmentDO assignment = insertAssignment(projectCode, 2002L,
                LocalDateTime.of(2026, 7, 14, 10, 0));
        insertProjectCodeChange(file, projectCode, assignment, 2003L,
                LocalDateTime.of(2026, 7, 14, 11, 0));
        insertTrainingProgress(file, 2004L, LocalDateTime.of(2026, 7, 14, 12, 0));

        DccControlledFileLogPageReqVO allReq = new DccControlledFileLogPageReqVO();
        allReq.setPageNo(1);
        allReq.setPageSize(20);
        PageResult<DccControlledFileLogRespVO> allPage = logQueryService.getLogPage(allReq);

        assertEquals(4L, allPage.getTotal());
        assertEquals(List.of("TRAINING_EXECUTION", "PROJECT_CODE_CHANGE", "PROJECT_CODE_ASSIGNMENT",
                "CONTROLLED_FILE_AUDIT"), allPage.getList().stream()
                .map(DccControlledFileLogRespVO::getLogType).toList());

        DccControlledFileLogPageReqVO changeReq = new DccControlledFileLogPageReqVO();
        changeReq.setLogType("PROJECT_CODE_CHANGE");
        changeReq.setFieldName("fileTypeLevel2");
        PageResult<DccControlledFileLogRespVO> changePage = logQueryService.getLogPage(changeReq);

        assertEquals(1L, changePage.getTotal());
        DccControlledFileLogRespVO changeRow = changePage.getList().get(0);
        assertEquals("字段修改", changeRow.getActionLabel());
        assertEquals("成功", changeRow.getResultLabel());
        assertEquals("DOC-1001", changeRow.getFileNumber());
        assertEquals("离心泵SOP", changeRow.getFileName());
        assertEquals("用户2003", changeRow.getOperatorName());
        assertEquals("旧阶段", changeRow.getOldValueText());
        assertEquals("新阶段", changeRow.getNewValueText());
        assertTrue(changeRow.getDetailJson().contains("\"fieldName\":\"fileTypeLevel2\""));

        DccControlledFileLogPageReqVO keywordReq = new DccControlledFileLogPageReqVO();
        keywordReq.setKeyword("IR-PTC");
        PageResult<DccControlledFileLogRespVO> keywordPage = logQueryService.getLogPage(keywordReq);
        assertEquals(Set.of("PROJECT_CODE_ASSIGNMENT", "PROJECT_CODE_CHANGE"),
                Set.copyOf(keywordPage.getList().stream().map(DccControlledFileLogRespVO::getLogType).toList()));

        DccControlledFileLogPageReqVO timeReq = new DccControlledFileLogPageReqVO();
        timeReq.setOccurredAt(new LocalDateTime[]{
                LocalDateTime.of(2026, 7, 14, 10, 30),
                LocalDateTime.of(2026, 7, 14, 12, 30)
        });
        PageResult<DccControlledFileLogRespVO> timePage = logQueryService.getLogPage(timeReq);
        assertEquals(List.of("TRAINING_EXECUTION", "PROJECT_CODE_CHANGE"), timePage.getList().stream()
                .map(DccControlledFileLogRespVO::getLogType).toList());
    }

    @Test
    void getLogPage_includesLifecycleAndDistributionLogs() {
        DccControlledFileDO file = insertLifecycleFile(1101L, "DOC-1101", "灭菌SOP", "B",
                "NEW", "ACTIVE",
                LocalDateTime.of(2026, 7, 15, 8, 0),
                LocalDateTime.of(2026, 7, 15, 10, 0),
                LocalDateTime.of(2026, 7, 15, 12, 0),
                null, null);
        insertDistribution(file, 6101L, 253L, "PUBLIC_FOLDER", "READ", null,
                LocalDateTime.of(2026, 7, 15, 13, 0), null, null);
        insertLifecycleFile(1102L, "DOC-1102", "灭菌SOP", "C",
                "REVISION", "PENDING_MATRIX_APPROVAL",
                LocalDateTime.of(2026, 7, 15, 9, 0),
                null, null, null, null);
        insertLifecycleFile(1103L, "DOC-1103", "灭菌SOP", "D",
                "OBSOLETE", "OBSOLETE",
                LocalDateTime.of(2026, 7, 15, 9, 30),
                null, null, 2401L, LocalDateTime.of(2026, 7, 15, 14, 0));

        DccControlledFileLogPageReqVO allReq = new DccControlledFileLogPageReqVO();
        allReq.setPageNo(1);
        allReq.setPageSize(20);
        PageResult<DccControlledFileLogRespVO> allPage = logQueryService.getLogPage(allReq);

        assertEquals(List.of("FILE_OBSOLETE", "FILE_DISTRIBUTION", "FILE_RELEASE", "FILE_APPROVAL",
                        "FILE_REVISION", "FILE_SUBMISSION"),
                allPage.getList().stream().map(DccControlledFileLogRespVO::getLogType).toList());

        DccControlledFileLogPageReqVO distributionReq = new DccControlledFileLogPageReqVO();
        distributionReq.setLogType("FILE_DISTRIBUTION");
        PageResult<DccControlledFileLogRespVO> distributionPage = logQueryService.getLogPage(distributionReq);

        assertEquals(1L, distributionPage.getTotal());
        DccControlledFileLogRespVO distributionRow = distributionPage.getList().get(0);
        assertEquals("FILE_DISTRIBUTION:6101", distributionRow.getId());
        assertEquals("分发", distributionRow.getActionLabel());
        assertEquals("已阅读", distributionRow.getResultLabel());
        assertEquals("DOC-1101", distributionRow.getFileNumber());
        assertEquals("灭菌SOP", distributionRow.getFileName());
        assertEquals("部门253", distributionRow.getRelatedObject());
        assertTrue(distributionRow.getSummary().contains("公共区"));

        DccControlledFileLogPageReqVO keywordReq = new DccControlledFileLogPageReqVO();
        keywordReq.setKeyword("分发");
        PageResult<DccControlledFileLogRespVO> keywordPage = logQueryService.getLogPage(keywordReq);
        assertEquals(List.of("FILE_DISTRIBUTION"),
                keywordPage.getList().stream().map(DccControlledFileLogRespVO::getLogType).toList());
    }

    @Test
    void getLogPage_handlesProjectCodeChangeWithMissingAssignmentAndProjectCode() {
        DccControlledFileDO file = insertControlledFile(1201L, "DOC-1201", "签核追溯SOP", "V1.0");
        DccControlledFileMetadataChangeDO change = DccControlledFileMetadataChangeDO.builder()
                .id(5201L)
                .controlledFileId(file.getId())
                .masterId(file.getMasterId())
                .operatorUserId(2201L)
                .source("MANUAL")
                .changeReason("历史修正追溯")
                .changedFieldCount(1)
                .changedTime(LocalDateTime.of(2026, 8, 2, 17, 30))
                .build();
        changeMapper.insert(change);
        changeItemMapper.insert(DccControlledFileMetadataChangeItemDO.builder()
                .id(5202L)
                .changeId(change.getId())
                .controlledFileId(file.getId())
                .operatorUserId(2201L)
                .fieldName("fileTypeLevel3")
                .fieldLabel("文件类别 III")
                .oldValueText(null)
                .newValueText("修正后类别")
                .changedTime(LocalDateTime.of(2026, 8, 2, 17, 31))
                .build());

        DccControlledFileLogPageReqVO req = new DccControlledFileLogPageReqVO();
        req.setLogType("PROJECT_CODE_CHANGE");
        req.setControlledFileId(String.valueOf(file.getId()));
        PageResult<DccControlledFileLogRespVO> page = logQueryService.getLogPage(req);

        assertEquals(1L, page.getTotal());
        DccControlledFileLogRespVO row = page.getList().get(0);
        assertEquals("PROJECT_CODE_CHANGE", row.getLogType());
        assertEquals("DOC-1201", row.getFileNumber());
        assertEquals("用户2201", row.getOperatorName());
        assertEquals("文件类别 III / 修正后类别", row.getSummary());
        assertEquals("修正后类别", row.getNewValueText());
        assertTrue(row.getDetailJson().contains("\"assignmentNo\":\"\""));
        assertTrue(row.getDetailJson().contains("\"fieldName\":\"fileTypeLevel3\""));
    }

    @Test
    void getLogPage_includesCheckoutCheckinAndCancelReasons() {
        DccControlledFileDO file = insertLifecycleFile(1301L, "DOC-1301", "检出SOP", "A/1",
                "NEW", "ACTIVE", LocalDateTime.of(2026, 8, 3, 9, 0), null, null, null, null);
        checkoutMapper.insert(DccControlledFileCheckoutDO.builder()
                .id(7301L).masterId(file.getMasterId()).baseIterationId(file.getId()).actorId(2201L)
                .reason("更新步骤").baseSourceSha256("old-sha").status("CANCELLED")
                .cancelReason("不再修改").cancelledTime(LocalDateTime.of(2026, 8, 3, 10, 0)).build());

        DccControlledFileLogPageReqVO req = new DccControlledFileLogPageReqVO();
        req.setLogType("FILE_CHECKOUT");
        req.setControlledFileId(String.valueOf(file.getId()));
        req.setMasterId(file.getMasterId());
        req.setVersionNo("A/1");
        PageResult<DccControlledFileLogRespVO> page = logQueryService.getLogPage(req);

        assertEquals(1L, page.getTotal());
        DccControlledFileLogRespVO row = page.getList().get(0);
        assertEquals("撤销检出", row.getActionLabel());
        assertEquals("不再修改", row.getReason());
        assertTrue(row.getDetailJson().contains("CANCELLED"));
    }

    @Test
    void getLogPage_preservesNineteenDigitControlledFileIdAtQueryBoundary() {
        long fileId = 2054545668044084022L;
        DccControlledFileDO file = insertLifecycleFile(fileId, "DOC-19-DIGIT", "19位受控文件", "B/1",
                "OBSOLETE", "OBSOLETE", LocalDateTime.of(2026, 8, 4, 9, 0),
                LocalDateTime.of(2026, 8, 4, 10, 0), LocalDateTime.of(2026, 8, 4, 11, 0),
                2201L, LocalDateTime.of(2026, 8, 4, 12, 0));

        DccControlledFileLogPageReqVO req = new DccControlledFileLogPageReqVO();
        req.setLogType("FILE_OBSOLETE");
        req.setControlledFileId(String.valueOf(file.getId()));

        PageResult<DccControlledFileLogRespVO> page = logQueryService.getLogPage(req);

        assertEquals(1L, page.getTotal());
        assertEquals("DOC-19-DIGIT", page.getList().get(0).getFileNumber());
    }

    @Test
    void getLogPage_withControlledFileIdUsesScopedSourceQueries() {
        long fileId = 2054545668044084024L;
        insertLifecycleFile(fileId, "DOC-SCOPED", "精确查询", "B/1",
                "OBSOLETE", "OBSOLETE", LocalDateTime.of(2026, 8, 5, 9, 0),
                LocalDateTime.of(2026, 8, 5, 10, 0), LocalDateTime.of(2026, 8, 5, 11, 0),
                2201L, LocalDateTime.of(2026, 8, 5, 12, 0));

        DccControlledFileLogPageReqVO req = new DccControlledFileLogPageReqVO();
        req.setControlledFileId(String.valueOf(fileId));
        req.setKeyword("DOC-SCOPED");

        PageResult<DccControlledFileLogRespVO> page = logQueryService.getLogPage(req);

        assertEquals(3L, page.getTotal());
        verify(controlledFileMapper, never()).selectList();
        verify(accessLogMapper, never()).selectList();
        verify(accessEventMapper, never()).selectList();
        verify(distributionMapper, never()).selectList();
        verify(checkoutMapper, never()).selectList();
        verify(assignmentMapper, never()).selectList();
        verify(assignmentFileMapper, never()).selectList();
        verify(changeMapper, never()).selectList();
        verify(changeItemMapper, never()).selectList();
        verify(trainingProgressMapper, never()).selectList();
    }

    @Test
    void getLogPage_usesScopedEventUserNameWhenAccessLogUserIsNullAndKeepsLogUserPriority() {
        long fileId = 2054545668044084026L;
        DccControlledFileDO file = insertControlledFile(fileId, "DOC-AUDIT-USER", "访问用户映射", "A/1");
        DccControlledFileDO otherFile = insertControlledFile(2054545668044084027L,
                "DOC-AUDIT-OTHER", "其他文件", "A/1");
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 28, 10, 0);

        DccControlledFileAccessEventDO eventUserFallback = DccControlledFileAccessEventDO.builder()
                .accessEventCode("AE-" + fileId + "-NULL-LOG-USER")
                .controlledFileId(fileId)
                .fileVersionNo(file.getVersionNo())
                .userId(2209L)
                .accessType("PREVIEW")
                .purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS")
                .occurredAt(occurredAt)
                .build();
        accessEventMapper.insert(eventUserFallback);
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(null)
                .accessEventId(eventUserFallback.getId())
                .accessEventCode(eventUserFallback.getAccessEventCode())
                .fileVersionNo(file.getVersionNo())
                .userId(null)
                .actionType("PREVIEW")
                .result("SUCCESS")
                .build());

        DccControlledFileAccessEventDO eventWithBothUsers = DccControlledFileAccessEventDO.builder()
                .accessEventCode("AE-" + fileId + "-LOG-USER-PRIORITY")
                .controlledFileId(fileId)
                .fileVersionNo(file.getVersionNo())
                .userId(2210L)
                .accessType("DOWNLOAD")
                .purpose("CONTROLLED_DOWNLOAD")
                .result("SUCCESS")
                .occurredAt(occurredAt.plusMinutes(1))
                .build();
        accessEventMapper.insert(eventWithBothUsers);
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(fileId)
                .accessEventId(eventWithBothUsers.getId())
                .accessEventCode(eventWithBothUsers.getAccessEventCode())
                .fileVersionNo(file.getVersionNo())
                .userId(2201L)
                .actionType("DOWNLOAD")
                .result("SUCCESS")
                .build());

        DccControlledFileAccessEventDO otherFileEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode("AE-OTHER-FILE")
                .controlledFileId(otherFile.getId())
                .fileVersionNo(otherFile.getVersionNo())
                .userId(2299L)
                .accessType("PREVIEW")
                .purpose("CONTROLLED_PREVIEW")
                .result("SUCCESS")
                .occurredAt(occurredAt.plusMinutes(2))
                .build();
        accessEventMapper.insert(otherFileEvent);
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(null)
                .accessEventId(otherFileEvent.getId())
                .accessEventCode(otherFileEvent.getAccessEventCode())
                .fileVersionNo(otherFile.getVersionNo())
                .userId(2298L)
                .actionType("PREVIEW")
                .result("SUCCESS")
                .build());

        DccControlledFileLogPageReqVO req = new DccControlledFileLogPageReqVO();
        req.setLogType("CONTROLLED_FILE_AUDIT");
        req.setControlledFileId(String.valueOf(fileId));
        PageResult<DccControlledFileLogRespVO> page = logQueryService.getLogPage(req);

        assertEquals(2L, page.getTotal());
        DccControlledFileLogRespVO eventFallbackRow = page.getList().stream()
                .filter(row -> row.getOperatorUserId().equals(2209L))
                .findFirst().orElseThrow();
        assertEquals("用户2209", eventFallbackRow.getOperatorName());
        DccControlledFileLogRespVO logUserPriorityRow = page.getList().stream()
                .filter(row -> row.getOperatorUserId().equals(2201L))
                .findFirst().orElseThrow();
        assertEquals("用户2201", logUserPriorityRow.getOperatorName());
        assertTrue(page.getList().stream().noneMatch(row -> row.getOperatorUserId().equals(2299L)));
        verify(accessEventMapper).selectListByControlledFileId(fileId);
        verify(accessEventMapper, never()).selectList();
        verify(accessLogMapper, never()).selectList();
    }

    @Test
    void getLogPage_rejectsInvalidControlledFileId() {
        DccControlledFileLogPageReqVO req = new DccControlledFileLogPageReqVO();
        req.setControlledFileId("2054545668044084022x");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> logQueryService.getLogPage(req));

        assertTrue(exception.getMessage().contains("controlledFileId"));
    }

    private DccControlledFileDO insertControlledFile(Long id, String fileNumber, String fileName, String versionNo) {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(id)
                .masterId(7000L + id)
                .categoryId(11L)
                .directoryId(31L)
                .sourceFileId(8000L + id)
                .originalFileId(9000L + id)
                .fileName(fileName)
                .title(fileName)
                .fileNumber(fileNumber)
                .processType("CONTROLLED_FILE")
                .versionNo(versionNo)
                .status("ACTIVE")
                .submitterId(100L)
                .requesterId(101L)
                .fileTypeLevel2("新阶段")
                .build();
        controlledFileMapper.insert(file);
        return file;
    }

    private DccControlledFileDO insertLifecycleFile(Long id, String fileNumber, String fileName, String versionNo,
                                                    String changeType, String status, LocalDateTime submittedTime,
                                                    LocalDateTime approvedTime, LocalDateTime publishedTime,
                                                    Long obsoletedBy, LocalDateTime obsoletedTime) {
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(id)
                .masterId(7100L + id)
                .categoryId(12L)
                .directoryId(32L)
                .sourceFileId(8100L + id)
                .originalFileId(9100L + id)
                .fileName(fileName)
                .title(fileName)
                .fileNumber(fileNumber)
                .processType("CONTROLLED_FILE")
                .changeType(changeType)
                .versionNo(versionNo)
                .status(status)
                .submitterId(2101L)
                .requesterId(2102L)
                .dccProjectCodeId(3101L)
                .submittedTime(submittedTime)
                .approvedTime(approvedTime)
                .publishedTime(publishedTime)
                .obsoletedBy(obsoletedBy)
                .obsoletedTime(obsoletedTime)
                .obsoleteReason("版本下线")
                .build();
        controlledFileMapper.insert(file);
        return file;
    }

    private void insertDistribution(DccControlledFileDO file, Long id, Long departmentId, String medium, String status,
                                    Long acknowledgedBy, LocalDateTime acknowledgedAt,
                                    Long recoveredBy, LocalDateTime recoveredAt) {
        distributionMapper.insert(DccControlledFileDistributionDO.builder()
                .id(id)
                .controlledFileId(file.getId())
                .departmentId(departmentId)
                .distributionMedium(medium)
                .status(status)
                .acknowledgedBy(acknowledgedBy)
                .acknowledgedAt(acknowledgedAt)
                .recoveredBy(recoveredBy)
                .recoveredAt(recoveredAt)
                .build());
    }

    private void insertAccessAudit(DccControlledFileDO file, Long userId, String actionType, String result,
                                   LocalDateTime occurredAt) {
        DccControlledFileAccessEventDO accessEvent = DccControlledFileAccessEventDO.builder()
                .accessEventCode("AE-" + file.getId())
                .controlledFileId(file.getId())
                .fileVersionNo(file.getVersionNo())
                .userId(userId)
                .accessType(actionType)
                .purpose("CONTROLLED_PREVIEW")
                .result(result)
                .requestId("REQ-" + file.getId())
                .occurredAt(occurredAt)
                .build();
        accessEventMapper.insert(accessEvent);
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(file.getId())
                .accessEventId(accessEvent.getId())
                .accessEventCode(accessEvent.getAccessEventCode())
                .fileVersionNo(file.getVersionNo())
                .userId(userId)
                .actionType(actionType)
                .purpose("CONTROLLED_PREVIEW")
                .result(result)
                .requestId(accessEvent.getRequestId())
                .build());
    }

    private DccProjectCodeDO insertProjectCode(Long id, String projectName, String projectCodeText) {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(id)
                .projectName(projectName)
                .projectCode(projectCodeText)
                .status("ENABLE")
                .build();
        projectCodeMapper.insert(projectCode);
        return projectCode;
    }

    private DccProjectCodeAssignmentDO insertAssignment(DccProjectCodeDO projectCode, Long assigneeUserId,
                                                        LocalDateTime assignedTime) {
        DccProjectCodeAssignmentDO assignment = DccProjectCodeAssignmentDO.builder()
                .id(4001L)
                .assignmentNo("DCC-A-4001")
                .projectCodeId(projectCode.getId())
                .scopeMode("PROJECT_CODE_CURRENT_FILES")
                .assigneeUserId(assigneeUserId)
                .assignedBy(1999L)
                .assignedTime(assignedTime)
                .status("ACTIVE")
                .assignmentReason("修正项目代码")
                .fileCount(1)
                .changedFileCount(0)
                .changedFieldCount(0)
                .build();
        assignmentMapper.insert(assignment);
        return assignment;
    }

    private void insertProjectCodeChange(DccControlledFileDO file, DccProjectCodeDO projectCode,
                                         DccProjectCodeAssignmentDO assignment, Long operatorUserId,
                                         LocalDateTime changedTime) {
        DccControlledFileMetadataChangeDO change = DccControlledFileMetadataChangeDO.builder()
                .id(5001L)
                .assignmentId(assignment.getId())
                .projectCodeId(projectCode.getId())
                .controlledFileId(file.getId())
                .masterId(file.getMasterId())
                .operatorUserId(operatorUserId)
                .source("ASSIGNMENT_USER")
                .changeReason("纠正阶段")
                .changedFieldCount(1)
                .changedTime(changedTime)
                .build();
        changeMapper.insert(change);
        changeItemMapper.insert(DccControlledFileMetadataChangeItemDO.builder()
                .id(5002L)
                .changeId(change.getId())
                .assignmentId(assignment.getId())
                .projectCodeId(projectCode.getId())
                .controlledFileId(file.getId())
                .operatorUserId(operatorUserId)
                .fieldName("fileTypeLevel2")
                .fieldLabel("文件类别 II")
                .oldValueText("旧阶段")
                .newValueText("新阶段")
                .changedTime(changedTime)
                .build());
    }

    private void insertTrainingProgress(DccControlledFileDO file, Long userId, LocalDateTime acknowledgedAt) {
        trainingProgressMapper.insert(DccControlledFileTrainingProgressDO.builder()
                .id(6001L)
                .controlledFileId(file.getId())
                .userId(userId)
                .requiredViewSeconds(600)
                .accumulatedViewSeconds(600)
                .firstViewedAt(acknowledgedAt.minusMinutes(20))
                .lastViewedAt(acknowledgedAt.minusMinutes(1))
                .acknowledgedAt(acknowledgedAt)
                .build());
    }
}
