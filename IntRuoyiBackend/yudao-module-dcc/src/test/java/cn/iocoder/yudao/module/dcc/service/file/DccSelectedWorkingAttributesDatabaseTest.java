package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.*;

/** Actual earlier working body -> HTTP saved read -> formal candidate attributes on H2 and Flowable. */
class DccSelectedWorkingAttributesDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    long selected() {
        controlled("A/1");long earlier=checkin(20L,"earlier selection");
        workflow.saveWorkingApplicationAttributes(99L,earlier,ce);
        changeDefault();long later=checkin(earlier,"later working body");workflow.saveWorkingApplicationAttributes(99L,later,attrs("MADSAP"));
        if(Arrays.stream(DccControlledFileWorkflowServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("assignmentScopeService")))wire(workflow,"assignmentScopeService",scope);
        return earlier;
    }
    @Test void exactEarlierHttpReadAndCandidateFreezeUseTheSameSavedSourceAndActual() throws Exception {
        long earlier=selected();var controller=new DccControlledFileController();wire(controller,"workflowService",workflow);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/"+earlier+"/working-attributes")).andReturn();
            assertEquals(200,response.getResponse().getStatus());
            var body=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(),com.fasterxml.jackson.databind.JsonNode.class);
            assertEquals(0,body.get("code").intValue());assertEquals(earlier+"",body.get("data").get("controlledFileId").textValue());
            assertEquals("NMPA",body.get("data").get("defaultSource").get("targetMarkets").get(0).asText());
            assertEquals("CE",body.get("data").get("actual").get("targetMarkets").get(0).asText());
        }
        var read=workflow.readWorkingApplicationAttributes(99L,earlier);assertEquals(nmpa,read.defaultSource());assertEquals(ce,read.actual());
        var request=request("PARTIAL");request.setProjectAttributes(read.actual());long candidate=workflow.submitWorkingIteration(99L,earlier,request);
        var saved=attributeRows.find("REVISION",candidate,rounds.require("REVISION",candidate,files.selectById(candidate).getProcessInstanceId()));
        assertEquals(read.defaultSource(),attributes.readValue(saved.getDefaultSourceJson()));assertEquals(read.actual(),attributes.readValue(saved.getActualAttributesJson()));
    }
    @Test void submissionIneligibilityNeverHidesTheEarlierSavedDraft() {
        long earlier=selected();long latest=files.selectListByMasterId(10L).stream().filter(f->"A/1-2".equals(f.getVersionNo())).findFirst().orElseThrow().getId();
        checkout(latest);int before=count("dcc_project_application_attributes");
        var read=workflow.readWorkingApplicationAttributes(99L,earlier);assertFalse(read.canSubmit());assertNotNull(read.unavailableReason());
        assertEquals(nmpa,read.defaultSource());assertEquals(ce,read.actual());assertEquals(before,count("dcc_project_application_attributes"));
    }
    @ParameterizedTest @ValueSource(strings={"MASTER_PROJECT","MASTER_TYPE","NUMBER","CATEGORY","SCOPE"})
    void readableDraftStillRequiresExactIdentityAndHardScope(String violation) {
        long earlier=selected();
        switch(violation){
            case "MASTER_PROJECT" -> jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=999 WHERE id=10");
            case "MASTER_TYPE" -> jdbc.update("UPDATE dcc_controlled_file_master SET file_type_taxonomy_leaf_id=999 WHERE id=10");
            case "NUMBER" -> jdbc.update("UPDATE dcc_controlled_file SET file_number='FOREIGN' WHERE id=?",earlier);
            case "CATEGORY" -> jdbc.update("UPDATE dcc_controlled_file SET category_id=999 WHERE id=?",earlier);
            case "SCOPE" -> {var denied=mock(DccControlledFileAssignmentScopeService.class);if(Arrays.stream(DccControlledFileWorkflowServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("assignmentScopeService")))wire(workflow,"assignmentScopeService",denied);}
        }
        int before=count("dcc_project_application_attributes");assertThrows(RuntimeException.class,()->workflow.readWorkingApplicationAttributes(99L,earlier));
        assertEquals(before,count("dcc_project_application_attributes"));
    }
    @Test void explicitOwnerReplacementOfAnotherRequesterBodyHasIndependentCandidateAndExactSavedProvenance() {
        long earlier=selected();int round=rounds.requireDraft("REVISION",earlier);var source=attributeRows.find("REVISION",earlier,round);
        jdbc.update("UPDATE dcc_controlled_file SET requester_id=88,submitter_id=88 WHERE id=?",earlier);
        assertThrows(RuntimeException.class,()->workflow.readWorkingApplicationAttributes(99L,earlier));
        when(access.hasProjectOwner(99L,5L)).thenReturn(true);
        var req=request("REPLACEMENT");req.setProjectAttributes(ce);
        long candidate=workflow.submitWorkingIteration(99L,earlier,req);
        assertEquals("B/1",files.selectById(candidate).getVersionNo());assertEquals(99L,files.selectById(candidate).getRequesterId());
        assertEquals(88L,files.selectById(earlier).getRequesterId());assertEquals("WORKING",files.selectById(earlier).getStatus());assertNull(files.selectById(earlier).getProcessInstanceId());
        var frozen=attributeRows.find("REVISION",candidate,rounds.require("REVISION",candidate,files.selectById(candidate).getProcessInstanceId()));
        assertEquals(source.getDefaultSourceJson(),frozen.getDefaultSourceJson());assertEquals(source.getActualAttributesJson(),frozen.getActualAttributesJson());
        assertEquals(earlier,frozen.getSourceApplicationId());assertEquals(source.getDefaultSourceJson(),attributeRows.find("REVISION",earlier,round).getDefaultSourceJson());
    }
    @Test void ownerReplacementHttpReadsExactSavedBodyWhileOrdinaryDraftWritesStayForbidden() throws Exception {
        long earlier=selected();jdbc.update("UPDATE dcc_controlled_file SET requester_id=88,submitter_id=88 WHERE id=?",earlier);when(access.hasProjectOwner(99L,5L)).thenReturn(true);
        var controller=new DccControlledFileController();wire(controller,"workflowService",workflow);
        try(var login=mockStatic(SecurityFrameworkUtils.class)){
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/"+earlier+"/replacement-attributes").param("controlledBaselineId","20")).andReturn();
            var value=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(),com.fasterxml.jackson.databind.JsonNode.class);
            assertEquals(0,value.get("code").intValue());assertEquals("NMPA",value.get("data").get("defaultSource").get("targetMarkets").get(0).textValue());assertEquals("CE",value.get("data").get("actual").get("targetMarkets").get(0).textValue());
        }
        assertThrows(RuntimeException.class,()->workflow.saveWorkingApplicationAttributes(99L,earlier,fda));
        assertThrows(RuntimeException.class,()->workflow.restoreWorkingApplicationAttributes(99L,earlier));
        assertThrows(RuntimeException.class,()->workflow.submitWorkingIteration(99L,earlier,request("PARTIAL")));
        assertThrows(RuntimeException.class,()->workflow.readReplacementApplicationAttributes(99L,earlier,999L));
        when(access.hasProjectOwner(99L,5L)).thenReturn(false);
        assertThrows(RuntimeException.class,()->workflow.readReplacementApplicationAttributes(99L,earlier,20L));
        assertThrows(RuntimeException.class,()->workflow.submitWorkingIteration(99L,earlier,request("REPLACEMENT")));
        assertNull(files.selectById(earlier).getProcessInstanceId());
    }
}
