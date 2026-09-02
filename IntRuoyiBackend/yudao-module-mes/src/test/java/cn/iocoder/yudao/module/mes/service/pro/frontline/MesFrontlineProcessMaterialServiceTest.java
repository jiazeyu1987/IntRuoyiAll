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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void listFrozenMaterials_returnsOnlyCurrentProcessBatchRecordMaterialsFromLockedVersion() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "frontlineReportMaterialIds": [502, 501]},
                      {"routeProcessId": 1002, "frontlineReportMaterialIds": [504]}
                    ],
                    "productBoms": {
                      "201:61:503": {"processId": 201, "productId": 61, "itemId": 503, "quantity": 9}
                    }
                  }
                }
                """));
        when(itemMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                item(502L, "A002", "杠杆"),
                item(501L, "A001", "弹簧")));
        when(batchQueryService.listBatchCodes(WORK_ORDER_ID, "A001")).thenReturn(List.of("S-001"));
        when(batchQueryService.listBatchCodes(WORK_ORDER_ID, "A002")).thenReturn(List.of());

        List<MesFrontlineProcessMaterial> materials = service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertEquals(2, materials.size());
        assertEquals(501L, materials.get(0).materialId());
        assertEquals("弹簧", materials.get(0).materialName());
        assertNull(materials.get(0).bomQuantity());
        assertEquals(List.of("S-001"), materials.get(0).batchCodes());
        assertEquals(502L, materials.get(1).materialId());
        assertEquals("杠杆", materials.get(1).materialName());
        assertNull(materials.get(1).bomQuantity());
        assertEquals(List.of(), materials.get(1).batchCodes());
    }

    @Test
    void listFrozenMaterials_allowsMissingCurrentProcessBatchRecordMaterials() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "frontlineReportMaterialIds": []}
                    ],
                    "productBoms": {
                      "201:61:503": {"processId": 201, "productId": 61, "itemId": 503, "quantity": 1}
                    }
                  }
                }
                """));

        List<MesFrontlineProcessMaterial> materials = service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID,
                ROUTE_PROCESS_ID, PROCESS_ID);

        assertTrue(materials.isEmpty());
        verifyNoInteractions(itemMapper, batchQueryService);
    }

    @Test
    void listFrozenMaterials_rejectsDuplicateBatchRecordMaterialIds() {
        when(routeVersionMapper.selectById(ROUTE_VERSION_ID)).thenReturn(routeVersion("""
                {
                  "routeId": 101,
                  "configSnapshots": {
                    "batchUseConfigs": [
                      {"routeProcessId": 1001, "frontlineReportMaterialIds": [501, 501]}
                    ]
                  }
                }
                """));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.listFrozenMaterials(ACTIVE_ORDER_ID, ROUTE_ID, ROUTE_PROCESS_ID, PROCESS_ID));

        assertTrue(error.getMessage().contains("报工物料重复"));
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
}
