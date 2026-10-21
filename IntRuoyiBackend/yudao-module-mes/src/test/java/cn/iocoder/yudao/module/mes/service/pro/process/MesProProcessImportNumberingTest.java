package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.*;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordRouteGenerationServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProProcessImportNumberingTest {
    private final MesProProcessMapper mapper = mock(MesProProcessMapper.class);
    private final MesMdAutoCodeRecordService codes = mock(MesMdAutoCodeRecordService.class);
    private final MesProProcessCreationService creator = new MesProProcessCreationService();

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(creator, "processMapper", mapper);
        ReflectionTestUtils.setField(creator, "autoCodeRecordService", codes);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void wordAndExcelCreateUseTheSameSequenceAndReuseExistingProcesses() throws Exception {
        when(codes.generateAutoCode("PRO_PROCESS_GX_CODE", null)).thenReturn("GX-0001", "GX-0002");
        var word = new MesProBatchRecordRouteGenerationServiceImpl();
        ReflectionTestUtils.setField(word, "processMapper", mapper);
        ReflectionTestUtils.setField(word, "processCreationService", creator);
        MesProProcessDO first = ReflectionTestUtils.invokeMethod(word, "resolveProcess", "Word process");
        assertEquals("GX-0001", first.getCode());

        var parser = mock(Sheet1RouteExcelParser.class);
        when(parser.parse(any(java.io.InputStream.class))).thenReturn(new Sheet1RouteExcelParser.ParseResult(
                List.of(new Sheet1RouteExcelParser.Route("Route", List.of("ITEM"), List.of(
                        new Sheet1RouteExcelParser.Step("Word process"), new Sheet1RouteExcelParser.Step("Excel process"))))));
        first.setId(10L);
        when(mapper.selectByName("Word process")).thenReturn(first);
        var routes = mock(MesProRouteMapper.class);
        var excel = new Sheet1RouteExcelImportServiceImpl(parser, routes, mapper,
                mock(MesProRouteProcessMapper.class), mock(MesProRouteProcessFlowEdgeMapper.class),
                mock(MesProRouteOwnerPermissionService.class));
        ReflectionTestUtils.setField(excel, "processCreationService", creator);
        var result = excel.importExcel(new MockMultipartFile("file", "routes.xlsx",
                "application/octet-stream", new byte[]{1}), 0);
        assertEquals(1, result.getProcessCreatedCount());
        assertEquals(1, result.getProcessReusedCount());
        ArgumentCaptor<MesProProcessDO> inserted = ArgumentCaptor.forClass(MesProProcessDO.class);
        verify(mapper, times(2)).insert(inserted.capture());
        assertEquals(List.of("GX-0001", "GX-0002"), inserted.getAllValues().stream().map(MesProProcessDO::getCode).toList());
        verify(codes, times(2)).generateAutoCode("PRO_PROCESS_GX_CODE", null);
    }

    @Test void createRequestIgnoresIncomingCodeWhileUpdateKeepsIt() throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var create = json.readValue("{\"name\":\"A\",\"status\":0,\"code\":\"OLD\",\"id\":12}",
                cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessCreateReqVO.class);
        assertEquals("A", create.getName());
        var update = json.readValue("{\"name\":\"A\",\"status\":0,\"code\":\"OLD\",\"id\":12}",
                cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO.class);
        assertEquals("OLD", update.getCode());
        assertEquals(12L, update.getId());
    }
}
