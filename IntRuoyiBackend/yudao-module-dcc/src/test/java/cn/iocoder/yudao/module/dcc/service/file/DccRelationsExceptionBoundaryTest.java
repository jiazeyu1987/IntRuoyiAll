package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.dcc.controller.admin.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Deliberately registers the actual highest-priority global advice before D's boundary. */
class DccRelationsExceptionBoundaryTest {
    MockMvc mvc;
    DccControlledFileRelatedFileService relations;
    DccProjectReferenceService references;
    DccRelationRemediationService remediation;
    @BeforeEach void setup(){
        var actor=new LoginUser();actor.setId(7L);actor.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
        relations=mock(DccControlledFileRelatedFileService.class);references=mock(DccProjectReferenceService.class);
        remediation=mock(DccRelationRemediationService.class);
        mvc=MockMvcBuilders.standaloneSetup(new DccFileRelationsController(relations),new DccProjectReferenceController(references),
                new DccRelationRemediationController(remediation,mock(DccRelationNotificationRecoveryService.class)))
                .setControllerAdvice(new GlobalExceptionHandler("dcc-d-boundary",mock(ApiErrorLogCommonApi.class))).build();
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void currentRelationsBusinessFailureIsVisibleWithGlobalAdviceRegisteredFirst() throws Exception {
        when(relations.getCurrentRelationView(7L,100L)).thenThrow(new DccRelationFailure("DCC_RELATION_CURRENT_SET_NOT_INITIALIZED"));
        mvc.perform(get("/dcc/file-relations/100/current")).andExpect(jsonPath("$.code").value(DccRelationErrorCodes.BUSINESS_FAILURE))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("DCC_RELATION_CURRENT_SET_NOT_INITIALIZED")));
    }
    @Test void referenceInputFailureIsVisibleWithGlobalAdviceRegisteredFirst() throws Exception {
        when(references.create(7L,2L,21L,100L,"引用原因")).thenThrow(new DccRelationInputFailure("DCC_REFERENCE_SELECTION_REQUIRED"));
        mvc.perform(post("/dcc/project-file-references").contentType("application/json")
                .content("{\"projectId\":2,\"folderId\":21,\"selectedFileId\":100,\"reason\":\"引用原因\"}"))
                .andExpect(jsonPath("$.code").value(DccRelationErrorCodes.BUSINESS_FAILURE))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("请先选择引用文件")));
    }
    @Test void arrangementContextFailureIsVisibleWithGlobalAdviceRegisteredFirst() throws Exception {
        when(remediation.listArrangements(7L,100L,"round-real")).thenThrow(new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN"));
        mvc.perform(get("/dcc/relation-remediation/arrangements").param("sourceFileId","100").param("applicationRound","round-real"))
                .andExpect(jsonPath("$.code").value(DccRelationErrorCodes.BUSINESS_FAILURE))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("DCC_RELATION_ARRANGEMENT_FORBIDDEN")));
    }
    @Test void unexpectedTechnicalFailureRetainsGlobalBoundaryAndDoesNotLeakItsCause() throws Exception {
        when(relations.getCurrentRelationView(7L,100L)).thenThrow(new IllegalStateException("PRIVATE_CONNECTION_DETAIL"));
        mvc.perform(get("/dcc/file-relations/100/current")).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_CONNECTION_DETAIL"))));
    }
}
