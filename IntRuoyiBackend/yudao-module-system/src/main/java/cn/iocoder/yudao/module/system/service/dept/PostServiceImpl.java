package cn.iocoder.yudao.module.system.service.dept;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 岗位 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class PostServiceImpl implements PostService {

    @Resource
    private cn.iocoder.yudao.module.system.service.permission.GlobalApprovalAuthorityCoordinator approvalAuthority;

    @Resource
    private PostMapper postMapper;

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Long createPost(PostSaveReqVO createReqVO) {
        var authority = approvalAuthority.lockGlobalRoleTenants();
        // 校验正确性
        validatePostForCreateOrUpdate(null, createReqVO.getName());

        // 插入岗位
        PostDO post = BeanUtils.toBean(createReqVO, PostDO.class);
        post.setCode(generatePostCode());
        if (postMapper.insert(post) != 1) throw new IllegalStateException("APPROVAL_POST_CREATE_ROW_COUNT");
        approvalAuthority.incrementLockedTenants(authority);
        return post.getId();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void updatePost(PostSaveReqVO updateReqVO) {
        var authority = approvalAuthority.lockGlobalRoleTenants();
        // 校验正确性
        validatePostForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getName());

        // 更新岗位
        PostDO updateObj = BeanUtils.toBean(updateReqVO, PostDO.class);
        if (postMapper.updateById(updateObj) != 1) throw new IllegalStateException("APPROVAL_POST_UPDATE_ROW_COUNT");
        approvalAuthority.incrementLockedTenants(authority);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        var authority = approvalAuthority.lockGlobalRoleTenants();
        // 校验是否存在
        validatePostExists(id);
        // 删除岗位
        if (postMapper.deleteById(id) != 1) throw new IllegalStateException("APPROVAL_POST_DELETE_ROW_COUNT");
        approvalAuthority.incrementLockedTenants(authority);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deletePostList(List<Long> ids) {
        var authority = approvalAuthority.lockGlobalRoleTenants();
        if (ids == null || ids.isEmpty() || new java.util.HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("APPROVAL_POST_DELETE_IDENTITIES_REQUIRED");
        }
        if (postMapper.deleteByIds(ids) != ids.size()) throw new IllegalStateException("APPROVAL_POST_DELETE_ROW_COUNT");
        approvalAuthority.incrementLockedTenants(authority);
    }

    private String generatePostCode() {
        // 全局审批权限锁已串行化岗位创建/导入；FOR UPDATE 使用当前读，避免等待锁后读取旧快照。
        BigInteger next = postMapper.selectGeneratedCodesForUpdate().stream()
                .filter(code -> code.matches("(?i)GW-[0-9]+"))
                .map(code -> new BigInteger(code.substring(3)))
                .max(BigInteger::compareTo).orElse(BigInteger.ZERO).add(BigInteger.ONE);
        String code = String.format(Locale.ROOT, "GW-%03d", next);
        if (code.length() > 64) {
            throw new IllegalStateException("岗位编码流水号已超出数据库长度限制");
        }
        return code;
    }

    private void validatePostForCreateOrUpdate(Long id, String name) {
        // 校验自己存在
        validatePostExists(id);
        // 校验岗位名的唯一性
        validatePostNameUnique(id, name);
    }

    private void validatePostNameUnique(Long id, String name) {
        PostDO post = postMapper.selectByName(name);
        if (post == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的岗位
        if (id == null) {
            throw exception(POST_NAME_DUPLICATE);
        }
        if (!post.getId().equals(id)) {
            throw exception(POST_NAME_DUPLICATE);
        }
    }

    private void validatePostExists(Long id) {
        if (id == null) {
            return;
        }
        if (postMapper.selectById(id) == null) {
            throw exception(POST_NOT_FOUND);
        }
    }

    @Override
    public List<PostDO> getPostList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return postMapper.selectByIds(ids);
    }

    @Override
    public List<PostDO> getPostList(Collection<Long> ids, Collection<Integer> statuses) {
        return postMapper.selectList(ids, statuses);
    }

    @Override
    public PageResult<PostDO> getPostPage(PostPageReqVO reqVO) {
        return postMapper.selectPage(reqVO);
    }

    @Override
    public PostDO getPost(Long id) {
        return postMapper.selectById(id);
    }

    @Override
    public void validatePostList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得岗位信息
        List<PostDO> posts = postMapper.selectByIds(ids);
        Map<Long, PostDO> postMap = convertMap(posts, PostDO::getId);
        // 校验
        ids.forEach(id -> {
            PostDO post = postMap.get(id);
            if (post == null) {
                throw exception(POST_NOT_FOUND);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(post.getStatus())) {
                throw exception(POST_NOT_ENABLE, post.getName());
            }
        });
    }
}
