package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.MesKingdeeProductionMaterialListController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderProductionMaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MesTeamLeaderActiveOrderProductionMaterialControllerTest {
    private static final String PATH = "/mes/pro/process-pool/team-leader/active-order/production-material-lists";

    @Test
    void clientCannotSpecifyDifferentOrderNumberOrUnboundedPagination() throws Exception {
        var service = mock(MesTeamLeaderActiveOrderProductionMaterialService.class);
        var row = new MesKingdeeProductionMaterialListRespVO();
        row.setProductionOrderNo("MO-409");
        when(service.getList(341L, 409L)).thenReturn(List.of(row));
        var mvc = MockMvcBuilders.standaloneSetup(new MesTeamLeaderActiveOrderProductionMaterialController(service)).build();
        try (var login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(341L);
            mvc.perform(get(PATH).param("activeOrderId", "409").param("productionOrderNo", "MO-OTHER")
                            .param("pageSize", "-1").param("leaderUserId", "342")
                            .param("sourceWorkOrderCode", "MO-OTHER"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data[0].productionOrderNo").value("MO-409"));
        }
        verify(service).getList(341L, 409L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void activeOrderIdIsRequiredSoFilterCannotBeRemoved() throws Exception {
        var service = mock(MesTeamLeaderActiveOrderProductionMaterialService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new MesTeamLeaderActiveOrderProductionMaterialController(service)).build();
        mvc.perform(get(PATH).param("productionOrderNo", "")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void mesEntryRequiresExistingQueryPermissionAndErpEntryKeepsItsOriginalPermission() throws Exception {
        var narrow = MesTeamLeaderActiveOrderProductionMaterialController.class.getMethod("getList", Long.class);
        assertEquals("@ss.hasPermission('mes:pro-process-pool-team-leader:query')",
                narrow.getAnnotation(PreAuthorize.class).value());
        assertEquals(1, narrow.getParameterCount());
        var erp = MesKingdeeProductionMaterialListController.class.getMethod("getPage", MesKingdeeProductionMaterialListPageReqVO.class);
        assertEquals("@ss.hasPermission('erp:production-material-list:query')", erp.getAnnotation(PreAuthorize.class).value());
    }
}
