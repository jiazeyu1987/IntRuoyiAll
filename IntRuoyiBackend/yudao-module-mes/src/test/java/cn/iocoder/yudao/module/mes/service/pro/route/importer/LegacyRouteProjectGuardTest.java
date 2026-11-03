package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.*;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProjectGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LegacyRouteProjectGuardTest {
    @Test void markdownRejectsOccupiedProjectBeforeCreatingProcesses() {
        var routes=mock(MesProRouteMapper.class);
        var processes=mock(MesProProcessMapper.class);
        var guard=mock(MesProRouteProjectGuard.class);
        var parser=mock(IntGyRouteMarkdownParser.class);
        when(parser.parse(org.mockito.ArgumentMatchers.anyString())).thenReturn(new IntGyRouteMarkdownParser.ParseResult(List.of(
                new IntGyRouteMarkdownParser.Route("R1","流程1",List.of()))));
        var service=new IntGyRouteMarkdownImportServiceImpl(parser,routes,processes,
                mock(MesProRouteProcessMapper.class),mock(MesProRouteProcessFlowEdgeMapper.class),
                mock(MesProRouteOwnerPermissionService.class));
        ReflectionTestUtils.setField(service,"projectGuard",guard);
        doThrow(new IllegalStateException("已有工艺流程")).when(guard).requireAvailable(42L,null);
        assertThrows(IllegalStateException.class,()->service.importMarkdown("source",0,null,42L));
        verifyNoInteractions(routes,processes);
    }

    @Test void sheet1RejectsOccupiedProjectBeforeCreatingProcesses() throws Exception {
        var routes=mock(MesProRouteMapper.class);
        var processes=mock(MesProProcessMapper.class);
        var guard=mock(MesProRouteProjectGuard.class);
        var parser=mock(Sheet1RouteExcelParser.class);
        when(parser.parse(any(java.io.InputStream.class))).thenReturn(new Sheet1RouteExcelParser.ParseResult(List.of(
                new Sheet1RouteExcelParser.Route("流程1",List.of(),List.of()))));
        var service=new Sheet1RouteExcelImportServiceImpl(parser,routes,processes,
                mock(MesProRouteProcessMapper.class),mock(MesProRouteProcessFlowEdgeMapper.class),
                mock(MesProRouteOwnerPermissionService.class));
        ReflectionTestUtils.setField(service,"projectGuard",guard);
        doThrow(new IllegalStateException("已有工艺流程")).when(guard).requireAvailable(42L,null);
        var file=new MockMultipartFile("file","route.xlsx","application/octet-stream",new byte[]{1});
        assertThrows(IllegalStateException.class,()->service.importExcel(file,0,42L));
        verifyNoInteractions(routes,processes);
    }

    @Test void markdownCannotCreateTwoRoutesForOneProject() {
        var parser=mock(IntGyRouteMarkdownParser.class);
        when(parser.parse(org.mockito.ArgumentMatchers.anyString())).thenReturn(new IntGyRouteMarkdownParser.ParseResult(List.of(
                new IntGyRouteMarkdownParser.Route("R1","流程1",List.of()),
                new IntGyRouteMarkdownParser.Route("R2","流程2",List.of()))));
        var routes=mock(MesProRouteMapper.class);
        var processes=mock(MesProProcessMapper.class);
        var service=new IntGyRouteMarkdownImportServiceImpl(parser,routes,processes,
                mock(MesProRouteProcessMapper.class),mock(MesProRouteProcessFlowEdgeMapper.class),
                mock(MesProRouteOwnerPermissionService.class));
        var error=assertThrows(ServiceException.class,()->service.importMarkdown("source",0,null,42L));
        assertTrue(error.getMessage().contains("只能为一个项目导入一条工艺流程"));
        verifyNoInteractions(routes,processes);
    }
}
