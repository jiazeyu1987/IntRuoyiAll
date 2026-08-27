package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDingTalkImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDingTalkImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl.USER_INIT_PASSWORD_KEY;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import(AdminUserServiceImpl.class)
public class AdminUserServiceImplTest extends BaseDbUnitTest {

    private static final String TEST_INIT_PASSWORD = "Yudao2026";

    @Resource
    private AdminUserServiceImpl userService;

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private UserPostMapper userPostMapper;

    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private PostService postService;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private TenantService tenantService;
    @MockitoBean
    private FileApi fileApi;
    @MockitoBean
    private ConfigApi configApi;
    @MockitoBean
    private OAuth2TokenService oauth2TokenService;

    @BeforeEach
    public void before() {
        // mock 初始化密码
        when(configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY)).thenReturn(TEST_INIT_PASSWORD);
    }

    @Test
    public void testCreatUser_success() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setPostIds(asSet(1L, 2L));
            o.setPassword("Create2026");
        }).setId(null); // 避免 id 被赋值
        // mock 账户额度充足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock postService 的方法
        List<PostDO> posts = CollectionUtils.convertList(reqVO.getPostIds(), postId ->
                randomPojo(PostDO.class, o -> {
                    o.setId(postId);
                    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
                }));
        when(postService.getPostList(eq(reqVO.getPostIds()), isNull())).thenReturn(posts);
        // mock passwordEncoder 的方法
        when(passwordEncoder.encode(eq(reqVO.getPassword()))).thenReturn("yudaoyuanma");

        // 调用
        Long userId = userService.createUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user, "password", "id");
        assertEquals("yudaoyuanma", user.getPassword());
        assertNotNull(user.getPasswordUpdateTime());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus());
        // 断言关联岗位
        List<UserPostDO> userPosts = userPostMapper.selectListByUserId(user.getId());
        assertEquals(1L, userPosts.get(0).getPostId());
        assertEquals(2L, userPosts.get(1).getPostId());
    }

    @Test
    public void testCreatUser_max() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class);
        // mock 账户额度不足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(-1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));

        // 调用，并断言异常
        assertServiceException(() -> userService.createUser(reqVO), USER_COUNT_MAX, -1);
    }

    @Test
    public void testCreateUser_weakPassword() {
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setPassword("yuanma");
            o.setMobile(randomString());
            o.setPostIds(null);
        }).setId(null);
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        assertServiceException(() -> userService.createUser(reqVO), USER_PASSWORD_STRENGTH_INVALID);
    }

    @Test
    public void testUpdateUser_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPostIds(asSet(1L, 2L)));
        userMapper.insert(dbUser);
        userPostMapper.insert(new UserPostDO().setUserId(dbUser.getId()).setPostId(1L));
        userPostMapper.insert(new UserPostDO().setUserId(dbUser.getId()).setPostId(2L));
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setId(dbUser.getId());
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setPostIds(asSet(2L, 3L));
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock postService 的方法
        List<PostDO> posts = CollectionUtils.convertList(reqVO.getPostIds(), postId ->
                randomPojo(PostDO.class, o -> {
                    o.setId(postId);
                    o.setStatus(CommonStatusEnum.ENABLE.getStatus());
                }));
        when(postService.getPostList(eq(reqVO.getPostIds()), isNull())).thenReturn(posts);

        // 调用
        userService.updateUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, user, "password");
        // 断言关联岗位
        List<UserPostDO> userPosts = userPostMapper.selectListByUserId(user.getId());
        assertEquals(2L, userPosts.get(0).getPostId());
        assertEquals(3L, userPosts.get(1).getPostId());
    }

    @Test
    public void testUpdateUserLogin() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setLoginDate(null));
        userMapper.insert(user);
        // 准备参数
        Long id = user.getId();
        String loginIp = randomString();

        // 调用
        userService.updateUserLogin(id, loginIp);
        // 断言
        AdminUserDO dbUser = userMapper.selectById(id);
        assertEquals(loginIp, dbUser.getLoginIp());
        assertNotNull(dbUser.getLoginDate());
    }

    @Test
    public void testRecordUserLoginFailure_lockOnFifthFailure() {
        AdminUserDO user = randomAdminUserDO(o -> {
            o.setLoginFailureCount(4);
            o.setLoginLocked(0);
            o.setLoginLockedTime(null);
        });
        userMapper.insert(user);

        userService.recordUserLoginFailure(user.getId());

        AdminUserDO dbUser = userMapper.selectById(user.getId());
        assertEquals(5, dbUser.getLoginFailureCount());
        assertEquals(1, dbUser.getLoginLocked());
        assertNotNull(dbUser.getLoginLockedTime());
    }

    @Test
    public void testResetUserLoginFailure() {
        AdminUserDO user = randomAdminUserDO(o -> {
            o.setLoginFailureCount(4);
            o.setLoginLocked(1);
            o.setLoginLockedTime(LocalDateTime.now().minusMinutes(10));
        });
        userMapper.insert(user);

        userService.resetUserLoginFailure(user.getId());

        AdminUserDO dbUser = userMapper.selectById(user.getId());
        assertEquals(0, dbUser.getLoginFailureCount());
        assertEquals(0, dbUser.getLoginLocked());
        assertNull(dbUser.getLoginLockedTime());
    }

    @Test
    public void testUpdateUserProfile_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        UserProfileUpdateReqVO reqVO = randomPojo(UserProfileUpdateReqVO.class, o -> {
            o.setMobile(randomString());
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setAvatar(randomURL());
        });

        // 调用
        userService.updateUserProfile(userId, reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user);
    }

    @Test
    public void testUpdateUserPassword_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPassword("encode:tudou"));
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        UserProfileUpdatePasswordReqVO reqVO = randomPojo(UserProfileUpdatePasswordReqVO.class, o -> {
            o.setOldPassword("tudou");
            o.setNewPassword("Yuanma2026");
        });
        // mock 方法
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));
        when(passwordEncoder.matches(eq(reqVO.getOldPassword()), eq(dbUser.getPassword()))).thenReturn(true);

        // 调用
        userService.updateUserPassword(userId, reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals("encode:Yuanma2026", user.getPassword());
        assertNotNull(user.getPasswordUpdateTime());
    }

    @Test
    public void testUpdateUserPassword_weakPassword() {
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPassword("encode:tudou"));
        userMapper.insert(dbUser);
        UserProfileUpdatePasswordReqVO reqVO = randomPojo(UserProfileUpdatePasswordReqVO.class, o -> {
            o.setOldPassword("tudou");
            o.setNewPassword("12345678");
        });
        when(passwordEncoder.matches(eq(reqVO.getOldPassword()), eq(dbUser.getPassword()))).thenReturn(true);

        assertServiceException(() -> userService.updateUserPassword(dbUser.getId(), reqVO),
                USER_PASSWORD_STRENGTH_INVALID);
    }

    @Test
    public void testUpdateUserPassword02_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        String password = "Yudao2026";
        // mock 方法
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));

        // 调用
        userService.updateUserPassword(userId, password);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals("encode:" + password, user.getPassword());
        assertNotNull(user.getPasswordUpdateTime());
    }

    @Test
    public void testUpdateUserPassword02_weakPassword() {
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);

        assertServiceException(() -> userService.updateUserPassword(dbUser.getId(), "yudao"),
                USER_PASSWORD_STRENGTH_INVALID);
    }

    @Test
    public void testUpdateUserStatus() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        Integer status = randomCommonStatus();

        // 调用
        userService.updateUserStatus(userId, status);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals(status, user.getStatus());
    }

    @Test
    public void testDeleteUser_success(){
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用数据
        userService.deleteUser(userId);
        // 校验结果
        assertNull(userMapper.selectById(userId));
        // 校验调用次数
        verify(permissionService, times(1)).processUserDeleted(eq(userId));
    }

    @Test
    public void testDeleteUserList_empty() {
        assertServiceException(() -> userService.deleteUserList(Collections.emptyList()),
                USER_DELETE_LIST_IS_EMPTY);
    }

    @Test
    public void testGetUserByUsername() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        String username = dbUser.getUsername();

        // 调用
        AdminUserDO user = userService.getUserByUsername(username);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserByMobile() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        String mobile = dbUser.getMobile();

        // 调用
        AdminUserDO user = userService.getUserByMobile(mobile);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserPage() {
        // mock 数据
        AdminUserDO dbUser = initGetUserPageData();
        // 准备参数
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setUsername("tu");
        reqVO.setMobile("1560");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2020, 12, 1, 2020, 12, 24));
        reqVO.setDeptId(1L); // 其中，1L 是 2L 的父部门
        // mock 方法
        List<DeptDO> deptList = newArrayList(randomPojo(DeptDO.class, o -> o.setId(2L)));
        when(deptService.getChildDeptList(eq(reqVO.getDeptId()))).thenReturn(deptList);

        // 调用
        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbUser, pageResult.getList().get(0));
    }

    @Test
    public void testGetUserPage_prioritizesDeptLeaderOnFirstPage() {
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setDeptId(1L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);

        when(deptService.getChildDeptList(eq(reqVO.getDeptId()))).thenReturn(Collections.emptyList());

        AdminUserDO earliestMember = randomAdminUserDO(o -> {
            o.setDeptId(1L);
            o.setUsername("member-earliest");
        });
        userMapper.insert(earliestMember);
        AdminUserDO deptLeader = randomAdminUserDO(o -> {
            o.setDeptId(1L);
            o.setUsername("dept-leader");
        });
        userMapper.insert(deptLeader);
        AdminUserDO latestMember = randomAdminUserDO(o -> {
            o.setDeptId(1L);
            o.setUsername("member-latest");
        });
        userMapper.insert(latestMember);

        when(deptService.getDeptList(argThat((Collection<Long> ids) -> ids != null && ids.size() == 1 && ids.contains(1L))))
                .thenReturn(singletonList(new DeptDO().setId(1L).setLeaderUserId(deptLeader.getId())));

        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);

        assertEquals(3, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertEquals(deptLeader.getId(), pageResult.getList().get(0).getId());
    }

    @Test
    public void testGetUserPage_prioritizesSelectedDeptLeaderBeforeChildDeptUsers() {
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setDeptId(1L);
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);

        when(deptService.getChildDeptList(eq(reqVO.getDeptId())))
                .thenReturn(singletonList(new DeptDO().setId(2L)));

        AdminUserDO selectedDeptMember = randomAdminUserDO(o -> {
            o.setDeptId(1L);
            o.setUsername("selected-member");
        });
        userMapper.insert(selectedDeptMember);
        AdminUserDO selectedDeptLeader = randomAdminUserDO(o -> {
            o.setDeptId(1L);
            o.setUsername("selected-leader");
        });
        userMapper.insert(selectedDeptLeader);
        AdminUserDO childDeptLeader = randomAdminUserDO(o -> {
            o.setDeptId(2L);
            o.setUsername("child-leader");
        });
        userMapper.insert(childDeptLeader);
        AdminUserDO childDeptMember = randomAdminUserDO(o -> {
            o.setDeptId(2L);
            o.setUsername("child-member");
        });
        userMapper.insert(childDeptMember);

        when(deptService.getDeptList(argThat((Collection<Long> ids) ->
                ids != null && ids.size() == 2 && ids.containsAll(Arrays.asList(1L, 2L)))))
                .thenReturn(Arrays.asList(
                        new DeptDO().setId(1L).setLeaderUserId(selectedDeptLeader.getId()),
                        new DeptDO().setId(2L).setLeaderUserId(childDeptLeader.getId())
                ));

        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);

        assertEquals(4, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertEquals(selectedDeptLeader.getId(), pageResult.getList().get(0).getId());
    }

    /**
     * 初始化 getUserPage 方法的测试数据
     */
    private AdminUserDO initGetUserPageData() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> { // 等会查询到
            o.setUsername("tudou");
            o.setMobile("15601691300");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2020, 12, 12));
            o.setDeptId(2L);
        });
        userMapper.insert(dbUser);
        // 测试 username 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setUsername("dou")));
        // 测试 mobile 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setMobile("18818260888")));
        // 测试 status 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 测试 createTime 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setCreateTime(buildTime(2020, 11, 11))));
        // 测试 dept 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setDeptId(0L)));
        return dbUser;
    }

    @Test
    public void testGetUser() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用
        AdminUserDO user = userService.getUser(userId);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserListByDeptIds() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setDeptId(1L));
        userMapper.insert(dbUser);
        // 测试 deptId 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setDeptId(2L)));
        // 准备参数
        Collection<Long> deptIds = singleton(1L);

        // 调用
        List<AdminUserDO> list = userService.getUserListByDeptIds(deptIds);
        // 断言
        assertEquals(1, list.size());
        assertEquals(dbUser, list.get(0));
    }

    /**
     * 情况一，校验不通过，导致插入失败
     */
    @Test
    public void testImportUserList_01() {
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock 方法，模拟失败
        doThrow(new ServiceException(DEPT_NOT_FOUND)).when(deptService).validateDeptList(any());

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(1, respVO.getFailureUsernames().size());
        assertEquals(DEPT_NOT_FOUND.getMsg(), respVO.getFailureUsernames().get(importUser.getUsername()));
    }

    /**
     * 情况二，不存在，进行插入
     */
    @Test
    public void testImportUserList_02() {
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock passwordEncoder 的方法
        when(passwordEncoder.encode(eq(TEST_INIT_PASSWORD))).thenReturn("java");

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(1, respVO.getCreateUsernames().size());
        AdminUserDO user = userMapper.selectByUsername(respVO.getCreateUsernames().get(0));
        assertPojoEquals(importUser, user);
        assertEquals("java", user.getPassword());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(0, respVO.getFailureUsernames().size());
    }

    /**
     * 情况三，存在，但是不强制更新
     */
    @Test
    public void testImportUserList_03() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setUsername(dbUser.getUsername());
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), false);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(1, respVO.getFailureUsernames().size());
        assertEquals(USER_USERNAME_EXISTS.getMsg(), respVO.getFailureUsernames().get(importUser.getUsername()));
    }

    /**
     * 情况四，存在，强制更新
     */
    @Test
    public void testImportUserList_04() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setUsername(dbUser.getUsername());
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(1, respVO.getUpdateUsernames().size());
        AdminUserDO user = userMapper.selectByUsername(respVO.getUpdateUsernames().get(0));
        assertPojoEquals(importUser, user);
        assertEquals(0, respVO.getFailureUsernames().size());
    }

    @Test
    public void testImportDingTalkUserList_success() {
        when(passwordEncoder.encode(eq(TEST_INIT_PASSWORD))).thenReturn("java");
        List<UserDingTalkImportExcelVO> importUsers = List.of(
                buildDingTalkUser("张三", "zhangsan@example.test", "E001", "张三",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null),
                buildDingTalkUser("李四", "lisi@example.test", "E002", "张三",
                        "上海瑛泰医疗器械股份有限公司", "dept-doc-control", "质量管理部", "文控中心", null, null, null, null)
        );

        UserDingTalkImportRespVO respVO = userService.importDingTalkUserList(importUsers);

        assertEquals(List.of("zhangsan", "lisi"), respVO.getCreateUsernames());
        assertTrue(respVO.getFailureUsernames().isEmpty());
        assertTrue(respVO.getCreatedDeptPaths().contains("上海瑛泰医疗器械股份有限公司"));
        assertTrue(respVO.getCreatedDeptPaths().contains("上海瑛泰医疗器械股份有限公司 / 质量管理部"));
        assertTrue(respVO.getCreatedDeptPaths().contains("上海瑛泰医疗器械股份有限公司 / 质量管理部 / 文控中心"));

        AdminUserDO zhangsan = userMapper.selectByUsername("zhangsan");
        AdminUserDO lisi = userMapper.selectByUsername("lisi");
        assertNotNull(zhangsan);
        assertNotNull(lisi);
        assertEquals("java", zhangsan.getPassword());

        DeptDO company = deptMapper.selectByParentIdAndName(DeptDO.PARENT_ID_ROOT, "上海瑛泰医疗器械股份有限公司");
        assertNotNull(company);
        DeptDO quality = deptMapper.selectByParentIdAndName(company.getId(), "质量管理部");
        assertNotNull(quality);
        DeptDO docCenter = deptMapper.selectByParentIdAndName(quality.getId(), "文控中心");
        assertNotNull(docCenter);
        assertEquals(quality.getId(), zhangsan.getDeptId());
        assertEquals(docCenter.getId(), lisi.getDeptId());
        assertEquals(zhangsan.getId(), quality.getLeaderUserId());
        assertEquals(zhangsan.getId(), docCenter.getLeaderUserId());
        assertEquals("zhangsan", respVO.getLeaderAssignedDeptPaths().get("上海瑛泰医疗器械股份有限公司 / 质量管理部"));
        assertEquals("zhangsan", respVO.getLeaderAssignedDeptPaths().get("上海瑛泰医疗器械股份有限公司 / 质量管理部 / 文控中心"));
    }

    @Test
    public void testImportDingTalkUserList_duplicatePinyinAndExistingUsername() {
        when(passwordEncoder.encode(eq(TEST_INIT_PASSWORD))).thenReturn("java");
        userMapper.insert(randomAdminUserDO(o -> o.setUsername("liming")));

        List<UserDingTalkImportExcelVO> importUsers = List.of(
                buildDingTalkUser("李明", "liming1@example.test", "E001", "李明",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null),
                buildDingTalkUser("李明", "liming2@example.test", "E002", "李明",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null),
                buildDingTalkUser("李明", "liming3@example.test", "E003", "李明",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null)
        );

        UserDingTalkImportRespVO respVO = userService.importDingTalkUserList(importUsers);

        assertTrue(respVO.getCreateUsernames().contains("liming2"));
        assertTrue(respVO.getCreateUsernames().contains("liming3"));
        assertEquals(USER_USERNAME_EXISTS.getMsg(), respVO.getFailureUsernames().get("liming"));
        assertNotNull(userMapper.selectByUsername("liming2"));
        assertNotNull(userMapper.selectByUsername("liming3"));
    }

    @Test
    public void testImportDingTalkUserList_enableDisabledDept() {
        when(passwordEncoder.encode(eq(TEST_INIT_PASSWORD))).thenReturn("java");
        DeptDO company = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setName("上海瑛泰医疗器械股份有限公司");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(company);
        DeptDO disabledDept = randomPojo(DeptDO.class, o -> {
            o.setParentId(company.getId());
            o.setName("质量管理部");
            o.setStatus(CommonStatusEnum.DISABLE.getStatus());
        });
        deptMapper.insert(disabledDept);

        UserDingTalkImportRespVO respVO = userService.importDingTalkUserList(List.of(
                buildDingTalkUser("张三", "zhangsan@example.test", "E001", "张三",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null)
        ));

        DeptDO actualDept = deptMapper.selectById(disabledDept.getId());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), actualDept.getStatus());
        assertTrue(respVO.getEnabledDeptPaths().contains("上海瑛泰医疗器械股份有限公司 / 质量管理部"));
    }

    @Test
    public void testImportDingTalkUserList_skipLeaderWhenManagerConflicts() {
        when(passwordEncoder.encode(eq(TEST_INIT_PASSWORD))).thenReturn("java");
        List<UserDingTalkImportExcelVO> importUsers = List.of(
                buildDingTalkUser("张三", "zhangsan@example.test", "E001", "张三",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null),
                buildDingTalkUser("李四", "lisi@example.test", "E002", "李四",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, null, null, null, null)
        );

        UserDingTalkImportRespVO respVO = userService.importDingTalkUserList(importUsers);

        DeptDO company = deptMapper.selectByParentIdAndName(DeptDO.PARENT_ID_ROOT, "上海瑛泰医疗器械股份有限公司");
        DeptDO quality = deptMapper.selectByParentIdAndName(company.getId(), "质量管理部");
        assertNull(quality.getLeaderUserId());
        assertTrue(respVO.getLeaderSkippedDeptPaths().containsKey("上海瑛泰医疗器械股份有限公司 / 质量管理部"));
    }

    @Test
    public void testDingTalkExcelParser_missingHeaders() {
        UserDingTalkImportExcelParser parser = new UserDingTalkImportExcelParser();

        assertServiceException(() -> parser.parse(new ByteArrayInputStream(createDingTalkWorkbookBytes(
                List.of("姓名", "邮箱"),
                List.of(List.of("张三", "zhangsan@example.test"))
        ))), USER_DING_TALK_IMPORT_HEADERS_MISSING);
    }

    @Test
    public void testDingTalkExcelParser_levelGap() {
        UserDingTalkImportExcelParser parser = new UserDingTalkImportExcelParser();

        assertServiceException(() -> parser.parse(new ByteArrayInputStream(createDingTalkWorkbookBytes(
                buildStandardDingTalkHeaders(),
                List.of(Arrays.asList("seed_emp_1", "张三", "zhangsan@example.test", "E001", "张三",
                        "上海瑛泰医疗器械股份有限公司", "dept-quality", "质量管理部", null, "文控中心", null, null, null))
        ))), USER_DING_TALK_IMPORT_LEVEL_GAP);
    }

    @Test
    public void testValidateUserExists_notExists() {
        assertServiceException(() -> userService.validateUserExists(randomLongId()), USER_NOT_EXISTS);
    }

    @Test
    public void testValidateUsernameUnique_usernameExistsForCreate() {
        // 准备参数
        String username = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setUsername(username)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateUsernameUnique(null, username),
                USER_USERNAME_EXISTS);
    }

    @Test
    public void testValidateUsernameUnique_usernameExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String username = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setUsername(username)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateUsernameUnique(id, username),
                USER_USERNAME_EXISTS);
    }

    @Test
    public void testValidateEmailUnique_emailExistsForCreate() {
        // 准备参数
        String email = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setEmail(email)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateEmailUnique(null, email),
                USER_EMAIL_EXISTS);
    }

    @Test
    public void testValidateEmailUnique_emailExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String email = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setEmail(email)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateEmailUnique(id, email),
                USER_EMAIL_EXISTS);
    }

    @Test
    public void testValidateMobileUnique_mobileExistsForCreate() {
        // 准备参数
        String mobile = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setMobile(mobile)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateMobileUnique(null, mobile),
                USER_MOBILE_EXISTS);
    }

    @Test
    public void testValidateMobileUnique_mobileExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String mobile = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setMobile(mobile)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateMobileUnique(id, mobile),
                USER_MOBILE_EXISTS);
    }

    @Test
    public void testValidateOldPassword_notExists() {
        assertServiceException(() -> userService.validateOldPassword(randomLongId(), randomString()),
                USER_NOT_EXISTS);
    }

    @Test
    public void testValidateOldPassword_passwordFailed() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 准备参数
        Long id = user.getId();
        String oldPassword = user.getPassword();

        // 调用，校验异常
        assertServiceException(() -> userService.validateOldPassword(id, oldPassword),
                USER_PASSWORD_FAILED);
        // 校验调用
        verify(passwordEncoder, times(1)).matches(eq(oldPassword), eq(user.getPassword()));
    }

    @Test
    public void testUserListByPostIds() {
        // 准备参数
        Collection<Long> postIds = asSet(10L, 20L);
        // mock user1 数据
        AdminUserDO user1 = randomAdminUserDO(o -> o.setPostIds(asSet(10L, 30L)));
        userMapper.insert(user1);
        userPostMapper.insert(new UserPostDO().setUserId(user1.getId()).setPostId(10L));
        userPostMapper.insert(new UserPostDO().setUserId(user1.getId()).setPostId(30L));
        // mock user2 数据
        AdminUserDO user2 = randomAdminUserDO(o -> o.setPostIds(singleton(100L)));
        userMapper.insert(user2);
        userPostMapper.insert(new UserPostDO().setUserId(user2.getId()).setPostId(100L));

        // 调用
        List<AdminUserDO> result = userService.getUserListByPostIds(postIds);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user1, result.get(0));
    }

    @Test
    public void testGetUserList() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 测试 id 不匹配
        userMapper.insert(randomAdminUserDO());
        // 准备参数
        Collection<Long> ids = singleton(user.getId());

        // 调用
        List<AdminUserDO> result = userService.getUserList(ids);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserMap() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 测试 id 不匹配
        userMapper.insert(randomAdminUserDO());
        // 准备参数
        Collection<Long> ids = singleton(user.getId());

        // 调用
        Map<Long, AdminUserDO> result = userService.getUserMap(ids);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(user.getId()));
    }

    @Test
    public void testGetUserListByNickname() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setNickname("芋头"));
        userMapper.insert(user);
        // 测试 nickname 不匹配
        userMapper.insert(randomAdminUserDO(o -> o.setNickname("源码")));
        // 准备参数
        String nickname = "芋";

        // 调用
        List<AdminUserDO> result = userService.getUserListByNickname(nickname);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserListByStatus() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        userMapper.insert(user);
        // 测试 status 不匹配
        userMapper.insert(randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus())));
        // 准备参数
        Integer status = CommonStatusEnum.DISABLE.getStatus();

        // 调用
        List<AdminUserDO> result = userService.getUserListByStatus(status);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testValidateUserList_success() {
        // mock 数据
        AdminUserDO userDO = randomAdminUserDO().setStatus(CommonStatusEnum.ENABLE.getStatus());
        userMapper.insert(userDO);
        // 准备参数
        List<Long> ids = singletonList(userDO.getId());

        // 调用，无需断言
        userService.validateUserList(ids);
    }

    @Test
    public void testValidateUserList_notFound() {
        // 准备参数
        List<Long> ids = singletonList(randomLongId());

        // 调用, 并断言异常
        assertServiceException(() -> userService.validateUserList(ids), USER_NOT_EXISTS);
    }

    @Test
    public void testValidateUserList_notEnable() {
        // mock 数据
        AdminUserDO userDO = randomAdminUserDO().setStatus(CommonStatusEnum.DISABLE.getStatus());
        userMapper.insert(userDO);
        // 准备参数
        List<Long> ids = singletonList(userDO.getId());

        // 调用, 并断言异常
        assertServiceException(() -> userService.validateUserList(ids), USER_IS_DISABLE,
                userDO.getNickname());
    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static AdminUserDO randomAdminUserDO(Consumer<AdminUserDO>... consumers) {
        Consumer<AdminUserDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setLoginFailureCount(0);
            o.setLoginLocked(0);
            o.setLoginLockedTime(null);
        };
        return randomPojo(AdminUserDO.class, ArrayUtils.append(consumer, consumers));
    }

    private static UserDingTalkImportExcelVO buildDingTalkUser(String name, String email, String employeeNo,
                                                               String departmentManagerName, String companyName,
                                                               String sourceDepartmentId, String level2, String level3,
                                                               String level4, String level5, String level6,
                                                               String level7) {
        return UserDingTalkImportExcelVO.builder()
                .employeeUserId("seed_" + employeeNo)
                .name(name)
                .email(email)
                .employeeNo(employeeNo)
                .departmentManagerName(departmentManagerName)
                .companyName(companyName)
                .sourceDepartmentId(sourceDepartmentId)
                .level2DepartmentName(level2)
                .level3DepartmentName(level3)
                .level4DepartmentName(level4)
                .level5DepartmentName(level5)
                .level6DepartmentName(level6)
                .level7DepartmentName(level7)
                .build();
    }

    private static List<String> buildStandardDingTalkHeaders() {
        return List.of("员工UserID", "姓名", "邮箱", "工号", "部门主管", "1级部门", "主部门ID",
                "2级部门", "3级部门", "4级部门", "5级部门", "6级部门", "7级部门");
    }

    private static byte[] createDingTalkWorkbookBytes(List<String> headers, List<List<Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("org_seed");
            var headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.size(); col++) {
                headerRow.createCell(col).setCellValue(headers.get(col));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                var row = sheet.createRow(rowIndex + 1);
                List<Object> values = rows.get(rowIndex);
                for (int col = 0; col < values.size(); col++) {
                    Object value = values.get(col);
                    if (value != null) {
                        row.createCell(col).setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

}
