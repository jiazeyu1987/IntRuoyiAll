package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeBomLine;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.itemconsume.MesWmItemConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.outsourceissue.MesWmOutsourceIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdProductBomService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesKingdeeWorkOrderBomSyncServiceImplTest {

    @Mock
    private ErpKingdeeBomClient bomClient;
    @Mock
    private ErpKingdeeConfigService kingdeeConfigService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesMdProductBomService productBomService;
    @Mock
    private MesWmProductIssueMapper productIssueMapper;
    @Mock
    private MesWmOutsourceIssueMapper outsourceIssueMapper;
    @Mock
    private MesWmItemConsumeMapper itemConsumeMapper;

    private ErpKingdeeProperties kingdeeProperties;
    private MesKingdeeWorkOrderBomSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        kingdeeProperties = new ErpKingdeeProperties();
        kingdeeProperties.setBaseUrl("https://k3.example.com");
        kingdeeProperties.setAcctId("acct");
        kingdeeProperties.setUsername("user");
        kingdeeProperties.setPassword("password");
        kingdeeProperties.setLcid(2052);
        kingdeeProperties.getBom().setQueryLimit(200);
        syncService = new MesKingdeeWorkOrderBomSyncServiceImpl(
                bomClient, kingdeeConfigService, workOrderService, workOrderBomMapper, itemMapper,
                productBomService, productIssueMapper, outsourceIssueMapper, itemConsumeMapper);
    }

    @Test
    void syncErpBom_replacesExistingBomForPrepareWorkOrder() {
        MesProWorkOrderDO workOrder = buildWorkOrder(MesProWorkOrderStatusEnum.PREPARE.getStatus(), new BigDecimal("10"));
        ErpKingdeeBomLine bomLine = buildBomLine("V1", "SUB-001", "Child 1", "Spec 1", "PCS", "2", "1");
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemMapper.selectById(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(bomLine));
        when(itemMapper.selectByCode("SUB-001")).thenReturn(new MesMdItemDO().setId(200L).setCode("SUB-001"));
        when(productBomService.getProductBomListByItemId(200L)).thenReturn(List.of());
        when(productIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(outsourceIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(itemConsumeMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);

        MesKingdeeWorkOrderBomSyncResult result = syncService.syncErpBom(100L);

        assertEquals(100L, result.getWorkOrderId());
        assertEquals("V1", result.getErpBomVersion());
        assertEquals(1, result.getSyncedBomCount());
        verify(workOrderBomMapper).deleteByWorkOrderId(100L);
        ArgumentCaptor<List<MesProWorkOrderBomDO>> bomCaptor = ArgumentCaptor.forClass(List.class);
        verify(workOrderBomMapper).insertBatch(bomCaptor.capture());
        assertEquals(new BigDecimal("20"), bomCaptor.getValue().get(0).getQuantity());
    }

    @Test
    void syncErpBom_allowsConfirmedWorkOrderWithoutDownstreamRecords() {
        MesProWorkOrderDO workOrder = buildWorkOrder(MesProWorkOrderStatusEnum.CONFIRMED.getStatus(), new BigDecimal("3"));
        ErpKingdeeBomLine bomLine = buildBomLine("V2", "SUB-002", "Child 2", "Spec 2", "PCS", "1.5", "1");
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemMapper.selectById(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001"));
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(bomLine));
        when(itemMapper.selectByCode("SUB-002")).thenReturn(new MesMdItemDO().setId(201L).setCode("SUB-002"));
        when(productBomService.getProductBomListByItemId(201L)).thenReturn(List.of());
        when(productIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(outsourceIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(itemConsumeMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);

        MesKingdeeWorkOrderBomSyncResult result = syncService.syncErpBom(100L);

        assertEquals("V2", result.getErpBomVersion());
        assertEquals(1, result.getSyncedBomCount());
    }

    @Test
    void syncErpBom_blocksWhenMultipleApprovedVersionsExist() {
        MesProWorkOrderDO workOrder = buildWorkOrder(MesProWorkOrderStatusEnum.PREPARE.getStatus(), new BigDecimal("10"));
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemMapper.selectById(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001"));
        when(productIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(outsourceIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(itemConsumeMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-001", "Child 1", "Spec 1", "PCS", "1", "1"),
                buildBomLine("V2", "SUB-001", "Child 1", "Spec 1", "PCS", "1", "1")
        ));

        assertThrows(RuntimeException.class, () -> syncService.syncErpBom(100L));
        verify(workOrderBomMapper, never()).deleteByWorkOrderId(any());
    }

    @Test
    void syncErpBom_blocksWhenDownstreamRecordsExist() {
        MesProWorkOrderDO workOrder = buildWorkOrder(MesProWorkOrderStatusEnum.CONFIRMED.getStatus(), new BigDecimal("10"));
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(productIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> syncService.syncErpBom(100L));
        verify(bomClient, never()).fetchApprovedBomByParentMaterialNumber(any(), any());
    }

    @Test
    void syncErpBom_blocksWhenLocalMesItemIsMissing() {
        MesProWorkOrderDO workOrder = buildWorkOrder(MesProWorkOrderStatusEnum.PREPARE.getStatus(), new BigDecimal("10"));
        when(kingdeeConfigService.getEffectiveProperties()).thenReturn(kingdeeProperties);
        when(workOrderService.validateWorkOrderExists(100L)).thenReturn(workOrder);
        when(itemMapper.selectById(20L)).thenReturn(new MesMdItemDO().setId(20L).setCode("MAT-001"));
        when(productIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(outsourceIssueMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(itemConsumeMapper.selectCountByWorkOrderId(100L)).thenReturn(0L);
        when(bomClient.fetchApprovedBomByParentMaterialNumber(kingdeeProperties, "MAT-001")).thenReturn(List.of(
                buildBomLine("V1", "SUB-MISSING", "Missing Child", "Spec 1", "PCS", "1", "1")
        ));
        when(itemMapper.selectByCode("SUB-MISSING")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> syncService.syncErpBom(100L));
        verify(workOrderBomMapper, never()).deleteByWorkOrderId(any());
    }

    private static MesProWorkOrderDO buildWorkOrder(Integer status, BigDecimal quantity) {
        return MesProWorkOrderDO.builder()
                .id(100L)
                .productId(20L)
                .status(status)
                .quantity(quantity)
                .requestDate(LocalDateTime.of(2026, 5, 16, 8, 0))
                .build();
    }

    private static ErpKingdeeBomLine buildBomLine(String version, String childCode, String childName,
                                                  String childSpec, String childUnit, String numerator,
                                                  String denominator) {
        ErpKingdeeBomLine line = new ErpKingdeeBomLine();
        line.setFid("310119");
        line.setBomVersion(version);
        line.setParentMaterialNumber("MAT-001");
        line.setParentMaterialName("Parent Product");
        line.setParentMaterialSpecification("Parent Spec");
        line.setChildMaterialNumber(childCode);
        line.setChildMaterialName(childName);
        line.setChildMaterialSpecification(childSpec);
        line.setChildUnitName(childUnit);
        line.setNumerator(new BigDecimal(numerator));
        line.setDenominator(new BigDecimal(denominator));
        return line;
    }

}
