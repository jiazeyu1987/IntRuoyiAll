package cn.iocoder.yudao.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 部门 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
@Slf4j
public class DeptServiceImpl implements DeptService {

    @Resource
    private cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi approvalAuthority;

    @Resource
    private DeptMapper deptMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptSupervisorService supervisorService;

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public Long createDept(DeptSaveReqVO createReqVO) {
        var authority = approvalAuthority.lockTenantApprovalAuthority(
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
        // 校验父部门的有效性
        validateParentDept(null, createReqVO.getParentId());
        // 校验部门名的唯一性
        validateDeptNameUnique(null, createReqVO.getParentId(), createReqVO.getName());

        supervisorService.validateSupervisor(createReqVO.getLeaderUserId());
        // 插入部门
        DeptDO dept = BeanUtils.toBean(createReqVO, DeptDO.class);
        if (deptMapper.insert(dept) != 1) throw new IllegalStateException("APPROVAL_DEPT_CREATE_ROW_COUNT");
        approvalAuthority.incrementLockedAuthorityRevision(authority.tenantId(), authority.revision());
        supervisorService.recordChange(new DeptDO().setId(dept.getId()).setName(dept.getName()),
                dept.getLeaderUserId(), "创建部门");
        return dept.getId();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void updateDept(DeptSaveReqVO updateReqVO) {
        var authority = approvalAuthority.lockTenantApprovalAuthority(
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (updateReqVO.getParentId() == null) {
            updateReqVO.setParentId(DeptDO.PARENT_ID_ROOT);
        }
        // 校验自己存在
        validateDeptExists(updateReqVO.getId());
        // 校验父部门的有效性
        validateParentDept(updateReqVO.getId(), updateReqVO.getParentId());
        // 校验部门名的唯一性
        validateDeptNameUnique(updateReqVO.getId(), updateReqVO.getParentId(), updateReqVO.getName());

        DeptDO previous = deptMapper.selectById(updateReqVO.getId());
        if (updateReqVO.isLeaderUserIdPresent()) {
            supervisorService.validateSupervisor(updateReqVO.getLeaderUserId());
        }
        // 常规字段仍走原有更新；主管只由显式字段触发，null 表示解除。
        DeptDO updateObj = BeanUtils.toBean(updateReqVO, DeptDO.class);
        updateObj.setLeaderUserId(null);
        if (deptMapper.updateById(updateObj) != 1) throw new IllegalStateException("APPROVAL_DEPT_UPDATE_ROW_COUNT");
        if (updateReqVO.isLeaderUserIdPresent()) {
            if (deptMapper.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaUpdate(DeptDO.class)
                    .eq(DeptDO::getId, updateReqVO.getId())
                    .set(DeptDO::getLeaderUserId, updateReqVO.getLeaderUserId())) != 1) {
                throw new IllegalStateException("DEPT_SUPERVISOR_UPDATE_ROW_COUNT");
            }
            supervisorService.recordChange(previous, updateReqVO.getLeaderUserId(), "修改部门主管");
        }
        approvalAuthority.incrementLockedAuthorityRevision(authority.tenantId(), authority.revision());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void deleteDept(Long id) {
        var authority = approvalAuthority.lockTenantApprovalAuthority(
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        // 校验是否存在
        validateDeptExists(id);
        // 计算整棵待删除组织树
        LinkedHashSet<Long> deleteDeptIds = new LinkedHashSet<>();
        deleteDeptIds.add(id);
        getChildDeptList(id).forEach(dept -> deleteDeptIds.add(dept.getId()));
        // 树内只要还有员工，就禁止删除
        validateDeptTreeHasNoUsers(deleteDeptIds);
        // 删除整棵组织树
        if (deptMapper.deleteByIds(new ArrayList<>(deleteDeptIds)) != deleteDeptIds.size()) {
            throw new IllegalStateException("APPROVAL_DEPT_DELETE_ROW_COUNT");
        }
        approvalAuthority.incrementLockedAuthorityRevision(authority.tenantId(), authority.revision());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，因为操作一个部门，涉及到多个缓存
    public void deleteDeptList(List<Long> ids) {
        var authority = approvalAuthority.lockTenantApprovalAuthority(
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new java.util.HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("APPROVAL_DEPT_DELETE_IDENTITIES_REQUIRED");
        }
        // 校验是否有子部门
        for (Long id : ids) {
            validateDeptExists(id);
            if (cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils.executeIgnore(
                    () -> deptMapper.selectCountByParentId(id)) > 0) {
                throw exception(DEPT_DELETE_HAS_CHILDREN, deptMapper.selectById(id).getName());
            }
        }
        validateDeptTreeHasNoUsers(ids);

        // 批量删除部门
        if (deptMapper.deleteByIds(ids) != ids.size()) throw new IllegalStateException("APPROVAL_DEPT_DELETE_ROW_COUNT");
        approvalAuthority.incrementLockedAuthorityRevision(authority.tenantId(), authority.revision());
    }

    @VisibleForTesting
    void validateDeptExists(Long id) {
        if (id == null) {
            return;
        }
        DeptDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw exception(DEPT_NOT_FOUND);
        }
    }

    @VisibleForTesting
    void validateParentDept(Long id, Long parentId) {
        if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) {
            return;
        }
        // 1. 不能设置自己为父部门
        if (Objects.equals(id, parentId)) {
            throw exception(DEPT_PARENT_ERROR);
        }
        // 2. 父部门不存在
        DeptDO parentDept = deptMapper.selectById(parentId);
        if (parentDept == null) {
            throw exception(DEPT_PARENT_NOT_EXITS);
        }
        // 3. 递归校验父部门，如果父部门是自己的子部门，则报错，避免形成环路
        if (id == null) { // id 为空，说明新增，不需要考虑环路
            return;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            // 3.1 校验环路
            parentId = parentDept.getParentId();
            if (Objects.equals(id, parentId)) {
                throw exception(DEPT_PARENT_IS_CHILD);
            }
            // 3.2 继续递归下一级父部门
            if (parentId == null || DeptDO.PARENT_ID_ROOT.equals(parentId)) {
                break;
            }
            parentDept = deptMapper.selectById(parentId);
            if (parentDept == null) {
                break;
            }
        }
    }

    @VisibleForTesting
    void validateDeptNameUnique(Long id, Long parentId, String name) {
        DeptDO dept = deptMapper.selectByParentIdAndName(parentId, name);
        if (dept == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的部门
        if (id == null) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
        if (ObjectUtil.notEqual(dept.getId(), id)) {
            throw exception(DEPT_NAME_DUPLICATE);
        }
    }

    @VisibleForTesting
    void validateDeptTreeHasNoUsers(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return;
        }
        Set<Long> occupied = cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils.executeIgnore(
                () -> userMapper.selectListByDeptIds(deptIds).stream()
                        .map(cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO::getDeptId)
                        .collect(java.util.stream.Collectors.toSet()));
        if (!occupied.isEmpty()) {
            String names = cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils.executeIgnore(
                    () -> deptMapper.selectByIds(occupied).stream().map(DeptDO::getName)
                            .sorted().collect(java.util.stream.Collectors.joining("、")));
            throw exception(DEPT_DELETE_HAS_USERS, names);
        }
    }

    @Override
    public DeptDO getDept(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    public List<DeptDO> getDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return deptMapper.selectByIds(ids);
    }

    @Override
    public List<DeptDO> getDeptList(DeptListReqVO reqVO) {
        List<DeptDO> list = deptMapper.selectList(reqVO);
        list.sort(Comparator.comparing(DeptDO::getSort));
        return list;
    }

    @Override
    @DataPermission(enable = false)
    public List<DeptDO> getSimpleDeptList() {
        return getDeptList(new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
    }

    @Override
    public List<DeptDO> getChildDeptList(Collection<Long> ids) {
        List<DeptDO> children = new LinkedList<>();
        Set<Long> visitedParentIds = new LinkedHashSet<>();
        Set<Long> returnedDeptIds = new LinkedHashSet<>();
        // 遍历每一层
        Collection<Long> parentIds = normalizeUnvisitedParentIds(ids, visitedParentIds);
        for (int i = 0; i < Short.MAX_VALUE; i++) { // 使用 Short.MAX_VALUE 避免 bug 场景下，存在死循环
            if (CollUtil.isEmpty(parentIds)) {
                break;
            }
            // 查询当前层，所有的子部门
            List<DeptDO> depts = deptMapper.selectListByParentId(parentIds);
            // 1. 如果没有子部门，则结束遍历
            if (CollUtil.isEmpty(depts)) {
                break;
            }
            // 2. 如果有子部门，继续遍历
            for (DeptDO dept : depts) {
                if (dept.getId() != null && returnedDeptIds.add(dept.getId())) {
                    children.add(dept);
                }
            }
            parentIds = normalizeUnvisitedParentIds(convertSet(depts, DeptDO::getId), visitedParentIds);
            if (CollUtil.isEmpty(parentIds)) {
                break;
            }
        }
        return children;
    }

    private Collection<Long> normalizeUnvisitedParentIds(Collection<Long> parentIds, Set<Long> visitedParentIds) {
        if (CollUtil.isEmpty(parentIds)) {
            return Collections.emptySet();
        }
        Set<Long> unvisitedParentIds = new LinkedHashSet<>();
        for (Long parentId : parentIds) {
            if (parentId != null && visitedParentIds.add(parentId)) {
                unvisitedParentIds.add(parentId);
            }
        }
        return unvisitedParentIds;
    }

    @Override
    public List<DeptDO> getDeptListByLeaderUserId(Long id) {
        return deptMapper.selectListByLeaderUserId(id);
    }

    @Override
    @DataPermission(enable = false) // 禁用数据权限，避免建立不正确的缓存
    @Cacheable(cacheNames = RedisKeyConstants.DEPT_CHILDREN_ID_LIST, key = "#id")
    public Set<Long> getChildDeptIdListFromCache(Long id) {
        List<DeptDO> children = getChildDeptList(id);
        return convertSet(children, DeptDO::getId);
    }

    @Override
    public void validateDeptList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得科室信息
        Map<Long, DeptDO> deptMap = getDeptMap(ids);
        // 校验
        ids.forEach(id -> {
            DeptDO dept = deptMap.get(id);
            if (dept == null) {
                throw exception(DEPT_NOT_FOUND);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus())) {
                throw exception(DEPT_NOT_ENABLE, dept.getName());
            }
        });
    }

}
