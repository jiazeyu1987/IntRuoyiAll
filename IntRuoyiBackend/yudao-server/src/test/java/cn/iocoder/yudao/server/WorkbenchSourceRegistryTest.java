package cn.iocoder.yudao.server;

import cn.iocoder.yudao.server.profileworkbench.WorkbenchSourceRegistry;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentWorkbenchSource;
import org.junit.jupiter.api.Test;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.server.profileworkbench.WorkbenchQuerySpec;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkbenchSourceRegistryTest {
    private SourceDescriptor descriptor(String id, int rank, String action) {
        return new SourceDescriptor(id, "fixture", 1, rank, "来源" + rank, "行政", "fixture.policy", action, 1);
    }
    private SourceAdapter source(SourceDescriptor d, boolean authorized) {
        return new SourceAdapter() {
            public SourceDescriptor descriptor() { return d; }
            public boolean isAuthorized(long user, long tenant) { return authorized; }
            public long count(QueryContext q) { throw new AssertionError("registry must not query rows"); }
            public TaskChunk readChunk(QueryContext q, ChunkCursor after, int limit) { throw new AssertionError(); }
            public RowOrderContract rowOrderContract() { return RowOrderContract.UTF8_BINARY_DESC; }
            public SourceIdentityContract identityContract() { return SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY; }
            public TaskSummary resolveVisibilityIdentity(QueryContext q,String key) { throw new AssertionError(); }
        };
    }
    private WorkbenchSourceRegistry.Manifest manifest(boolean enabled, String... ids) {
        return new WorkbenchSourceRegistry.Manifest(1, "revision", List.of(new WorkbenchSourceRegistry.Module("fixture", enabled, List.of(ids))));
    }
    @Test void sixthSourceNeedsOnlyAdapterAndManifest() {
        List<SourceAdapter> beans = new ArrayList<>(); String[] ids = new String[6];
        for (int i=0; i<6; i++) { ids[i]="FIXTURE_"+i; beans.add(source(descriptor(ids[i], i, "fixture.open"+i), true)); }
        WorkbenchSourceRegistry registry = new WorkbenchSourceRegistry(beans, manifest(true, ids));
        assertEquals(6, registry.directory(1, 2).sources().size());
        assertEquals("FIXTURE_5", registry.require("FIXTURE_5").sourceId());
        assertEquals("独立第六类别", new WorkbenchQuerySpec("visible", "独立第六类别", null, null).normalized().taskType());
    }
    @Test void enabledMissingSourceFails() {
        assertThrows(IllegalStateException.class, () -> new WorkbenchSourceRegistry(List.of(), manifest(true,"ONE")));
    }
    @Test void duplicateSourceFails() {
        var s=source(descriptor("ONE",0,"one.open"),true);
        assertThrows(IllegalStateException.class, () -> new WorkbenchSourceRegistry(List.of(s,s),manifest(true,"ONE")));
    }
    @Test void duplicateRankFails() {
        assertThrows(IllegalStateException.class, () -> new WorkbenchSourceRegistry(List.of(source(descriptor("ONE",0,"one.open"),true), source(descriptor("TWO",0,"two.open"),true)),manifest(true,"ONE","TWO")));
    }
    @Test void duplicateActionFails() {
        assertThrows(IllegalStateException.class, () -> new WorkbenchSourceRegistry(List.of(source(descriptor("ONE",0,"one.open"),true), source(descriptor("TWO",1,"one.open"),true)),manifest(true,"ONE","TWO")));
    }
    @Test void manifestAloneDisablesCompiledSourceAndRequestFails() {
        var registry=new WorkbenchSourceRegistry(List.of(source(descriptor("ONE",0,"one.open"),true)),manifest(false,"ONE"));
        assertTrue(registry.directory(1,2).sources().isEmpty());
        assertEquals(WorkbenchFailure.MODULE_DISABLED.code(), assertThrows(ServiceException.class,()->registry.require("ONE")).getCode());
    }
    @Test void unauthorizedDirectoryIsValidEmptyAndPolicyFailurePropagates() {
        var registry=new WorkbenchSourceRegistry(List.of(source(descriptor("ONE",0,"one.open"),false)),manifest(true,"ONE"));
        assertTrue(registry.directory(1,2).sources().isEmpty());
        var bad=mock(SourceAdapter.class); when(bad.descriptor()).thenReturn(descriptor("ONE",0,"one.open"));
        when(bad.rowOrderContract()).thenReturn(RowOrderContract.UTF8_BINARY_DESC);
        when(bad.identityContract()).thenReturn(SourceIdentityContract.SQL_BINARY_UNIQUE_TASK_KEY);
        when(bad.isAuthorized(1,2)).thenThrow(new IllegalStateException("permission service failed"));
        assertThrows(IllegalStateException.class,()->new WorkbenchSourceRegistry(List.of(bad),manifest(true,"ONE")).directory(1,2));
    }
    @Test void unsupportedVersionAndRevisionFail() {
        assertThrows(IllegalArgumentException.class,()->new SourceDescriptor("ONE","fixture",2,0,"一","行政","policy","action",1));
        var registry=new WorkbenchSourceRegistry(List.of(source(descriptor("ONE",0,"one.open"),true)),manifest(true,"ONE"));
        assertThrows(ServiceException.class,()->registry.requireRevision("other"));
        assertThrows(ServiceException.class,()->registry.normalize(List.of("ONE","ONE")));
        assertThrows(ServiceException.class,()->registry.require("UNKNOWN"));
    }
    @Test void showroomUsesFormalEditorRoleAuthorityRatherThanFrontendRoute() {
        PermissionApi permissions=mock(PermissionApi.class);
        ShowroomAssignmentWorkbenchSource source=new ShowroomAssignmentWorkbenchSource();
        ReflectionTestUtils.setField(source,"permissionApi",permissions);
        when(permissions.hasAnyRolesOrSuperAdmin(1L,"EDITOR")).thenReturn(true);
        assertTrue(source.isAuthorized(1,2));
        assertFalse(source.isAuthorized(3,2));
        verify(permissions).hasAnyRolesOrSuperAdmin(1L,"EDITOR");
        verify(permissions).hasAnyRolesOrSuperAdmin(3L,"EDITOR");
        verifyNoMoreInteractions(permissions);
    }
}
