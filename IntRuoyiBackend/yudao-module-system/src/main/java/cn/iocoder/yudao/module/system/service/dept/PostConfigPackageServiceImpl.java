package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_CONTENT_INVALID;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FILE_EMPTY;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CONFIG_PACKAGE_FORMAT_UNSUPPORTED;

@Service
public class PostConfigPackageServiceImpl implements PostConfigPackageService {

    private static final String PACKAGE_VERSION = "1";

    @Resource
    private PostMapper postMapper;

    @Override
    public byte[] exportPackage() {
        PostConfigPackage payload = new PostConfigPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setPosts(postMapper.selectList().stream()
                .sorted(Comparator.comparing(PostDO::getSort).thenComparing(PostDO::getId))
                .map(this::toItem)
                .toList());
        return JsonUtils.toJsonByte(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importPackage(byte[] content) {
        PostConfigPackage payload = parsePayload(content);
        validatePayload(payload);
        for (PostConfigItem item : payload.getPosts()) {
            validateItem(item);
            PostDO existing = postMapper.selectByCode(item.getCode());
            if (existing == null) {
                PostDO post = new PostDO();
                post.setName(item.getName());
                post.setCode(item.getCode());
                post.setSort(item.getSort());
                post.setStatus(item.getStatus());
                post.setRemark(item.getRemark());
                postMapper.insert(post);
                continue;
            }
            existing.setName(item.getName());
            existing.setSort(item.getSort());
            existing.setStatus(item.getStatus());
            existing.setRemark(item.getRemark());
            postMapper.updateById(existing);
        }
    }

    private PostConfigPackage parsePayload(byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(CONFIG_PACKAGE_FILE_EMPTY);
        }
        try {
            return JsonUtils.parseObject(content, PostConfigPackage.class);
        } catch (RuntimeException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包 JSON 非法");
        }
    }

    private PostConfigItem toItem(PostDO post) {
        PostConfigItem item = new PostConfigItem();
        item.setCode(post.getCode());
        item.setName(post.getName());
        item.setSort(post.getSort());
        item.setStatus(post.getStatus());
        item.setRemark(post.getRemark());
        return item;
    }

    private void validatePayload(PostConfigPackage payload) {
        if (payload == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包 JSON 非法");
        }
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw exception(CONFIG_PACKAGE_FORMAT_UNSUPPORTED, payload.getPackageVersion());
        }
        if (payload.getPosts() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包 posts 不能为空");
        }
    }

    private void validateItem(PostConfigItem item) {
        if (item == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包存在空 post");
        }
        if (!StringUtils.hasText(item.getCode())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包缺少 post code");
        }
        if (!StringUtils.hasText(item.getName())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包缺少 post name，code={}", item.getCode());
        }
        if (item.getSort() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包缺少 post sort，code={}", item.getCode());
        }
        if (item.getStatus() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "组织角色配置包缺少 post status，code={}", item.getCode());
        }
    }

    @Data
    public static class PostConfigPackage {
        private String packageVersion;
        private List<PostConfigItem> posts = new ArrayList<>();
    }

    @Data
    public static class PostConfigItem {
        private String code;
        private String name;
        private Integer sort;
        private Integer status = CommonStatusEnum.ENABLE.getStatus();
        private String remark;
    }
}
