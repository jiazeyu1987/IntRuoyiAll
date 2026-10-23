package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileActionProjectionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectFilePlacementMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** R5 original O/file/round regression; persistence and live-duty ports are isolated, Query and Guard are real. */
@MockitoSettings(strictness = Strictness.LENIENT)
class DccTrainingOwnerProjectionRegressionTest extends BaseMockitoUnitTest {
    private static final long FILE = 2054545668044084222L;
    private static final long OWNER = 910463L;
    private static final long ORDINARY = 999002L; // Isolated test subject, not a runtime account.
    private static final String BPM = "fb0c1653-ca6f-11f1-9342-b082e25ec548";
    @InjectMocks private DccControlledFileQueryServiceImpl query;
    @InjectMocks private DccControlledFileDetailAuthorizationGuard guard;
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock private DccProjectCodeMapper projectCodeMapper;
    @Mock private DccProjectFilePlacementMapper projectFilePlacementMapper;
    @Mock private FileMapper fileMapper;
    @Mock private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Mock private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock private DccProjectAccessService projectAccessService;
    @Mock private DccOfflineTrainingRecordService offlineTraining;
    @Mock private AdminUserApi adminUserApi;
    @Mock private BpmProcessInstanceService bpmProcessInstanceService;
    @Mock private DccApplicationRoundService applicationRoundService;
    @Mock private DccBoundWorkflowContractResolver boundWorkflowContractResolver;
    private DccControlledFileDO file;
    private FileDO source;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        file = DccControlledFileDO.builder().id(FILE).tenantId(1L).masterId(700L).dccProjectCodeId(5L)
                .categoryId(1L).directoryId(99L).requesterId(910457L).fileNumber("DCC241016C13R18ADJUST")
                .fileName("DCC241016C13R18Adjust.pdf").versionNo("A/1").changeType("NEW")
                .status(DccOfflineTrainingRecordService.STATUS).processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .processInstanceId(BPM).needTraining(true).sourceFileId(8800L).build();
        // Master/project/source IDs below belong only to this isolated fixture.
        source = FileDO.builder().id(8800L).name("DCC241016C13R18Adjust.pdf").type("application/pdf").build();
        when(controlledFileMapper.selectById(FILE)).thenReturn(file);
        when(controlledFileMasterMapper.selectById(700L)).thenReturn(DccControlledFileMasterDO.builder()
                .id(700L).tenantId(1L).dccProjectCodeId(5L).build());
        var project = DccProjectCodeDO.builder().id(5L).projectName("isolated original O regression").build();
        project.setTenantId(1L);
        when(projectCodeMapper.selectById(5L)).thenReturn(project);
        when(fileMapper.selectById(8800L)).thenReturn(source);
        when(assignmentScopeService.isWithinAssignedFileScope(anyLong(), eq(FILE))).thenReturn(true);
        when(projectAccessService.listReadableProjectIds(anyLong())).thenReturn(List.of(5L));
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(anyLong(), any())).thenReturn(Set.of());
        when(offlineTraining.canOrganizeTraining(OWNER, file)).thenReturn(true);
        when(offlineTraining.canReadTrainingBody(OWNER, file)).thenReturn(true);
    }

    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test void originalOwnerRelationSourceUsesCurrentDutyWithoutGlobalDiscoveryOrEditGrant() {
        assertFalse(query.canViewFileName(OWNER, file));
        assertDoesNotThrow(() -> query.assertRelationNameVisible(OWNER, FILE));
        var projection = query.getRelationPermissions(OWNER, FILE);
        assertEquals(FILE, projection.controlledFileId());
        assertEquals("A/1", projection.versionNo());
        assertEquals("DCC241016C13R18ADJUST", projection.fileNumber());
        assertFalse(projection.canEdit());
        assertTrue(projection.canPreview());
        assertFalse(projection.hasCurrentControlledSource());
    }

    @Test void originalOwnerApprovedSourceProducesPreviewActionWithoutUploadOrDownloadGrant() {
        FileDO preview = ReflectionTestUtils.invokeMethod(query, "resolvePreviewFileForSummary", file);
        assertSame(source, preview, "native training wait must project this round's approved source artifact");
        Boolean canPreview = ReflectionTestUtils.invokeMethod(query, "canReadBinary", OWNER, file, DccAccessTypeEnum.PREVIEW);
        assertEquals(Boolean.TRUE, canPreview);
        DccControlledFileActionProjectionRespVO projection = ReflectionTestUtils.invokeMethod(query,
                "buildActionProjection", OWNER, file, canPreview, false, false, false, false, false, false);
        assertEquals(List.of("VIEW", "PREVIEW"), projection.getAllowedActions());
        assertFalse(projection.getCanWithdraw());
        when(offlineTraining.canReadTrainingBody(OWNER, file)).thenReturn(false);
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(query, "canReadBinary", OWNER, file, DccAccessTypeEnum.PREVIEW));
        when(fileMapper.selectById(8800L)).thenReturn(null);
        assertNull(ReflectionTestUtils.invokeMethod(query, "resolvePreviewFileForSummary", file));
        file.setSourceFileId(null);
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(query, "resolvePreviewFileForSummary", file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ordinary", "expiredDuty", "outsideScope", "wrongTenant"})
    void sourceMetadataStillRejectsWithoutCurrentExactDuty(String change) {
        long actor = "ordinary".equals(change) ? ORDINARY : OWNER;
        if ("expiredDuty".equals(change)) when(offlineTraining.canOrganizeTraining(OWNER, file)).thenReturn(false);
        if ("outsideScope".equals(change)) when(assignmentScopeService.isWithinAssignedFileScope(OWNER, FILE)).thenReturn(false);
        if ("wrongTenant".equals(change)) file.setTenantId(2L);
        assertServiceException(() -> query.getRelationPermissions(actor, FILE), CONTROLLED_FILE_ACCESS_DENIED);
        assertServiceException(() -> query.assertRelationNameVisible(actor, FILE), CONTROLLED_FILE_ACCESS_DENIED);
    }

    @Test void exactOriginalOwnerTrainingContextPassesRealBodyGuard() {
        authenticate(OWNER);
        prepareRound();
        var context = trainingContext(FILE, "A/1", BPM);
        assertDoesNotThrow(() -> guard.requireBodyRead(OWNER, file, context));
        verify(offlineTraining).canReadTrainingBody(OWNER, file);
        assertServiceException(() -> guard.requireBodyRead(OWNER, file, DccControlledFileBodyReadContext.daily(FILE)),
                CONTROLLED_FILE_ACCESS_DENIED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ordinary", "expiredDuty", "outsideScope", "wrongTenant", "otherRound", "otherFile", "otherVersion"})
    void trainingContextNeverGrantsAnUnauthorizedOrMismatchedBody(String change) {
        long actor = "ordinary".equals(change) ? ORDINARY : OWNER;
        authenticate(actor);
        prepareRound();
        if ("expiredDuty".equals(change)) when(offlineTraining.canReadTrainingBody(OWNER, file)).thenReturn(false);
        if ("outsideScope".equals(change)) when(assignmentScopeService.isWithinAssignedFileScope(OWNER, FILE)).thenReturn(false);
        if ("wrongTenant".equals(change)) file.setTenantId(2L);
        var context = trainingContext("otherFile".equals(change) ? FILE + 1 : FILE,
                "otherVersion".equals(change) ? "A/2" : "A/1", "otherRound".equals(change) ? "other-round" : BPM);
        if ("otherFile".equals(change) || "otherVersion".equals(change)) {
            assertThrows(IllegalArgumentException.class, () -> guard.requireBodyRead(actor, file, context));
        } else {
            assertServiceException(() -> guard.requireBodyRead(actor, file, context), CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    private DccControlledFileBodyReadContext trainingContext(long id, String version, String round) {
        return new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.TRAINING_APPLICATION,
                Long.toString(id), version, "UPLOAD", round, null);
    }

    private void authenticate(long actor) {
        var login = new LoginUser();
        login.setId(actor); login.setTenantId(1L); login.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        var user = new AdminUserRespDTO();
        user.setId(actor); user.setTenantId(1L); user.setStatus(0);
        when(adminUserApi.getUser(actor)).thenReturn(user);
    }

    private void prepareRound() {
        var history = mock(HistoricProcessInstance.class);
        when(bpmProcessInstanceService.getHistoricProcessInstance(BPM)).thenReturn(history);
        when(history.getId()).thenReturn(BPM); when(history.getTenantId()).thenReturn("1");
        when(history.getProcessDefinitionId()).thenReturn("definition-original-O");
        when(history.getStartUserId()).thenReturn("910457"); when(history.getBusinessKey()).thenReturn(Long.toString(FILE));
        var runtime = mock(ProcessInstance.class);
        when(bpmProcessInstanceService.getProcessInstance(BPM)).thenReturn(runtime);
        when(runtime.getId()).thenReturn(BPM); when(runtime.getTenantId()).thenReturn("1");
        when(runtime.getProcessDefinitionId()).thenReturn("definition-original-O"); when(runtime.getBusinessKey()).thenReturn(Long.toString(FILE));
        when(applicationRoundService.require("UPLOAD", FILE, BPM)).thenReturn(1);
        var flow = new DccBoundWorkflowContractResolver.RoundFlowContract(1L, FILE, "UPLOAD", 1, BPM,
                DccControlledFileProcessDefinitionKeys.UPLOAD, "definition-original-O", "deployment-original-O", "digest-original-O",
                "PUBLIC_MATRIX_NATIVE_V1", DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,
                false, null, null, DccBoundWorkflowContractResolver.RoundContractStatus.READY, null);
        when(boundWorkflowContractResolver.resolveRoundFlowContract(eq(file), any(DccApplicationRoundSummary.class))).thenReturn(flow);
    }
}
