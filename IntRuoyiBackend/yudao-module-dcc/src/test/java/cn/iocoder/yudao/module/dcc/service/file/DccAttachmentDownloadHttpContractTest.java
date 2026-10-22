package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccAttachmentDownloadHttpContractTest {
    @Test void attachmentEndpointDelegatesExactFileAttachmentAndWarningAndExposesAuditHeaders() throws Exception {
        var controller=new DccControlledFileController();var query=mock(DccControlledFileQueryService.class);ReflectionTestUtils.setField(controller,"queryService",query);
        when(query.readAttachmentDownloadFile(eq(99L),eq(20L),eq(700L),eq(true),eq("request-1"),any(DccRequestAuditContext.class)))
                .thenReturn(new DccDownloadFileBinary("附件.json","application/json","{}".getBytes(StandardCharsets.UTF_8),"request-1","AE-real","a".repeat(64)));
        var request=new MockHttpServletRequest();request.setRemoteAddr("127.0.0.1");request.addHeader("User-Agent","unit-test");
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=controller.downloadControlledFileAttachment(20L,700L,true,"request-1",request);
            assertEquals("request-1",response.getHeaders().getFirst("X-DCC-Download-Request-Id"));assertEquals("AE-real",response.getHeaders().getFirst("X-DCC-Access-Event-Code"));
            assertEquals("a".repeat(64),response.getHeaders().getFirst("X-DCC-Plain-SHA256"));assertTrue(response.getHeaders().getFirst("Content-Disposition").startsWith("attachment"));
            var method=controller.getClass().getMethod("downloadControlledFileAttachment",Long.class,Long.class,Boolean.class,String.class,jakarta.servlet.http.HttpServletRequest.class);
            assertEquals("/{id:\\d+}/attachments/{attachmentId:\\d+}/download",method.getAnnotation(GetMapping.class).value()[0]);assertEquals("isAuthenticated()",method.getAnnotation(PreAuthorize.class).value());
        }
    }
}
