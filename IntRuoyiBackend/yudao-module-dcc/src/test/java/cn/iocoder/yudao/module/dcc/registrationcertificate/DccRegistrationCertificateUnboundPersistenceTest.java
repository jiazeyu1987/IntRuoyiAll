package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccApprovedProductIdentityMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization.DccRegistrationCertificateFormalizationService;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real H2 project/provenance and certificate transactions; not a UI or permission test. */
@Import({DccRegistrationCertificateCommandService.class, DccRegistrationCertificateCommandTransactionService.class,
        DccRegistrationCertificateDraftRepository.class, DccRegistrationCertificatePrerequisiteValidator.class,
        DccRegistrationCertificateTerminalAuditService.class, DccRegistrationCertificateFailureAuditService.class,
        DccRegistrationCertificateFormalizationService.class, DccRegistrationCertificateCommandMutex.class,
        DccRegistrationCertificateCommandServiceTest.DbTestConfiguration.class})
class DccRegistrationCertificateUnboundPersistenceTest extends BaseDbUnitTest {
    @Resource private DccRegistrationCertificateCommandService commands;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccApprovedProductIdentityMapper evidence;
    @Resource private DccRegistrationCertificateMapper certificates;
    @Resource private DccRegistrationCertificateVersionMapper versions;
    @Resource private DccRegistrationCertificateSnapshotMapper snapshots;
    @Resource private DccRegistrationCertificateFileMapper files;
    @Resource private JdbcTemplate jdbc;
    @MockitoBean private DccProjectCodeService projectService;
    @MockitoBean private MdmCompanyScopeApi companyScope;
    @MockitoBean private MdmEnterpriseApi enterprises;
    @MockitoBean private MdmProductApi products;
    @MockitoBean private ControlledContentRegistrationProjectionService projection;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(1L);
        reset(projectService, enterprises, products, projection, companyScope);
        when(enterprises.getEnabledEnterprises(eq(List.of(10L)), any())).thenReturn(List.of(
                MdmEnterpriseRespDTO.builder().id(10L).tenantId(1L).name("Task owner").build()));
        // Access permission is the external boundary; identity is read from the real project table.
        when(projectService.getProjectCode(99L, 40L)).thenAnswer(call -> projects.selectById(40L));
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @ParameterizedTest
    @ValueSource(strings = {"UNBOUND", "DAMAGED", "QMS", "GENERAL"})
    void persistedProjectIsRevalidatedBeforeCertificateCanBecomeFormal(String state) {
        var project = DccProjectCodeDO.builder().id(40L).projectCode("WORD1104-PC-UNBOUND")
                .projectName("Task legacy project").libraryScope("PROJECT").status("ENABLE").build();
        project.setTenantId(1L);
        assertEquals(1, projects.insert(project));
        assertEquals(0, evidence.countDeclaredProductEvidence(1L, 40L));
        assertTrue(evidence.selectApprovedProduct(1L, 40L).isEmpty());
        var facts = new DccRegistrationCertificateFacts();
        facts.setCertificateType("DOMESTIC"); facts.setCertificateStatus("VALID"); facts.setOwnProduct(true);
        facts.setCertificateNo("WORD1104-PC-UNBOUND-CERT"); facts.setClassification("II"); facts.setRemark("Task test");
        facts.setFirstObtainedDate(LocalDate.of(2026, 1, 1));
        facts.setEffectiveDate(LocalDate.of(2026, 8, 1)); facts.setExpiryDate(LocalDate.of(2031, 8, 1));
        var draft = new DccRegistrationCertificateDraftData(10L, null, "Explicit manual product", 40L,
                facts.getFirstObtainedDate(), facts.getCertificateNo(), null, facts.getEffectiveDate(),
                facts.getExpiryDate(), "II", null, null, null, null, null, null, null,
                false, true, List.of(), "Task test", facts.toJson());
        Long id = commands.createDraft(1L, 99L, "unbound-create", "task-trace", draft);
        var version = versions.selectList().stream().filter(v -> id.equals(v.getCertificateId())).findFirst().orElseThrow();
        var file = DccRegistrationCertificateFileDO.builder().ownerType("VERSION").ownerId(version.getId())
                .fileKind("REGISTRATION_CERTIFICATE").infraFileId(7001L).originalName("task.pdf")
                .mimeType("application/pdf").fileSize(128L).sha256("a".repeat(64)).status("STAGED").build();
        file.setTenantId(1L); files.insert(file);

        if ("DAMAGED".equals(state)) {
            jdbc.update("INSERT INTO dcc_project_product_relation (tenant_id,request_id,project_code_id,product_catalog_id,relation_status) VALUES (1,999,40,888,'ACTIVE')");
            assertEquals(1, evidence.countDeclaredProductEvidence(1L, 40L));
            assertTrue(evidence.selectApprovedProduct(1L, 40L).isEmpty());
        } else if (!"UNBOUND".equals(state)) {
            project.setLibraryScope(state); projects.updateById(project);
        }
        if ("UNBOUND".equals(state)) {
            assertEquals(id, commands.formalizeApprovedUpload(1L, 99L, 99L, "unbound-approve", "task-trace", id, 1, 1, file.getId()));
            var certificate = certificates.selectById(id);
            assertEquals("ACTIVE", certificate.getStatus()); assertEquals(40L, certificate.getProjectCodeId());
            assertNull(certificate.getProductMasterId());
            assertEquals("Explicit manual product", snapshots.selectById(certificate.getCurrentSnapshotId()).getProductName());
            assertEquals("BOUND", files.selectById(file.getId()).getStatus());
        } else {
            assertThrows(ServiceException.class, () -> commands.createDraft(
                    1L, 99L, "invalid-project-create", "task-trace", draft));
            assertEquals(1, certificates.selectList().size(), "Invalid project must not create another certificate");
            var error = assertThrows(ServiceException.class, () -> commands.formalizeApprovedUpload(
                    1L, 99L, 99L, "unbound-approve", "task-trace", id, 1, 1, file.getId()));
            assertEquals("DAMAGED".equals(state) ? REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH.getCode()
                    : REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID.getCode(), error.getCode());
            assertEquals("DRAFT", certificates.selectById(id).getStatus());
            assertEquals("STAGED", files.selectById(file.getId()).getStatus());
            assertNull(certificates.selectById(id).getCurrentSnapshotId());
        }
        verifyNoInteractions(products);
    }
}
