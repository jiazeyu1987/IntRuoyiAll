package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialBatchQueryService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFeedbackMaterialBatchEvidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineProcessMaterialServiceTest {

    private static final Long ACTIVE_ORDER_ID = 8101L;
    private static final Long WORK_ORDER_ID = 4101L;
    private static final Long ROUTE_ID = 101L;
    private static final Long ROUTE_VERSION_ID = 627L;
    private static final Long ROUTE_PROCESS_ID = 1001L;
    private static final Long PROCESS_ID = 201L;
    @Mock
    private ActiveOrderSnapshotResolver activeOrderSnapshotResolver;
    @Mock
    private MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProFeedbackMaterialBatchQueryService batchQueryService;

    private MesFrontlineProcessMaterialService service;

    @BeforeEach
    void setUp() {
        service = new MesFrontlineProcessMaterialServiceImpl(activeOrderSnapshotResolver, processSnapshotMapper,
                routeVersionMapper, workOrderMapper, itemMapper, batchQueryService);
        when(activeOrderSnapshotResolver.requireEffective(ACTIVE_ORDER_ID))
                .thenReturn(new ActiveOrderSnapshotResolver.ActiveOrderSnapshot(ACTIVE_ORDER_ID, WORK_ORDER_ID,
                        ROUTE_ID, ROUTE_VERSION_ID, 71L, 81L, 91L));
        when(processSnapshotMapper.selectByActiveOrderAndProcess(ACTIVE_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID))
                .thenReturn(new MesProcessPoolActiveOrderProcessSnapshotDO()
                        .setId(5101L)
                        .setActiveOrderId(ACTIVE_ORDER_ID)
                        .setWorkOrderId(WORK_ORDER_ID)
                        .setRouteId(ROUTE_ID)
                        .setRouteVersionId(ROUTE_VERSION_ID)
                        .setRouteProcessId(ROUTE_PROCESS_ID)
                        .setProcessId(PROCESS_ID));
        when(workOrderMapper.selectById(WORK_ORDER_ID))
                .thenReturn(MesProWorkOrderDO.builder().id(WORK_ORDER_ID).build());
    }

    @Test
    void listFrozenMaterials_separatesInputBatchEvidenceFromOutputCompletionMaterials() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "inputMaterialIds": [503], "outputMaterialIds": [502, 501]},
                      {"routeProcessId": 1002, "inputMaterialIds": [], "outputMaterialIds": [504]}
                    ],
                    "productBoms": {
                      "201:61:503": {"processId": 201, "productId": 61, "itemId": 503, "quantity": 9}
                    }
                  }
                }
                """));
        when(itemMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                item(503L, "A003", "输入原料"),
                item(502L, "A002", "杠杆"),
                item(501L, "A001", "弹簧")));
        when(batchQueryService.resolveEvidence(WORK_ORDER_ID, "A003"))
                .thenReturn(evidence("A003", List.of("LOT-001", "LOT-002")));

        List<MesFrontlineProcessMaterial> materials = service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(3, materials.size());
        assertEquals(503L, materials.get(0).materialId());
        assertEquals("INPUT", materials.get(0).materialRole());
        assertEquals(List.of("LOT-001", "LOT-002"), materials.get(0).batchCodes());
        assertEquals(501L, materials.get(1).materialId());
        assertEquals("OUTPUT", materials.get(1).materialRole());
        assertEquals("弹簧", materials.get(1).materialName());
        assertTrue(materials.get(1).batchCodes().isEmpty());
        assertNull(materials.get(0).bomQuantity());
        assertEquals(502L, materials.get(2).materialId());
        assertEquals("OUTPUT", materials.get(2).materialRole());
        verify(batchQueryService).resolveEvidence(WORK_ORDER_ID, "A003");
        verify(batchQueryService, never()).resolveEvidence(WORK_ORDER_ID, "A001");
        verify(batchQueryService, never()).resolveEvidence(WORK_ORDER_ID, "A002");
    }

    @Test
    void listFrozenMaterials_allowsMissingCurrentProcessBatchRecordMaterials() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "inputMaterialIds": [503], "outputMaterialIds": []}
                    ],
                    "productBoms": {
                      "201:61:503": {"processId": 201, "productId": 61, "itemId": 503, "quantity": 1}
                    }
                  }
                }
                """));
        when(itemMapper.selectListByIds(anyCollection())).thenReturn(List.of(item(503L, "A003", "输入原料")));
        when(batchQueryService.resolveEvidence(WORK_ORDER_ID, "A003"))
                .thenReturn(evidence("A003", List.of("LOT-001")));

        List<MesFrontlineProcessMaterial> materials = service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(1, materials.size());
        assertEquals("INPUT", materials.get(0).materialRole());
    }

    @Test
    void listFrozenMaterials_rejectsDuplicateBatchRecordMaterialIds() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "inputMaterialIds": [], "outputMaterialIds": [501, 501]}
                    ]
                  }
                }
                """));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID));

        assertTrue(error.getMessage().contains("输出物料重复"));
    }

    @Test
    void listFrozenMaterials_rejectsInputOutputRoleOverlap() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "inputMaterialIds": [501], "outputMaterialIds": [501]}
                    ]
                  }
                }
                """));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID));

        assertTrue(error.getMessage().contains("输入输出物料重复"));
        verifyNoInteractions(itemMapper, batchQueryService);
    }

    private static MesProRouteVersionDO routeVersion(String snapshotJson) {
        return MesProRouteVersionDO.builder()
                .id(ROUTE_VERSION_ID)
                .routeId(ROUTE_ID)
                .routeSnapshotJson(snapshotJson)
                .build();
    }

    private static MesMdItemDO item(Long id, String code, String name) {
        return MesMdItemDO.builder().id(id).code(code).name(name).specification("规格-" + code).build();
    }

    private static MesProFeedbackMaterialBatchEvidence evidence(String code, List<String> lots) {
        return new MesProFeedbackMaterialBatchEvidence(code, lots, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, List.of(101L, 102L), List.of(1001L, 1002L), "source-hash");
    }
}
