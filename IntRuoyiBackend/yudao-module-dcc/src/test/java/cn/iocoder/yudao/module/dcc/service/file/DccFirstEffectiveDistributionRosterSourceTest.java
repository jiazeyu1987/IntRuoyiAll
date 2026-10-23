package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccFirstEffectiveDistributionRosterSourceTest {
    DeptService departments;
    AdminUserApi users;
    DccFirstEffectiveDistributionRosterSource source;
    DccDistributionRosterSource.Request request;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
        departments = mock(DeptService.class);
        users = mock(AdminUserApi.class);
        source = new DccFirstEffectiveDistributionRosterSource(departments, users);
        request = request(List.of("61", "62"));
        when(departments.getDept(61L)).thenReturn(department(61L, 1L, 0));
        when(departments.getDept(62L)).thenReturn(department(62L, 1L, 0));
        when(users.getUserListByDeptIds(any())).thenReturn(List.of(user(99L, 61L, 1L, 0), user(100L, 62L, 1L, 0)));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); DataPermissionContextHolder.clear(); }
    static DeptDO department(Long id, Long tenant, Integer status) {
        var result = new DeptDO(); result.setId(id); result.setTenantId(tenant); result.setStatus(status); return result;
    }
    static AdminUserRespDTO user(Long id, Long dept, Long tenant, Integer status) {
        return new AdminUserRespDTO().setId(id).setDeptId(dept).setTenantId(tenant).setStatus(status);
    }
    static DccDistributionRosterSource.Request request(List<String> ids) {
        return new DccDistributionRosterSource.Request(1L, 51L, 91L, "event42", 10L, 42L, "B/1", "round42", LocalDateTime.now().minusDays(1), ids);
    }
    DccDistributionRosterSource.NotReady notReady(String code) {
        var result = assertInstanceOf(DccDistributionRosterSource.NotReady.class, source.resolve(request));
        assertEquals(code, result.errorCode()); assertFalse(result.reason().isBlank()); return result;
    }
    @Test void currentEnabledDirectMembersProduceExactReadTimeAndActualPolicyEvidence() {
        when(users.getUserListByDeptIds(any())).thenReturn(List.of(user(102L, 61L, 1L, 0), user(101L, 62L, 1L, 1), user(100L, 62L, 1L, 0), user(99L, 61L, 1L, 0)));
        var before = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        var result = assertInstanceOf(DccDistributionRosterSource.Ready.class, source.resolve(request));
        var snapshot = result.snapshot(); var after = LocalDateTime.now();
        assertEquals(1L, snapshot.tenantId()); assertEquals(42L, snapshot.controlledFileId()); assertEquals("B/1", snapshot.versionNo()); assertEquals("round42", snapshot.approvalProcessInstanceId());
        assertEquals(List.of(new DccDistributionRosterSnapshot.Department(61L,List.of(99L,102L)),new DccDistributionRosterSnapshot.Department(62L,List.of(100L))),snapshot.departments());
        assertFalse(snapshot.snapshotAt().isBefore(before)); assertFalse(snapshot.snapshotAt().isAfter(after)); assertTrue(snapshot.snapshotAt().isAfter(request.activatedAt()));
        assertEquals(0,snapshot.snapshotAt().getNano()%1000); assertEquals("FIRST_EFFECTIVE_DISPATCH_V1",snapshot.policyVersion());
        assertTrue(result.sourceIdentity().contains("SYSTEM_USER_DIRECTORY")); assertTrue(result.sourceIdentity().contains("round42")); assertTrue(result.sourceIdentity().contains("event42"));
        assertTrue(result.policyEvidence().contains("下发名单是首次生效下发时冻结")); assertFalse(result.policyEvidence().contains("approvedBy"));
        verify(users).getUserListByDeptIds(List.of(61L,62L)); verify(departments).getDept(61L); verify(departments).getDept(62L);
        verifyNoMoreInteractions(users,departments);
        assertThrows(UnsupportedOperationException.class,()->snapshot.departments().clear()); assertThrows(UnsupportedOperationException.class,()->snapshot.departments().get(0).userIds().clear());
    }
    @Test void directoryReadsIgnoreActorDataPermissionOnlyAndRestoreIt() {
        when(departments.getDept(anyLong())).thenAnswer(call->{assertNotNull(DataPermissionContextHolder.get());assertFalse(DataPermissionContextHolder.get().enable());assertFalse(TenantContextHolder.isIgnore());return department(call.getArgument(0),1L,0);});
        when(users.getUserListByDeptIds(any())).thenAnswer(call->{assertFalse(DataPermissionContextHolder.get().enable());assertEquals(1L,TenantContextHolder.getTenantId());assertFalse(TenantContextHolder.isIgnore());return List.of(user(99L,61L,1L,0),user(100L,62L,1L,0));});
        assertNull(DataPermissionContextHolder.get()); assertInstanceOf(DccDistributionRosterSource.Ready.class,source.resolve(request)); assertNull(DataPermissionContextHolder.get());
        DataPermissionUtils.executeIgnore(()->{var before=DataPermissionContextHolder.get();source.resolve(request);assertSame(before,DataPermissionContextHolder.get());});assertNull(DataPermissionContextHolder.get());
    }
    @Test void actualDirectoryFailurePropagatesAndRestoresPermissionContext() {
        var failure=new IllegalStateException("system directory unavailable");when(users.getUserListByDeptIds(any())).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->source.resolve(request)));assertNull(DataPermissionContextHolder.get());assertFalse(TenantContextHolder.isIgnore());
    }
    @ParameterizedTest @ValueSource(strings={"MISSING","DISABLED","FOREIGN","WRONG_ID","UNKNOWN_STATUS"})
    void eachSavedDepartmentMustBeEnabledAndExactlyCurrentTenant(String fault) {
        var invalid=switch(fault){case "MISSING"->null;case "DISABLED"->department(62L,1L,1);case "FOREIGN"->department(62L,2L,0);case "WRONG_ID"->department(63L,1L,0);default->department(62L,1L,null);};
        when(departments.getDept(62L)).thenReturn(invalid);assertTrue(notReady("ROSTER_DEPARTMENT_UNAVAILABLE").reason().contains("62"));verifyNoInteractions(users);
    }
    @Test void anEmptySavedDepartmentFailsWithoutDroppingItsObligation() {
        when(users.getUserListByDeptIds(any())).thenReturn(List.of(user(99L,61L,1L,0),user(100L,62L,1L,1)));
        assertTrue(notReady("ROSTER_DEPARTMENT_EMPTY").reason().contains("62"));
    }
    @ParameterizedTest @ValueSource(strings={"FOREIGN","CHILD","UNKNOWN_STATUS","NULL_ID","DUPLICATE","NULL_ROW"})
    void invalidDirectoryRowsNeverCreatePartialOrExpandedRoster(String fault) {
        List<AdminUserRespDTO> rows=switch(fault){
            case "FOREIGN"->List.of(user(99L,61L,2L,0));case "CHILD"->List.of(user(99L,63L,1L,0));case "UNKNOWN_STATUS"->List.of(user(99L,61L,1L,null));case "NULL_ID"->List.of(user(null,61L,1L,0));case "DUPLICATE"->List.of(user(99L,61L,1L,0),user(99L,61L,1L,0));default->Arrays.asList((AdminUserRespDTO)null);};
        when(users.getUserListByDeptIds(any())).thenReturn(rows);notReady("ROSTER_DIRECTORY_INVALID");
    }
    @Test void nullDirectoryListIsTechnicalFailureRatherThanEmptySuccess() {when(users.getUserListByDeptIds(any())).thenReturn(null);assertThrows(IllegalStateException.class,()->source.resolve(request));}
    @ParameterizedTest @ValueSource(strings={"EMPTY","DUPLICATE","LEADING_ZERO","OVERFLOW","WHITESPACE"})
    void invalidSavedDepartmentIdentityCannotBeCoerced(String fault) {
        request=request(switch(fault){case "EMPTY"->List.of();case "DUPLICATE"->List.of("61","61");case "LEADING_ZERO"->List.of("061");case "OVERFLOW"->List.of("9223372036854775808");default->List.of(" 61");});
        notReady(fault.equals("EMPTY")?"ROSTER_DEPARTMENTS_MISSING":"ROSTER_REQUEST_INVALID");verifyNoInteractions(departments,users);
    }
    @Test void tenantMismatchAndTenantIgnoreAreRejectedBeforeDirectoryReads() {
        TenantContextHolder.setTenantId(2L);notReady("ROSTER_REQUEST_INVALID");TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(true);notReady("ROSTER_REQUEST_INVALID");verifyNoInteractions(departments,users);
    }
    @Test void missingExactEventVersionAndBpmProofCannotQueryDirectory() {
        request=new DccDistributionRosterSource.Request(1L,51L,91L,"event42",10L,42L,null,"round42",LocalDateTime.now().minusDays(1),List.of("61"));notReady("ROSTER_REQUEST_INVALID");
        request=new DccDistributionRosterSource.Request(1L,51L,91L,"event42",10L,42L,"B/1",null,LocalDateTime.now().minusDays(1),List.of("61"));notReady("ROSTER_REQUEST_INVALID");verifyNoInteractions(departments,users);
    }
    @Test void highLongDepartmentAndAccountIdsStayExact() {
        request=request(List.of(Long.toString(Long.MAX_VALUE)));when(departments.getDept(Long.MAX_VALUE)).thenReturn(department(Long.MAX_VALUE,1L,0));
        when(users.getUserListByDeptIds(List.of(Long.MAX_VALUE))).thenReturn(List.of(user(9007199254740993L,Long.MAX_VALUE,1L,0)));
        var ready=assertInstanceOf(DccDistributionRosterSource.Ready.class,source.resolve(request));assertEquals(Long.MAX_VALUE,ready.snapshot().departments().get(0).departmentId());assertEquals(List.of(9007199254740993L),ready.snapshot().departments().get(0).userIds());
    }
}
