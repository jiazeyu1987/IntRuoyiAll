package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DccControlledFileBodyReadHttpContractTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileQueryService query;
    @InjectMocks DccControlledFileController controller;
    private DccControlledFileBodyReadContext context() {
        return new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                "910","A/1-2","REVISION","round-exact","task-exact");
    }
    @Test void realMetadataAndBinaryResourcesForwardTheSameExactContext() throws Exception {
        var context=context();
        when(query.getPreviewMetadata(eq(99L),eq(910L),eq(context),any())).thenReturn(
                new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO());
        when(query.readPreviewFile(eq(99L),eq(910L),eq(context),eq("token"),eq("event"),eq("trace"),eq("id"),eq("nonce"),any()))
                .thenReturn(new DccControlledFileBinary("selected.pdf","application/pdf",new byte[]{1,2,3},
                        cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build()));
        try(var auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var mvc=MockMvcBuilders.standaloneSetup(controller).build();
            mvc.perform(get("/dcc/controlled-files/910/preview-metadata").header("User-Agent","G07-isolated-HTTP-contract")
                    .header("X-DCC-Request-Id","G07-meta").param("bodyReadContext",context.wire())).andExpect(status().isOk());
            mvc.perform(get("/dcc/controlled-files/910/preview").header(DccControlledFileBodyReadContext.HEADER,context.wire())
                    .header("User-Agent","G07-isolated-HTTP-contract").header("X-DCC-Request-Id","G07-binary")
                    .header("X-DCC-Viewer-Token","token").header("X-DCC-Access-Event-Code","event")
                    .header("X-DCC-Watermark-Trace-Code","trace").header("X-DCC-Viewer-Token-Id","id")
                    .header("X-DCC-Viewer-Token-Nonce","nonce")).andExpect(status().isOk());
        }
        verify(query).getPreviewMetadata(eq(99L),eq(910L),eq(context),any());
        verify(query).readPreviewFile(eq(99L),eq(910L),eq(context),eq("token"),eq("event"),eq("trace"),eq("id"),eq("nonce"),any());
    }
    @Test void defaultIsDailyAndDuplicateOrMixedTransportFailsBeforeService() {
        var empty=new MockHttpServletRequest();
        assertEquals(DccControlledFileBodyReadContext.daily(910L),ReflectionTestUtils.invokeMethod(controller,"bodyReadRequest",910L,empty,false));
        var duplicate=new MockHttpServletRequest();duplicate.addParameter("bodyReadContext",context().wire(),context().wire());
        assertThrows(IllegalArgumentException.class,()->ReflectionTestUtils.invokeMethod(controller,"bodyReadRequest",910L,duplicate,false));
        var mixed=new MockHttpServletRequest();mixed.addParameter("distributionId","2");mixed.addParameter("recipientId","3");
        mixed.addHeader(DccControlledFileBodyReadContext.HEADER,context().wire());
        assertThrows(IllegalArgumentException.class,()->ReflectionTestUtils.invokeMethod(controller,"distributionPreviewTask",mixed));
        verifyNoInteractions(query);
    }
}
