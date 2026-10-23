package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP identity/order contract; transaction effects are verified with real B/H2 in the adjacent test. */
class DccPublicUploadPlacementHttpContractTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileWorkflowService workflowService;
    @Mock DccPublicUploadPlacementService publicUploadPlacementService;
    @InjectMocks DccControlledFileController controller;

    @ParameterizedTest @ValueSource(strings={"submit", "working"})
    void exactLongFolderAndRelatedIdsReachSameActorOrchestrationBeforeCreation(String endpoint) throws Exception {
        if ("submit".equals(endpoint)) {
            when(workflowService.submitNewWithDerivedStorage(eq(99L), any(), any())).thenReturn(900L);
        } else {
            when(workflowService.createWorkingWithDerivedStorage(eq(99L), any(), any())).thenReturn(900L);
        }
        when(publicUploadPlacementService.create(eq(99L), any(), org.mockito.ArgumentMatchers.<java.util.function.Function<DccDerivedUploadStorage,Long>>any())).thenAnswer(invocation -> {
            verifyNoInteractions(workflowService);
            DccControlledFileSubmitReqVO request = invocation.getArgument(1);
            assertEquals(9007199254740993L, request.getProjectFolderId());
            assertEquals(9223372036854775807L, request.getRelatedControlledFileIds().get(0));
            assertEquals("Selected project location", request.getProjectFolderChangeReason());
            assertNull(request.getDirectoryId());assertFalse(request.isDirectoryIdProvided());
            return invocation.<java.util.function.Function<DccDerivedUploadStorage,Long>>getArgument(2).apply(
                new DccDerivedUploadStorage(1L,99L,5L,9007199254740993L,2L,1L,20L,21L,false));
        });
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/" + endpoint)
                    .contentType("application/json").content("""
                    {"categoryId":2,"dccProjectCodeId":"5","fileName":"SOP.pdf","fileNumber":"N-1",
                    "idempotencyKey":"real-upload-session","changeType":"NEW","needTraining":false,"versionNo":"A/1","effectiveDate":"2099-12-20",
                    "projectFolderId":"9007199254740993","projectFolderChangeReason":"Selected project location",
                    "relatedControlledFileIds":["9223372036854775807"]}
                    """))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(900));
        }
        verify(publicUploadPlacementService).create(eq(99L), any(), org.mockito.ArgumentMatchers.<java.util.function.Function<DccDerivedUploadStorage,Long>>any());
    }
}
