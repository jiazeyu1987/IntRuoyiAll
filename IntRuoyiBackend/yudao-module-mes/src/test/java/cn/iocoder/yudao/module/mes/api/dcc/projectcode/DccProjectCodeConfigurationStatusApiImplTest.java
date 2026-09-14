package cn.iocoder.yudao.module.mes.api.dcc.projectcode;

import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationQuery;
import cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatus;
import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationProjectStatusRespVO;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceService;
import cn.iocoder.yudao.module.mes.service.pro.dccprojectgovernance.MesProDccProjectGovernanceStatus;
import cn.iocoder.yudao.module.mes.service.qa.regulation.MesQaInspectionRegulationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccProjectCodeConfigurationStatusApiImplTest {

    @Mock
    private MesProDccProjectGovernanceService governanceService;
    @Mock
    private MesQaInspectionRegulationService qaRegulationService;
    @InjectMocks
    private DccProjectCodeConfigurationStatusApiImpl api;

    @Test
    void getStatusShouldKeepRouteBatchRecordAndQaIndependent() {
        List<DccProjectCodeConfigurationQuery> projects = List.of(
                new DccProjectCodeConfigurationQuery(1L, "P1"),
                new DccProjectCodeConfigurationQuery(2L, "P2"),
                new DccProjectCodeConfigurationQuery(3L, "P3"));
        when(governanceService.getStatus(List.of("P1", "P2", "P3"),
                true, true, false)).thenReturn(List.of(
                MesProDccProjectGovernanceStatus.builder()
                        .projectName("P1").routeStatus("OK").mainBatchRecordStatus("MISSING").build(),
                MesProDccProjectGovernanceStatus.builder()
                        .projectName("P2").routeStatus("MISSING").mainBatchRecordStatus("DUPLICATE").build(),
                MesProDccProjectGovernanceStatus.builder()
                        .projectName("P3").routeStatus("MISSING").mainBatchRecordStatus("MISSING").build()));
        when(qaRegulationService.getProjectStatuses(List.of(1L, 2L, 3L))).thenReturn(List.of(
                MesQaInspectionRegulationProjectStatusRespVO.builder()
                        .dccProjectCodeId(1L).configured(false).build(),
                MesQaInspectionRegulationProjectStatusRespVO.builder()
                        .dccProjectCodeId(2L).configured(false).build(),
                MesQaInspectionRegulationProjectStatusRespVO.builder()
                        .dccProjectCodeId(3L).configured(true).build()));

        Map<Long, DccProjectCodeConfigurationStatus> statuses = api.getStatus(projects);

        assertTrue(statuses.get(1L).routeConfigured());
        assertFalse(statuses.get(1L).mainBatchRecordConfigured());
        assertFalse(statuses.get(1L).qaRegulationConfigured());
        assertFalse(statuses.get(2L).routeConfigured());
        assertTrue(statuses.get(2L).mainBatchRecordConfigured());
        assertFalse(statuses.get(2L).qaRegulationConfigured());
        assertFalse(statuses.get(3L).routeConfigured());
        assertFalse(statuses.get(3L).mainBatchRecordConfigured());
        assertTrue(statuses.get(3L).qaRegulationConfigured());
    }

    @Test
    void getStatusShouldSkipQaLookupWhenOnlyRouteRequested() {
        List<DccProjectCodeConfigurationQuery> projects = List.of(
                new DccProjectCodeConfigurationQuery(1L, "P1", true, false, false));
        when(governanceService.getStatus(List.of("P1"), true, false, false)).thenReturn(List.of(
                MesProDccProjectGovernanceStatus.builder()
                        .projectName("P1").routeStatus("OK").mainBatchRecordStatus("MISSING").build()));

        Map<Long, DccProjectCodeConfigurationStatus> statuses = api.getStatus(projects);

        assertTrue(statuses.get(1L).routeConfigured());
        assertFalse(statuses.get(1L).mainBatchRecordConfigured());
        assertFalse(statuses.get(1L).qaRegulationConfigured());
        verify(qaRegulationService, never()).getProjectStatuses(any());
    }

    @Test
    void getStatusShouldSkipGovernanceLookupWhenOnlyQaRequested() {
        List<DccProjectCodeConfigurationQuery> projects = List.of(
                new DccProjectCodeConfigurationQuery(1L, "P1", false, false, true));
        when(qaRegulationService.getProjectStatuses(List.of(1L))).thenReturn(List.of(
                MesQaInspectionRegulationProjectStatusRespVO.builder()
                        .dccProjectCodeId(1L).configured(true).build()));

        Map<Long, DccProjectCodeConfigurationStatus> statuses = api.getStatus(projects);

        assertFalse(statuses.get(1L).routeConfigured());
        assertFalse(statuses.get(1L).mainBatchRecordConfigured());
        assertTrue(statuses.get(1L).qaRegulationConfigured());
        verify(governanceService, never()).getStatus(any(), anyBoolean(), anyBoolean(), anyBoolean());
    }
}
