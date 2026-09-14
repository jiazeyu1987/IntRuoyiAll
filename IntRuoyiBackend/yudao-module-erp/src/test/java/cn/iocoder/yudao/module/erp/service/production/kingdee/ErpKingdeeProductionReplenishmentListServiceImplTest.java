package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListPageReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentList;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListClient;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpKingdeeProductionReplenishmentListServiceImplTest {

    @Mock
    private ErpKingdeeProductionReplenishmentListClient productionReplenishmentListClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private ErpKingdeeProductionReplenishmentListMapper productionReplenishmentListMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListItemMapper productionReplenishmentListItemMapper;

    private ErpKingdeeProperties properties;
    private ErpKingdeeProductionReplenishmentListServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        properties = new ErpKingdeeProperties();
        properties.setBaseUrl("https://k3.example.com");
        properties.setAcctId("acct");
        properties.setUsername("user");
        properties.setPassword("password");
        properties.setLcid(2052);
        service = new ErpKingdeeProductionReplenishmentListServiceImpl();
        ReflectionTestUtils.setField(service, "productionReplenishmentListClient",
                productionReplenishmentListClient);
        ReflectionTestUtils.setField(service, "kingdeeConfigService", kingdeeConfigService);
        ReflectionTestUtils.setField(service, "productionReplenishmentListMapper", productionReplenishmentListMapper);
        ReflectionTestUtils.setField(service, "productionReplenishmentListItemMapper",
                productionReplenishmentListItemMapper);
    }

    @Test
    void syncAll_createsIndependentHeaderAndReplacesItsLines() {
        ErpKingdeeProductionReplenishmentList replenishmentList = buildReplenishmentList();
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);
        LocalDateTime windowStart = LocalDateTime.of(2025, 8, 22, 0, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 8, 22, 13, 0);
        when(productionReplenishmentListClient.fetchProductionReplenishmentLists(properties, windowStart, windowEnd))
                .thenReturn(List.of(replenishmentList));
        when(productionReplenishmentListMapper.selectBySource("PRD_FeedMtrl", "1001"))
                .thenReturn(null);
        doAnswer(invocation -> {
            ErpKingdeeProductionReplenishmentListDO record = invocation.getArgument(0);
            record.setId(501L);
            return 1;
        }).when(productionReplenishmentListMapper).insert(any(ErpKingdeeProductionReplenishmentListDO.class));

        ErpKingdeeProductionReplenishmentListSyncResult result = service.syncAll(windowStart, windowEnd);

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        ArgumentCaptor<ErpKingdeeProductionReplenishmentListDO> headerCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionReplenishmentListDO.class);
        verify(productionReplenishmentListMapper).insert(headerCaptor.capture());
        assertEquals("PRD_FeedMtrl", headerCaptor.getValue().getSourceFormId());
        assertEquals("FEED001", headerCaptor.getValue().getSourceBillNo());
        assertEquals(1L, headerCaptor.getValue().getTenantId());
        verify(productionReplenishmentListItemMapper).deleteByProductionReplenishmentListId(501L);
        ArgumentCaptor<ErpKingdeeProductionReplenishmentListItemDO> itemCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionReplenishmentListItemDO.class);
        verify(productionReplenishmentListItemMapper).insert(itemCaptor.capture());
        assertEquals("1001|2001", itemCaptor.getValue().getSourceLineKey());
        assertEquals("MAT001", itemCaptor.getValue().getMaterialNumber());
        assertEquals(new BigDecimal("6"), itemCaptor.getValue().getActualQuantity());
        assertEquals(1L, itemCaptor.getValue().getTenantId());
    }

    @Test
    void syncModifiedBetween_fetchesWindowRowsWithoutInitialFullFallback() {
        ErpKingdeeProductionReplenishmentList replenishmentList = buildReplenishmentList();
        LocalDateTime windowStart = LocalDateTime.of(2026, 2, 21, 0, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2026, 8, 21, 10, 0);
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(properties);
        when(productionReplenishmentListClient.fetchProductionReplenishmentListsModifiedBetween(properties, windowStart, windowEnd))
                .thenReturn(List.of(replenishmentList));
        when(productionReplenishmentListMapper.selectBySource("PRD_FeedMtrl", "1001"))
                .thenReturn(null);
        doAnswer(invocation -> {
            ErpKingdeeProductionReplenishmentListDO record = invocation.getArgument(0);
            record.setId(502L);
            return 1;
        }).when(productionReplenishmentListMapper).insert(any(ErpKingdeeProductionReplenishmentListDO.class));

        ErpKingdeeProductionReplenishmentListSyncResult result = service.syncModifiedBetween(windowStart, windowEnd);

        assertEquals(1, result.getCreatedCount());
        verify(productionReplenishmentListClient, never()).fetchProductionReplenishmentLists(
                any(ErpKingdeeProperties.class), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(productionReplenishmentListClient).fetchProductionReplenishmentListsModifiedBetween(properties, windowStart, windowEnd);
    }

    @Test
    void getPage_filtersHeadersByProductionOrderFromLines() {
        ErpProductionReplenishmentListPageReqVO reqVO = new ErpProductionReplenishmentListPageReqVO();
        reqVO.setProductionOrderNo("MO001");
        ErpKingdeeProductionReplenishmentListDO header = ErpKingdeeProductionReplenishmentListDO.builder()
                .id(501L)
                .sourceBillNo("FEED001")
                .build();
        ErpKingdeeProductionReplenishmentListItemDO item = ErpKingdeeProductionReplenishmentListItemDO.builder()
                .productionReplenishmentListId(501L)
                .productionOrderNo("MO001")
                .materialName("物料一")
                .build();
        when(productionReplenishmentListItemMapper.selectReplenishmentListIdsByProductionOrderNo("MO001"))
                .thenReturn(List.of(501L));
        when(productionReplenishmentListMapper.selectPageByProductionReplenishmentListIds(eq(reqVO), eq(List.of(501L))))
                .thenReturn(new PageResult<>(List.of(header), 1L));
        when(productionReplenishmentListItemMapper.selectListByReplenishmentListIds(Set.of(501L)))
                .thenReturn(List.of(item));

        PageResult<?> result = service.getPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        verify(productionReplenishmentListItemMapper).selectReplenishmentListIdsByProductionOrderNo("MO001");
        verify(productionReplenishmentListMapper)
                .selectPageByProductionReplenishmentListIds(eq(reqVO), eq(List.of(501L)));
    }

    private static ErpKingdeeProductionReplenishmentList buildReplenishmentList() {
        ErpKingdeeProductionReplenishmentList.Line line = new ErpKingdeeProductionReplenishmentList.Line();
        line.setEntryId("2001");
        line.setMaterialNumber("MAT001");
        line.setMaterialName("物料一");
        line.setActualQuantity(new BigDecimal("6"));
        line.setRequestedQuantity(new BigDecimal("8"));
        line.setProductionOrderNo("MO001");

        ErpKingdeeProductionReplenishmentList replenishmentList = new ErpKingdeeProductionReplenishmentList();
        replenishmentList.setFid("1001");
        replenishmentList.setBillNo("FEED001");
        replenishmentList.setLines(List.of(line));
        return replenishmentList;
    }

}
