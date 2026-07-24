package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(RoleCategoryServiceImpl.class)
class RoleCategoryServiceImplTest extends BaseDbUnitTest {

    @Resource
    private RoleCategoryServiceImpl roleCategoryService;
    @Resource
    private RoleCategoryMapper roleCategoryMapper;
    @Resource
    private RoleMapper roleMapper;

    @Test
    void createRoleCategory_success() {
        RoleCategorySaveReqVO reqVO = randomPojo(RoleCategorySaveReqVO.class, o -> {
            o.setId(null);
            o.setName("展厅");
            o.setCode("showroom");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });

        Long id = roleCategoryService.createRoleCategory(reqVO);

        RoleCategoryDO category = roleCategoryMapper.selectById(id);
        assertPojoEquals(reqVO, category, "id");
    }

    @Test
    void createRoleCategory_nameDuplicate() {
        roleCategoryMapper.insert(randomPojo(RoleCategoryDO.class, o -> {
            o.setName("展厅");
            o.setCode("showroom");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        }));
        RoleCategorySaveReqVO reqVO = randomPojo(RoleCategorySaveReqVO.class, o -> {
            o.setName("展厅");
            o.setCode("showroom-new");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });

        assertServiceException(() -> roleCategoryService.createRoleCategory(reqVO),
                ROLE_CATEGORY_NAME_DUPLICATE, "展厅");
    }

    @Test
    void validateRoleCategoryEnabled_disabled() {
        RoleCategoryDO category = randomPojo(RoleCategoryDO.class, o -> {
            o.setName("批记录");
            o.setCode("batch-record");
            o.setStatus(CommonStatusEnum.DISABLE.getStatus());
        });
        roleCategoryMapper.insert(category);

        assertServiceException(() -> roleCategoryService.validateRoleCategoryEnabled(category.getId()),
                ROLE_CATEGORY_DISABLE, "批记录");
    }

    @Test
    void deleteRoleCategory_hasRoles() {
        RoleCategoryDO category = randomPojo(RoleCategoryDO.class, o -> {
            o.setName("排产");
            o.setCode("scheduling");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        roleCategoryMapper.insert(category);
        roleMapper.insert(randomPojo(RoleDO.class, o -> {
            o.setName("排产员");
            o.setCode("scheduler");
            o.setCategoryId(category.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setType(RoleTypeEnum.CUSTOM.getType());
            o.setDataScope(DataScopeEnum.ALL.getScope());
        }));

        assertServiceException(() -> roleCategoryService.deleteRoleCategory(category.getId()),
                ROLE_CATEGORY_HAS_ROLES, "排产");
        assertNotNull(roleCategoryMapper.selectById(category.getId()));
    }

}
