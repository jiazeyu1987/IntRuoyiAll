package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryActiveNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNasTransferTaskItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileStampDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNasTransferTaskMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileStampMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessSubjectTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferServiceImpl;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryDeleteSubtreeResult;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_ACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_CONFIRM_TEXT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_NAS_TRANSFER_ACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import({DccDirectoryAdminServiceImpl.class, DccDirectoryNasTransferGuardService.class})
class DccDirectoryAdminServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccDirectoryAdminServiceImpl directoryAdminService;
    @Resource
    private DccDirectoryNasTransferGuardService nasTransferGuardService;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccDirectoryAccessRuleMapper accessRuleMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileNasTransferTaskMapper nasTransferTaskMapper;
    @Resource
    private DccControlledFileNasTransferTaskItemMapper nasTransferTaskItemMapper;
    @Resource
    private DccExternalFileReviewMapper externalFileReviewMapper;
    @Resource
    private DccControlledFileStampMapper stampMapper;
    @Resource
    private DccProjectCodeAssignmentMapper projectCodeAssignmentMapper;
    @Resource
    private DccProjectCodeAssignmentFileMapper projectCodeAssignmentFileMapper;
    @MockitoBean
    private DccDirectoryAccessPermissionService accessPermissionService;
    @MockitoBean
    private DccIntAuthDirectoryClient intAuthDirectoryClient;
    @MockitoBean
    private FileMapper fileMapper;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private PermissionApi permissionApi;

    @Test
    void testCreateDirectoryAndGetTree_success() {
        DccDirectorySaveReqVO rootReqVO = new DccDirectorySaveReqVO();
        rootReqVO.setCode("QUALITY_ROOT");
        rootReqVO.setName("质量体系");
        rootReqVO.setParentId(null);
        rootReqVO.setActive(Boolean.TRUE);
        rootReqVO.setSort(1);
        rootReqVO.setRemark("root");

        Long rootId = directoryAdminService.createDirectory(rootReqVO);
        assertNotNull(rootId);

        DccDirectorySaveReqVO childReqVO = new DccDirectorySaveReqVO();
        childReqVO.setCode("SOP_LIBRARY");
        childReqVO.setName("SOP库");
        childReqVO.setParentId(rootId);
        childReqVO.setActive(Boolean.TRUE);
        childReqVO.setSort(10);
        childReqVO.setRemark("child");
        directoryAdminService.createDirectory(childReqVO);

        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);

        List<DccFileDirectoryDO> roots = directoryAdminService.getDirectoryTree(99L);

        assertEquals(2, roots.size());
        DccFileDirectoryDO dbRoot = directoryMapper.selectById(rootId);
        DccFileDirectoryDO expectedRoot = BeanUtils.toBean(rootReqVO, DccFileDirectoryDO.class);
        expectedRoot.setAccessRuleManuallyBound(Boolean.FALSE);
        assertPojoEquals(expectedRoot, dbRoot,
                "id", "createTime", "updateTime", "creator", "updater", "deleted");
        assertEquals("QUALITY_ROOT", roots.get(0).getCode());
        assertEquals(Boolean.FALSE, dbRoot.getAccessRuleManuallyBound());
    }

    @Test
    void testGetDirectoryTree_nonManagerOnlyVisibleBranchAndAncestors() {
        DccFileDirectoryDO root = DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code("ROOT")
                .name("root")
                .parentId(null)
                .active(Boolean.TRUE)
                .sort(1)
                .remark("root")
                .build();
        directoryMapper.insert(root);
        DccFileDirectoryDO visibleChild = DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code("VISIBLE")
                .name("visible")
                .parentId(root.getId())
                .active(Boolean.TRUE)
                .sort(2)
                .remark("visible")
                .build();
        directoryMapper.insert(visibleChild);
        DccFileDirectoryDO hiddenRoot = DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code("HIDDEN")
                .name("hidden")
                .parentId(null)
                .active(Boolean.TRUE)
                .sort(3)
                .remark("hidden")
                .build();
        directoryMapper.insert(hiddenRoot);

        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(accessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY))
                .thenReturn(Set.of(visibleChild.getId()));

        List<DccFileDirectoryDO> visibleDirectories = directoryAdminService.getDirectoryTree(99L);

        assertEquals(List.of(root.getId(), visibleChild.getId()),
                visibleDirectories.stream().map(DccFileDirectoryDO::getId).toList());
    }

    @Test
    void listVisibleChildDirectories_rootForManager_returnsFirstLevelOnlyWithHasChildren() {
        DccFileDirectoryDO root = insertDirectory("LAZY_ROOT", "根目录", null);
        DccFileDirectoryDO siblingRoot = insertDirectory("LAZY_SIBLING", "同级目录", null);
        DccFileDirectoryDO zeroParentRoot = insertDirectory("LAZY_ZERO_ROOT", "零父级根目录", 0L);
        DccFileDirectoryDO child = insertDirectory("LAZY_CHILD", "子目录", root.getId());
        insertDirectory("LAZY_GRAND", "孙目录", child.getId());
        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);

        List<DccVisibleDirectoryNode> result = directoryAdminService.listVisibleChildDirectories(99L, null);

        assertEquals(Set.of(root.getId(), siblingRoot.getId(), zeroParentRoot.getId()), result.stream()
                .map(item -> item.directory().getId())
                .collect(java.util.stream.Collectors.toSet()));
        DccVisibleDirectoryNode rootNode = result.stream()
                .filter(item -> root.getId().equals(item.directory().getId()))
                .findFirst()
                .orElseThrow();
        DccVisibleDirectoryNode siblingNode = result.stream()
                .filter(item -> siblingRoot.getId().equals(item.directory().getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(Boolean.TRUE, rootNode.hasChildren());
        assertEquals(Boolean.FALSE, siblingNode.hasChildren());
        assertEquals("根目录", rootNode.directoryPath());
    }

    @Test
    void listVisibleChildDirectories_parentForManager_returnsDirectChildrenOnly() {
        DccFileDirectoryDO root = insertDirectory("LAZY_PARENT", "父目录", null);
        DccFileDirectoryDO child = insertDirectory("LAZY_DIRECT", "直属子目录", root.getId());
        insertDirectory("LAZY_GRAND_DIRECT", "孙目录", child.getId());
        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);

        List<DccVisibleDirectoryNode> result = directoryAdminService.listVisibleChildDirectories(99L, root.getId());

        assertEquals(List.of(child.getId()), result.stream().map(item -> item.directory().getId()).toList());
        assertEquals(Boolean.TRUE, result.get(0).hasChildren());
        assertEquals("父目录/直属子目录", result.get(0).directoryPath());
    }

    @Test
    void searchVisibleDirectories_forManager_returnsMatchedDirectoriesWithPathAndHasChildren() {
        DccFileDirectoryDO root = insertDirectory("SEARCH_ROOT", "根目录", null);
        DccFileDirectoryDO child = insertDirectory("SEARCH_DRAWING", "图纸目录", root.getId());
        insertDirectory("SEARCH_DRAWING_GRAND", "图纸子目录", child.getId());
        insertDirectory("SEARCH_OTHER", "其他目录", root.getId());
        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);

        List<DccVisibleDirectoryNode> result = directoryAdminService.searchVisibleDirectories(99L, "图纸目录", 50);

        assertEquals(1, result.size());
        assertEquals(child.getId(), result.get(0).directory().getId());
        assertEquals(Boolean.TRUE, result.get(0).hasChildren());
        assertEquals("根目录/图纸目录", result.get(0).directoryPath());
    }

    @Test
    void listVisibleChildDirectories_andSearchForOrdinaryUser_doNotLeakUnauthorizedDirectories() {
        DccFileDirectoryDO root = insertDirectory("VISIBLE_ROOT", "可见父目录", null);
        DccFileDirectoryDO visibleChild = insertDirectory("VISIBLE_CHILD", "可见图纸", root.getId());
        DccFileDirectoryDO hiddenChild = insertDirectory("HIDDEN_CHILD", "隐藏图纸", root.getId());
        DccFileDirectoryDO hiddenRoot = insertDirectory("HIDDEN_ROOT", "隐藏根目录", null);
        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(false);
        when(accessPermissionService.getAuthorizedDirectoryIds(99L, DccAccessTypeEnum.QUERY))
                .thenReturn(Set.of(visibleChild.getId()));

        List<DccVisibleDirectoryNode> rootChildren = directoryAdminService.listVisibleChildDirectories(99L, null);
        List<DccVisibleDirectoryNode> visibleChildren = directoryAdminService.listVisibleChildDirectories(99L, root.getId());
        List<DccVisibleDirectoryNode> hiddenSearchResult = directoryAdminService.searchVisibleDirectories(99L, "隐藏", 50);
        List<DccVisibleDirectoryNode> visibleSearchResult = directoryAdminService.searchVisibleDirectories(99L, "图纸", 50);

        assertEquals(Set.of(root.getId()), rootChildren.stream()
                .map(item -> item.directory().getId())
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of(visibleChild.getId()),
                visibleChildren.stream().map(item -> item.directory().getId()).toList());
        assertEquals(List.of(), hiddenSearchResult);
        assertEquals(List.of(visibleChild.getId()),
                visibleSearchResult.stream().map(item -> item.directory().getId()).toList());
        assertEquals("可见父目录/可见图纸", visibleSearchResult.get(0).directoryPath());
        assertEquals(0L, visibleChildren.stream()
                .filter(item -> hiddenChild.getId().equals(item.directory().getId())
                        || hiddenRoot.getId().equals(item.directory().getId()))
                .count());
    }

    @Test
    void getDirectoryTree_activeProjectCodeAssignmentRestrictsDocControlRoleUserDirectoriesToAssignedFileScope() {
        DccFileDirectoryDO root = insertDirectory("ASSIGN_ROOT", "文件库", null);
        DccFileDirectoryDO assignedChild = insertDirectory("ASSIGN_VISIBLE", "PTCABC", root.getId());
        DccFileDirectoryDO hiddenChild = insertDirectory("ASSIGN_HIDDEN", "其他项目", root.getId());
        DccFileDirectoryDO hiddenRoot = insertDirectory("ASSIGN_OTHER_ROOT", "其他根目录", null);
        controlledFileMapper.insert(DccControlledFileDO.builder()
                .id(9101L)
                .masterId(9100L)
                .categoryId(10L)
                .directoryId(assignedChild.getId())
                .sourceFileId(91001L)
                .originalFileId(91002L)
                .fileName("assigned.pdf")
                .title("assigned.pdf")
                .fileNumber("ASSIGN-001")
                .versionNo("1.0")
                .status("ACTIVE")
                .submitterId(88L)
                .requesterId(88L)
                .build());
        projectCodeAssignmentMapper.insert(DccProjectCodeAssignmentDO.builder()
                .id(9201L)
                .assignmentNo("ASSIGN-9201")
                .projectCodeId(3001L)
                .scopeMode("ASSIGNED_FILES")
                .assigneeUserId(99L)
                .assignedBy(1L)
                .assignedTime(LocalDateTime.now().minusDays(1))
                .status(STATUS_ACTIVE)
                .fileCount(1)
                .build());
        projectCodeAssignmentFileMapper.insert(DccProjectCodeAssignmentFileDO.builder()
                .id(9202L)
                .assignmentId(9201L)
                .projectCodeId(3001L)
                .controlledFileId(9101L)
                .directoryIdSnapshot(assignedChild.getId())
                .build());
        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        when(permissionApi.hasAnyRoles(99L, "doc_control")).thenReturn(true);

        List<DccFileDirectoryDO> tree = directoryAdminService.getDirectoryTree(99L);
        List<DccVisibleDirectoryNode> children = directoryAdminService.listVisibleChildDirectories(99L, root.getId());
        List<DccVisibleDirectoryNode> hiddenSearch = directoryAdminService.searchVisibleDirectories(99L, "其他", 50);

        assertEquals(List.of(root.getId(), assignedChild.getId()),
                tree.stream().map(DccFileDirectoryDO::getId).toList());
        assertEquals(List.of(assignedChild.getId()),
                children.stream().map(item -> item.directory().getId()).toList());
        assertEquals(List.of(), hiddenSearch);
        assertEquals(0L, tree.stream()
                .filter(item -> hiddenChild.getId().equals(item.getId())
                        || hiddenRoot.getId().equals(item.getId()))
                .count());
    }

    @Test
    void testReplaceAccessRules_success() {
        DccFileDirectoryDO directory = DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code("FORM_LIBRARY")
                .name("表单库")
                .parentId(null)
                .active(Boolean.TRUE)
                .sort(1)
                .remark("dir")
                .build();
        directoryMapper.insert(directory);

        DccDirectoryAccessRuleSaveReqVO saveReqVO = new DccDirectoryAccessRuleSaveReqVO();
        saveReqVO.setDirectoryId(directory.getId());
        saveReqVO.setSubjectType(DccAccessSubjectTypeEnum.USER.name());
        saveReqVO.setSubjectId(100L);
        saveReqVO.setCanQuery(Boolean.TRUE);
        saveReqVO.setCanPreview(Boolean.TRUE);
        saveReqVO.setCanDownload(Boolean.FALSE);
        saveReqVO.setActive(Boolean.TRUE);
        saveReqVO.setChangeReason("seed");

        directoryAdminService.replaceAccessRules(directory.getId(), List.of(saveReqVO));

        List<DccDirectoryAccessRuleDO> rules = accessRuleMapper.selectList(
                DccDirectoryAccessRuleDO::getDirectoryId, directory.getId());
        assertEquals(1, rules.size());
        assertEquals("USER", rules.get(0).getSubjectType());
        assertEquals(Boolean.TRUE, rules.get(0).getCanPreview());
        assertEquals(Boolean.FALSE, rules.get(0).getCanDownload());
        assertEquals(Boolean.TRUE, directoryMapper.selectById(directory.getId()).getAccessRuleManuallyBound());
    }

    @Test
    void replaceAccessRules_mergesLegacyPreviewOnlyRuleIntoUnifiedReadPermission() {
        DccFileDirectoryDO directory = insertDirectory("MERGED_RULE_DIR", "合并规则目录", null);

        DccDirectoryAccessRuleSaveReqVO saveReqVO = new DccDirectoryAccessRuleSaveReqVO();
        saveReqVO.setDirectoryId(directory.getId());
        saveReqVO.setSubjectType(DccAccessSubjectTypeEnum.USER.name());
        saveReqVO.setSubjectId(101L);
        saveReqVO.setCanQuery(Boolean.FALSE);
        saveReqVO.setCanPreview(Boolean.TRUE);
        saveReqVO.setCanDownload(Boolean.FALSE);
        saveReqVO.setActive(Boolean.TRUE);
        saveReqVO.setChangeReason("merge read permission");

        directoryAdminService.replaceAccessRules(directory.getId(), List.of(saveReqVO));

        List<DccDirectoryAccessRuleDO> rules = accessRuleMapper.selectList(
                DccDirectoryAccessRuleDO::getDirectoryId, directory.getId());
        assertEquals(1, rules.size());
        assertEquals(Boolean.TRUE, rules.get(0).getCanQuery());
        assertEquals(Boolean.TRUE, rules.get(0).getCanPreview());
        assertEquals(Boolean.FALSE, rules.get(0).getCanDownload());
    }

    @Test
    void listAccessRuleDirectories_returnsOnlyManualBoundPreOrderPathSummaries() {
        DccFileDirectoryDO root = insertDirectory("QMS_ROOT", "质量管理", null);
        DccFileDirectoryDO qmsDocs = insertDirectory("QMS_DOC", "1.QMS documents", root.getId());
        DccFileDirectoryDO target = insertDirectory("QMS_TARGET", "4 经营体系管理制度", qmsDocs.getId(), Boolean.TRUE);
        DccFileDirectoryDO deptRoot = insertDirectory("DEPT_ROOT", "部门目录", root.getId());
        DccFileDirectoryDO deptLeaf = insertDirectory("DEPT_LEAF", "生产制造中心", deptRoot.getId(), Boolean.FALSE);
        insertDirectory("NO_RULE", "无规则目录", root.getId());

        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(randomLongId())
                .directoryId(target.getId())
                .subjectType("DEPT")
                .subjectId(100L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build());
        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(randomLongId())
                .directoryId(deptLeaf.getId())
                .subjectType("ROLE")
                .subjectId(200L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.FALSE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("seed")
                .build());

        List<DccDirectoryAccessRuleDirectorySummary> result = directoryAdminService.listAccessRuleDirectories();

        assertEquals(List.of(target.getId()), result.stream().map(DccDirectoryAccessRuleDirectorySummary::id).toList());
        assertEquals("质量管理/1.QMS documents/4 经营体系管理制度", result.get(0).directoryPath());
    }

    @Test
    void listAccessRuleDirectories_whenRuleReferencesMissingDirectory_failFast() {
        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(randomLongId())
                .directoryId(999999L)
                .subjectType("USER")
                .subjectId(100L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("orphan")
                .build());

        assertServiceException(directoryAdminService::listAccessRuleDirectories, FILE_DIRECTORY_NOT_EXISTS);
    }

    @Test
    void deleteAccessRules_removesWholeDirectoryRuleSet() {
        DccFileDirectoryDO directory = insertDirectory("ACL_DIR", "访问目录", null, Boolean.TRUE);
        DccFileDirectoryDO otherDirectory = insertDirectory("ACL_OTHER", "其他目录", null);
        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(randomLongId())
                .directoryId(directory.getId())
                .subjectType("USER")
                .subjectId(100L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("first")
                .build());
        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(randomLongId())
                .directoryId(directory.getId())
                .subjectType("ROLE")
                .subjectId(200L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.FALSE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("second")
                .build());
        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(randomLongId())
                .directoryId(otherDirectory.getId())
                .subjectType("DEPT")
                .subjectId(300L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.FALSE)
                .canDownload(Boolean.FALSE)
                .active(Boolean.TRUE)
                .changeReason("keep")
                .build());

        directoryAdminService.deleteAccessRules(directory.getId());

        assertEquals(0, accessRuleMapper.selectCount(DccDirectoryAccessRuleDO::getDirectoryId, directory.getId()));
        assertEquals(1, accessRuleMapper.selectCount(DccDirectoryAccessRuleDO::getDirectoryId, otherDirectory.getId()));
        assertEquals(Boolean.FALSE, directoryMapper.selectById(directory.getId()).getAccessRuleManuallyBound());
    }

    @Test
    void testImportDirectoriesFromIntAuth_success() {
        when(intAuthDirectoryClient.listBaselineDirectories()).thenReturn(List.of(
                new DccIntAuthDirectoryClient.IntAuthDirectoryNode("dmr-root", null, "3.DMR"),
                new DccIntAuthDirectoryClient.IntAuthDirectoryNode("cat-a", "dmr-root", "01.图纸"),
                new DccIntAuthDirectoryClient.IntAuthDirectoryNode("cat-b", "cat-a", "设计输入")
        ));

        DccDirectoryImportResult result = directoryAdminService.importDirectoriesFromIntAuth();

        assertEquals(3, result.getImportedCount());
        assertEquals(1, result.getRootCount());

        when(accessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        List<DccFileDirectoryDO> directories = directoryAdminService.getDirectoryTree(99L);

        assertEquals(3, directories.size());
        DccFileDirectoryDO root = directories.stream()
                .filter(item -> item.getParentId() == null)
                .findFirst()
                .orElseThrow();
        DccFileDirectoryDO firstChild = directories.stream()
                .filter(item -> root.getId().equals(item.getParentId()))
                .findFirst()
                .orElseThrow();
        DccFileDirectoryDO grandChild = directories.stream()
                .filter(item -> firstChild.getId().equals(item.getParentId()))
                .findFirst()
                .orElseThrow();

        assertEquals("3.DMR", root.getName());
        assertEquals("01.图纸", firstChild.getName());
        assertEquals("设计输入", grandChild.getName());
        assertEquals(Boolean.TRUE, root.getActive());
        assertEquals(1, root.getSort());
        assertEquals(1, firstChild.getSort());
        assertEquals(1, grandChild.getSort());
    }

    @Test
    void testImportDirectoriesFromIntAuth_whenLocalDirectoryExists_failFast() {
        directoryMapper.insert(DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code("LOCAL-ROOT")
                .name("本地目录")
                .active(Boolean.TRUE)
                .sort(1)
                .remark("manual")
                .build());

        assertServiceException(directoryAdminService::importDirectoriesFromIntAuth,
                INTAUTH_DIRECTORY_IMPORT_NOT_ALLOWED);

        verifyNoInteractions(intAuthDirectoryClient);
    }

    @Test
    void testDeleteDirectorySubtree_withProdConfirmation_deletesBusinessGraphAndInfraFiles() throws Exception {
        DccFileDirectoryDO root = insertDirectory("ROOT", "父目录", null);
        DccFileDirectoryDO child = insertDirectory("CHILD", "子目录", root.getId());
        Long masterId = randomLongId();
        controlledFileMasterMapper.insert(DccControlledFileMasterDO.builder()
                .id(masterId)
                .categoryId(10L)
                .fileName("quality.docx")
                .fileNumber("DCC-QA-001")
                .currentActiveControlledFileId(901L)
                .status("PUBLISHED")
                .build());
        controlledFileMapper.insert(buildControlledFile(900L, masterId, root.getId(), "DRAFT",
                1001L, 1002L, 1003L, null, null, null));
        controlledFileMapper.insert(buildControlledFile(901L, masterId, child.getId(), "PUBLISHED",
                1004L, 1005L, null, 1006L, 1007L, 1008L));
        externalFileReviewMapper.insert(DccExternalFileReviewDO.builder()
                .id(3001L)
                .controlledFileId(901L)
                .externalSource("supplier")
                .externalOwner("owner")
                .reviewReason("reason")
                .participantUserIds("1")
                .outputFileId(1009L)
                .build());
        stampMapper.insert(DccControlledFileStampDO.builder()
                .id(4001L)
                .controlledFileId(901L)
                .stampType("CONTROLLED")
                .templateId("tpl")
                .rendererType("PDF_BOX")
                .outputFormat("PDF")
                .sourceFileId(1007L)
                .outputFileId(1010L)
                .status("SUCCESS")
                .build());
        accessRuleMapper.insert(DccDirectoryAccessRuleDO.builder()
                .id(5001L)
                .directoryId(child.getId())
                .subjectType("USER")
                .subjectId(100L)
                .canQuery(Boolean.TRUE)
                .canPreview(Boolean.TRUE)
                .canDownload(Boolean.TRUE)
                .active(Boolean.TRUE)
                .changeReason("cleanup test")
                .build());
        categoryDirectoryBindingMapper.insert(DccCategoryDirectoryBindingDO.builder()
                .id(6001L)
                .categoryId(10L)
                .directoryId(child.getId())
                .active(Boolean.TRUE)
                .build());
        when(fileMapper.selectBatchIds(any(Collection.class))).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> FileDO.builder().id(id).configId(1L).path("dcc/" + id).build()).toList();
        });

        DccDirectoryDeleteSubtreeResult result = directoryAdminService.deleteDirectorySubtree(root.getId(), " PROD ");

        assertEquals(2, result.getDirectoryCount());
        assertEquals(2, result.getControlledFileCount());
        assertEquals(1, result.getMasterCount());
        assertEquals(10, result.getInfraFileCount());
        assertNull(directoryMapper.selectById(root.getId()));
        assertNull(directoryMapper.selectById(child.getId()));
        assertNull(controlledFileMapper.selectById(900L));
        assertNull(controlledFileMapper.selectById(901L));
        assertNull(controlledFileMasterMapper.selectById(masterId));
        assertEquals(0, accessRuleMapper.selectCount(DccDirectoryAccessRuleDO::getDirectoryId, child.getId()));
        assertEquals(0, categoryDirectoryBindingMapper.selectCount(DccCategoryDirectoryBindingDO::getDirectoryId, child.getId()));
        verify(fileService).deleteFileList(argThat(ids -> ids.containsAll(List.of(
                1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L))));
    }

    @Test
    void testDeleteDirectorySubtree_invalidConfirmation_failsAndKeepsData() throws Exception {
        DccFileDirectoryDO root = insertDirectory("ROOT", "父目录", null);

        assertServiceException(() -> directoryAdminService.deleteDirectorySubtree(root.getId(), "prod"),
                FILE_DIRECTORY_DELETE_CONFIRM_TEXT_INVALID);

        assertNotNull(directoryMapper.selectById(root.getId()));
        verify(fileService, never()).deleteFileList(any());
    }

    @Test
    void testDeleteDirectorySubtree_activeNasTransferTask_failsAndKeepsData() throws Exception {
        DccFileDirectoryDO root = insertDirectory("ACTIVE_ROOT", "收集父目录", null);
        DccFileDirectoryDO child = insertDirectory("ACTIVE_CHILD", "收集子目录", root.getId());
        nasTransferTaskMapper.insert(DccControlledFileNasTransferTaskDO.builder()
                .id(7001L)
                .operatorUserId(1L)
                .templateCategoryId(10L)
                .effectiveDate(LocalDate.now())
                .selectedNasPathsJson("[\"收集父目录/收集子目录\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_RUNNING)
                .build());
        nasTransferTaskItemMapper.insert(DccControlledFileNasTransferTaskItemDO.builder()
                .id(7002L)
                .taskId(7001L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("收集父目录/收集子目录")
                .itemName("收集子目录")
                .resolvedDirectoryId(child.getId())
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_RUNNING)
                .attemptCount(1)
                .build());

        assertServiceException(() -> directoryAdminService.deleteDirectorySubtree(root.getId(), "PROD"),
                FILE_DIRECTORY_DELETE_NAS_TRANSFER_ACTIVE);

        assertNotNull(directoryMapper.selectById(root.getId()));
        assertNotNull(directoryMapper.selectById(child.getId()));
        verify(fileService, never()).deleteFileList(any());
    }

    @Test
    void testStopActiveNasTransfer_thenDeleteDirectorySubtree_success() throws Exception {
        DccFileDirectoryDO root = insertDirectory("WAITING_ROOT", "等待父目录", null);
        DccFileDirectoryDO child = insertDirectory("WAITING_CHILD", "等待子目录", root.getId());
        nasTransferTaskMapper.insert(DccControlledFileNasTransferTaskDO.builder()
                .id(7101L)
                .operatorUserId(1L)
                .templateCategoryId(10L)
                .effectiveDate(LocalDate.now())
                .selectedNasPathsJson("[\"等待父目录/等待子目录\"]")
                .status(DccControlledFileNasTransferServiceImpl.TASK_STATUS_WAITING)
                .build());
        nasTransferTaskItemMapper.insert(DccControlledFileNasTransferTaskItemDO.builder()
                .id(7102L)
                .taskId(7101L)
                .itemType(DccControlledFileNasTransferServiceImpl.ITEM_TYPE_DIRECTORY)
                .nasPath("等待父目录/等待子目录")
                .itemName("等待子目录")
                .resolvedDirectoryId(child.getId())
                .status(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_WAITING)
                .attemptCount(0)
                .build());

        DccDirectoryActiveNasTransferRespVO activeTransfer =
                nasTransferGuardService.getActiveTransfer(root.getId());
        DccDirectoryActiveNasTransferRespVO stoppedTransfer =
                nasTransferGuardService.stopActiveTransfer(root.getId());
        DccDirectoryDeleteSubtreeResult result = directoryAdminService.deleteDirectorySubtree(root.getId(), "PROD");

        assertEquals(Boolean.TRUE, activeTransfer.getActive());
        assertEquals(7101L, activeTransfer.getTaskId());
        assertEquals(Boolean.FALSE, stoppedTransfer.getActive());
        assertEquals(DccControlledFileNasTransferServiceImpl.TASK_STATUS_CANCELLED,
                nasTransferTaskMapper.selectById(7101L).getStatus());
        assertEquals(DccControlledFileNasTransferServiceImpl.ITEM_STATUS_CANCELLED,
                nasTransferTaskItemMapper.selectById(7102L).getStatus());
        assertEquals(2, result.getDirectoryCount());
        assertNull(directoryMapper.selectById(root.getId()));
        assertNull(directoryMapper.selectById(child.getId()));
        verify(fileService, never()).deleteFileList(any());
    }

    @Test
    void testDeleteDirectorySubtree_masterChainOutsideSubtree_failsAndKeepsData() throws Exception {
        DccFileDirectoryDO root = insertDirectory("ROOT", "父目录", null);
        DccFileDirectoryDO outside = insertDirectory("OUTSIDE", "外部目录", null);
        Long masterId = randomLongId();
        controlledFileMasterMapper.insert(DccControlledFileMasterDO.builder()
                .id(masterId)
                .categoryId(10L)
                .fileName("chain.docx")
                .fileNumber("DCC-QA-002")
                .currentActiveControlledFileId(902L)
                .status("PUBLISHED")
                .build());
        controlledFileMapper.insert(buildControlledFile(902L, masterId, root.getId(), "PUBLISHED",
                1101L, 1101L, null, null, null, null));
        controlledFileMapper.insert(buildControlledFile(903L, masterId, outside.getId(), "OBSOLETED",
                1102L, 1102L, null, null, null, null));

        assertServiceException(() -> directoryAdminService.deleteDirectorySubtree(root.getId(), "PROD"),
                FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE);

        assertNotNull(directoryMapper.selectById(root.getId()));
        assertNotNull(controlledFileMapper.selectById(902L));
        assertNotNull(controlledFileMapper.selectById(903L));
        verify(fileService, never()).deleteFileList(any());
    }

    private DccFileDirectoryDO insertDirectory(String code, String name, Long parentId) {
        return insertDirectory(code, name, parentId, Boolean.FALSE);
    }

    private DccFileDirectoryDO insertDirectory(String code, String name, Long parentId,
                                               Boolean accessRuleManuallyBound) {
        DccFileDirectoryDO directory = DccFileDirectoryDO.builder()
                .id(randomLongId())
                .code(code)
                .name(name)
                .parentId(parentId)
                .active(Boolean.TRUE)
                .sort(1)
                .remark("test")
                .accessRuleManuallyBound(accessRuleManuallyBound)
                .build();
        directoryMapper.insert(directory);
        return directory;
    }

    private DccControlledFileDO buildControlledFile(Long id, Long masterId, Long directoryId, String status,
                                                   Long sourceFileId, Long originalFileId, Long drawingPdfFileId,
                                                   Long trainingRecordFileId, Long publishedFileId,
                                                   Long stampedFileId) {
        return DccControlledFileDO.builder()
                .id(id)
                .masterId(masterId)
                .categoryId(10L)
                .directoryId(directoryId)
                .sourceFileId(sourceFileId)
                .originalFileId(originalFileId)
                .drawingPdfFileId(drawingPdfFileId)
                .trainingRecordFileId(trainingRecordFileId)
                .publishedFileId(publishedFileId)
                .stampedFileId(stampedFileId)
                .fileName("quality-" + id + ".docx")
                .title("质量文件" + id)
                .fileNumber("DCC-" + id)
                .needTraining(Boolean.FALSE)
                .processType("CONTROLLED_FILE")
                .versionNo("A")
                .status(status)
                .submitterId(1L)
                .requesterId(1L)
                .build();
    }
}
