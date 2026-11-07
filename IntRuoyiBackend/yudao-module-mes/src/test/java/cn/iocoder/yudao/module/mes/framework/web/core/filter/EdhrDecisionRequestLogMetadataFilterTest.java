package cn.iocoder.yudao.module.mes.framework.web.core.filter;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrReleaseController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrNonconformanceReviewController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdhrDecisionRequestLogMetadataFilterTest {
    @Test
    @SuppressWarnings("unchecked")
    void decisionRequestsHaveCanonicalMetadataBeforeLoggingAndAuthentication() throws Exception {
        ObjectProvider<MesProEdhrReleaseController> releases = mock(ObjectProvider.class);
        ObjectProvider<MesProEdhrNonconformanceReviewController> reviews = mock(ObjectProvider.class);
        var release = mock(MesProEdhrReleaseController.class);
        var review = mock(MesProEdhrNonconformanceReviewController.class);
        when(releases.getObject()).thenReturn(release);
        when(reviews.getObject()).thenReturn(review);
        var filter = new EdhrDecisionRequestLogMetadataFilter(new WebProperties(), releases, reviews);
        for (String path : new String[]{"/mes/pro/edhr-release/reject", "/mes/pro/edhr-nonconformance-review/dispose"}) {
            for (String method : new String[]{"POST", "GET"}) {
                var request = new MockHttpServletRequest(method, "/admin-api" + path);
                request.setQueryString("password=PRIVATE_TEST_VALUE");
                filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                    assertTrue(isRequestLoggingDisabled(request));
                    assertEquals("/admin-api" + path, getLogRequestUri(request));
                });
            }
        }
        var unrelated = new MockHttpServletRequest("GET", "/admin-api/mes/pro/edhr-release/get");
        filter.doFilter(unrelated, new MockHttpServletResponse(), (req, res) -> {
            assertFalse(isRequestLoggingDisabled(unrelated));
            assertNull(unrelated.getAttribute(ATTRIBUTE_HANDLER_METHOD));
        });
        verifyNoInteractions(release, review);
    }
}
