package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import({Sheet1RouteExcelImportServiceImpl.class, Sheet1RouteExcelParser.class})
class Sheet1RouteExcelImportServiceImplDbTest extends BaseDbUnitTest {

    private static final Path FIXTURE = Path.of("D:\\ocr2\\resource\\球囊扩张导管工序(1).xlsx");

    @Resource
    private Sheet1RouteExcelImportService importService;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProProcessMapper processMapper;

    @MockitoBean
    private MesProRouteProcessMapper routeProcessMapper;
    @MockitoBean
    private MesProRouteOwnerPermissionService routeOwnerPermissionService;

    @Test
    void importWhenRouteProcessInsertFails_rollsBackCreatedRoutesAndProcesses() throws Exception {
        when(routeProcessMapper.insert(any(MesProRouteProcessDO.class)))
                .thenThrow(new RuntimeException("route process insert failed"));

        MockMultipartFile file = new MockMultipartFile("file", "球囊扩张导管工序(1).xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Files.readAllBytes(FIXTURE));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> importService.importExcel(file, CommonStatusEnum.ENABLE.getStatus()));

        assertEquals("route process insert failed", exception.getMessage());
        assertEquals(0L, routeMapper.selectCount());
        assertEquals(0L, processMapper.selectCount());
    }
}
