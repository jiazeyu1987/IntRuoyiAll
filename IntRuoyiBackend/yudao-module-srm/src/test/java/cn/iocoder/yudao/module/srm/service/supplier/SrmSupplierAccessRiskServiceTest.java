package cn.iocoder.yudao.module.srm.service.supplier;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.*;
import cn.iocoder.yudao.module.srm.controller.admin.supplieraccess.vo.SrmSupplierProfileRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.SrmSupplierRiskResolveReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierrisk.vo.SrmSupplierRiskSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierAccessMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierAccessStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskLevelEnum;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierRiskSourceTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({
        SrmSupplierAccessRiskServiceImpl.class,
        SrmSupplierPortalApplicationServiceImpl.class
})
class SrmSupplierAccessRiskServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmSupplierAccessRiskServiceImpl supplierAccessRiskService;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSupplierAccessMapper supplierAccessMapper;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    @Resource
    private DataSource dataSource;

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS contact VARCHAR(128)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS mobile VARCHAR(32)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS email VARCHAR(128)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS remark VARCHAR(512)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS tax_no VARCHAR(64)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS bank_name VARCHAR(128)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS bank_account VARCHAR(255)");
            statement.execute("ALTER TABLE erp_supplier ADD COLUMN IF NOT EXISTS bank_address VARCHAR(255)");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS srm_supplier_portal_application (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  tenant_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  supplier_id BIGINT,
                  company_name VARCHAR(128),
                  unified_social_credit_code VARCHAR(64),
                  contact_name VARCHAR(64),
                  contact_phone VARCHAR(32),
                  contact_email VARCHAR(128),
                  qualification_attachment_urls VARCHAR(2000),
                  qualification_expire_date DATE,
                  bank_name VARCHAR(128),
                  bank_account VARCHAR(128),
                  bank_address VARCHAR(255),
                  application_status VARCHAR(32) NOT NULL,
                  submitter_name VARCHAR(64),
                  submitted_time TIMESTAMP,
                  audit_by BIGINT,
                  audit_name VARCHAR(64),
                  audit_time TIMESTAMP,
                  audit_remark VARCHAR(500),
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  deleted BIT DEFAULT 0
                )
                """);
        }
    }

    @Test
    void checkSupplierEligibility_shouldPassForApprovedEnabledSupplierWithoutOpenHighRisk() {
        insertSupplier(100L, "合格供应商");
        insertApprovedPortalApplication(100L, 10L, "creator");

        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(100L, "首次准入"));
        }
        progressToApprovedAccess(accessId);

        SrmSupplierEligibilityRespVO result = supplierAccessRiskService.checkSupplierEligibility(100L);

        assertTrue(result.getEligible());
        assertEquals(SrmSupplierAccessStatusEnum.APPROVED.getStatus(), result.getAccessStatus());
        assertEquals(0L, result.getOpenHighRiskCount());
        assertNull(result.getBlockedReason());
    }

    @Test
    void checkSupplierEligibility_shouldBlockWhenAccessPending() {
        insertSupplier(101L, "待审供应商");
        insertApprovedPortalApplication(101L, 10L, "creator");
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(101L, "待审核"));
        }

        SrmSupplierEligibilityRespVO result = supplierAccessRiskService.checkSupplierEligibility(101L);

        assertFalse(result.getEligible());
        assertTrue(result.getBlockedReason().contains("待审核"));
    }

    @Test
    void checkSupplierEligibility_shouldBlockWhenAccessDisabled() {
        insertSupplier(102L, "停用供应商");
        insertApprovedPortalApplication(102L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(102L, "创建"));
        }
        progressToApprovedAccess(accessId);
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "auditor")) {
            supplierAccessRiskService.enableSupplierAccess(buildEnableReq(accessId, false, "暂停合作"));
        }

        SrmSupplierEligibilityRespVO result = supplierAccessRiskService.checkSupplierEligibility(102L);

        assertFalse(result.getEligible());
        assertTrue(result.getBlockedReason().contains("停用"));
    }

    @Test
    void checkSupplierEligibility_shouldBlockWhenOpenHighRiskExistsAndKeepTraceability() {
        insertSupplier(103L, "高风险供应商");
        insertApprovedPortalApplication(103L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(103L, "创建"));
        }
        progressToApprovedAccess(accessId);
        Long riskId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "risk-owner")) {
            riskId = supplierAccessRiskService.createSupplierRisk(buildRiskSaveReq(103L, accessId));
        }

        SrmSupplierEligibilityRespVO blocked = supplierAccessRiskService.checkSupplierEligibility(103L);

        assertFalse(blocked.getEligible());
        assertEquals(1L, blocked.getOpenHighRiskCount());
        assertTrue(blocked.getBlockedReason().contains("ACCESS-103"));
        assertTrue(blocked.getOpenHighRiskSources().get(0).contains("准入申请-103"));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "resolver")) {
            SrmSupplierRiskResolveReqVO resolveReqVO = new SrmSupplierRiskResolveReqVO();
            resolveReqVO.setId(riskId);
            resolveReqVO.setResolutionRemark("已补齐资质");
            supplierAccessRiskService.resolveSupplierRisk(resolveReqVO);
        }

        SrmSupplierEligibilityRespVO resolved = supplierAccessRiskService.checkSupplierEligibility(103L);
        assertTrue(resolved.getEligible());
        assertEquals(0L, resolved.getOpenHighRiskCount());
    }

    @Test
    void validateSupplierEligible_shouldBlockWhenCurrentTenantHasNoAccessRecord() {
        insertSupplier(104L, "未建档供应商");
        ServiceException exception = assertThrows(ServiceException.class,
                () -> supplierAccessRiskService.validateSupplierEligible(104L));

        assertTrue(exception.getMessage().contains("当前租户"));
    }

    @Test
    void createSupplierAccess_shouldFailWhenReferenceSupplierBelongsToAnotherTenant() {
        insertSupplier(105L, "其他租户供应商", 2L);

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            exception = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(105L, "cross-tenant")));
        }

        assertTrue(exception.getMessage().contains("不属于当前租户"));
    }

    @Test
    void approveSupplierAccess_shouldFailWhenSubmitterAuditsSelf() {
        insertSupplier(106L, "自审供应商");
        insertApprovedPortalApplication(106L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(106L, "首次准入"));
        }

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            exception = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.approveSupplierAccess(buildAuditReq(accessId, "自审通过")));
        }

        assertTrue(exception.getMessage().contains("不能自审"));
        assertEquals(SrmSupplierAccessStatusEnum.PENDING.getStatus(),
                supplierAccessMapper.selectById(accessId).getAccessStatus());
    }

    @Test
    void approveSupplierAccess_shouldFailWhenOpenHighRiskExists() {
        insertSupplier(107L, "高风险待审供应商");
        insertApprovedPortalApplication(107L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(107L, "首次准入"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "risk-owner")) {
            supplierAccessRiskService.createSupplierRisk(buildRiskSaveReq(107L, accessId));
        }

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "auditor")) {
            supplierAccessRiskService.approveSampleTest(buildAuditReq(accessId, "样品通过"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "trial-auditor")) {
            supplierAccessRiskService.approveTrialOrder(buildAuditReq(accessId, "试用通过"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "auditor")) {
            exception = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.approveSupplierAccess(buildAuditReq(accessId, "忽略风险通过")));
        }

        assertTrue(exception.getMessage().contains("未处理高风险"));
        assertEquals(SrmSupplierAccessStatusEnum.PENDING.getStatus(),
                supplierAccessMapper.selectById(accessId).getAccessStatus());
    }

    @Test
    void resolveSupplierRisk_shouldFailWhenResolutionRemarkIsBlank() {
        insertSupplier(108L, "风险处理说明供应商");
        insertApprovedPortalApplication(108L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(buildAccessSaveReq(108L, "首次准入"));
        }
        Long riskId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "risk-owner")) {
            riskId = supplierAccessRiskService.createSupplierRisk(buildRiskSaveReq(108L, accessId));
        }

        SrmSupplierRiskResolveReqVO resolveReqVO = new SrmSupplierRiskResolveReqVO();
        resolveReqVO.setId(riskId);
        resolveReqVO.setResolutionRemark(" ");

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "resolver")) {
            exception = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.resolveSupplierRisk(resolveReqVO));
        }

        assertTrue(exception.getMessage().contains("处理说明"));
        assertFalse(supplierAccessRiskService.checkSupplierEligibility(108L).getEligible());
    }

    @Test
    void createSupplierAccess_shouldPersistPortalProfileAndQualificationExpiry() {
        insertSupplier(109L, "门户资料供应商");
        insertApprovedPortalApplication(109L, 10L, "creator");

        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(109L, "门户提交", "联系人甲", "13800138000", LocalDate.now().plusDays(45)));
        }

        SrmSupplierAccessDO access = supplierAccessMapper.selectById(accessId);
        assertEquals("联系人甲", access.getPortalContactName());
        assertEquals("13800138000", access.getPortalContactPhone());
        assertEquals(LocalDate.now().plusDays(45), access.getQualificationExpireDate());
    }

    @Test
    void deleteSupplierAccess_shouldRemoveAccessRecordAndResetEligibility() {
        insertSupplier(114L, "可删除准入供应商");
        insertApprovedPortalApplication(114L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(114L, "待删除档案", "联系人己", "13400134000", LocalDate.now().plusDays(45)));
        }

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "auditor")) {
            supplierAccessRiskService.deleteSupplierAccess(accessId);
        }

        assertNull(supplierAccessMapper.selectById(accessId));
        SrmSupplierEligibilityRespVO result = supplierAccessRiskService.checkSupplierEligibility(114L);
        assertFalse(result.getEligible());
        assertTrue(result.getBlockedReason().contains("尚未建立"));
    }

    @Test
    void deleteSupplierAccess_shouldAllowRecreateAfterHistoricalDeletion() {
        insertSupplier(115L, "可重复删档供应商");
        insertApprovedPortalApplication(115L, 10L, "creator");
        Long firstAccessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            firstAccessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(115L, "首次建档", "联系人庚", "13300133000", LocalDate.now().plusDays(45)));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "auditor")) {
            supplierAccessRiskService.deleteSupplierAccess(firstAccessId);
        }

        Long recreatedAccessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "creator-2")) {
            recreatedAccessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(115L, "二次建档", "联系人辛", "13200132000", LocalDate.now().plusDays(60)));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "auditor-2")) {
            supplierAccessRiskService.deleteSupplierAccess(recreatedAccessId);
        }

        assertNull(supplierAccessMapper.selectById(firstAccessId));
        assertNull(supplierAccessMapper.selectById(recreatedAccessId));
        assertNull(supplierAccessMapper.selectBySupplierId(1L, 115L));
    }

    @Test
    void checkSupplierEligibility_shouldBlockWhenQualificationExpired() {
        insertSupplier(110L, "资质过期供应商");
        insertApprovedPortalApplication(110L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(110L, "资质过期", "联系人乙", "13900139000", LocalDate.now().minusDays(1)));
        }
        progressToApprovedAccess(accessId);

        SrmSupplierEligibilityRespVO result = supplierAccessRiskService.checkSupplierEligibility(110L);

        assertFalse(result.getEligible());
        assertTrue(result.getBlockedReason().contains("资质"));
    }

    @Test
    void approveSupplierAccess_shouldFailWhenSampleOrTrialStageNotPassed() {
        insertSupplier(111L, "阶段未完成供应商");
        insertApprovedPortalApplication(111L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(111L, "阶段待完成", "联系人丙", "13700137000", LocalDate.now().plusDays(60)));
        }

        ServiceException sampleBlocked;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "auditor")) {
            sampleBlocked = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.approveSupplierAccess(buildAuditReq(accessId, "直接准入")));
        }
        assertTrue(sampleBlocked.getMessage().contains("样品"));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "sample-auditor")) {
            supplierAccessRiskService.approveSampleTest(buildAuditReq(accessId, "样品通过"));
        }

        ServiceException trialBlocked;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "auditor")) {
            trialBlocked = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.approveSupplierAccess(buildAuditReq(accessId, "跳过试用准入")));
        }
        assertTrue(trialBlocked.getMessage().contains("小批"));
    }

    @Test
    void sampleAndTrialStage_shouldEnforceOrderAndPersistTraceability() {
        insertSupplier(112L, "阶段留痕供应商");
        insertApprovedPortalApplication(112L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(112L, "阶段留痕", "联系人丁", "13600136000", LocalDate.now().plusDays(90)));
        }

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "trial-auditor")) {
            exception = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.approveTrialOrder(buildAuditReq(accessId, "跳过样品")));
        }
        assertTrue(exception.getMessage().contains("样品"));

        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "sample-auditor")) {
            supplierAccessRiskService.approveSampleTest(buildAuditReq(accessId, "样品通过"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "trial-auditor")) {
            supplierAccessRiskService.rejectTrialOrder(buildAuditReq(accessId, "试用不稳定"));
        }

        SrmSupplierAccessDO access = supplierAccessMapper.selectById(accessId);
        assertEquals("PASSED", access.getSampleTestStatus());
        assertEquals("sample-auditor", access.getSampleAuditName());
        assertEquals("样品通过", access.getSampleAuditRemark());
        assertEquals("REJECTED", access.getTrialOrderStatus());
        assertEquals("trial-auditor", access.getTrialAuditName());
        assertEquals("试用不稳定", access.getTrialAuditRemark());
    }

    @Test
    void getSupplierProfile_shouldAggregateAccessStageAndRiskRecords() {
        insertSupplier(113L, "统一档案供应商");
        insertApprovedPortalApplication(113L, 10L, "creator");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(113L, "统一档案", "联系人戊", "13500135000", LocalDate.now().plusDays(20)));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "sample-auditor")) {
            supplierAccessRiskService.approveSampleTest(buildAuditReq(accessId, "样品通过"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "risk-owner")) {
            supplierAccessRiskService.createSupplierRisk(buildRiskSaveReq(113L, accessId));
        }

        SrmSupplierProfileRespVO profile = supplierAccessRiskService.getSupplierProfile(113L);

        assertEquals("统一档案供应商", profile.getSupplierName());
        assertEquals("待更新", profile.getQualificationStatusLabel());
        assertEquals("PASSED", profile.getSampleTestStatus());
        assertEquals("PENDING", profile.getTrialOrderStatus());
        assertEquals("小批试用中", profile.getOnboardingStageSummary());
        assertEquals(1L, profile.getOpenHighRiskCount());
        assertEquals(1, profile.getRiskList().size());
        assertTrue(profile.getRiskList().get(0).getSourceCode().contains("ACCESS-113"));
    }

    @Test
    void approveSampleTest_shouldFailWhenPortalApplicationNotApproved() {
        insertSupplier(116L, "未过门户审核供应商");
        Long accessId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(10L, "creator")) {
            accessId = supplierAccessRiskService.createSupplierAccess(
                    buildAccessSaveReq(116L, "未过门户审核", "联系人壬", "13100131000", LocalDate.now().plusDays(45)));
        }

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "sample-auditor")) {
            exception = assertThrows(ServiceException.class,
                    () -> supplierAccessRiskService.approveSampleTest(buildAuditReq(accessId, "样品通过")));
        }

        assertTrue(exception.getMessage().contains("门户资料审核通过"));
    }

    private void insertSupplier(Long id, String name) {
        insertSupplier(id, name, 1L);
    }

    private void insertSupplier(Long id, String name, Long tenantId) {
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(id)
                .name(name)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .tenantId(tenantId)
                .build());
    }

    private static SrmSupplierAccessSaveReqVO buildAccessSaveReq(Long supplierId, String remark) {
        return buildAccessSaveReq(supplierId, remark, null, null, null);
    }

    private static SrmSupplierAccessSaveReqVO buildAccessSaveReq(Long supplierId, String remark,
                                                                 String portalContactName,
                                                                 String portalContactPhone,
                                                                 LocalDate qualificationExpireDate) {
        SrmSupplierAccessSaveReqVO reqVO = new SrmSupplierAccessSaveReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setAccessRemark(remark);
        reqVO.setPortalContactName(portalContactName);
        reqVO.setPortalContactPhone(portalContactPhone);
        reqVO.setQualificationExpireDate(qualificationExpireDate);
        return reqVO;
    }

    private static SrmSupplierAccessAuditReqVO buildAuditReq(Long id, String remark) {
        SrmSupplierAccessAuditReqVO reqVO = new SrmSupplierAccessAuditReqVO();
        reqVO.setId(id);
        reqVO.setAuditRemark(remark);
        return reqVO;
    }

    private static SrmSupplierAccessEnableReqVO buildEnableReq(Long id, boolean enabled, String remark) {
        SrmSupplierAccessEnableReqVO reqVO = new SrmSupplierAccessEnableReqVO();
        reqVO.setId(id);
        reqVO.setEnabled(enabled);
        reqVO.setOperationRemark(remark);
        return reqVO;
    }

    private static SrmSupplierRiskSaveReqVO buildRiskSaveReq(Long supplierId, Long supplierAccessId) {
        SrmSupplierRiskSaveReqVO reqVO = new SrmSupplierRiskSaveReqVO();
        reqVO.setSupplierId(supplierId);
        reqVO.setSupplierAccessId(supplierAccessId);
        reqVO.setRiskLevel(SrmSupplierRiskLevelEnum.HIGH.getLevel());
        reqVO.setSourceType(SrmSupplierRiskSourceTypeEnum.ACCESS_REQUEST.getType());
        reqVO.setSourceId(supplierAccessId);
        reqVO.setSourceCode("ACCESS-" + supplierId);
        reqVO.setSourceName("准入申请-" + supplierId);
        reqVO.setRiskDescription("资质文件过期");
        reqVO.setRiskRemark("等待补充");
        return reqVO;
    }

    private void progressToApprovedAccess(Long accessId) {
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(11L, "sample-auditor")) {
            supplierAccessRiskService.approveSampleTest(buildAuditReq(accessId, "样品通过"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(12L, "trial-auditor")) {
            supplierAccessRiskService.approveTrialOrder(buildAuditReq(accessId, "试用通过"));
        }
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(13L, "access-auditor")) {
            supplierAccessRiskService.approveSupplierAccess(buildAuditReq(accessId, "准入通过"));
        }
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId, String nickname) {
        MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class);
        mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        mocked.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn(nickname);
        return mocked;
    }

    private void insertApprovedPortalApplication(Long supplierId, Long userId, String submitterName) {
        supplierPortalApplicationMapper.insert(SrmSupplierPortalApplicationDO.builder()
                .tenantId(1L)
                .userId(userId)
                .supplierId(supplierId)
                .companyName("门户申请-" + supplierId)
                .unifiedSocialCreditCode("USCC-" + supplierId)
                .contactName("联系人-" + supplierId)
                .contactPhone("1380000" + supplierId)
                .contactEmail("portal" + supplierId + "@example.com")
                .qualificationAttachmentUrls("http://files.local/" + supplierId + ".pdf")
                .qualificationExpireDate(LocalDate.now().plusDays(60))
                .bankName("招商银行")
                .bankAccount("622202" + supplierId)
                .bankAddress("深圳")
                .applicationStatus(SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus())
                .submitterName(submitterName)
                .submittedTime(LocalDateTime.now().minusDays(1))
                .auditBy(99L)
                .auditName("portal-auditor")
                .auditTime(LocalDateTime.now())
                .auditRemark("通过")
                .build());
    }
}
