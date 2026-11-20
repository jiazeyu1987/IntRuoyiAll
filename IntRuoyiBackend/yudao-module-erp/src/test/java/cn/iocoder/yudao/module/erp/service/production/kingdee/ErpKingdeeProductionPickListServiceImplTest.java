package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickList;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListClient;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeProductionPickListServiceImplTest {

    @Mock
    private ErpKingdeeProductionPickListClient productionPickListClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpKingdeeProductionPickListMapper productionPickListMapper;
    @Mock
    private ErpKingdeeProductionPickListItemMapper productionPickListItemMapper;

    private ErpKingdeeProperties properties;
    private ErpKingdeeProductionPickListServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("user");
        properties.setPassword("password");
        properties.setLcid(2052);
        service = new ErpKingdeeProductionPickListServiceImpl();
        ReflectionTestUtils.setField(service, "productionPickListClient",
                productionPickListClient);
        ReflectionTestUtils.setField(service, "kingdeeConfigService", kingdeeConfigService);
        ReflectionTestUtils.setField(service, "productionPickListMapper", productionPickListMapper);
        ReflectionTestUtils.setField(service, "productionPickListItemMapper",
                productionPickListItemMapper);
    }

    @Test
    void syncAll_createsIndependentHeaderAndReplacesItsLines() {
        ErpKingdeeProductionPickList pickList = buildPickList();
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);
        LocalDateTime windowStart = LocalDateTime.of(2025, 8, 22, 0, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 8, 22, 13, 0);
        when(productionPickListClient.fetchProductionPickLists(properties, windowStart, windowEnd))
                .thenReturn(List.of(pickList));
        when(productionPickListMapper.selectBySource("PRD_PickMtrl", "1001"))
                .thenReturn(null);
        doAnswer(invocation -> {
            ErpKingdeeProductionPickListDO record = invocation.getArgument(0);
            record.setId(501L);
            return 1;
        }).when(productionPickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));
        when(productionPickListItemMapper.insertBatch(any(), eq(1000))).thenReturn(true);

        ErpKingdeeProductionPickListSyncResult result = service.syncAll(windowStart, windowEnd);

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        ArgumentCaptor<ErpKingdeeProductionPickListDO> headerCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionPickListDO.class);
        verify(productionPickListMapper).insert(headerCaptor.capture());
        assertEquals("PRD_PickMtrl", headerCaptor.getValue().getSourceFormId());
        assertEquals("PICK001", headerCaptor.getValue().getSourceBillNo());
        assertEquals(1L, headerCaptor.getValue().getTenantId());
        verify(productionPickListItemMapper).deleteByProductionPickListId(501L);
        ArgumentCaptor<Collection<ErpKingdeeProductionPickListItemDO>> itemCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(productionPickListItemMapper).insertBatch(itemCaptor.capture(), eq(1000));
        assertEquals(1, itemCaptor.getValue().size());
        ErpKingdeeProductionPickListItemDO item = itemCaptor.getValue().iterator().next();
        assertEquals("1001|2001", item.getSourceLineKey());
        assertEquals("MAT001", item.getMaterialNumber());
        assertEquals(new BigDecimal("6"), item.getActualQuantity());
        assertEquals(1L, item.getTenantId());
        verify(productionPickListItemMapper, never()).insert(any(ErpKingdeeProductionPickListItemDO.class));
    }

    @Test
    void syncModifiedBetween_fetchesWindowRowsWithoutInitialFullFallback() {
        ErpKingdeeProductionPickList pickList = buildPickList();
        LocalDateTime windowStart = LocalDateTime.of(2026, 2, 21, 0, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 8, 21, 10, 0);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);
        when(productionPickListClient.fetchProductionPickListsModifiedBetween(properties, windowStart, windowEnd))
                .thenReturn(List.of(pickList));
        when(productionPickListMapper.selectBySource("PRD_PickMtrl", "1001"))
                .thenReturn(null);
        doAnswer(invocation -> {
            ErpKingdeeProductionPickListDO record = invocation.getArgument(0);
            record.setId(502L);
            return 1;
        }).when(productionPickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));
        when(productionPickListItemMapper.insertBatch(any(), eq(1000))).thenReturn(true);

        ErpKingdeeProductionPickListSyncResult result = service.syncModifiedBetween(windowStart, windowEnd);

        assertEquals(1, result.getCreatedCount());
        verify(productionPickListClient, never()).fetchProductionPickLists(
                any(ErpKingdeeProperties.class), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(productionPickListClient).fetchProductionPickListsModifiedBetween(properties, windowStart, windowEnd);
    }

    @Test
    void syncAll_batchFailureIsReportedInsteadOfReturningSuccess() {
        ErpKingdeeProductionPickList pickList = buildPickList();
        LocalDateTime windowStart = LocalDateTime.of(2026, 8, 21, 0, 0);
        LocalDateTime windowEnd = windowStart.plusDays(1);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);
        when(productionPickListClient.fetchProductionPickLists(properties, windowStart, windowEnd))
                .thenReturn(List.of(pickList));
        when(productionPickListMapper.selectBySource("PRD_PickMtrl", "1001"))
                .thenReturn(ErpKingdeeProductionPickListDO.builder().id(501L).build());
        when(productionPickListItemMapper.insertBatch(any(), eq(1000))).thenReturn(false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.syncAll(windowStart, windowEnd));

        assertEquals("Production pick list item batch insert failed: fid=1001", error.getMessage());
        verify(productionPickListItemMapper).deleteByProductionPickListId(501L);
    }

    @Test
    void getPage_filtersHeadersByProductionOrderFromLines() {
        ErpProductionPickListPageReqVO reqVO = new ErpProductionPickListPageReqVO();
        reqVO.setProductionOrderNo("MO001");
        ErpKingdeeProductionPickListDO header = ErpKingdeeProductionPickListDO.builder()
                .id(501L)
                .sourceBillNo("PICK001")
                .build();
        ErpKingdeeProductionPickListItemDO item = ErpKingdeeProductionPickListItemDO.builder()
                .productionPickListId(501L)
                .productionOrderNo("MO001")
                .materialName("物料一")
                .build();
        when(productionPickListItemMapper.selectPickListIdsByProductionOrderNo("MO001"))
                .thenReturn(List.of(501L));
        when(productionPickListMapper.selectPageByProductionPickListIds(eq(reqVO), eq(List.of(501L))))
                .thenReturn(new PageResult<>(List.of(header), 1L));
        when(productionPickListItemMapper.selectListByPickListIds(Set.of(501L)))
                .thenReturn(List.of(item));

        PageResult<?> result = service.getPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        verify(productionPickListItemMapper).selectPickListIdsByProductionOrderNo("MO001");
        verify(productionPickListMapper)
                .selectPageByProductionPickListIds(eq(reqVO), eq(List.of(501L)));
    }

    @Test
    void getPage_columnOrderFilterRestrictsHeadersAndPreservesOtherFiltersAndRequest() {
        var request = new ErpProductionPickListPageReqVO();
        String original = "[{\"key\":\"productionOrderNos\",\"operator\":\"contains\",\"value\":\"MO001\"},"
                + "{\"key\":\"documentStatus\",\"operator\":\"eq\",\"value\":\"C\"}]";
        request.setListFilters(original);
        request.setPageNo(2);
        request.setPageSize(20);
        request.setStockOrgName("测试库存组织");
        when(productionPickListItemMapper.selectPickListIdsByOrderFilter(any())).thenReturn(List.of(501L));
        when(productionPickListMapper.selectPageByProductionPickListIds(any(), eq(List.of(501L))))
                .thenReturn(PageResult.empty(1L));

        assertEquals(1L, service.getPage(request).getTotal());

        var header = ArgumentCaptor.forClass(ErpProductionPickListPageReqVO.class);
        verify(productionPickListMapper).selectPageByProductionPickListIds(header.capture(), eq(List.of(501L)));
        assertEquals("[{\"key\":\"documentStatus\",\"operator\":\"eq\",\"value\":\"C\"}]",
                header.getValue().getListFilters());
        assertEquals(2, header.getValue().getPageNo());
        assertEquals(20, header.getValue().getPageSize());
        assertEquals("测试库存组织", header.getValue().getStockOrgName());
        assertEquals(original, request.getListFilters());
        var detail = ArgumentCaptor.forClass(PageParam.class);
        verify(productionPickListItemMapper).selectPickListIdsByOrderFilter(detail.capture());
        assertEquals("[{\"key\":\"productionOrderNos\",\"operator\":\"contains\",\"value\":\"MO001\"}]",
                detail.getValue().getListFilters());
    }

    @Test
    void getPage_multipleOrderConditionsIntersectBillsRatherThanRequireOneLineToMatchAll() {
        var request = new ErpProductionPickListPageReqVO();
        request.setListFilters("[{\"key\":\"productionOrderNos\",\"operator\":\"eq\",\"value\":\"MO001\"},"
                + "{\"key\":\"productionOrderNos\",\"operator\":\"eq\",\"value\":\"MO002\"}]");
        when(productionPickListItemMapper.selectPickListIdsByOrderFilter(any()))
                .thenReturn(List.of(501L, 502L), List.of(502L, 503L));
        when(productionPickListMapper.selectPageByProductionPickListIds(any(), eq(List.of(502L))))
                .thenReturn(PageResult.empty(1L));

        assertEquals(1L, service.getPage(request).getTotal());

        var header = ArgumentCaptor.forClass(ErpProductionPickListPageReqVO.class);
        verify(productionPickListMapper).selectPageByProductionPickListIds(header.capture(), eq(List.of(502L)));
        assertNull(header.getValue().getListFilters());
    }

    @Test
    void getPage_emptyOrderMatchDoesNotRunUnrestrictedHeaderQuery() {
        var request = new ErpProductionPickListPageReqVO();
        request.setListFilters("[{\"key\":\"productionOrderNos\",\"operator\":\"contains\",\"value\":\"MISSING\"}]");
        when(productionPickListItemMapper.selectPickListIdsByOrderFilter(any())).thenReturn(List.of());

        assertEquals(0L, service.getPage(request).getTotal());

        verify(productionPickListMapper, never()).selectPageByProductionPickListIds(any(), any());
    }

    @Test
    void getPage_validatesAllFiltersBeforeEmptyMatchCanShortCircuit() {
        var request = new ErpProductionPickListPageReqVO();
        request.setListFilters("[{\"key\":\"productionOrderNos\",\"operator\":\"contains\",\"value\":\"MO001\"},"
                + "{\"key\":\"unconfigured\",\"operator\":\"eq\",\"value\":\"C\"}]");

        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.getPage(request));

        verify(productionPickListItemMapper, never()).selectPickListIdsByOrderFilter(any());
        verify(productionPickListMapper, never()).selectPageByProductionPickListIds(any(), any());
    }

    private static ErpKingdeeProductionPickList buildPickList() {
        ErpKingdeeProductionPickList.Line line = new ErpKingdeeProductionPickList.Line();
        line.setEntryId("2001");
        line.setMaterialNumber("MAT001");
        line.setMaterialName("物料一");
        line.setActualQuantity(new BigDecimal("6"));
        line.setRequestedQuantity(new BigDecimal("8"));
        line.setProductionOrderNo("MO001");

        ErpKingdeeProductionPickList pickList = new ErpKingdeeProductionPickList();
        pickList.setFid("1001");
        pickList.setBillNo("PICK001");
        pickList.setLines(List.of(line));
        return pickList;
    }

}
