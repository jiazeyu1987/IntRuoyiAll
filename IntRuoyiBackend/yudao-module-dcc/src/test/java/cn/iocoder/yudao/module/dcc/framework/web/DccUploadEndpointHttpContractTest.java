package cn.iocoder.yudao.module.dcc.framework.web;

import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileUploadService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTemporaryFileStatus;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DccUploadEndpointHttpContractTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileUploadService uploadService;
    @Mock
    private DccUploadTicketService uploadTicketService;
    @Mock
    private DccControlledFileAccessAuditService accessAuditService;
    @Mock
    private ApiErrorLogCommonApi apiErrorLogCommonApi;

    @InjectMocks
    private DccControlledFileController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(99L);
        loginUser.setTenantId(31L);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityFrameworkUtils.setLoginUser(loginUser, new MockHttpServletRequest());

        WebProperties webProperties = new WebProperties();
        webProperties.getAdminApi().setPrefix("");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new DccExplicitTenantRequestValidator(webProperties))
                .addFilters(new DccApiHttpContractFilter(webProperties))
                .setControllerAdvice(new GlobalExceptionHandler("dcc-test", apiErrorLogCommonApi))
                .build();
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void missingTenantHeader_blocksAllThreeHandlersWithoutSideEffects() throws Exception {
        mockMvc.perform(multipart("/dcc/controlled-files/upload-preview"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/dcc/controlled-files/upload-temporary/status").param("requestId", "REQ-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/dcc/controlled-files/upload-temporary/session-cleanup")
                        .contentType("application/json")
                        .content("{\"sessionId\":\"session-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(uploadService, uploadTicketService, accessAuditService);
    }

    @Test
    void statusEndpoint_matchingTenantReturnsSuccess() throws Exception {
        when(uploadTicketService.getTemporaryFileStatusByRequestId(99L, "REQ-1"))
                .thenReturn(new DccUploadTemporaryFileStatus("REQ-1", 1, true, "session-1", "SOURCE",
                        "AVAILABLE", LocalDateTime.now().plusMinutes(10), "ACTIVE", null, null));

        mockMvc.perform(get("/dcc/controlled-files/upload-temporary/status")
                        .header("tenant-id", "31")
                        .param("requestId", "REQ-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bindable").value(true));
    }

    @Test
    void targetEndpoints_mapParameterNotFoundConflictAndInternalFailures() throws Exception {
        mockMvc.perform(get("/dcc/controlled-files/upload-temporary/status").header("tenant-id", "31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        when(uploadTicketService.getTemporaryFileStatusByRequestId(99L, "REQ-MISSING"))
                .thenThrow(exception(CONTROLLED_FILE_NOT_EXISTS));
        mockMvc.perform(get("/dcc/controlled-files/upload-temporary/status")
                        .header("tenant-id", "31")
                        .param("requestId", "REQ-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CONTROLLED_FILE_NOT_EXISTS.getCode()));

        when(uploadService.uploadPreviewFile(anyLong(), any(), any()))
                .thenThrow(exception(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT));
        MockMultipartFile source = new MockMultipartFile("files", "sample.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx".getBytes());
        mockMvc.perform(multipart("/dcc/controlled-files/upload-preview")
                        .file(source)
                        .header("tenant-id", "31")
                        .header(DccControlledFileController.REQUEST_ID_HEADER, "REQ-CONFLICT")
                        .header("User-Agent", "JUnit")
                        .param("categoryId", "10")
                        .param("sessionId", "session-1")
                        .param("purpose", "SOURCE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(CONTROLLED_FILE_UPLOAD_SLOT_CONFLICT.getCode()));

        when(uploadTicketService.getTemporaryFileStatusByRequestId(99L, "REQ-FAIL"))
                .thenThrow(new IllegalStateException("status failed"));
        mockMvc.perform(get("/dcc/controlled-files/upload-temporary/status")
                        .header("tenant-id", "31")
                        .param("requestId", "REQ-FAIL"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
