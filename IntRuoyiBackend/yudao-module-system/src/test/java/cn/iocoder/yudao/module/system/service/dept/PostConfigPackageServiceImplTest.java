package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(PostConfigPackageServiceImpl.class)
class PostConfigPackageServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PostConfigPackageServiceImpl postConfigPackageService;

    @Test
    void importPackage_shouldReturnBusinessErrorWhenPostCodeMissing() {
        PostConfigPackageServiceImpl.PostConfigItem item = new PostConfigPackageServiceImpl.PostConfigItem();
        item.setName("组织角色");
        item.setSort(1);

        PostConfigPackageServiceImpl.PostConfigPackage payload = new PostConfigPackageServiceImpl.PostConfigPackage();
        payload.setPackageVersion("1");
        payload.setPosts(java.util.List.of(item));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> postConfigPackageService.importPackage(JsonUtils.toJsonByte(payload)));

        assertEquals(ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID.getCode(), exception.getCode());
        assertEquals("配置包内容非法，原因：组织角色配置包缺少 post code", exception.getMessage());
    }

    @Test
    void importPackage_shouldAllowEmptyPostsRoundTrip() {
        PostConfigPackageServiceImpl.PostConfigPackage payload = new PostConfigPackageServiceImpl.PostConfigPackage();
        payload.setPackageVersion("1");

        postConfigPackageService.importPackage(JsonUtils.toJsonByte(payload));

        String exported = new String(postConfigPackageService.exportPackage());
        assertEquals("{\"packageVersion\":\"1\",\"posts\":[]}", exported);
    }
}
