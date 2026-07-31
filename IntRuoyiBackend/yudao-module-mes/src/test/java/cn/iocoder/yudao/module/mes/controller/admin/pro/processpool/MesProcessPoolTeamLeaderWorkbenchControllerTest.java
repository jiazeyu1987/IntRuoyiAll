package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProcessPoolTeamLeaderWorkbenchControllerTest {

    @Test
    void shouldExposeTeamLeaderReadonlyEndpointsWithDedicatedPermission() throws NoSuchMethodException {
        RequestMapping classMapping = MesProProcessPoolTeamLeaderWorkbenchController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/process-pool/team-leader-workbench"}, classMapping.value());

        Method page = MesProProcessPoolTeamLeaderWorkbenchController.class.getMethod("getWorkbenchPage",
                cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-process-pool-team-leader:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method detail = MesProProcessPoolTeamLeaderWorkbenchController.class.getMethod("getWorkbenchDetail", Long.class);
        assertArrayEquals(new String[]{"/detail"}, detail.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-process-pool-team-leader:query')",
                detail.getAnnotation(PreAuthorize.class).value());
    }
}
