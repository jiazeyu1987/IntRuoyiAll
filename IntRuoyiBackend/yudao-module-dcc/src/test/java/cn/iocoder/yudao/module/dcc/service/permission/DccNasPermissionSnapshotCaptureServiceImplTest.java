package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclAceDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDescriptorDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclAceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDescriptorMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclDirectorySnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.permission.DccNasAclSnapshotMapper;
import cn.iocoder.yudao.module.infra.service.file.NasAclAce;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionSnapshotCaptureServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccNasAclSnapshotMapper snapshotMapper;
    @Mock
    private DccNasAclDescriptorMapper descriptorMapper;
    @Mock
    private DccNasAclAceMapper aceMapper;
    @Mock
    private DccNasAclDirectorySnapshotMapper directorySnapshotMapper;
    @Mock
    private DccControlledFileNasTransferTaskMapper taskMapper;
    @Mock
    private DccControlledFileNasTransferTaskItemMapper taskItemMapper;
    @Mock
    private NasSettingsService nasSettingsService;

    @InjectMocks
    private DccNasPermissionSnapshotCaptureServiceImpl captureService;

    @Test
    void captureDirectorySnapshot_persistsSnapshotDescriptorAceAndDirectorySnapshot() {
        Long taskId = 10L;
        Long taskItemId = 100L;
        Long dccDirectoryId = 902634L;
        String nasPath = "3.DMR/01.图纸";
        NasAclReadResult acl = sampleAcl(nasPath);
        DccControlledFileNasTransferTaskDO task = DccControlledFileNasTransferTaskDO.builder()
                .id(taskId)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson("[\"3.DMR/01.图纸\"]")
                .status("RUNNING")
                .build();
        DccControlledFileNasTransferTaskItemDO taskItem = DccControlledFileNasTransferTaskItemDO.builder()
                .id(taskItemId)
                .taskId(taskId)
                .itemType("DIRECTORY")
                .nasPath(nasPath)
                .itemName("01.图纸")
                .status("RUNNING")
                .resolvedDirectoryId(dccDirectoryId)
                .build();

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskItemMapper.selectById(taskItemId)).thenReturn(taskItem);
        when(nasSettingsService.getRequiredNasConfig())
                .thenReturn(new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "int", "secret"));
        doAnswer(invocation -> {
            DccNasAclSnapshotDO snapshot = invocation.getArgument(0);
            snapshot.setId(7001L);
            return 1;
        }).when(snapshotMapper).insert(any(DccNasAclSnapshotDO.class));
        doAnswer(invocation -> {
            DccNasAclDescriptorDO descriptor = invocation.getArgument(0);
            descriptor.setId(7101L);
            return 1;
        }).when(descriptorMapper).insert(any(DccNasAclDescriptorDO.class));
        doAnswer(invocation -> {
            DccNasAclAceDO ace = invocation.getArgument(0);
            ace.setId(7201L);
            return 1;
        }).when(aceMapper).insert(any(DccNasAclAceDO.class));
        doAnswer(invocation -> {
            DccNasAclDirectorySnapshotDO directorySnapshot = invocation.getArgument(0);
            directorySnapshot.setId(7301L);
            return 1;
        }).when(directorySnapshotMapper).insert(any(DccNasAclDirectorySnapshotDO.class));

        captureService.captureDirectorySnapshot(taskId, taskItemId, nasPath, dccDirectoryId, acl);

        ArgumentCaptor<DccNasAclSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(DccNasAclSnapshotDO.class);
        ArgumentCaptor<DccNasAclDescriptorDO> descriptorCaptor = ArgumentCaptor.forClass(DccNasAclDescriptorDO.class);
        ArgumentCaptor<DccNasAclAceDO> aceCaptor = ArgumentCaptor.forClass(DccNasAclAceDO.class);
        ArgumentCaptor<DccNasAclDirectorySnapshotDO> directorySnapshotCaptor =
                ArgumentCaptor.forClass(DccNasAclDirectorySnapshotDO.class);

        org.mockito.Mockito.verify(snapshotMapper).insert(snapshotCaptor.capture());
        org.mockito.Mockito.verify(descriptorMapper).insert(descriptorCaptor.capture());
        org.mockito.Mockito.verify(aceMapper).insert(aceCaptor.capture());
        org.mockito.Mockito.verify(directorySnapshotMapper).insert(directorySnapshotCaptor.capture());
        org.mockito.Mockito.verify(snapshotMapper).updateById(snapshotCaptor.capture());

        DccNasAclSnapshotDO snapshot = snapshotCaptor.getValue();
        assertEquals(taskId, snapshot.getTransferTaskId());
        assertEquals("172.30.30.4", snapshot.getServer());
        assertEquals("质量体系文件", snapshot.getShare());
        assertEquals("[\"3.DMR/01.图纸\"]", snapshot.getRootPathsJson());
        assertEquals("RUNNING", snapshot.getStatus());
        assertNotNull(snapshot.getNormalizationVersion());
        assertEquals(1L, snapshot.getTotalDirectoryCount());
        assertEquals(1L, snapshot.getSnapshottedDirectoryCount());
        assertEquals(0L, snapshot.getFailedDirectoryCount());

        DccNasAclDescriptorDO descriptor = descriptorCaptor.getValue();
        assertEquals("S-1-5-21-1000-2000-3000-500", descriptor.getOwnerSid());
        assertEquals("S-1-5-21-1000-2000-3000-513", descriptor.getGroupSid());
        assertEquals(Boolean.TRUE, descriptor.getDaclPresent());
        assertEquals(Boolean.TRUE, descriptor.getDaclProtected());
        assertEquals("SMBJ_SECURITY_DESCRIPTOR_DACL", descriptor.getCaptureCapability());
        assertTrue(descriptor.getNormalizedDescriptorJson().contains("S-1-5-21-1000-2000-3000-1101"));
        assertTrue(descriptor.getNormalizedDescriptorJson().contains("2032127"));

        DccNasAclAceDO ace = aceCaptor.getValue();
        assertEquals(7101L, ace.getDescriptorId());
        assertEquals(0, ace.getAceIndex());
        assertEquals("ACCESS_ALLOWED_ACE_TYPE", ace.getAceType());
        assertEquals(2032127L, ace.getAccessMask());
        assertEquals("S-1-5-21-1000-2000-3000-1101", ace.getTrusteeSid());
        assertFalse(ace.getInherited());
        assertTrue(ace.getRawAceJson().contains("CONTAINER_INHERIT_ACE"));

        DccNasAclDirectorySnapshotDO directorySnapshot = directorySnapshotCaptor.getValue();
        assertEquals(7001L, directorySnapshot.getSnapshotId());
        assertEquals(taskId, directorySnapshot.getTransferTaskId());
        assertEquals(taskItemId, directorySnapshot.getTransferTaskItemId());
        assertEquals(dccDirectoryId, directorySnapshot.getDccDirectoryId());
        assertEquals(nasPath, directorySnapshot.getNasPath());
        assertNotNull(directorySnapshot.getPathHash());
        assertEquals("01.图纸", directorySnapshot.getItemName());
        assertEquals(7101L, directorySnapshot.getDescriptorId());
        assertEquals("SUCCESS", directorySnapshot.getCollectStatus());
    }

    @Test
    void captureDirectorySnapshot_reusesDescriptorWhenDifferentPathsHaveSameAclContent() {
        Long taskId = 10L;
        DccControlledFileNasTransferTaskDO task = sampleTask(taskId, "[\"3.DMR/01.图纸\",\"3.DMR/02.图纸\"]");
        DccControlledFileNasTransferTaskItemDO firstItem = sampleTaskItem(100L, taskId, "3.DMR/01.图纸", "01.图纸", 902634L);
        DccControlledFileNasTransferTaskItemDO secondItem = sampleTaskItem(101L, taskId, "3.DMR/02.图纸", "02.图纸", 902635L);
        DccNasAclSnapshotDO existingSnapshot = DccNasAclSnapshotDO.builder()
                .id(7001L)
                .transferTaskId(taskId)
                .snapshotKey("snapshot-key")
                .status("RUNNING")
                .normalizationVersion("NAS_ACL_V1")
                .totalDirectoryCount(0L)
                .snapshottedDirectoryCount(0L)
                .failedDirectoryCount(0L)
                .build();
        DccNasAclDescriptorDO reusedDescriptor = DccNasAclDescriptorDO.builder()
                .id(7101L)
                .descriptorHash("same-descriptor-hash")
                .ownerSid("S-1-5-21-1000-2000-3000-500")
                .groupSid("S-1-5-21-1000-2000-3000-513")
                .build();

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskItemMapper.selectById(100L)).thenReturn(firstItem);
        when(taskItemMapper.selectById(101L)).thenReturn(secondItem);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(sampleConfig());
        when(snapshotMapper.selectBySnapshotKey(any())).thenReturn(existingSnapshot);
        when(descriptorMapper.selectByDescriptorHash(any())).thenReturn(null, reusedDescriptor);
        doAnswer(invocation -> {
            DccNasAclDescriptorDO descriptor = invocation.getArgument(0);
            descriptor.setId(7101L);
            return 1;
        }).when(descriptorMapper).insert(any(DccNasAclDescriptorDO.class));

        captureService.captureDirectorySnapshot(taskId, 100L, "3.DMR/01.图纸", 902634L, sampleAcl("3.DMR/01.图纸"));
        captureService.captureDirectorySnapshot(taskId, 101L, "3.DMR/02.图纸", 902635L, sampleAcl("3.DMR/02.图纸"));

        ArgumentCaptor<String> descriptorHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(descriptorMapper, times(2)).selectByDescriptorHash(descriptorHashCaptor.capture());
        assertEquals(descriptorHashCaptor.getAllValues().get(0), descriptorHashCaptor.getAllValues().get(1));
        verify(descriptorMapper, times(1)).insert(any(DccNasAclDescriptorDO.class));
        verify(aceMapper, times(1)).insert(any(DccNasAclAceDO.class));
        verify(directorySnapshotMapper, times(2)).insert(any(DccNasAclDirectorySnapshotDO.class));
    }

    @Test
    void captureDirectorySnapshot_rejectsAclPathMismatchBeforeAnyInsertOrUpdate() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> captureService.captureDirectorySnapshot(10L, 100L,
                        "3.DMR/01.图纸", 902634L, sampleAcl("3.DMR/02.图纸")));

        assertTrue(ex.getMessage().contains("nasPath must match acl.path"));
        verifyNoPersistenceWrites();
    }

    @Test
    void captureDirectorySnapshot_rejectsTaskItemPathMismatchBeforeConfigLookupOrAnyInsertOrUpdate() {
        Long taskId = 10L;
        Long taskItemId = 100L;
        DccControlledFileNasTransferTaskDO task = sampleTask(taskId, "[\"3.DMR/01.图纸\"]");
        DccControlledFileNasTransferTaskItemDO taskItem =
                sampleTaskItem(taskItemId, taskId, "3.DMR/02.图纸", "02.图纸", 902635L);

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskItemMapper.selectById(taskItemId)).thenReturn(taskItem);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> captureService.captureDirectorySnapshot(taskId, taskItemId,
                        "3.DMR/01.图纸", 902634L, sampleAcl("3.DMR/01.图纸")));

        assertTrue(ex.getMessage().contains("taskItem.nasPath must match nasPath"));
        verify(nasSettingsService, never()).getRequiredNasConfig();
        verifyNoPersistenceWrites();
    }

    @Test
    void captureDirectorySnapshot_rejectsPathTraversalBeforeAnyInsertOrUpdate() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> captureService.captureDirectorySnapshot(10L, 100L,
                        "3.DMR/../01.图纸", 902634L, sampleAcl("3.DMR/../01.图纸")));

        assertTrue(ex.getMessage().contains("nasPath contains traversal"));
        verifyNoPersistenceWrites();
    }

    @Test
    void captureDirectorySnapshot_usesScopedCaseInsensitivePathKeyAndPreservesChineseNasPath() {
        Long taskId = 10L;
        DccControlledFileNasTransferTaskDO task = sampleTask(taskId, "[\"3.DMR/01.图纸\"]");
        DccControlledFileNasTransferTaskItemDO taskItem = sampleTaskItem(100L, taskId, "3.DMR/01.图纸", "01.图纸", 902634L);
        DccNasAclSnapshotDO existingSnapshot = DccNasAclSnapshotDO.builder()
                .id(7001L)
                .transferTaskId(taskId)
                .status("RUNNING")
                .normalizationVersion("NAS_ACL_V1")
                .totalDirectoryCount(1L)
                .snapshottedDirectoryCount(1L)
                .failedDirectoryCount(0L)
                .build();
        DccNasAclDirectorySnapshotDO existingDirectorySnapshot = DccNasAclDirectorySnapshotDO.builder()
                .id(7301L)
                .snapshotId(7001L)
                .transferTaskId(taskId)
                .transferTaskItemId(100L)
                .dccDirectoryId(902634L)
                .nasPath("3.DMR/01.图纸")
                .pathHash("existing")
                .itemName("01.图纸")
                .descriptorId(7101L)
                .collectStatus("SUCCESS")
                .build();
        DccNasAclDescriptorDO descriptor = DccNasAclDescriptorDO.builder().id(7101L).build();

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskItemMapper.selectById(100L)).thenReturn(taskItem);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(sampleConfig());
        when(snapshotMapper.selectBySnapshotKey(any())).thenReturn(existingSnapshot);
        when(descriptorMapper.selectByDescriptorHash(any())).thenReturn(descriptor);
        when(directorySnapshotMapper.selectBySnapshotIdAndPathHash(any(), any()))
                .thenReturn(null, existingDirectorySnapshot);

        captureService.captureDirectorySnapshot(taskId, 100L, "3.DMR/01.图纸", 902634L, sampleAcl("3.DMR/01.图纸"));
        captureService.captureDirectorySnapshot(taskId, 100L, "3.dmr/01.图纸", 902634L, sampleAcl("3.DMR/01.图纸"));

        ArgumentCaptor<String> pathHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(directorySnapshotMapper, times(2)).selectBySnapshotIdAndPathHash(org.mockito.ArgumentMatchers.eq(7001L),
                pathHashCaptor.capture());
        assertEquals(pathHashCaptor.getAllValues().get(0), pathHashCaptor.getAllValues().get(1));
        ArgumentCaptor<DccNasAclDirectorySnapshotDO> insertedCaptor =
                ArgumentCaptor.forClass(DccNasAclDirectorySnapshotDO.class);
        verify(directorySnapshotMapper).insert(insertedCaptor.capture());
        assertEquals("3.DMR/01.图纸", insertedCaptor.getValue().getNasPath());
    }

    @Test
    void captureDirectorySnapshot_updatesHeaderCountsAfterIdempotentDirectoryUpdate() {
        Long taskId = 10L;
        DccControlledFileNasTransferTaskDO task = sampleTask(taskId, "[\"3.DMR/01.图纸\"]");
        DccControlledFileNasTransferTaskItemDO taskItem = sampleTaskItem(100L, taskId, "3.DMR/01.图纸", "01.图纸", 902634L);
        DccNasAclSnapshotDO existingSnapshot = DccNasAclSnapshotDO.builder()
                .id(7001L)
                .transferTaskId(taskId)
                .status("RUNNING")
                .normalizationVersion("NAS_ACL_V1")
                .totalDirectoryCount(1L)
                .snapshottedDirectoryCount(1L)
                .failedDirectoryCount(0L)
                .build();
        DccNasAclDescriptorDO descriptor = DccNasAclDescriptorDO.builder().id(7101L).build();
        DccNasAclDirectorySnapshotDO existingDirectorySnapshot = DccNasAclDirectorySnapshotDO.builder()
                .id(7301L)
                .snapshotId(7001L)
                .transferTaskId(taskId)
                .transferTaskItemId(100L)
                .dccDirectoryId(902634L)
                .nasPath("3.DMR/01.图纸")
                .itemName("01.图纸")
                .descriptorId(7101L)
                .collectStatus("SUCCESS")
                .build();

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(taskItemMapper.selectById(100L)).thenReturn(taskItem);
        when(nasSettingsService.getRequiredNasConfig()).thenReturn(sampleConfig());
        when(snapshotMapper.selectBySnapshotKey(any())).thenReturn(existingSnapshot);
        when(descriptorMapper.selectByDescriptorHash(any())).thenReturn(descriptor);
        when(directorySnapshotMapper.selectBySnapshotIdAndPathHash(any(), any())).thenReturn(existingDirectorySnapshot);

        captureService.captureDirectorySnapshot(taskId, 100L, "3.DMR/01.图纸", 902634L, sampleAcl("3.DMR/01.图纸"));

        ArgumentCaptor<DccNasAclSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(DccNasAclSnapshotDO.class);
        InOrder inOrder = org.mockito.Mockito.inOrder(directorySnapshotMapper, snapshotMapper);
        inOrder.verify(directorySnapshotMapper).updateById(any(DccNasAclDirectorySnapshotDO.class));
        inOrder.verify(snapshotMapper).updateById(snapshotCaptor.capture());
        DccNasAclSnapshotDO updatedSnapshot = snapshotCaptor.getValue();
        assertEquals(1L, updatedSnapshot.getTotalDirectoryCount());
        assertEquals(1L, updatedSnapshot.getSnapshottedDirectoryCount());
        assertEquals(0L, updatedSnapshot.getFailedDirectoryCount());
    }

    @Test
    void completeSnapshotForTask_marksSnapshotCapturedWhenAllDirectorySnapshotsSucceeded() {
        Long taskId = 10L;
        DccControlledFileNasTransferTaskDO task = sampleTask(taskId, "[\"3.DMR\"]");
        DccNasAclSnapshotDO snapshot = DccNasAclSnapshotDO.builder()
                .id(7001L)
                .transferTaskId(taskId)
                .status("RUNNING")
                .totalDirectoryCount(1L)
                .snapshottedDirectoryCount(1L)
                .failedDirectoryCount(0L)
                .build();
        DccControlledFileNasTransferTaskItemDO firstItem =
                sampleTaskItem(100L, taskId, "3.DMR", "3.DMR", 902634L);
        DccControlledFileNasTransferTaskItemDO secondItem =
                sampleTaskItem(101L, taskId, "3.DMR/01.图纸", "01.图纸", 902635L);
        DccNasAclDirectorySnapshotDO firstDirectorySnapshot = DccNasAclDirectorySnapshotDO.builder()
                .id(7301L)
                .snapshotId(7001L)
                .transferTaskId(taskId)
                .transferTaskItemId(100L)
                .collectStatus("SUCCESS")
                .build();
        DccNasAclDirectorySnapshotDO secondDirectorySnapshot = DccNasAclDirectorySnapshotDO.builder()
                .id(7302L)
                .snapshotId(7001L)
                .transferTaskId(taskId)
                .transferTaskItemId(101L)
                .collectStatus("SUCCESS")
                .build();

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(snapshotMapper.selectBySnapshotKey(any())).thenReturn(snapshot);
        when(taskItemMapper.selectListByTaskId(taskId)).thenReturn(List.of(firstItem, secondItem));
        when(directorySnapshotMapper.selectListBySnapshotId(7001L))
                .thenReturn(List.of(firstDirectorySnapshot, secondDirectorySnapshot));

        captureService.completeSnapshotForTask(taskId);

        ArgumentCaptor<DccNasAclSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(DccNasAclSnapshotDO.class);
        verify(snapshotMapper).updateById(snapshotCaptor.capture());
        DccNasAclSnapshotDO completedSnapshot = snapshotCaptor.getValue();
        assertEquals("CAPTURED", completedSnapshot.getStatus());
        assertEquals(2L, completedSnapshot.getTotalDirectoryCount());
        assertEquals(2L, completedSnapshot.getSnapshottedDirectoryCount());
        assertEquals(0L, completedSnapshot.getFailedDirectoryCount());
        assertNotNull(completedSnapshot.getCompletedAt());
        assertNull(completedSnapshot.getFailureCode());
        assertNull(completedSnapshot.getFailureMessage());
    }

    @Test
    void completeSnapshotForTask_marksSnapshotFailedWhenDirectorySnapshotMissing() {
        Long taskId = 10L;
        DccControlledFileNasTransferTaskDO task = sampleTask(taskId, "[\"3.DMR\"]");
        DccNasAclSnapshotDO snapshot = DccNasAclSnapshotDO.builder()
                .id(7001L)
                .transferTaskId(taskId)
                .status("RUNNING")
                .totalDirectoryCount(1L)
                .snapshottedDirectoryCount(1L)
                .failedDirectoryCount(0L)
                .build();
        DccControlledFileNasTransferTaskItemDO firstItem =
                sampleTaskItem(100L, taskId, "3.DMR", "3.DMR", 902634L);
        DccControlledFileNasTransferTaskItemDO secondItem =
                sampleTaskItem(101L, taskId, "3.DMR/01.图纸", "01.图纸", 902635L);
        DccNasAclDirectorySnapshotDO firstDirectorySnapshot = DccNasAclDirectorySnapshotDO.builder()
                .id(7301L)
                .snapshotId(7001L)
                .transferTaskId(taskId)
                .transferTaskItemId(100L)
                .collectStatus("SUCCESS")
                .build();

        when(taskMapper.selectById(taskId)).thenReturn(task);
        when(snapshotMapper.selectBySnapshotKey(any())).thenReturn(snapshot);
        when(taskItemMapper.selectListByTaskId(taskId)).thenReturn(List.of(firstItem, secondItem));
        when(directorySnapshotMapper.selectListBySnapshotId(7001L)).thenReturn(List.of(firstDirectorySnapshot));

        captureService.completeSnapshotForTask(taskId);

        ArgumentCaptor<DccNasAclSnapshotDO> snapshotCaptor = ArgumentCaptor.forClass(DccNasAclSnapshotDO.class);
        verify(snapshotMapper).updateById(snapshotCaptor.capture());
        DccNasAclSnapshotDO failedSnapshot = snapshotCaptor.getValue();
        assertEquals("FAILED", failedSnapshot.getStatus());
        assertEquals(2L, failedSnapshot.getTotalDirectoryCount());
        assertEquals(1L, failedSnapshot.getSnapshottedDirectoryCount());
        assertEquals(1L, failedSnapshot.getFailedDirectoryCount());
        assertEquals("SNAPSHOT_INCOMPLETE", failedSnapshot.getFailureCode());
        assertTrue(failedSnapshot.getFailureMessage().contains("expectedDirectoryCount=2"));
        assertNotNull(failedSnapshot.getCompletedAt());
    }

    private void verifyNoPersistenceWrites() {
        verify(snapshotMapper, never()).insert(any(DccNasAclSnapshotDO.class));
        verify(snapshotMapper, never()).updateById(any(DccNasAclSnapshotDO.class));
        verify(descriptorMapper, never()).insert(any(DccNasAclDescriptorDO.class));
        verify(aceMapper, never()).insert(any(DccNasAclAceDO.class));
        verify(directorySnapshotMapper, never()).insert(any(DccNasAclDirectorySnapshotDO.class));
        verify(directorySnapshotMapper, never()).updateById(any(DccNasAclDirectorySnapshotDO.class));
    }

    private static DccControlledFileNasTransferTaskDO sampleTask(Long taskId, String selectedNasPathsJson) {
        return DccControlledFileNasTransferTaskDO.builder()
                .id(taskId)
                .operatorUserId(99L)
                .templateCategoryId(900250L)
                .effectiveDate(LocalDate.of(2026, 5, 23))
                .selectedNasPathsJson(selectedNasPathsJson)
                .status("RUNNING")
                .build();
    }

    private static DccControlledFileNasTransferTaskItemDO sampleTaskItem(Long taskItemId,
                                                                         Long taskId,
                                                                         String nasPath,
                                                                         String itemName,
                                                                         Long dccDirectoryId) {
        return DccControlledFileNasTransferTaskItemDO.builder()
                .id(taskItemId)
                .taskId(taskId)
                .itemType("DIRECTORY")
                .nasPath(nasPath)
                .itemName(itemName)
                .status("RUNNING")
                .resolvedDirectoryId(dccDirectoryId)
                .build();
    }

    private static NasConnectionConfig sampleConfig() {
        return new NasConnectionConfig("172.30.30.4", 445, "质量体系文件", "", "int", "secret");
    }

    private static NasAclReadResult sampleAcl(String path) {
        return new NasAclReadResult(
                path,
                "S-1-5-21-1000-2000-3000-500",
                "S-1-5-21-1000-2000-3000-513",
                List.of("SE_DACL_PRESENT", "SE_DACL_PROTECTED"),
                true,
                true,
                List.of(new NasAclAce(
                        0,
                        "ACCESS_ALLOWED_ACE_TYPE",
                        List.of("CONTAINER_INHERIT_ACE", "OBJECT_INHERIT_ACE"),
                        2032127L,
                        "S-1-5-21-1000-2000-3000-1101",
                        false
                ))
        );
    }
}
