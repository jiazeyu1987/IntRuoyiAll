package cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProDccProjectGovernanceServiceImplTest {

    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProRouteProductMapper routeProductMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProBatchRecordDefinitionMapper definitionMapper;
    @Mock
    private MesProBatchRecordVersionMapper versionMapper;
    @Mock
    private MesProBatchRecordReportMapper reportMapper;

    @InjectMocks
    private MesProDccProjectGovernanceServiceImpl service;

    @Test
    void getStatus_sameNameRouteWithoutFormalProjectCodeChain_reportsMissing() {
        String projectName = "同名但未绑定项目";
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of());
        stubBatchRecordLookups(projectName);

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName)).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_MISSING, status.routeStatus());
        assertEquals(0L, status.routeCount());
        assertTrue(status.routeCodes().isEmpty());
        verifyNoInteractions(itemMapper, routeProductMapper, routeMapper);
    }

    @Test
    void getStatus_routeOnlyDoesNotTouchBatchRecordLookups() {
        String projectName = "只查工艺路线";
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of());

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName),
                true, false, false).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_MISSING, status.routeStatus());
        assertEquals(0L, status.routeCount());
        verifyNoInteractions(definitionMapper, versionMapper, reportMapper);
    }

    @Test
    void getStatus_projectCodeToItemBinding_resolvesRouteRegardlessOfRouteName() {
        String projectName = "DCC 项目名称";
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(10L).projectName(projectName).projectCode("MAT-001").build();
        MesMdItemDO item = MesMdItemDO.builder().id(20L).code("MAT-001").name("MES 产品").build();
        MesProRouteProductDO binding = MesProRouteProductDO.builder().id(30L).itemId(20L).routeId(40L).build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(40L).code("RT-001").name("与 DCC 项目不同的路线名称").build();
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of(projectCode));
        when(itemMapper.selectListByCodes(List.of("MAT-001"))).thenReturn(List.of(item));
        when(routeProductMapper.selectListByItemIds(List.of(20L))).thenReturn(List.of(binding));
        when(routeMapper.selectBatchIds(List.of(40L))).thenReturn(List.of(route));
        stubBatchRecordLookups(projectName);

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName)).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_OK, status.routeStatus());
        assertEquals(1L, status.routeCount());
        assertEquals(List.of("RT-001"), status.routeCodes());
    }

    @Test
    void getStatus_projectCodeWithoutMesItem_reportsMissing() {
        String projectName = "缺少 MES 物料";
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of(
                DccProjectCodeDO.builder().id(11L).projectName(projectName).projectCode("MAT-MISSING").build()));
        when(itemMapper.selectListByCodes(List.of("MAT-MISSING"))).thenReturn(List.of());
        stubBatchRecordLookups(projectName);

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName)).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_MISSING, status.routeStatus());
        assertEquals(0L, status.routeCount());
        verifyNoInteractions(routeProductMapper, routeMapper);
    }

    @Test
    void getStatus_itemBoundToMultipleRoutes_reportsDuplicate() {
        String projectName = "重复路线绑定";
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of(
                DccProjectCodeDO.builder().id(12L).projectName(projectName).projectCode("MAT-002").build()));
        when(itemMapper.selectListByCodes(List.of("MAT-002"))).thenReturn(List.of(
                MesMdItemDO.builder().id(21L).code("MAT-002").build()));
        when(routeProductMapper.selectListByItemIds(List.of(21L))).thenReturn(List.of(
                MesProRouteProductDO.builder().id(31L).itemId(21L).routeId(41L).build(),
                MesProRouteProductDO.builder().id(32L).itemId(21L).routeId(42L).build()));
        when(routeMapper.selectBatchIds(List.of(41L, 42L))).thenReturn(List.of(
                MesProRouteDO.builder().id(41L).code("RT-002-A").name("路线甲").build(),
                MesProRouteDO.builder().id(42L).code("RT-002-B").name("路线乙").build()));
        stubBatchRecordLookups(projectName);

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName)).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_DUPLICATE, status.routeStatus());
        assertEquals(2L, status.routeCount());
        assertEquals(List.of("RT-002-A", "RT-002-B"), status.routeCodes());
        assertTrue(status.blockerMessages().stream()
                .anyMatch(message -> message.contains("工艺路线重复 2 份")));
    }

    private void stubBatchRecordLookups(String projectName) {
        when(definitionMapper.selectListByBatchRecordName(projectName)).thenReturn(List.of());
        when(reportMapper.selectListByBatchRecordNameAndFormSlotType(projectName, "LOSS_REPORT"))
                .thenReturn(List.of());
        when(reportMapper.selectListByBatchRecordNameAndFormSlotType(projectName, "PROCESS_INSPECTION"))
                .thenReturn(List.of());
        when(reportMapper.selectListByBatchRecordNameAndFormSlotType(projectName, "PARAMETER_RECORD"))
                .thenReturn(List.of());
    }
}
