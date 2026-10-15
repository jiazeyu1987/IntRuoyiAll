package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkingApplicationSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSubmissionValidationGroup;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
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
                    .contentType("application/json").content("{\"actual\":{\"targetMarkets\":[\"CE\"],\"licenseHolder\":\"N\",\"actualManufacturer\":\"Y\",\"documentTransfer\":\"N\"},\"effectiveMode\":\"AFTER_STANDARDIZATION_SIGNATURE\",\"effectiveDate\":null,\"selectedSignoffDepartmentIds\":[\"8\"],\"selectedDistributionDepartmentIds\":[\"9\"],\"needTraining\":true,\"changeDescription\":\"本次草稿\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(true));
            var request=org.mockito.ArgumentCaptor.forClass(DccWorkingApplicationSaveReqVO.class);
            verify(workflowService).saveWorkingApplication(eq(99L),eq(900L),request.capture());
            assertEquals(new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null),request.getValue().getActual());
            assertEquals("AFTER_STANDARDIZATION_SIGNATURE",request.getValue().getEffectiveMode());
            assertNull(request.getValue().getEffectiveDate());
            assertEquals(List.of(8L),request.getValue().getSelectedSignoffDepartmentIds());
            assertEquals(List.of(9L),request.getValue().getSelectedDistributionDepartmentIds());
            assertTrue(request.getValue().getNeedTraining());
        }
    }

    @Test void emptyBodyCannotReportASavedApplication() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/900/working-attributes")
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(workflowService);
    }
    @Test void unfilledModeIsADraftChoiceButCannotBeSubmitted() {
        var input=new DccControlledFileSubmitReqVO();
        input.setCategoryId(1L);input.setIdempotencyKey("draft-1");input.setChangeType("NEW");
        input.setFileName("SOP.pdf");input.setFileNumber("SOP-1");input.setVersionNo("A/1");
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(input).isEmpty());
            assertFalse(factory.getValidator().validate(input,DccSubmissionValidationGroup.class).isEmpty());
            input.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");
            assertTrue(factory.getValidator().validate(input,DccSubmissionValidationGroup.class).isEmpty());
            input.setEffectiveDate(java.time.LocalDate.of(2026,10,11));
            assertFalse(factory.getValidator().validate(input,DccSubmissionValidationGroup.class).isEmpty());
        }
    }
    @Test void externalReviewRetainsItsExistingRequiredDateWithoutInventingANormalEffectiveMode() {
        var input=new DccControlledFileSubmitReqVO();
        input.setCategoryId(1L);input.setIdempotencyKey("external-1");input.setChangeType("NEW");
        input.setProcessType("EXTERNAL_REVIEW");input.setFileName("External.pdf");input.setFileNumber("EXT-1");input.setVersionNo("V1.0");
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(input).isEmpty());
            input.setEffectiveDate(java.time.LocalDate.of(2026,10,11));
            assertTrue(factory.getValidator().validate(input).isEmpty());
        }
    }
    @Test void readAndRestoreUseExactFileIdentityAndReturnSavedSourceWithoutInventedDepartments() throws Exception {
        var actual=new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null);
        when(workflowService.readWorkingApplicationAttributes(99L,9007199254740993L))
                .thenReturn(new DccWorkingApplicationAttributes(9007199254740993L,9007199254740995L,"UPLOAD",actual,actual,
                        false,"EFFECTIVE_SELECTION_INCOMPLETE",null,false,null,"draft",null,null));
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
                .thenReturn(new DccWorkingApplicationAttributes(9007199254740993L,2L,"REVISION",actual,actual,false,"CHECKED_OUT",null,false,null,"source",null,null));
        try(var auth=mockStatic(SecurityFrameworkUtils.class)){
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/9007199254740993/replacement-attributes").param("controlledBaselineId","9007199254740995"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("9007199254740993")).andExpect(jsonPath("$.data.canSubmit").value(false));
        }
    }
}
