package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Controller route/serialization contract; Query authorization/persistence has separate H2 coverage. */
class DccApplicationReadHttpContractTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileQueryService queryService;
    @InjectMocks DccControlledFileController controller;

    @Test void revisionOptionsUsesCurrentActorAndExactSelectedBaselineWithSafeLongResponse() throws Exception {
        var options=new DccControlledFileRevisionOptions(900L,"B/1",800L,"SOP.pdf",
                new DccControlledFileRevisionOptions.Target("B/2",null),
                new DccControlledFileRevisionOptions.Target("C/1",null),null,null,null,null,
                List.of(new DccControlledFileRevisionOptions.Iteration(9223372036854775000L,"B/1-2",true,false,false)));
        when(queryService.getRevisionOptions(99L,900L)).thenReturn(options);
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/revision-options"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.controlledBaselineId").value("900"))
                    .andExpect(jsonPath("$.data.iterations[0].id").value("9223372036854775000"));
            verify(queryService).getRevisionOptions(99L,900L);
        }
    }

    @Test void frozenEvidenceReadsExplicitTypeAndOpaqueRoundWithoutBorrowingApplicant() throws Exception {
        var evidence=new DccControlledFileApplicationEvidence(900L,"B/1","OBSOLETE","real-obsolete-round",2,
                false,"NOT_RECORDED",null,null,List.of());
        when(queryService.getApplicationEvidence(99L,900L,"OBSOLETE","real-obsolete-round")).thenReturn(evidence);
        try(MockedStatic<SecurityFrameworkUtils> auth=mockStatic(SecurityFrameworkUtils.class)) {
            auth.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/application-evidence")
                    .param("applicationType","OBSOLETE").param("bpmRound","real-obsolete-round"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.controlledFileId").value("900"))
                    .andExpect(jsonPath("$.data.bpmRound").value("real-obsolete-round"))
                    .andExpect(jsonPath("$.data.unavailableReason").value("NOT_RECORDED"))
                    .andExpect(jsonPath("$.data.recorded").value(false));
            verify(queryService).getApplicationEvidence(99L,900L,"OBSOLETE","real-obsolete-round");
        }
    }

    @Test void evidenceCannotCallQueryWhenTheFormalRoundIsOmitted() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/dcc/controlled-files/900/application-evidence")
                .param("applicationType","REVISION"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(queryService);
    }
}
