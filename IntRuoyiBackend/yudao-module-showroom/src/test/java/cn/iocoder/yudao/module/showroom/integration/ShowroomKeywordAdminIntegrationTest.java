package cn.iocoder.yudao.module.showroom.integration;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.showroom.controller.admin.keyword.ShowroomKeywordAdminController;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordPageReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordPageRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordSaveReqVO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import cn.iocoder.yudao.module.showroom.keyword.service.ShowroomKeywordService;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@Import({
        ShowroomKeywordAdminController.class,
        ShowroomKeywordService.class
})
class ShowroomKeywordAdminIntegrationTest extends BaseDbUnitTest {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Long TEST_TENANT_ID = 122L;
    private static final Long PUBLICITY_USER_ID = 300L;

    @Resource
    private ShowroomKeywordAdminController controller;

    @Resource
    private ShowroomKeywordMapper keywordMapper;

    @MockBean
    private SecurityFrameworkService securityFrameworkService;

    @BeforeEach
    void setUpRoleChecks() {
        when(securityFrameworkService.hasRole(anyString())).thenAnswer(invocation -> {
            String roleCode = invocation.getArgument(0);
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId == null) {
                return false;
            }
            if (ShowroomKeywordAdminController.SHOWROOM_PUBLICITY_ROLE_CODE.equals(roleCode)) {
                return PUBLICITY_USER_ID.equals(loginUserId);
            }
            if ("super_admin".equals(roleCode) || RoleCodeEnum.SUPER_ADMIN.getCode().equals(roleCode)) {
                return 1L == loginUserId;
            }
            return false;
        });
    }

    @Test
    void pageShouldReturnTenantScopedRowsAndKeywordFilter() {
        TenantUtils.execute(TEST_TENANT_ID, () -> {
            insertKeyword("上海瑛泰医疗器械自动化有限公司", "Shanghai INT Medical Instruments Automation Co., Ltd.");
            insertKeyword("珠海德瑞医疗器械有限公司", "Zhuhai Derui Medical Instruments Co., Ltd.");
        });
        TenantUtils.execute(DEFAULT_TENANT_ID, () ->
                insertKeyword("珠海德瑞医疗器械有限公司", "Other tenant duplicate"));

        PageResult<KeywordPageRespVO> page = TenantUtils.execute(TEST_TENANT_ID,
                () -> withLoginUser(PUBLICITY_USER_ID, () -> controller.getPage(pageReq("德瑞")).getCheckedData()));

        assertEquals(1L, page.getTotal());
        assertEquals("珠海德瑞医疗器械有限公司", page.getList().get(0).getNameZh());
        assertEquals("Zhuhai Derui Medical Instruments Co., Ltd.", page.getList().get(0).getNameEn());

        PageResult<KeywordPageRespVO> allRows = TenantUtils.execute(TEST_TENANT_ID,
                () -> withLoginUser(PUBLICITY_USER_ID, () -> controller.getPage(pageReq("")).getCheckedData()));
        assertEquals(2L, allRows.getTotal());
        assertTrue(allRows.getList().get(0).getId() < allRows.getList().get(1).getId());
    }

    @Test
    void createUpdateDeleteShouldOperateOnlyWithinCurrentTenant() {
        Long keywordId = TenantUtils.execute(TEST_TENANT_ID, () -> withLoginUser(PUBLICITY_USER_ID,
                () -> controller.create(new KeywordSaveReqVO(null, "  测试关键词  ", "  Initial English Keyword  "))
                        .getCheckedData()));

        assertNotNull(keywordId);
        ShowroomKeywordDO created = TenantUtils.execute(TEST_TENANT_ID, () -> keywordMapper.selectById(keywordId));
        assertEquals("测试关键词", created.getNameZh());
        assertEquals("Initial English Keyword", created.getNameEn());

        TenantUtils.execute(TEST_TENANT_ID, () -> withLoginUser(PUBLICITY_USER_ID, () -> {
            controller.update(new KeywordSaveReqVO(keywordId, "测试关键词", "Updated English Keyword"));
            return null;
        }));

        KeywordRespVO detail = TenantUtils.execute(TEST_TENANT_ID,
                () -> withLoginUser(PUBLICITY_USER_ID, () -> controller.get(keywordId).getCheckedData()));
        assertEquals("Updated English Keyword", detail.getNameEn());

        TenantUtils.execute(TEST_TENANT_ID, () -> withLoginUser(PUBLICITY_USER_ID, () -> {
            controller.delete(keywordId);
            return null;
        }));

        assertEquals(0, TenantUtils.execute(TEST_TENANT_ID, () -> keywordMapper.selectListOrdered().size()));
    }

    @Test
    void createShouldRejectDuplicateChineseNameWithinTenant() {
        TenantUtils.execute(TEST_TENANT_ID, () ->
                insertKeyword("上海瑛泰企业管理有限公司", "Shanghai INT Enterprise Management Co., Ltd."));
        TenantUtils.execute(DEFAULT_TENANT_ID, () ->
                insertKeyword("上海瑛泰企业管理有限公司", "Allowed in another tenant"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> TenantUtils.execute(TEST_TENANT_ID,
                        () -> withLoginUser(PUBLICITY_USER_ID,
                                () -> controller.create(new KeywordSaveReqVO(null,
                                        "上海瑛泰企业管理有限公司", "Duplicate Chinese Name")).getCheckedData())));

        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        assertTrue(cause.getMessage().contains("SHOWROOM_KEYWORD_DUPLICATE_ZH"));
        assertEquals(1,
                TenantUtils.execute(TEST_TENANT_ID,
                        () -> keywordMapper.selectListOrdered().stream()
                                .filter(keyword -> "上海瑛泰企业管理有限公司".equals(keyword.getNameZh()))
                                .count()));
    }

    private KeywordPageReqVO pageReq(String keyword) {
        KeywordPageReqVO reqVO = new KeywordPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(50);
        reqVO.setKeyword(keyword);
        return reqVO;
    }

    private void insertKeyword(String nameZh, String nameEn) {
        ShowroomKeywordDO keyword = new ShowroomKeywordDO();
        keyword.setTenantId(TenantContextHolder.getRequiredTenantId());
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        keywordMapper.insert(keyword);
    }

    private <T> T withLoginUser(Long userId, CheckedSupplier<T> supplier) {
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            return supplier.get();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
