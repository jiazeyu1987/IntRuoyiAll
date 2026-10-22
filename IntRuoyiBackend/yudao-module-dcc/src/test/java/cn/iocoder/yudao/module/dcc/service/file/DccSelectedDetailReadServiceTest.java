package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccSelectedDetailReadServiceTest {
    final DccControlledFileMapper files = mock(DccControlledFileMapper.class);
    final DccControlledFileQueryService query = mock(DccControlledFileQueryService.class);
    final DccControlledFileDetailAuthorizationGuard guard = mock(DccControlledFileDetailAuthorizationGuard.class);
    final DccOfflineTrainingRecordService offlineTraining = mock(DccOfflineTrainingRecordService.class);
    final DccSelectedDetailReadService reader = new DccSelectedDetailReadService();
    final cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper rounds =
            mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper.class);
    final cn.iocoder.yudao.module.bpm.service.task.BpmTaskService tasks = mock(cn.iocoder.yudao.module.bpm.service.task.BpmTaskService.class);
    final Long id = 9007199254740993L;
    final DccControlledFileBodyReadContext context = new DccControlledFileBodyReadContext(
            DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY, id.toString(), "A/1", "REVISION", "old-round", "old-task");
    DccControlledFileDO file;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(31L);
        ReflectionTestUtils.setField(reader,"files",files);
        ReflectionTestUtils.setField(reader,"query",query);
        ReflectionTestUtils.setField(reader,"guard",guard);
        ReflectionTestUtils.setField(reader,"rounds",rounds);
        ReflectionTestUtils.setField(reader,"tasks",tasks);
        ReflectionTestUtils.setField(reader,"offlineTraining",offlineTraining);
        file = DccControlledFileDO.builder().id(id).tenantId(31L).versionNo("A/1").status("SUPERSEDED").build();
        when(files.selectById(id)).thenReturn(file);
        var response = new DccControlledFileRespVO(); response.setId(id); response.setVersionNo("A/1");
        when(query.getControlledFile(eq(99L),eq(id),any(DccControlledFileBodyReadContext.class))).thenReturn(response);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void exactHistoryIsAuthorizedBeforeReadingAnyDetailProjection() {
        var result=reader.read(99L,id,context); assertEquals(id,result.getId());
        var order=inOrder(guard,query); order.verify(guard).requireBodyRead(99L,file,context);
        order.verify(query).getControlledFile(99L,id,context);
    }
    @Test void deniedHistoryNeverReadsAttachmentsOrOtherDetailData() {
        doThrow(new IllegalArgumentException("denied exact application")).when(guard).requireBodyRead(99L,file,context);
        assertThrows(IllegalArgumentException.class,()->reader.read(99L,id,context)); verifyNoInteractions(query);
    }
    @Test void foreignOrChangedVersionFailsBeforeGuardAndProjection() {
        file.setTenantId(32L);
        assertThrows(RuntimeException.class,()->reader.read(99L,id,context)); verifyNoInteractions(guard,query);
        file.setTenantId(31L); file.setVersionNo("B/1");
        assertThrows(RuntimeException.class,()->reader.read(99L,id,context)); verifyNoInteractions(guard,query);
    }
    @Test void projectionCannotReplaceSelectedVersionWithCurrentVersion() {
        var response=new DccControlledFileRespVO(); response.setId(id+1); response.setVersionNo("B/1");
        when(query.getControlledFile(99L,id,context)).thenReturn(response);
        assertThrows(RuntimeException.class,()->reader.read(99L,id,context));
    }
    private DccApplicationRoundSummary round(String bpm) {
        var row=new DccApplicationRoundSummary(); row.setControlledFileId(id);row.setApplicationType("REVISION");
        row.setAttributeRound(1);row.setBpmRound(bpm);return row;
    }
    @Test void explicitRouteReadsOnlyItsFormalRoundAndRealServerTask() {
        when(rounds.selectApplicationRounds(31L,null,id)).thenReturn(java.util.List.of(round("old-round"),round("new-round")));
        var task=mock(org.flowable.task.api.Task.class); when(tasks.getTask("old-task")).thenReturn(task);
        reader.readApplication(99L,id,"old-round","old-task","REVISION");
        verify(guard).requireBodyRead(99L,file,new DccControlledFileBodyReadContext(
                DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,id.toString(),"A/1","REVISION","old-round","old-task"));
    }
    @Test void missingOrAmbiguousOrConflictingMappingCannotReadAnyProjection() {
        when(rounds.selectApplicationRounds(31L,null,id)).thenReturn(java.util.List.of(round("new-round")));
        assertThrows(RuntimeException.class,()->reader.readApplication(99L,id,"old-round","old-task",null));
        when(rounds.selectApplicationRounds(31L,null,id)).thenReturn(java.util.List.of(round("old-round"),round("old-round")));
        assertThrows(RuntimeException.class,()->reader.readApplication(99L,id,"old-round","old-task",null));
        when(rounds.selectApplicationRounds(31L,null,id)).thenReturn(java.util.List.of(round("old-round")));
        assertThrows(RuntimeException.class,()->reader.readApplication(99L,id,"old-round","old-task","UPLOAD"));
        verifyNoInteractions(guard,query,tasks);
    }
    @Test void selectedApplicationProjectionRetainsOnlyItsSignaturesWithoutChangingVersionStatus() {
        var old=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO();old.setProcessInstanceId("old-round");
        var other=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO();other.setProcessInstanceId("new-round");
        var response=new DccControlledFileRespVO();response.setId(id);response.setVersionNo("A/1");response.setStatus("SUPERSEDED");
        response.setSignatureSummaries(java.util.List.of(old,other));when(query.getControlledFile(99L,id,context)).thenReturn(response);
        var result=reader.read(99L,id,context);assertEquals(java.util.List.of(old),result.getSignatureSummaries());
        assertEquals("SUPERSEDED",result.getStatus());assertEquals("A/1",result.getVersionNo());
    }

    private DccControlledFileRespVO waitingTrainingApplication() {
        file.setStatus(DccOfflineTrainingRecordService.STATUS);
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setProcessInstanceId("training-round");
        file.setNeedTraining(true);
        file.setRequesterId(88L);
        file.setFileOwnerUserId(99L);
        file.setFileOwnerSignatureId(860L);
        file.setFileOwnerApprovalTaskId("owner-approval-task");
        file.setFileOwnerProcessInstanceId("training-round");
        file.setFileOwnerSelectedTime(java.time.LocalDateTime.of(2026,10,18,5,24));
        var mapping=round("training-round");mapping.setApplicationType("UPLOAD");
        when(rounds.selectApplicationRounds(31L,null,id)).thenReturn(java.util.List.of(mapping));
        var response=new DccControlledFileRespVO();response.setId(id);response.setVersionNo("A/1");
        response.setProcessInstanceId("training-round");
        when(query.getControlledFile(99L,id)).thenReturn(response);
        when(offlineTraining.canReadProgress(99L,file)).thenReturn(true);
        return response;
    }

    @Test void waitingTrainingOwnerReadsBoundApplicationMetadataInsteadOfApprovalHistoryBody() {
        var response=waitingTrainingApplication();
        var history=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                id.toString(),"A/1","UPLOAD","training-round",null);
        doThrow(new IllegalArgumentException("training owner is not historical approver"))
                .when(guard).requireBodyRead(99L,file,history);

        var result=assertDoesNotThrow(()->reader.readApplication(99L,id,"training-round",null,"UPLOAD"));
        assertSame(response,result);
        verify(query).getControlledFile(99L,id);
        verify(guard,never()).requireBodyRead(any(),any(),any());
        verifyNoInteractions(tasks);
    }

    @Test void originalUploaderStillReadsTheSameBoundTrainingMetadata() {
        var response=waitingTrainingApplication();
        when(offlineTraining.canReadProgress(88L,file)).thenReturn(true);
        when(query.getControlledFile(88L,id)).thenReturn(response);

        assertSame(response,assertDoesNotThrow(()->reader.readApplication(88L,id,"training-round",null,null)));
        verify(offlineTraining).canReadProgress(88L,file);
        verify(query).getControlledFile(88L,id);
        verifyNoInteractions(guard,tasks);
    }

    @Test void trainingMetadataKeepsOnlyBoundRoundSignaturesAndCurrentRoundIdentity() {
        var response=waitingTrainingApplication();
        var selected=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO();
        selected.setProcessInstanceId("training-round");
        var foreign=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSignatureSummaryRespVO();
        foreign.setProcessInstanceId("another-round");
        response.setSignatureSummaries(java.util.List.of(selected,foreign));

        var result=reader.readApplication(99L,id,"training-round",null,"UPLOAD");
        assertEquals("training-round",result.getProcessInstanceId());
        assertEquals(java.util.List.of(selected),result.getSignatureSummaries());
        assertEquals(id,result.getId());assertEquals("A/1",result.getVersionNo());
        assertEquals(DccOfflineTrainingRecordService.STATUS,file.getStatus());
    }

    @Test void actorWithoutTrainingProgressDutyCannotUseMetadataBranch() {
        waitingTrainingApplication();
        var history=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                id.toString(),"A/1","UPLOAD","training-round",null);
        doThrow(new IllegalArgumentException("not a training participant")).when(guard).requireBodyRead(77L,file,history);

        assertThrows(IllegalArgumentException.class,()->reader.readApplication(77L,id,"training-round",null,"UPLOAD"));
        verify(offlineTraining).canReadProgress(77L,file);
        verify(query,never()).getControlledFile(anyLong(),anyLong());
    }

    @Test void formallyMappedOldRoundDoesNotInheritCurrentTrainingMetadataAccess() {
        waitingTrainingApplication();
        var old=round("old-round");old.setApplicationType("UPLOAD");
        when(rounds.selectApplicationRounds(31L,null,id)).thenReturn(java.util.List.of(old));
        var history=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                id.toString(),"A/1","UPLOAD","old-round",null);
        doThrow(new IllegalArgumentException("not the old applicant")).when(guard).requireBodyRead(99L,file,history);

        assertThrows(IllegalArgumentException.class,()->reader.readApplication(99L,id,"old-round",null,"UPLOAD"));
        verifyNoInteractions(offlineTraining);
        verify(query,never()).getControlledFile(anyLong(),anyLong());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void explicitTaskRetainsItsStrictTaskOrHistoryGuard(boolean live) {
        waitingTrainingApplication();
        if(live)when(tasks.getTask("explicit-task")).thenReturn(mock(org.flowable.task.api.Task.class));
        var explicit=new DccControlledFileBodyReadContext(live?DccControlledFileBodyReadContext.Intent.APPROVAL_TASK
                :DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,id.toString(),"A/1","UPLOAD","training-round","explicit-task");
        doThrow(new IllegalArgumentException("not the exact task actor")).when(guard).requireBodyRead(99L,file,explicit);

        assertThrows(IllegalArgumentException.class,()->reader.readApplication(99L,id,"training-round","explicit-task","UPLOAD"));
        verify(guard).requireBodyRead(99L,file,explicit);
        verifyNoInteractions(offlineTraining);
        verify(query,never()).getControlledFile(anyLong(),anyLong());
    }

    @Test void metadataAdmissionNeverBypassesAnExplicitBodyReadDenial() {
        var response=waitingTrainingApplication();
        assertSame(response,reader.readApplication(99L,id,"training-round",null,"UPLOAD"));
        for(var intent:java.util.List.of(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                DccControlledFileBodyReadContext.Intent.TRAINING_APPLICATION)) {
            var body=new DccControlledFileBodyReadContext(intent,id.toString(),"A/1","UPLOAD","training-round",null);
            doThrow(new IllegalArgumentException("body separately denied")).when(guard).requireBodyRead(99L,file,body);
            assertThrows(IllegalArgumentException.class,()->reader.read(99L,id,body));
            verify(guard).requireBodyRead(99L,file,body);
        }
        verify(query,never()).getControlledFile(anyLong(),anyLong(),any(DccControlledFileBodyReadContext.class));
    }

    @ParameterizedTest @ValueSource(strings={"FILE","VERSION","ROUND"})
    void metadataProjectionCannotSubstituteAnotherFileVersionOrRound(String changed) {
        var response=waitingTrainingApplication();
        switch(changed) {
            case "FILE" -> response.setId(id+1);
            case "VERSION" -> response.setVersionNo("B/1");
            case "ROUND" -> response.setProcessInstanceId("another-round");
        }
        assertThrows(IllegalStateException.class,()->reader.readApplication(99L,id,"training-round",null,"UPLOAD"));
        verifyNoInteractions(guard);
    }

    @ParameterizedTest @ValueSource(strings={"ACTIVE","NON_NATIVE"})
    void onlyCurrentNativeWaitingTrainingCanUseMetadataProjection(String changed) {
        waitingTrainingApplication();
        if("ACTIVE".equals(changed))file.setStatus("ACTIVE");else file.setProcessDefinitionKey("legacy-process");
        var history=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                id.toString(),"A/1","UPLOAD","training-round",null);
        doThrow(new IllegalArgumentException("strict historical body gate")).when(guard).requireBodyRead(99L,file,history);
        assertThrows(IllegalArgumentException.class,()->reader.readApplication(99L,id,"training-round",null,"UPLOAD"));
        verifyNoInteractions(offlineTraining);
        verify(query,never()).getControlledFile(anyLong(),anyLong());
    }
}
