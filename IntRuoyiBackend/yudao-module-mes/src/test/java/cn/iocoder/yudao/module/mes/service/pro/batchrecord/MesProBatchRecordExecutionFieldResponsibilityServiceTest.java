package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.projection.MesProBatchRecordExecutionFieldResponsibilityAuditProjection;
import jakarta.annotation.Resource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_OBJECT_PERMISSION_DENIED;

@Import(MesProBatchRecordExecutionFieldResponsibilityService.class)
class MesProBatchRecordExecutionFieldResponsibilityServiceTest extends BaseDbUnitTest {

    @Resource
    private MesProBatchRecordExecutionFieldResponsibilityService responsibilityService;

    @MockitoBean
    private MesProBatchRecordExecutionMapper executionMapper;
    @MockitoBean
    private MesProBatchRecordExecutionFieldAuditItemMapper itemMapper;
    @MockitoBean
    private MesProBatchRecordExecutionFieldAuditBatchMapper batchMapper;
    @MockitoBean
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @MockitoBean
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @MockitoBean
    private MesProEdhrPermissionScopeService permissionScopeService;
    @MockitoBean
    private MesProBatchRecordExecutionFieldAuditService fieldAuditService;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void overallEvidenceUsesCompleteSnapshotBeforeFilterAndPage() {
        TenantContextHolder.setTenantId(122L);
        ResponsibilityFixture fixture = responsibilityFixture();
        when(executionMapper.selectById(1001L)).thenReturn(fixture.execution());
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityProjectionList(1001L)).thenReturn(List.of(fixture.audit()));
        when(batchMapper.selectListByExecutionId(1001L)).thenReturn(List.of(fixture.batch()));
        when(signatureMapper.selectResponsibilityListByIds(Set.of(501L)))
                .thenReturn(List.of(fixture.signature()));
        when(workTaskMapper.selectTimelineListByExecutionId(1001L)).thenReturn(List.of(
                new MesProEdhrWorkTaskDO().setId(701L).setExecutionId(1001L)
                        .setAssigneeUserId(999L).setCandidateUserSnapshot("998,999")));

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO full = responsibilityService.getSummary(
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(1001L)
                        .setPageSize(200));

        assertEquals(5L, full.getTotal());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                full.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS),
                full.getOverallReasonCodes());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityContextWarning.VERSION_CONTEXT_MISSING),
                full.getContextWarnings());

        MesProBatchRecordExecutionFieldResponsibilityItemRespVO human = item(full, "temperature");
        assertEquals("37.2", human.getCurrentValueJson());
        assertEquals("37.2", human.getCurrentValueDisplay());
        assertEquals(fixture.currentValueHash(), human.getCurrentValueHash());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.HUMAN, human.getValueOrigin());
        assertEquals(101L, human.getFirstHumanActorId());
        assertEquals("Alice", human.getFirstHumanActorName());
        assertEquals(101L, human.getCurrentValueActorId());
        assertEquals("Alice", human.getCurrentValueActorName());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE, human.getEvidenceStatus());
        assertEquals(List.of(), human.getReasonCodes());
        assertEquals(1L, human.getHistoryCount());
        assertEquals(401L, human.getLatestAuditItemId());

        MesProBatchRecordExecutionFieldResponsibilityItemRespVO baseline = item(full, "batchCode");
        assertEquals("\"AUTO-01\"", baseline.getCurrentValueJson());
        assertEquals("AUTO-01", baseline.getCurrentValueDisplay());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.SYSTEM_BASELINE, baseline.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE, baseline.getEvidenceStatus());
        assertFalse(baseline.getReasonCodes()
                .contains(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_MISSING));
        assertNull(baseline.getCurrentValueActorId());

        MesProBatchRecordExecutionFieldResponsibilityItemRespVO untouched = item(full, "notes");
        assertEquals("null", untouched.getCurrentValueJson());
        assertEquals("", untouched.getCurrentValueDisplay());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.EMPTY_UNTOUCHED,
                untouched.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE,
                untouched.getEvidenceStatus());
        assertNull(untouched.getCurrentValueActorId());

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO filtered = responsibilityService.getSummary(
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(1001L)
                        .setFieldKeyword("Temperature")
                        .setEvidenceStatus(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE)
                        .setPageNo(1)
                        .setPageSize(1));

        assertEquals(1L, filtered.getTotal());
        assertEquals(List.of("temperature"),
                filtered.getList().stream().map(MesProBatchRecordExecutionFieldResponsibilityItemRespVO::getFieldKey)
                        .toList());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                filtered.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS),
                filtered.getOverallReasonCodes());
        assertEquals(101L, filtered.getList().get(0).getCurrentValueActorId());
    }

    @Test
    void summaryFiltersOnlyChangeListAndTotalWhileOverallStaysCompleteUniverse() {
        ResponsibilityFixture fixture = responsibilityFixture();

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO keyword = summary(
                fixture.execution(), List.of(fixture.audit()), List.of(fixture.batch()),
                List.of(fixture.signature()),
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(1001L)
                        .setFieldKeyword("Notes")
                        .setPageSize(200));
        assertEquals(1L, keyword.getTotal());
        assertEquals(List.of("notes"), keyword.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityItemRespVO::getFieldKey).toList());
        assertBlockedOverallFromCompleteUniverse(keyword);

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO evidence = summary(
                fixture.execution(), List.of(fixture.audit()), List.of(fixture.batch()),
                List.of(fixture.signature()),
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(1001L)
                        .setEvidenceStatus(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE)
                        .setPageNo(1)
                        .setPageSize(1));
        assertEquals(3L, evidence.getTotal());
        assertEquals(1, evidence.getList().size());
        assertBlockedOverallFromCompleteUniverse(evidence);

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO origin = summary(
                fixture.execution(), List.of(fixture.audit()), List.of(fixture.batch()),
                List.of(fixture.signature()),
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(1001L)
                        .setValueOrigin(MesProBatchRecordExecutionResponsibilityValueOrigin.SYSTEM_BASELINE)
                        .setPageSize(200));
        assertEquals(1L, origin.getTotal());
        assertEquals(List.of("batchCode"), origin.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityItemRespVO::getFieldKey).toList());
        assertBlockedOverallFromCompleteUniverse(origin);

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO actor = summary(
                fixture.execution(), List.of(fixture.audit()), List.of(fixture.batch()),
                List.of(fixture.signature()),
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(1001L)
                        .setActorId(101L)
                        .setPageSize(200));
        assertEquals(1L, actor.getTotal());
        assertEquals(List.of("temperature"), actor.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityItemRespVO::getFieldKey).toList());
        assertBlockedOverallFromCompleteUniverse(actor);
    }

    @Test
    void export_keepsBlockedEvidencePackageWithReasonCodesInsteadOfFailing() {
        TenantContextHolder.setTenantId(122L);
        ResponsibilityFixture fixture = responsibilityFixture();
        when(executionMapper.selectById(1001L)).thenReturn(fixture.execution());
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityProjectionList(1001L)).thenReturn(List.of(fixture.audit()));
        when(batchMapper.selectListByExecutionId(1001L)).thenReturn(List.of(fixture.batch()));
        when(signatureMapper.selectResponsibilityListByIds(Set.of(501L)))
                .thenReturn(List.of(fixture.signature()));
        when(workTaskMapper.selectTimelineListByExecutionId(1001L)).thenReturn(List.of(
                new MesProEdhrWorkTaskDO().setId(701L).setExecutionId(1001L)
                        .setAssigneeUserId(999L).setCandidateUserSnapshot("998,999")));

        MesProBatchRecordExecutionFieldResponsibilityExportRespVO export = responsibilityService.export(
                new MesProBatchRecordExecutionFieldResponsibilityExportReqVO().setExecutionId(1001L));

        assertEquals("field-responsibility-1001.xlsx", export.getFileName());
        assertEquals("XLSX", export.getFormat());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED, export.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS),
                export.getReasonCodes());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityContextWarning.VERSION_CONTEXT_MISSING),
                export.getContextWarnings());
        assertEquals(5L, export.getRecordCount());
        byte[] content = Base64.getDecoder().decode(export.getContentBase64());
        assertTrue(content.length > 0);
        assertEquals(DigestUtil.sha256Hex(content), export.getSha256());
    }

    @Test
    void emptyAuditChainWithoutRevisionAndHeadIsBlocked() {
        MesProBatchRecordExecutionDO execution = untouchedExecution(1101L, "\"AUTO-01\"", "AUTO-01");
        execution.setFieldAuditRevision(null).setFieldAuditHeadHash(null);

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                summary(execution, List.of(), List.of(), List.of());

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                summary.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO item = item(summary, "field");
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, item.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED, item.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                item.getReasonCodes());
    }

    @Test
    void currentValueWithoutAuditIsUnknownAndDoesNotGuessActor() {
        String cellValuesJson = cellValuesJson("\"MANUAL\"", "MANUAL",
                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "MANUAL"));
        MesProBatchRecordExecutionDO execution = baseExecution(1102L, standardSnapshot("\"AUTO\""),
                cellValuesJson)
                .setFieldAuditRevision(0L)
                .setFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH);

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                summary(execution, List.of(), List.of(), List.of());

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_AUDIT_MISSING),
                summary.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO item = item(summary, "field");
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, item.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                item.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_AUDIT_MISSING),
                item.getReasonCodes());
        assertNull(item.getFirstHumanActorId());
        assertNull(item.getCurrentValueActorId());
    }

    @Test
    void validClearingKeepsFirstAndCurrentActorsFromAuditHistory() {
        ClearingFixture fixture = clearingFixture();

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                summary(fixture.execution(), fixture.audits(), fixture.batches(), fixture.signatures());

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(), summary.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO item = item(summary, "field");
        assertEquals("\"\"", item.getCurrentValueJson());
        assertEquals("", item.getCurrentValueDisplay());
        assertEquals(fixture.clearedValueHash(), item.getCurrentValueHash());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.HUMAN, item.getValueOrigin());
        assertEquals(101L, item.getFirstHumanActorId());
        assertEquals("Alice", item.getFirstHumanActorName());
        assertEquals(fixture.firstChangedAt(), item.getFirstHumanChangedAt());
        assertEquals(202L, item.getCurrentValueActorId());
        assertEquals("Bob", item.getCurrentValueActorName());
        assertEquals(fixture.currentChangedAt(), item.getCurrentValueChangedAt());
        assertEquals(2L, item.getHistoryCount());
        assertEquals(402L, item.getLatestAuditItemId());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE, item.getEvidenceStatus());
        assertEquals(List.of(), item.getReasonCodes());
    }

    @Test
    void missingSnapshotBaselineAndAuditExposeExactEvidenceReasons() {
        MesProBatchRecordExecutionDO snapshotMissing = baseExecution(1103L, null, "[]")
                .setFieldAuditRevision(0L)
                .setFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH);
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO missingSnapshot =
                summary(snapshotMissing, List.of(), List.of(), List.of());
        assertEquals(0L, missingSnapshot.getTotal());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                missingSnapshot.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.EXECUTION_SNAPSHOT_MISSING),
                missingSnapshot.getOverallReasonCodes());

        MesProBatchRecordExecutionDO baselineMissing = baseExecution(1104L, standardSnapshot("\"AUTO\""), null)
                .setFieldAuditRevision(0L)
                .setFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH);
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO missingBaseline =
                summary(baselineMissing, List.of(), List.of(), List.of());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO baselineItem = item(missingBaseline, "field");
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, baselineItem.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                baselineItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.BASELINE_MISSING),
                baselineItem.getReasonCodes());
    }

    @Test
    void signatureMissingAndInvalidAreDistinguished() {
        ResponsibilityFixture missingFixture = isolatedResponsibilityFixture();
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO missing =
                summary(missingFixture.execution(), List.of(missingFixture.audit()),
                        List.of(missingFixture.batch()), List.of());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                missing.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_MISSING),
                missing.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO missingItem = item(missing, "temperature");
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, missingItem.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                missingItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_MISSING),
                missingItem.getReasonCodes());

        ResponsibilityFixture invalidFixture = isolatedResponsibilityFixture();
        invalidFixture.signature().setPasswordVerified(Boolean.FALSE);
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO invalid =
                summary(invalidFixture.execution(), List.of(invalidFixture.audit()),
                        List.of(invalidFixture.batch()), List.of(invalidFixture.signature()));
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                invalid.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID),
                invalid.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO invalidItem = item(invalid, "temperature");
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, invalidItem.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                invalidItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID),
                invalidItem.getReasonCodes());
    }

    @Test
    void brokenAuditChainIsBlockedWithExactReason() {
        ResponsibilityFixture fixture = isolatedResponsibilityFixture();
        fixture.audit().setPreviousHash("broken-previous-hash");

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                summary(fixture.execution(), List.of(fixture.audit()),
                        List.of(fixture.batch()), List.of(fixture.signature()));

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                summary.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO item = item(summary, "temperature");
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, item.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED, item.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                item.getReasonCodes());
    }

    @Test
    void currentValueHashMismatchIsBlockedWithoutActorAttribution() {
        ResponsibilityFixture fixture = isolatedResponsibilityFixture();
        String mismatchedValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 38.0);
        String mismatchedCellValues = """
                [{"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","rowIndex":1,"columnIndex":1,"valueType":"NUMBER","value":38.0,"valueDisplay":"38.0","valueHash":"%s"}]
                """.formatted(mismatchedValueHash);
        fixture.execution()
                .setCellValuesJson(mismatchedCellValues)
                .setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(mismatchedCellValues));

        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary =
                summary(fixture.execution(), List.of(fixture.audit()),
                        List.of(fixture.batch()), List.of(fixture.signature()));

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CURRENT_VALUE_MISMATCH),
                summary.getOverallReasonCodes());
        MesProBatchRecordExecutionFieldResponsibilityItemRespVO item = item(summary, "temperature");
        assertEquals("38", item.getCurrentValueJson());
        assertEquals(MesProBatchRecordExecutionResponsibilityValueOrigin.UNKNOWN, item.getValueOrigin());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED, item.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CURRENT_VALUE_MISMATCH),
                item.getReasonCodes());
        assertNull(item.getFirstHumanActorId());
        assertNull(item.getCurrentValueActorId());
    }

    @Test
    void fieldDefinitionMissingAndIdentityConflictRemainExplicit() {
        String missingDefinitionSnapshot = """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","label":"Missing key","rowIndex":1,"columnIndex":1,"component":"input","valueType":"STRING","defaultValue":"AUTO"}
                ]}
                """;
        MesProBatchRecordExecutionDO missingDefinition = baseExecution(
                1105L, missingDefinitionSnapshot, "[]")
                .setFieldAuditRevision(0L)
                .setFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH);
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO missing =
                summary(missingDefinition, List.of(), List.of(), List.of());
        assertEquals(1L, missing.getTotal());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                missing.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_DEFINITION_MISSING),
                missing.getOverallReasonCodes());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_DEFINITION_MISSING),
                missing.getList().get(0).getReasonCodes());

        String ambiguousSnapshot = """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"duplicate","label":"First","rowIndex":1,"columnIndex":1,"component":"input","valueType":"STRING","defaultValue":"AUTO"},
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"duplicate","label":"Second","rowIndex":1,"columnIndex":1,"component":"input","valueType":"STRING","defaultValue":"AUTO"}
                ]}
                """;
        MesProBatchRecordExecutionDO ambiguous = baseExecution(1106L, ambiguousSnapshot, "[]")
                .setFieldAuditRevision(0L)
                .setFieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH);
        MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO conflict =
                summary(ambiguous, List.of(), List.of(), List.of());
        assertEquals(2L, conflict.getTotal());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                conflict.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS),
                conflict.getOverallReasonCodes());
        assertTrue(conflict.getList().stream().allMatch(item ->
                item.getReasonCodes().equals(List.of(
                        MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS))));
    }

    @Test
    void historyUsesRevisionAndAuditItemCompositeCursor() {
        TenantContextHolder.setTenantId(122L);
        Long executionId = 1201L;
        MesProBatchRecordExecutionDO execution = baseExecution(
                executionId, standardSnapshot("\"\""), null);
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection revisionNineHigh =
                historyAudit(executionId, 903L, 9L, null, 301L, "Alice");
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection revisionNineLow =
                historyAudit(executionId, 902L, 9L, null, 302L, "Bob");
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection revisionEight =
                historyAudit(executionId, 801L, 8L, null, 303L, "Carol");
        when(executionMapper.selectById(executionId)).thenReturn(execution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                null, null, 3))
                .thenReturn(List.of(revisionNineHigh, revisionNineLow, revisionEight));
        when(itemMapper.selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                9L, 902L, 3))
                .thenReturn(List.of(revisionEight));
        when(batchMapper.selectListByExecutionId(executionId)).thenReturn(List.of());

        MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO first =
                responsibilityService.getHistory(historyReq(executionId, 2));

        assertEquals(List.of(903L, 902L), first.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO::getAuditItemId).toList());
        assertTrue(first.getHasMore());
        assertEquals(9L, first.getNextCursorFieldAuditRevision());
        assertEquals(902L, first.getNextCursorAuditItemId());

        MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO second =
                responsibilityService.getHistory(historyReq(executionId, 2)
                        .setCursorFieldAuditRevision(first.getNextCursorFieldAuditRevision())
                        .setCursorAuditItemId(first.getNextCursorAuditItemId()));

        assertEquals(List.of(801L), second.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO::getAuditItemId).toList());
        assertFalse(second.getHasMore());
        assertNull(second.getNextCursorFieldAuditRevision());
        assertNull(second.getNextCursorAuditItemId());
        assertEquals(List.of(903L, 902L, 801L), List.of(
                first.getList().get(0).getAuditItemId(),
                first.getList().get(1).getAuditItemId(),
                second.getList().get(0).getAuditItemId()));
        verify(itemMapper).selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                null, null, 3);
        verify(itemMapper).selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                9L, 902L, 3);
        verify(signatureMapper, never()).selectResponsibilityListByIds(any());
    }

    @Test
    void historyExposesPerItemEvidenceAndBatchesSignatureLookupOnce() {
        TenantContextHolder.setTenantId(122L);
        Long executionId = 1202L;
        String snapshotJson = standardSnapshot("\"\"");
        String snapshotHash = MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(snapshotJson);
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        HumanAudit complete = humanAudit(executionId, 704L, 604L, 504L, 4L,
                MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                "\"\"", "", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, ""),
                "\"A\"", "A", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "A"),
                304L, "Dora", LocalDateTime.of(2026, 7, 10, 12, 4), cellValuesHash, snapshotHash);
        HumanAudit missing = humanAudit(executionId, 703L, 603L, 503L, 3L,
                complete.audit().getAuditHash(),
                "\"A\"", "A", complete.audit().getNewValueHash(),
                "\"B\"", "B", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "B"),
                303L, "Carol", LocalDateTime.of(2026, 7, 10, 12, 3), cellValuesHash, snapshotHash);
        missing.audit().setSignatureId(null);
        missing.batch().setSignatureId(null);
        HumanAudit invalid = humanAudit(executionId, 702L, 602L, 502L, 2L,
                missing.audit().getAuditHash(),
                "\"B\"", "B", missing.audit().getNewValueHash(),
                "\"C\"", "C", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "C"),
                302L, "Bob", LocalDateTime.of(2026, 7, 10, 12, 2), cellValuesHash, snapshotHash);
        invalid.audit().setExecutionId(9999L);
        invalid.signature().setPasswordVerified(Boolean.FALSE);
        HumanAudit broken = humanAudit(executionId, 701L, 601L, 501L, 1L,
                invalid.audit().getAuditHash(),
                "\"C\"", "C", invalid.audit().getNewValueHash(),
                "\"D\"", "D", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "D"),
                301L, "Alice", LocalDateTime.of(2026, 7, 10, 12, 1), cellValuesHash, snapshotHash);
        broken.audit().setAuditHash("broken-audit-hash");
        complete.signature().setActorUsernameSnapshot("dora")
                .setActorNicknameSnapshot("Dora Snapshot");

        MesProBatchRecordExecutionDO execution = baseExecution(executionId, snapshotJson, "[]");
        when(executionMapper.selectById(executionId)).thenReturn(execution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                null, null, 11))
                .thenReturn(List.of(complete.audit(), missing.audit(), invalid.audit(), broken.audit()));
        when(batchMapper.selectListByExecutionId(executionId)).thenReturn(List.of(
                complete.batch(), missing.batch(), invalid.batch(), broken.batch()));
        when(signatureMapper.selectResponsibilityListByIds(eq(Set.of(504L, 502L, 501L))))
                .thenReturn(List.of(complete.signature(), invalid.signature(), broken.signature()));

        MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO response =
                responsibilityService.getHistory(historyReq(executionId, 10));

        assertEquals(4, response.getList().size());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE,
                response.getList().get(0).getEvidenceStatus());
        assertEquals(List.of(), response.getList().get(0).getReasonCodes());
        assertEquals("dora", response.getList().get(0).getSignatureActorUsernameSnapshot());
        assertEquals("Dora Snapshot", response.getList().get(0).getSignatureActorNicknameSnapshot());
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                response.getList().get(1).getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_MISSING),
                response.getList().get(1).getReasonCodes());
        assertNoResponsibilityAttribution(response.getList().get(1));
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                response.getList().get(2).getEvidenceStatus());
        assertEquals(List.of(
                        MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID,
                        MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION),
                response.getList().get(2).getReasonCodes());
        assertNoResponsibilityAttribution(response.getList().get(2));
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                response.getList().get(3).getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                response.getList().get(3).getReasonCodes());
        assertNoResponsibilityAttribution(response.getList().get(3));
        verify(signatureMapper, times(1))
                .selectResponsibilityListByIds(eq(Set.of(504L, 502L, 501L)));
    }

    @Test
    void sameTenantCrossExecutionSignatureHasExactAssociationReason() {
        TenantContextHolder.setTenantId(122L);
        Long executionId = 1209L;
        String snapshotJson = standardSnapshot("\"\"");
        String snapshotHash = MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(snapshotJson);
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");
        HumanAudit crossExecutionSignature = historyHumanAudit(
                executionId, 709L, 609L, 509L, 1L,
                MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                "same-tenant-cross-execution-signature",
                309L, "Cross Execution Signature Actor", LocalDateTime.of(2026, 7, 10, 13, 9),
                cellValuesHash, snapshotHash);
        crossExecutionSignature.signature()
                .setExecutionId(9999L)
                .setActorUsernameSnapshot("cross-execution-signature-user")
                .setActorNicknameSnapshot("Cross Execution Signature Snapshot");

        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO item = firstHistoryItem(
                executionId,
                crossExecutionSignature.audit(),
                List.of(crossExecutionSignature.batch()),
                List.of(crossExecutionSignature.signature()),
                snapshotJson);

        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                item.getEvidenceStatus());
        assertEquals(List.of(
                        MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID,
                        MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION),
                item.getReasonCodes());
        assertNoResponsibilityAttribution(item);
        assertHistoryInvestigationFacts(item, crossExecutionSignature.audit());
    }

    @Test
    void invisibleAndMissingExecutionsReturnSameResultWithoutPermissionOrEvidenceReads() {
        TenantContextHolder.setTenantId(122L);

        ServiceException summaryException = assertThrows(ServiceException.class,
                () -> responsibilityService.getSummary(
                        new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                                .setExecutionId(1301L)));
        ServiceException historyException = assertThrows(ServiceException.class,
                () -> responsibilityService.getHistory(historyReq(1302L, 50)));

        assertEquals(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS.getCode(), summaryException.getCode());
        assertEquals(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS.getMsg(), summaryException.getMessage());
        assertEquals(summaryException.getCode(), historyException.getCode());
        assertEquals(summaryException.getMessage(), historyException.getMessage());
        verify(executionMapper).selectById(1301L);
        verify(executionMapper).selectById(1302L);
        verifyNoInteractions(permissionScopeService, itemMapper, batchMapper, signatureMapper, workTaskMapper);
    }

    @Test
    void auditViewDenyStopsSummaryAndHistoryBeforeEvidenceReads() {
        TenantContextHolder.setTenantId(122L);
        MesProBatchRecordExecutionDO execution = baseExecution(
                1303L, standardSnapshot("\"\""), "[]");
        when(executionMapper.selectById(1303L)).thenReturn(execution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "DENY")));

        ServiceException summaryException = assertThrows(ServiceException.class,
                () -> responsibilityService.getSummary(
                        new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                                .setExecutionId(1303L)));
        ServiceException historyException = assertThrows(ServiceException.class,
                () -> responsibilityService.getHistory(historyReq(1303L, 50)));

        assertEquals(PRO_EDHR_OBJECT_PERMISSION_DENIED.getCode(), summaryException.getCode());
        assertEquals(summaryException.getCode(), historyException.getCode());
        assertEquals(summaryException.getMessage(), historyException.getMessage());
        verify(permissionScopeService, times(2)).evaluate(any());
        verifyNoInteractions(itemMapper, batchMapper, signatureMapper, workTaskMapper);
    }

    @Test
    void historyClearsResponsibilityAttributionWhenEvidenceIsNotComplete() {
        TenantContextHolder.setTenantId(122L);
        String snapshotJson = standardSnapshot("\"\"");
        String snapshotHash = MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(snapshotJson);
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]");

        HumanAudit complete = historyHumanAudit(1210L, 710L, 610L, 510L, 1L,
                "complete-previous-hash", "complete",
                310L, "Complete Actor", LocalDateTime.of(2026, 7, 10, 13, 10),
                cellValuesHash, snapshotHash);
        complete.signature().setActorUsernameSnapshot("complete-user")
                .setActorNicknameSnapshot("Complete Snapshot");
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO completeItem =
                firstHistoryItem(1210L, complete.audit(), List.of(complete.batch()), List.of(complete.signature()),
                        snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.COMPLETE,
                completeItem.getEvidenceStatus());
        assertEquals(310L, completeItem.getActorId());
        assertEquals("Complete Actor", completeItem.getActorName());
        assertEquals("complete-user", completeItem.getSignatureActorUsernameSnapshot());
        assertEquals("Complete Snapshot", completeItem.getSignatureActorNicknameSnapshot());
        assertEquals(complete.signature().getSignatureDisplayAt(), completeItem.getSignatureDisplayAt());
        assertEquals(List.of(), completeItem.getReasonCodes());
        assertHistoryInvestigationFacts(completeItem, complete.audit());

        HumanAudit signatureMissing = historyHumanAudit(1211L, 711L, 611L, 511L, 1L,
                "signature-missing-previous-hash", "signature-missing",
                311L, "Missing Actor", LocalDateTime.of(2026, 7, 10, 13, 11),
                cellValuesHash, snapshotHash);
        signatureMissing.signature().setActorUsernameSnapshot("signature-missing-user")
                .setActorNicknameSnapshot("Signature Missing Snapshot");
        signatureMissing.audit().setSignatureId(null);
        signatureMissing.batch().setSignatureId(null);
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO missingItem =
                firstHistoryItem(1211L, signatureMissing.audit(), List.of(signatureMissing.batch()), List.of(),
                        snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.EVIDENCE_MISSING,
                missingItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_MISSING),
                missingItem.getReasonCodes());
        assertNoResponsibilityAttribution(missingItem);
        assertHistoryInvestigationFacts(missingItem, signatureMissing.audit());

        HumanAudit signatureInvalid = historyHumanAudit(1212L, 712L, 612L, 512L, 1L,
                "signature-invalid-previous-hash", "signature-invalid",
                312L, "Invalid Actor", LocalDateTime.of(2026, 7, 10, 13, 12),
                cellValuesHash, snapshotHash);
        signatureInvalid.signature().setPasswordVerified(Boolean.FALSE)
                .setActorUsernameSnapshot("invalid-user")
                .setActorNicknameSnapshot("Invalid Snapshot");
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO invalidItem =
                firstHistoryItem(1212L, signatureInvalid.audit(), List.of(signatureInvalid.batch()),
                        List.of(signatureInvalid.signature()), snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                invalidItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID),
                invalidItem.getReasonCodes());
        assertNoResponsibilityAttribution(invalidItem);
        assertHistoryInvestigationFacts(invalidItem, signatureInvalid.audit());

        HumanAudit crossExecution = historyHumanAudit(1213L, 713L, 613L, 513L, 1L,
                "cross-execution-previous-hash", "cross-execution",
                313L, "Cross Execution Actor", LocalDateTime.of(2026, 7, 10, 13, 13),
                cellValuesHash, snapshotHash);
        crossExecution.batch().setExecutionId(9999L);
        crossExecution.signature().setActorUsernameSnapshot("cross-execution-user")
                .setActorNicknameSnapshot("Cross Execution Snapshot");
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO crossExecutionItem =
                firstHistoryItem(1213L, crossExecution.audit(), List.of(crossExecution.batch()),
                        List.of(crossExecution.signature()), snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                crossExecutionItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_EXECUTION_ASSOCIATION),
                crossExecutionItem.getReasonCodes());
        assertNoResponsibilityAttribution(crossExecutionItem);
        assertHistoryInvestigationFacts(crossExecutionItem, crossExecution.audit());

        HumanAudit crossTenant = historyHumanAudit(1214L, 714L, 614L, 514L, 1L,
                "cross-tenant-previous-hash", "cross-tenant",
                314L, "Cross Tenant Actor", LocalDateTime.of(2026, 7, 10, 13, 14),
                cellValuesHash, snapshotHash);
        crossTenant.batch().setTenantId(999L);
        crossTenant.signature().setActorUsernameSnapshot("cross-tenant-user")
                .setActorNicknameSnapshot("Cross Tenant Snapshot");
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO crossTenantItem =
                firstHistoryItem(1214L, crossTenant.audit(), List.of(crossTenant.batch()),
                        List.of(crossTenant.signature()), snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                crossTenantItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CROSS_TENANT_ASSOCIATION),
                crossTenantItem.getReasonCodes());
        assertNoResponsibilityAttribution(crossTenantItem);
        assertHistoryInvestigationFacts(crossTenantItem, crossTenant.audit());

        HumanAudit batchMissing = historyHumanAudit(1215L, 715L, 615L, 515L, 1L,
                "batch-missing-previous-hash", "batch-missing",
                315L, "Batch Missing Actor", LocalDateTime.of(2026, 7, 10, 13, 15),
                cellValuesHash, snapshotHash);
        batchMissing.signature().setActorUsernameSnapshot("batch-missing-user")
                .setActorNicknameSnapshot("Batch Missing Snapshot");
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO batchMissingItem =
                firstHistoryItem(1215L, batchMissing.audit(), List.of(), List.of(batchMissing.signature()),
                        snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                batchMissingItem.getEvidenceStatus());
        assertEquals(List.of(
                        MesProBatchRecordExecutionResponsibilityReasonCode.SIGNATURE_INVALID,
                        MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                batchMissingItem.getReasonCodes());
        assertNoResponsibilityAttribution(batchMissingItem);
        assertHistoryInvestigationFacts(batchMissingItem, batchMissing.audit());

        HumanAudit chainInvalid = historyHumanAudit(1216L, 716L, 616L, 516L, 1L,
                "chain-invalid-previous-hash", "chain-invalid",
                316L, "Chain Invalid Actor", LocalDateTime.of(2026, 7, 10, 13, 16),
                cellValuesHash, snapshotHash);
        chainInvalid.signature().setActorUsernameSnapshot("chain-invalid-user")
                .setActorNicknameSnapshot("Chain Invalid Snapshot");
        chainInvalid.audit().setAuditHash("chain-invalid-investigation-audit-hash");
        MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO chainInvalidItem =
                firstHistoryItem(1216L, chainInvalid.audit(), List.of(chainInvalid.batch()),
                        List.of(chainInvalid.signature()), snapshotJson);
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                chainInvalidItem.getEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.CHAIN_INVALID),
                chainInvalidItem.getReasonCodes());
        assertNoResponsibilityAttribution(chainInvalidItem);
        assertHistoryInvestigationFacts(chainInvalidItem, chainInvalid.audit());
    }

    @Test
    void historyDoesNotLoadSignatureFromOverflowRowWhenVisiblePageHasNoSignature() {
        TenantContextHolder.setTenantId(122L);
        Long executionId = 1220L;
        MesProBatchRecordExecutionDO execution = baseExecution(executionId, standardSnapshot("\"\""), null);
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection visible =
                historyAudit(executionId, 920L, 9L, 998L, 320L, "Visible Actor");
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection overflow =
                historyAudit(executionId, 919L, 9L, 999L, 321L, "Overflow Actor");
        when(executionMapper.selectById(executionId)).thenReturn(execution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                null, null, 2))
                .thenReturn(List.of(visible, overflow));
        when(batchMapper.selectListByExecutionId(executionId)).thenReturn(List.of());
        when(signatureMapper.selectResponsibilityListByIds(eq(Set.of(998L))))
                .thenReturn(List.of(new MesProBatchRecordExecutionSignatureDO().setId(998L)));

        MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO response =
                responsibilityService.getHistory(historyReq(executionId, 1));

        assertEquals(List.of(920L), response.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO::getAuditItemId).toList());
        assertTrue(response.getHasMore());
        assertEquals(9L, response.getNextCursorFieldAuditRevision());
        assertEquals(920L, response.getNextCursorAuditItemId());
        verify(signatureMapper, times(1)).selectResponsibilityListByIds(eq(Set.of(998L)));
        verify(signatureMapper, never()).selectResponsibilityListByIds(
                org.mockito.ArgumentMatchers.argThat(ids -> ids.contains(999L)));

        org.mockito.Mockito.clearInvocations(
                executionMapper, permissionScopeService, itemMapper, batchMapper, signatureMapper);
        Long emptyExecutionId = 1221L;
        MesProBatchRecordExecutionDO emptyExecution =
                baseExecution(emptyExecutionId, standardSnapshot("\"\""), null);
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection emptyVisible =
                historyAudit(emptyExecutionId, 921L, 10L, null, 322L, "Empty Visible Actor");
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection emptyOverflow =
                historyAudit(emptyExecutionId, 918L, 9L, 1001L, 323L, "Empty Overflow Actor");
        when(executionMapper.selectById(emptyExecutionId)).thenReturn(emptyExecution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityHistoryProjectionPage(
                emptyExecutionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                null, null, 2))
                .thenReturn(List.of(emptyVisible, emptyOverflow));
        when(batchMapper.selectListByExecutionId(emptyExecutionId)).thenReturn(List.of());

        MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO emptyResponse =
                responsibilityService.getHistory(historyReq(emptyExecutionId, 1));

        assertEquals(List.of(921L), emptyResponse.getList().stream()
                .map(MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO::getAuditItemId).toList());
        verify(signatureMapper, never()).selectResponsibilityListByIds(any());
    }

    @Test
    void closedV1ContractsExposeExactEnumsAndFields() throws Exception {
        assertNotNull(responsibilityService);
        assertArrayEquals(new Class<?>[]{MesProBatchRecordExecutionFieldResponsibilityService.class},
                getClass().getAnnotation(Import.class).value());

        assertEnumValues(MesProBatchRecordExecutionResponsibilityEvidenceStatus.class,
                "COMPLETE", "EVIDENCE_MISSING", "BLOCKED");
        assertEnumValues(MesProBatchRecordExecutionResponsibilityValueOrigin.class,
                "HUMAN", "SYSTEM_BASELINE", "EMPTY_UNTOUCHED", "UNKNOWN");
        assertEnumValues(MesProBatchRecordExecutionResponsibilityReasonCode.class,
                "EXECUTION_SNAPSHOT_MISSING",
                "FIELD_DEFINITION_MISSING",
                "BASELINE_MISSING",
                "FIELD_AUDIT_MISSING",
                "SIGNATURE_MISSING",
                "SIGNATURE_INVALID",
                "CHAIN_INVALID",
                "CURRENT_VALUE_MISMATCH",
                "FIELD_IDENTITY_AMBIGUOUS",
                "CROSS_TENANT_ASSOCIATION",
                "CROSS_EXECUTION_ASSOCIATION");
        assertEnumValues(MesProBatchRecordExecutionResponsibilityContextWarning.class,
                "VERSION_CONTEXT_MISSING");
        assertFalse(Arrays.stream(MesProBatchRecordExecutionResponsibilityReasonCode.values())
                .map(Enum::name)
                .anyMatch("VERSION_CONTEXT_MISSING"::equals));

        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class,
                fields(
                        "executionId", Long.class,
                        "pageNo", Integer.class,
                        "pageSize", Integer.class,
                        "fieldKeyword", String.class,
                        "evidenceStatus", MesProBatchRecordExecutionResponsibilityEvidenceStatus.class,
                        "valueOrigin", MesProBatchRecordExecutionResponsibilityValueOrigin.class,
                        "actorId", Long.class));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO.class,
                fields(
                        "executionId", Long.class,
                        "executionCode", String.class,
                        "batchRecordDefinitionId", Long.class,
                        "batchRecordVersionId", Long.class,
                        "batchRecordReportId", String.class,
                        "fieldAuditRevision", Long.class,
                        "fieldAuditHeadHash", String.class,
                        "cellValuesHash", String.class,
                        "overallEvidenceStatus", MesProBatchRecordExecutionResponsibilityEvidenceStatus.class,
                        "overallReasonCodes",
                        "java.util.List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode>",
                        "contextWarnings",
                        "java.util.List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityContextWarning>",
                        "total", Long.class,
                        "list",
                        "java.util.List<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityItemRespVO>"));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilityItemRespVO.class,
                fields(
                        "fieldPath", String.class,
                        "fieldKey", String.class,
                        "fieldLabel", String.class,
                        "rowIndex", Integer.class,
                        "columnIndex", Integer.class,
                        "component", String.class,
                        "valueType", String.class,
                        "currentValueJson", String.class,
                        "currentValueDisplay", String.class,
                        "currentValueHash", String.class,
                        "valueOrigin", MesProBatchRecordExecutionResponsibilityValueOrigin.class,
                        "firstHumanActorId", Long.class,
                        "firstHumanActorName", String.class,
                        "firstHumanChangedAt", java.time.LocalDateTime.class,
                        "currentValueActorId", Long.class,
                        "currentValueActorName", String.class,
                        "currentValueChangedAt", java.time.LocalDateTime.class,
                        "evidenceStatus", MesProBatchRecordExecutionResponsibilityEvidenceStatus.class,
                        "reasonCodes",
                        "java.util.List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode>",
                        "historyCount", Long.class,
                        "latestAuditItemId", Long.class));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class,
                fields(
                        "executionId", Long.class,
                        "fieldPath", String.class,
                        "fieldKey", String.class,
                        "rowIndex", Integer.class,
                        "columnIndex", Integer.class,
                        "pageSize", Integer.class,
                        "cursorFieldAuditRevision", Long.class,
                        "cursorAuditItemId", Long.class));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO.class,
                fields(
                        "executionId", Long.class,
                        "fieldPath", String.class,
                        "fieldKey", String.class,
                        "rowIndex", Integer.class,
                        "columnIndex", Integer.class,
                        "list",
                        "java.util.List<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO>",
                        "hasMore", Boolean.class,
                        "nextCursorFieldAuditRevision", Long.class,
                        "nextCursorAuditItemId", Long.class));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO.class,
                fields(
                        "auditItemId", Long.class,
                        "auditBatchId", Long.class,
                        "fieldAuditRevision", Long.class,
                        "oldValueJson", String.class,
                        "oldValueDisplay", String.class,
                        "oldValueHash", String.class,
                        "newValueJson", String.class,
                        "newValueDisplay", String.class,
                        "newValueHash", String.class,
                        "reasonCategory", String.class,
                        "reasonText", String.class,
                        "actorId", Long.class,
                        "actorName", String.class,
                        "changedAt", java.time.LocalDateTime.class,
                        "signatureId", Long.class,
                        "signatureActorUsernameSnapshot", String.class,
                        "signatureActorNicknameSnapshot", String.class,
                        "signatureDisplayAt", java.time.LocalDateTime.class,
                        "signatureProjectionHash", String.class,
                        "previousHash", String.class,
                        "auditHash", String.class,
                        "evidenceStatus", MesProBatchRecordExecutionResponsibilityEvidenceStatus.class,
                        "reasonCodes",
                        "java.util.List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode>"));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilityExportReqVO.class,
                fields(
                        "executionId", Long.class,
                        "format", String.class));
        assertDeclaredFields(MesProBatchRecordExecutionFieldResponsibilityExportRespVO.class,
                fields(
                        "fileName", String.class,
                        "format", String.class,
                        "contentType", String.class,
                        "contentBase64", String.class,
                        "sha256", String.class,
                        "recordCount", Long.class,
                        "fieldAuditRevision", Long.class,
                        "fieldAuditHeadHash", String.class,
                        "cellValuesHash", String.class,
                        "evidenceStatus", MesProBatchRecordExecutionResponsibilityEvidenceStatus.class,
                        "reasonCodes",
                        "java.util.List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityReasonCode>",
                        "contextWarnings",
                        "java.util.List<cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionResponsibilityContextWarning>",
                        "generatedAt", java.time.LocalDateTime.class));

        assertReadOnlyMethod(MesProBatchRecordExecutionFieldResponsibilityService.class, "getSummary",
                MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class,
                MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO.class);
        assertReadOnlyMethod(MesProBatchRecordExecutionFieldResponsibilityService.class, "getHistory",
                MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class,
                MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO.class);
        assertReadOnlyMethod(MesProBatchRecordExecutionFieldResponsibilityService.class, "export",
                MesProBatchRecordExecutionFieldResponsibilityExportReqVO.class,
                MesProBatchRecordExecutionFieldResponsibilityExportRespVO.class);
        assertMethod(MesProBatchRecordExecutionFieldAuditService.class, "getResponsibilitySummary",
                MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO.class,
                MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO.class);
        assertMethod(MesProBatchRecordExecutionFieldAuditService.class, "getResponsibilityHistory",
                MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO.class,
                MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO.class);
        assertMethod(MesProBatchRecordExecutionFieldAuditService.class, "exportResponsibility",
                MesProBatchRecordExecutionFieldResponsibilityExportReqVO.class,
                MesProBatchRecordExecutionFieldResponsibilityExportRespVO.class);

        assertRequestValidationContracts();
        assertPrivacyBoundary();
    }

    private ResponsibilityFixture responsibilityFixture() {
        return responsibilityFixture(true);
    }

    private ResponsibilityFixture isolatedResponsibilityFixture() {
        return responsibilityFixture(false);
    }

    private ResponsibilityFixture responsibilityFixture(boolean includeAmbiguousFields) {
        String currentValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 37.2);
        String snapshotJson = includeAmbiguousFields ? """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","label":"Temperature","rowIndex":1,"columnIndex":1,"component":"input-number","valueType":"NUMBER","defaultValue":20},
                  {"fieldPath":"sheet[0].rows[1].cells[2]","fieldKey":"batchCode","label":"Batch Code","rowIndex":1,"columnIndex":2,"component":"input","valueType":"STRING","defaultValue":"AUTO-01"},
                  {"fieldPath":"sheet[0].rows[1].cells[3]","fieldKey":"notes","label":"Notes","rowIndex":1,"columnIndex":3,"component":"input","valueType":"NULL","defaultValue":null},
                  {"fieldPath":"sheet[0].rows[2].cells[1]","fieldKey":"ambiguous","label":"Hidden conflict A","rowIndex":2,"columnIndex":1,"component":"input","valueType":"NULL","defaultValue":null},
                  {"fieldPath":"sheet[0].rows[2].cells[1]","fieldKey":"ambiguous","label":"Hidden conflict B","rowIndex":2,"columnIndex":1,"component":"input","valueType":"NULL","defaultValue":null}
                ]}
                """ : """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","label":"Temperature","rowIndex":1,"columnIndex":1,"component":"input-number","valueType":"NUMBER","defaultValue":20},
                  {"fieldPath":"sheet[0].rows[1].cells[2]","fieldKey":"batchCode","label":"Batch Code","rowIndex":1,"columnIndex":2,"component":"input","valueType":"STRING","defaultValue":"AUTO-01"},
                  {"fieldPath":"sheet[0].rows[1].cells[3]","fieldKey":"notes","label":"Notes","rowIndex":1,"columnIndex":3,"component":"input","valueType":"NULL","defaultValue":null}
                ]}
                """;
        String cellValuesJson = """
                [{"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"temperature","rowIndex":1,"columnIndex":1,"valueType":"NUMBER","value":37.2,"valueDisplay":"37.2","valueHash":"%s"}]
                """.formatted(currentValueHash);
        String cellValuesHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson);
        LocalDateTime changedAt = LocalDateTime.of(2026, 7, 10, 9, 30);
        MesProBatchRecordExecutionSignatureDO signature = new MesProBatchRecordExecutionSignatureDO()
                .setId(501L)
                .setExecutionId(1001L)
                .setActionType("FIELD_CHANGE")
                .setActorId(101L)
                .setActorName("Alice")
                .setSignatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                .setPasswordVerified(Boolean.TRUE)
                .setSignedAt(changedAt)
                .setSignatureDisplayAt(changedAt)
                .setSignatureTimeMode("SERVER")
                .setSelectedTimeZone("")
                .setSelectedTimeReason("")
                .setSelectedTimePolicyVersion("")
                .setSelectedTimeAuditHash("")
                .setReasonCategory("OPERATOR_ENTRY")
                .setReason("entered")
                .setAuditBatchId(601L)
                .setSignatureChallengeHash("challenge-501")
                .setFieldAuditRevision(1L)
                .setCellValuesHash(cellValuesHash);
        String signatureProjectionHash = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureProjection(
                signatureProjection(signature));
        String oldValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.NUMBER, 20);
        String previousHash = MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH;
        String auditHash = MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                        .fieldPath("sheet[0].rows[1].cells[1]")
                        .fieldKey("temperature")
                        .rowIndex(1)
                        .columnIndex(1)
                        .valueType(MesProBatchRecordExecutionFieldAuditValueType.NUMBER)
                        .oldValueJson("20")
                        .oldValueDisplay("20")
                        .oldValueHash(oldValueHash)
                        .newValueJson("37.2")
                        .newValueDisplay("37.2")
                        .newValueHash(currentValueHash)
                        .reasonCategory("OPERATOR_ENTRY")
                        .reasonText("entered")
                        .actorId(101L)
                        .actorName("Alice")
                        .signatureProjectionHash(signatureProjectionHash)
                        .previousHash(previousHash)
                        .changedAt(changedAt)
                        .build());
        signature.setFieldAuditHeadHash(auditHash);
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit =
                MesProBatchRecordExecutionFieldResponsibilityAuditProjection.builder()
                        .auditItemId(401L)
                        .auditBatchId(601L)
                        .executionId(1001L)
                        .tenantId(122L)
                        .fieldAuditRevision(1L)
                        .batchItemIndex(0)
                        .fieldPath("sheet[0].rows[1].cells[1]")
                        .fieldKey("temperature")
                        .fieldLabel("Temperature")
                        .rowIndex(1)
                        .columnIndex(1)
                        .component("input-number")
                        .valueType("NUMBER")
                        .oldValueJson("20")
                        .oldValueDisplay("20")
                        .oldValueHash(oldValueHash)
                        .newValueJson("37.2")
                        .newValueDisplay("37.2")
                        .newValueHash(currentValueHash)
                        .reasonCategory("OPERATOR_ENTRY")
                        .reasonText("entered")
                        .actorId(101L)
                        .actorName("Alice")
                        .signatureId(501L)
                        .signatureProjectionHash(signatureProjectionHash)
                        .previousHash(previousHash)
                        .auditHash(auditHash)
                        .afterCellValuesHash(cellValuesHash)
                        .executionSnapshotHash(
                                MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(snapshotJson))
                        .changedAt(changedAt)
                        .build();
        MesProBatchRecordExecutionFieldAuditBatchDO batch =
                new MesProBatchRecordExecutionFieldAuditBatchDO()
                        .setId(601L)
                        .setExecutionId(1001L)
                        .setActionType("FIELD_CHANGE")
                        .setReasonCategory("OPERATOR_ENTRY")
                        .setReasonText("entered")
                        .setFieldCount(1)
                        .setActorId(101L)
                        .setActorName("Alice")
                        .setSignatureId(501L)
                        .setSignatureChallengeHash("challenge-501")
                        .setSignatureProjectionHash(signatureProjectionHash)
                        .setAfterCellValuesHash(cellValuesHash)
                        .setAfterFieldAuditRevision(1L)
                        .setPreviousHeadHash(previousHash)
                        .setNewHeadHash(auditHash)
                        .setChangedAt(changedAt)
                        .setTenantId(122L);
        MesProBatchRecordExecutionDO execution = new MesProBatchRecordExecutionDO()
                .setId(1001L)
                .setExecutionCode("EXE-1001")
                .setPermissionScopeId(801L)
                .setExecutionSnapshotJson(snapshotJson)
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(cellValuesHash)
                .setFieldAuditRevision(1L)
                .setFieldAuditHeadHash(auditHash);
        return new ResponsibilityFixture(execution, audit, batch, signature, currentValueHash);
    }

    private MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary(
            MesProBatchRecordExecutionDO execution,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits,
            List<MesProBatchRecordExecutionFieldAuditBatchDO> batches,
            List<MesProBatchRecordExecutionSignatureDO> signatures) {
        return summary(execution, audits, batches, signatures,
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO()
                        .setExecutionId(execution.getId())
                        .setPageSize(200));
    }

    private MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary(
            MesProBatchRecordExecutionDO execution,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits,
            List<MesProBatchRecordExecutionFieldAuditBatchDO> batches,
            List<MesProBatchRecordExecutionSignatureDO> signatures,
            MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO) {
        TenantContextHolder.setTenantId(122L);
        when(executionMapper.selectById(execution.getId())).thenReturn(execution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityProjectionList(execution.getId())).thenReturn(audits);
        when(batchMapper.selectListByExecutionId(execution.getId())).thenReturn(batches);
        when(signatureMapper.selectResponsibilityListByIds(any())).thenReturn(signatures);
        when(workTaskMapper.selectTimelineListByExecutionId(execution.getId())).thenReturn(List.of());
        return responsibilityService.getSummary(reqVO);
    }

    private MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO historyReq(Long executionId, int pageSize) {
        return new MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO()
                .setExecutionId(executionId)
                .setFieldPath("sheet[0].rows[1].cells[1]")
                .setFieldKey("field")
                .setRowIndex(1)
                .setColumnIndex(1)
                .setPageSize(pageSize);
    }

    private MesProBatchRecordExecutionFieldResponsibilityAuditProjection historyAudit(
            Long executionId, Long auditItemId, Long revision, Long signatureId, Long actorId, String actorName) {
        return MesProBatchRecordExecutionFieldResponsibilityAuditProjection.builder()
                .auditItemId(auditItemId)
                .auditBatchId(auditItemId + 1000)
                .executionId(executionId)
                .tenantId(122L)
                .fieldAuditRevision(revision)
                .batchItemIndex(0)
                .fieldPath("sheet[0].rows[1].cells[1]")
                .fieldKey("field")
                .rowIndex(1)
                .columnIndex(1)
                .valueType("STRING")
                .oldValueJson("\"old\"")
                .oldValueDisplay("old")
                .newValueJson("\"new\"")
                .newValueDisplay("new")
                .reasonCategory("OPERATOR_ENTRY")
                .reasonText("entered")
                .actorId(actorId)
                .actorName(actorName)
                .signatureId(signatureId)
                .previousHash("previous-" + auditItemId)
                .auditHash("audit-" + auditItemId)
                .changedAt(LocalDateTime.of(2026, 7, 10, 12, 0))
                .build();
    }

    private MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO firstHistoryItem(
            Long executionId,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            List<MesProBatchRecordExecutionFieldAuditBatchDO> batches,
            List<MesProBatchRecordExecutionSignatureDO> signatures,
            String snapshotJson) {
        MesProBatchRecordExecutionDO execution = baseExecution(executionId, snapshotJson, "[]");
        when(executionMapper.selectById(executionId)).thenReturn(execution);
        when(permissionScopeService.evaluate(any())).thenReturn(new MesProEdhrPermissionEvaluateResult()
                .setDecisions(Map.of("AUDIT_VIEW", "ALLOW")));
        when(itemMapper.selectResponsibilityHistoryProjectionPage(
                executionId, "sheet[0].rows[1].cells[1]", "field", 1, 1,
                null, null, 2))
                .thenReturn(List.of(audit));
        when(batchMapper.selectListByExecutionId(executionId)).thenReturn(batches);
        if (!signatures.isEmpty()) {
            Set<Long> signatureIds = signatures.stream()
                    .map(MesProBatchRecordExecutionSignatureDO::getId)
                    .collect(Collectors.toSet());
            when(signatureMapper.selectResponsibilityListByIds(eq(signatureIds))).thenReturn(signatures);
        }
        return responsibilityService.getHistory(historyReq(executionId, 1)).getList().get(0);
    }

    private HumanAudit historyHumanAudit(
            Long executionId,
            Long auditItemId,
            Long auditBatchId,
            Long signatureId,
            Long revision,
            String previousHash,
            String fixtureName,
            Long actorId,
            String actorName,
            LocalDateTime changedAt,
            String afterCellValuesHash,
            String snapshotHash) {
        String oldValue = fixtureName + "-old";
        String newValue = fixtureName + "-new";
        return humanAudit(
                executionId, auditItemId, auditBatchId, signatureId, revision, previousHash,
                "\"" + oldValue + "\"", oldValue,
                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, oldValue),
                "\"" + newValue + "\"", newValue,
                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, newValue),
                actorId, actorName, changedAt, afterCellValuesHash, snapshotHash,
                fixtureName + "-category", fixtureName + "-reason");
    }

    private void assertNoResponsibilityAttribution(
            MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO item) {
        assertNull(item.getActorId());
        assertNull(item.getActorName());
        assertNull(item.getSignatureActorUsernameSnapshot());
        assertNull(item.getSignatureActorNicknameSnapshot());
        assertNull(item.getSignatureDisplayAt());
    }

    private void assertHistoryInvestigationFacts(
            MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO item,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit) {
        assertEquals(audit.getOldValueJson(), item.getOldValueJson());
        assertEquals(audit.getOldValueDisplay(), item.getOldValueDisplay());
        assertEquals(audit.getOldValueHash(), item.getOldValueHash());
        assertEquals(audit.getNewValueJson(), item.getNewValueJson());
        assertEquals(audit.getNewValueDisplay(), item.getNewValueDisplay());
        assertEquals(audit.getNewValueHash(), item.getNewValueHash());
        assertEquals(audit.getReasonCategory(), item.getReasonCategory());
        assertEquals(audit.getReasonText(), item.getReasonText());
        assertEquals(audit.getChangedAt(), item.getChangedAt());
        assertEquals(audit.getSignatureId(), item.getSignatureId());
        assertEquals(audit.getSignatureProjectionHash(), item.getSignatureProjectionHash());
        assertEquals(audit.getPreviousHash(), item.getPreviousHash());
        assertEquals(audit.getAuditHash(), item.getAuditHash());
    }

    private void assertBlockedOverallFromCompleteUniverse(
            MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary) {
        assertEquals(MesProBatchRecordExecutionResponsibilityEvidenceStatus.BLOCKED,
                summary.getOverallEvidenceStatus());
        assertEquals(List.of(MesProBatchRecordExecutionResponsibilityReasonCode.FIELD_IDENTITY_AMBIGUOUS),
                summary.getOverallReasonCodes());
    }

    private MesProBatchRecordExecutionDO untouchedExecution(Long executionId, String defaultValueJson,
                                                            String defaultDisplay) {
        String snapshotJson = standardSnapshot(defaultValueJson);
        MesProBatchRecordExecutionFieldAuditValueType valueType =
                MesProBatchRecordExecutionFieldAuditValueType.STRING;
        Object value = "null".equals(defaultValueJson) ? null : defaultDisplay;
        String valueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(valueType, value);
        return baseExecution(executionId, snapshotJson,
                cellValuesJson(defaultValueJson, defaultDisplay, valueHash));
    }

    private MesProBatchRecordExecutionDO baseExecution(Long executionId, String snapshotJson, String cellValuesJson) {
        return new MesProBatchRecordExecutionDO()
                .setId(executionId)
                .setExecutionCode("EXE-" + executionId)
                .setPermissionScopeId(801L)
                .setExecutionSnapshotJson(snapshotJson)
                .setCellValuesJson(cellValuesJson)
                .setCellValuesHash(cellValuesJson == null ? null
                        : MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson));
    }

    private String standardSnapshot(String defaultValueJson) {
        return """
                {"fields":[
                  {"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"field","label":"Field","rowIndex":1,"columnIndex":1,"component":"input","valueType":"STRING","defaultValue":%s}
                ]}
                """.formatted(defaultValueJson);
    }

    private String cellValuesJson(String valueJson, String valueDisplay, String valueHash) {
        return """
                [{"fieldPath":"sheet[0].rows[1].cells[1]","fieldKey":"field","rowIndex":1,"columnIndex":1,"valueType":"STRING","value":%s,"valueDisplay":"%s","valueHash":"%s"}]
                """.formatted(valueJson, valueDisplay, valueHash);
    }

    private ClearingFixture clearingFixture() {
        Long executionId = 1107L;
        String snapshotJson = standardSnapshot("\"\"");
        String firstCellValuesJson = cellValuesJson("\"entered\"", "entered",
                MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "entered"));
        String clearedValueHash = MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                MesProBatchRecordExecutionFieldAuditValueType.STRING, "");
        String finalCellValuesJson = cellValuesJson("\"\"", "", clearedValueHash);
        String firstCellValuesHash =
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(firstCellValuesJson);
        String finalCellValuesHash =
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(finalCellValuesJson);
        String snapshotHash =
                MesProBatchRecordExecutionFieldAuditHasher.hashExecutionSnapshot(snapshotJson);
        LocalDateTime firstChangedAt = LocalDateTime.of(2026, 7, 10, 9, 30);
        LocalDateTime currentChangedAt = LocalDateTime.of(2026, 7, 10, 10, 30);

        HumanAudit first = humanAudit(executionId, 401L, 601L, 501L, 1L,
                MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                "\"\"", "", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, ""),
                "\"entered\"", "entered", MesProBatchRecordExecutionFieldAuditHasher.hashTypedValue(
                        MesProBatchRecordExecutionFieldAuditValueType.STRING, "entered"),
                101L, "Alice", firstChangedAt, firstCellValuesHash, snapshotHash);
        HumanAudit current = humanAudit(executionId, 402L, 602L, 502L, 2L,
                first.audit().getAuditHash(),
                "\"entered\"", "entered", first.audit().getNewValueHash(),
                "\"\"", "", clearedValueHash,
                202L, "Bob", currentChangedAt, finalCellValuesHash, snapshotHash);
        MesProBatchRecordExecutionDO execution = baseExecution(executionId, snapshotJson, finalCellValuesJson)
                .setFieldAuditRevision(2L)
                .setFieldAuditHeadHash(current.audit().getAuditHash());
        return new ClearingFixture(execution,
                List.of(first.audit(), current.audit()),
                List.of(first.batch(), current.batch()),
                List.of(first.signature(), current.signature()),
                clearedValueHash, firstChangedAt, currentChangedAt);
    }

    private HumanAudit humanAudit(
            Long executionId,
            Long auditItemId,
            Long auditBatchId,
            Long signatureId,
            Long revision,
            String previousHash,
            String oldValueJson,
            String oldValueDisplay,
            String oldValueHash,
            String newValueJson,
            String newValueDisplay,
            String newValueHash,
            Long actorId,
            String actorName,
            LocalDateTime changedAt,
            String afterCellValuesHash,
            String snapshotHash) {
        return humanAudit(
                executionId, auditItemId, auditBatchId, signatureId, revision, previousHash,
                oldValueJson, oldValueDisplay, oldValueHash,
                newValueJson, newValueDisplay, newValueHash,
                actorId, actorName, changedAt, afterCellValuesHash, snapshotHash,
                "OPERATOR_ENTRY", "entered");
    }

    private HumanAudit humanAudit(
            Long executionId,
            Long auditItemId,
            Long auditBatchId,
            Long signatureId,
            Long revision,
            String previousHash,
            String oldValueJson,
            String oldValueDisplay,
            String oldValueHash,
            String newValueJson,
            String newValueDisplay,
            String newValueHash,
            Long actorId,
            String actorName,
            LocalDateTime changedAt,
            String afterCellValuesHash,
            String snapshotHash,
            String reasonCategory,
            String reasonText) {
        String challengeHash = "challenge-" + signatureId;
        MesProBatchRecordExecutionSignatureDO signature = new MesProBatchRecordExecutionSignatureDO()
                .setId(signatureId)
                .setExecutionId(executionId)
                .setActionType("FIELD_CHANGE")
                .setActorId(actorId)
                .setActorName(actorName)
                .setSignatureMode(MesProBatchRecordExecutionSignatureService.SIGNATURE_MODE_PASSWORD)
                .setPasswordVerified(Boolean.TRUE)
                .setSignedAt(changedAt)
                .setSignatureDisplayAt(changedAt)
                .setSignatureTimeMode("SERVER")
                .setSelectedTimeZone("")
                .setSelectedTimeReason("")
                .setSelectedTimePolicyVersion("")
                .setSelectedTimeAuditHash("")
                .setReasonCategory(reasonCategory)
                .setReason(reasonText)
                .setAuditBatchId(auditBatchId)
                .setSignatureChallengeHash(challengeHash)
                .setFieldAuditRevision(revision)
                .setCellValuesHash(afterCellValuesHash);
        String signatureProjectionHash = MesProBatchRecordExecutionFieldAuditHasher.hashSignatureProjection(
                signatureProjection(signature));
        String auditHash = MesProBatchRecordExecutionFieldAuditHasher.hashItem(
                MesProBatchRecordExecutionFieldAuditItemHashInput.builder()
                        .fieldPath("sheet[0].rows[1].cells[1]")
                        .fieldKey("field")
                        .rowIndex(1)
                        .columnIndex(1)
                        .valueType(MesProBatchRecordExecutionFieldAuditValueType.STRING)
                        .oldValueJson(oldValueJson)
                        .oldValueDisplay(oldValueDisplay)
                        .oldValueHash(oldValueHash)
                        .newValueJson(newValueJson)
                        .newValueDisplay(newValueDisplay)
                        .newValueHash(newValueHash)
                        .reasonCategory(reasonCategory)
                        .reasonText(reasonText)
                        .actorId(actorId)
                        .actorName(actorName)
                        .signatureProjectionHash(signatureProjectionHash)
                        .previousHash(previousHash)
                        .changedAt(changedAt)
                        .build());
        signature.setFieldAuditHeadHash(auditHash);
        MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit =
                MesProBatchRecordExecutionFieldResponsibilityAuditProjection.builder()
                        .auditItemId(auditItemId)
                        .auditBatchId(auditBatchId)
                        .executionId(executionId)
                        .tenantId(122L)
                        .fieldAuditRevision(revision)
                        .batchItemIndex(0)
                        .fieldPath("sheet[0].rows[1].cells[1]")
                        .fieldKey("field")
                        .fieldLabel("Field")
                        .rowIndex(1)
                        .columnIndex(1)
                        .component("input")
                        .valueType("STRING")
                        .oldValueJson(oldValueJson)
                        .oldValueDisplay(oldValueDisplay)
                        .oldValueHash(oldValueHash)
                        .newValueJson(newValueJson)
                        .newValueDisplay(newValueDisplay)
                        .newValueHash(newValueHash)
                        .reasonCategory(reasonCategory)
                        .reasonText(reasonText)
                        .actorId(actorId)
                        .actorName(actorName)
                        .signatureId(signatureId)
                        .signatureProjectionHash(signatureProjectionHash)
                        .previousHash(previousHash)
                        .auditHash(auditHash)
                        .afterCellValuesHash(afterCellValuesHash)
                        .executionSnapshotHash(snapshotHash)
                        .changedAt(changedAt)
                        .build();
        MesProBatchRecordExecutionFieldAuditBatchDO batch =
                new MesProBatchRecordExecutionFieldAuditBatchDO()
                        .setId(auditBatchId)
                        .setExecutionId(executionId)
                        .setActionType("FIELD_CHANGE")
                        .setReasonCategory(reasonCategory)
                        .setReasonText(reasonText)
                        .setFieldCount(1)
                        .setActorId(actorId)
                        .setActorName(actorName)
                        .setSignatureId(signatureId)
                        .setSignatureChallengeHash(challengeHash)
                        .setSignatureProjectionHash(signatureProjectionHash)
                        .setAfterCellValuesHash(afterCellValuesHash)
                        .setAfterFieldAuditRevision(revision)
                        .setPreviousHeadHash(previousHash)
                        .setNewHeadHash(auditHash)
                        .setChangedAt(changedAt)
                        .setTenantId(122L);
        return new HumanAudit(audit, batch, signature);
    }

    private MesProBatchRecordExecutionFieldAuditSignatureProjection signatureProjection(
            MesProBatchRecordExecutionSignatureDO signature) {
        return new MesProBatchRecordExecutionFieldAuditSignatureProjection()
                .setId(signature.getId())
                .setExecutionId(signature.getExecutionId())
                .setActionType(signature.getActionType())
                .setActorId(signature.getActorId())
                .setActorName(signature.getActorName())
                .setSignatureMode(signature.getSignatureMode())
                .setPasswordVerified(signature.getPasswordVerified())
                .setSignedAt(signature.getSignedAt())
                .setSelectedSignedAt(signature.getSelectedSignedAt())
                .setSignatureDisplayAt(signature.getSignatureDisplayAt())
                .setSignatureTimeMode(signature.getSignatureTimeMode())
                .setSelectedTimeZone(signature.getSelectedTimeZone())
                .setSelectedTimeReason(signature.getSelectedTimeReason())
                .setSelectedTimePolicyVersion(signature.getSelectedTimePolicyVersion())
                .setSelectedTimeAuditHash(signature.getSelectedTimeAuditHash())
                .setReasonCategory(signature.getReasonCategory())
                .setReasonText(signature.getReason())
                .setAuditBatchId(signature.getAuditBatchId())
                .setSignatureChallengeHash(signature.getSignatureChallengeHash())
                .setFieldAuditRevision(signature.getFieldAuditRevision())
                .setFieldAuditHeadHash(signature.getFieldAuditHeadHash())
                .setCellValuesHash(signature.getCellValuesHash());
    }

    private MesProBatchRecordExecutionFieldResponsibilityItemRespVO item(
            MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO summary, String fieldKey) {
        return summary.getList().stream()
                .filter(item -> fieldKey.equals(item.getFieldKey()))
                .findFirst()
                .orElseThrow();
    }

    private record ResponsibilityFixture(
            MesProBatchRecordExecutionDO execution,
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch,
            MesProBatchRecordExecutionSignatureDO signature,
            String currentValueHash) {
    }

    private record HumanAudit(
            MesProBatchRecordExecutionFieldResponsibilityAuditProjection audit,
            MesProBatchRecordExecutionFieldAuditBatchDO batch,
            MesProBatchRecordExecutionSignatureDO signature) {
    }

    private record ClearingFixture(
            MesProBatchRecordExecutionDO execution,
            List<MesProBatchRecordExecutionFieldResponsibilityAuditProjection> audits,
            List<MesProBatchRecordExecutionFieldAuditBatchDO> batches,
            List<MesProBatchRecordExecutionSignatureDO> signatures,
            String clearedValueHash,
            LocalDateTime firstChangedAt,
            LocalDateTime currentChangedAt) {
    }

    private void assertRequestValidationContracts() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO summary =
                new MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO().setExecutionId(1L);
        assertEquals(1, summary.getPageNo());
        assertEquals(50, summary.getPageSize());
        assertTrue(validator.validate(summary).isEmpty());
        assertFalse(validator.validate(summary.setPageNo(0)).isEmpty());
        assertFalse(validator.validate(summary.setPageNo(1).setPageSize(201)).isEmpty());

        MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO history =
                new MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO()
                        .setExecutionId(1L)
                        .setFieldPath("sheet[0].rows[1].cells[2]")
                        .setFieldKey("temperature")
                        .setRowIndex(1)
                        .setColumnIndex(2);
        assertEquals(50, history.getPageSize());
        assertTrue(validator.validate(history).isEmpty());
        assertFalse(validator.validate(history.setPageSize(201)).isEmpty());
        history.setPageSize(50).setCursorFieldAuditRevision(10L).setCursorAuditItemId(null);
        assertFalse(validator.validate(history).isEmpty());
        history.setCursorAuditItemId(20L);
        assertTrue(validator.validate(history).isEmpty());

        MesProBatchRecordExecutionFieldResponsibilityExportReqVO export =
                new MesProBatchRecordExecutionFieldResponsibilityExportReqVO().setExecutionId(1L);
        assertEquals("XLSX", export.getFormat());
        assertTrue(validator.validate(export).isEmpty());
        assertFalse(validator.validate(export.setFormat("CSV")).isEmpty());
    }

    private void assertPrivacyBoundary() {
        Set<String> forbidden = Set.of(
                "clientip", "useragent", "password", "passwordhash", "token", "session", "credential", "secret");
        List<Class<?>> responseTypes = List.of(
                MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO.class,
                MesProBatchRecordExecutionFieldResponsibilityItemRespVO.class,
                MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO.class,
                MesProBatchRecordExecutionFieldResponsibilityHistoryItemRespVO.class,
                MesProBatchRecordExecutionFieldResponsibilityExportRespVO.class);
        for (Class<?> type : responseTypes) {
            for (Field field : type.getDeclaredFields()) {
                String normalized = field.getName().toLowerCase();
                assertTrue(forbidden.stream().noneMatch(normalized::contains),
                        () -> type.getSimpleName() + " exposes forbidden field " + field.getName());
            }
        }
    }

    private static void assertEnumValues(Class<? extends Enum<?>> type, String... expected) {
        assertArrayEquals(expected, Arrays.stream(type.getEnumConstants()).map(Enum::name).toArray(String[]::new));
    }

    private static void assertDeclaredFields(Class<?> type, Map<String, String> expected) {
        Map<String, String> actual = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .collect(Collectors.toMap(
                        Field::getName,
                        field -> field.getGenericType().getTypeName(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        assertEquals(expected, actual, type.getSimpleName());
    }

    private static Map<String, String> fields(Object... entries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            Object type = entries[index + 1];
            result.put((String) entries[index], type instanceof Class<?> clazz ? clazz.getName() : (String) type);
        }
        return result;
    }

    private static void assertReadOnlyMethod(Class<?> owner, String methodName, Class<?> parameterType,
                                             Class<?> returnType) throws Exception {
        Method method = assertMethod(owner, methodName, parameterType, returnType);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, owner.getSimpleName() + "." + methodName + " must be transactional");
        assertTrue(transactional.readOnly(), owner.getSimpleName() + "." + methodName + " must be read-only");
    }

    private static Method assertMethod(Class<?> owner, String methodName, Class<?> parameterType,
                                       Class<?> returnType) throws Exception {
        Method method = owner.getDeclaredMethod(methodName, parameterType);
        assertEquals(returnType, method.getReturnType());
        return method;
    }
}
