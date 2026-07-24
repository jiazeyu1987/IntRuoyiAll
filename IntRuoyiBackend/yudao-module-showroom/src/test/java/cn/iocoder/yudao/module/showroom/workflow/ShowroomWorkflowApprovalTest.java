package cn.iocoder.yudao.module.showroom.workflow;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomVersionAuditMapper;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomApprovalDetail;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequest;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomChangeRequestItem;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomWorkflowStart;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomPersistentWorkflowService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({ShowroomPersistentContentService.class, ShowroomPersistentWorkflowService.class})
class ShowroomWorkflowApprovalTest extends BaseDbUnitTest {

    @Resource
    private ShowroomPersistentContentService contentService;

    @Resource
    private ShowroomPersistentWorkflowService workflowService;

    @Resource
    private ShowroomChangeRequestMapper changeRequestMapper;

    @Resource
    private ShowroomChangeRequestItemMapper changeRequestItemMapper;

    @Resource
    private ShowroomVersionAuditMapper versionAuditMapper;

    @Test
    void workflowShouldPersistRequestDetailAndProcessInstanceBindingAcrossServiceInstances() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Introducer Sheath Set",
                Map.of("target_market", "新市场")));

        ShowroomChangeRequest submitted = workflowService.submit(new ShowroomWorkflowStart(
                "PRODUCT", draftRevision.productId(), draftRevision.revisionId(), "product",
                "CONTENT_UPDATE", "MANUAL", null, 100L, 8L, 200L, 300L, null,
                List.of(
                        new ShowroomChangeRequestItem("name_cn",
                                jsonValue("导管鞘组 V1"), jsonValue("导管鞘组 V2")),
                        new ShowroomChangeRequestItem("target_market",
                                jsonValue("旧市场"), jsonValue("新市场"))
                )));

        ShowroomApprovalDetail detail = workflowService.getApprovalDetail(submitted.changeRequestId());
        assertEquals("PENDING_SUPERVISOR_REVIEW", detail.changeRequest().status());
        assertEquals("CONTENT_UPDATE", detail.changeRequest().requestType());
        assertEquals("MANUAL", detail.changeRequest().submissionSource());
        assertNull(detail.changeRequest().processInstanceId());
        assertEquals(2, detail.fieldDiffs().size());
        assertEquals("PENDING", detail.fieldDiffs().get(0).approvalStatus());
        assertEquals(jsonValue("导管鞘组 V1"), detail.fieldDiffs().get(0).oldValueJson());
        assertEquals(jsonValue("导管鞘组 V2"), detail.fieldDiffs().get(0).newValueJson());
        assertEquals(liveRevision.revisionId(), detail.targetPreview().liveRevisionId());
        assertEquals(draftRevision.revisionId(), detail.targetPreview().targetRevisionId());
        assertEquals("导管鞘组 V1", detail.targetPreview().liveFields().get("name_cn"));
        assertEquals("导管鞘组 V2", detail.targetPreview().targetFields().get("name_cn"));
        assertTrue(detail.versionDiffs().stream().anyMatch(audit -> "name_cn".equals(audit.fieldCode())));
        assertTrue(detail.signatureRecords().isEmpty());

        workflowService.attachProcessInstance(submitted.changeRequestId(), "pi-showroom-001");
        ShowroomPersistentWorkflowService restartedService = new ShowroomPersistentWorkflowService(
                changeRequestMapper, changeRequestItemMapper, null, versionAuditMapper, contentService);
        ShowroomChangeRequest reloaded = restartedService.getChangeRequest(submitted.changeRequestId());

        assertEquals("pi-showroom-001", reloaded.processInstanceId());
        assertEquals(2, reloaded.items().size());
    }

    @Test
    void workflowShouldRejectAtSupervisorStageWithoutPublishing() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Introducer Sheath Set",
                Map.of("target_market", "新市场")));

        ShowroomChangeRequest submitted = workflowService.submit(new ShowroomWorkflowStart(
                "PRODUCT", draftRevision.productId(), draftRevision.revisionId(), "product",
                "CONTENT_UPDATE", "MANUAL", null, 100L, 8L, 200L, 300L, null,
                List.of(new ShowroomChangeRequestItem("name_cn",
                        jsonValue("导管鞘组 V1"), jsonValue("导管鞘组 V2")))));

        ShowroomChangeRequest rejected = workflowService.supervisorReject(
                submitted.changeRequestId(), 200L, "字段内容不完整");

        assertEquals("REJECTED", rejected.status());
        assertEquals("字段内容不完整", rejected.rejectionReason());
        assertEquals("REJECTED", rejected.items().get(0).approvalStatus());
        assertEquals(liveRevision.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());
    }

    @Test
    void workflowShouldRejectAtGaoxinStageWithoutPublishing() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Introducer Sheath Set",
                Map.of("target_market", "新市场")));

        ShowroomChangeRequest submitted = workflowService.submit(new ShowroomWorkflowStart(
                "PRODUCT", draftRevision.productId(), draftRevision.revisionId(), "product",
                "CONTENT_UPDATE", "MANUAL", null, 100L, 8L, 200L, 300L, null,
                List.of(new ShowroomChangeRequestItem("target_market",
                        jsonValue("旧市场"), jsonValue("新市场")))));

        workflowService.supervisorApprove(submitted.changeRequestId(), 200L);
        ShowroomChangeRequest rejected = workflowService.gaoxinReject(
                submitted.changeRequestId(), 300L, "高新审批未通过");

        assertEquals("REJECTED", rejected.status());
        assertEquals("高新审批未通过", rejected.rejectionReason());
        assertEquals("REJECTED", rejected.items().get(0).approvalStatus());
        assertEquals(liveRevision.revisionId(),
                contentService.requireCurrentProductRevision(liveRevision.productId()).revisionId());
    }

    @Test
    void workflowShouldStartAtGaoxinApprovalWhenSubmitterDeptMissing() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V2", "Introducer Sheath Set",
                Map.of("target_market", "新市场")));

        ShowroomChangeRequest submitted = workflowService.submit(new ShowroomWorkflowStart(
                "PRODUCT", draftRevision.productId(), draftRevision.revisionId(), "product",
                "CONTENT_UPDATE", "MANUAL", null, 100L, null, null, 300L, null,
                List.of(new ShowroomChangeRequestItem("target_market",
                        jsonValue("旧市场"), jsonValue("新市场")))));

        ShowroomApprovalDetail detail = workflowService.getApprovalDetail(submitted.changeRequestId());

        assertEquals("PENDING_GAOXIN_APPROVAL", submitted.status());
        assertEquals("PENDING_GAOXIN_APPROVAL", detail.changeRequest().status());
        assertNull(detail.changeRequest().submitterDeptId());
        assertNull(detail.changeRequest().supervisorUserId());
        assertNull(detail.changeRequest().supervisorDeptId());
        assertEquals(300L, detail.changeRequest().gaoxinUserId());
        assertEquals("PENDING", detail.fieldDiffs().get(0).approvalStatus());
    }

    @Test
    void workflowShouldStartAtGaoxinApprovalWhenSubmitterDeptExistsButSupervisorMissing() {
        var liveRevision = publishBaselineProduct();
        var draftRevision = contentService.saveProductDraft(new ShowroomProductDraft(
                liveRevision.productId(), "YT-GW-001", "导管鞘组 V3", "Introducer Sheath Set",
                Map.of("target_market", "缺主管市场")));

        ShowroomChangeRequest submitted = workflowService.submit(new ShowroomWorkflowStart(
                "PRODUCT", draftRevision.productId(), draftRevision.revisionId(), "product",
                "CONTENT_UPDATE", "MANUAL", null, 100L, 8L, null, 300L, null,
                List.of(new ShowroomChangeRequestItem("target_market",
                        jsonValue("旧市场"), jsonValue("缺主管市场")))));

        ShowroomApprovalDetail detail = workflowService.getApprovalDetail(submitted.changeRequestId());

        assertEquals("PENDING_GAOXIN_APPROVAL", submitted.status());
        assertEquals("PENDING_GAOXIN_APPROVAL", detail.changeRequest().status());
        assertEquals(8L, detail.changeRequest().submitterDeptId());
        assertNull(detail.changeRequest().supervisorUserId());
        assertNull(detail.changeRequest().supervisorDeptId());
    }

    private cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision publishBaselineProduct() {
        var baseline = contentService.saveProductDraft(new ShowroomProductDraft(
                null, "YT-GW-001", "导管鞘组 V1", "Introducer Sheath Set",
                Map.of("target_market", "旧市场")));
        return contentService.publishProductRevision(baseline.revisionId(), 901L);
    }

    private static String jsonValue(String value) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("value", value);
        return JsonUtils.toJsonString(payload);
    }

}
