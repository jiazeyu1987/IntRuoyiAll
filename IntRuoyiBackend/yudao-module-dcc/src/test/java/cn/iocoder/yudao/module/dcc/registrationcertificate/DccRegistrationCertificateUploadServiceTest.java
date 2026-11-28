package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProjectProductService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAccessRequestMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.association.DccRegistrationCertificateProjectCodeFileAssociationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandMutex;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftData;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificatePrerequisiteValidator;
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
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Mock
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Mock
    private DccRegistrationCertificateProjectCodeFileAssociationService projectCodeFileAssociationService;
    @Mock
    private FileService fileService;
    @Mock
    private DccProjectCodeService projectCodeService;
    @Mock
    private MdmCompanyScopeApi companyScopeApi;
    @Mock
    private MdmEnterpriseApi enterpriseApi;
    @Mock
    private DccRegistrationCertificateBusinessClock businessClock;
    @Mock
    private DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;

    private DccRegistrationCertificateUploadService uploadService;

    @Mock private cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateProjectProductMapper projectProductMapper;
    @Mock private cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProductBindingService productBindings;

    @Mock
    private DccRegistrationCertificateProjectProductService projectProductService;
    @Mock
    private MdmProductApi productApi;

    @org.mockito.Mock
    private cn.iocoder.yudao.module.dcc.registrationcertificate.service.handling.DccRegistrationHandlingTaskService registrationHandlingTasks;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        uploadService = new DccRegistrationCertificateUploadService(
                commandMutex, commandService, requestMapper, requestFileMapper, fileMapper, snapshotMapper, versionMapper,
                projectCodeFileAssociationService, fileService, projectCodeService, enterpriseApi, businessClock,
                businessEventNotifier);
        org.springframework.test.util.ReflectionTestUtils.setField(uploadService, "registrationHandlingTasks", registrationHandlingTasks);
        org.springframework.test.util.ReflectionTestUtils.setField(uploadService, "projectProductMapper", projectProductMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(uploadService, "productBindings", productBindings);
        var validator = new DccRegistrationCertificatePrerequisiteValidator(
                companyScopeApi, enterpriseApi, productApi, projectCodeService, businessClock);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "projectProductService", projectProductService);
        org.springframework.test.util.ReflectionTestUtils.setField(uploadService, "prerequisiteValidator", validator);
        lenient().doAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        }).when(commandMutex).execute(anyString(), any());
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test void selectedProjectCandidatesReturnsExactLargeIdAndDeduplicates() {
        long id = 2121453181393523290L;
        var project = DccProjectCodeDO.builder().id(id).projectCode("REAL-733")
                .projectName("Exact selected project").status(DccProjectCodeStatusConstants.ENABLE).build();
        project.setTenantId(TENANT_ID);
        when(projectCodeService.getProjectCode(ACTOR_ID, id)).thenReturn(project);
        when(projectProductMapper.selectBindings(TENANT_ID, id)).thenReturn(List.of(
                new cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateProjectProductMapper.Row(
                        TENANT_ID,id,10L,"P10","Product ten","CONFIRMED",1L)));
        var result = uploadService.selectedProjectCandidates(TENANT_ID, ACTOR_ID, List.of(id,id), null);
        assertEquals(1, result.size());
        assertEquals(id, result.get(0).get("id"));
        assertEquals("REAL-733", result.get(0).get("projectCode"));
        assertEquals("Exact selected project", result.get(0).get("projectName"));
        assertEquals(false, result.get(0).containsKey("disabled"));
        verify(projectCodeService).getProjectCode(ACTOR_ID,id);
        org.mockito.Mockito.verifyNoInteractions(productBindings);
    }

    @Test void selectedProjectCandidatesPreservesMissingFormalProductDisabledState() {
        mockProjectCodeWithoutProductBinding();
        when(projectProductMapper.selectBindings(TENANT_ID,1001L)).thenReturn(List.of());
        var row=uploadService.selectedProjectCandidates(TENANT_ID,ACTOR_ID,List.of(1001L),null).get(0);
        assertEquals(true,row.get("disabled"));
        assertEquals("尚未绑定有效DCC产品",row.get("bindingMessage"));
    }

    @Test void selectedProjectCandidatesPropagatesRevokedPermission() {
        var denied=new IllegalStateException("project access denied");
        when(projectCodeService.getProjectCode(ACTOR_ID,1001L)).thenThrow(denied);
        assertEquals(denied,assertThrows(IllegalStateException.class,()->
                uploadService.selectedProjectCandidates(TENANT_ID,ACTOR_ID,List.of(1001L),null)));
        org.mockito.Mockito.verifyNoInteractions(projectProductMapper);
    }

    @ParameterizedTest
    @ValueSource(strings={"FOREIGN_TENANT","DISABLED"})
    void selectedProjectCandidatesRejectsForeignOrDisabledProject(String scenario) {
        var project=DccProjectCodeDO.builder().id(1001L).projectCode("P-1")
                .status("DISABLED".equals(scenario)?"DISABLE":DccProjectCodeStatusConstants.ENABLE).build();
        project.setTenantId("FOREIGN_TENANT".equals(scenario)?TENANT_ID+1:TENANT_ID);
        when(projectCodeService.getProjectCode(ACTOR_ID,1001L)).thenReturn(project);
        assertThrows(ServiceException.class,()->uploadService.selectedProjectCandidates(TENANT_ID,ACTOR_ID,List.of(1001L),null));
        org.mockito.Mockito.verifyNoInteractions(projectProductMapper);
    }
    @Test void validProductCandidateIsAvailableWithoutCheckingOtherCertificates() {
        when(projectProductMapper.selectBindings(TENANT_ID,100L)).thenReturn(List.of(
            new cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateProjectProductMapper.Row(
                TENANT_ID,100L,10L,"P10","产品十","CONFIRMED",1L)));
        assertEquals(Map.of(),uploadService.candidateBinding(TENANT_ID,100L,null));
        org.mockito.Mockito.verifyNoInteractions(productBindings);
    }

    @Test void projectWithoutProductIdentityRemainsDisabled() {
        when(projectProductMapper.selectBindings(TENANT_ID,100L)).thenReturn(List.of());
        assertEquals(true,uploadService.candidateBinding(TENANT_ID,100L,null).get("disabled"));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"MDM,,DOMESTIC", "CATALOG,,DOMESTIC", "MDM,701,DOMESTIC", "MDM,,CE", "MULTI,,DOMESTIC", "MULTI,,CE", "STATED,,DOMESTIC"})
    void submitUploadUsesBoundDccProductRegardlessOfShowroomBinding(String productSource, Long handlingTaskId, String certificateType) {
        mockOwnedCompany();
        if ("MDM".equals(productSource)) mockProjectCode();
        else mockProjectCodeWithoutProductBinding();
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
        when(registrationHandlingTasks.resolveUploadTaskId(null, handlingTaskId)).thenReturn(handlingTaskId);

        var command = uploadCommand(1001L, 501L, "一次性使用无菌导管", true, false, List.of(301L, 302L), handlingTaskId);
        if ("MULTI".equals(productSource)) {
            var secondary=DccProjectCodeDO.builder().id(1002L).status(DccProjectCodeStatusConstants.ENABLE).build();secondary.setTenantId(TENANT_ID);
            when(projectCodeService.getProjectCode(ACTOR_ID,1002L)).thenReturn(secondary);
            var facts=cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(command.factsJson());
            var first=facts.getProjectProduct();
            var second=new DccRegistrationCertificateProjectProduct(1002L,664L,"P664","产品乙",List.of(new DccRegistrationCertificateProjectProduct.Binding("CONFIRMED",57L)));
            when(projectProductService.resolve(any())).thenAnswer(inv->inv.<DccProjectCodeDO>getArgument(0).getId().equals(1001L)?first:second);
            facts.setProjectProducts(List.of(first,second));
            command=new DccRegistrationCertificateUploadCommand(command.projectCodeId(),command.companyId(),"一次性使用无菌导管、产品乙",command.certificateNo(),command.firstObtainedDate(),command.effectiveDate(),command.expiryDate(),command.classification(),command.entrustedProduction(),command.selfProduction(),command.entrustedEnterpriseIds(),command.remark(),command.file(),facts.toJson(),List.of(),List.of(),null,List.of(),handlingTaskId);
        }
        if ("CE".equals(certificateType)) {
            var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(command.factsJson());
            facts.setCertificateType("CE");facts.setCertificateNo(null);facts.setFirstObtainedDate(null);facts.setEffectiveDate(null);
            var rev = new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.CeCertificate();
            rev.setRevision("R04");rev.setCertificateNo("SIMULATION-CE-R04");facts.setCeCertificates(List.of(rev));
            command = new DccRegistrationCertificateUploadCommand(command.projectCodeId(),command.companyId(),command.productName(),
                    null,null,null,command.expiryDate(),command.classification(),command.entrustedProduction(),command.selfProduction(),
                    command.entrustedEnterpriseIds(),command.remark(),command.file(),facts.toJson(),List.of(),List.of(),null,List.of(),handlingTaskId);
        }
        if ("STATED".equals(productSource)) {
            command = new DccRegistrationCertificateUploadCommand(command.projectCodeId(),command.companyId(),"证载产品名称",command.certificateNo(),command.firstObtainedDate(),command.effectiveDate(),command.expiryDate(),command.classification(),command.entrustedProduction(),command.selfProduction(),command.entrustedEnterpriseIds(),command.remark(),command.file(),command.factsJson(),List.of(),List.of(),null,List.of(),handlingTaskId,
                List.of(new MockMultipartFile("additionalPrimaryFiles","second.pdf","application/pdf",RegistrationCertificateMaterialFixtures.pdf("second"))));
            when(fileService.createFileAndReturnId(any(byte[].class),eq("second.pdf"),eq("dcc/registration-certificate/upload/9101"),eq("application/pdf"))).thenReturn(9202L);
        }
        DccRegistrationCertificateUploadSubmitResult result = uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-1", "TRACE-1", command);

        assertEquals(9401L, result.requestId());
        assertEquals(9001L, result.certificateId());
        assertEquals(9301L, result.businessFileId());
        assertEquals("MDM".equals(productSource) ? 2001L : null, draft.getValue().productMasterId());
        assertEquals(Boolean.TRUE, cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts
                .parse(draft.getValue().factsJson()).getOwnProduct());
        if ("CE".equals(certificateType)) {
            assertEquals("SIMULATION-CE-R04", draft.getValue().certificateNo());
            assertEquals("SIMULATION-CE-R04", cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts
                    .parse(draft.getValue().factsJson()).getCertificateNo());
            assertNull(draft.getValue().firstObtainedDate());
        }
        assertEquals(501L, draft.getValue().ownerCompanyId());
        assertEquals("STATED".equals(productSource)?"证载产品名称":"MULTI".equals(productSource)?"一次性使用无菌导管、产品乙":"一次性使用无菌导管", draft.getValue().productName());
        if ("STATED".equals(productSource)) {
            var links = ArgumentCaptor.forClass(DccRegistrationCertificateAccessRequestFileDO.class);
            verify(requestFileMapper, org.mockito.Mockito.times(2)).insert(links.capture());
            assertEquals("REGISTRATION_CERTIFICATE",links.getAllValues().get(1).getFileKind());
            assertEquals(Boolean.TRUE,cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(links.getAllValues().get(1).getDetailJson(),java.util.Map.class).get("additional"));
        }
        assertEquals("MULTI".equals(productSource)?2:1,cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(draft.getValue().factsJson()).getProjectProducts().size());
        assertEquals(1001L, draft.getValue().projectCodeId());
        assertEquals(Boolean.TRUE, draft.getValue().entrustedProduction());
        assertEquals(Boolean.FALSE, draft.getValue().selfProduction());
        assertEquals(List.of(301L, 302L), draft.getValue().entrustedEnterpriseIds());
        verify(registrationHandlingTasks).bindRequest(TENANT_ID, ACTOR_ID, 1001L, 9001L, 9401L, "UPLOAD", handlingTaskId);
        if (handlingTaskId == null) verify(registrationHandlingTasks, never()).authorizesProjectUpload(anyLong(), anyLong());
    }

    @Test
    void missingDomesticApprovalDateDoesNotCreateDraftOrApproval() {
        mockOwnedCompany();
        mockProjectCode();
        var command = uploadCommand(true, false, List.of(301L, 302L));
        var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(command.factsJson());
        facts.setApprovalDate(null);
        var unsupported = new DccRegistrationCertificateUploadCommand(command.projectCodeId(), command.companyId(), command.productName(),
                command.certificateNo(), command.firstObtainedDate(), command.effectiveDate(), command.expiryDate(), command.classification(),
                command.entrustedProduction(), command.selfProduction(), command.entrustedEnterpriseIds(), command.remark(), command.file(),
                facts.toJson(), List.of(), List.of(), null, List.of(), null);
        var error = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "unsupported-approval-date", "unsupported-approval-trace", unsupported));
        assertEquals("请填写批准日期", error.getMessage());
        verify(commandService, never()).createDraft(any(), any(), any(), any(), any());
        verify(requestMapper, never()).insert(any(DccRegistrationCertificateAccessRequestDO.class));
    }

    @Test
    void submitUploadForApprovalCreatesDraftWithoutProjectCodeWhenProductNameIsManual() {
        mockOwnedCompany();
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
        assertNull(draft.getValue().productMasterId());
        assertEquals("一次性使用无菌导管", draft.getValue().productName());
        assertNull(draft.getValue().projectCodeId());
        assertNull(request.getValue().getProjectCodeId());
        Map<?, ?> detail = JsonUtils.parseObject(request.getValue().getDetailJson(), Map.class);
        assertEquals("REG-CERT-UPLOAD-1", detail.get("certificateNo"));
        assertEquals("A类", detail.get("classification"));
        assertEquals("一次性使用无菌导管", detail.get("productName"));
        assertEquals("上海七木医疗器械有限公司", detail.get("ownerCompanyName"));
        verify(projectCodeService, never()).getProjectCode(any(), any());
    }

    @Test
    void submitUploadForApprovalCreatesDraftWithManualProductNameWithoutProductMaster() {
        mockOwnedCompany();
        when(requestMapper.selectByTenantAndRequestKey(TENANT_ID, "UPLOAD-KEY-MANUAL-PRODUCT")).thenReturn(null);
        ArgumentCaptor<DccRegistrationCertificateDraftData> draft =
                ArgumentCaptor.forClass(DccRegistrationCertificateDraftData.class);
        when(commandService.createDraft(eq(TENANT_ID), eq(ACTOR_ID), eq("UPLOAD-KEY-MANUAL-PRODUCT"),
                eq("TRACE-MANUAL-PRODUCT"), draft.capture())).thenReturn(9004L);
        DccRegistrationCertificateVersionDO draftVersion = DccRegistrationCertificateVersionDO.builder()
                .id(9104L)
                .certificateId(9004L)
                .status("DRAFT")
                .build();
        draftVersion.setTenantId(TENANT_ID);
        when(versionMapper.selectList(any())).thenReturn(List.of(draftVersion));
        when(fileService.createFileAndReturnId(any(byte[].class), eq("registration.pdf"),
                eq("dcc/registration-certificate/upload/9004"), eq("application/pdf"))).thenReturn(9204L);
        doAnswer(invocation -> {
            DccRegistrationCertificateFileDO file = invocation.getArgument(0);
            file.setId(9304L);
            return 1;
        }).when(fileMapper).insert(any(DccRegistrationCertificateFileDO.class));
        doAnswer(invocation -> {
            DccRegistrationCertificateAccessRequestDO request = invocation.getArgument(0);
            request.setId(9404L);
            return 1;
        }).when(requestMapper).insert(any(DccRegistrationCertificateAccessRequestDO.class));
        when(requestFileMapper.insert(any(DccRegistrationCertificateAccessRequestFileDO.class))).thenReturn(1);
        when(businessClock.now()).thenReturn(LocalDateTime.of(2026, 8, 29, 10, 0));

        DccRegistrationCertificateUploadSubmitResult result = uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-MANUAL-PRODUCT", "TRACE-MANUAL-PRODUCT",
                uploadCommand(null, "手填注册证产品", false, true, List.of()));

        assertEquals(9404L, result.requestId());
        assertEquals(9004L, result.certificateId());
        assertEquals(9304L, result.businessFileId());
        assertNull(draft.getValue().productMasterId());
        assertEquals("手填注册证产品", draft.getValue().productName());
        assertEquals("CUSTOMER-PRJ-01",cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(draft.getValue().factsJson()).getProjectCode());
        assertNull(draft.getValue().projectCodeId());
        verify(projectCodeService, never()).getProjectCode(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"QMS", "GENERAL"})
    void submitUploadRejectsLibraryInsteadOfFormalProject(String scope) {
        mockOwnedCompany();
        mockProjectCodeWithoutProductBinding();
        projectCodeService.getProjectCode(ACTOR_ID, 1001L).setLibraryScope(scope);
        ServiceException error = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-LIBRARY", "TRACE-LIBRARY",
                uploadCommand(false, true, List.of())));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID.getCode(), error.getCode());
        verify(commandService, never()).createDraft(any(), any(), any(), any(), any());
        org.mockito.Mockito.verifyNoInteractions(fileService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unbound", "duplicate", "outage"})
    void submitUploadRejectsInvalidCatalogIdentityBeforeCreatingDraftOrFile(String failure) {
        mockOwnedCompany();
        mockProjectCodeWithoutProductBinding();
        if ("outage".equals(failure)) {
            var outage = new IllegalStateException("catalog unavailable");
            when(projectProductService.resolve(any())).thenThrow(outage);
            assertEquals(outage, assertThrows(IllegalStateException.class, () -> uploadService.submitUploadForApproval(
                    TENANT_ID, ACTOR_ID, "CATALOG-INVALID", "TRACE-INVALID", uploadCommand(false, true, List.of()))));
        } else {
            when(projectProductService.resolve(any())).thenThrow(new ServiceException(
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH));
            var error = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                    TENANT_ID, ACTOR_ID, "CATALOG-INVALID", "TRACE-INVALID",
                    uploadCommand(1001L, "一次性使用无菌导管",
                            false, true, List.of())));
            assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH.getCode(), error.getCode());
        }
        verify(commandService, never()).createDraft(any(), any(), any(), any(), any());
        org.mockito.Mockito.verifyNoInteractions(fileService, productApi);
    }

    @Test
    void projectProductPreviewReturnsFormalCatalogNameAndRejectsWrongTenant() {
        mockProjectCodeWithoutProductBinding();
        var product = uploadService.getProjectProduct(TENANT_ID, ACTOR_ID, 1001L);
        assertEquals("0313170202", product.productCode());
        assertEquals("一次性使用无菌导管", product.productName());
        var denied = assertThrows(ServiceException.class, () -> uploadService.getProjectProduct(999L, ACTOR_ID, 1001L));
        assertEquals(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH.getCode(), denied.getCode());
        org.mockito.Mockito.verifyNoInteractions(fileService, commandService);
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
    void listOwnerCompaniesReturnsAllEnabledTenantOwnedCandidatesWithoutUserScope() {
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
        when(enterpriseApi.listEnabledEnterprises(
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()), "上海", 20))
                .thenReturn(List.of(candidate, otherCandidate));

        List<MdmEnterpriseRespDTO> result = uploadService.listOwnerCompanies(TENANT_ID, ACTOR_ID, "上海");

        assertEquals(2, result.size());
        assertEquals(501L, result.get(0).getId());
        assertEquals("上海七木医疗器械有限公司", result.get(0).getName());
        verify(companyScopeApi, never()).getEnabledCompanyIdsForUser(ACTOR_ID);
        assertEquals(502L, result.get(1).getId());
        verify(companyScopeApi, never()).validateUserCompanyAccess(any(), any());
    }

    @Test
    void submitUploadForApprovalRejectsUnavailableOwnerCompany() {
        org.mockito.Mockito.doThrow(new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED))
                .when(enterpriseApi).getEnabledEnterprises(List.of(999L),
                        Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));

        ServiceException exception = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "UPLOAD-KEY-UNAUTHORIZED-COMPANY", "TRACE-UNAUTHORIZED-COMPANY",
                uploadCommand(1001L, 999L, "一次性使用无菌导管", false, true, List.of())));

        assertEquals(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED.getCode(), exception.getCode());
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void approveUploadRequestFormalizesAsApproverWithRequesterCompanyScope(boolean multiple) {
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
                        .fileKind("REGISTRATION_CERTIFICATE")
                        .status("REQUESTED")
                        .build();
        requestFile.setTenantId(TENANT_ID);
        var additionalRequestFile = DccRegistrationCertificateAccessRequestFileDO.builder().id(9502L).requestId(9401L).businessFileId(9302L).fileKind("REGISTRATION_CERTIFICATE").status("REQUESTED").detailJson("{\"additional\":true}").build();
        additionalRequestFile.setTenantId(TENANT_ID);
        when(requestFileMapper.selectByRequestId(TENANT_ID, 9401L)).thenReturn(multiple ? List.of(requestFile, additionalRequestFile) : List.of(requestFile));
        if (multiple) {
            var additionalFile = DccRegistrationCertificateFileDO.builder().id(9302L).ownerType("VERSION").ownerId(9101L).fileKind("REGISTRATION_CERTIFICATE").status("STAGED").build();
            additionalFile.setTenantId(TENANT_ID);
            when(fileMapper.selectById(9302L)).thenReturn(additionalFile);
            when(fileMapper.updateById(additionalFile)).thenReturn(1);
            when(requestFileMapper.updateById(additionalRequestFile)).thenReturn(1);
        }
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
                .status("CURRENT")
                .effectiveDate(LocalDate.of(2025, 1, 2))
                .expiryDate(LocalDate.of(2026, 1, 1))
                .build();
        version.setTenantId(TENANT_ID);
        when(versionMapper.selectById(9101L)).thenReturn(version);
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .id(9901L)
                .versionId(9101L)
                .productName("一次性使用无菌导管")
                .build();
        snapshot.setTenantId(TENANT_ID);
        snapshot.setFactsJson(DccRegistrationCertificateTestFacts.domestic(version,LocalDate.of(2025,1,1)));
        when(snapshotMapper.selectListByVersionId(9101L)).thenReturn(List.of(snapshot));
        uploadService.approveUploadRequest(TENANT_ID, 88L, 9401L, "APPROVAL-KEY-1");

        verify(commandService).formalizeApprovedUpload(
                TENANT_ID, 88L, ACTOR_ID, "APPROVAL-KEY-1", "APPROVAL-KEY-1",
                9001L, 1, 1, 9301L);
        verify(projectCodeFileAssociationService).bindVersionRegistrationFile(TENANT_ID, 9101L, 9301L, 88L);
        assertEquals("APPROVED", requestFile.getStatus());
        if (multiple) {
            assertEquals("APPROVED",additionalRequestFile.getStatus());
            assertEquals("BOUND",fileMapper.selectById(9302L).getStatus());
        }
        verify(businessEventNotifier).notifyNewCertificateFormalized(
                TENANT_ID, 501L, 9001L, 9101L, 88L, "APPROVAL-KEY-1",
                "一次性使用无菌导管", "REG-CERT-UPLOAD-1",
                LocalDate.of(2025, 1, 2), LocalDate.of(2026, 1, 1), "DOMESTIC", true);
    }

    private void mockOwnedCompany() {
        lenient().when(companyScopeApi.getEnabledCompanyIdsForUser(ACTOR_ID)).thenReturn(Set.of(501L));
        MdmEnterpriseRespDTO owner = MdmEnterpriseRespDTO.builder()
                .id(501L)
                .tenantId(TENANT_ID)
                .enterpriseCode("COMP-501")
                .name("上海七木医疗器械有限公司")
                .type(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType())
                .status("ENABLE")
                .revision(1)
                .build();
        lenient().when(enterpriseApi.getEnabledEnterprises(Set.of(501L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()))).thenReturn(List.of(owner));
        lenient().when(enterpriseApi.getEnabledEnterprises(List.of(501L),
                Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()))).thenReturn(List.of(owner));
    }

    private void mockProjectCode() {
        lenient().when(projectProductService.resolve(any())).thenReturn(new DccRegistrationCertificateProjectProduct(
                1001L, 663L, "0313170202", "一次性使用无菌导管",
                List.of(new DccRegistrationCertificateProjectProduct.Binding("CONFIRMED", 56L))));
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
        lenient().when(projectProductService.resolve(any())).thenReturn(new DccRegistrationCertificateProjectProduct(
                1001L, 663L, "0313170202", "一次性使用无菌导管",
                List.of(new DccRegistrationCertificateProjectProduct.Binding("CONFIRMED", 56L))));
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(1001L)
                .projectCode("DCC-PROJ-001")
                .projectName("Catalog project")
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCode.setTenantId(TENANT_ID);
        when(projectCodeService.getProjectCode(ACTOR_ID, 1001L)).thenReturn(projectCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PRIMARY", "ADDITIONAL", "ATTACHMENT", "DESCRIPTION"})
    void rejectsInvalidMaterialBeforeDraftStorageAndApproval(String slot) {
        if (!"PRIMARY".equals(slot)) {
            mockOwnedCompany();
            mockProjectCode();
        }
        var base = uploadCommand(1001L, "一次性使用无菌导管", false, true, List.of());
        var invalid = new MockMultipartFile("file", "invalid.pdf", "application/pdf",
                "not a PDF".getBytes(StandardCharsets.UTF_8));
        var command = new DccRegistrationCertificateUploadCommand(base.projectCodeId(), base.companyId(),
                base.productName(), base.certificateNo(), base.firstObtainedDate(), base.effectiveDate(),
                base.expiryDate(), base.classification(), base.entrustedProduction(), base.selfProduction(),
                base.entrustedEnterpriseIds(), base.remark(), "PRIMARY".equals(slot) ? invalid : base.file(),
                base.factsJson(), "ATTACHMENT".equals(slot) ? List.of(invalid) : List.of(),
                "DESCRIPTION".equals(slot) ? List.of(invalid) : List.of(), null, List.of(), null,
                "ADDITIONAL".equals(slot) ? List.of(invalid) : List.of());

        ServiceException exception = assertThrows(ServiceException.class, () -> uploadService.submitUploadForApproval(
                TENANT_ID, ACTOR_ID, "INVALID-MATERIAL", "TRACE-INVALID", command));

        assertEquals(400, exception.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("实际格式与扩展名不符"));
        org.mockito.Mockito.verifyNoInteractions(commandService, fileService);
        verify(requestMapper, never()).insert(any(DccRegistrationCertificateAccessRequestDO.class));
        verify(fileMapper, never()).insert(any(DccRegistrationCertificateFileDO.class));
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds) {
        return uploadCommand(1001L, "一次性使用无菌导管",
                entrustedProduction, selfProduction, entrustedEnterpriseIds);
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Long projectCodeId, String productName,
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds) {
        return uploadCommand(projectCodeId, 501L, productName,
                entrustedProduction, selfProduction, entrustedEnterpriseIds);
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Long projectCodeId, Long companyId, String productName,
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds) {
        return uploadCommand(projectCodeId, companyId, productName, entrustedProduction, selfProduction, entrustedEnterpriseIds, null);
    }

    private DccRegistrationCertificateUploadCommand uploadCommand(
            Long projectCodeId, Long companyId, String productName,
            Boolean entrustedProduction, Boolean selfProduction, List<Long> entrustedEnterpriseIds, Long handlingTaskId) {
        return new DccRegistrationCertificateUploadCommand(
                projectCodeId,
                companyId,
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
                        RegistrationCertificateMaterialFixtures.pdf("registration")),
                uploadFacts(projectCodeId!=null),List.of(),List.of(),null,List.of(),handlingTaskId);
    }

    private String uploadFacts(boolean ownProduct) {
        var facts=new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts();
        facts.setCertificateType("DOMESTIC");facts.setCertificateStatus("VALID");facts.setOwnProduct(ownProduct);
        facts.setCertificateNo("REG-CERT-UPLOAD-1");facts.setClassification("A类");
        facts.setFirstObtainedDate(LocalDate.of(2025,1,1));facts.setEffectiveDate(LocalDate.of(2025,1,2));
        facts.setApprovalDate(LocalDate.of(2025,1,1));
        if(ownProduct) {
            var product=new DccRegistrationCertificateProjectProduct(1001L,663L,"0313170202","一次性使用无菌导管",List.of(new DccRegistrationCertificateProjectProduct.Binding("CONFIRMED",56L)));
            facts.setProjectProduct(product);facts.setProjectProducts(List.of(product));
        }
        if(!ownProduct)facts.setProjectCode("CUSTOMER-PRJ-01");facts.setExpiryDate(LocalDate.of(2026,1,1));facts.setRemark("上传注册证审批");
        return facts.toJson();
    }
}
