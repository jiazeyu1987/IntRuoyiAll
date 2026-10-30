package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesDvMachineryWorkbookServiceTest {
    @InjectMocks private MesDvMachineryWorkbookService service;
    @Mock private MesMdWorkshopMapper workshopMapper;
    @Mock private MesMdWorkshopService workshopService;
    @Mock private MesDvMachineryService machineryService;
    @Mock private SecurityFrameworkService security;
    @Mock private MesDvMachineryTypeWorkbookService typeWorkbookService;

    private MockMultipartFile workbook() throws Exception {
        MesDvWorkshopExcelVO workshop = new MesDvWorkshopExcelVO();
        workshop.setCode("WS-1"); workshop.setName("一车间"); workshop.setStatus(0);
        MesDvMachineryRespVO device = new MesDvMachineryRespVO();
        device.setCode("EQ-1"); device.setName("设备"); device.setMachineryTypeCode("TYPE");
        device.setWorkshopCode("WS-1"); device.setWorkshopName("一车间");
        MesDvMachineryTypeExcelVO type = new MesDvMachineryTypeExcelVO();
        type.setCode("TYPE"); type.setName("设备类型"); type.setStatus(0); type.setSort(0);
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.write(response, MesDvMachineryRespVO.class, List.of(device), List.of(workshop), List.of(type));
        return new MockMultipartFile("file", "设备.xlsx", "application/octet-stream", response.getContentAsByteArray());
    }

    @Test void roundTripCreatesWorkshopBeforeImportingDevice() throws Exception {
        when(security.hasPermission("mes:md-workshop:create")).thenReturn(true);
        when(machineryService.importMachineryList(anyList(), eq(false))).thenReturn(
                MesDvMachineryImportRespVO.builder().createCodes(List.of("EQ-1"))
                        .updateCodes(List.of()).failureCodes(Map.of()).build());
        var result = service.read(workbook(), false);
        assertEquals(1, result.getWorkshopCreateCount());
        InOrder order = inOrder(typeWorkbookService, workshopService, machineryService);
        order.verify(typeWorkbookService).importTypes(argThat(rows -> rows.size() == 1 && "TYPE".equals(rows.get(0).getCode())), eq(false));
        order.verify(workshopService).createWorkshop(argThat(row -> "WS-1".equals(row.getCode())));
        order.verify(machineryService).importMachineryList(argThat(rows -> rows.size() == 1
                && "WS-1".equals(rows.get(0).getWorkshopCode())
                && "TYPE".equals(rows.get(0).getMachineryTypeCode())), eq(false));
    }

    @Test void missingWorkshopPermissionStopsWrites() throws Exception {
        assertThrows(AccessDeniedException.class, () -> service.read(workbook(), false));
        verifyNoInteractions(workshopService, machineryService);
    }

    @Test void deviceFailureIsNotReportedAsSuccessfulWorkbookImport() throws Exception {
        when(security.hasPermission("mes:md-workshop:create")).thenReturn(true);
        when(machineryService.importMachineryList(anyList(), eq(false))).thenReturn(
                MesDvMachineryImportRespVO.builder().failureCodes(Map.of("EQ-1", "设备类型不存在")).build());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.read(workbook(), false));
    }

    @Test void existingWorkshopCanBeReusedWithoutWritePermission() throws Exception {
        when(workshopMapper.selectByCode("WS-1")).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO.builder()
                        .id(10L).code("WS-1").name("一车间").status(0).build());
        when(machineryService.importMachineryList(anyList(), eq(false))).thenReturn(
                MesDvMachineryImportRespVO.builder().failureCodes(Map.of()).build());
        assertEquals(1, service.read(workbook(), false).getWorkshopReuseCount());
        verifyNoInteractions(workshopService, security);
    }

    @Test void existingWorkshopUpdateUsesExistingId() throws Exception {
        when(workshopMapper.selectByCode("WS-1")).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO.builder()
                        .id(10L).code("WS-1").name("旧车间").status(0).build());
        when(security.hasPermission("mes:md-workshop:update")).thenReturn(true);
        when(machineryService.importMachineryList(anyList(), eq(true))).thenReturn(
                MesDvMachineryImportRespVO.builder().failureCodes(Map.of()).build());
        assertEquals(1, service.read(workbook(), true).getWorkshopUpdateCount());
        verify(workshopService).updateWorkshop(argThat(row -> Long.valueOf(10L).equals(row.getId())
                && "一车间".equals(row.getName())));
    }

    @Test void existingWorkshopConflictRequiresExplicitUpdate() throws Exception {
        when(workshopMapper.selectByCode("WS-1")).thenReturn(
                cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO.builder()
                        .id(10L).code("WS-1").name("旧车间").status(0).build());
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.read(workbook(), false));
        verifyNoInteractions(workshopService, machineryService);
    }
}
