package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchLookupServiceTest {
    @Mock MesProEdhrTraceIdentityMapper identityMapper;
    @Mock MesProEdhrBatchExecutionOriginMapper originMapper;
    @Mock List<MesProEdhrReverseTraceSourceAdapter> adapters;
    @Mock MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock MesProProcessPoolEventMapper eventMapper;
    @Mock MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock MesPqcProcessInspectionAggregateDetailMapper pqcDetailMapper;
    @Mock MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    @Mock MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock MesProcessPoolActiveOrderPickListBindingItemMapper materialMapper;
    @Mock MesProEdhrReleaseTransactionMapper releaseMapper;
    @Mock MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @InjectMocks MesProEdhrBatchLookupService service;

    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void materialAndLotMustBelongToSameSavedRow() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setTraceMaterial("MAT-A"); request.setTraceMaterialLot("LOT-B");
        List<CatalogItem> facts = List.of(material("MAT-A", "LOT-A"), material("MAT-B", "LOT-B"));
        assertFalse(matches(facts, request));
        request.setTraceMaterialLot("LOT-A"); assertTrue(matches(facts, request));
        request.setTraceMaterial(null); assertTrue(matches(facts, request));
        request.setTraceMaterialLot("' OR 1=1 --"); assertFalse(matches(facts, request));
    }

    @Test void employeeAndSystemUserIdsAreSeparateNamespaces() {
        var request = new EdhrBatchExecutionPageReqVO(); request.setTracePerson("张三");
        var user = new CatalogItem().setCategory(Category.PERSON).setEvidenceKey("PERSON:SYSTEM_USER:11").setSavedValue("11");
        assertFalse(MesProEdhrBatchLookupService.matches(List.of(user), request, Set.of(11L), Set.of(), Set.of()));
        assertTrue(MesProEdhrBatchLookupService.matches(List.of(user), request, Set.of(), Set.of(11L), Set.of()));
    }

    @Test void deviceIdsAcceptSavedStringAndNumberWithoutCrossingNamespaces() {
        List<CatalogItem> facts = new ArrayList<>();
        MesProEdhrBatchLookupService.addProductionEquipment(facts, "{\"selectedDevices\":[{\"deviceId\":\"31\",\"deviceCode\":\"EQ-1\"}],\"materialDetails\":[{\"selectedDevices\":[{\"deviceId\":32,\"deviceName\":\"物料设备\"}]}]}");
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceEquipment("eq-1");
        assertTrue(matches(facts, request));
        request.setTraceEquipment("物料设备"); assertTrue(matches(facts, request));
        request.setTraceEquipment("主数据设备名");
        assertTrue(MesProEdhrBatchLookupService.matches(facts, request, Set.of(), Set.of(), Set.of(31L)));
        var pqc = new CatalogItem().setCategory(Category.EQUIPMENT).setEvidenceKey("EQUIPMENT:PQC:31").setSavedValue("31");
        assertFalse(MesProEdhrBatchLookupService.matches(List.of(pqc), request, Set.of(), Set.of(), Set.of(31L)));
        assertThrows(IllegalStateException.class, () -> MesProEdhrBatchLookupService.addProductionEquipment(facts, "{\"selectedDevices\":{}}"));
    }

    @Test void businessTimeIsInclusiveAndDoesNotUseBatchCreationTime() {
        var time = LocalDateTime.of(2026,10,20,10,0);
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceBusinessTime(new LocalDateTime[]{time,time});
        assertTrue(request.isTraceBusinessTimeValid());
        assertTrue(matches(List.of(new CatalogItem().setCategory(Category.PERSON).setRecordedAt(time)),request));
        assertFalse(matches(List.of(new CatalogItem().setCategory(Category.PERSON).setRecordedAt(time.minusSeconds(1))),request));
        assertFalse(matches(List.of(),request));
        assertFalse(matches(List.of(new CatalogItem().setCategory(Category.MATERIAL).setRecordedAt(time)), request));
        request.setTraceBusinessTime(new LocalDateTime[]{time,time.minusSeconds(1)}); assertFalse(request.isTraceBusinessTimeValid());
        request.setTraceBusinessTime(new LocalDateTime[]{time}); assertFalse(request.isTraceBusinessTimeValid());
    }

    @Test void completedBatchUsesFormalCatalogAndDoesNotReadLiveMaterial() {
        var batch = batch(1L);
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(origin().setCompletionBackfillReceiptId(99L)));
        var adapter = mock(MesProEdhrReverseTraceSourceAdapter.class);
        when(adapters.stream()).thenAnswer(invocation -> java.util.stream.Stream.of(adapter));
        when(adapter.category()).thenReturn(Category.MATERIAL);
        when(adapter.readCatalogs(List.of(batch), Set.of(Category.MATERIAL))).thenReturn(Map.of(Category.MATERIAL, Map.of(1L, new MesProEdhrReverseTraceSourceAdapter.CatalogResult("v","s","AVAILABLE",null,null,List.of(material("MAT-A","LOT-A"))))));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceMaterial("MAT-A");
        assertEquals(List.of(batch),service.filter(List.of(batch),request));
        verifyNoInteractions(bindingMapper,materialMapper,allocationMapper,eventMapper);
    }

    @Test void incompleteFormalSourceFailsInsteadOfReturningAnEmptySuccess() {
        var batch = batch(1L);
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(origin().setCompletionBackfillReceiptId(99L)));
        var adapter = mock(MesProEdhrReverseTraceSourceAdapter.class);
        when(adapters.stream()).thenAnswer(invocation -> java.util.stream.Stream.of(adapter));
        when(adapter.category()).thenReturn(Category.MATERIAL);
        when(adapter.readCatalogs(List.of(batch), Set.of(Category.MATERIAL))).thenReturn(Map.of(Category.MATERIAL, Map.of(1L, new MesProEdhrReverseTraceSourceAdapter.CatalogResult(null,null,"SOURCE_MISSING","SOURCE_MISSING","正式领料快照缺失",List.of()))));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceMaterial("MAT-A");
        assertThrows(ServiceException.class, () -> service.filter(List.of(batch),request));
    }

    @Test void completedBatchCombinesAllCategoriesInOneWindowAndKeepsAndSemantics() {
        var batch = batch(1L);
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(origin().setCompletionBackfillReceiptId(99L)));
        var personAdapter = mock(MesProEdhrReverseTraceSourceAdapter.class);
        var equipmentAdapter = mock(MesProEdhrReverseTraceSourceAdapter.class);
        var materialAdapter = mock(MesProEdhrReverseTraceSourceAdapter.class);
        when(personAdapter.category()).thenReturn(Category.PERSON);
        when(equipmentAdapter.category()).thenReturn(Category.EQUIPMENT);
        when(materialAdapter.category()).thenReturn(Category.MATERIAL);
        when(adapters.stream()).thenAnswer(invocation -> java.util.stream.Stream.of(personAdapter, equipmentAdapter, materialAdapter));
        var categories = EnumSet.of(Category.PERSON, Category.EQUIPMENT, Category.MATERIAL);
        var catalogs = Map.of(
                Category.PERSON, Map.of(1L, catalog(new CatalogItem().setCategory(Category.PERSON)
                        .setEvidenceKey("PERSON:EMPLOYEE:11").setSavedValue("11"))),
                Category.EQUIPMENT, Map.of(1L, catalog(new CatalogItem().setCategory(Category.EQUIPMENT)
                        .setEvidenceKey("EQUIPMENT:PRODUCTION:12").setSavedValue("12")
                        .setSourceContext(Map.of("deviceName", "EQ-A")).setLabel("EQ-A"))),
                Category.MATERIAL, Map.of(1L, catalog(material("MAT-A", "LOT-A"))));
        when(equipmentAdapter.readCatalogs(List.of(batch), categories)).thenReturn(catalogs);
        when(identityMapper.employeeIds(1L, "张三")).thenReturn(Set.of(11L));
        var request = new EdhrBatchExecutionPageReqVO();
        request.setTracePerson("张三"); request.setTraceEquipment("EQ-A");
        request.setTraceMaterial("MAT-A"); request.setTraceMaterialLot("LOT-A");
        assertEquals(List.of(batch), service.filter(List.of(batch), request));
        request.setTraceMaterialLot("LOT-B");
        assertTrue(service.filter(List.of(batch), request).isEmpty());
        verify(equipmentAdapter, times(2)).readCatalogs(List.of(batch), categories);
        verify(personAdapter, never()).readCatalogs(anyList(), anySet());
        verify(materialAdapter, never()).readCatalogs(anyList(), anySet());
        verifyNoInteractions(bindingMapper, materialMapper, allocationMapper, eventMapper);
    }

    private static MesProEdhrReverseTraceSourceAdapter.CatalogResult catalog(CatalogItem item) {
        return new MesProEdhrReverseTraceSourceAdapter.CatalogResult("v", "s", "AVAILABLE", null, null, List.of(item));
    }

    @Test void liveProductionUsesActiveOrderAllocationsAndIncludesReviewer() {
        var batch = batch(1L);
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(origin()));
        when(identityMapper.employeeIds(1L,"张三")).thenReturn(Set.of(11L));
        var allocation = new MesProcessPoolReportAllocationDO().setActiveOrderId(101L).setWorkOrderId(501L).setEventId(41L).setReviewId(61L);
        allocation.setTenantId(1L);
        when(allocationMapper.selectListByActiveOrderIds(List.of(101L))).thenReturn(List.of(allocation));
        var event = new MesProProcessPoolEventDO().setId(41L).setActualEmployeeId(11L).setEventType("PRODUCTION_SUBMIT");
        event.setTenantId(1L); when(eventMapper.selectBatchIds(List.of(41L))).thenReturn(List.of(event));
        var review = new MesProcessPoolSubmissionReviewDO().setId(61L).setLeaderUserId(21L); review.setTenantId(1L);
        when(reviewMapper.selectBatchIds(Set.of(61L))).thenReturn(List.of(review));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTracePerson("张三");
        assertEquals(List.of(batch), service.filter(List.of(batch),request));
        verify(allocationMapper).selectListByActiveOrderIds(List.of(101L));
        verifyNoInteractions(adapters);
    }

    @Test void integrityConvertsMalformedLiveJsonIntoEvidenceError() {
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(origin()));
        var allocation = new MesProcessPoolReportAllocationDO().setActiveOrderId(101L).setWorkOrderId(501L)
                .setEventId(41L).setReviewId(61L); allocation.setTenantId(1L);
        when(allocationMapper.selectListByActiveOrderIds(List.of(101L))).thenReturn(List.of(allocation));
        var event = new MesProProcessPoolEventDO().setId(41L).setEventType("PRODUCTION_SUBMIT").setRawPayload("{broken");
        event.setTenantId(1L); when(eventMapper.selectBatchIds(List.of(41L))).thenReturn(List.of(event));
        var review = new MesProcessPoolSubmissionReviewDO().setId(61L); review.setTenantId(1L);
        when(reviewMapper.selectBatchIds(Set.of(61L))).thenReturn(List.of(review));
        var error = assertThrows(ServiceException.class, () -> service.validateAllFacts(List.of(batch(1L))));
        assertEquals(400, error.getCode()); assertTrue(error.getMessage().contains("JSON"));
    }

    @Test void integrityDoesNotConvertDatabaseFailureToEvidenceError() {
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,
                () -> service.validateAllFacts(List.of(batch(1L))));
    }

    @Test void tenantMismatchIsRejectedBeforeReadingFacts() {
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceMaterial("MAT-A");
        assertThrows(IllegalStateException.class, () -> service.filter(List.of(batch(2L)),request));
        verifyNoInteractions(originMapper);
    }

    @Test void emptyWindowDoesNotReadBusinessFacts() {
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceMaterial("MAT-A");
        assertEquals(List.of(), service.prepareWindowFilter(request).apply(List.of()));
        verifyNoInteractions(originMapper, allocationMapper, eventMapper, reviewMapper, pqcDetailMapper,
                pqcRecordMapper, bindingMapper, materialMapper, releaseMapper, applicationMapper, adapters);
    }

    @Test void windowBatchesShareActiveOrderReadsButKeepSeparateReleaseFacts() {
        var first = batch(1L);
        var second = batch(1L).setId(2L).setBatchExecutionCode("B-2");
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(origin(), origin().setBatchExecutionId(2L)));
        var allocation = new MesProcessPoolReportAllocationDO().setActiveOrderId(101L).setWorkOrderId(501L)
                .setEventId(41L).setReviewId(61L); allocation.setTenantId(1L);
        when(allocationMapper.selectListByActiveOrderIds(List.of(101L))).thenReturn(List.of(allocation));
        var event = new MesProProcessPoolEventDO().setId(41L).setEventType("PRODUCTION_SUBMIT");
        event.setTenantId(1L);
        when(eventMapper.selectBatchIds(List.of(41L))).thenReturn(List.of(event));
        var review = new MesProcessPoolSubmissionReviewDO().setId(61L); review.setTenantId(1L);
        when(reviewMapper.selectBatchIds(Set.of(61L))).thenReturn(List.of(review));
        when(identityMapper.userIds(1L, "放行人")).thenReturn(Set.of(81L));
        var release = new MesProEdhrReleaseTransactionDO().setBatchExecutionId(2L).setApprovedBy(81L);
        when(releaseMapper.selectListByBatchExecutionIds(List.of(1L, 2L))).thenReturn(List.of(release));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTracePerson("放行人");
        assertEquals(List.of(second), service.prepareWindowFilter(request).apply(List.of(first, second)));
        verify(originMapper, times(1)).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(allocationMapper, times(1)).selectListByActiveOrderIds(List.of(101L));
        verify(eventMapper, times(1)).selectBatchIds(List.of(41L));
        verify(reviewMapper, times(1)).selectBatchIds(Set.of(61L));
        verify(pqcDetailMapper, times(1)).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(applicationMapper, times(1)).selectListByBatchExecutionIds(List.of(1L, 2L));
        verify(eventMapper, never()).selectById(any(java.io.Serializable.class));
    }

    @Test void distinctActiveOrdersDoNotBorrowMaterialFacts() {
        var first = batch(1L);
        var second = batch(1L).setId(2L).setBatchExecutionCode("B-2");
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(origin(), origin().setBatchExecutionId(2L).setActiveOrderId(102L)));
        var binding = new MesProcessPoolActiveOrderPickListBindingDO().setId(71L).setActiveOrderId(101L)
                .setWorkOrderId(501L).setBindingStatus("BOUND"); binding.setTenantId(1L);
        when(bindingMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(binding));
        var material = new MesProcessPoolActiveOrderPickListBindingItemDO().setBindingId(71L)
                .setMaterialNumber("MAT-A").setLotNumber("LOT-A"); material.setTenantId(1L);
        when(materialMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(material));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceMaterial("MAT-A"); request.setTraceMaterialLot("LOT-A");
        assertEquals(List.of(first), service.filter(List.of(first, second), request));
        verify(bindingMapper, times(1)).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(materialMapper, times(1)).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test void sharedEventMissingStillFailsInsteadOfExcludingTheBatch() {
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(origin()));
        var allocation = new MesProcessPoolReportAllocationDO().setActiveOrderId(101L).setWorkOrderId(501L).setEventId(41L);
        allocation.setTenantId(1L);
        when(allocationMapper.selectListByActiveOrderIds(List.of(101L))).thenReturn(List.of(allocation));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceEquipment("EQ-1");
        assertThrows(ServiceException.class, () -> service.filter(List.of(batch(1L)), request));
        verify(eventMapper).selectBatchIds(List.of(41L));
    }

    @Test void sharedActiveOrderDoesNotHideConflictingWorkOrderEvidence() {
        var second = batch(1L).setId(2L).setWorkOrderId(502L);
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(origin(), origin().setBatchExecutionId(2L).setWorkOrderId(502L)));
        var binding = new MesProcessPoolActiveOrderPickListBindingDO().setId(71L).setActiveOrderId(101L)
                .setWorkOrderId(501L).setBindingStatus("UNBOUND"); binding.setTenantId(1L);
        when(bindingMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(binding));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceMaterial("MAT-A");
        assertThrows(ServiceException.class, () -> service.filter(List.of(batch(1L), second), request));
    }

    @Test void fullCandidateWindowUsesOneEventReadAndNeverBorrowsFactsFromPriorWindow() {
        var batches = java.util.stream.LongStream.rangeClosed(1, 128)
                .mapToObj(id -> batch(1L).setId(id)).toList();
        var origins = batches.stream().map(b -> origin().setBatchExecutionId(b.getId())).toList();
        when(originMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(origins, List.of(origin().setBatchExecutionId(129L).setActiveOrderId(102L)));
        var allocation = new MesProcessPoolReportAllocationDO().setActiveOrderId(101L).setWorkOrderId(501L).setEventId(41L);
        allocation.setTenantId(1L);
        when(allocationMapper.selectListByActiveOrderIds(List.of(101L))).thenReturn(List.of(allocation));
        var event = new MesProProcessPoolEventDO().setId(41L).setEventType("PRODUCTION_SUBMIT")
                .setRawPayload("{\"selectedDevices\":[{\"deviceId\":31,\"deviceCode\":\"EQ-1\"}]}");
        event.setTenantId(1L);
        when(eventMapper.selectBatchIds(List.of(41L))).thenReturn(List.of(event));
        var request = new EdhrBatchExecutionPageReqVO(); request.setTraceEquipment("EQ-1");
        var filter = service.prepareWindowFilter(request);
        assertEquals(batches, filter.apply(batches));
        assertEquals(List.of(), filter.apply(List.of(batch(1L).setId(129L))));
        verify(identityMapper, times(1)).deviceIds(1L, "EQ-1");
        verify(eventMapper, times(1)).selectBatchIds(List.of(41L));
        verify(eventMapper, never()).selectById(any(java.io.Serializable.class));
        verify(allocationMapper, times(1)).selectListByActiveOrderIds(List.of(101L));
        verify(allocationMapper, times(1)).selectListByActiveOrderIds(List.of(102L));
    }

    @Test void allRequestedDimensionsRemainConjunctive() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setTracePerson("张三"); request.setTraceEquipment("EQ-1");
        request.setTraceMaterial("MAT-A"); request.setTraceMaterialLot("LOT-A");
        var person = new CatalogItem().setCategory(Category.PERSON).setEvidenceKey("PERSON:EMPLOYEE:11").setSavedValue("11");
        List<CatalogItem> facts = new ArrayList<>(List.of(person, material("MAT-A", "LOT-A")));
        assertFalse(MesProEdhrBatchLookupService.matches(facts, request, Set.of(11L), Set.of(), Set.of()));
        MesProEdhrBatchLookupService.addProductionEquipment(facts,
                "{\"selectedDevices\":[{\"deviceId\":31,\"deviceCode\":\"EQ-1\"}]}");
        assertTrue(MesProEdhrBatchLookupService.matches(facts, request, Set.of(11L), Set.of(), Set.of()));
        request.setTraceMaterialLot("LOT-B");
        assertFalse(MesProEdhrBatchLookupService.matches(facts, request, Set.of(11L), Set.of(), Set.of()));
    }

    private static MesProEdhrBatchExecutionDO batch(Long tenantId) {
        return new MesProEdhrBatchExecutionDO().setId(1L).setTenantId(tenantId).setWorkOrderId(501L).setBatchExecutionCode("B-1");
    }
    private static MesProEdhrBatchExecutionOriginDO origin() {
        return new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(1L).setTenantId(1L).setWorkOrderId(501L).setActiveOrderId(101L);
    }
    private static CatalogItem material(String code,String lot) {
        return new CatalogItem().setCategory(Category.MATERIAL).setLabel(code).setSourceContext(Map.of("materialNumber",code,"lotNumber",lot));
    }
    private static boolean matches(List<CatalogItem> facts,EdhrBatchExecutionPageReqVO request) {
        return MesProEdhrBatchLookupService.matches(facts,request,Set.of(),Set.of(),Set.of());
    }
}
