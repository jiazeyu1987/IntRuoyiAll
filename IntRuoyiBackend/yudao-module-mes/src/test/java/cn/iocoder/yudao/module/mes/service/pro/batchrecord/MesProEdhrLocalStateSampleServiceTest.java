package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrLocalStateSampleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Import(MesProEdhrLocalStateSampleServiceImpl.class)
class MesProEdhrLocalStateSampleServiceTest extends BaseDbUnitTest {

    private static final Long ADMIN_USER_ID = 10001L;

    @Resource
    private MesProEdhrLocalStateSampleService sampleService;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrReleaseCheckItemMapper releaseCheckItemMapper;
    @Resource
    private MesProEdhrBatchExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private MesProEdhrPermissionScopeService permissionScopeService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(sampleService, "activeProfiles", "local");
        when(adminUserApi.getUser(ADMIN_USER_ID)).thenReturn(adminUser("admin"));
        when(permissionScopeService.saveRules(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProEdhrPermissionScopeDetailResult().setScopeId(88001L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLOSE", "PRECHECK", "RELEASE_APPROVAL", "ARCHIVE", "ARCHIVED", "QUALITY_TERMINAL"})
    void createLocalStateSample_writesExpectedStateCombination(String state) {
        EdhrLocalStateSampleRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockAdminLogin(1L, 1L)) {
            response = sampleService.createLocalStateSample(new EdhrLocalStateSampleReqVO().setState(state));
        }

        assertNotNull(response.getBatchExecutionId());
        assertTrue(response.getBatchExecutionCode().startsWith("EDHR-UI-SAMPLE-" + state + "-"));
        assertEquals(state, response.getSampleState());
        assertEquals("/mes/pro/feedback/edhr-batch-execution/detail", response.getDetailPath());
        assertEquals(Map.of(
                "id", String.valueOf(response.getBatchExecutionId()),
                "release", "1",
                "sampleState", state
        ), response.getRouteQuery());

        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(response.getBatchExecutionId());
        assertEquals(expectedBatchStatus(state), batch.getStatus());
        assertTrue(batch.getRemark().contains("[LOCAL_STATE_SAMPLE]"));
        assertEquals(1L, batchTaskMapper.selectCount(
                MesProEdhrBatchExecutionTaskDO::getBatchExecutionId, batch.getId()));
        MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectListByBatchExecutionId(batch.getId())
                .stream()
                .findFirst()
                .orElseThrow();
        assertEquals(88001L, batchTask.getPermissionScopeId());

        MesProEdhrReleaseTransactionDO release = releaseTransactionMapper.selectByBatchExecutionId(batch.getId());
        if ("CLOSE".equals(state)) {
            assertEquals(null, release);
            assertEquals(1L, workTaskMapper.selectCount(
                    MesProEdhrWorkTaskDO::getTaskType, MesProEdhrWorkTaskService.TASK_TYPE_CLOSE));
            return;
        }

        assertNotNull(release);
        assertEquals(expectedReleaseStatus(state), release.getReleaseStatus());
        assertTrue(release.getRemark().contains("[LOCAL_STATE_SAMPLE]"));
        assertTrue(releaseCheckItemMapper.selectCount() > 0);

        if ("RELEASE_APPROVAL".equals(state)) {
            MesProEdhrWorkTaskDO approvalTask = workTaskMapper.selectList().stream()
                    .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_RELEASE_APPROVE.equals(task.getTaskType()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(MesProEdhrWorkTaskStatus.TODO, approvalTask.getStatus());
        }
        if ("ARCHIVE".equals(state)) {
            MesProEdhrWorkTaskDO archiveTask = workTaskMapper.selectList().stream()
                    .filter(task -> MesProEdhrWorkTaskService.TASK_TYPE_ARCHIVE.equals(task.getTaskType()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(MesProEdhrWorkTaskStatus.TODO, archiveTask.getStatus());
            assertEquals(0L, archiveMapper.selectCount());
        }
        if ("ARCHIVED".equals(state)) {
            MesProEdhrBatchExecutionArchiveDO archive = archiveMapper.selectListByBatchExecutionId(batch.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow();
            assertEquals("SEALED", archive.getArchiveStatus());
            assertEquals(Boolean.TRUE, archive.getArchiveValidFlag());
        }
    }

    @Test
    void createLocalStateSample_acceptsRealTokenLoginUserWithoutVisitTenantId() {
        EdhrLocalStateSampleRespVO response;

        try (MockedStatic<SecurityFrameworkUtils> security = mockAdminLogin(1L, null)) {
            response = sampleService.createLocalStateSample(new EdhrLocalStateSampleReqVO().setState("CLOSE"));
        }

        assertNotNull(response.getBatchExecutionId());
        assertTrue(response.getBatchExecutionCode().startsWith("EDHR-UI-SAMPLE-CLOSE-"));
    }

    @Test
    void createLocalStateSample_rejectsNonLocalProfileWithoutWritingRows() {
        ReflectionTestUtils.setField(sampleService, "activeProfiles", "unit-test");
        RowCounts before = rowCounts();

        try (MockedStatic<SecurityFrameworkUtils> security = mockAdminLogin(1L, 1L)) {
            assertThrows(ServiceException.class, () -> sampleService.createLocalStateSample(
                    new EdhrLocalStateSampleReqVO().setState("CLOSE")));
        }

        assertRowCountsUnchanged(before);
    }

    @Test
    void createLocalStateSample_rejectsNonAdminContextWithoutWritingRows() {
        when(adminUserApi.getUser(ADMIN_USER_ID)).thenReturn(adminUser("aoteman"));
        RowCounts before = rowCounts();

        try (MockedStatic<SecurityFrameworkUtils> security = mockAdminLogin(1L, 1L)) {
            assertThrows(ServiceException.class, () -> sampleService.createLocalStateSample(
                    new EdhrLocalStateSampleReqVO().setState("CLOSE")));
        }

        assertRowCountsUnchanged(before);
    }

    @Test
    void createLocalStateSample_rejectsWrongTenantContextWithoutWritingRows() {
        RowCounts before = rowCounts();

        try (MockedStatic<SecurityFrameworkUtils> security = mockAdminLogin(122L, null)) {
            assertThrows(ServiceException.class, () -> sampleService.createLocalStateSample(
                    new EdhrLocalStateSampleReqVO().setState("CLOSE")));
        }

        assertRowCountsUnchanged(before);
    }

    @Test
    void createLocalStateSample_rejectsInvalidStateWithoutWritingRows() {
        RowCounts before = rowCounts();

        try (MockedStatic<SecurityFrameworkUtils> security = mockAdminLogin(1L, 1L)) {
            assertThrows(ServiceException.class, () -> sampleService.createLocalStateSample(
                    new EdhrLocalStateSampleReqVO().setState("UNKNOWN")));
        }

        assertRowCountsUnchanged(before);
    }

    private RowCounts rowCounts() {
        return new RowCounts(
                batchExecutionMapper.selectCount(),
                batchTaskMapper.selectCount(),
                releaseTransactionMapper.selectCount(),
                releaseCheckItemMapper.selectCount(),
                archiveMapper.selectCount(),
                workTaskMapper.selectCount());
    }

    private void assertRowCountsUnchanged(RowCounts before) {
        assertEquals(before.batchExecutionCount(), batchExecutionMapper.selectCount());
        assertEquals(before.batchTaskCount(), batchTaskMapper.selectCount());
        assertEquals(before.releaseTransactionCount(), releaseTransactionMapper.selectCount());
        assertEquals(before.releaseCheckItemCount(), releaseCheckItemMapper.selectCount());
        assertEquals(before.archiveCount(), archiveMapper.selectCount());
        assertEquals(before.workTaskCount(), workTaskMapper.selectCount());
    }

    private int expectedBatchStatus(String state) {
        return switch (state) {
            case "CLOSE" -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE;
            case "ARCHIVED" -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED;
            case "QUALITY_TERMINAL" -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED;
            default -> MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED;
        };
    }

    private String expectedReleaseStatus(String state) {
        return switch (state) {
            case "PRECHECK" -> MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED;
            case "RELEASE_APPROVAL" -> MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL;
            case "ARCHIVE", "ARCHIVED" -> MesProEdhrReleaseServiceImpl.STATUS_RELEASED;
            case "QUALITY_TERMINAL" -> MesProEdhrReleaseServiceImpl.STATUS_REJECTED;
            default -> throw new IllegalArgumentException(state);
        };
    }

    private MockedStatic<SecurityFrameworkUtils> mockAdminLogin(Long tenantId, Long visitTenantId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(ADMIN_USER_ID);
        loginUser.setTenantId(tenantId);
        loginUser.setVisitTenantId(visitTenantId);
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ADMIN_USER_ID);
        return security;
    }

    private AdminUserRespDTO adminUser(String username) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(ADMIN_USER_ID);
        user.setUsername(username);
        user.setNickname(username);
        return user;
    }

    private record RowCounts(long batchExecutionCount,
                             long batchTaskCount,
                             long releaseTransactionCount,
                             long releaseCheckItemCount,
                             long archiveCount,
                             long workTaskCount) {
    }
}
