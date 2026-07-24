package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(DccApprovalPositionConfigPackageServiceImpl.class)
class DccApprovalPositionConfigPackageServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DccApprovalPositionConfigPackageServiceImpl configPackageService;

    @Test
    void importPackage_shouldReturnBusinessErrorWhenPositionCodeMissing() {
        DccApprovalPositionConfigPackageServiceImpl.DccApprovalPositionConfigItem item =
                new DccApprovalPositionConfigPackageServiceImpl.DccApprovalPositionConfigItem();
        item.setName("审批角色");
        item.setSource("MANUAL");
        item.setActive(Boolean.TRUE);

        DccApprovalPositionConfigPackageServiceImpl.DccApprovalPositionConfigPackage payload =
                new DccApprovalPositionConfigPackageServiceImpl.DccApprovalPositionConfigPackage();
        payload.setPackageVersion("1");
        payload.setPositions(java.util.List.of(item));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> configPackageService.importPackage(JsonUtils.toJsonByte(payload)));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID.getCode(), exception.getCode());
        assertEquals("配置包内容非法，原因：审批角色配置包缺少 position code", exception.getMessage());
    }

    @Test
    void importPackage_shouldAllowEmptyPositionsRoundTrip() {
        DccApprovalPositionConfigPackageServiceImpl.DccApprovalPositionConfigPackage payload =
                new DccApprovalPositionConfigPackageServiceImpl.DccApprovalPositionConfigPackage();
        payload.setPackageVersion("1");

        configPackageService.importPackage(JsonUtils.toJsonByte(payload));

        String exported = new String(configPackageService.exportPackage());
        assertEquals("{\"packageVersion\":\"1\",\"positions\":[]}", exported);
    }
}
