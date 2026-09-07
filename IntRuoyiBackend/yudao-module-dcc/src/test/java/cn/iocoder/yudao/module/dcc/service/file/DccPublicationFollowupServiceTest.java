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
    @Mock private DccControlledFileMasterMapper masterMapper;
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private DeptApi deptApi;

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
        when(adminUserApi.getUserList(Set.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
                user(1L, "责任人", 10L), user(2L, "分发人", 11L), user(3L, "关联人Y", 12L),
                user(4L, "关联人Z", 13L)));
        when(deptApi.getDeptList(Set.of(10L, 11L, 12L, 13L))).thenReturn(List.of(
                dept(10L, "质量部"), dept(11L, "生产部"), dept(12L, "研发一部"),
                dept(13L, "研发二部")));

        service.recordPublishedRevision(published, DccControlledFileDO.builder().id(99L).build());

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
        verify(candidateMapper, times(4)).insert(candidateCaptor.capture());
        assertEquals(Set.of(1L, 2L, 3L, 4L), candidateCaptor.getAllValues().stream()
                .map(DccPublicationNotificationCandidateDO::getUserId).collect(java.util.stream.Collectors.toSet()));
        verify(candidateReasonMapper, times(5)).insert(any(DccPublicationNotificationCandidateReasonDO.class));

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

        service.recordPublishedRevision(published, null);
        service.recordPublishedRevision(published, null);

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
                () -> service.recordPublishedRevision(published, DccControlledFileDO.builder().id(99L).build()));

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
                () -> service.recordPublishedRevision(published, null));

        assertEquals("snapshot insert failed", error.getMessage());
        verifyNoInteractions(candidateMapper, candidateReasonMapper, relationSnapshotMapper, relationDirectionMapper);
    }

    @Test
    void recordPublishedRevision_batchInsertFailurePropagatesWithoutChildSnapshots() {
        DccControlledFileDO published = publishedB1();
        doThrow(new IllegalStateException("batch insert failed"))
                .when(batchMapper).insertOrKeepExisting(any(DccPublicationFollowupBatchDO.class));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.recordPublishedRevision(published, null));

        assertEquals("batch insert failed", error.getMessage());
        verifyNoInteractions(visibilityRuleMapper, visibilityUserMapper, candidateMapper, candidateReasonMapper,
                relationSnapshotMapper, relationDirectionMapper);
    }

    @Test
    void recordPublishedRevision_publishedB2StillCreatesFollowupBatch() {
        DccControlledFileDO publishedB2 = publishedB1();
        publishedB2.setVersionNo("B/2");
        publishedB2.setIterationNo(2);
        when(viewMatrixRuleMapper.selectActiveListByCategoryId(20L)).thenReturn(List.of());
        when(distributionMapper.selectListByControlledFileId(100L)).thenReturn(List.of());
        when(relatedFileService.listForwardRelations(100L)).thenReturn(List.of());
        when(relatedFileService.listReverseCurrentActiveRelations(1L, 10L)).thenReturn(List.of());
        when(assignmentScopeService.filterBusinessVisibleUserIds(Set.of(1L), 100L)).thenReturn(Set.of(1L));
        when(adminUserApi.getUserList(Set.of(1L))).thenReturn(List.of(user(1L, "责任人", 10L)));
        when(deptApi.getDeptList(Set.of(10L))).thenReturn(List.of(dept(10L, "质量部")));

        service.recordPublishedRevision(publishedB2, null);

        verify(batchMapper).insertOrKeepExisting(any(DccPublicationFollowupBatchDO.class));
        verify(visibilityRuleMapper).insert(any(DccPublicationVisibilityRuleSnapshotDO.class));
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
