package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryViewMatrixRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateReasonDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationDirectionSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityRuleSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationVisibilityUserSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryViewMatrixRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationDirectionSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationRelationSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityRuleSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationVisibilityUserSnapshotMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccPublicationFollowupServiceTest extends BaseMockitoUnitTest {

    @Mock private DccPublicationFollowupBatchMapper batchMapper;
    @Mock private DccPublicationVisibilityRuleSnapshotMapper visibilityRuleMapper;
    @Mock private DccPublicationVisibilityUserSnapshotMapper visibilityUserMapper;
    @Mock private DccPublicationNotificationCandidateMapper candidateMapper;
    @Mock private DccPublicationNotificationCandidateReasonMapper candidateReasonMapper;
    @Mock private DccPublicationRelationSnapshotMapper relationSnapshotMapper;
    @Mock private DccPublicationRelationDirectionSnapshotMapper relationDirectionMapper;
    @Mock private DccCategoryViewMatrixRuleMapper viewMatrixRuleMapper;
    @Mock private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Mock private DccControlledFileDistributionMapper distributionMapper;
    @Mock private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock private DccControlledFileRelatedFileService relatedFileService;
    @Mock private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService relationStages;
    @Mock private DccControlledFileMasterMapper masterMapper;
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private DeptApi deptApi;
    @Mock private DccRelatedFileImpactAssessmentService impactAssessmentService;
    @Mock private DccPublicationNotificationService publicationNotificationService;

    @InjectMocks
    private DccPublicationFollowupServiceImpl service;

    private final AtomicReference<DccPublicationFollowupBatchDO> storedBatch = new AtomicReference<>();
    private final AtomicLong childId = new AtomicLong(1000L);

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        lenient().doAnswer(invocation -> {
            DccPublicationFollowupBatchDO candidate = invocation.getArgument(0);
            if (storedBatch.get() == null) {
                candidate.setId(900L);
                storedBatch.set(candidate);
            }
            return 1;
        }).when(batchMapper).insertOrKeepExisting(any(DccPublicationFollowupBatchDO.class));
        lenient().when(batchMapper.selectByPublishedControlledFileId(1L, 100L))
                .thenAnswer(invocation -> storedBatch.get());
        assignGeneratedId(visibilityRuleMapper, DccPublicationVisibilityRuleSnapshotDO.class);
        assignGeneratedId(candidateMapper, DccPublicationNotificationCandidateDO.class);
        assignGeneratedId(relationSnapshotMapper, DccPublicationRelationSnapshotDO.class);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void recordPublishedRevision_freezesActualViewSourcesCandidatesAndBidirectionalRelationsByMaster() {
        DccControlledFileDO published = publishedB1();
        DccCategoryViewMatrixRuleDO matrixRule = DccCategoryViewMatrixRuleDO.builder()
                .id(501L).categoryId(20L).subjectType("ROLE").subjectId(700L)
                .scopeType("ALL_MEMBERS").active(true).subjectLabel("研发角色").build();
        when(viewMatrixRuleMapper.selectActiveListByCategoryId(20L)).thenReturn(List.of(matrixRule));
        DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput matrixInput =
                new DccControlledFileViewMatrixAccessService.ViewMatrixRuleInput(
                        501L, 20L, null, null, null, "研发角色", null, null,
                        null, "ALL_MEMBERS", "ROLE", 700L, true, null);
        when(viewMatrixAccessService.toInput(matrixRule)).thenReturn(matrixInput);
        when(viewMatrixAccessService.resolveRules(20L, List.of(matrixInput))).thenReturn(
                new DccControlledFileViewMatrixAccessService.ViewMatrixAccessResolution(20L, List.of(
                        new DccControlledFileViewMatrixAccessService.ViewMatrixAccessSubject(
                                5L, "矩阵用户", "CURRENT_VIEW_MATRIX", null, null, null,
                                "研发角色", null, "ALL_MEMBERS", "ROLE", 700L, "角色解析")), List.of()));
        when(distributionMapper.selectListByControlledFileId(100L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder().id(601L).controlledFileId(100L)
                        .departmentId(11L).distributionMedium("PUBLIC_FOLDER").status("PENDING").build(),
                DccControlledFileDistributionDO.builder().id(602L).controlledFileId(100L)
                        .departmentId(12L).distributionMedium("PAPER").status("PENDING").build()));
        when(distributionRecipientMapper.selectListByDistributionId(601L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder().id(611L).distributionId(601L).userId(1L).build(),
                DccControlledFileDistributionRecipientDO.builder().id(612L).distributionId(601L).userId(2L).build()));
        when(relatedFileService.listForwardRelations(100L)).thenReturn(List.of(
                relation(701L, 100L, 200L, 20L)));
        when(relatedFileService.listReverseCurrentActiveRelations(1L, 10L)).thenReturn(List.of(
                relation(702L, 200L, 99L, 10L),
                relation(703L, 300L, 98L, 10L)));
        when(masterMapper.selectBatchIds(Set.of(20L, 30L))).thenReturn(List.of(
                master(20L, 200L, "REL-20"), master(30L, 300L, "REL-30")));
        when(controlledFileMapper.selectBatchIds(Set.of(200L, 300L))).thenReturn(List.of(
                relatedActive(200L, 20L, 3L, "A/1"), relatedActive(300L, 30L, 4L, "C/1")));
        when(assignmentScopeService.filterBusinessVisibleUserIds(Set.of(1L, 2L, 5L), 100L))
                .thenReturn(Set.of(1L, 2L));
        when(adminUserApi.getUserList(Set.of(1L, 2L))).thenReturn(List.of(
                user(1L, "责任人", 10L), user(2L, "分发人", 11L)));
        when(deptApi.getDeptList(Set.of(10L, 11L))).thenReturn(List.of(
                dept(10L, "质量部"), dept(11L, "生产部")));

        service.recordPublishedRevision(published, activeA2());

        ArgumentCaptor<DccPublicationFollowupBatchDO> batchCaptor =
                ArgumentCaptor.forClass(DccPublicationFollowupBatchDO.class);
        verify(batchMapper).insertOrKeepExisting(batchCaptor.capture());
        assertEquals(40L, batchCaptor.getValue().getDccProjectCodeId());
        assertEquals(20L, batchCaptor.getValue().getCategoryId());
        assertEquals(30L, batchCaptor.getValue().getDirectoryId());
        assertEquals(50L, batchCaptor.getValue().getFileTypeTaxonomyLeafId());
        assertEquals(published.getPublishedTime(), batchCaptor.getValue().getPublishedAt());

        ArgumentCaptor<DccPublicationVisibilityRuleSnapshotDO> ruleCaptor =
                ArgumentCaptor.forClass(DccPublicationVisibilityRuleSnapshotDO.class);
        verify(visibilityRuleMapper, times(3)).insert(ruleCaptor.capture());
        assertEquals(Set.of("FILE_REQUESTER", "CURRENT_VIEW_MATRIX", "PUBLIC_FOLDER_DISTRIBUTION"),
                ruleCaptor.getAllValues().stream().map(DccPublicationVisibilityRuleSnapshotDO::getSourceType)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(ruleCaptor.getAllValues().stream().anyMatch(rule ->
                "CURRENT_VIEW_MATRIX".equals(rule.getSourceType())
                        && "FILTERED_BY_ASSIGNMENT".equals(rule.getResolutionStatus())));
        ArgumentCaptor<DccPublicationVisibilityUserSnapshotDO> visibleUserCaptor =
                ArgumentCaptor.forClass(DccPublicationVisibilityUserSnapshotDO.class);
        verify(visibilityUserMapper, times(3)).insert(visibleUserCaptor.capture());
        assertEquals(List.of(1L, 1L, 2L), visibleUserCaptor.getAllValues().stream()
                .map(DccPublicationVisibilityUserSnapshotDO::getUserId).toList());
        assertTrue(visibleUserCaptor.getAllValues().stream()
                .allMatch(user -> "ALLOWED".equals(user.getAssignmentScopeResult())));

        ArgumentCaptor<DccPublicationNotificationCandidateDO> candidateCaptor =
                ArgumentCaptor.forClass(DccPublicationNotificationCandidateDO.class);
        verify(candidateMapper, times(2)).insert(candidateCaptor.capture());
        assertEquals(Set.of(1L, 2L), candidateCaptor.getAllValues().stream()
                .map(DccPublicationNotificationCandidateDO::getUserId).collect(java.util.stream.Collectors.toSet()));
        verify(candidateReasonMapper, times(3)).insert(any(DccPublicationNotificationCandidateReasonDO.class));

        ArgumentCaptor<DccPublicationRelationSnapshotDO> relationCaptor =
                ArgumentCaptor.forClass(DccPublicationRelationSnapshotDO.class);
        verify(relationSnapshotMapper, times(2)).insert(relationCaptor.capture());
        assertEquals(Set.of(20L, 30L), relationCaptor.getAllValues().stream()
                .map(DccPublicationRelationSnapshotDO::getRelatedMasterId).collect(java.util.stream.Collectors.toSet()));
        ArgumentCaptor<DccPublicationRelationDirectionSnapshotDO> directionCaptor =
                ArgumentCaptor.forClass(DccPublicationRelationDirectionSnapshotDO.class);
        verify(relationDirectionMapper, times(3)).insert(directionCaptor.capture());
        assertEquals(Set.of("FORWARD", "REVERSE"), directionCaptor.getAllValues().stream()
                .map(DccPublicationRelationDirectionSnapshotDO::getDirection).collect(java.util.stream.Collectors.toSet()));
        verify(impactAssessmentService,never()).materializeForPublicationBatch(any());
        verify(publicationNotificationService).materializeForPublicationBatch(900L);
        verify(impactAssessmentService).resolveLinkedRevisionAfterPublication(published);
    }

    @Test
    void recordPublishedRevision_duplicateCallbackAndPermissionMutationDoNotRewriteSnapshots() {
        DccControlledFileDO published = publishedB1();
        when(viewMatrixRuleMapper.selectActiveListByCategoryId(20L)).thenReturn(List.of());
        when(distributionMapper.selectListByControlledFileId(100L)).thenReturn(List.of());
        when(relatedFileService.listForwardRelations(100L)).thenReturn(List.of());
        when(relatedFileService.listReverseCurrentActiveRelations(1L, 10L)).thenReturn(List.of());
        when(assignmentScopeService.filterBusinessVisibleUserIds(Set.of(1L), 100L)).thenReturn(Set.of(1L));
        when(adminUserApi.getUserList(Set.of(1L))).thenReturn(List.of(user(1L, "责任人", 10L)));
        when(deptApi.getDeptList(Set.of(10L))).thenReturn(List.of(dept(10L, "质量部")));

        service.recordPublishedRevision(published, activeA2());
        service.recordPublishedRevision(published, activeA2());

        verify(viewMatrixRuleMapper, times(1)).selectActiveListByCategoryId(20L);
        verify(visibilityRuleMapper, times(1)).insert(any(DccPublicationVisibilityRuleSnapshotDO.class));
        verify(visibilityUserMapper, times(1)).insert(any(DccPublicationVisibilityUserSnapshotDO.class));
        verify(candidateMapper, times(1)).insert(any(DccPublicationNotificationCandidateDO.class));
        verify(candidateReasonMapper, times(1)).insert(any(DccPublicationNotificationCandidateReasonDO.class));
    }

    @Test
    void recordPublishedRevision_duplicatePublishedFileWithConflictingIdentityFailsFast() {
        DccControlledFileDO published = publishedB1();
        DccPublicationFollowupBatchDO conflicting = DccPublicationFollowupBatchDO.builder()
                .id(900L).publishedControlledFileId(100L).publishedMasterId(999L)
                .previousActiveControlledFileId(99L).dccProjectCodeId(40L).categoryId(20L).directoryId(30L)
                .fileTypeTaxonomyLeafId(50L).fileNumberSnapshot("DOC-100").fileNameSnapshot("设计规范")
                .versionNoSnapshot("B/1").publishedAt(published.getPublishedTime())
                .creationToken("existing-token").build();
        conflicting.setTenantId(1L);
        storedBatch.set(conflicting);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.recordPublishedRevision(published, activeA2()));

        assertTrue(error.getMessage().contains("identity conflict"));
        verifyNoInteractions(visibilityRuleMapper, visibilityUserMapper, candidateMapper, candidateReasonMapper,
                relationSnapshotMapper, relationDirectionMapper);
    }

    @Test
    void recordPublishedRevision_snapshotInsertFailurePropagatesBeforeCandidatesAndRelations() {
        DccControlledFileDO published = publishedB1();
        when(viewMatrixRuleMapper.selectActiveListByCategoryId(20L)).thenReturn(List.of());
        doThrow(new IllegalStateException("snapshot insert failed"))
                .when(visibilityRuleMapper).insert(any(DccPublicationVisibilityRuleSnapshotDO.class));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.recordPublishedRevision(published, activeA2()));

        assertEquals("snapshot insert failed", error.getMessage());
        verifyNoInteractions(candidateMapper, candidateReasonMapper, relationSnapshotMapper, relationDirectionMapper);
    }

    @Test
    void recordPublishedRevision_distributionNotificationFailurePropagatesAndNoImplicitImpactMaterialization() {
        DccControlledFileDO published = publishedB1();
        when(viewMatrixRuleMapper.selectActiveListByCategoryId(20L)).thenReturn(List.of());
        when(distributionMapper.selectListByControlledFileId(100L)).thenReturn(List.of());
        when(relatedFileService.listForwardRelations(100L)).thenReturn(List.of());
        when(relatedFileService.listReverseCurrentActiveRelations(1L, 10L)).thenReturn(List.of());
        when(assignmentScopeService.filterBusinessVisibleUserIds(Set.of(1L), 100L)).thenReturn(Set.of(1L));
        when(adminUserApi.getUserList(Set.of(1L))).thenReturn(List.of(user(1L, "责任人", 10L)));
        when(deptApi.getDeptList(Set.of(10L))).thenReturn(List.of(dept(10L, "质量部")));
        doThrow(new IllegalStateException("distribution notification materialization failed"))
                .when(publicationNotificationService).materializeForPublicationBatch(900L);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.recordPublishedRevision(published, activeA2()));

        assertEquals("distribution notification materialization failed", error.getMessage());
        verify(impactAssessmentService,never()).materializeForPublicationBatch(any());
        verify(impactAssessmentService, never()).resolveLinkedRevisionAfterPublication(any());
    }

    @Test
    void recordPublishedRevision_batchInsertFailurePropagatesWithoutChildSnapshots() {
        DccControlledFileDO published = publishedB1();
        doThrow(new IllegalStateException("batch insert failed"))
                .when(batchMapper).insertOrKeepExisting(any(DccPublicationFollowupBatchDO.class));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.recordPublishedRevision(published, activeA2()));

        assertEquals("batch insert failed", error.getMessage());
        verifyNoInteractions(visibilityRuleMapper, visibilityUserMapper, candidateMapper, candidateReasonMapper,
                relationSnapshotMapper, relationDirectionMapper);
    }

    @Test
    void recordPublishedRevision_minorIterationDoesNotCreateFollowupBatch() {
        DccControlledFileDO publishedA2 = publishedB1();
        publishedA2.setVersionNo("A/2");
        publishedA2.setRevisionCode("A");
        publishedA2.setIterationNo(2);

        service.recordPublishedRevision(publishedA2, activeA1());

        verifyNoInteractions(batchMapper, visibilityRuleMapper, visibilityUserMapper, candidateMapper,
                candidateReasonMapper, relationSnapshotMapper, relationDirectionMapper,
                viewMatrixRuleMapper, viewMatrixAccessService, assignmentScopeService,
                distributionMapper, distributionRecipientMapper, relatedFileService, masterMapper,
                controlledFileMapper, adminUserApi, deptApi, impactAssessmentService,
                publicationNotificationService);
    }

    @Test
    void recordPublishedRevision_configuredTwoSegmentMajorIdentityTreatsThirdSegmentAsMinor() {
        DccControlledFileVersionPolicyProperties properties = new DccControlledFileVersionPolicyProperties();
        properties.setMajorIdentitySegmentCount(2);
        ReflectionTestUtils.setField(service, "versionPolicy", new DccControlledFileVersionPolicy(properties));
        DccControlledFileDO publishedA12 = publishedB1();
        publishedA12.setVersionNo("A/1/2");
        publishedA12.setRevisionCode("A/1");
        publishedA12.setIterationNo(2);
        DccControlledFileDO previousA11 = DccControlledFileDO.builder()
                .id(98L).masterId(10L).versionNo("A/1/1").revisionCode("A/1").iterationNo(1).build();

        service.recordPublishedRevision(publishedA12, previousA11);

        verifyNoInteractions(batchMapper, visibilityRuleMapper, visibilityUserMapper, candidateMapper,
                candidateReasonMapper, relationSnapshotMapper, relationDirectionMapper,
                viewMatrixRuleMapper, viewMatrixAccessService, assignmentScopeService,
                distributionMapper, distributionRecipientMapper, relatedFileService, masterMapper,
                controlledFileMapper, adminUserApi, deptApi, impactAssessmentService,
                publicationNotificationService);
    }

    @Test
    void recordPublishedRevision_firstPublicationDoesNotCreateFollowupBatch() {
        service.recordPublishedRevision(publishedB1(), null);

        verifyNoInteractions(batchMapper, visibilityRuleMapper, visibilityUserMapper, candidateMapper,
                candidateReasonMapper, relationSnapshotMapper, relationDirectionMapper,
                viewMatrixRuleMapper, viewMatrixAccessService, assignmentScopeService,
                distributionMapper, distributionRecipientMapper, relatedFileService, masterMapper,
                controlledFileMapper, adminUserApi, deptApi, impactAssessmentService,
                publicationNotificationService);
    }

    @Test
    void activationNotifiesReverseOwnersOnceForPartialWithoutChangingVisibility() {
        var published = publishedB1();
        published.setStatus("ACTIVE");
        published.setRevisionChangeType("PARTIAL");
        published.setVersionNo("A/3");
        published.setControlledTime(published.getPublishedTime());
        published.setActivatedTime(published.getPublishedTime().plusDays(1));
        published.setProcessInstanceId("revision-round");
        var previous = activeA2(); previous.setTenantId(1L);
        when(controlledFileMapper.selectById(100L)).thenReturn(published);
        when(controlledFileMapper.selectById(99L)).thenReturn(previous);
        when(relatedFileService.listReverseCurrentActiveRelations(1L,10L)).thenReturn(List.of(
                relation(702L,200L,98L,10L), relation(703L,300L,99L,10L)));
        when(masterMapper.selectBatchIds(Set.of(20L,30L))).thenReturn(List.of(
                master(20L,200L,"A"), master(30L,300L,"C")));
        // Both dependent files belong to one compiler: one delivery candidate, two reasons.
        when(controlledFileMapper.selectBatchIds(Set.of(200L,300L))).thenReturn(List.of(
                relatedActive(200L,20L,3L,"A/1"), relatedActive(300L,30L,3L,"C/1")));
        when(adminUserApi.getUserList(Set.of(3L))).thenReturn(List.of(user(3L,"编制人",11L)));
        when(deptApi.getDeptList(Set.of(11L))).thenReturn(List.of(dept(11L,"编制部门")));
        var event = new DccControlledFileLifecycleEvent("activated-100","ACTIVATED",1L,10L,100L,99L,
                "A/3","revision-round",published.getActivatedTime());
        service.recordActivatedRevision(event);
        service.recordActivatedRevision(event);
        var candidates=ArgumentCaptor.forClass(DccPublicationNotificationCandidateDO.class);
        verify(candidateMapper,times(1)).insert(candidates.capture());
        assertEquals(3L,candidates.getValue().getUserId());
        var reasons=ArgumentCaptor.forClass(DccPublicationNotificationCandidateReasonDO.class);
        verify(candidateReasonMapper,times(2)).insert(reasons.capture());
        assertTrue(reasons.getAllValues().stream().allMatch(row->"RELATED_FILE_OWNER".equals(row.getReasonType())));
        verify(publicationNotificationService,times(1)).materializeForPublicationBatch(900L);
        verifyNoInteractions(visibilityRuleMapper,visibilityUserMapper,assignmentScopeService,
                viewMatrixRuleMapper,viewMatrixAccessService,impactAssessmentService,distributionMapper);
        verify(relatedFileService,never()).listForwardRelations(any());
    }

    @Test
    void activationMaterializesSignedSupplementOwnerUsingExactRoundProjection() {
        var published=publishedB1();published.setStatus("ACTIVE");published.setRevisionChangeType("PARTIAL");
        published.setControlledTime(published.getPublishedTime());published.setActivatedTime(published.getPublishedTime());published.setProcessInstanceId("revision-round");
        when(controlledFileMapper.selectById(100L)).thenReturn(published);
        when(relatedFileService.listReverseCurrentActiveRelations(1L,10L)).thenReturn(List.of());
        var selection=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Selection(10L,98L,40L,"DOC-100","旧B","A/1","SUPPLEMENT",false);
        when(relationStages.findEffectiveReverseSelections(1L,10L)).thenReturn(List.of(new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService.EffectiveReverseSelection(88L,200L,selection)));
        when(controlledFileMapper.selectBatchIds(Set.of(200L))).thenReturn(List.of(relatedActive(200L,20L,3L,"A/1")));
        when(masterMapper.selectBatchIds(Set.of(20L))).thenReturn(List.of(master(20L,200L,"A")));
        when(adminUserApi.getUserList(Set.of(3L))).thenReturn(List.of(user(3L,"编制人",11L)));
        when(deptApi.getDeptList(Set.of(11L))).thenReturn(List.of(dept(11L,"编制部门")));
        service.recordActivatedRevision(new DccControlledFileLifecycleEvent("activated","ACTIVATED",1L,10L,100L,null,"B/1","revision-round",published.getActivatedTime()));
        var candidate=ArgumentCaptor.forClass(DccPublicationNotificationCandidateDO.class);verify(candidateMapper).insert(candidate.capture());assertEquals(3L,candidate.getValue().getUserId());
        var direction=ArgumentCaptor.forClass(DccPublicationRelationDirectionSnapshotDO.class);verify(relationDirectionMapper).insert(direction.capture());assertEquals("SIGNED_ROUND",direction.getValue().getRelationSourceSnapshot());assertEquals(88L,direction.getValue().getSourceRelationId());
        verify(publicationNotificationService).materializeForPublicationBatch(900L);
        verifyNoInteractions(assignmentScopeService,impactAssessmentService);
    }

    @Test
    void replacementWithoutPreviouslyActiveVersionStillCreatesEffectiveRevisionNotificationBatch() {
        var file=publishedB1();file.setRevisionChangeType("REPLACEMENT");file.setStatus("ACTIVE");
        file.setControlledTime(file.getPublishedTime());file.setActivatedTime(file.getPublishedTime().plusDays(1));
        file.setProcessInstanceId("revision-round");
        when(controlledFileMapper.selectById(100L)).thenReturn(file);
        when(relatedFileService.listReverseCurrentActiveRelations(1L,10L)).thenReturn(List.of());
        service.recordActivatedRevision(new DccControlledFileLifecycleEvent("activated","ACTIVATED",1L,
                10L,100L,null,"B/1","revision-round",file.getActivatedTime()));
        verify(batchMapper).insertOrKeepExisting(any());
        verify(publicationNotificationService).materializeForPublicationBatch(900L);
        verifyNoInteractions(assignmentScopeService,impactAssessmentService);
    }

    @Test
    void activationRequiresActualTenantRoundAndEffectiveTime() {
        var file=publishedB1();file.setRevisionChangeType("REPLACEMENT");
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");file.setControlledTime(file.getPublishedTime());
        file.setProcessInstanceId("revision-round");
        when(controlledFileMapper.selectById(100L)).thenReturn(file);
        var event=new DccControlledFileLifecycleEvent("activated","ACTIVATED",1L,10L,100L,99L,
                "B/1","revision-round",file.getPublishedTime().plusDays(1));
        assertThrows(IllegalStateException.class,()->service.recordActivatedRevision(event));
        assertThrows(IllegalArgumentException.class,()->service.recordActivatedRevision(
                new DccControlledFileLifecycleEvent("other","ACTIVATED",2L,10L,100L,99L,"B/1","revision-round",event.occurredAt())));
        file.setStatus("ACTIVE");file.setActivatedTime(event.occurredAt());
        assertThrows(IllegalStateException.class,()->service.recordActivatedRevision(
                new DccControlledFileLifecycleEvent("other","ACTIVATED",1L,10L,100L,99L,"B/1","foreign-round",event.occurredAt())));
        verifyNoInteractions(batchMapper,publicationNotificationService);
    }

    @Test
    void checkinWorkflowHasNoPublicationFollowupDependency() {
        assertTrue(java.util.Arrays.stream(DccControlledFileQueryServiceImpl.class.getDeclaredFields())
                .noneMatch(field -> DccPublicationFollowupService.class.equals(field.getType())));
    }

    private <T> void assignGeneratedId(Object mapper, Class<T> type) {
        if (mapper instanceof DccPublicationVisibilityRuleSnapshotMapper typedMapper) {
            lenient().doAnswer(invocation -> {
                ((DccPublicationVisibilityRuleSnapshotDO) invocation.getArgument(0)).setId(childId.getAndIncrement());
                return 1;
            }).when(typedMapper).insert(any(DccPublicationVisibilityRuleSnapshotDO.class));
        } else if (mapper instanceof DccPublicationNotificationCandidateMapper typedMapper) {
            lenient().doAnswer(invocation -> {
                ((DccPublicationNotificationCandidateDO) invocation.getArgument(0)).setId(childId.getAndIncrement());
                return 1;
            }).when(typedMapper).insert(any(DccPublicationNotificationCandidateDO.class));
        } else if (mapper instanceof DccPublicationRelationSnapshotMapper typedMapper) {
            lenient().doAnswer(invocation -> {
                ((DccPublicationRelationSnapshotDO) invocation.getArgument(0)).setId(childId.getAndIncrement());
                return 1;
            }).when(typedMapper).insert(any(DccPublicationRelationSnapshotDO.class));
        } else {
            throw new IllegalArgumentException("Unsupported mapper fixture: " + type.getName());
        }
    }

    private DccControlledFileDO publishedB1() {
        return DccControlledFileDO.builder()
                .id(100L).tenantId(1L).masterId(10L).dccProjectCodeId(40L).categoryId(20L).directoryId(30L)
                .fileTypeTaxonomyId(50L).fileNumber("DOC-100").fileName("设计规范")
                .versionNo("B/1").revisionCode("B").iterationNo(1).requesterId(1L)
                .publishedTime(java.time.LocalDateTime.of(2026, 9, 7, 11, 0)).build();
    }

    private DccControlledFileDO activeA1() {
        return DccControlledFileDO.builder()
                .id(98L).masterId(10L).versionNo("A/1").revisionCode("A").iterationNo(1).build();
    }

    private DccControlledFileDO activeA2() {
        return DccControlledFileDO.builder()
                .id(99L).masterId(10L).versionNo("A/2").revisionCode("A").iterationNo(2).build();
    }

    private DccControlledFileRelatedFileDO relation(Long id, Long sourceFileId, Long targetFileId,
                                                    Long targetMasterId) {
        return DccControlledFileRelatedFileDO.builder().id(id).controlledFileId(sourceFileId)
                .relatedControlledFileId(targetFileId).relatedMasterId(targetMasterId)
                .projectCodeId(40L).relationSource("UPLOAD").build();
    }

    private DccControlledFileMasterDO master(Long id, Long activeFileId, String number) {
        return DccControlledFileMasterDO.builder().id(id).currentActiveControlledFileId(activeFileId)
                .fileNumber(number).fileName("相关文件" + id).build();
    }

    private DccControlledFileDO relatedActive(Long id, Long masterId, Long requesterId, String version) {
        return DccControlledFileDO.builder().id(id).masterId(masterId).fileNumber("REL-" + masterId)
                .fileName("相关文件" + masterId).versionNo(version).requesterId(requesterId).build();
    }

    private AdminUserRespDTO user(Long id, String name, Long deptId) {
        return new AdminUserRespDTO().setId(id).setNickname(name).setDeptId(deptId).setStatus(0);
    }

    private DeptRespDTO dept(Long id, String name) {
        return new DeptRespDTO().setId(id).setName(name).setStatus(0);
    }
}
