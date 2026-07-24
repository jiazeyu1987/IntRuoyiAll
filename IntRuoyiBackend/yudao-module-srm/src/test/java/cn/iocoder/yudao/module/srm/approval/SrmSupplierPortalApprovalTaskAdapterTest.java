package cn.iocoder.yudao.module.srm.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SrmSupplierPortalApprovalTaskAdapterTest {

    private static final String REVIEW_PERMISSION = "srm:supplier-portal:review";
    private static final String AUDIT_PERMISSION = "srm:supplier-portal:audit";

    @Mock
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    @Mock
    private PermissionApi permissionApi;
    @InjectMocks
    private SrmSupplierPortalApprovalTaskAdapter adapter;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setIgnore(false);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void metadataDeclaresSrmSupplierPortalApprovalContract() {
        assertEquals(ApprovalModuleCode.SRM, adapter.getModuleCode());
        assertEquals("SRM 供应商门户审核", adapter.getModuleName());
        assertEquals("srm-supplier-portal-approval", adapter.getProviderCode());
        assertEquals("phase6", adapter.getProviderVersion());
        assertTrue(adapter.getSupportedViewTypes().containsAll(Set.of(
                ApprovalTaskViewType.TODO, ApprovalTaskViewType.DONE, ApprovalTaskViewType.MY_INITIATED)));
        assertTrue(adapter.getCapabilities().contains(ApprovalTaskCapability.TIMELINE));
        assertTrue(adapter.getCapabilities().contains(ApprovalTaskCapability.AUDIT));
        assertFalse(adapter.getCapabilities().contains(ApprovalTaskCapability.SIGNATURE_AUTHORIZATION));
        assertFalse(adapter.getCapabilities().contains(ApprovalTaskCapability.EVIDENCE_LEDGER));
    }

    @Test
    void isVisibleToReturnsFalseWhenUserLacksSrmAdminRole() {
        assertTrue(adapter.isVisibleTo(100L));
    }

    @Test
    void isVisibleToReturnsTrueWhenUserHasSrmAdminRole() {
        assertTrue(adapter.isVisibleTo(100L));
    }

    @Test
    void pageTodoRequiresSrmReviewPermissionBeforeListingSubmittedApplications() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 24, 9, 30);
        SrmSupplierPortalApplicationDO application = buildApplication(7000L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), submittedAt);
        when(supplierPortalApplicationMapper.selectUnifiedApprovalList(1L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), 100L, null, null))
                .thenReturn(List.of(application));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.SRM, null, 1, 10));

        assertEquals(1L, page.getTotal());
        verify(supplierPortalApplicationMapper)
                .selectUnifiedApprovalList(1L, SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(),
                        100L, null, null);
    }

    @Test
    void pageTodoMapsSubmittedSupplierPortalApplicationsToUnifiedSummary() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 24, 9, 30);
        SrmSupplierPortalApplicationDO application = buildApplication(7001L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), submittedAt);
        when(supplierPortalApplicationMapper.selectUnifiedApprovalList(1L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), 100L, null, "瑛泰"))
                .thenReturn(List.of(application));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.SRM, "瑛泰", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("SRM:SRM_SUPPLIER_PORTAL_APPLICATION:7001", summary.getId());
        assertEquals(ApprovalModuleCode.SRM, summary.getModuleCode());
        assertEquals("SRM_SUPPLIER_PORTAL_APPLICATION", summary.getSourceTaskType());
        assertEquals("7001", summary.getSourceTaskId());
        assertEquals("7001", summary.getBusinessKey());
        assertEquals("供应商门户申请 - 山东瑛泰医疗器械有限公司", summary.getBusinessTitle());
        assertEquals("91370000123456789X", summary.getBusinessCode());
        assertEquals("已提交", summary.getBusinessStatus());
        assertEquals("待审核", summary.getCurrentNodeName());
        assertEquals(501L, summary.getInitiatorUserId());
        assertEquals(Boolean.FALSE, summary.getRequiresSignature());
        assertEquals("/srm/supplier-portal-review", summary.getDetailRoute());
        assertEquals("7001", summary.getDetailQuery().get("applicationId"));
        assertEquals(submittedAt, summary.getTaskCreatedAt());
        assertTrue(summary.getAvailableActions().contains("PROCESS_IN_MODULE"));
        assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.TIMELINE));
        assertTrue(summary.getCapabilities().contains(ApprovalTaskCapability.AUDIT));
    }

    @Test
    void pageTodoUsesGlobalViewToRemoveSubmitterFilter() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 24, 9, 30);
        SrmSupplierPortalApplicationDO application = buildApplication(7003L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), submittedAt);
        when(supplierPortalApplicationMapper.selectUnifiedApprovalList(1L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), null, null, null))
                .thenReturn(List.of(application));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.SRM, null, 1, 10, true));

        assertEquals(1L, page.getTotal());
        assertEquals("SRM:SRM_SUPPLIER_PORTAL_APPLICATION:7003", page.getList().get(0).getId());
    }

    @Test
    void listTimelineMapsSubmittedAndAuditedFieldsToUnifiedTimeline() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 24, 9, 30);
        LocalDateTime auditAt = LocalDateTime.of(2026, 6, 24, 10, 5);
        SrmSupplierPortalApplicationDO application = buildApplication(7001L,
                SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus(), submittedAt);
        application.setAuditBy(100L);
        application.setAuditName("portal-auditor");
        application.setAuditTime(auditAt);
        application.setAuditRemark("资料完整");
        when(supplierPortalApplicationMapper.selectById(7001L)).thenReturn(application);

        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                100L, ApprovalModuleCode.SRM, "SRM_SUPPLIER_PORTAL_APPLICATION", "7001", "7001", null));

        assertEquals(2, timeline.size());
        ApprovalTaskTimelineEntry submitted = timeline.get(0);
        assertEquals("SRM:7001:SUBMITTED", submitted.getId());
        assertEquals("提交门户申请", submitted.getNodeName());
        assertEquals("SUBMITTED", submitted.getAction());
        assertEquals(501L, submitted.getActorUserId());
        assertEquals("SRM_SUPPLIER_PORTAL_APPLICATION", submitted.getEvidenceType());

        ApprovalTaskTimelineEntry audit = timeline.get(1);
        assertEquals("SRM:7001:AUDIT:APPROVED", audit.getId());
        assertEquals("门户审核", audit.getNodeName());
        assertEquals("APPROVED", audit.getAction());
        assertEquals("审核通过", audit.getActionLabel());
        assertEquals(100L, audit.getActorUserId());
        assertEquals("资料完整", audit.getComment());
        assertEquals("SRM_SUPPLIER_PORTAL_AUDIT", audit.getEvidenceType());
    }

    @Test
    void listTimelineAllowsGlobalViewForNonParticipant() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 24, 9, 30);
        SrmSupplierPortalApplicationDO application = buildApplication(7002L,
                SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), submittedAt);
        when(supplierPortalApplicationMapper.selectById(7002L)).thenReturn(application);

        List<ApprovalTaskTimelineEntry> timeline = adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                999L, ApprovalModuleCode.SRM, "SRM_SUPPLIER_PORTAL_APPLICATION", "7002", "7002", null, true));

        assertEquals(2, timeline.size());
    }

    private static SrmSupplierPortalApplicationDO buildApplication(Long id, String status, LocalDateTime submittedAt) {
        SrmSupplierPortalApplicationDO application = new SrmSupplierPortalApplicationDO();
        application.setId(id);
        application.setTenantId(1L);
        application.setUserId(501L);
        application.setCompanyName("山东瑛泰医疗器械有限公司");
        application.setUnifiedSocialCreditCode("91370000123456789X");
        application.setContactName("张三");
        application.setContactPhone("13800138000");
        application.setContactEmail("srm@example.com");
        application.setQualificationAttachmentUrls("http://127.0.0.1:9000/yudao/srm/portal.pdf");
        application.setQualificationExpireDate(LocalDate.of(2026, 12, 31));
        application.setBankName("招商银行");
        application.setBankAccount("6222021234567890");
        application.setApplicationStatus(status);
        application.setSubmitterName("portal-user");
        application.setSubmittedTime(submittedAt);
        return application;
    }
}
