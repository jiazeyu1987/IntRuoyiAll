package cn.iocoder.yudao.module.srm.service.supplier;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationAuditReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationSaveReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierAccessDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmSupplierPortalApplicationDO;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmErpSupplierMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierAccessMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.supplier.SrmSupplierPortalApplicationMapper;
import cn.iocoder.yudao.module.srm.enums.supplier.SrmSupplierPortalApplicationStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import({
        SrmSupplierPortalApplicationServiceImpl.class
})
class SrmSupplierPortalApplicationServiceTest extends BaseDbUnitTest {

    @Resource
    private SrmSupplierPortalApplicationService supplierPortalApplicationService;
    @Resource
    private SrmSupplierPortalApplicationMapper supplierPortalApplicationMapper;
    @Resource
    private SrmErpSupplierMapper erpSupplierMapper;
    @Resource
    private SrmSupplierAccessMapper supplierAccessMapper;
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
    void submit_shouldRequireCoreFields() {
        SrmSupplierPortalApplicationSaveReqVO reqVO = new SrmSupplierPortalApplicationSaveReqVO();
        reqVO.setCompanyName("测试供应商");

        ServiceException exception;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(200L, "portal-user")) {
            exception = assertThrows(ServiceException.class, () -> supplierPortalApplicationService.submit(reqVO));
        }

        assertTrue(exception.getMessage().contains("完整填写"));
    }

    @Test
    void submitAndApprove_shouldCreateErpSupplierAndAccessRecord() {
        SrmSupplierPortalApplicationSaveReqVO reqVO = buildSaveReq("SRM 门户供应商");

        SrmSupplierPortalApplicationRespVO submitted;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(201L, "portal-user")) {
            submitted = supplierPortalApplicationService.submit(reqVO);
        }

        assertEquals(SrmSupplierPortalApplicationStatusEnum.SUBMITTED.getStatus(), submitted.getApplicationStatus());
        SrmSupplierPortalApplicationDO persisted = supplierPortalApplicationMapper.selectByUserId(1L, 201L);
        assertNotNull(persisted);

        SrmSupplierPortalApplicationAuditReqVO auditReqVO = new SrmSupplierPortalApplicationAuditReqVO();
        auditReqVO.setId(persisted.getId());
        auditReqVO.setAuditRemark("资料完整");
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(301L, "portal-auditor")) {
            supplierPortalApplicationService.approve(auditReqVO);
        }

        SrmSupplierPortalApplicationDO approved = supplierPortalApplicationMapper.selectById(persisted.getId());
        assertEquals(SrmSupplierPortalApplicationStatusEnum.APPROVED.getStatus(), approved.getApplicationStatus());
        assertNotNull(approved.getSupplierId());

        SrmErpSupplierDO supplier = erpSupplierMapper.selectById(approved.getSupplierId());
        assertNotNull(supplier);
        assertEquals("SRM 门户供应商", supplier.getName());
        assertEquals("统一社会信用代码-001", supplier.getTaxNo());

        SrmSupplierAccessDO access = supplierAccessMapper.selectBySupplierId(1L, approved.getSupplierId());
        assertNotNull(access);
        assertEquals("张三", access.getPortalContactName());
        assertEquals("13800138000", access.getPortalContactPhone());
    }

    @Test
    void approve_shouldReuseExistingSupplierByTaxNo() {
        erpSupplierMapper.insert(SrmErpSupplierDO.builder()
                .id(901L)
                .name("旧供应商")
                .taxNo("统一社会信用代码-001")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .tenantId(1L)
                .build());
        SrmSupplierPortalApplicationSaveReqVO reqVO = buildSaveReq("复用税号供应商");

        Long applicationId;
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(202L, "portal-user-2")) {
            applicationId = supplierPortalApplicationService.submit(reqVO).getId();
        }

        SrmSupplierPortalApplicationAuditReqVO auditReqVO = new SrmSupplierPortalApplicationAuditReqVO();
        auditReqVO.setId(applicationId);
        try (MockedStatic<SecurityFrameworkUtils> ignored = mockLoginUser(302L, "portal-auditor")) {
            supplierPortalApplicationService.approve(auditReqVO);
        }

        SrmSupplierPortalApplicationDO approved = supplierPortalApplicationMapper.selectById(applicationId);
        assertEquals(901L, approved.getSupplierId());
    }

    private static SrmSupplierPortalApplicationSaveReqVO buildSaveReq(String companyName) {
        SrmSupplierPortalApplicationSaveReqVO reqVO = new SrmSupplierPortalApplicationSaveReqVO();
        reqVO.setCompanyName(companyName);
        reqVO.setUnifiedSocialCreditCode("统一社会信用代码-001");
        reqVO.setContactName("张三");
        reqVO.setContactPhone("13800138000");
        reqVO.setContactEmail("portal@example.com");
        reqVO.setQualificationAttachmentUrls("http://files.local/portal.pdf");
        reqVO.setQualificationExpireDate(LocalDate.now().plusDays(90));
        reqVO.setBankName("招商银行");
        reqVO.setBankAccount("6222021234567890");
        reqVO.setBankAddress("深圳南山");
        return reqVO;
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser(Long userId, String nickname) {
        MockedStatic<SecurityFrameworkUtils> mocked = mockStatic(SecurityFrameworkUtils.class);
        mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
        mocked.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn(nickname);
        return mocked;
    }
}
