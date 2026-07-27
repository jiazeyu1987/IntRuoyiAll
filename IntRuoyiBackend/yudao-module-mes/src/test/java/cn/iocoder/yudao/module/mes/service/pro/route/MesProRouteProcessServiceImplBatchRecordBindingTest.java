package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteProcessServiceImplBatchRecordBindingTest {

    @InjectMocks
    private MesProRouteProcessServiceImpl routeProcessService;

    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProductService routeProductService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProRouteService routeService;

    @Test
    void createRouteProcess_ignoresHistoricalBatchRecordReportFieldOnBaseRouteSave() {
        MesProRouteProcessSaveReqVO reqVO = new MesProRouteProcessSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setProcessId(20L);
        reqVO.setSort(1);

        when(routeService.getRoute(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(processService.getProcess(20L)).thenReturn(MesProProcessDO.builder().id(20L).build());
        when(routeProcessMapper.selectByRouteIdAndSort(10L, 1)).thenReturn(null);
        when(routeProcessMapper.selectByRouteIdAndProcessId(10L, 20L)).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteProcessDO routeProcess = invocation.getArgument(0);
            routeProcess.setId(100L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        routeProcessService.createRouteProcess(reqVO);

        verify(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
    }

    @Test
    void createRouteProcess_shouldMaintainRouteVersionAfterProcessChainRebuild() {
        MesProRouteProcessSaveReqVO reqVO = new MesProRouteProcessSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setProcessId(20L);
        reqVO.setSort(1);
        reqVO.setKeyFlag(Boolean.FALSE);

        when(routeService.getRoute(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(processService.getProcess(20L)).thenReturn(MesProProcessDO.builder().id(20L).build());
        when(routeProcessMapper.selectByRouteIdAndSort(10L, 1)).thenReturn(null);
        when(routeProcessMapper.selectByRouteIdAndProcessId(10L, 20L)).thenReturn(null);
        doAnswer(invocation -> {
            MesProRouteProcessDO routeProcess = invocation.getArgument(0);
            routeProcess.setId(100L);
            return 1;
        }).when(routeProcessMapper).insert(any(MesProRouteProcessDO.class));

        routeProcessService.createRouteProcess(reqVO);

        verify(routeProcessMapper).insert(any(MesProRouteProcessDO.class));
        verify(routeService).maintainRouteVersionAfterProcessChange(10L);
        verify(routeService).ensureDefaultScheduleArtifacts(10L, 100L);
    }
}
