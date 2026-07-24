package cn.iocoder.yudao.module.showroom.foundation;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductCommentDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionRelationDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.prompt.ShowroomImagePromptVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestItemDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductCommentMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.version.ShowroomVersionBundleMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.prompt.ShowroomImagePromptVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShowroomSchemaMapperContractTest extends BaseDbUnitTest {

    @Resource
    private ShowroomProductRevisionRelationMapper productRevisionRelationMapper;

    @Resource
    private ShowroomChangeRequestMapper changeRequestMapper;

    @Resource
    private ShowroomChangeRequestItemMapper changeRequestItemMapper;

    @Resource
    private ShowroomFieldAssignmentMapper fieldAssignmentMapper;

    @Resource
    private ShowroomProductCommentMapper productCommentMapper;

    @Resource
    private ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    @Resource
    private ShowroomImagePromptVersionMapper imagePromptVersionMapper;

    @Resource
    private ShowroomVersionBundleMapper versionBundleMapper;

    @Test
    void remediationMappersShouldPersistRowsAgainstUnitTestSchema() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 19, 13, 35);

        ShowroomProductRevisionRelationDO relation = ShowroomProductRevisionRelationDO.builder()
                .productRevisionId(101L)
                .relatedProductId(202L)
                .relationType("RELATED")
                .build();
        productRevisionRelationMapper.insert(relation);
        assertNotNull(relation.getId());
        assertEquals(1, productRevisionRelationMapper.selectListByProductRevisionId(101L).size());

        ShowroomChangeRequestDO request = ShowroomChangeRequestDO.builder()
                .targetType("PRODUCT")
                .targetId(101L)
                .targetRevisionId(301L)
                .moduleCode("PRODUCT_CORE")
                .requestType("CONTENT_UPDATE")
                .submissionSource("MANUAL")
                .status("PENDING_SUPERVISOR_REVIEW")
                .processInstanceId("pi-showroom-001")
                .submittedBy(401L)
                .submitterDeptId(501L)
                .submittedAt(now)
                .supervisorUserId(601L)
                .supervisorDeptId(501L)
                .sourceAssignmentId(701L)
                .build();
        changeRequestMapper.insert(request);
        assertNotNull(request.getId());
        assertEquals(request.getId(), changeRequestMapper.selectByProcessInstanceId("pi-showroom-001").getId());

        ShowroomChangeRequestItemDO item = ShowroomChangeRequestItemDO.builder()
                .changeRequestId(request.getId())
                .fieldCode("name_cn")
                .oldValueJson("{\"value\":\"旧名称\"}")
                .newValueJson("{\"value\":\"新名称\"}")
                .approvalStatus("PENDING")
                .build();
        changeRequestItemMapper.insert(item);
        assertNotNull(item.getId());
        assertEquals(1, changeRequestItemMapper.selectListByChangeRequestId(request.getId()).size());

        ShowroomFieldAssignmentDO assignment = ShowroomFieldAssignmentDO.builder()
                .targetType("PRODUCT")
                .targetId(101L)
                .fieldCode("name_cn")
                .assigneeUserId(801L)
                .assignedBy(802L)
                .status("OPEN")
                .notifyMessageId(803L)
                .createdAt(now)
                .build();
        assignment.setTenantId(TenantContextHolder.getRequiredTenantId());
        fieldAssignmentMapper.insert(assignment);
        assertNotNull(assignment.getId());
        assertEquals(assignment.getId(),
                fieldAssignmentMapper.selectLatestByTargetAndField("PRODUCT", 101L, "name_cn").getId());

        ShowroomProductCommentDO comment = ShowroomProductCommentDO.builder()
                .productId(101L)
                .targetRevisionId(301L)
                .anchorType("FIELD")
                .anchorKey("name_cn")
                .content("需要补齐展厅名称讨论")
                .status("OPEN")
                .createdBy(901L)
                .createdAt(now)
                .build();
        productCommentMapper.insert(comment);
        assertNotNull(comment.getId());
        assertEquals(1, productCommentMapper.selectListByProductId(101L).size());

        ShowroomPreviewAssetVersionDO previewAsset = ShowroomPreviewAssetVersionDO.builder()
                .targetType("PRODUCT")
                .targetId(101L)
                .sourceRevisionId(301L)
                .versionNo(1)
                .imageFileId(1001L)
                .status("PUBLISHED")
                .generatedByAi(Boolean.FALSE)
                .generatedAt(now)
                .publishedAt(now)
                .build();
        previewAsset.setTenantId(TenantContextHolder.getRequiredTenantId());
        previewAssetVersionMapper.insert(previewAsset);
        assertNotNull(previewAsset.getId());
        assertEquals(previewAsset.getId(),
                previewAssetVersionMapper.selectLatestPublishedByKey("PRODUCT", 101L).getId());

        ShowroomVersionBundleDO bundle = ShowroomVersionBundleDO.builder()
                .targetType("PRODUCT")
                .targetId(101L)
                .revisionId(301L)
                .revisionNo(2)
                .releasePreviewAssetVersionId(previewAsset.getId())
                .narrationZhVersionId(1101L)
                .narrationEnVersionId(1102L)
                .copiedFromRevisionId(201L)
                .publishedBy(1002L)
                .publishedAt(now)
                .build();
        bundle.setTenantId(TenantContextHolder.getRequiredTenantId());
        versionBundleMapper.insert(bundle);
        assertNotNull(bundle.getId());
        assertEquals(bundle.getId(), versionBundleMapper.selectByTargetAndRevision("PRODUCT", 101L, 301L).getId());

        ShowroomImagePromptVersionDO promptVersion = ShowroomImagePromptVersionDO.builder()
                .sceneCode("PRODUCT_COVER")
                .versionNo(1)
                .templateText("主体是“{{product_name_cn}}”")
                .placeholderCodesJson("[\"product_name_cn\"]")
                .useCount(0)
                .build();
        imagePromptVersionMapper.insert(promptVersion);
        assertNotNull(promptVersion.getId());
        assertEquals(promptVersion.getId(),
                imagePromptVersionMapper.selectLatestBySceneCode("PRODUCT_COVER").getId());
    }

    @Test
    void versionBundleMapperShouldScopeReadsByCurrentTenant() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 30, 9, 30);

        ShowroomVersionBundleDO tenantOneBundle = ShowroomVersionBundleDO.builder()
                .targetType("PRODUCT")
                .targetId(888L)
                .revisionId(999L)
                .revisionNo(1)
                .publishedBy(1001L)
                .publishedAt(now)
                .build();
        tenantOneBundle.setTenantId(1L);
        versionBundleMapper.insert(tenantOneBundle);

        ShowroomVersionBundleDO tenantTwoBundle = ShowroomVersionBundleDO.builder()
                .targetType("PRODUCT")
                .targetId(888L)
                .revisionId(999L)
                .revisionNo(2)
                .publishedBy(2001L)
                .publishedAt(now.plusMinutes(1))
                .build();
        tenantTwoBundle.setTenantId(2L);
        versionBundleMapper.insert(tenantTwoBundle);

        TenantContextHolder.setTenantId(1L);

        assertEquals(tenantOneBundle.getId(),
                versionBundleMapper.selectByTargetAndRevision("PRODUCT", 888L, 999L).getId());
        assertEquals(List.of(tenantOneBundle.getId()), versionBundleMapper.selectListByTarget("PRODUCT", 888L)
                .stream()
                .map(ShowroomVersionBundleDO::getId)
                .toList());
    }

}
