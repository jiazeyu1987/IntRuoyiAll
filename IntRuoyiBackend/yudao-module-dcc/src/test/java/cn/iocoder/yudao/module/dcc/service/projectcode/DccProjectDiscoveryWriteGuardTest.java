package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeUpdateReqVO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual VIEW project reader must not gain write capability merely because an update menu is present. */
class DccProjectDiscoveryWriteGuardTest extends DccProjectDiscoveryReadTest {
    @jakarta.annotation.Resource
    cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService access;

    @Test void projectCodeAdministratorCanSaveStorageLocationWithoutProjectFileEditAccess() throws Exception {
        var project = project("METADATA-ADMIN", 7L);
        when(permissions.hasAnyRoles(8L, "dcc_project_code_admin")).thenReturn(true);
        when(permissions.hasAnyPermissions(8L, "dcc:project-code:update")).thenReturn(true);
        discoveryMvc().perform(put("/dcc/project-codes/update").contentType("application/json")
                        .content("{\"id\":\"" + project.getId() + "\",\"projectName\":\"METADATA-ADMIN\","
                                + "\"projectCode\":\"METADATA-ADMIN\",\"storageLocation\":\"A-02\",\"status\":\"ENABLE\"}"))
                .andExpect(jsonPath("$.code").value(0));
        assertEquals("A-02", projects.selectById(project.getId()).getStorageLocation());
        assertFalse(access.hasProjectEditorOrOwner(8L, project.getId()));
        assertFalse(access.hasProjectOwner(8L, project.getId()));
    }
    @Test void discoveryWriteMetadataViewCannotEditProjectWithUpdateMenu() throws Exception {
        var project=project("DISCOVERY-VIEW-WRITE",7L);view(project.getId());
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:update")).thenReturn(true);
        String before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(projects.selectById(project.getId()));
        discoveryMvc().perform(put("/dcc/project-codes/update").contentType("application/json")
                        .content("{\"id\":\""+project.getId()+"\",\"projectName\":\"unwritten-name\",\"projectCode\":\"UNWRITTEN\",\"status\":\"ENABLE\"}"))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
        assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(projects.selectById(project.getId())));
    }
    @Test void discoveryWriteConfigurationViewCannotReplaceDefaultsOrLeaderWithUpdateMenu() throws Exception {
        var project=project("DISCOVERY-VIEW-CONFIG",7L);view(project.getId());
        policy("dcc.project-attributes.configure");
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:update")).thenReturn(true);
        String before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(projects.selectById(project.getId()));
        discoveryMvc().perform(put("/dcc/project-codes/{id}/attributes/configuration",project.getId()).contentType("application/json")
                        .content("{\"projectLeaderUserId\":7,\"defaultAttributes\":{\"targetMarkets\":[\"FDA\"],\"licenseHolder\":\"Y\",\"actualManufacturer\":\"N\",\"documentTransfer\":\"N\"},\"changeReason\":\"explicit denied config\"}"))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
        assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(projects.selectById(project.getId())));
    }
    @Test void discoveryWriteViewCannotDeleteEvenEmptyProjectWithDeleteMenu() throws Exception {
        var project=project("DISCOVERY-VIEW-DELETE",7L);view(project.getId());
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:delete")).thenReturn(true);
        discoveryMvc().perform(delete("/dcc/project-codes/delete").param("id",String.valueOf(project.getId())))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
        assertNotNull(projects.selectById(project.getId()));
    }
}
