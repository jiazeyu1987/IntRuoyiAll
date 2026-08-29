package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandMutex;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftData;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadSubmitResult;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateUploadServiceTest {

    private static final Long TENANT_ID = 11L;
    private static final Long ACTOR_ID = 22L;

    @Mock
    private DccRegistrationCertificateCommandMutex commandMutex;
    @Mock
    private DccRegistrationCertificateCommandService commandService;
    @Mock
    private DccRegistrationCertificateAccessRequestMapper requestMapper;
    @Mock
    private DccRegistrationCertificateAccessRequestFileMapper requestFileMapper;
    @Mock
    private DccRegistrationCertificateFileMapper fileMapper;
    @Mock
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccProjectCodeService projectCodeService;
    @Mock
    private MdmCompanyScopeApi companyScopeApi;
    @Mock
    private MdmEnterpriseApi enterpriseApi;
    @Mock
    private MdmProductApi productApi;
    @Mock
    private DccRegistrationCertificateBusinessClock businessClock;
    @Mock
    private DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;

    private DccRegistrationCertificateUploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new DccRegistrationCertificateUploadService(
                commandMutex, commandService, requestMapper, requestFileMapper, fileMapper, versionMapper,
                fileService, projectCodeService, companyScopeApi, enterpriseApi, productApi, businessClock,
                businessEventNotifier);
        lenient().doAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        }).when(commandMutex).execute(anyString(), any());
    }

    @Test
    void submitUploadForApprovalCreatesDraftWithProductionRelation() {
        mockOwnedCompany();
        mockProjectCode();
        mockProduct("一次性使用无菌导管", 2001L);
        when(requestMapper.selectByTenantAndRequestKey(TENANT_ID, "UPLOAD-KEY-1")).thenReturn(null);
        ArgumentCaptor<DccRegistrationCertificateDraftData> draft =
                ArgumentCaptor.forClass(DccRegistrationCertificateDraftData.class);
        when(commandService.createDraft(eq(TENANT_ID), eq(ACTOR_ID), eq("UPLOAD-KEY-1"), eq("TRACE-1"),
                draft.capture())).thenReturn(9001L);
        DccRegistrationCertificateVersionDO draftVersion = DccRegistrationCertificateVersionDO.builder()
                .id(9101L)
                .certificateId(9001L)
                .status("DRAFT")
                .build();
        draftVersion.setTenantId(TENANT_ID);
        when(versionMapper.selectList(any())).thenReturn(List.of(draftVersion));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("registration.pdf"),
                eq("dcc/registration-certificate/upload/9001"), eq("application/pdf"))).thenReturn(9201L);
        doAnswer(invocation -> {
            DccRegistrationCertificateFileDO file = invocation.getArgument(0);
            file.setId(9301L);
            return 1;
        }).when(fileMapper).insert(any(DccRegistrationCertificateFileDO.class));
        doAnswer(invocation -> {
            DccRegistrationCertificateAccessRequestDO request = invocation.getArgument(0);
            request.setId(9401L);
            return 1;
        }).when(requestMapper).insert(any(DccRegistrationCertificateAccessRequestDO.class));
        when(requestFileMapper.insert(any(DccRegistrationCertificateAccessRequestFileDO.class))).thenReturn(1);
        when(businessClock.now()).thenReturn(LocalDateTime.of(2026, 8, 29, 10, 0));

        DccRegistrationCertificateUploadSubmitResult result = uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-1", "TRACE-1",
                uploadCommand(true, false, List.of(301L, 302L)));

        assertEquals(9401L, result.requestId());
        assertEquals(9001L, result.certificateId());
        assertEquals(9301L, result.businessFileId());
        assertEquals(Boolean.TRUE, draft.getValue().entrustedProduction());
        assertEquals(Boolean.FALSE, draft.getValue().selfProduction());
        assertEquals(List.of(301L, 302L), draft.getValue().entrustedEnterpriseIds());
    }

    @Test
    void submitUploadForApprovalCreatesDraftWithoutProjectCodeWhenProductNameIsUnique() {
        mockOwnedCompany();
        mockProduct("一次性使用无菌导管", 2001L);
        when(requestMapper.selectByTenantAndRequestKey(TENANT_ID, "UPLOAD-KEY-NO-PROJECT")).thenReturn(null);
        ArgumentCaptor<DccRegistrationCertificateDraftData> draft =
                ArgumentCaptor.forClass(DccRegistrationCertificateDraftData.class);
        when(commandService.createDraft(eq(TENANT_ID), eq(ACTOR_ID), eq("UPLOAD-KEY-NO-PROJECT"),
                eq("TRACE-NO-PROJECT"), draft.capture())).thenReturn(9002L);
        DccRegistrationCertificateVersionDO draftVersion = DccRegistrationCertificateVersionDO.builder()
                .id(9102L)
                .certificateId(9002L)
                .status("DRAFT")
                .build();
        draftVersion.setTenantId(TENANT_ID);
        when(versionMapper.selectList(any())).thenReturn(List.of(draftVersion));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("registration.pdf"),
                eq("dcc/registration-certificate/upload/9002"), eq("application/pdf"))).thenReturn(9202L);
        doAnswer(invocation -> {
            DccRegistrationCertificateFileDO file = invocation.getArgument(0);
            file.setId(9302L);
            return 1;
        }).when(fileMapper).insert(any(DccRegistrationCertificateFileDO.class));
        ArgumentCaptor<DccRegistrationCertificateAccessRequestDO> request =
                ArgumentCaptor.forClass(DccRegistrationCertificateAccessRequestDO.class);
        doAnswer(invocation -> {
            DccRegistrationCertificateAccessRequestDO inserted = invocation.getArgument(0);
            inserted.setId(9402L);
            return 1;
        }).when(requestMapper).insert(request.capture());
        when(requestFileMapper.insert(any(DccRegistrationCertificateAccessRequestFileDO.class))).thenReturn(1);
        when(businessClock.now()).thenReturn(LocalDateTime.of(2026, 8, 29, 10, 0));

        DccRegistrationCertificateUploadSubmitResult result = uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-NO-PROJECT", "TRACE-NO-PROJECT",
                uploadCommand(null, "一次性使用无菌导管", false, true, List.of()));

        assertEquals(9402L, result.requestId());
        assertEquals(9002L, result.certificateId());
        assertEquals(9302L, result.businessFileId());
        assertEquals(2001L, draft.getValue().productMasterId());
        assertNull(draft.getValue().projectCodeId());
        assertNull(request.getValue().getProjectCodeId());
        verify(projectCodeService, never()).getProjectCode(any(), any());
    }

    @Test
    void submitUploadForApprovalAllowsSelectedProjectCodeWithoutProductBinding() {
        mockOwnedCompany();
        mockProjectCodeWithoutProductBinding();
        mockProduct("一次性使用无菌导管", 2001L);
        when(requestMapper.selectByTenantAndRequestKey(TENANT_ID, "UPLOAD-KEY-UNBOUND-PROJECT")).thenReturn(null);
        ArgumentCaptor<DccRegistrationCertificateDraftData> draft =
                ArgumentCaptor.forClass(DccRegistrationCertificateDraftData.class);
        when(commandService.createDraft(eq(TENANT_ID), eq(ACTOR_ID), eq("UPLOAD-KEY-UNBOUND-PROJECT"),
                eq("TRACE-UNBOUND-PROJECT"), draft.capture())).thenReturn(9003L);
        DccRegistrationCertificateVersionDO draftVersion = DccRegistrationCertificateVersionDO.builder()
                .id(9103L)
                .certificateId(9003L)
                .status("DRAFT")
                .build();
        draftVersion.setTenantId(TENANT_ID);
        when(versionMapper.selectList(any())).thenReturn(List.of(draftVersion));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("registration.pdf"),
                eq("dcc/registration-certificate/upload/9003"), eq("application/pdf"))).thenReturn(9203L);
        doAnswer(invocation -> {
            DccRegistrationCertificateFileDO file = invocation.getArgument(0);
            file.setId(9303L);
            return 1;
        }).when(fileMapper).insert(any(DccRegistrationCertificateFileDO.class));
        doAnswer(invocation -> {
            DccRegistrationCertificateAccessRequestDO request = invocation.getArgument(0);
            request.setId(9403L);
            return 1;
        }).when(requestMapper).insert(any(DccRegistrationCertificateAccessRequestDO.class));
        when(requestFileMapper.insert(any(DccRegistrationCertificateAccessRequestFileDO.class))).thenReturn(1);
        when(businessClock.now()).thenReturn(LocalDateTime.of(2026, 8, 29, 10, 0));

        uploadService.submitUploadForApproval(TENANT_ID, ACTOR_ID, "UPLOAD-KEY-UNBOUND-PROJECT",
                "TRACE-UNBOUND-PROJECT", uploadCommand(1001L, "一次性使用无菌导管", false, true, List.of()));

        assertEquals(2001L, draft.getValue().productMasterId());
        assertEquals(1001L, draft.getValue().projectCodeId());
    }

    @Test
    void submitUploadForApprovalRejectsSelectedProjectCodeBoundToDifferentProductName() {
        mockOwnedCompany();
        mockProjectCode();
        mockProduct("另一个产品", 2002L);

        ServiceException exception = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-MISMATCH", "TRACE-MISMATCH",
                uploadCommand(1001L, "另一个产品", false, true, List.of())));

        assertEquals(REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH.getCode(), exception.getCode());
        verify(commandService, never()).createDraft(any(), any(), any(), any(), any());
    }

    @Test
    void listEntrustedEnterprisesReturnsTenantScopedCandidates() {
        MdmEnterpriseRespDTO candidate = MdmEnterpriseRespDTO.builder()
                .id(301L)
                .tenantId(TENANT_ID)
                .enterpriseCode("TRUST-301")
                .name("受托企业：上海受托制造有限公司")
                .type(MdmEnterpriseTypeEnum.ENTRUSTED_PARTY.getType())
                .status("ENABLE")
                .revision(1)
                .build();
        when(enterpriseApi.listEnabledEnterprises(Set.of(MdmEnterpriseTypeEnum.ENTRUSTED_PARTY.getType()),
                "上海受托", 20)).thenReturn(List.of(candidate));

        List<MdmEnterpriseRespDTO> result = uploadService.listEntrustedEnterprises(TENANT_ID, " 上海受托 ");

        assertEquals(1, result.size());
        assertEquals(301L, result.get(0).getId());
        assertEquals("受托企业：上海受托制造有限公司", result.get(0).getName());
    }

    @Test
    void listOwnerCompaniesReturnsCurrentUserTenantScopedCandidates() {
        MdmEnterpriseRespDTO candidate = MdmEnterpriseRespDTO.builder()
                .id(501L)
                .tenantId(TENANT_ID)
                .enterpriseCode("COMP-501")
                .name("上海七木医疗器械有限公司")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status("ENABLE")
                .revision(1)
                .build();
        MdmEnterpriseRespDTO otherCandidate = MdmEnterpriseRespDTO.builder()
                .id(502L)
                .tenantId(TENANT_ID)
                .enterpriseCode("COMP-502")
                .name("苏州七木医疗器械有限公司")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status("ENABLE")
                .revision(1)
                .build();
        when(companyScopeApi.getEnabledCompanyIdsForUser(ACTOR_ID)).thenReturn(Set.of(501L, 502L));
        when(enterpriseApi.getEnabledEnterprises(Set.of(501L, 502L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())))
                .thenReturn(List.of(candidate, otherCandidate));

        List<MdmEnterpriseRespDTO> result = uploadService.listOwnerCompanies(TENANT_ID, ACTOR_ID, "上海");

        assertEquals(1, result.size());
        assertEquals(501L, result.get(0).getId());
        assertEquals("上海七木医疗器械有限公司", result.get(0).getName());
    }

    @Test
    void submitUploadForApprovalRejectsUnknownOwnerCompanyWithChineseMessage() {
        mockOwnedCompany();

        ServiceException exception = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-UNKNOWN-COMPANY", "TRACE-UNKNOWN-COMPANY",
                uploadCommand(1001L, "不存在的公司", "一次性使用无菌导管", false, true, List.of())));

        assertEquals("当前账号无该公司注册证上传权限，请选择已授权公司", exception.getMessage());
        verify(commandService, never()).createDraft(any(), any(), any(), any(), any());
    }

    @Test
    void submitUploadForApprovalRejectsBothProductionModesFalseBeforeDraftCreation() {
        ServiceException exception = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-2", "TRACE-2",
                uploadCommand(false, false, List.of())));

        assertEquals(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID.getCode(), exception.getCode());
        verify(commandService, never()).createDraft(any(), any(), any(), any(), any());
    }

    @Test
    void approveUploadRequestFormalizesAsApproverWithRequesterCompanyScope() {
        DccRegistrationCertificateAccessRequestDO request = DccRegistrationCertificateAccessRequestDO.builder()
                .id(9401L)
                .ownerCompanyId(501L)
                .certificateId(9001L)
                .requesterUserId(ACTOR_ID)
                .requestType("UPLOAD_CERTIFICATE")
                .status("APPROVED")
                .detailJson("""
                        {
                          "operation": "UPLOAD_CERTIFICATE",
                          "payloadHash": "payload-1",
                          "draftRowVersion": 1,
                          "draftSnapshotRevision": 1
                        }
                        """)
                .build();
        request.setTenantId(TENANT_ID);
        when(requestMapper.selectById(9401L)).thenReturn(request);
        DccRegistrationCertificateAccessRequestFileDO requestFile =
                DccRegistrationCertificateAccessRequestFileDO.builder()
                        .id(9501L)
                        .requestId(9401L)
                        .businessFileId(9301L)
                        .status("REQUESTED")
                        .build();
        requestFile.setTenantId(TENANT_ID);
        when(requestFileMapper.selectByRequestId(TENANT_ID, 9401L)).thenReturn(List.of(requestFile));
        when(commandService.formalizeApprovedUpload(
                TENANT_ID, 88L, ACTOR_ID, "APPROVAL-KEY-1", "APPROVAL-KEY-1",
                9001L, 1, 1, 9301L)).thenReturn(9001L);
        when(requestFileMapper.updateById(requestFile)).thenReturn(1);
        DccRegistrationCertificateFileDO businessFile = DccRegistrationCertificateFileDO.builder()
                .id(9301L)
                .ownerType("VERSION")
                .ownerId(9101L)
                .fileKind("REGISTRATION_CERTIFICATE")
                .status("BOUND")
                .build();
        businessFile.setTenantId(TENANT_ID);
        when(fileMapper.selectById(9301L)).thenReturn(businessFile);
        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .id(9101L)
                .certificateId(9001L)
                .certificateNo("REG-CERT-UPLOAD-1")
                .build();
        version.setTenantId(TENANT_ID);
        when(versionMapper.selectById(9101L)).thenReturn(version);

        uploadService.approveUploadRequest(TENANT_ID, 88L, 9401L, "APPROVAL-KEY-1");

        verify(commandService).formalizeApprovedUpload(
                TENANT_ID, 88L, ACTOR_ID, "APPROVAL-KEY-1", "APPROVAL-KEY-1",
                9001L, 1, 1, 9301L);
        assertEquals("APPROVED", requestFile.getStatus());
        verify(businessEventNotifier).notifyNewCertificateFormalized(
                TENANT_ID, 501L, 9001L, 9101L, 88L, "APPROVAL-KEY-1", "REG-CERT-UPLOAD-1");
    }

    private void mockOwnedCompany() {
        when(companyScopeApi.getEnabledCompanyIdsForUser(ACTOR_ID)).thenReturn(Set.of(501L));
        MdmEnterpriseRespDTO owner = MdmEnterpriseRespDTO.builder()
                .id(501L)
                .tenantId(TENANT_ID)
                .enterpriseCode("COMP-501")
                .name("上海七木医疗器械有限公司")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status("ENABLE")
                .revision(1)
                .build();
        when(enterpriseApi.getEnabledEnterprises(Set.of(501L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()))).thenReturn(List.of(owner));
    }

    private void mockProjectCode() {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(1001L)
                .productMasterId(2001L)
                .projectCode("DCC-PROJ-001")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCode.setTenantId(TENANT_ID);
        when(projectCodeService.getProjectCode(ACTOR_ID, 1001L)).thenReturn(projectCode);
    }

    private void mockProjectCodeWithoutProductBinding() {
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(1001L)
                .projectCode("DCC-PROJ-001")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCode.setTenantId(TENANT_ID);
        when(projectCodeService.getProjectCode(ACTOR_ID, 1001L)).thenReturn(projectCode);
    }

    private void mockProduct(String productName, Long productMasterId) {
        when(productApi.listSimpleProducts(MdmProductStatusConstants.ENABLE, true, productName))
                .thenReturn(List.of(MdmProductRespDTO.builder()
                        .id(productMasterId)
                        .nameCn(productName)
                        .status(MdmProductStatusConstants.ENABLE)
                        .build()));
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds) {
        return uploadCommand(1001L, "一次性使用无菌导管",
                entrustedProduction, selfProduction, entrustedEnterpriseIds);
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Long projectCodeId, String productName,
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds) {
        return uploadCommand(projectCodeId, "上海七木医疗器械有限公司", productName,
                entrustedProduction, selfProduction, entrustedEnterpriseIds);
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Long projectCodeId, String companyName, String productName,
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds) {
        return new DccRegistrationCertificateUploadCommand(
                projectCodeId,
                companyName,
                productName,
                "REG-CERT-UPLOAD-1",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2026, 1, 1),
                "A类",
                entrustedProduction,
                selfProduction,
                entrustedEnterpriseIds,
                "上传注册证审批",
                new MockMultipartFile("file", "registration.pdf", "application/pdf",
                        "%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
    }
}
