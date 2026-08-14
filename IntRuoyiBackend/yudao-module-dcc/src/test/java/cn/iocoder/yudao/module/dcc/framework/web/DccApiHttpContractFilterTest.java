package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DccApiHttpContractFilterTest {

    private final DccApiHttpContractFilter filter = new DccApiHttpContractFilter(new WebProperties());

    @Test
    void doFilter_mapsTargetFailureEnvelopeToHttpStatusAndPreservesBusinessCode() throws Exception {
        assertStatus(CommonResult.error(UNAUTHORIZED), 401);
        assertStatus(CommonResult.error(FORBIDDEN), 403);
        assertStatus(CommonResult.error(BAD_REQUEST), 400);
        assertStatus(CommonResult.error(CONTROLLED_FILE_NOT_EXISTS), 404);
        assertStatus(CommonResult.error(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT), 409);
        assertStatus(CommonResult.error(INTERNAL_SERVER_ERROR), 500);
    }

    @Test
    void doFilter_keepsTargetSuccessAndNonTargetLegacyProtocolUnchanged() throws Exception {
        assertStatus(CommonResult.success("ok"), 200);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/system/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ServletUtils.writeJSON((HttpServletResponse) servletResponse, CommonResult.error(BAD_REQUEST)));

        assertEquals(200, response.getStatus());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void doFilter_nonPositiveTenantRejectedByTenantFilterBecomesBadRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/admin-api/dcc/controlled-files/upload-preview");
        request.addHeader("tenant-id", "-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ServletUtils.writeJSON((HttpServletResponse) servletResponse, CommonResult.error(FORBIDDEN)));

        assertEquals(400, response.getStatus());
        CommonResult body = JsonUtils.parseObject(response.getContentAsString(StandardCharsets.UTF_8), CommonResult.class);
        assertEquals(BAD_REQUEST.getCode(), body.getCode());
    }

    @Test
    void doFilter_mapsWorkflowPostRequiredFailureToHttpBadRequest() throws Exception {
        assertStatus("/admin-api/dcc/controlled-files/900/approve-task",
                CommonResult.error(CONTROLLED_FILE_APPROVER_POST_REQUIRED), 400);
    }

    @Test
    void doFilter_mapsTaskReadinessRouteMismatchToHttpBadRequest() throws Exception {
        assertStatus("/admin-api/dcc/controlled-files/900/task-action-readiness",
                CommonResult.error(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH), 400);
    }

    @SuppressWarnings("rawtypes")
    private void assertStatus(CommonResult<?> result, int expectedStatus) throws Exception {
        assertStatus("/admin-api/dcc/controlled-files/upload-preview", result, expectedStatus);
    }

    @SuppressWarnings("rawtypes")
    private void assertStatus(String path, CommonResult<?> result, int expectedStatus) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ServletUtils.writeJSON((HttpServletResponse) servletResponse, result));

        assertEquals(expectedStatus, response.getStatus());
        CommonResult body = JsonUtils.parseObject(response.getContentAsString(StandardCharsets.UTF_8), CommonResult.class);
        assertEquals(result.getCode(), body.getCode());
    }
}
