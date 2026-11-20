package cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordDefinitionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
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
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MesProDccProjectGovernanceServiceImplTest {

    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;
    @Mock
    private MesRouteDccProjectBindingMapper routeProjectBindingMapper;
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
        verifyNoInteractions(routeProjectBindingMapper, routeMapper);
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
    void getStatus_formalProjectBindingWithoutMatchingItem_resolvesRouteAndActiveVersion() {
        String projectName = "DCC 项目名称";
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .id(10L).projectName(projectName).projectCode("IDE").build();
        MesRouteDccProjectBindingDO binding = MesRouteDccProjectBindingDO.builder()
                .id(30L).dccProjectCodeId(10L).routeId(40L).build();
        MesProRouteDO route = MesProRouteDO.builder()
                .id(40L).code("RT-001").name("与 DCC 项目不同的路线名称").build();
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of(projectCode));
        when(routeProjectBindingMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(binding));
        when(routeVersionMapper.selectActiveByRouteId(40L)).thenReturn(
                MesProRouteVersionDO.builder().id(50L).routeId(40L).versionNo("V3").active(true).build());
        when(routeMapper.selectBatchIds(List.of(40L))).thenReturn(List.of(route));
        stubBatchRecordLookups(projectName);

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName)).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_OK, status.routeStatus());
        assertEquals(1L, status.routeCount());
        assertEquals(List.of("RT-001"), status.routeCodes());
        assertEquals(List.of("V3"), status.routeVersionNos());
    }

    @Test
    void getStatus_projectWithoutFormalBinding_reportsMissing() {
        String projectName = "缺少 MES 物料";
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of(
                DccProjectCodeDO.builder().id(11L).projectName(projectName).projectCode("MAT-MISSING").build()));
        when(routeProjectBindingMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of());
        stubBatchRecordLookups(projectName);

        MesProDccProjectGovernanceStatus status = service.getStatus(List.of(projectName)).get(0);

        assertEquals(MesProDccProjectGovernanceServiceImpl.STATUS_MISSING, status.routeStatus());
        assertEquals(0L, status.routeCount());
        verifyNoInteractions(routeMapper);
    }

    @Test
    void getStatus_projectBoundToMultipleRoutes_reportsDuplicate() {
        String projectName = "重复路线绑定";
        when(dccProjectCodeMapper.selectEnabledListByProjectName(projectName)).thenReturn(List.of(
                DccProjectCodeDO.builder().id(12L).projectName(projectName).projectCode("MAT-002").build()));
        when(routeProjectBindingMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(
                MesRouteDccProjectBindingDO.builder().id(31L).dccProjectCodeId(12L).routeId(41L).build(),
                MesRouteDccProjectBindingDO.builder().id(32L).dccProjectCodeId(12L).routeId(42L).build()));
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
