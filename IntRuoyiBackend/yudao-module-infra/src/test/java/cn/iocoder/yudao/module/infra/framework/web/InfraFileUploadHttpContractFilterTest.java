package cn.iocoder.yudao.module.infra.framework.web;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_UPLOAD_EXECUTABLE_BLOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InfraFileUploadHttpContractFilterTest {

    private final InfraFileUploadHttpContractFilter filter =
            new InfraFileUploadHttpContractFilter(new WebProperties());

    @Test
    void doFilter_mapsExecutableRejectionToBadRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/infra/file/upload");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ServletUtils.writeJSON((HttpServletResponse) servletResponse,
                        CommonResult.error(FILE_UPLOAD_EXECUTABLE_BLOCKED, "installer.exe")));

        assertEquals(400, response.getStatus());
    }

    @Test
    void doFilter_doesNotChangeSuccessOrNonTargetLegacyResponse() throws Exception {
        assertStatus("/admin-api/infra/file/upload", CommonResult.success("ok"), 200);
        assertStatus("/admin-api/infra/file/create", CommonResult.error(BAD_REQUEST), 200);
    }

    private void assertStatus(String path, CommonResult<?> result, int expectedStatus) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ServletUtils.writeJSON((HttpServletResponse) servletResponse, result));
        assertEquals(expectedStatus, response.getStatus());
    }
}
