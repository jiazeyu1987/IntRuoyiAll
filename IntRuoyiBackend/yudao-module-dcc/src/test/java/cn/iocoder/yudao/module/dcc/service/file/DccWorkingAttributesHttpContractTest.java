package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Public write route delegates exact current actor/file/actual to A's formal draft authorization. */
class DccWorkingAttributesHttpContractTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileWorkflowService workflowService;
    @InjectMocks DccControlledFileController controller;

    @Test void saveCallsFormalDraftServiceWithActualValuesAndCurrentActor() throws Exception {
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/900/working-attributes")
                    .contentType("application/json").content("{\"targetMarkets\":[\"CE\"],\"licenseHolder\":\"N\",\"actualManufacturer\":\"Y\",\"documentTransfer\":\"N\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(true));
            verify(workflowService).saveWorkingApplicationAttributes(99L,900L,
                    new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null));
        }
    }

    @Test void emptyBodyCannotReportASavedApplication() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/900/working-attributes")
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(workflowService);
    }
    @Test void readAndRestoreUseExactFileIdentityAndReturnSavedSourceWithoutInventedDepartments() throws Exception {
        var actual=new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null);
        when(workflowService.readWorkingApplicationAttributes(99L,9007199254740993L))
                .thenReturn(new DccWorkingApplicationAttributes(9007199254740993L,9007199254740995L,"UPLOAD",actual,actual,
                        true,null,null,false,null,"draft"));
        try(var auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var mvc=MockMvcBuilders.standaloneSetup(controller).build();
            mvc.perform(get("/dcc/controlled-files/9007199254740993/working-attributes"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("9007199254740993"))
                    .andExpect(jsonPath("$.data.projectId").value("9007199254740995"))
                    .andExpect(jsonPath("$.data.defaultSource.targetMarkets[0]").value("CE"));
            mvc.perform(post("/dcc/controlled-files/9007199254740993/working-attributes/restore"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
            verify(workflowService).restoreWorkingApplicationAttributes(99L,9007199254740993L);
        }
    }
    @Test void replacementReadPassesExactLargeSourceAndBaselineWithActualCurrentActor() throws Exception {
        var actual=new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null);
        when(workflowService.readReplacementApplicationAttributes(99L,9007199254740993L,9007199254740995L))
                .thenReturn(new DccWorkingApplicationAttributes(9007199254740993L,2L,"REVISION",actual,actual,false,"CHECKED_OUT",null,false,null,"source"));
        try(var auth=mockStatic(SecurityFrameworkUtils.class)){
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/9007199254740993/replacement-attributes").param("controlledBaselineId","9007199254740995"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("9007199254740993")).andExpect(jsonPath("$.data.canSubmit").value(false));
        }
    }
}
