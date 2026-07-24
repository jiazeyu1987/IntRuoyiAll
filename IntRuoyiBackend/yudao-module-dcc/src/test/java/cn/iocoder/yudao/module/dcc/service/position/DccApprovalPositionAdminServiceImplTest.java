package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.module.dcc.controller.admin.position.vo.DccPositionAssignmentSaveReqVO;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_AMBIGUOUS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_CONFIG_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(DccApprovalPositionAdminServiceImpl.class)
class DccApprovalPositionAdminServiceImplTest extends BaseDbUnitTest {

    private static final String SPECIAL_DEPT_OWNER_CODE = "LOCAL-ROLE-APPROVER-DEPT";
    private static final String SPECIAL_AUTH_REP_CODE = "LOCAL-ROLE-AUTH-REP";

    @Resource
    private DccApprovalPositionAdminServiceImpl positionAdminService;
    @Resource
    private DccApprovalPositionMapper positionMapper;
    @Resource
    private DccPositionAssignmentMapper assignmentMapper;
    @MockitoBean
    private DccIntAuthPositionClient intAuthPositionClient;

    @Test
    void getPositionList_readsImportedLocalTableWithoutCallingIntAuth() {
        createPosition("INTAUTH-11", "QA", "INTAUTH:11", true);
        createPosition("DCC_RUNTIME_POSITION", "Local seed position", "E2E", true);

        List<DccApprovalPositionDO> positions = positionAdminService.getPositionList();

        assertEquals(1, positions.size());
        DccApprovalPositionDO position = positions.get(0);
        assertEquals("QA", position.getName());
        assertEquals("INTAUTH-11", position.getCode());
        assertEquals("INTAUTH:11", position.getSource());
        assertTrue(Boolean.TRUE.equals(position.getActive()));
        verifyNoInteractions(intAuthPositionClient);
    }

    @Test
    void getPositionList_includesFixedLocalPositionsButStillHidesOtherSeedRows() {
        createPosition("INTAUTH-11", "QA", "INTAUTH:11", true);
        createPosition(SPECIAL_DEPT_OWNER_CODE, "部门负责人", "LOCAL", true);
        createPosition(SPECIAL_AUTH_REP_CODE, "部门授权代表", "LOCAL", true);
        createPosition("DCC_RUNTIME_POSITION", "Local seed position", "E2E", true);

        List<DccApprovalPositionDO> positions = positionAdminService.getPositionList();

        assertEquals(3, positions.size());
        assertEquals(List.of("QA", "部门授权代表", "部门负责人"),
                positions.stream().map(DccApprovalPositionDO::getName).sorted().toList());
        verifyNoInteractions(intAuthPositionClient);
    }

    @Test
    void importPositionsFromIntAuth_createsMissingLocalPosition() {
        when(intAuthPositionClient.listPositions()).thenReturn(List.of(
                new DccIntAuthPositionClient.IntAuthPosition(11L, "QA")
        ));

        DccApprovalPositionImportResult result = positionAdminService.importPositionsFromIntAuth();

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getAdoptedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getDisabledCount());
        List<DccApprovalPositionDO> positions = positionAdminService.getPositionList();
        assertEquals(1, positions.size());
        DccApprovalPositionDO position = positions.get(0);
        assertEquals("QA", position.getName());
        assertEquals("INTAUTH-11", position.getCode());
        assertEquals("INTAUTH:11", position.getSource());
        assertTrue(Boolean.TRUE.equals(position.getActive()));
    }

    @Test
    void importPositionsFromIntAuth_reusesSameNameLocalPositionAndPreservesAssignments() {
        DccApprovalPositionDO localPosition = createPosition("POS-001", "QA", "LOCAL", true);
        DccPositionAssignmentDO assignment = DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(localPosition.getId())
                .assignmentType("USER")
                .userId(99L)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build();
        assignmentMapper.insert(assignment);

        when(intAuthPositionClient.listPositions()).thenReturn(List.of(
                new DccIntAuthPositionClient.IntAuthPosition(11L, "QA")
        ));

        DccApprovalPositionImportResult result = positionAdminService.importPositionsFromIntAuth();

        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getCreatedCount());
        assertEquals(1, result.getAdoptedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(0, result.getDisabledCount());
        DccApprovalPositionDO synced = positionMapper.selectById(localPosition.getId());
        assertEquals(localPosition.getId(), synced.getId());
        assertEquals("POS-001", synced.getCode());
        assertEquals("INTAUTH:11", synced.getSource());
        assertEquals(1, assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, localPosition.getId()).size());
    }

    @Test
    void importPositionsFromIntAuth_disablesMappedPositionMissingFromIntAuth() {
        DccApprovalPositionDO mappedPosition = createPosition("INTAUTH-11", "QA", "INTAUTH:11", true);

        when(intAuthPositionClient.listPositions()).thenReturn(List.of());

        DccApprovalPositionImportResult result = positionAdminService.importPositionsFromIntAuth();

        assertEquals(0, result.getTotalCount());
        assertEquals(0, result.getCreatedCount());
        assertEquals(0, result.getAdoptedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(1, result.getDisabledCount());
        DccApprovalPositionDO disabled = positionMapper.selectById(mappedPosition.getId());
        assertFalse(Boolean.TRUE.equals(disabled.getActive()));
    }

    @Test
    void getPositionList_hidesUnmappedLocalSeedPositions() {
        createPosition("DCC_RUNTIME_POSITION", "Local seed position", "E2E", true);
        createPosition("INTAUTH-11", "QA", "INTAUTH:11", true);

        List<DccApprovalPositionDO> positions = positionAdminService.getPositionList();

        assertEquals(1, positions.size());
        assertEquals("QA", positions.get(0).getName());
    }

    @Test
    void importPositionsFromIntAuth_duplicateSameNameLocalPositions_failFast() {
        createPosition("POS-001", "QA", "LOCAL", true);
        createPosition("POS-002", "QA", "LOCAL", true);

        when(intAuthPositionClient.listPositions()).thenReturn(List.of(
                new DccIntAuthPositionClient.IntAuthPosition(11L, "QA")
        ));

        assertServiceException(positionAdminService::importPositionsFromIntAuth, INTAUTH_POSITION_SYNC_AMBIGUOUS);
    }

    @Test
    void importPositionsFromIntAuth_missingIntAuthConfig_failFast() {
        when(intAuthPositionClient.listPositions())
                .thenThrow(cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                        .exception(INTAUTH_POSITION_SYNC_CONFIG_MISSING, "yudao.dcc.int-auth.internal-service-token"));

        assertServiceException(positionAdminService::importPositionsFromIntAuth, INTAUTH_POSITION_SYNC_CONFIG_MISSING);
    }

    @Test
    void replaceAssignments_uploaderDerivedPositionWithManualUsers_failFast() {
        DccApprovalPositionDO directManager = createPosition("INTAUTH-1", "编制人直接主管", "INTAUTH:1", true);

        assertServiceException(() -> positionAdminService.replaceAssignments(directManager.getId(), List.of(
                DccPositionAssignmentSaveReqVOBuilder.user(99L)
        )), APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED);
    }

    @Test
    void replaceAssignments_uploaderDerivedPositionWithEmptyPayload_clearsLegacyRows() {
        DccApprovalPositionDO departmentOwner = createPosition("LOCAL-ROLE-APPROVER-DEPT", "部门负责人", "LOCAL", true);
        assignmentMapper.insert(DccPositionAssignmentDO.builder()
                .id(randomLongId())
                .positionId(departmentOwner.getId())
                .assignmentType("USER")
                .userId(99L)
                .active(Boolean.TRUE)
                .changeReason("legacy")
                .build());

        List<DccPositionAssignmentDO> saved = positionAdminService.replaceAssignments(departmentOwner.getId(), List.of());

        assertEquals(List.of(), saved);
        assertEquals(0, assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, departmentOwner.getId()).size());
    }

    @Test
    void replaceAssignments_authorizedRepresentative_acceptsLocalDccAssignment() {
        DccApprovalPositionDO authorizedRepresentative = createPosition(SPECIAL_AUTH_REP_CODE, "部门授权代表",
                "LOCAL", true);

        List<DccPositionAssignmentDO> saved = positionAdminService.replaceAssignments(
                authorizedRepresentative.getId(), List.of(DccPositionAssignmentSaveReqVOBuilder.user(99L)));

        assertEquals(1, saved.size());
        assertEquals(99L, saved.get(0).getUserId());
        List<DccPositionAssignmentDO> rows = assignmentMapper.selectList(
                DccPositionAssignmentDO::getPositionId, authorizedRepresentative.getId());
        assertEquals(1, rows.size());
        assertEquals(99L, rows.get(0).getUserId());
    }

    private static final class DccPositionAssignmentSaveReqVOBuilder {
        private static DccPositionAssignmentSaveReqVO user(Long userId) {
            DccPositionAssignmentSaveReqVO reqVO = new DccPositionAssignmentSaveReqVO();
            reqVO.setAssignmentType("USER");
            reqVO.setUserId(userId);
            reqVO.setActive(Boolean.TRUE);
            reqVO.setChangeReason("seed");
            return reqVO;
        }
    }

    private DccApprovalPositionDO createPosition(String code, String name, String source, boolean active) {
        DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .active(active)
                .source(source)
                .remark("seed")
                .build();
        positionMapper.insert(position);
        return position;
    }
}
