package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProRouteProjectCreationTest {
    @InjectMocks private MesProRouteServiceImpl service;
    @Mock private MesProRouteProjectGuard projectGuard;
    @Mock private MesProRouteMapper routeMapper;
    private void occupied() {
        doThrow(invalidParamException("项目已存在工艺流程")).when(projectGuard).requireAvailable(10L, null);
    }
    @Test void manualCreateRejectsOccupiedProjectBeforeCreatingAnything() {
        occupied();
        MesProRouteCreateReqVO request = new MesProRouteCreateReqVO(); request.setDccProjectCodeId(10L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.createRouteWithProcesses(request));
        verifyNoInteractions(routeMapper);
    }
    @Test void workbookCreateRejectsOccupiedProjectBeforeCreatingAnything() {
        occupied();
        MesProRouteSaveReqVO request = new MesProRouteSaveReqVO(); request.setDccProjectCodeId(10L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.createRoute(request));
        verifyNoInteractions(routeMapper);
    }
    @Test void copyRejectsOccupiedProjectBeforeCopyingAnything() {
        occupied();
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.copyRoute(1L, "COPY", "副本", 10L));
        verifyNoInteractions(routeMapper);
    }
}
