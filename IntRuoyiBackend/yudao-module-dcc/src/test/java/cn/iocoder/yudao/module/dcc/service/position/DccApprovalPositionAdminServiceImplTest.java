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

import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_DERIVED_ASSIGNMENT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionAdminErrors.NAME_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @Test
    void createPosition_persistsLocalRoleAndListsItWithoutExternalService() {
        DccApprovalPositionDO created = positionAdminService.createPosition(" 文控 ", " 本地文控测试 ");
        DccApprovalPositionDO persisted = positionMapper.selectById(created.getId());
        assertEquals("文控", persisted.getName());
        assertEquals("LOCAL", persisted.getSource());
        assertTrue(persisted.getCode().startsWith("LOCAL-"));
        assertEquals("本地文控测试", persisted.getRemark());
        assertEquals(created.getId(), positionAdminService.getPositionList().get(0).getId());
        assertEquals(List.of(), positionAdminService.getAssignments(created.getId()));
    }

    @Test
    void createPosition_rejectsDuplicateNameWithoutChangingExistingAssignments() {
        DccApprovalPositionDO existing = positionAdminService.createPosition("文控", "初始化");
        positionAdminService.replaceAssignments(existing.getId(), List.of(DccPositionAssignmentSaveReqVOBuilder.user(99L)));
        assertServiceException(() -> positionAdminService.createPosition(" 文控 ", "重复创建"), NAME_EXISTS);
        assertEquals(1, positionMapper.selectList().size());
        assertEquals(99L, positionAdminService.getAssignments(existing.getId()).get(0).getUserId());
    }

    @Test
    void createPosition_generatesDistinctStableCodesForDifferentRoles() {
        DccApprovalPositionDO first = positionAdminService.createPosition("文控", "初始化");
        DccApprovalPositionDO second = positionAdminService.createPosition("审核", "初始化");
        assertFalse(first.getCode().equals(second.getCode()));
        assertEquals(first.getCode(), positionMapper.selectById(first.getId()).getCode());
    }

    @Test
    void getPositionList_includesLocalAndHistoricalRolesButExcludesDisabledRows() {
        createPosition("INTAUTH-11", "历史QA", "INTAUTH:11", true);
        createPosition(SPECIAL_DEPT_OWNER_CODE, "部门负责人", "LOCAL", true);
        createPosition(SPECIAL_AUTH_REP_CODE, "部门授权代表", "LOCAL", true);
        createPosition("LOCAL-TEST", "文控", "LOCAL", true);
        createPosition("DISABLED", "停用角色", "LOCAL", false);
        List<DccApprovalPositionDO> positions = positionAdminService.getPositionList();
        assertEquals(4, positions.size());
        assertFalse(positions.stream().anyMatch(item -> "停用角色".equals(item.getName())));
        assertTrue(positions.stream().anyMatch(item -> "文控".equals(item.getName())));
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

    @Test
    void replaceAssignments_persistsRelationshipWithoutConvertingItToAnAccount() {
        DccApprovalPositionDO position = createPosition("RELATION-RULE", "动态审核", "LOCAL", true);
        DccPositionAssignmentSaveReqVO relation = new DccPositionAssignmentSaveReqVO();
        relation.setAssignmentType("DIRECT_SUPERVISOR");
        relation.setActive(true);
        positionAdminService.replaceAssignments(position.getId(), List.of(relation));
        List<DccPositionAssignmentDO> saved = assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, position.getId());
        assertEquals(1, saved.size());
        assertEquals("DIRECT_SUPERVISOR", saved.get(0).getAssignmentType());
        org.junit.jupiter.api.Assertions.assertNull(saved.get(0).getUserId());
        org.junit.jupiter.api.Assertions.assertNull(saved.get(0).getSystemPostId());
    }

    @Test
    void relationshipWithFixedUserFailsBeforeReplacingExistingConfiguration() {
        DccApprovalPositionDO position = createPosition("RELATION-INVALID", "动态审核", "LOCAL", true);
        positionAdminService.replaceAssignments(position.getId(), List.of(DccPositionAssignmentSaveReqVOBuilder.user(99L)));
        DccPositionAssignmentSaveReqVO relation = DccPositionAssignmentSaveReqVOBuilder.user(99L);
        relation.setAssignmentType("DEPT_LEADER");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> positionAdminService.replaceAssignments(position.getId(), List.of(relation)));
        List<DccPositionAssignmentDO> saved = assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, position.getId());
        assertEquals(1, saved.size());
        assertEquals("USER", saved.get(0).getAssignmentType());
        assertEquals(99L, saved.get(0).getUserId());
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
