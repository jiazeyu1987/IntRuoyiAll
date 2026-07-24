package cn.iocoder.yudao.module.system.service.tablecolumn;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.tablecolumn.UserTableColumnConfigDO;
import cn.iocoder.yudao.module.system.dal.mysql.tablecolumn.UserTableColumnConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_TABLE_COLUMN_CONFIG_INVALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@Import(UserTableColumnConfigServiceImpl.class)
class UserTableColumnConfigServiceImplTest extends BaseDbUnitTest {

    @Resource
    private UserTableColumnConfigService userTableColumnConfigService;
    @Resource
    private UserTableColumnConfigMapper userTableColumnConfigMapper;

    @Test
    void saveAndGet_currentUserOnly() {
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            userTableColumnConfigService.saveConfig(saveReq("mes.pro.scheduleOrder.main",
                    column("productName", true, 220),
                    column("specification", false, 160)));

            UserTableColumnConfigRespVO config = userTableColumnConfigService.getConfig("mes.pro.scheduleOrder.main");

            assertNotNull(config);
            assertEquals("mes.pro.scheduleOrder.main", config.getTableKey());
            assertEquals(2, config.getColumns().size());
            assertEquals("productName", config.getColumns().get(0).getKey());
            assertTrue(config.getColumns().get(0).getVisible());
            assertEquals(220, config.getColumns().get(0).getWidth());
            UserTableColumnConfigDO dbConfig = userTableColumnConfigMapper.selectByUserAndTableKey(101L,
                    "mes.pro.scheduleOrder.main");
            assertNotNull(dbConfig);
            assertEquals(11L, dbConfig.getTenantId());
        }
    }

    @Test
    void sameTableKey_differentUsers_isolated() {
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            userTableColumnConfigService.saveConfig(saveReq("mes.pro.scheduleOrder.main",
                    column("productName", false, 180)));

            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(202L);
            userTableColumnConfigService.saveConfig(saveReq("mes.pro.scheduleOrder.main",
                    column("productName", true, 260)));

            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            assertFalse(userTableColumnConfigService.getConfig("mes.pro.scheduleOrder.main")
                    .getColumns().get(0).getVisible());
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(202L);
            assertTrue(userTableColumnConfigService.getConfig("mes.pro.scheduleOrder.main")
                    .getColumns().get(0).getVisible());
        }
    }

    @Test
    void reset_removesCurrentUserConfigOnly() {
        TenantContextHolder.setTenantId(11L);
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            userTableColumnConfigService.saveConfig(saveReq("dcc.controlledFile.browser.main",
                    column("fileName", true, 260)));
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(202L);
            userTableColumnConfigService.saveConfig(saveReq("dcc.controlledFile.browser.main",
                    column("fileName", false, 180)));

            userTableColumnConfigService.resetConfig("dcc.controlledFile.browser.main");

            assertNull(userTableColumnConfigService.getConfig("dcc.controlledFile.browser.main"));
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            assertNotNull(userTableColumnConfigService.getConfig("dcc.controlledFile.browser.main"));
        }
    }

    @Test
    void invalidConfig_failFast() {
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            assertServiceException(() -> userTableColumnConfigService.saveConfig(saveReq("",
                    column("productName", true, 160))), USER_TABLE_COLUMN_CONFIG_INVALID);
            assertServiceException(() -> userTableColumnConfigService.saveConfig(saveReq("mes.pro.scheduleOrder.main")),
                    USER_TABLE_COLUMN_CONFIG_INVALID);
            assertServiceException(() -> userTableColumnConfigService.saveConfig(saveReq("mes.pro.scheduleOrder.main",
                    column("", true, 160))), USER_TABLE_COLUMN_CONFIG_INVALID);
            assertServiceException(() -> userTableColumnConfigService.saveConfig(saveReq("mes.pro.scheduleOrder.main",
                    column("productName", true, 0))), USER_TABLE_COLUMN_CONFIG_INVALID);
        }
    }

    private static UserTableColumnConfigSaveReqVO saveReq(String tableKey,
                                                         UserTableColumnConfigSaveReqVO.Column... columns) {
        return new UserTableColumnConfigSaveReqVO()
                .setTableKey(tableKey)
                .setColumns(List.of(columns));
    }

    private static UserTableColumnConfigSaveReqVO.Column column(String key, Boolean visible, Integer width) {
        return new UserTableColumnConfigSaveReqVO.Column()
                .setKey(key)
                .setVisible(visible)
                .setWidth(width);
    }

}
