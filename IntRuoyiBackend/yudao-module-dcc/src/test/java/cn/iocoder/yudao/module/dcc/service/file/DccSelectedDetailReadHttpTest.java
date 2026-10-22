package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DccSelectedDetailReadHttpTest extends BaseMockitoUnitTest {
    @Mock DccSelectedDetailReadService selectedDetailReader;
    @Mock DccControlledFileQueryService queryService;
    @InjectMocks DccControlledFileController controller;
    final Long id=9007199254740993L;
    DccControlledFileBodyReadContext context() {return new DccControlledFileBodyReadContext(
            DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,id.toString(),"A/1","REVISION","old-round","old-task");}
    @Test void realHttpDetailForwardsCanonicalContextAndSerializesExactLong() throws Exception {
        var response=new DccControlledFileRespVO();response.setId(id);response.setVersionNo("A/1");
        when(selectedDetailReader.read(99L,id,context())).thenReturn(response);
        try(var actor=mockStatic(SecurityFrameworkUtils.class)) {
            actor.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/"+id)
                    .param("bodyReadContext",context().wire())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id.toString())).andExpect(jsonPath("$.data.versionNo").value("A/1"));
        }
        verify(selectedDetailReader).read(99L,id,context());verifyNoInteractions(queryService);
    }
    @Test void duplicateMixedOrForeignContextNeverEntersEitherReader() {
        for(String invalid:java.util.List.of("duplicate","mixed","foreign","header")) {
            var request=new MockHttpServletRequest();
            request.addParameter("bodyReadContext",context().wire());
            if(invalid.equals("duplicate"))request.addParameter("bodyReadContext",context().wire());
            if(invalid.equals("mixed"))request.addParameter("taskId","old-task");
            if(invalid.equals("header"))request.addHeader(DccControlledFileBodyReadContext.HEADER,context().wire());
            Long selected=invalid.equals("foreign")?id+1:id;
            assertThrows(IllegalArgumentException.class,()->controller.readControlledFileDetail(selected,request));
        }
        verifyNoInteractions(selectedDetailReader,queryService);
    }
    @Test void plainTaskRouteUsesExactApplicationReaderWithoutReturningUnboundMetadata() {
        var request=new MockHttpServletRequest();request.addParameter("processInstanceId","old-round");request.addParameter("taskId","old-task");
        try(var actor=mockStatic(SecurityFrameworkUtils.class)) {
            actor.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            controller.readControlledFileDetail(id,request);
        }
        verify(selectedDetailReader).readApplication(99L,id,"old-round","old-task",null);verifyNoInteractions(queryService);
    }

    @Test void trainingWorkbenchRoundWithoutTaskUsesTheExactMetadataReader() throws Exception {
        var response=new DccControlledFileRespVO();response.setId(id);response.setVersionNo("A/1");
        response.setProcessInstanceId("training-round");response.setFileOwnerUserId(99L);response.setFileOwnerSignatureId(860L);
        when(selectedDetailReader.readApplication(99L,id,"training-round",null,null)).thenReturn(response);
        try(var actor=mockStatic(SecurityFrameworkUtils.class)) {
            actor.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/"+id)
                    .param("processInstanceId","training-round")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id.toString()))
                    .andExpect(jsonPath("$.data.processInstanceId").value("training-round"))
                    .andExpect(jsonPath("$.data.fileOwnerUserId").value("99"))
                    .andExpect(jsonPath("$.data.fileOwnerSignatureId").value("860"));
        }
        verify(selectedDetailReader).readApplication(99L,id,"training-round",null,null);
        verifyNoInteractions(queryService);
    }
}
