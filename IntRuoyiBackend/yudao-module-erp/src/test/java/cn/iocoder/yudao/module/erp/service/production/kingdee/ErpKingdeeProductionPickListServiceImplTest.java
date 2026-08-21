package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickList;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListClient;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        when(productionPickListClient.fetchProductionPickLists(properties))
                .thenReturn(List.of(pickList));
        when(productionPickListMapper.selectBySource("PRD_PickMtrl", "1001"))
                .thenReturn(null);
        doAnswer(invocation -> {
            ErpKingdeeProductionPickListDO record = invocation.getArgument(0);
            record.setId(501L);
            return 1;
        }).when(productionPickListMapper).insert(any(ErpKingdeeProductionPickListDO.class));

        ErpKingdeeProductionPickListSyncResult result = service.syncAll();

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        ArgumentCaptor<ErpKingdeeProductionPickListDO> headerCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionPickListDO.class);
        verify(productionPickListMapper).insert(headerCaptor.capture());
        assertEquals("PRD_PickMtrl", headerCaptor.getValue().getSourceFormId());
        assertEquals("PICK001", headerCaptor.getValue().getSourceBillNo());
        assertEquals(1L, headerCaptor.getValue().getTenantId());
        verify(productionPickListItemMapper).deleteByProductionPickListId(501L);
        ArgumentCaptor<ErpKingdeeProductionPickListItemDO> itemCaptor =
                ArgumentCaptor.forClass(ErpKingdeeProductionPickListItemDO.class);
        verify(productionPickListItemMapper).insert(itemCaptor.capture());
        assertEquals("1001|2001", itemCaptor.getValue().getSourceLineKey());
        assertEquals("MAT001", itemCaptor.getValue().getMaterialNumber());
        assertEquals(new BigDecimal("6"), itemCaptor.getValue().getActualQuantity());
        assertEquals(1L, itemCaptor.getValue().getTenantId());
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

        ErpKingdeeProductionPickListSyncResult result = service.syncModifiedBetween(windowStart, windowEnd);

        assertEquals(1, result.getCreatedCount());
        verify(productionPickListClient, never()).fetchProductionPickLists(properties);
        verify(productionPickListClient).fetchProductionPickListsModifiedBetween(properties, windowStart, windowEnd);
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
