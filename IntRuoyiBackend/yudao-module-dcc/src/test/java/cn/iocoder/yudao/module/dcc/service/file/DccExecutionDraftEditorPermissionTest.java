package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccExecutionDraftEditorPermissionTest extends BaseMockitoUnitTest {
    @Mock DccProjectAccessService projectAccess;
    @Mock DccControlledFileCategoryPermissionSupport categoryPermissions;
    @InjectMocks DccApplicationExecutionContractService contracts;
    @BeforeEach void tenant(){TenantContextHolder.setTenantId(1L);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    private DccControlledFileDO working(){return DccControlledFileDO.builder().id(20L).tenantId(1L)
            .masterId(10L).dccProjectCodeId(5L).categoryId(2L).requesterId(99L).versionNo("A/1").changeType("NEW").status("WORKING").build();}
    @Test void anotherEditorCanUseInitialAndCheckedInNewWorkingBodies(){
        when(categoryPermissions.hasFilePermission(any(),eq(98L),eq(DccFileCategoryPermissionActionEnum.EDIT))).thenReturn(true);
        var file=working();assertDoesNotThrow(()->contracts.requireDraft(98L,file));
        file.setRevisionChangeType("INITIAL");assertDoesNotThrow(()->contracts.requireDraft(98L,file));
        file.setVersionNo("A/1-1");assertDoesNotThrow(()->contracts.requireDraft(98L,file));
        verify(projectAccess,times(3)).assertProjectEditorOrOwner(98L,5L);
    }
    @Test void editPermissionIsRequiredEvenWhenTheDraftHasNoBpm(){
        assertThrows(IllegalArgumentException.class,()->contracts.requireDraft(98L,working()));
        verify(categoryPermissions).hasFilePermission(any(),eq(98L),eq(DccFileCategoryPermissionActionEnum.EDIT));
    }
    @Test void otherEditorsCannotUseFrozenCandidateControlledOrBoundVersions(){
        for(String boundary:java.util.List.of("candidate","revisionCandidate","controlled","bpm","tenant","deleted")){
            var file=working();
            switch(boundary){
                case "candidate" -> {file.setRevisionChangeType("INITIAL");file.setSelectedIterationControlledFileId(19L);}
                case "revisionCandidate" -> {file.setChangeType("REVISION");file.setRevisionChangeType("MINOR");}
                case "controlled" -> file.setControlledTime(java.time.LocalDateTime.now());
                case "bpm" -> file.setProcessInstanceId("frozen-bpm");
                case "tenant" -> file.setTenantId(2L);
                case "deleted" -> file.setDeleted(true);
            }
            assertThrows(IllegalArgumentException.class,()->contracts.requireDraft(98L,file),boundary);
        }
        verifyNoInteractions(projectAccess,categoryPermissions);
    }
}
