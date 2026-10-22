package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccIntegrationAccessPolicyTest extends BaseMockitoUnitTest {
    @InjectMocks DccRelationAccessPolicyImpl access;
    @Mock DccControlledFileQueryService query;
    @Mock DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @Mock TaskService tasks;
    @Mock(answer=Answers.RETURNS_SELF) TaskQuery taskQuery;
    @Mock AdminUserApi users;
    @Mock DccControlledFileMasterMapper masters;
    @Mock DccProjectAccessService projects;
    @BeforeEach void setup() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    DccControlledFileTaskAssigneeSnapshotDO obligation(Long actor) {
        return DccControlledFileTaskAssigneeSnapshotDO.builder().tenantId(1L).controlledFileId(20L)
                .processInstanceId("round-2").stageCode("MATRIX_REVIEW").leaderUserId(actor).build();
    }
    @Test void staleRoundDoesNotAuthorizeArrangement() {
        when(obligations.selectListByControlledFileId(20L)).thenReturn(List.of(obligation(99L)));
        assertThrows(DccRelationFailure.class,()->access.assertCanArrange(99L,20L,"round-1"));
        verifyNoInteractions(tasks);
    }
    @Test void participantWithoutLiveSignoffTaskCannotWrite() {
        when(obligations.selectListByControlledFileId(20L)).thenReturn(List.of(obligation(99L)));
        when(tasks.createTaskQuery()).thenReturn(taskQuery);when(taskQuery.list()).thenReturn(List.of());
        assertThrows(DccRelationFailure.class,()->access.assertCanArrange(99L,20L,"round-2"));
        verify(taskQuery).taskTenantId("1");verify(taskQuery).taskAssignee("99");
    }
    @Test void historicalParticipantCanReadOnlyExactRound() {
        when(obligations.selectListByControlledFileId(20L)).thenReturn(List.of(obligation(99L)));
        access.assertCanReadArrangements(99L,20L,"round-2");
        assertThrows(DccRelationFailure.class,()->access.assertCanReadArrangements(100L,20L,"round-2"));
        verifyNoInteractions(tasks);
    }
    @Test void assigneeMustHaveFormalProjectEditingAuthority() {
        when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setStatus(0));
        when(masters.selectById(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).dccProjectCodeId(5L).build());
        doThrow(new IllegalArgumentException("no formal edit")).when(projects).assertProjectEditorOrOwner(99L,5L);
        assertThrows(IllegalArgumentException.class,()->access.assertAssigneeAvailable(99L,10L));
    }
    @Test void nameVisibilityDoesNotGrantContentPermission() {
        access.assertNameVisible(99L,20L);
        doThrow(new IllegalArgumentException("content denied")).when(query).assertRelationContentReadable(99L,20L);
        assertThrows(IllegalArgumentException.class,()->access.assertContentReadable(99L,20L));
        verify(query).assertRelationNameVisible(99L,20L);
    }
}
