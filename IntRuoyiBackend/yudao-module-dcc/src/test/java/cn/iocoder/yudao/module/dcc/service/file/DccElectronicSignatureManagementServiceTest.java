package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureExportSummaryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignaturePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureAuthorizationRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureEvidenceRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccSignatureVerifyRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class DccElectronicSignatureManagementServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;
    @Mock
    private DccElectronicSignatureAuthorizationAuditMapper authorizationAuditMapper;
    @Mock
    private AdminUserService adminUserService;
    @Mock
    private DeptService deptService;
    @Mock
    private DccElectronicSignatureAuthorizationService authorizationService;
    @Mock
    private DccElectronicSignatureAuthorizationAuditService authorizationAuditService;
    @Mock
    private DccSignatureEvidenceProperties signatureEvidenceProperties;
    @Mock
    private FileService fileService;

    @InjectMocks
    private DccElectronicSignatureManagementServiceImpl service;

    @Test
    void getSignaturePage_enrichesControlledFileAndActorMetadata() {
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        DccControlledFileSignatureDO signature = DccControlledFileSignatureDO.builder()
                .id(1L)
                .controlledFileId(900L)
                .revisionId(900L)
                .versionNo("A.1")
                .taskId("task-1")
                .actorId(99L)
                .actionType("APPROVE")
                .meaningCode("REVIEW_APPROVE")
                .signatureMode("PASSWORD")
                .comment("looks good")
                .sourceFileId(7001L)
                .sourceFileHash("0e7b12ca44fe9911")
                .controlledCopyFileId(7002L)
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .controlledCopyHash("1f8c22ee44aa")
                .evidenceHash("6f2c91ab03d4aabb")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 16, 19, 50, 0))
                .build();
        when(signatureMapper.selectPage(any(DccElectronicSignaturePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(signature), 1L));
        when(controlledFileMapper.selectBatchIds(Set.of(900L))).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(900L)
                        .title("受控文件A")
                        .fileNumber("DCC-001")
                        .status("PENDING_MATRIX_REVIEW")
                        .build()));
        when(adminUserService.getUserList(Set.of(99L))).thenReturn(List.of(
                AdminUserDO.builder()
                        .id(99L)
                        .username("auditor")
                        .nickname("审核员")
                        .build()));
        when(fileService.getFile(7001L)).thenReturn(FileDO.builder()
                .id(7001L)
                .path("dcc/source/DCC-001-A.1.pdf")
                .build());
        when(fileService.getFile(7002L)).thenReturn(FileDO.builder()
                .id(7002L)
                .path("dcc/controlled-copy/DCC-001-A.1.pdf")
                .build());

        PageResult<DccElectronicSignatureRespVO> result = service.getSignaturePage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("受控文件A", result.getList().get(0).getControlledFileTitle());
        assertEquals("DCC-001", result.getList().get(0).getControlledFileNumber());
        assertEquals("auditor", result.getList().get(0).getActorUsername());
        assertEquals("审核员", result.getList().get(0).getActorNickname());
        assertEquals(900L, result.getList().get(0).getRevisionId());
        assertEquals("A.1", result.getList().get(0).getVersionNo());
        assertEquals("REVIEW_APPROVE", result.getList().get(0).getMeaningCode());
        assertEquals("6f2c91ab03d4", result.getList().get(0).getEvidenceHashShort());
        assertEquals("VALID", result.getList().get(0).getEvidenceStatus());
        assertEquals("dcc/source/DCC-001-A.1.pdf", result.getList().get(0).getSourceObjectKey());
        assertEquals("A.1", result.getList().get(0).getSourceVersionId());
        assertEquals("dcc/controlled-copy/DCC-001-A.1.pdf", result.getList().get(0).getControlledCopyObjectKey());
        assertEquals("A.1", result.getList().get(0).getControlledCopyVersionId());
    }

    @Test
    void getSignaturePage_returnsHistoricalUnboundAndValidRowsForSignerFilter() {
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignerUserId(113L);
        DccControlledFileSignatureDO historical = DccControlledFileSignatureDO.builder()
                .id(2001L)
                .controlledFileId(901L)
                .revisionId(901L)
                .versionNo("A.0")
                .taskId("history-task-return")
                .actorId(113L)
                .actorUsernameSnapshot("aoteman")
                .actorNicknameSnapshot("芋道1")
                .actionType("RETURN")
                .evidenceStatus("HISTORICAL_UNBOUND")
                .signedAt(LocalDateTime.of(2026, 5, 26, 9, 0, 0))
                .build();
        DccControlledFileSignatureDO valid = DccControlledFileSignatureDO.builder()
                .id(2002L)
                .controlledFileId(902L)
                .revisionId(902L)
                .versionNo("A.1")
                .taskId("bpm-task-review")
                .actorId(113L)
                .actorUsernameSnapshot("aoteman")
                .actorNicknameSnapshot("芋道1")
                .actionType("APPROVE")
                .meaningCode("MATRIX_REVIEW_APPROVE")
                .sourceFileHash("0e7b12ca44fe9911")
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .evidencePayloadVersion("v1")
                .evidenceKeyVersion("kv1")
                .evidenceHash("6f2c91ab03d4aabb")
                .evidenceHashAlgorithm("HMAC_SHA256")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 10, 0, 0))
                .build();
        when(signatureMapper.selectPage(any(DccElectronicSignaturePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(historical, valid), 2L));
        when(controlledFileMapper.selectBatchIds(Set.of(901L, 902L))).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(901L)
                        .title("历史退回文件")
                        .fileNumber("DCC-HIS-RETURN")
                        .status("ARCHIVED")
                        .build(),
                DccControlledFileDO.builder()
                        .id(902L)
                        .title("新审核文件")
                        .fileNumber("CODEX-E2E-RETURN-2440108")
                        .status("APPROVING")
                        .build()));
        when(adminUserService.getUserList(Set.of(113L))).thenReturn(List.of(
                AdminUserDO.builder()
                        .id(113L)
                        .username("aoteman")
                        .nickname("芋道1")
                        .build()));

        PageResult<DccElectronicSignatureRespVO> result = service.getSignaturePage(reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getList().size());
        assertEquals(2001L, result.getList().get(0).getId());
        assertEquals("RETURNED", result.getList().get(0).getTaskActionResult());
        assertEquals("HISTORICAL_UNBOUND", result.getList().get(0).getEvidenceStatus());
        assertEquals(2002L, result.getList().get(1).getId());
        assertEquals("APPROVED", result.getList().get(1).getTaskActionResult());
        assertEquals("MATRIX_REVIEW_APPROVE", result.getList().get(1).getMeaningCode());
        assertEquals("VALID", result.getList().get(1).getEvidenceStatus());
    }

    @Test
    void getSignaturePage_historicalUnboundUnsupportedActionRemainsVisibleForAudit() {
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignerUserId(113L);
        DccControlledFileSignatureDO historical = DccControlledFileSignatureDO.builder()
                .id(2004L)
                .controlledFileId(906L)
                .revisionId(906L)
                .versionNo("A.0")
                .taskId("legacy-task-archive")
                .actorId(113L)
                .actorUsernameSnapshot("aoteman")
                .actorNicknameSnapshot("芋道1")
                .actionType("LEGACY_ARCHIVE")
                .evidenceStatus("HISTORICAL_UNBOUND")
                .signedAt(LocalDateTime.of(2026, 5, 26, 9, 30, 0))
                .build();
        when(signatureMapper.selectPage(any(DccElectronicSignaturePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(historical), 1L));
        when(controlledFileMapper.selectBatchIds(Set.of(906L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(906L).fileNumber("DCC-HIS-ARCHIVE").build()));
        when(adminUserService.getUserList(Set.of(113L))).thenReturn(List.of(
                AdminUserDO.builder().id(113L).username("aoteman").nickname("芋道1").build()));

        PageResult<DccElectronicSignatureRespVO> result = service.getSignaturePage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("LEGACY_ARCHIVE", result.getList().get(0).getTaskActionResult());
        assertEquals("HISTORICAL_UNBOUND", result.getList().get(0).getEvidenceStatus());
    }

    @Test
    void getSignaturePage_validRowWithUnknownActionStillFailsFast() {
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignerUserId(113L);
        DccControlledFileSignatureDO invalidNewEvidence = DccControlledFileSignatureDO.builder()
                .id(2003L)
                .controlledFileId(902L)
                .revisionId(902L)
                .versionNo("A.1")
                .taskId("bpm-task-review")
                .actorId(113L)
                .actionType("ARCHIVE")
                .meaningCode("MATRIX_REVIEW_ARCHIVE")
                .evidenceHash("6f2c91ab03d4aabb")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 10, 0, 0))
                .build();
        when(signatureMapper.selectPage(any(DccElectronicSignaturePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(invalidNewEvidence), 1L));
        when(controlledFileMapper.selectBatchIds(Set.of(902L))).thenReturn(List.of(
                DccControlledFileDO.builder()
                        .id(902L)
                        .title("新审核文件")
                        .fileNumber("CODEX-E2E-RETURN-2440108")
                        .status("APPROVING")
                        .build()));
        when(adminUserService.getUserList(Set.of(113L))).thenReturn(List.of(
                AdminUserDO.builder()
                        .id(113L)
                        .username("aoteman")
                        .nickname("芋道1")
                        .build()));

        assertServiceException(() -> service.getSignaturePage(reqVO), CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);
    }

    @Test
    void getSignaturePage_normalizesFrontendFiltersBeforeMapperQuery() {
        DccElectronicSignaturePageReqVO approvedReqVO = new DccElectronicSignaturePageReqVO();
        approvedReqVO.setPageNo(1);
        approvedReqVO.setPageSize(10);
        approvedReqVO.setSignerUserId(113L);
        approvedReqVO.setTaskActionResult("APPROVED");
        approvedReqVO.setControlledCopyHashStatus("NOT_APPLICABLE");
        approvedReqVO.setEvidenceHashShort("6f2c91ab03d4");
        DccElectronicSignaturePageReqVO rejectedReqVO = new DccElectronicSignaturePageReqVO();
        rejectedReqVO.setPageNo(1);
        rejectedReqVO.setPageSize(10);
        rejectedReqVO.setSignerUserId(114L);
        rejectedReqVO.setTaskActionResult("REJECTED");
        DccElectronicSignaturePageReqVO returnedReqVO = new DccElectronicSignaturePageReqVO();
        returnedReqVO.setPageNo(1);
        returnedReqVO.setPageSize(10);
        returnedReqVO.setSignerUserId(115L);
        returnedReqVO.setTaskActionResult("RETURNED");
        DccElectronicSignaturePageReqVO transferredReqVO = new DccElectronicSignaturePageReqVO();
        transferredReqVO.setPageNo(1);
        transferredReqVO.setPageSize(10);
        transferredReqVO.setSignerUserId(116L);
        transferredReqVO.setTaskActionResult("TRANSFERRED");
        DccElectronicSignaturePageReqVO signAddedReqVO = new DccElectronicSignaturePageReqVO();
        signAddedReqVO.setPageNo(1);
        signAddedReqVO.setPageSize(10);
        signAddedReqVO.setSignerUserId(117L);
        signAddedReqVO.setTaskActionResult("SIGN_ADDED");
        DccElectronicSignaturePageReqVO distributionAckReqVO = new DccElectronicSignaturePageReqVO();
        distributionAckReqVO.setPageNo(1);
        distributionAckReqVO.setPageSize(10);
        distributionAckReqVO.setSignerUserId(118L);
        distributionAckReqVO.setTaskActionResult("DISTRIBUTION_ACK");
        DccElectronicSignaturePageReqVO distributionSignReqVO = new DccElectronicSignaturePageReqVO();
        distributionSignReqVO.setPageNo(1);
        distributionSignReqVO.setPageSize(10);
        distributionSignReqVO.setSignerUserId(119L);
        distributionSignReqVO.setTaskActionResult("DISTRIBUTION_SIGN");
        when(signatureMapper.selectPage(any(DccElectronicSignaturePageReqVO.class)))
                .thenReturn(PageResult.empty());

        service.getSignaturePage(approvedReqVO);
        service.getSignaturePage(rejectedReqVO);
        service.getSignaturePage(returnedReqVO);
        service.getSignaturePage(transferredReqVO);
        service.getSignaturePage(signAddedReqVO);
        service.getSignaturePage(distributionAckReqVO);
        service.getSignaturePage(distributionSignReqVO);

        ArgumentCaptor<DccElectronicSignaturePageReqVO> reqCaptor =
                ArgumentCaptor.forClass(DccElectronicSignaturePageReqVO.class);
        verify(signatureMapper, times(7)).selectPage(reqCaptor.capture());
        assertEquals(113L, reqCaptor.getAllValues().get(0).getSignerUserId());
        assertEquals("APPROVE", reqCaptor.getAllValues().get(0).getPersistentActionType());
        assertEquals("NOT_APPLICABLE", reqCaptor.getAllValues().get(0).getControlledCopyHashStatus());
        assertEquals("6f2c91ab03d4", reqCaptor.getAllValues().get(0).getEvidenceHashShort());
        assertEquals(114L, reqCaptor.getAllValues().get(1).getSignerUserId());
        assertEquals("REJECT", reqCaptor.getAllValues().get(1).getPersistentActionType());
        assertEquals(115L, reqCaptor.getAllValues().get(2).getSignerUserId());
        assertEquals("RETURN", reqCaptor.getAllValues().get(2).getPersistentActionType());
        assertEquals(116L, reqCaptor.getAllValues().get(3).getSignerUserId());
        assertEquals("TRANSFER", reqCaptor.getAllValues().get(3).getPersistentActionType());
        assertEquals(117L, reqCaptor.getAllValues().get(4).getSignerUserId());
        assertEquals("ADD_SIGN", reqCaptor.getAllValues().get(4).getPersistentActionType());
        assertEquals(118L, reqCaptor.getAllValues().get(5).getSignerUserId());
        assertEquals("DISTRIBUTION_ACK", reqCaptor.getAllValues().get(5).getPersistentActionType());
        assertEquals(119L, reqCaptor.getAllValues().get(6).getSignerUserId());
        assertEquals("DISTRIBUTION_SIGN", reqCaptor.getAllValues().get(6).getPersistentActionType());
    }

    @Test
    void getSignaturePage_unsupportedTaskActionResultFailsFastBeforeMapperQuery() {
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setTaskActionResult("CANCELLED");

        assertServiceException(() -> service.getSignaturePage(reqVO), CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING);

        verify(signatureMapper, never()).selectPage(any(DccElectronicSignaturePageReqVO.class));
    }

    @Test
    void getSignaturePage_normalizesPersistentActionsForDisplay() {
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        DccControlledFileSignatureDO returned = DccControlledFileSignatureDO.builder()
                .id(3001L)
                .controlledFileId(903L)
                .revisionId(903L)
                .versionNo("A.1")
                .taskId("task-return")
                .actorId(113L)
                .actionType("RETURN")
                .meaningCode("MATRIX_REVIEW_RETURN")
                .evidenceHash("6f2c91ab03d4aabb")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 10, 10, 0))
                .build();
        DccControlledFileSignatureDO transferred = DccControlledFileSignatureDO.builder()
                .id(3002L)
                .controlledFileId(904L)
                .revisionId(904L)
                .versionNo("A.1")
                .taskId("task-transfer")
                .actorId(113L)
                .actionType("TRANSFER")
                .meaningCode("MATRIX_REVIEW_TRANSFER")
                .evidenceHash("7f2c91ab03d4aabb")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 10, 20, 0))
                .build();
        DccControlledFileSignatureDO signAdded = DccControlledFileSignatureDO.builder()
                .id(3003L)
                .controlledFileId(905L)
                .revisionId(905L)
                .versionNo("A.1")
                .taskId("task-add-sign")
                .actorId(113L)
                .actionType("ADD_SIGN")
                .meaningCode("MATRIX_REVIEW_ADD_SIGN")
                .evidenceHash("8f2c91ab03d4aabb")
                .evidenceStatus("VALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 10, 30, 0))
                .build();
        when(signatureMapper.selectPage(any(DccElectronicSignaturePageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(returned, transferred, signAdded), 3L));
        when(controlledFileMapper.selectBatchIds(Set.of(903L, 904L, 905L))).thenReturn(List.of(
                DccControlledFileDO.builder().id(903L).fileNumber("DCC-RETURN").build(),
                DccControlledFileDO.builder().id(904L).fileNumber("DCC-TRANSFER").build(),
                DccControlledFileDO.builder().id(905L).fileNumber("DCC-ADD-SIGN").build()));
        when(adminUserService.getUserList(Set.of(113L))).thenReturn(List.of(
                AdminUserDO.builder().id(113L).username("aoteman").nickname("芋道1").build()));

        PageResult<DccElectronicSignatureRespVO> result = service.getSignaturePage(reqVO);

        assertEquals(3L, result.getTotal());
        assertEquals("RETURNED", result.getList().get(0).getTaskActionResult());
        assertEquals("TRANSFERRED", result.getList().get(1).getTaskActionResult());
        assertEquals("SIGN_ADDED", result.getList().get(2).getTaskActionResult());
    }

    @Test
    void signatureMapperSelectPage_usesFrontendFilterContractInSqlConditions() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DccControlledFileSignatureDO.class);
        DccControlledFileSignatureMapper mapper = org.mockito.Mockito.mock(DccControlledFileSignatureMapper.class,
                withSettings().defaultAnswer(CALLS_REAL_METHODS));
        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Wrapper<DccControlledFileSignatureDO>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) Wrapper.class);
        doReturn(PageResult.empty()).when(mapper)
                .selectPage(any(PageParam.class), wrapperCaptor.capture());
        DccElectronicSignaturePageReqVO reqVO = new DccElectronicSignaturePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignerUserId(113L);
        reqVO.setPersistentActionType("APPROVE");
        reqVO.setControlledCopyHashStatus("NOT_APPLICABLE");
        reqVO.setEvidenceHashShort("6f2c91ab03d4");

        mapper.selectPage(reqVO);

        Wrapper<DccControlledFileSignatureDO> wrapper = wrapperCaptor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        Map<String, Object> params = ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
        assertTrue(sqlSegment.contains("actor_id"));
        assertTrue(sqlSegment.contains("action_type"));
        assertTrue(sqlSegment.contains("controlled_copy_hash_status"));
        assertTrue(sqlSegment.contains("evidence_hash"));
        assertTrue(sqlSegment.contains("LIKE"));
        assertTrue(params.containsValue(113L));
        assertTrue(params.containsValue("APPROVE"));
        assertTrue(params.containsValue("NOT_APPLICABLE"));
        assertTrue(params.containsValue("6f2c91ab03d4%"));
    }

    @Test
    void verifySignatureEvidence_recomputesHashWithoutMutatingSignature() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file);
        DccControlledFileSignatureDO signature = completeSignature(hmacSha256Hex("secret", canonicalPayload));
        when(signatureMapper.selectById(1001L)).thenReturn(signature);
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        TenantContextHolder.setTenantId(1L);
        DccSignatureVerifyRespVO result;
        try {
            result = service.verifySignatureEvidence(1001L);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(1001L, result.getSignatureId());
        assertEquals(signature.getEvidenceHash(), result.getStoredEvidenceHash());
        assertEquals(signature.getEvidenceHash(), result.getRecomputedEvidenceHash());
        assertEquals(signature.getEvidenceHash().substring(0, 12), result.getEvidenceHashShort());
        assertEquals("VALID", result.getVerificationStatus());
        verify(signatureMapper, never()).updateById(any(DccControlledFileSignatureDO.class));
    }

    @Test
    void getSignatureEvidenceDetail_returnsCanonicalPayloadAndVerificationStatus() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file);
        when(signatureMapper.selectById(1001L)).thenReturn(completeSignature(hmacSha256Hex("secret", canonicalPayload)));
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        TenantContextHolder.setTenantId(1L);
        DccSignatureEvidenceRespVO result;
        try {
            result = service.getSignatureEvidenceDetail(1001L);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(1001L, result.getSignatureId());
        assertEquals(canonicalPayload, result.getCanonicalPayload());
        assertEquals("VALID", result.getVerificationStatus());
        assertEquals(List.of("payloadVersion", "hashAlgorithm", "keyVersion", "tenantId", "controlledFileId",
                "fileNumber", "revisionId", "versionNo", "sourceFileHash", "controlledCopyHashStatus",
                "controlledCopyHash", "processInstanceId", "taskId", "taskActionResult", "meaningCode",
                "signerUserId", "signerUsername", "signerNickname", "signerDeptId", "signerDeptName",
                "signerPostNames", "signerRoleNames", "signaturePurpose", "authorizationBasis",
                "authenticationMethod", "signedAt", "reasonText"),
                result.getCanonicalPayloadFieldOrder());
    }

    @Test
    void getSignatureEvidenceDetail_normalizesAdditionalActionsInCanonicalPayload() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file, 20L, "SIGN_ADDED", "MATRIX_REVIEW_ADD_SIGN");
        DccControlledFileSignatureDO signature = completeSignature("ADD_SIGN",
                hmacSha256Hex("secret", canonicalPayload));
        signature.setMeaningCode("MATRIX_REVIEW_ADD_SIGN");
        when(signatureMapper.selectById(1001L)).thenReturn(signature);
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        TenantContextHolder.setTenantId(1L);
        DccSignatureEvidenceRespVO result;
        try {
            result = service.getSignatureEvidenceDetail(1001L);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(canonicalPayload, result.getCanonicalPayload());
        assertTrue(result.getCanonicalPayload().contains("\"taskActionResult\":\"SIGN_ADDED\""));
        assertEquals("VALID", result.getVerificationStatus());
    }

    @Test
    void verifySignatureEvidence_allowsNullActorDeptSnapshotWhenHashMatches() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file, null);
        DccControlledFileSignatureDO signature = completeSignature(hmacSha256Hex("secret", canonicalPayload));
        signature.setActorDeptIdSnapshot(null);
        signature.setActorDeptNameSnapshot(null);
        when(signatureMapper.selectById(1001L)).thenReturn(signature);
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        TenantContextHolder.setTenantId(1L);
        DccSignatureVerifyRespVO result;
        try {
            result = service.verifySignatureEvidence(1001L);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(signature.getEvidenceHash(), result.getRecomputedEvidenceHash());
        assertEquals("VALID", result.getVerificationStatus());
        verify(signatureMapper, never()).updateById(any(DccControlledFileSignatureDO.class));
    }

    @Test
    void getSignatureEvidenceDetail_keepsCanonicalNullDeptSnapshotWhenHashMatches() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file, null);
        DccControlledFileSignatureDO signature = completeSignature(hmacSha256Hex("secret", canonicalPayload));
        signature.setActorDeptIdSnapshot(null);
        signature.setActorDeptNameSnapshot(null);
        when(signatureMapper.selectById(1001L)).thenReturn(signature);
        when(controlledFileMapper.selectById(900L)).thenReturn(file);

        TenantContextHolder.setTenantId(1L);
        DccSignatureEvidenceRespVO result;
        try {
            result = service.getSignatureEvidenceDetail(1001L);
        } finally {
            TenantContextHolder.clear();
        }

        assertEquals(canonicalPayload, result.getCanonicalPayload());
        assertTrue(result.getCanonicalPayload().contains("\"signerDeptId\":null"));
        assertEquals("VALID", result.getVerificationStatus());
    }

    @Test
    void updateAuthorization_requiresReasonAndReturnsUpdatedAuthorizationRow() {
        when(adminUserService.getUser(101L)).thenReturn(AdminUserDO.builder()
                .id(101L).username("zhangsan").nickname("张三").deptId(20L).build());
        when(deptService.getDeptList(Set.of(20L))).thenReturn(List.of(dept(20L, "质量部")));
        when(authorizationMapper.selectByUserId(101L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .id(1L)
                .userId(101L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("ENABLED")
                .failureCount(0)
                .build());
        when(authorizationAuditMapper.selectList(any())).thenReturn(List.of(DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(101L)
                .operatorId(1L)
                .afterState("ENABLED")
                .reason("完成岗位电子签名授权")
                .operatedAt(LocalDateTime.of(2026, 5, 26, 13, 0, 0))
                .build()));
        when(adminUserService.getUserList(Set.of(1L))).thenReturn(List.of(AdminUserDO.builder()
                .id(1L).nickname("系统管理员").build()));

        DccSignatureAuthorizationRespVO result = service.updateAuthorization(
                101L, Boolean.TRUE, 1L, "完成岗位电子签名授权");

        assertEquals(101L, result.getUserId());
        assertEquals("张三", result.getUserName());
        assertEquals("质量部", result.getDeptName());
        assertEquals("ENABLED", result.getAuthorizationState());
        assertEquals("完成岗位电子签名授权", result.getLatestAuditReason());
        assertEquals(1L, result.getLatestAuditOperatorId());
        assertEquals("系统管理员", result.getLatestAuditOperatorName());
        verify(authorizationService).updateAuthorization(101L, true, 1L, "完成岗位电子签名授权");
    }

    @Test
    void updateAuthorization_blankReasonFailsFast() {
        assertServiceException(() -> service.updateAuthorization(101L, Boolean.TRUE, 1L, " "),
                CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);

        verify(authorizationService, never()).updateAuthorization(any(), any(Boolean.class), any(), any());
    }

    @Test
    void unlockAuthorization_requiresReasonClearsLockAndAudits() {
        DccElectronicSignatureAuthorizationDO locked = DccElectronicSignatureAuthorizationDO.builder()
                .id(1L)
                .userId(101L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .lockedUntil(LocalDateTime.of(2026, 5, 26, 14, 0, 0))
                .failureCount(5)
                .build();
        DccElectronicSignatureAuthorizationDO unlocked = DccElectronicSignatureAuthorizationDO.builder()
                .id(1L)
                .userId(101L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("ENABLED")
                .failureCount(0)
                .build();
        when(adminUserService.getUser(101L)).thenReturn(AdminUserDO.builder()
                .id(101L).username("zhangsan").nickname("张三").build());
        when(authorizationMapper.selectByUserId(101L)).thenReturn(locked, unlocked);
        when(authorizationMapper.updateById(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(1);
        when(authorizationAuditMapper.selectList(any())).thenReturn(List.of(DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(101L)
                .operatorId(1L)
                .afterState("ENABLED")
                .reason("签名人完成身份复核")
                .operatedAt(LocalDateTime.of(2026, 5, 26, 13, 30, 0))
                .build()));

        DccSignatureAuthorizationRespVO result = service.unlockAuthorization(101L, 1L, "签名人完成身份复核");

        assertEquals("ENABLED", result.getAuthorizationState());
        assertFalse(result.getLocked());
        verify(authorizationAuditService).recordAuthorizationChange(any(DccElectronicSignatureAuthorizationAuditDO.class));
    }

    @Test
    void unlockAuthorization_persistFailureFailsFast() {
        when(adminUserService.getUser(101L)).thenReturn(AdminUserDO.builder().id(101L).build());
        when(authorizationMapper.selectByUserId(101L)).thenReturn(DccElectronicSignatureAuthorizationDO.builder()
                .id(1L)
                .userId(101L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .build());
        when(authorizationMapper.updateById(any(DccElectronicSignatureAuthorizationDO.class))).thenReturn(0);

        assertServiceException(() -> service.unlockAuthorization(101L, 1L, "签名人完成身份复核"),
                CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
    }

    @Test
    void getAuthorizationAuditPage_enrichesOperatorName() {
        DccElectronicSignatureAuthorizationAuditPageReqVO reqVO = new DccElectronicSignatureAuthorizationAuditPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        when(authorizationAuditMapper.selectPage(eq(reqVO), any())).thenReturn(new PageResult<>(List.of(
                DccElectronicSignatureAuthorizationAuditDO.builder()
                        .id(61001L)
                        .targetUserId(101L)
                        .operatorId(1L)
                        .beforeState("DISABLED")
                        .afterState("ENABLED")
                        .reason("完成电子签名授权复核")
                        .operatedAt(LocalDateTime.of(2026, 5, 26, 13, 0, 0))
                        .build()), 1L));
        when(adminUserService.getUserList(Set.of(1L))).thenReturn(List.of(AdminUserDO.builder()
                .id(1L).nickname("系统管理员").build()));

        PageResult<DccElectronicSignatureAuthorizationAuditRespVO> result =
                service.getAuthorizationAuditPage(101L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("系统管理员", result.getList().get(0).getOperatorName());
        assertEquals("完成电子签名授权复核", result.getList().get(0).getReason());
    }

    @Test
    void getAuthorizationPage_returnsFailClosedStateAndAuditFields() {
        DccElectronicSignatureAuthorizationPageReqVO reqVO = new DccElectronicSignatureAuthorizationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        AdminUserDO user = AdminUserDO.builder()
                .id(101L).username("zhangsan").nickname("张三").deptId(20L).build();
        when(adminUserService.getUserPage(any())).thenReturn(new PageResult<>(List.of(user), 1L));
        when(authorizationMapper.selectListByUserIds(Set.of(101L))).thenReturn(List.of(DccElectronicSignatureAuthorizationDO.builder()
                .userId(101L)
                .electronicSignatureEnabled(Boolean.FALSE)
                .authorizationState("DISABLED")
                .failureCount(0)
                .build()));
        when(deptService.getDeptList(Set.of(20L))).thenReturn(List.of(dept(20L, "质量部")));
        when(authorizationAuditMapper.selectList(any())).thenReturn(List.of(DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(101L)
                .operatorId(1L)
                .reason("岗位调整")
                .operatedAt(LocalDateTime.of(2026, 5, 26, 12, 0, 0))
                .build()));

        PageResult<DccElectronicSignatureAuthorizationRespVO> result = service.getAuthorizationPage(reqVO);

        assertEquals(Boolean.FALSE, result.getList().get(0).getElectronicSignatureEnabled());
        assertEquals("DISABLED", result.getList().get(0).getAuthorizationState());
        assertEquals("质量部", result.getList().get(0).getDeptName());
        assertEquals("岗位调整", result.getList().get(0).getLatestAuditReason());
    }

    @Test
    void getAuthorizationPage_preservesLatestAuditWhenOperatorIdIsNull() {
        DccElectronicSignatureAuthorizationPageReqVO reqVO = new DccElectronicSignatureAuthorizationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setUsername("aoteman");
        AdminUserDO user = AdminUserDO.builder()
                .id(101L).username("aoteman").nickname("奥特曼").build();
        LocalDateTime auditAt = LocalDateTime.of(2026, 5, 26, 16, 40, 0);
        when(adminUserService.getUserPage(any())).thenReturn(new PageResult<>(List.of(user), 1L));
        when(authorizationMapper.selectListByUserIds(Set.of(101L))).thenReturn(List.of(DccElectronicSignatureAuthorizationDO.builder()
                .userId(101L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("ENABLED")
                .failureCount(0)
                .build()));
        when(authorizationAuditMapper.selectList(any())).thenReturn(List.of(DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(101L)
                .operatorId(null)
                .reason("历史授权审计导入")
                .operatedAt(auditAt)
                .build()));

        PageResult<DccElectronicSignatureAuthorizationRespVO> result = service.getAuthorizationPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("ENABLED", result.getList().get(0).getAuthorizationState());
        assertEquals("历史授权审计导入", result.getList().get(0).getLatestAuditReason());
        assertEquals(auditAt, result.getList().get(0).getLatestAuditAt());
        assertNull(result.getList().get(0).getLatestAuditOperatorId());
        assertNull(result.getList().get(0).getLatestAuditOperatorName());
        verify(adminUserService, never()).getUserList(any());
    }

    @Test
    void getAuthorizationPage_usesUsernameWhenNicknameIsUnreadableMojibake() {
        DccElectronicSignatureAuthorizationPageReqVO reqVO = new DccElectronicSignatureAuthorizationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        AdminUserDO user = AdminUserDO.builder()
                .id(102L).username("aoteman").nickname("??1").build();
        when(adminUserService.getUserPage(any())).thenReturn(new PageResult<>(List.of(user), 1L));
        when(authorizationMapper.selectListByUserIds(Set.of(102L))).thenReturn(List.of());
        when(authorizationAuditMapper.selectList(any())).thenReturn(List.of());

        PageResult<DccElectronicSignatureAuthorizationRespVO> result = service.getAuthorizationPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("??1", result.getList().get(0).getNickname());
        assertEquals("aoteman", result.getList().get(0).getUserName());
        assertEquals("aoteman", result.getList().get(0).getUsername());
    }

    @Test
    void getAuthorizationPage_treatsExpiredLockAsEnabledAndNotLocked() {
        DccElectronicSignatureAuthorizationPageReqVO reqVO = new DccElectronicSignatureAuthorizationPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        AdminUserDO user = AdminUserDO.builder()
                .id(101L).username("zhangsan").nickname("张三").deptId(20L).build();
        when(adminUserService.getUserPage(any())).thenReturn(new PageResult<>(List.of(user), 1L));
        when(authorizationMapper.selectListByUserIds(Set.of(101L))).thenReturn(List.of(DccElectronicSignatureAuthorizationDO.builder()
                .userId(101L)
                .electronicSignatureEnabled(Boolean.TRUE)
                .authorizationState("LOCKED")
                .lockedUntil(LocalDateTime.now().minusMinutes(1))
                .failureCount(5)
                .build()));
        when(deptService.getDeptList(Set.of(20L))).thenReturn(List.of(dept(20L, "质量部")));
        when(authorizationAuditMapper.selectList(any())).thenReturn(List.of());

        PageResult<DccElectronicSignatureAuthorizationRespVO> result = service.getAuthorizationPage(reqVO);

        assertTrue(result.getList().get(0).getElectronicSignatureEnabled());
        assertEquals("ENABLED", result.getList().get(0).getAuthorizationState());
        assertFalse(result.getList().get(0).getLocked());
        assertNull(result.getList().get(0).getLockedUntil());
    }

    @Test
    void getSignatureExportSummaryBlocksWhenEvidenceInvalid() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .versionNo("A.1")
                .build());
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of(DccControlledFileSignatureDO.builder()
                .id(1001L)
                .controlledFileId(900L)
                .revisionId(900L)
                .versionNo("A.1")
                .actionType("APPROVE")
                .meaningCode("REVIEW_APPROVE")
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .evidenceHash("badc91ab03d4")
                .evidenceStatus("INVALID")
                .signedAt(LocalDateTime.of(2026, 5, 26, 14, 32, 18))
                .build()));

        DccControlledFileSignatureExportSummaryRespVO result = service.getSignatureExportSummary(900L);

        assertFalse(result.getAllRequiredEvidenceValid());
        assertEquals("SIGNATURE_EVIDENCE_INVALID", result.getBlockedReason());
        assertEquals("INVALID", result.getSignatures().get(0).getEvidenceStatus());
    }

    @Test
    void exportSignatureEvidenceReturnsSystemVerifiedPdfArtifactWithReadableEvidence() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file);
        DccControlledFileSignatureDO signature = completeSignature(hmacSha256Hex("secret", canonicalPayload));
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of(signature));

        TenantContextHolder.setTenantId(1L);
        DccSignatureEvidenceExportArtifact artifact;
        try {
            artifact = service.exportSignatureEvidence(900L);
        } finally {
            TenantContextHolder.clear();
        }

        assertTrue(artifact.fileName().startsWith("dcc-signature-evidence-DCC-SOP-001-A.1-"));
        assertTrue(artifact.fileName().endsWith(".pdf"));
        assertEquals("application/pdf", artifact.contentType());
        assertTrue(new String(artifact.bytes(), 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));

        String pdfText;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(artifact.bytes()))) {
            pdfText = new PDFTextStripper().getText(document);
        }
        assertTrue(pdfText.contains("本系统认证并可校验"));
        assertTrue(pdfText.contains("签名 ID: 1001"));
        assertTrue(pdfText.contains("文件编号: DCC-SOP-001"));
        assertTrue(pdfText.contains("版本: A.1"));
        assertTrue(pdfText.contains("证据哈希: " + signature.getEvidenceHash()));
        assertTrue(pdfText.contains("校验状态: VALID"));
        assertFalse(pdfText.contains("DocuSign"));
        assertFalse(pdfText.contains("外部 CA"));
    }

    @Test
    void exportSignatureEvidenceUsesAccountNameWhenNicknameSnapshotIsUnreadable() throws Exception {
        when(signatureEvidenceProperties.getHmacSecret()).thenReturn("secret");
        DccControlledFileDO file = signedFile();
        String canonicalPayload = canonicalPayload(file, 20L, "APPROVED", "REVIEW_APPROVE", "??1");
        DccControlledFileSignatureDO signature = completeSignature(hmacSha256Hex("secret", canonicalPayload));
        signature.setActorNicknameSnapshot("??1");
        when(controlledFileMapper.selectById(900L)).thenReturn(file);
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of(signature));

        TenantContextHolder.setTenantId(1L);
        DccSignatureEvidenceExportArtifact artifact;
        try {
            artifact = service.exportSignatureEvidence(900L);
        } finally {
            TenantContextHolder.clear();
        }

        String pdfText;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(artifact.bytes()))) {
            pdfText = new PDFTextStripper().getText(document);
        }
        assertTrue(pdfText.contains("签名人: auditor"));
        assertFalse(pdfText.contains("签名人: ??1"));
        assertTrue(pdfText.contains("Record Tracking / 记录追踪"));
        assertTrue(pdfText.contains("System Verification / 系统校验"));
        assertTrue(pdfText.contains("Signer Events / 签名事件 1"));
    }

    @Test
    void exportSignatureEvidenceFailsFastWhenEvidenceMissingOrInvalid() {
        when(controlledFileMapper.selectById(900L)).thenReturn(signedFile());
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of());

        assertServiceException(() -> service.exportSignatureEvidence(900L),
                CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED);

        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of(DccControlledFileSignatureDO.builder()
                .id(1001L)
                .controlledFileId(900L)
                .revisionId(900L)
                .versionNo("A.1")
                .evidenceStatus("INVALID")
                .build()));

        assertServiceException(() -> service.exportSignatureEvidence(900L),
                CONTROLLED_FILE_SIGNATURE_EXPORT_BLOCKED);
    }

    private DccControlledFileDO signedFile() {
        return DccControlledFileDO.builder()
                .id(900L)
                .fileNumber("DCC-SOP-001")
                .fileName("成品检验规程")
                .versionNo("A.1")
                .processInstanceId("bpm-pi-8001")
                .build();
    }

    private DeptDO dept(Long id, String name) {
        DeptDO dept = new DeptDO();
        dept.setId(id);
        dept.setName(name);
        return dept;
    }

    private DccControlledFileSignatureDO completeSignature(String evidenceHash) {
        return completeSignature("APPROVE", evidenceHash);
    }

    private DccControlledFileSignatureDO completeSignature(String actionType, String evidenceHash) {
        return DccControlledFileSignatureDO.builder()
                .id(1001L)
                .controlledFileId(900L)
                .revisionId(900L)
                .versionNo("A.1")
                .taskId("bpm-task-9001")
                .actorId(101L)
                .actorUsernameSnapshot("auditor")
                .actorNicknameSnapshot("审核员")
                .actorDeptIdSnapshot(20L)
                .actorDeptNameSnapshot("质量部")
                .actorPostNamesSnapshot("QA岗位")
                .actorRoleNamesSnapshot("质量审核员")
                .signaturePurpose("REVIEW_APPROVE")
                .authorizationBasis("DCC电子签名授权启用；系统角色/岗位快照已记录")
                .authenticationMethod("PASSWORD")
                .recordVersionSnapshot("A.1")
                .recordHashSnapshot("0e7b12ca44fe")
                .snapshotStatus("CAPTURED")
                .actionType(actionType)
                .meaningCode("REVIEW_APPROVE")
                .sourceFileHash("0e7b12ca44fe")
                .sourceFileHashStatus("BOUND")
                .controlledCopyHashStatus("NOT_APPLICABLE")
                .evidencePayloadVersion("v2")
                .evidenceKeyVersion("kv1")
                .evidenceHash(evidenceHash)
                .evidenceHashAlgorithm("HMAC_SHA256")
                .evidenceStatus("VALID")
                .comment("")
                .signedAt(LocalDateTime.of(2026, 5, 26, 14, 32, 18))
                .build();
    }

    private String canonicalPayload(DccControlledFileDO file) {
        return canonicalPayload(file, 20L);
    }

    private String canonicalPayload(DccControlledFileDO file, Long signerDeptId) {
        return canonicalPayload(file, signerDeptId, "APPROVED", "REVIEW_APPROVE");
    }

    private String canonicalPayload(DccControlledFileDO file, Long signerDeptId,
                                    String taskActionResult, String meaningCode) {
        return canonicalPayload(file, signerDeptId, taskActionResult, meaningCode, "审核员");
    }

    private String canonicalPayload(DccControlledFileDO file, Long signerDeptId,
                                    String taskActionResult, String meaningCode, String signerNickname) {
        return "{\"payloadVersion\":\"v2\",\"hashAlgorithm\":\"HMAC_SHA256\",\"keyVersion\":\"kv1\","
                + "\"tenantId\":1,\"controlledFileId\":900,\"fileNumber\":\"" + file.getFileNumber() + "\","
                + "\"revisionId\":900,\"versionNo\":\"A.1\",\"sourceFileHash\":\"0e7b12ca44fe\","
                + "\"controlledCopyHashStatus\":\"NOT_APPLICABLE\",\"controlledCopyHash\":\"\","
                + "\"processInstanceId\":\"bpm-pi-8001\",\"taskId\":\"bpm-task-9001\","
                + "\"taskActionResult\":\"" + taskActionResult + "\",\"meaningCode\":\"" + meaningCode + "\","
                + "\"signerUserId\":101,\"signerUsername\":\"auditor\","
                + "\"signerNickname\":\"" + signerNickname + "\",\"signerDeptId\":" + signerDeptId
                + ",\"signerDeptName\":\"" + (signerDeptId == null ? "" : "质量部") + "\","
                + "\"signerPostNames\":\"QA岗位\",\"signerRoleNames\":\"质量审核员\","
                + "\"signaturePurpose\":\"REVIEW_APPROVE\","
                + "\"authorizationBasis\":\"DCC电子签名授权启用；系统角色/岗位快照已记录\","
                + "\"authenticationMethod\":\"PASSWORD\""
                + ",\"signedAt\":\"2026-05-26T14:32:18+08:00\","
                + "\"reasonText\":\"\"}";
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
