package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_OLD_VALUE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import({MesProBatchRecordExecutionFieldAuditServiceImpl.class,
        MesProEdhrPreReleaseEditabilityService.class,
        MesProEdhrWorkTaskServiceImpl.class,
        MesProBatchRecordExecutionAttachmentServiceImpl.class,
        MesProEdhrGoldenFingerPermissionService.class})
class MesProBatchRecordExecutionFieldAuditServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;
    private static final String FIELD_PATH = "sheet[0].rows[1].cells[2].temperature";

    @Resource
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper batchMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditItemMapper itemMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;

    private MockedStatic<SecurityFrameworkUtils> securityMock;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;

    @MockitoBean
    private MesProBatchRecordExecutionSignatureService signatureService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProEdhrPermissionGateService permissionGateService;
    @MockitoBean
    private MesProEdhrCandidateResolver candidateResolver;
    @MockitoBean
    private MesProBatchRecordExecutionFieldResponsibilityService responsibilityService;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private NotifyMessageSendApi notifyMessageSendApi;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private RoleApi roleApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private MesProEdhrRecordbookGlobalSettingService recordbookGlobalSettingService;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
        securityMock = mockStatic(SecurityFrameworkUtils.class);
        securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
    }

    @AfterEach
    void clearTenant() {
        verifyNoInteractions(candidateResolver, responsibilityService);
        if (securityMock != null) {
            securityMock.close();
        }
        TenantContextHolder.clear();
    }

    @Test
    void saveChanges_writesAuditBeforeUpdatingCellProjection() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6"))));

        assertNotNull(result.getAuditBatchId());
        assertEquals(501L, result.getSignatureId());
        assertEquals(1L, result.getFieldAuditRevision());
        assertEquals("VALID", result.getHashVerification().getStatus().name());
        assertEquals(1, result.getChangedFieldCount());

        MesProBatchRecordExecutionDO updated = executionMapper.selectById(execution.getId());
        assertEquals(result.getCellValuesHash(), updated.getCellValuesHash());
        assertEquals(result.getFieldAuditHeadHash(), updated.getFieldAuditHeadHash());
        assertEquals(result.getAuditBatchId(), updated.getFieldAuditLastBatchId());
        assertEquals(1L, updated.getFieldAuditRevision());
        assertNotEquals(beforeHash, updated.getCellValuesHash());

        MesProBatchRecordExecutionFieldAuditBatchDO batch = batchMapper.selectById(result.getAuditBatchId());
        assertEquals("FIELD_CHANGE", batch.getActionType());
        assertEquals("CORRECTION", batch.getReasonCategory());
        assertEquals(beforeHash, batch.getBeforeCellValuesHash());
        assertEquals(result.getCellValuesHash(), batch.getAfterCellValuesHash());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH, batch.getPreviousHeadHash());
        assertEquals(result.getFieldAuditHeadHash(), batch.getNewHeadHash());

        List<MesProBatchRecordExecutionFieldAuditItemDO> items = itemMapper.selectListByBatchId(result.getAuditBatchId());
        assertEquals(1, items.size());
        MesProBatchRecordExecutionFieldAuditItemDO item = items.get(0);
        assertEquals(FIELD_PATH, item.getFieldPath());
        assertEquals("temperature", item.getFieldKey());
        assertEquals("36.6", item.getOldValueJson());
        assertEquals("37.5", item.getNewValueJson());
        assertEquals("36.6", item.getOldValueDisplay());
        assertEquals("37.5", item.getNewValueDisplay());
        assertEquals("CORRECTION", item.getReasonCategory());
        assertEquals("operator correction", item.getReasonText());
        assertEquals(99L, item.getActorId());
        assertEquals("QA", item.getActorName());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH, item.getPreviousHash());
        assertEquals(result.getFieldAuditHeadHash(), item.getAuditHash());

        verify(permissionGateService, never()).requireAbility(any());
        verify(signatureService).attachFieldChangeSignature(any());
    }

    @Test
    void saveChanges_withoutSignatureRecordsLoginSessionDraftSaveEvidence() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeDraftSave();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-draft-save-no-signature",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setSignature(null);

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        assertEquals(601L, result.getSignatureId());
        assertEquals(1, result.getChangedFieldCount());
        MesProBatchRecordExecutionSignatureDO signature = signatureMapper.selectById(result.getSignatureId());
        assertEquals(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_LOGIN_SESSION,
                signature.getSignatureMode());
        assertEquals(Boolean.FALSE, signature.getPasswordVerified());
        verify(signatureService).recordFieldChangeDraftSave(any());
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveSystemCellLinkChanges_persistsAutoPrefillWithoutWorkTaskValidation() {
        String beforeJson = "[]";
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeDraftSave();

        MesProBatchRecordExecutionFieldAuditSaveResult result =
                fieldAuditService.saveSystemCellLinkChanges(new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                        .setExecutionId(execution.getId())
                        .setIdempotencyKey("cell-link-auto-prefill-system")
                        .setBaseCellValuesHash(beforeHash)
                        .setBaseFieldAuditRevision(0L)
                        .setBaseFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                        .setReasonCategory("OTHER")
                        .setReasonText("系统根据单元格链接自动预填生产批号")
                        .setChanges(List.of(new MesProBatchRecordExecutionFieldAuditChange()
                                .setFieldPath(FIELD_PATH)
                                .setFieldKey("temperature")
                                .setRowIndex(1)
                                .setColumnIndex(2)
                                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                                .setNewValueJson(new BigDecimal("37.5"))
                                .setNewValueDisplay("37.5")
                                .setExpectedOldValueHash(MesProBatchRecordExecutionFieldAuditHasher
                                        .hashCanonicalTypedValue("null")))));

        assertEquals(1L, result.getFieldAuditRevision());
        assertEquals(1, result.getChangedFieldCount());
        assertEquals("VALID", result.getHashVerification().getStatus().name());

        MesProBatchRecordExecutionDO updated = executionMapper.selectById(execution.getId());
        assertEquals(result.getCellValuesHash(), updated.getCellValuesHash());
        assertEquals(result.getFieldAuditHeadHash(), updated.getFieldAuditHeadHash());
        assertEquals(1L, updated.getFieldAuditRevision());

        List<MesProBatchRecordExecutionFieldAuditBatchDO> batches =
                batchMapper.selectListByExecutionId(execution.getId());
        assertEquals(1, batches.size());
        assertEquals("OTHER", batches.get(0).getReasonCategory());
        assertEquals("系统根据单元格链接自动预填生产批号", batches.get(0).getReasonText());
        verify(signatureService).recordFieldChangeDraftSave(any());
        verify(signatureService).attachFieldChangeSignature(any());
        verifyNoInteractions(candidateResolver, responsibilityService);
    }

    @Test
    void saveChanges_withValidFillWorkTaskDoesNotRequireExecutionScopeFill() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();
        doAnswer(invocation -> {
            throw new IllegalStateException("valid fill task should authorize field audit save without execution scope FILL");
        }).when(permissionGateService).requireAbility(argThat(command -> "FILL".equals(command.getAbility())));

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-fill-task-authorizes-save",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6"))));

        assertNotNull(result.getAuditBatchId());
        assertEquals(1, result.getChangedFieldCount());
        verify(permissionGateService, never()).requireAbility(any());
        verify(signatureService).attachFieldChangeSignature(any());
    }

    @Test
    void saveChanges_withSelectedSignatureTimePassesTimeCommandAndStoresDualTimeSignature() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();
        LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 14, 5);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-signature-time-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        command.getSignature().setSignatureTimeCommand(new MesProBatchRecordExecutionSignatureTimeCommand()
                .setSelectedSignedAt(selectedSignedAt)
                .setSelectedTimeZone("Asia/Shanghai")
                .setSelectedTimeReason("字段变更按纸面更正时间显示"));

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        ArgumentCaptor<MesProBatchRecordExecutionFieldAuditSignatureCommand> signatureCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionFieldAuditSignatureCommand.class);
        verify(signatureService).recordFieldChangeSignature(signatureCaptor.capture());
        assertEquals(selectedSignedAt, signatureCaptor.getValue().getSignatureTimeCommand().getSelectedSignedAt());
        assertEquals("Asia/Shanghai", signatureCaptor.getValue().getSignatureTimeCommand().getSelectedTimeZone());
        assertEquals("字段变更按纸面更正时间显示",
                signatureCaptor.getValue().getSignatureTimeCommand().getSelectedTimeReason());

        MesProBatchRecordExecutionSignatureDO signature = signatureMapper.selectById(result.getSignatureId());
        assertEquals(selectedSignedAt, signature.getSelectedSignedAt());
        assertEquals(selectedSignedAt, signature.getSignatureDisplayAt());
        assertEquals("USER_SELECTED", signature.getSignatureTimeMode());
        assertEquals("Asia/Shanghai", signature.getSelectedTimeZone());
        assertEquals("字段变更按纸面更正时间显示", signature.getSelectedTimeReason());
        assertEquals("EDHR_SIGNATURE_TIME_V1", signature.getSelectedTimePolicyVersion());
        assertNotNull(signature.getSelectedTimeAuditHash());
        verify(operationAuditService).record(argThat(audit ->
                "FIELD_CHANGE".equals(audit.getOperationType())
                        && "FIELD_AUDIT_BATCH".equals(audit.getObjectType())
                        && String.valueOf(result.getAuditBatchId()).equals(audit.getObjectId())
                        && "SUCCESS".equals(audit.getResultStatus())));
    }

    @Test
    void saveChanges_multiUserSameFormKeepsActualActorSignatureAndHashSeparated() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        mockFieldChangeSignature(99L, "QA", 501L, LocalDateTime.of(2026, 5, 26, 10, 30));
        MesProBatchRecordExecutionFieldAuditSaveResult first = fieldAuditService.saveChanges(
                saveCommand(execution, MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson),
                        "idem-t7-user-a", new BigDecimal("36.6"),
                        MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6"))));

        securityMock.close();
        securityMock = mockStatic(SecurityFrameworkUtils.class);
        securityMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
        reset(signatureService);
        mockFieldChangeSignature(188L, "生产员B", 502L, LocalDateTime.of(2026, 5, 26, 10, 45));
        MesProBatchRecordExecutionDO afterFirst = executionMapper.selectById(execution.getId());
        MesProEdhrWorkTaskDO secondWorkTask = insertFillWorkTask(afterFirst, 188L, MesProEdhrWorkTaskStatus.TODO);
        MesProBatchRecordExecutionFieldAuditSaveResult second = fieldAuditService.saveChanges(
                saveCommand(afterFirst, afterFirst.getCellValuesHash(), "idem-t7-user-b",
                        new BigDecimal("37.5"),
                        MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("37.5")))
                        .setBaseFieldAuditRevision(afterFirst.getFieldAuditRevision())
                        .setBaseFieldAuditHeadHash(afterFirst.getFieldAuditHeadHash())
                        .setWorkTaskId(secondWorkTask.getId())
                        .setChanges(List.of(new MesProBatchRecordExecutionFieldAuditChange()
                                .setFieldPath(FIELD_PATH)
                                .setFieldKey("temperature")
                                .setRowIndex(1)
                                .setColumnIndex(2)
                                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                                .setNewValueJson(new BigDecimal("38.0"))
                                .setNewValueDisplay("38.0")
                                .setExpectedOldValueJson(new BigDecimal("37.5"))
                                .setExpectedOldValueHash(MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("37.5"))))));

        List<MesProBatchRecordExecutionFieldAuditItemDO> firstItems =
                itemMapper.selectListByBatchId(first.getAuditBatchId());
        List<MesProBatchRecordExecutionFieldAuditItemDO> secondItems =
                itemMapper.selectListByBatchId(second.getAuditBatchId());
        assertEquals(1, firstItems.size());
        assertEquals(1, secondItems.size());
        assertEquals(99L, firstItems.get(0).getActorId());
        assertEquals("QA", firstItems.get(0).getActorName());
        assertEquals(501L, firstItems.get(0).getSignatureId());
        assertEquals(188L, secondItems.get(0).getActorId());
        assertEquals("生产员B", secondItems.get(0).getActorName());
        assertEquals(502L, secondItems.get(0).getSignatureId());
        assertEquals(first.getFieldAuditHeadHash(), secondItems.get(0).getPreviousHash());
        assertNotEquals(firstItems.get(0).getAuditHash(), secondItems.get(0).getAuditHash());
    }

    @Test
    void saveChanges_rejectsWhenLoginUserIsNotFillTaskAssignee() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(execution, 188L, MesProEdhrWorkTaskStatus.TODO);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-t6-no-permission",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setWorkTaskId(workTask.getId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fieldAuditService.saveChanges(command));
        assertEquals(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH.getCode(), exception.getCode());

        verify(signatureService, never()).recordFieldChangeSignature(any());
        assertTrue(batchMapper.selectList().isEmpty());
    }

    @Test
    void saveChanges_rejectsWhenFillTaskIsNotWritable() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(execution, 99L, MesProEdhrWorkTaskStatus.DONE);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-t6-not-writable",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setWorkTaskId(workTask.getId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fieldAuditService.saveChanges(command));
        assertEquals(PRO_EDHR_WORK_TASK_STATUS_INVALID.getCode(), exception.getCode());

        verify(signatureService, never()).recordFieldChangeSignature(any());
        assertTrue(batchMapper.selectList().isEmpty());
    }

    @Test
    void saveChanges_fillCompletedOrdinaryBeforeReleaseAllowsHistoricalDoneFillTask() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED));
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(execution, 99L, MesProEdhrWorkTaskStatus.DONE);
        insertPreReleaseBatchTask(execution, 8101L, 8201L,
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-pre-release-done",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setWorkTaskId(workTask.getId()));

        assertNotNull(result.getAuditBatchId());
        assertEquals(1L, result.getFieldAuditRevision());
        assertNotEquals(beforeHash, result.getCellValuesHash());
        verify(signatureService).attachFieldChangeSignature(any());
    }

    @Test
    void saveChanges_fillCompletedOrdinaryPendingReleaseAllowsBeforeBatchClose() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED));
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(execution, 99L, MesProEdhrWorkTaskStatus.DONE);
        Long batchExecutionId = 8301L;
        insertPreReleaseBatchTask(execution, batchExecutionId, 8401L,
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-FIELD-AUDIT-LOCK")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-pre-release-pending",
                        new BigDecimal("36.6"),
                        MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setWorkTaskId(workTask.getId()));

        assertNotNull(result.getAuditBatchId());
        assertEquals(1L, result.getFieldAuditRevision());
        assertNotEquals(beforeHash, result.getCellValuesHash());
        verify(signatureService).attachFieldChangeSignature(any());
    }

    @Test
    void saveChanges_rejectsWhenExecutionIsNotCurrentActiveFormForTask() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(execution, 99L, MesProEdhrWorkTaskStatus.TODO)
                .setExecutionId(execution.getId() + 1000L);
        workTaskMapper.updateById(workTask);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-t6-not-current-active",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setWorkTaskId(workTask.getId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fieldAuditService.saveChanges(command));
        assertEquals(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("当前活动表单"));

        verify(signatureService, never()).recordFieldChangeSignature(any());
        assertTrue(batchMapper.selectList().isEmpty());
    }

    @Test
    void saveChanges_rejectsBatchSharedOutOfScopeRowBeforeSignature() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setMetaJson(JsonUtils.toJsonString(Map.of("sourceTableIndex", 0))));
        insertSharedBatchExecutionTask(execution, JsonUtils.toJsonString(Map.of("ranges", List.of(Map.of(
                "sourceTableIndex", 0,
                "startRow", 0,
                "endRow", 1
        )))));
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-batch-shared-out-of-scope",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        command.getChanges().get(0)
                .setFieldPath("sheet[0].rows[2].cells[2].temperature")
                .setRowIndex(2);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fieldAuditService.saveChanges(command));
        assertEquals(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID.getCode(), exception.getCode());

        MesProBatchRecordExecutionDO unchanged = executionMapper.selectById(execution.getId());
        assertEquals(beforeJson, unchanged.getCellValuesJson());
        assertEquals(beforeHash, unchanged.getCellValuesHash());
        assertTrue(batchMapper.selectListByExecutionId(execution.getId()).isEmpty());
        assertTrue(itemMapper.selectListByExecutionId(execution.getId()).isEmpty());
        verify(signatureService, never()).recordFieldChangeSignature(any());
        verify(signatureService, never()).attachFieldChangeSignature(any());
    }

    @Test
    void saveChanges_goldenFingerBypassesAssigneeAndSharedScopeAndRecordsAudit() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setMetaJson(JsonUtils.toJsonString(Map.of("sourceTableIndex", 0))));
        insertSharedBatchExecutionTask(execution, JsonUtils.toJsonString(Map.of("ranges", List.of(Map.of(
                "sourceTableIndex", 0,
                "startRow", 0,
                "endRow", 0
        )))));
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-golden-finger-shared-scope",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(command.getWorkTaskId())
                .setAssigneeUserId(188L);
        workTaskMapper.updateById(workTask);
        grantGoldenFingerPermission(99L);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        assertNotNull(result.getAuditBatchId());
        assertEquals(1, result.getChangedFieldCount());
        assertNotEquals(beforeHash, result.getCellValuesHash());
        verify(signatureService).attachFieldChangeSignature(any());
        verify(operationAuditService).record(argThat(audit ->
                "FIELD_CHANGE".equals(audit.getOperationType())
                        && MesProEdhrGoldenFingerPermissionService.PERMISSION.equals(audit.getPermissionCode())
                        && "ALLOW_GOLDEN_FINGER".equals(audit.getPermissionDecision())
                        && audit.getMetadataJson() != null
                        && audit.getMetadataJson().contains("\"goldenFingerMode\":true")
                        && audit.getMetadataJson().contains("FILL_SCOPE")));
    }

    @Test
    void saveChanges_goldenFingerBypassesPendingReleaseLock() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED));
        MesProEdhrWorkTaskDO workTask = insertFillWorkTask(execution, 188L, MesProEdhrWorkTaskStatus.DONE);
        Long batchExecutionId = 8501L;
        insertPreReleaseBatchTask(execution, batchExecutionId, 8502L,
                MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS);
        releaseTransactionMapper.insert(new MesProEdhrReleaseTransactionDO()
                .setBatchExecutionId(batchExecutionId)
                .setReleaseCode("REL-GF-FIELD-AUDIT-LOCK")
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        grantGoldenFingerPermission(99L);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-golden-finger-pending-release", new BigDecimal("36.6"),
                        MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")))
                        .setWorkTaskId(workTask.getId()));

        assertNotNull(result.getAuditBatchId());
        assertEquals(1, result.getChangedFieldCount());
        verify(signatureService).attachFieldChangeSignature(any());
        verify(operationAuditService).record(argThat(audit ->
                "FIELD_CHANGE".equals(audit.getOperationType())
                        && MesProEdhrGoldenFingerPermissionService.PERMISSION.equals(audit.getPermissionCode())
                        && "ALLOW_GOLDEN_FINGER".equals(audit.getPermissionDecision())
                        && audit.getMetadataJson() != null
                        && audit.getMetadataJson().contains("ACTION_LOCKS")));
    }

    @Test
    void saveChanges_writesTypedNullWithoutMixingEmptyString() throws Exception {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", ""
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-null-001",
                        "", MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("\"\""));
        command.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.NULL)
                .setNewValueJson(null)
                .setNewValueDisplay("null");

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        List<MesProBatchRecordExecutionFieldAuditItemDO> items = itemMapper.selectListByBatchId(result.getAuditBatchId());
        assertEquals(1, items.size());
        MesProBatchRecordExecutionFieldAuditItemDO item = items.get(0);
        assertEquals(MesProBatchRecordExecutionFieldAuditValueType.NULL.name(), item.getValueType());
        assertEquals("\"\"", item.getOldValueJson());
        assertEquals("null", item.getNewValueJson());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("\"\""),
                item.getOldValueHash());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("null"),
                item.getNewValueHash());
        JsonNode savedValue = JsonUtils.getObjectMapper()
                .readTree(executionMapper.selectById(execution.getId()).getCellValuesJson())
                .get(0).get("value");
        assertTrue(savedValue.isNull());
    }

    @Test
    void saveChanges_validatesSnapshotRuleAndPersistsTypedProjectionWithUnitAndValueHash() throws Exception {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "valueType", "NUMBER",
                "value", new BigDecimal("36.6"),
                "valueDisplay", "36.6",
                "valueHash", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")),
                "unit", "℃"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRule(beforeJson,
                "NUMBER", Map.of("min", 0, "max", 100, "scale", 1), "℃", true);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-rule-number-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6"))));

        JsonNode savedCell = JsonUtils.getObjectMapper()
                .readTree(executionMapper.selectById(execution.getId()).getCellValuesJson())
                .get(0);
        assertEquals("NUMBER", savedCell.get("valueType").asText());
        assertEquals("37.5", savedCell.get("value").decimalValue().stripTrailingZeros().toPlainString());
        assertEquals("37.5", savedCell.get("valueDisplay").asText());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("37.5")),
                savedCell.get("valueHash").asText());
        assertEquals("℃", savedCell.get("unit").asText());
        assertEquals(1, result.getChangedFieldCount());
    }

    @Test
    void saveChanges_toleratesSignatureSnapshotFieldsWhenSavingTypedCells() throws Exception {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "valueType", "NUMBER",
                "value", new BigDecimal("36.6"),
                "valueDisplay", "36.6",
                "valueHash", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")),
                "unit", "℃"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRuleAndSignature(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-signature-snapshot-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6"))));

        JsonNode savedCell = JsonUtils.getObjectMapper()
                .readTree(executionMapper.selectById(execution.getId()).getCellValuesJson())
                .get(0);
        assertEquals("NUMBER", savedCell.get("valueType").asText());
        assertEquals("37.5", savedCell.get("valueDisplay").asText());
        assertEquals(1, result.getChangedFieldCount());
    }

    @Test
    void saveChanges_rejectsSignatureCellAsOrdinaryFieldValue() {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithTypedSnapshotDefault(
                "[]", "SIGNATURE", "signature", "");
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-signature-cell-forbidden", null, null);
        command.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.SIGNATURE)
                .setNewValueJson(Map.of("actorName", "admin", "signedAt", "2026-06-24 00:00:00"))
                .setNewValueDisplay("admin\n2026-06-24 00:00:00");

        assertServiceException(() -> fieldAuditService.saveChanges(command),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_SIGNATURE_CELL_VALUE_FORBIDDEN);
        verify(signatureService, never()).recordFieldChangeSignature(any());

        MesProBatchRecordExecutionDO unchanged = executionMapper.selectById(execution.getId());
        assertEquals("[]", unchanged.getCellValuesJson());
        assertEquals(beforeHash, unchanged.getCellValuesHash());
    }

    @Test
    void saveChanges_normalizesEmptyBooleanBaselineBeforeTypedSave() {
        MesProBatchRecordExecutionDO booleanExecution = insertDraftExecutionWithTypedSnapshotDefault(
                "[]", "BOOLEAN", "checkbox", "");
        String booleanBeforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand booleanCommand =
                saveCommand(booleanExecution, booleanBeforeHash, "idem-empty-boolean-001",
                        false, MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN, false));
        booleanCommand.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.BOOLEAN)
                .setNewValueJson(true)
                .setNewValueDisplay("true");

        MesProBatchRecordExecutionFieldAuditSaveResult booleanResult =
                fieldAuditService.saveChanges(booleanCommand);

        assertEquals(1, booleanResult.getChangedFieldCount());
    }

    @Test
    void saveChanges_normalizesEmptyDateBaselineBeforeTypedSave() {
        MesProBatchRecordExecutionDO dateExecution = insertDraftExecutionWithTypedSnapshotDefault(
                "[]", "DATE", "date", "");
        String dateBeforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand dateCommand =
                saveCommand(dateExecution, dateBeforeHash, "idem-empty-date-001",
                        null, MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("null"));
        dateCommand.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.DATE)
                .setNewValueJson("2026-06-10")
                .setNewValueDisplay("2026-06-10");

        MesProBatchRecordExecutionFieldAuditSaveResult dateResult =
                fieldAuditService.saveChanges(dateCommand);

        assertEquals(1, dateResult.getChangedFieldCount());
    }

    @Test
    void saveChanges_rejectsValueTypeMismatchOrConstraintViolationBeforeSignature() {
        MesProBatchRecordExecutionDO numberExecution = insertDraftExecutionWithCellRule("[]",
                "NUMBER", Map.of("min", 0, "max", 100, "scale", 1), "℃", true);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand mismatch =
                saveCommand(numberExecution, beforeHash, "idem-rule-mismatch", null, null);
        mismatch.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.STRING)
                .setNewValueJson("37.5")
                .setNewValueDisplay("37.5");

        assertServiceException(() -> fieldAuditService.saveChanges(mismatch),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);

        MesProBatchRecordExecutionFieldAuditSaveChangesCommand outOfRange =
                saveCommand(numberExecution, beforeHash, "idem-rule-out-of-range", null, null);
        outOfRange.getChanges().get(0)
                .setNewValueJson(new BigDecimal("101.0"))
                .setNewValueDisplay("101.0");

        ServiceException outOfRangeException = assertThrows(ServiceException.class,
                () -> fieldAuditService.saveChanges(outOfRange));
        assertEquals(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION.getCode(),
                outOfRangeException.getCode());
        assertTrue(outOfRangeException.getMessage().contains("大于最大值 100"));
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_internalTraceIgnoresNumberMinMaxButKeepsAuditAndSignature() throws Exception {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRule("[]",
                "NUMBER", Map.of("min", 0, "max", 100, "scale", 1), "℃", true)
                .setRecordCategory("INTERNAL_RECORD")
                .setValidationProfile("INTERNAL_TRACE");
        executionMapper.updateById(execution);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-internal-trace-out-of-range", null, null);
        command.getChanges().get(0)
                .setNewValueJson(new BigDecimal("101.0"))
                .setNewValueDisplay("101.0");

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        assertEquals(1, result.getChangedFieldCount());
        assertEquals(501L, result.getSignatureId());
        MesProBatchRecordExecutionDO updated = executionMapper.selectById(execution.getId());
        assertEquals("INTERNAL_RECORD", updated.getRecordCategory());
        assertEquals("INTERNAL_TRACE", updated.getValidationProfile());
        JsonNode savedCell = JsonUtils.getObjectMapper()
                .readTree(updated.getCellValuesJson())
                .get(0);
        assertEquals("101", savedCell.get("value").decimalValue().stripTrailingZeros().toPlainString());
        verify(signatureService).recordFieldChangeSignature(any());
        verify(operationAuditService).record(argThat(audit ->
                "FIELD_CHANGE".equals(audit.getOperationType())
                        && "FIELD_AUDIT_BATCH".equals(audit.getObjectType())
                        && String.valueOf(result.getAuditBatchId()).equals(audit.getObjectId())
                        && "INTERNAL_RECORD".equals(audit.getRecordCategory())
                        && "SUCCESS".equals(audit.getResultStatus())
                        && containsInternalTraceNonBlockingLimitMetadata(audit)));
    }

    @ParameterizedTest
    @CsvSource({
            "50, 40",
            "30, 30",
            "10, 20"
    })
    void saveChanges_recordbookModeClampsNumberAndStoresRecordbookAndBatchValues(
            String recordbookValue, String expectedBatchRecordValue) throws Exception {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRule("[]",
                "NUMBER", Map.of("min", 20, "max", 40), "℃", false)
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRecordbookEnabled(Boolean.TRUE);
        executionMapper.updateById(execution);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-recordbook-clamp-" + recordbookValue, null, null)
                        .setFillCarrier("RECORDBOOK")
                        .setFillMode("RECORDBOOK_UNRESTRICTED");
        command.getChanges().get(0)
                .setNewValueJson(new BigDecimal(recordbookValue))
                .setNewValueDisplay(recordbookValue);

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        assertEquals(1, result.getChangedFieldCount());
        MesProBatchRecordExecutionDO updated = executionMapper.selectById(execution.getId());
        JsonNode savedCell = JsonUtils.getObjectMapper()
                .readTree(updated.getCellValuesJson())
                .get(0);
        assertEquals(expectedBatchRecordValue,
                savedCell.get("value").decimalValue().stripTrailingZeros().toPlainString());
        assertEquals(expectedBatchRecordValue, savedCell.get("valueDisplay").asText());
        List<MesProBatchRecordExecutionFieldAuditItemDO> items = itemMapper.selectListByBatchId(result.getAuditBatchId());
        assertEquals(1, items.size());
        MesProBatchRecordExecutionFieldAuditItemDO item = items.get(0);
        assertEquals(recordbookValue, item.getRecordbookValueJson());
        assertEquals(recordbookValue, item.getRecordbookValueDisplay());
        assertEquals(expectedBatchRecordValue, item.getBatchRecordValueJson());
        assertEquals(expectedBatchRecordValue, item.getBatchRecordValueDisplay());
        assertEquals(expectedBatchRecordValue, item.getNewValueJson());
        assertEquals(expectedBatchRecordValue, item.getNewValueDisplay());
        verify(operationAuditService).record(argThat(audit ->
                "FIELD_CHANGE".equals(audit.getOperationType())
                        && "BATCH_RECORD".equals(audit.getRecordCategory())
                        && !audit.getMetadataJson().contains("nonBlockingLimitWarnings")));
    }

    @Test
    void saveChanges_recordbookModeKeepsNonNumberValueUnchanged() throws Exception {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRule("[]",
                "STRING", Map.of("maxLength", 2), "", false)
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRecordbookEnabled(Boolean.TRUE);
        executionMapper.updateById(execution);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-recordbook-text", null, null)
                        .setFillCarrier("RECORDBOOK")
                        .setFillMode("RECORDBOOK_UNRESTRICTED");
        command.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.STRING)
                .setNewValueJson("abc")
                .setNewValueDisplay("abc");

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        MesProBatchRecordExecutionFieldAuditItemDO item =
                itemMapper.selectListByBatchId(result.getAuditBatchId()).get(0);
        assertEquals("\"abc\"", item.getRecordbookValueJson());
        assertEquals("abc", item.getRecordbookValueDisplay());
        assertEquals("\"abc\"", item.getBatchRecordValueJson());
        assertEquals("abc", item.getBatchRecordValueDisplay());
        assertEquals("\"abc\"", item.getNewValueJson());
    }

    @Test
    void saveChanges_recordbookModeKeepsDateValueWithoutBatchFormatValidation() throws Exception {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRule("[]",
                "DATE", Map.of(), "", false)
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRecordbookEnabled(Boolean.TRUE);
        executionMapper.updateById(execution);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-recordbook-date", null, null)
                        .setFillCarrier("RECORDBOOK")
                        .setFillMode("RECORDBOOK_UNRESTRICTED");
        command.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.DATE)
                .setNewValueJson("not-a-standard-date")
                .setNewValueDisplay("not-a-standard-date");

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        MesProBatchRecordExecutionFieldAuditItemDO item =
                itemMapper.selectListByBatchId(result.getAuditBatchId()).get(0);
        assertEquals("\"not-a-standard-date\"", item.getRecordbookValueJson());
        assertEquals("not-a-standard-date", item.getRecordbookValueDisplay());
        assertEquals("\"not-a-standard-date\"", item.getBatchRecordValueJson());
        assertEquals("not-a-standard-date", item.getBatchRecordValueDisplay());
        assertEquals("\"not-a-standard-date\"", item.getNewValueJson());
    }

    @Test
    void saveChanges_rejectsRecordbookModeWhenExecutionDisabledIt() {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithCellRule("[]",
                "NUMBER", Map.of("min", 20, "max", 40), "℃", false)
                .setRecordCategory("BATCH_RECORD")
                .setValidationProfile("CONTROLLED_BATCH")
                .setRecordbookEnabled(Boolean.FALSE);
        executionMapper.updateById(execution);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-recordbook-disabled", null, null)
                        .setFillCarrier("RECORDBOOK")
                        .setFillMode("RECORDBOOK_UNRESTRICTED");
        command.getChanges().get(0)
                .setNewValueJson(new BigDecimal("50"))
                .setNewValueDisplay("50");

        assertServiceException(() -> fieldAuditService.saveChanges(command),
                PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_usesSnapshotDefaultWhenCellValueIsMissing() {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionWithSnapshotDefault("[]", "");
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-default-empty-string",
                        "", MesProBatchRecordExecutionFieldAuditHasher.hashCanonicalTypedValue("\"\""));
        command.getChanges().get(0)
                .setValueType(MesProBatchRecordExecutionFieldAuditValueType.STRING)
                .setNewValueJson("A-001")
                .setNewValueDisplay("A-001");

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        List<MesProBatchRecordExecutionFieldAuditItemDO> items = itemMapper.selectListByBatchId(result.getAuditBatchId());
        assertEquals(1, items.size());
        MesProBatchRecordExecutionFieldAuditItemDO item = items.get(0);
        assertEquals(MesProBatchRecordExecutionFieldAuditValueType.STRING.name(), item.getValueType());
        assertEquals("\"\"", item.getOldValueJson());
        assertEquals("", item.getOldValueDisplay());
        assertEquals("\"A-001\"", item.getNewValueJson());
        assertEquals("A-001", item.getNewValueDisplay());
    }

    @Test
    void saveChanges_rejectsMissingNonNullTypedValueWithoutJsonUpdate() {
        assertRejectsMissingNewValue(MesProBatchRecordExecutionFieldAuditValueType.STRING,
                "before", "idem-string-null");
        assertRejectsMissingNewValue(MesProBatchRecordExecutionFieldAuditValueType.NUMBER,
                "36.6", "idem-number-null");
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_expectedOldValueOnlyDetectsConflict() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);

        ServiceException mismatch = assertThrows(ServiceException.class, () -> fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-002", new BigDecimal("35.0"), null)));
        assertEquals(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_OLD_VALUE_MISMATCH.getCode(), mismatch.getCode());
        assertTrue(mismatch.getMessage().contains("rowIndex=1"));
        assertTrue(mismatch.getMessage().contains("columnIndex=2"));
        assertTrue(mismatch.getMessage().contains("expected=35"));
        assertTrue(mismatch.getMessage().contains("current=36.6"));

        MesProBatchRecordExecutionDO unchanged = executionMapper.selectById(execution.getId());
        assertEquals(beforeJson, unchanged.getCellValuesJson());
        assertEquals(beforeHash, unchanged.getCellValuesHash());
        assertEquals(0L, unchanged.getFieldAuditRevision());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH, unchanged.getFieldAuditHeadHash());
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_rejectsStaleBaseBeforeSignature() {
        MesProBatchRecordExecutionDO execution = insertDraftExecution("[]");

        assertServiceException(() -> fieldAuditService.saveChanges(
                        saveCommand(execution, MesProBatchRecordExecutionFieldAuditHasher.sha256("stale"),
                                "idem-003", null, null)),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);

        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_rejectsBatchSharedStaleBaseBeforeSignature() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setMetaJson(JsonUtils.toJsonString(Map.of("sourceTableIndex", 0))));
        insertSharedBatchExecutionTask(execution, JsonUtils.toJsonString(Map.of("ranges", List.of(Map.of(
                "sourceTableIndex", 0,
                "startRow", 0,
                "endRow", 1
        )))));
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);

        assertServiceException(() -> fieldAuditService.saveChanges(
                        saveCommand(execution, MesProBatchRecordExecutionFieldAuditHasher.sha256("stale-shared"),
                                "idem-batch-shared-stale", new BigDecimal("36.6"),
                                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                        MesProBatchRecordExecutionFieldAuditValueType.NUMBER,
                                        new BigDecimal("36.6")))),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);

        MesProBatchRecordExecutionDO unchanged = executionMapper.selectById(execution.getId());
        assertEquals(beforeJson, unchanged.getCellValuesJson());
        assertEquals(beforeHash, unchanged.getCellValuesHash());
        assertTrue(batchMapper.selectListByExecutionId(execution.getId()).isEmpty());
        assertTrue(itemMapper.selectListByExecutionId(execution.getId()).isEmpty());
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_rejectsUnknownFieldPathWithoutJsonUpdate() {
        MesProBatchRecordExecutionDO execution = insertDraftExecution("[]");
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-004", null, null);
        command.getChanges().get(0).setFieldPath("sheet[0].rows[9].cells[9].missing");

        assertServiceException(() -> fieldAuditService.saveChanges(command),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);

        MesProBatchRecordExecutionDO unchanged = executionMapper.selectById(execution.getId());
        assertEquals("[]", unchanged.getCellValuesJson());
        assertEquals(beforeHash, unchanged.getCellValuesHash());
        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_requiresTenantContextInsteadOfFallingBackToZero() {
        MesProBatchRecordExecutionDO execution = insertDraftExecution("[]");
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        TenantContextHolder.clear();

        assertThrows(NullPointerException.class, () -> fieldAuditService.saveChanges(
                saveCommand(execution, beforeHash, "idem-tenant-required", null, null)));

        verify(signatureService, never()).recordFieldChangeSignature(any());
    }

    @Test
    void saveChanges_bindsAttachmentChangesToSameAuditBatchAndSignature() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-field-attachment-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        command.setAttachmentChanges(List.of(new MesProBatchRecordExecutionFieldAuditAttachmentChange()
                .setWorkTaskId(workTask.getId())
                .setRowIndex(1)
                .setColumnIndex(2)
                .setFieldKey("visualEvidence")
                .setFieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                .setFieldLabel("现场图片")
                .setAttachmentType("IMAGE")
                .setAttachmentGroupKey("R1C2-IMG-1")
                .setFileId(901L)
                .setFileUrl("http://127.0.0.1:9000/yudao/edhr/501/evidence.png")
                .setStorageConfigId(28L)
                .setStoragePath("edhr/501/evidence.png")
                .setFileName("evidence.png")
                .setContentType("image/png")
                .setFileSize(2048L)
                .setSha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .setStorageRetentionJson("{\"fileId\":901,\"retention\":\"batch-record\"}")));

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(execution.getId());
        assertEquals(1, attachments.size());
        MesProBatchRecordExecutionAttachmentDO attachment = attachments.get(0);
        assertEquals(result.getAuditBatchId(), attachment.getAuditBatchId());
        assertEquals(result.getSignatureId(), attachment.getSignatureId());
        assertEquals(1, attachment.getVersionNo());
        assertEquals("ADD", attachment.getAttachmentAction());
        assertEquals("IMAGE", attachment.getAttachmentType());
        assertEquals("visualEvidence", attachment.getFieldKey());
        assertEquals("QA", attachment.getOperatorName());
        assertEquals("CORRECTION", attachment.getReasonCategory());
        assertEquals("operator correction", attachment.getReasonText());
    }

    @Test
    void saveChanges_bindsAttachmentOnlyChangeToAuditBatchAndSignature() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-field-attachment-only-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        command.setChanges(List.of());
        command.setAttachmentChanges(List.of(baseAttachmentChange(workTask)));

        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        assertNotNull(result.getAuditBatchId());
        assertEquals(501L, result.getSignatureId());
        assertEquals(0, result.getChangedFieldCount());
        assertEquals(1, batchMapper.selectListByExecutionId(execution.getId()).size());
        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(execution.getId());
        assertEquals(1, attachments.size());
        assertEquals(result.getAuditBatchId(), attachments.get(0).getAuditBatchId());
        assertEquals(result.getSignatureId(), attachments.get(0).getSignatureId());
        assertEquals("ADD", attachments.get(0).getAttachmentAction());
    }

    @Test
    void saveChanges_signatureChallengeIncludesAttachmentMetadata() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand first =
                saveCommand(execution, beforeHash, "idem-field-attachment-challenge-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        first.setAttachmentChanges(List.of(baseAttachmentChange(workTask)));
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand second =
                saveCommand(execution, beforeHash, "idem-field-attachment-challenge-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        second.setAttachmentChanges(List.of(baseAttachmentChange(workTask)
                .setFileId(902L)
                .setStoragePath("edhr/501/evidence-replaced.png")
                .setFileName("evidence-replaced.png")
                .setSha256("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789")));

        String firstChallenge = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureChallenge(first, 99L);
        String secondChallenge = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureChallenge(second, 99L);

        assertNotEquals(firstChallenge, secondChallenge);
    }

    @Test
    void saveChanges_rejectsAttachmentReplaceWhenExpectedPreviousHashMismatch() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        attachmentService.bindAttachment(baseAttachmentCommand(execution, workTask));
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();

        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-field-attachment-replace-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        command.setAttachmentChanges(List.of(baseAttachmentChange(workTask)
                .setAttachmentAction("REPLACE")
                .setFileId(902L)
                .setStoragePath("edhr/501/evidence-replaced.png")
                .setFileName("evidence-replaced.png")
                .setExpectedPreviousAttachmentHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")));

        ServiceException exception = assertThrows(ServiceException.class, () -> fieldAuditService.saveChanges(command));

        assertEquals(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT.getCode(), exception.getCode());
        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(execution.getId());
        assertEquals(1, attachments.size());
        assertEquals("ADD", attachments.get(0).getAttachmentAction());
    }

    @Test
    void detail_includesAttachmentSummariesForAuditBatch() {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", "36.6"
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment(beforeJson);
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        mockFieldChangeSignature();
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, "idem-field-attachment-detail-001",
                        new BigDecimal("36.6"), MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, new BigDecimal("36.6")));
        command.setAttachmentChanges(List.of(baseAttachmentChange(workTask)));
        MesProBatchRecordExecutionFieldAuditSaveResult result = fieldAuditService.saveChanges(command);

        MesProBatchRecordExecutionFieldAuditDetailRespVO detail = fieldAuditService.getDetail(
                new MesProBatchRecordExecutionFieldAuditDetailReqVO()
                        .setExecutionId(execution.getId())
                        .setAuditBatchId(result.getAuditBatchId()));

        assertEquals(1, detail.getAttachmentSummaries().size());
        MesProBatchRecordExecutionFieldAuditDetailRespVO.AttachmentSummary attachment =
                detail.getAttachmentSummaries().get(0);
        assertEquals(result.getAuditBatchId(), attachment.getAuditBatchId());
        assertEquals(result.getSignatureId(), attachment.getSignatureId());
        assertEquals("ADD", attachment.getAttachmentAction());
        assertEquals("visualEvidence", attachment.getFieldKey());
        assertEquals("现场图片", attachment.getFieldLabel());
        assertEquals("evidence.png", attachment.getFileName());
        assertEquals("image/png", attachment.getContentType());
        assertEquals(2048L, attachment.getFileSize());
        assertEquals("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                attachment.getSha256());
        assertNotNull(attachment.getAttachmentHash());
    }

    @Test
    void verifyChain_includesAttachmentChainStatus() {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment("[]");
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        attachmentService.bindAttachment(baseAttachmentCommand(execution, workTask));
        MesProBatchRecordExecutionAttachmentDO attachment =
                attachmentMapper.selectListByExecutionId(execution.getId()).get(0);

        MesProBatchRecordExecutionFieldAuditHashVerification verification =
                fieldAuditService.verifyChain(execution.getId());

        assertEquals(MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID,
                verification.getStatus());
        assertEquals("VALID", verification.getAttachmentChainStatus());
        assertEquals(1, verification.getCheckedAttachmentCount());
        assertEquals(attachment.getAttachmentHash(), verification.getAttachmentChainHeadHash());
        assertEquals(0, verification.getAttachmentChainIssueCount());
    }

    @Test
    void verifyChain_invalidAttachmentChainMarksFieldAuditVerificationFailed() {
        MesProBatchRecordExecutionDO execution = insertDraftExecutionForAttachment("[]");
        MesProEdhrWorkTaskDO workTask = insertAttachmentWorkTask(execution, 99L);
        attachmentService.bindAttachment(baseAttachmentCommand(execution, workTask));
        MesProBatchRecordExecutionAttachmentDO attachment =
                attachmentMapper.selectListByExecutionId(execution.getId()).get(0);
        attachmentMapper.updateById(new MesProBatchRecordExecutionAttachmentDO()
                .setId(attachment.getId())
                .setAttachmentHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"));

        MesProBatchRecordExecutionFieldAuditHashVerification verification =
                fieldAuditService.verifyChain(execution.getId());

        assertEquals(MesProBatchRecordExecutionFieldAuditHashVerificationStatus.CHAIN_BROKEN,
                verification.getStatus());
        assertEquals("INVALID", verification.getAttachmentChainStatus());
        assertEquals(1, verification.getAttachmentChainIssueCount());
        assertTrue(verification.getAttachmentChainFailedReason().contains("Attachment hash"));
    }

    private MesProBatchRecordExecutionFieldAuditSaveChangesCommand saveCommand(
            MesProBatchRecordExecutionDO execution, String beforeHash, String idempotencyKey,
            Object expectedOldValueJson, String expectedOldValueHash) {
        return new MesProBatchRecordExecutionFieldAuditSaveChangesCommand()
                .setExecutionId(execution.getId())
                .setWorkTaskId(insertFillWorkTask(execution, 99L, MesProEdhrWorkTaskStatus.TODO).getId())
                .setIdempotencyKey(idempotencyKey)
                .setBaseCellValuesHash(beforeHash)
                .setBaseFieldAuditRevision(0L)
                .setBaseFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .setReasonCategory("CORRECTION")
                .setReasonText("operator correction")
                .setSignature(new MesProBatchRecordExecutionFieldAuditSaveChangesCommand.Signature()
                        .setPassword("secret"))
                .setChanges(List.of(new MesProBatchRecordExecutionFieldAuditChange()
                        .setFieldPath(FIELD_PATH)
                        .setFieldKey("temperature")
                        .setRowIndex(1)
                        .setColumnIndex(2)
                        .setValueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                        .setNewValueJson(new BigDecimal("37.5"))
                        .setNewValueDisplay("37.5")
                        .setExpectedOldValueJson(expectedOldValueJson)
                        .setExpectedOldValueHash(expectedOldValueHash)));
    }

    private void assertRejectsMissingNewValue(MesProBatchRecordExecutionFieldAuditValueType valueType,
                                              String oldValueDisplay,
                                              String idempotencyKey) {
        String beforeJson = JsonUtils.toJsonString(List.of(Map.of(
                "rowIndex", 1,
                "columnIndex", 2,
                "value", oldValueDisplay
        )));
        MesProBatchRecordExecutionDO execution = insertDraftExecution(beforeJson);
        String beforeHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(beforeJson);
        MesProBatchRecordExecutionFieldAuditSaveChangesCommand command =
                saveCommand(execution, beforeHash, idempotencyKey, null, null);
        command.getChanges().get(0)
                .setValueType(valueType)
                .setNewValueJson(null)
                .setNewValueDisplay("null");

        assertServiceException(() -> fieldAuditService.saveChanges(command),
                PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_TYPE_UNSUPPORTED);

        MesProBatchRecordExecutionDO unchanged = executionMapper.selectById(execution.getId());
        assertEquals(beforeJson, unchanged.getCellValuesJson());
        assertEquals(beforeHash, unchanged.getCellValuesHash());
    }

    private void mockFieldChangeDraftSave() {
        when(signatureService.recordFieldChangeDraftSave(any()))
                .thenAnswer(invocation -> {
                    MesProBatchRecordExecutionFieldAuditSignatureCommand command = invocation.getArgument(0);
                    LocalDateTime signedAt = LocalDateTime.of(2026, 5, 26, 10, 30);
                    signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                            .id(601L)
                            .executionId(command.getExecutionId())
                            .actorId(99L)
                            .actionType(MesProBatchRecordExecutionSignatureService.ACTION_FIELD_CHANGE)
                            .signatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_LOGIN_SESSION)
                            .passwordVerified(Boolean.FALSE)
                            .comment(command.getReasonText())
                            .signedAt(signedAt)
                            .signatureDisplayAt(signedAt)
                            .signatureTimeMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER)
                            .selectedTimeZone(MesProBatchRecordExecutionSignatureService.DEFAULT_SIGNATURE_TIME_ZONE)
                            .selectedTimeReason("")
                            .selectedTimePolicyVersion(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION)
                            .selectedTimeAuditHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                            .reason(command.getReasonText())
                            .actorName("QA")
                            .reasonCategory(command.getReasonCategory())
                            .signatureChallengeHash(command.getSignatureChallengeHash())
                            .build());
                    return new MesProBatchRecordExecutionFieldAuditSignatureResult()
                            .setSignatureId(601L)
                            .setActorId(99L)
                            .setActorName("QA")
                            .setSignedAt(signedAt)
                            .setSignatureDisplayAt(signedAt)
                            .setSignatureTimeMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_MODE_SERVER)
                            .setSelectedTimeZone(MesProBatchRecordExecutionSignatureService.DEFAULT_SIGNATURE_TIME_ZONE)
                            .setSelectedTimeReason("")
                            .setSelectedTimePolicyVersion(MesProBatchRecordExecutionSignatureService.SIGNATURE_TIME_POLICY_VERSION)
                            .setSelectedTimeAuditHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                });
        doAnswer(invocation -> {
            MesProBatchRecordExecutionFieldAuditSignatureAttachCommand command = invocation.getArgument(0);
            int updated = signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                    .setId(command.getSignatureId())
                    .setAuditBatchId(command.getAuditBatchId())
                    .setSignatureChallengeHash(command.getSignatureChallengeHash())
                    .setFieldAuditRevision(command.getFieldAuditRevision())
                    .setFieldAuditHeadHash(command.getFieldAuditHeadHash())
                    .setCellValuesHash(command.getCellValuesHash()));
            assertEquals(1, updated);
            return null;
        }).when(signatureService).attachFieldChangeSignature(any());
    }

    private void mockFieldChangeSignature() {
        mockFieldChangeSignature(99L, "QA", 501L, LocalDateTime.of(2026, 5, 26, 10, 30));
    }

    private void grantGoldenFingerPermission(Long userId) {
        RoleRespDTO role = new RoleRespDTO();
        role.setId(7007L);
        role.setCode(MesProEdhrGoldenFingerPermissionService.ROLE_CODE);
        role.setName("批记录金手指管理员");
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(permissionApi.getUserRoleIdListByUserId(userId)).thenReturn(Set.of(role.getId()));
        when(roleApi.getRoleList(Set.of(role.getId()))).thenReturn(List.of(role));
        when(permissionApi.hasAnyPermissionsInRoles(Set.of(role.getId()),
                MesProEdhrGoldenFingerPermissionService.PERMISSION)).thenReturn(true);
    }

    private void mockFieldChangeSignature(Long actorId, String actorName, Long signatureId, LocalDateTime signedAt) {
        when(signatureService.recordFieldChangeSignature(any()))
                .thenAnswer(invocation -> {
                    MesProBatchRecordExecutionFieldAuditSignatureCommand command = invocation.getArgument(0);
                    MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand =
                            command.getSignatureTimeCommand();
                    LocalDateTime selectedSignedAt = signatureTimeCommand == null
                            ? null : signatureTimeCommand.getSelectedSignedAt();
                    LocalDateTime displayAt = selectedSignedAt == null ? signedAt : selectedSignedAt;
                    String signatureTimeMode = selectedSignedAt == null ? "SERVER_TIME" : "USER_SELECTED";
                    String selectedTimeZone = signatureTimeCommand == null
                            ? "Asia/Shanghai" : signatureTimeCommand.getSelectedTimeZone();
                    String selectedTimeReason = signatureTimeCommand == null
                            ? "" : signatureTimeCommand.getSelectedTimeReason();
                    String selectedTimeAuditHash =
                            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
                    signatureMapper.insert(MesProBatchRecordExecutionSignatureDO.builder()
                            .id(signatureId)
                            .executionId(command.getExecutionId())
                            .actorId(actorId)
                            .actionType(MesProBatchRecordExecutionSignatureService.ACTION_FIELD_CHANGE)
                            .signatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                            .passwordVerified(Boolean.TRUE)
                            .comment(command.getReasonText())
                            .signedAt(signedAt)
                            .selectedSignedAt(selectedSignedAt)
                            .signatureDisplayAt(displayAt)
                            .signatureTimeMode(signatureTimeMode)
                            .selectedTimeZone(selectedTimeZone)
                            .selectedTimeReason(selectedTimeReason)
                            .selectedTimePolicyVersion("EDHR_SIGNATURE_TIME_V1")
                            .selectedTimeAuditHash(selectedTimeAuditHash)
                            .reason(command.getReasonText())
                            .actorName(actorName)
                            .reasonCategory(command.getReasonCategory())
                            .signatureChallengeHash(command.getSignatureChallengeHash())
                            .build());
                    return new MesProBatchRecordExecutionFieldAuditSignatureResult()
                            .setSignatureId(signatureId)
                            .setActorId(actorId)
                            .setActorName(actorName)
                            .setSignedAt(signedAt)
                            .setSelectedSignedAt(selectedSignedAt)
                            .setSignatureDisplayAt(displayAt)
                            .setSignatureTimeMode(signatureTimeMode)
                            .setSelectedTimeZone(selectedTimeZone)
                            .setSelectedTimeReason(selectedTimeReason)
                            .setSelectedTimePolicyVersion("EDHR_SIGNATURE_TIME_V1")
                            .setSelectedTimeAuditHash(selectedTimeAuditHash);
                });
        doAnswer(invocation -> {
            MesProBatchRecordExecutionFieldAuditSignatureAttachCommand command = invocation.getArgument(0);
            int updated = signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                    .setId(command.getSignatureId())
                    .setAuditBatchId(command.getAuditBatchId())
                    .setSignatureChallengeHash(command.getSignatureChallengeHash())
                    .setFieldAuditRevision(command.getFieldAuditRevision())
                    .setFieldAuditHeadHash(command.getFieldAuditHeadHash())
                    .setCellValuesHash(command.getCellValuesHash()));
            assertEquals(1, updated);
            return null;
        }).when(signatureService).attachFieldChangeSignature(any());
    }

    private MesProBatchRecordExecutionDO insertDraftExecution(String cellValuesJson) {
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-FIELD-AUDIT")
                .workOrderId(1001L)
                .workOrderCode("WO-FIELD-AUDIT")
                .batchCode("BATCH-FIELD-AUDIT")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "fields", List.of(Map.of(
                                "fieldPath", FIELD_PATH,
                                "fieldKey", "temperature",
                                "label", "Temperature",
                                "rowIndex", 1,
                                "columnIndex", 2,
                                "component", "input-number"
                        ))
                )))
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private boolean containsInternalTraceNonBlockingLimitMetadata(MesProEdhrOperationAuditCommand audit) {
        try {
            JsonNode metadata = JsonUtils.getObjectMapper().readTree(audit.getMetadataJson());
            JsonNode warnings = metadata.get("nonBlockingLimitWarnings");
            if (warnings == null || !warnings.isArray() || warnings.size() != 1) {
                return false;
            }
            JsonNode warning = warnings.get(0);
            return "INTERNAL_TRACE".equals(metadata.path("validationProfile").asText())
                    && metadata.path("nonBlockingLimitWarningCount").asInt() == 1
                    && FIELD_PATH.equals(warning.path("fieldPath").asText())
                    && "temperature".equals(warning.path("fieldKey").asText())
                    && "Temperature".equals(warning.path("fieldLabel").asText())
                    && "101".equals(warning.path("value").decimalValue().stripTrailingZeros().toPlainString())
                    && "0".equals(warning.path("min").decimalValue().stripTrailingZeros().toPlainString())
                    && "100".equals(warning.path("max").decimalValue().stripTrailingZeros().toPlainString())
                    && "CORRECTION".equals(warning.path("reasonCategory").asText())
                    && "operator correction".equals(warning.path("reasonText").asText());
        } catch (Exception ex) {
            return false;
        }
    }

    private MesProEdhrWorkTaskDO insertFillWorkTask(MesProBatchRecordExecutionDO execution, Long assigneeUserId,
                                                    String status) {
        MesProEdhrWorkTaskDO workTask = new MesProEdhrWorkTaskDO()
                .setTaskCode("EDHRT-T6-" + execution.getId())
                .setTaskType(MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                .setBatchExecutionId(7001L)
                .setBatchTaskId(7101L)
                .setBusinessScopeType("BATCH_TASK")
                .setBusinessScopeId(7101L)
                .setExecutionId(execution.getId())
                .setWorkOrderId(execution.getWorkOrderId())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchCode(execution.getBatchCode())
                .setRouteId(4001L)
                .setRouteProcessId(execution.getRouteProcessId())
                .setProcessId(5001L)
                .setProcessName("T6 写入门禁")
                .setAssigneeUserId(assigneeUserId)
                .setStatus(status)
                .setActionUrl("/mes/pro/feedback/edhr-execution/detail?id=" + execution.getId());
        workTaskMapper.insert(workTask);
        return workTask;
    }

    private MesProEdhrBatchExecutionTaskDO insertSharedBatchExecutionTask(
            MesProBatchRecordExecutionDO execution, String fillableScopeJson) {
        MesProEdhrBatchExecutionTaskDO batchTask = MesProEdhrBatchExecutionTaskDO.builder()
                .id(7101L)
                .batchExecutionId(7001L)
                .nodeType("PROCESS")
                .routeProcessId(execution.getRouteProcessId())
                .rootProcessFlag(Boolean.FALSE)
                .routeProcessSort(1)
                .processId(5001L)
                .processCode("T6")
                .processName("T6 写入门禁")
                .batchRecordReportId("SHARED-QC")
                .batchRecordReportName("过程检验单")
                .batchRecordDefinitionId(6001L)
                .batchRecordVersionId(6002L)
                .batchRecordSort(1)
                .instanceScope("BATCH_SHARED")
                .sharedFormKey("PROCESS_INSPECTION")
                .fillableScopeJson(fillableScopeJson)
                .executionMode("SEQUENTIAL")
                .formSlotType("ATTACHMENT")
                .recordCategory("PROCESS_INSPECTION")
                .validationProfile("CONTROLLED_BATCH")
                .requiredPolicy("REQUIRED")
                .ownerRoleKey("PRODUCTION")
                .archiveVisibility("FINAL_DHR")
                .executionId(execution.getId())
                .status(0)
                .requiredFlag(Boolean.TRUE)
                .build();
        batchTaskMapper.insert(batchTask);
        return batchTask;
    }

    private void insertPreReleaseBatchTask(MesProBatchRecordExecutionDO execution, Long batchExecutionId,
                                           Long batchTaskId, Integer batchStatus) {
        batchExecutionMapper.insert(new MesProEdhrBatchExecutionDO()
                .setId(batchExecutionId)
                .setBatchExecutionCode("EDHR-FIELD-" + batchExecutionId)
                .setWorkOrderId(execution.getWorkOrderId())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchCode(execution.getBatchCode())
                .setRouteId(4001L)
                .setStatus(batchStatus)
                .setTaskTotal(1)
                .setTaskApprovedCount(1)
                .setBlockedCount(0));
        batchTaskMapper.insert(new MesProEdhrBatchExecutionTaskDO()
                .setId(batchTaskId)
                .setBatchExecutionId(batchExecutionId)
                .setNodeType(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM)
                .setRouteProcessId(execution.getRouteProcessId())
                .setRouteProcessSort(1)
                .setProcessId(5001L)
                .setProcessCode("P-FIELD")
                .setProcessName("字段审计工序")
                .setBatchRecordReportId("report-field-audit")
                .setBatchRecordReportName("字段审计表单")
                .setExecutionId(execution.getId())
                .setRequiredFlag(Boolean.TRUE)
                .setStatus(MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED));
        workTaskMapper.selectTimelineListByExecutionId(execution.getId()).forEach(workTask ->
                workTaskMapper.updateById(new MesProEdhrWorkTaskDO()
                        .setId(workTask.getId())
                        .setBatchExecutionId(batchExecutionId)
                        .setBatchTaskId(batchTaskId)
                        .setBusinessScopeId(batchTaskId)));
    }

    private MesProBatchRecordExecutionDO insertDraftExecutionWithSnapshotDefault(
            String cellValuesJson, String defaultValue) {
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-FIELD-AUDIT-DEFAULT")
                .workOrderId(1001L)
                .workOrderCode("WO-FIELD-AUDIT")
                .batchCode("BATCH-FIELD-AUDIT")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "fields", List.of(Map.of(
                                "fieldPath", FIELD_PATH,
                                "fieldKey", "temperature",
                                "label", "Temperature",
                                "rowIndex", 1,
                                "columnIndex", 2,
                                "component", "input-text",
                                "defaultValue", defaultValue,
                                "value", defaultValue
                        ))
                )))
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private MesProBatchRecordExecutionDO insertDraftExecutionForAttachment(String cellValuesJson) {
        MesProBatchRecordExecutionDO execution = insertDraftExecution(cellValuesJson);
        String snapshotJson = attachmentExecutionSnapshotJson();
        execution.setTaskId(701L);
        execution.setActiveRevisionFlag(true);
        execution.setExecutionSnapshotJson(snapshotJson);
        executionMapper.updateById(new MesProBatchRecordExecutionDO()
                .setId(execution.getId())
                .setTaskId(701L)
                .setActiveRevisionFlag(true)
                .setExecutionSnapshotJson(snapshotJson));
        return execution;
    }

    private MesProEdhrWorkTaskDO insertAttachmentWorkTask(MesProBatchRecordExecutionDO execution,
                                                         Long assigneeUserId) {
        MesProEdhrWorkTaskDO workTask = MesProEdhrWorkTaskDO.builder()
                .taskCode("WT-FIELD-ATT-1")
                .taskType("FILL")
                .batchExecutionId(601L)
                .batchTaskId(701L)
                .executionId(execution.getId())
                .workOrderId(execution.getWorkOrderId())
                .workOrderCode(execution.getWorkOrderCode())
                .batchCode(execution.getBatchCode())
                .routeId(801L)
                .routeProcessId(2001L)
                .processId(901L)
                .processName("附件工序")
                .assigneeUserId(assigneeUserId)
                .status(MesProEdhrWorkTaskStatus.DOING)
                .actionUrl("/mes/pro/edhr/executions/" + execution.getId())
                .build();
        workTaskMapper.insert(workTask);
        return workTask;
    }

    private MesProBatchRecordExecutionAttachmentBindCommand baseAttachmentCommand(
            MesProBatchRecordExecutionDO execution, MesProEdhrWorkTaskDO workTask) {
        MesProBatchRecordExecutionFieldAuditAttachmentChange change = baseAttachmentChange(workTask);
        return new MesProBatchRecordExecutionAttachmentBindCommand()
                .setExecutionId(execution.getId())
                .setWorkTaskId(change.getWorkTaskId())
                .setAuditBatchId(7001L)
                .setSignatureId(501L)
                .setRowIndex(change.getRowIndex())
                .setColumnIndex(change.getColumnIndex())
                .setFieldKey(change.getFieldKey())
                .setFieldPath(change.getFieldPath())
                .setFieldLabel(change.getFieldLabel())
                .setAttachmentType(change.getAttachmentType())
                .setAttachmentGroupKey(change.getAttachmentGroupKey())
                .setFileId(change.getFileId())
                .setFileUrl(change.getFileUrl())
                .setStorageConfigId(change.getStorageConfigId())
                .setStoragePath(change.getStoragePath())
                .setFileName(change.getFileName())
                .setContentType(change.getContentType())
                .setFileSize(change.getFileSize())
                .setSha256(change.getSha256())
                .setStorageRetentionJson(change.getStorageRetentionJson())
                .setOperatorId(99L)
                .setOperatorName("QA")
                .setReasonCategory("CORRECTION")
                .setReasonText("operator correction");
    }

    private MesProBatchRecordExecutionFieldAuditAttachmentChange baseAttachmentChange(MesProEdhrWorkTaskDO workTask) {
        return new MesProBatchRecordExecutionFieldAuditAttachmentChange()
                .setWorkTaskId(workTask.getId())
                .setRowIndex(1)
                .setColumnIndex(2)
                .setFieldKey("visualEvidence")
                .setFieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                .setFieldLabel("现场图片")
                .setAttachmentType("IMAGE")
                .setAttachmentGroupKey("R1C2-IMG-1")
                .setFileId(901L)
                .setFileUrl("http://127.0.0.1:9000/yudao/edhr/501/evidence.png")
                .setStorageConfigId(28L)
                .setStoragePath("edhr/501/evidence.png")
                .setFileName("evidence.png")
                .setContentType("image/png")
                .setFileSize(2048L)
                .setSha256("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .setStorageRetentionJson("{\"fileId\":901,\"retention\":\"batch-record\"}");
    }

    private String attachmentExecutionSnapshotJson() {
        Map<String, Object> temperatureField = new java.util.LinkedHashMap<>();
        temperatureField.put("fieldPath", FIELD_PATH);
        temperatureField.put("fieldKey", "temperature");
        temperatureField.put("label", "Temperature");
        temperatureField.put("rowIndex", 1);
        temperatureField.put("columnIndex", 2);
        temperatureField.put("component", "input-number");

        Map<String, Object> constraints = new java.util.LinkedHashMap<>();
        constraints.put("allowedContentTypes", List.of("image/png"));
        constraints.put("maxFileSize", 4096);
        Map<String, Object> attachmentRule = new java.util.LinkedHashMap<>();
        attachmentRule.put("rowIndex", 1);
        attachmentRule.put("columnIndex", 2);
        attachmentRule.put("valueType", "STRING");
        attachmentRule.put("componentFlag", "upload-image");
        attachmentRule.put("required", false);
        attachmentRule.put("label", "现场图片");
        attachmentRule.put("constraints", constraints);
        attachmentRule.put("reviewed", true);
        Map<String, Object> attachmentField = new java.util.LinkedHashMap<>();
        attachmentField.put("fieldPath", "sheet[0].rows[1].cells[2].visualEvidence");
        attachmentField.put("fieldKey", "visualEvidence");
        attachmentField.put("label", "现场图片");
        attachmentField.put("rowIndex", 1);
        attachmentField.put("columnIndex", 2);
        attachmentField.put("component", "upload-image");
        attachmentField.put("valueType", "STRING");
        attachmentField.put("constraints", constraints);
        attachmentField.put("edhrCellRule", attachmentRule);
        return JsonUtils.toJsonString(Map.of("fields", List.of(temperatureField, attachmentField)));
    }

    private MesProBatchRecordExecutionDO insertDraftExecutionWithCellRule(
            String cellValuesJson, String valueType, Map<String, Object> constraints, String unit, boolean required) {
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        Map<String, Object> edhrCellRule = new java.util.LinkedHashMap<>();
        edhrCellRule.put("rowIndex", 1);
        edhrCellRule.put("columnIndex", 2);
        edhrCellRule.put("valueType", valueType);
        edhrCellRule.put("componentFlag", "input-number");
        edhrCellRule.put("required", required);
        edhrCellRule.put("constraints", constraints);
        edhrCellRule.put("unit", unit);
        edhrCellRule.put("reviewed", true);
        Map<String, Object> field = new java.util.LinkedHashMap<>();
        field.put("fieldPath", FIELD_PATH);
        field.put("fieldKey", "temperature");
        field.put("label", "Temperature");
        field.put("rowIndex", 1);
        field.put("columnIndex", 2);
        field.put("component", "input-number");
        field.put("valueType", valueType);
        field.put("required", required);
        field.put("constraints", constraints);
        field.put("unit", unit);
        field.put("edhrCellRule", edhrCellRule);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-FIELD-AUDIT-RULE")
                .workOrderId(1001L)
                .workOrderCode("WO-FIELD-AUDIT")
                .batchCode("BATCH-FIELD-AUDIT")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "fields", List.of(field)
                )))
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private MesProBatchRecordExecutionDO insertDraftExecutionWithCellRuleAndSignature(String cellValuesJson) {
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        Map<String, Object> numberRule = new java.util.LinkedHashMap<>();
        numberRule.put("rowIndex", 1);
        numberRule.put("columnIndex", 2);
        numberRule.put("valueType", "NUMBER");
        numberRule.put("componentFlag", "input-number");
        numberRule.put("required", true);
        numberRule.put("constraints", Map.of("min", 0, "max", 100, "scale", 1));
        numberRule.put("unit", "℃");
        numberRule.put("reviewed", true);
        Map<String, Object> numberField = new java.util.LinkedHashMap<>();
        numberField.put("fieldPath", FIELD_PATH);
        numberField.put("fieldKey", "temperature");
        numberField.put("label", "Temperature");
        numberField.put("rowIndex", 1);
        numberField.put("columnIndex", 2);
        numberField.put("component", "input-number");
        numberField.put("valueType", "NUMBER");
        numberField.put("required", true);
        numberField.put("constraints", Map.of("min", 0, "max", 100, "scale", 1));
        numberField.put("unit", "℃");
        numberField.put("edhrCellRule", numberRule);

        Map<String, Object> signatureRule = new java.util.LinkedHashMap<>();
        signatureRule.put("rowIndex", 9);
        signatureRule.put("columnIndex", 1);
        signatureRule.put("valueType", "SIGNATURE");
        signatureRule.put("componentFlag", "signature");
        signatureRule.put("required", true);
        signatureRule.put("constraints", Map.of());
        signatureRule.put("reviewed", true);
        Map<String, Object> signatureField = new java.util.LinkedHashMap<>();
        signatureField.put("fieldPath", "sheet[0].rows[9].cells[1].qaSignature");
        signatureField.put("fieldKey", "qaSignature");
        signatureField.put("label", "QA 签名");
        signatureField.put("rowIndex", 9);
        signatureField.put("columnIndex", 1);
        signatureField.put("component", "signature");
        signatureField.put("valueType", "SIGNATURE");
        signatureField.put("required", true);
        signatureField.put("edhrCellRule", signatureRule);

        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-FIELD-AUDIT-SIGNATURE-SNAPSHOT")
                .workOrderId(1001L)
                .workOrderCode("WO-FIELD-AUDIT")
                .batchCode("BATCH-FIELD-AUDIT")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "fields", List.of(numberField, signatureField)
                )))
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private MesProBatchRecordExecutionDO insertDraftExecutionWithTypedSnapshotDefault(
            String cellValuesJson, String valueType, String component, String defaultValue) {
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("BRE-FIELD-AUDIT-TYPED-EMPTY-" + valueType)
                .workOrderId(1001L)
                .workOrderCode("WO-FIELD-AUDIT")
                .batchCode("BATCH-FIELD-AUDIT")
                .status(0)
                .sheetLayoutJson("{}")
                .metaJson("{}")
                .executionSnapshotJson(JsonUtils.toJsonString(Map.of(
                        "fields", List.of(Map.of(
                                "fieldPath", FIELD_PATH,
                                "fieldKey", "temperature",
                                "label", "Temperature",
                                "rowIndex", 1,
                                "columnIndex", 2,
                                "component", component,
                                "valueType", valueType,
                                "defaultValue", defaultValue,
                                "value", defaultValue
                        ))
                )))
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .build();
        executionMapper.insert(execution);
        return execution;
    }
}
