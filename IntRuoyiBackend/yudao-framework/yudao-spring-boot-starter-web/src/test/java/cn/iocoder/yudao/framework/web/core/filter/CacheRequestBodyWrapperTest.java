package cn.iocoder.yudao.framework.web.core.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheRequestBodyWrapperTest {

    private static final String BODY = "{\"reason\":\"授予角色与撤权\",\"name\":\"E2E分类\"}";

    @Test
    void readsUtf8JsonWithoutDependingOnWindowsDefaultCharset() throws Exception {
        assertBody(StandardCharsets.UTF_8, true);
    }

    @Test
    void usesUtf8ForJsonWithoutDeclaredEncoding() throws Exception {
        assertBody(StandardCharsets.UTF_8, false);
    }

    @Test
    void honorsExplicitRequestEncodingAndSupportsRepeatedReads() throws Exception {
        assertBody(StandardCharsets.UTF_16LE, true);
    }

    private void assertBody(Charset charset, boolean declared) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        if (declared) request.setCharacterEncoding(charset.name());
        request.setContent(BODY.getBytes(charset));
        CacheRequestBodyWrapper wrapper = new CacheRequestBodyWrapper(request);
        assertEquals(BODY, wrapper.getReader().readLine());
        assertEquals(BODY, wrapper.getReader().readLine());
        assertEquals(BODY.getBytes(charset).length, wrapper.getContentLength());
    }
}
