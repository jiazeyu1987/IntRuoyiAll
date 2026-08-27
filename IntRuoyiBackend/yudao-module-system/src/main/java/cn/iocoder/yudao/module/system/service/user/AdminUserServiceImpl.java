package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.github.promeg.pinyinhelper.Pinyin;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDingTalkImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserDingTalkImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dept.PostService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import com.google.common.annotations.VisibleForTesting;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.system.enums.LogRecordConstants.*;

/**
 * 后台用户 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service("adminUserService")
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    static final String USER_INIT_PASSWORD_KEY = "system.user.init-password";

    static final String USER_REGISTER_ENABLED_KEY = "system.user.register-enabled";

    static final int USER_LOGIN_FAILURE_LOCK_THRESHOLD = 5;

    @Resource
    private AdminUserMapper userMapper;

    @Resource
    private DeptService deptService;
    @Resource
    private PostService postService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private TenantService tenantService;
    @Resource
    @Lazy // 懒加载，避免循环依赖
    private OAuth2TokenService oauth2TokenService;

    @Resource
    private UserPostMapper userPostMapper;

    @Resource
    private ConfigApi configApi;
    @Resource
    private DeptMapper deptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_CREATE_SUB_TYPE, bizNo = "{{#user.id}}",
            success = SYSTEM_USER_CREATE_SUCCESS)
    public Long createUser(UserSaveReqVO createReqVO) {
        // 1.1 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // 1.2 校验正确性
        validateUserForCreateOrUpdate(null, createReqVO.getUsername(),
                createReqVO.getMobile(), createReqVO.getEmail(), createReqVO.getDeptId(), createReqVO.getPostIds());
        validatePasswordStrength(createReqVO.getPassword());
        // 2.1 插入用户
        AdminUserDO user = BeanUtils.toBean(createReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(createReqVO.getPassword())); // 加密密码
        user.setPasswordUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        // 2.2 插入关联岗位
        if (CollectionUtil.isNotEmpty(user.getPostIds())) {
            userPostMapper.insertBatch(convertList(user.getPostIds(),
                    postId -> new UserPostDO().setUserId(user.getId()).setPostId(postId)));
        }

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        return user.getId();
    }

    @Override
    public Long registerUser(AuthRegisterReqVO registerReqVO) {
        // 1.1 校验是否开启注册
        if (ObjUtil.notEqual(configApi.getConfigValueByKey(USER_REGISTER_ENABLED_KEY), "true")) {
            throw exception(USER_REGISTER_DISABLED);
        }
        // 1.2 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // 1.3 校验正确性
        validateUserForCreateOrUpdate(null, registerReqVO.getUsername(), null, null, null, null);
        validatePasswordStrength(registerReqVO.getPassword());

        // 2. 插入用户
        AdminUserDO user = BeanUtils.toBean(registerReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(registerReqVO.getPassword())); // 加密密码
        user.setPasswordUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = SYSTEM_USER_UPDATE_SUCCESS)
    public void updateUser(UserSaveReqVO updateReqVO) {
        updateReqVO.setPassword(null); // 特殊：此处不更新密码
        // 1. 校验正确性
        AdminUserDO oldUser = validateUserForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getUsername(),
                updateReqVO.getMobile(), updateReqVO.getEmail(), updateReqVO.getDeptId(), updateReqVO.getPostIds());

        // 2.1 更新用户
        AdminUserDO updateObj = BeanUtils.toBean(updateReqVO, AdminUserDO.class);
        userMapper.updateById(updateObj);
        // 2.2 更新岗位
        updateUserPost(updateReqVO, updateObj);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldUser, UserSaveReqVO.class));
        LogRecordContext.putVariable("user", oldUser);
    }

    private void updateUserPost(UserSaveReqVO reqVO, AdminUserDO updateObj) {
        Long userId = reqVO.getId();
        Set<Long> dbPostIds = convertSet(userPostMapper.selectListByUserId(userId), UserPostDO::getPostId);
        // 计算新增和删除的岗位编号
        Set<Long> postIds = CollUtil.emptyIfNull(updateObj.getPostIds());
        Collection<Long> createPostIds = CollUtil.subtract(postIds, dbPostIds);
        Collection<Long> deletePostIds = CollUtil.subtract(dbPostIds, postIds);
        // 执行新增和删除。对于已经授权的岗位，不用做任何处理
        if (!CollectionUtil.isEmpty(createPostIds)) {
            userPostMapper.insertBatch(convertList(createPostIds,
                    postId -> new UserPostDO().setUserId(userId).setPostId(postId)));
        }
        if (!CollectionUtil.isEmpty(deletePostIds)) {
            userPostMapper.deleteByUserIdAndPostId(userId, deletePostIds);
        }
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        userMapper.updateById(new AdminUserDO().setId(id).setLoginIp(loginIp).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public void recordUserLoginFailure(Long id) {
        AdminUserDO user = validateUserExists(id);
        int failureCount = Optional.ofNullable(user.getLoginFailureCount()).orElse(0) + 1;
        AdminUserDO updateObj = new AdminUserDO().setId(id).setLoginFailureCount(failureCount);
        if (failureCount >= USER_LOGIN_FAILURE_LOCK_THRESHOLD) {
            updateObj.setLoginLocked(1);
            updateObj.setLoginLockedTime(LocalDateTime.now());
        }
        userMapper.updateById(updateObj);
    }

    @Override
    public void resetUserLoginFailure(Long id) {
        validateUserExists(id);
        userMapper.update(null, Wrappers.lambdaUpdate(AdminUserDO.class)
                .eq(AdminUserDO::getId, id)
                .set(AdminUserDO::getLoginFailureCount, 0)
                .set(AdminUserDO::getLoginLocked, 0)
                .set(AdminUserDO::getLoginLockedTime, null));
    }

    @Override
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        // 校验正确性
        validateUserExists(id);
        validateEmailUnique(id, reqVO.getEmail());
        validateMobileUnique(id, reqVO.getMobile());
        // 执行更新
        userMapper.updateById(BeanUtils.toBean(reqVO, AdminUserDO.class).setId(id));
    }

    @Override
    public void updateUserPassword(Long id, UserProfileUpdatePasswordReqVO reqVO) {
        // 校验旧密码密码
        validateOldPassword(id, reqVO.getOldPassword());
        validatePasswordStrength(reqVO.getNewPassword());
        // 执行更新
        AdminUserDO updateObj = new AdminUserDO().setId(id);
        updateObj.setPassword(encodePassword(reqVO.getNewPassword())); // 加密密码
        updateObj.setPasswordUpdateTime(LocalDateTime.now());
        userMapper.updateById(updateObj);
    }

    @Override
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_UPDATE_PASSWORD_SUCCESS)
    public void updateUserPassword(Long id, String password) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);
        validatePasswordStrength(password);

        // 2. 更新密码
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setPassword(encodePassword(password)); // 加密密码
        updateObj.setPasswordUpdateTime(LocalDateTime.now());
        userMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        LogRecordContext.putVariable("newPassword", updateObj.getPassword());
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        // 校验用户存在
        validateUserExists(id);
        // 更新状态
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        userMapper.updateById(updateObj);

        // 如果是禁用用户，则删除其 Token 信息
        if (CommonStatusEnum.isDisable(status)) {
            oauth2TokenService.removeAccessToken(id, UserTypeEnum.ADMIN.getValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_DELETE_SUCCESS)
    public void deleteUser(Long id) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);

        // 2.1 删除用户
        userMapper.deleteById(id);
        // 2.2 删除用户关联数据
        permissionService.processUserDeleted(id);
        // 2.2 删除用户岗位
        userPostMapper.deleteByUserId(id);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw exception(USER_DELETE_LIST_IS_EMPTY);
        }
        // 1. 批量删除用户
        userMapper.deleteByIds(ids);

        // 2. 批量删除用户关联数据
        ids.forEach(id -> {
            permissionService.processUserDeleted(id);
            userPostMapper.deleteByUserId(id);
        });
    }

    @Override
    public AdminUserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public AdminUserDO getUserByMobile(String mobile) {
        return userMapper.selectByMobile(mobile);
    }

    @Override
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        // 如果有角色编号，查询角色对应的用户编号
        Set<Long> userIds = null;
        if (reqVO.getRoleId() != null) {
            userIds = permissionService.getUserRoleIdListByRoleId(singleton(reqVO.getRoleId()));
            if (CollUtil.isEmpty(userIds)) {
                return PageResult.empty();
            }
        }

        Set<Long> deptIds = getDeptCondition(reqVO.getDeptId());
        if (reqVO.getDeptId() == null) {
            return userMapper.selectPage(reqVO, deptIds, userIds);
        }

        List<AdminUserDO> matchedUsers = userMapper.selectListForPage(reqVO, deptIds, userIds);
        if (CollUtil.isEmpty(matchedUsers)) {
            return PageResult.empty();
        }

        Map<Long, DeptDO> deptMap = CollectionUtils.convertMap(deptService.getDeptList(deptIds), DeptDO::getId);
        List<AdminUserDO> sortedUsers = sortUsersByDeptLeader(matchedUsers, deptMap, reqVO.getDeptId());
        return buildPagedResult(reqVO, sortedUsers);
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<AdminUserDO> getUserListByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectListByDeptIds(deptIds);
    }

    @Override
    public List<AdminUserDO> getUserListByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(userPostMapper.selectListByPostIds(postIds), UserPostDO::getUserId);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(userIds);
    }

    @Override
    public List<AdminUserDO> getUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(ids);
    }

    @Override
    public void validateUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得岗位信息
        List<AdminUserDO> users = userMapper.selectByIds(ids);
        Map<Long, AdminUserDO> userMap = CollectionUtils.convertMap(users, AdminUserDO::getId);
        // 校验
        ids.forEach(id -> {
            AdminUserDO user = userMap.get(id);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                throw exception(USER_IS_DISABLE, user.getNickname());
            }
        });
    }

    @Override
    public List<AdminUserDO> getUserListByNickname(String nickname) {
        return userMapper.selectListByNickname(nickname);
    }

    /**
     * 获得部门条件：查询指定部门的子部门编号们，包括自身
     *
     * @param deptId 部门编号
     * @return 部门编号集合
     */
    private Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = convertSet(deptService.getChildDeptList(deptId), DeptDO::getId);
        deptIds.add(deptId); // 包括自身
        return deptIds;
    }

    private List<AdminUserDO> sortUsersByDeptLeader(List<AdminUserDO> users, Map<Long, DeptDO> deptMap,
                                                    Long priorityDeptId) {
        Map<Long, List<AdminUserDO>> deptUsers = new LinkedHashMap<>();
        List<AdminUserDO> detachedUsers = new ArrayList<>();
        for (AdminUserDO user : users) {
            if (user.getDeptId() == null) {
                detachedUsers.add(user);
                continue;
            }
            deptUsers.computeIfAbsent(user.getDeptId(), key -> new ArrayList<>()).add(user);
        }

        List<AdminUserDO> sortedUsers = new ArrayList<>(users.size());
        if (priorityDeptId != null && deptUsers.containsKey(priorityDeptId)) {
            DeptDO priorityDept = deptMap.get(priorityDeptId);
            Long priorityLeaderUserId = priorityDept != null ? priorityDept.getLeaderUserId() : null;
            appendDeptUsersWithLeaderFirst(sortedUsers, deptUsers.get(priorityDeptId), priorityLeaderUserId);
        }
        for (Map.Entry<Long, List<AdminUserDO>> entry : deptUsers.entrySet()) {
            if (priorityDeptId != null && priorityDeptId.equals(entry.getKey())) {
                continue;
            }
            DeptDO dept = deptMap.get(entry.getKey());
            Long leaderUserId = dept != null ? dept.getLeaderUserId() : null;
            appendDeptUsersWithLeaderFirst(sortedUsers, entry.getValue(), leaderUserId);
        }
        sortedUsers.addAll(detachedUsers);
        return sortedUsers;
    }

    private void appendDeptUsersWithLeaderFirst(List<AdminUserDO> sortedUsers, List<AdminUserDO> deptUsers,
                                                Long leaderUserId) {
        if (leaderUserId == null) {
            sortedUsers.addAll(deptUsers);
            return;
        }
        for (AdminUserDO user : deptUsers) {
            if (leaderUserId.equals(user.getId())) {
                sortedUsers.add(user);
            }
        }
        for (AdminUserDO user : deptUsers) {
            if (!leaderUserId.equals(user.getId())) {
                sortedUsers.add(user);
            }
        }
    }

    private PageResult<AdminUserDO> buildPagedResult(UserPageReqVO reqVO, List<AdminUserDO> users) {
        int fromIndex = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), users.size());
        int toIndex = Math.min(fromIndex + reqVO.getPageSize(), users.size());
        return new PageResult<>(new ArrayList<>(users.subList(fromIndex, toIndex)), (long) users.size());
    }

    private AdminUserDO validateUserForCreateOrUpdate(Long id, String username, String mobile, String email,
                                               Long deptId, Set<Long> postIds) {
        // 关闭数据权限，避免因为没有数据权限，查询不到数据，进而导致唯一校验不正确
        return DataPermissionUtils.executeIgnore(() -> {
            // 校验用户存在
            AdminUserDO user = validateUserExists(id);
            // 校验用户名唯一
            validateUsernameUnique(id, username);
            // 校验手机号唯一
            validateMobileUnique(id, mobile);
            // 校验邮箱唯一
            validateEmailUnique(id, email);
            // 校验部门处于开启状态
            deptService.validateDeptList(CollectionUtils.singleton(deptId));
            // 校验岗位处于开启状态
            postService.validatePostList(postIds);
            return user;
        });
    }

    @VisibleForTesting
    AdminUserDO validateUserExists(Long id) {
        if (id == null) {
            return null;
        }
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    @VisibleForTesting
    void validateUsernameUnique(Long id, String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_USERNAME_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
    }

    @VisibleForTesting
    void validateEmailUnique(Long id, String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        AdminUserDO user = userMapper.selectByEmail(email);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_EMAIL_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_EMAIL_EXISTS);
        }
    }

    @VisibleForTesting
    void validateMobileUnique(Long id, String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return;
        }
        AdminUserDO user = userMapper.selectByMobile(mobile);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_MOBILE_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_MOBILE_EXISTS);
        }
    }

    /**
     * 校验旧密码
     * @param id          用户 id
     * @param oldPassword 旧密码
     */
    @Override
    @VisibleForTesting
    public void validateOldPassword(Long id, String oldPassword) {
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!isPasswordMatch(oldPassword, user.getPassword())) {
            throw exception(USER_PASSWORD_FAILED);
        }
    }

    @VisibleForTesting
    void validatePasswordStrength(String password) {
        if (!AdminUserPasswordPolicy.isStrong(password)) {
            throw exception(USER_PASSWORD_STRENGTH_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常则回滚所有导入
    public UserImportRespVO importUserList(List<UserImportExcelVO> importUsers, boolean isUpdateSupport) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        // 1.2 初始化密码不能为空
        String initPassword = configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY);
        if (StrUtil.isEmpty(initPassword)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }
        validatePasswordStrength(initPassword);

        // 2. 遍历，逐个创建 or 更新
        UserImportRespVO respVO = UserImportRespVO.builder().createUsernames(new ArrayList<>())
                .updateUsernames(new ArrayList<>()).failureUsernames(new LinkedHashMap<>()).build();
        AtomicInteger index = new AtomicInteger(1);
        importUsers.forEach(importUser -> {
            int currentIndex = index.getAndIncrement();
            // 2.1.1 校验字段是否符合要求
            try {
                ValidationUtils.validate(BeanUtils.toBean(importUser, UserSaveReqVO.class).setPassword(initPassword));
            } catch (ConstraintViolationException ex) {
                String key = StrUtil.blankToDefault(importUser.getUsername(), "第 " + currentIndex + " 行");
                respVO.getFailureUsernames().put(key, ex.getMessage());
                return;
            }
            // 2.1.2 校验，判断是否有不符合的原因
            try {
                validateUserForCreateOrUpdate(null, null, importUser.getMobile(), importUser.getEmail(),
                        importUser.getDeptId(), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(importUser.getUsername(), ex.getMessage());
                return;
            }

            // 2.2.1 判断如果不存在，在进行插入
            AdminUserDO existUser = userMapper.selectByUsername(importUser.getUsername());
            if (existUser == null) {
                userMapper.insert(BeanUtils.toBean(importUser, AdminUserDO.class)
                        .setPassword(encodePassword(initPassword))
                        .setPasswordUpdateTime(LocalDateTime.now())
                        .setPostIds(new HashSet<>())); // 设置默认密码及空岗位编号数组
                respVO.getCreateUsernames().add(importUser.getUsername());
                return;
            }
            // 2.2.2 如果存在，判断是否允许更新
            if (!isUpdateSupport) {
                respVO.getFailureUsernames().put(importUser.getUsername(), USER_USERNAME_EXISTS.getMsg());
                return;
            }
            AdminUserDO updateUser = BeanUtils.toBean(importUser, AdminUserDO.class);
            updateUser.setId(existUser.getId());
            userMapper.updateById(updateUser);
            respVO.getUpdateUsernames().add(importUser.getUsername());
        });
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDingTalkImportRespVO importDingTalkUserList(List<UserDingTalkImportExcelVO> importUsers) {
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_DING_TALK_IMPORT_LIST_IS_EMPTY);
        }
        String initPassword = configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY);
        if (StrUtil.isBlank(initPassword)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }
        validatePasswordStrength(initPassword);

        UserDingTalkImportRespVO respVO = UserDingTalkImportRespVO.builder()
                .createUsernames(new ArrayList<>())
                .failureUsernames(new LinkedHashMap<>())
                .createdDeptPaths(new ArrayList<>())
                .enabledDeptPaths(new ArrayList<>())
                .leaderAssignedDeptPaths(new LinkedHashMap<>())
                .leaderSkippedDeptPaths(new LinkedHashMap<>())
                .build();
        Map<String, DeptDO> deptPathMap = new LinkedHashMap<>();
        Map<String, String> pathSourceDeptIdMap = new LinkedHashMap<>();
        Map<String, String> sourceDeptIdPathMap = new LinkedHashMap<>();
        Map<String, Set<String>> leaderNamesByDeptPath = new LinkedHashMap<>();
        Map<String, List<AdminUserDO>> createdUsersByName = new LinkedHashMap<>();
        Map<String, Integer> usernameCounters = new LinkedHashMap<>();
        AtomicInteger rowIndex = new AtomicInteger(1);

        for (UserDingTalkImportExcelVO importUser : importUsers) {
            int currentRowIndex = rowIndex.getAndIncrement();
            List<String> pathParts = new ArrayList<>();
            pathParts.add(StrUtil.trim(importUser.getCompanyName()));
            List<String> departments = Arrays.asList(importUser.getLevel2DepartmentName(), importUser.getLevel3DepartmentName(),
                    importUser.getLevel4DepartmentName(), importUser.getLevel5DepartmentName(),
                    importUser.getLevel6DepartmentName(), importUser.getLevel7DepartmentName());

            Long parentId = DeptDO.PARENT_ID_ROOT;
            DeptDO companyDept = getOrCreateDept(parentId, pathParts.get(0), String.join(" / ", pathParts), respVO);
            deptPathMap.put(String.join(" / ", pathParts), companyDept);
            parentId = companyDept.getId();
            DeptDO targetDept = companyDept;

            for (String departmentName : departments) {
                if (StrUtil.isBlank(departmentName)) {
                    break;
                }
                pathParts.add(StrUtil.trim(departmentName));
                String path = String.join(" / ", pathParts);
                targetDept = getOrCreateDept(parentId, StrUtil.trim(departmentName), path, respVO);
                deptPathMap.put(path, targetDept);
                parentId = targetDept.getId();
            }
            String targetDeptPath = String.join(" / ", pathParts);
            if (pathParts.size() > 1) {
                String sourceDepartmentId = StrUtil.trim(importUser.getSourceDepartmentId());
                String existingSourceDeptId = pathSourceDeptIdMap.get(targetDeptPath);
                if (existingSourceDeptId != null && !StrUtil.equals(existingSourceDeptId, sourceDepartmentId)) {
                    throw exception(USER_DING_TALK_IMPORT_SOURCE_DEPT_ID_CONFLICT);
                }
                String existingDeptPath = sourceDeptIdPathMap.get(sourceDepartmentId);
                if (existingDeptPath != null && !StrUtil.equals(existingDeptPath, targetDeptPath)) {
                    throw exception(USER_DING_TALK_IMPORT_SOURCE_DEPT_ID_CONFLICT);
                }
                pathSourceDeptIdMap.put(targetDeptPath, sourceDepartmentId);
                sourceDeptIdPathMap.put(sourceDepartmentId, targetDeptPath);
            }

            String leaderName = StrUtil.trim(importUser.getDepartmentManagerName());
            if (StrUtil.isNotBlank(leaderName)) {
                leaderNamesByDeptPath.computeIfAbsent(targetDeptPath, key -> new LinkedHashSet<>()).add(leaderName);
            }

            String username;
            try {
                username = generateImportUsername(importUser.getName(), usernameCounters);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(resolveFailureKey(null, importUser, currentRowIndex), ex.getMessage());
                continue;
            }
            try {
                validateUserForCreateOrUpdate(null, username, null, importUser.getEmail(), targetDept.getId(), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(username, ex.getMessage());
                continue;
            }

            AdminUserDO user = AdminUserDO.builder()
                    .username(username)
                    .nickname(StrUtil.trim(importUser.getName()))
                    .deptId(targetDept.getId())
                    .postIds(new HashSet<>())
                    .email(StrUtil.emptyToDefault(StrUtil.trim(importUser.getEmail()), ""))
                    .mobile("")
                    .status(CommonStatusEnum.ENABLE.getStatus())
                    .password(encodePassword(initPassword))
                    .passwordUpdateTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            respVO.getCreateUsernames().add(username);
            createdUsersByName.computeIfAbsent(StrUtil.trim(importUser.getName()), key -> new ArrayList<>()).add(user);
        }

        for (Map.Entry<String, Set<String>> entry : leaderNamesByDeptPath.entrySet()) {
            String deptPath = entry.getKey();
            Set<String> leaderNames = entry.getValue();
            if (leaderNames.size() > 1) {
                respVO.getLeaderSkippedDeptPaths().put(deptPath, "部门主管姓名不一致");
                continue;
            }
            String leaderName = leaderNames.iterator().next();
            List<AdminUserDO> matchedUsers = createdUsersByName.getOrDefault(leaderName, Collections.emptyList());
            if (matchedUsers.isEmpty()) {
                respVO.getLeaderSkippedDeptPaths().put(deptPath, "部门主管未成功导入");
                continue;
            }
            if (matchedUsers.size() > 1) {
                respVO.getLeaderSkippedDeptPaths().put(deptPath, "部门主管匹配到多个导入用户");
                continue;
            }
            AdminUserDO leaderUser = matchedUsers.get(0);
            DeptDO dept = deptPathMap.get(deptPath);
            DeptDO updateObj = new DeptDO();
            updateObj.setId(dept.getId());
            updateObj.setLeaderUserId(leaderUser.getId());
            deptMapper.updateById(updateObj);
            respVO.getLeaderAssignedDeptPaths().put(deptPath, leaderUser.getUsername());
        }
        return respVO;
    }

    @Override
    public List<AdminUserDO> getUserListByStatus(Integer status) {
        return userMapper.selectListByStatus(status);
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 对密码进行加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private DeptDO getOrCreateDept(Long parentId, String deptName, String pathName, UserDingTalkImportRespVO respVO) {
        DeptDO existingDept = deptMapper.selectByParentIdAndName(parentId, deptName);
        if (existingDept != null) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(existingDept.getStatus())) {
                DeptDO updateObj = new DeptDO();
                updateObj.setId(existingDept.getId());
                updateObj.setStatus(CommonStatusEnum.ENABLE.getStatus());
                deptMapper.updateById(updateObj);
                respVO.getEnabledDeptPaths().add(pathName);
                existingDept.setStatus(CommonStatusEnum.ENABLE.getStatus());
            }
            return existingDept;
        }
        DeptDO dept = new DeptDO();
        dept.setParentId(parentId);
        dept.setName(deptName);
        dept.setSort(Math.toIntExact(deptMapper.selectCountByParentId(parentId)));
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        deptMapper.insert(dept);
        respVO.getCreatedDeptPaths().add(pathName);
        return dept;
    }

    private String resolveFailureKey(String username, UserDingTalkImportExcelVO importUser, int rowIndex) {
        if (StrUtil.isNotBlank(username)) {
            return username;
        }
        if (StrUtil.isNotBlank(importUser.getEmployeeNo())) {
            return importUser.getEmployeeNo();
        }
        return "第 " + rowIndex + " 行";
    }

    private String generateImportUsername(String rawName, Map<String, Integer> usernameCounters) {
        String normalized = StrUtil.trim(rawName);
        StringBuilder builder = new StringBuilder();
        for (char ch : normalized.toCharArray()) {
            if (Pinyin.isChinese(ch)) {
                builder.append(Pinyin.toPinyin(ch).toLowerCase(Locale.ROOT));
            } else if (Character.isLetterOrDigit(ch)) {
                builder.append(Character.toLowerCase(ch));
            }
        }
        String base = builder.toString();
        if (StrUtil.isBlank(base)) {
            throw exception(USER_DING_TALK_IMPORT_USERNAME_INVALID);
        }
        base = StrUtil.sub(base, 0, 30);
        int index = usernameCounters.getOrDefault(base, 0) + 1;
        usernameCounters.put(base, index);
        if (index == 1) {
            return base;
        }
        String suffix = String.valueOf(index);
        int maxBaseLength = 30 - suffix.length();
        if (maxBaseLength <= 0) {
            throw exception(USER_DING_TALK_IMPORT_USERNAME_INVALID);
        }
        return StrUtil.sub(base, 0, maxBaseLength) + suffix;
    }

}
