package cn.iocoder.yudao.module.dcc.service.position;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
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
public class DccApprovalPositionConfigPackageServiceImpl implements DccApprovalPositionConfigPackageService {

    private static final String PACKAGE_VERSION = "1";

    @Resource
    private DccApprovalPositionMapper positionMapper;

    @Resource
    private DccPositionAssignmentMapper assignmentMapper;

    @Override
    public byte[] exportPackage() {
        DccApprovalPositionConfigPackage payload = new DccApprovalPositionConfigPackage();
        payload.setPackageVersion(PACKAGE_VERSION);
        payload.setPositions(positionMapper.selectList().stream()
                .sorted(Comparator.comparing(DccApprovalPositionDO::getName).thenComparing(DccApprovalPositionDO::getId))
                .map(this::toItem)
                .toList());
        return JsonUtils.toJsonByte(payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importPackage(byte[] content) {
        DccApprovalPositionConfigPackage payload = parsePayload(content);
        validatePayload(payload);
        for (DccApprovalPositionConfigItem item : payload.getPositions()) {
            validateItem(item);
            DccApprovalPositionDO existing = positionMapper.selectOne(DccApprovalPositionDO::getCode, item.getCode());
            if (existing == null) {
                DccApprovalPositionDO position = DccApprovalPositionDO.builder()
                        .code(item.getCode())
                        .name(item.getName())
                        .active(item.getActive())
                        .source(item.getSource())
                        .remark(item.getRemark())
                        .build();
                positionMapper.insert(position);
                replaceAssignments(position.getId(), item.getAssignments());
                continue;
            }
            existing.setName(item.getName());
            existing.setActive(item.getActive());
            existing.setSource(item.getSource());
            existing.setRemark(item.getRemark());
            positionMapper.updateById(existing);
            replaceAssignments(existing.getId(), item.getAssignments());
        }
    }

    private DccApprovalPositionConfigPackage parsePayload(byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(CONFIG_PACKAGE_FILE_EMPTY);
        }
        try {
            return JsonUtils.parseObject(content, DccApprovalPositionConfigPackage.class);
        } catch (RuntimeException ex) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包 JSON 非法");
        }
    }

    private DccApprovalPositionConfigItem toItem(DccApprovalPositionDO position) {
        DccApprovalPositionConfigItem item = new DccApprovalPositionConfigItem();
        item.setCode(position.getCode());
        item.setName(position.getName());
        item.setActive(position.getActive());
        item.setSource(position.getSource());
        item.setRemark(position.getRemark());
        item.setAssignments(CollectionUtils.convertList(
                assignmentMapper.selectList(DccPositionAssignmentDO::getPositionId, position.getId()),
                this::toAssignmentItem));
        return item;
    }

    private DccPositionAssignmentConfigItem toAssignmentItem(DccPositionAssignmentDO assignment) {
        DccPositionAssignmentConfigItem item = new DccPositionAssignmentConfigItem();
        item.setAssignmentType(assignment.getAssignmentType());
        item.setSystemPostId(assignment.getSystemPostId());
        item.setUserId(assignment.getUserId());
        item.setActive(assignment.getActive());
        item.setChangeReason(assignment.getChangeReason());
        return item;
    }

    private void replaceAssignments(Long positionId, List<DccPositionAssignmentConfigItem> assignments) {
        assignmentMapper.delete(DccPositionAssignmentDO::getPositionId, positionId);
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        for (DccPositionAssignmentConfigItem item : assignments) {
            DccPositionAssignmentDO assignment = DccPositionAssignmentDO.builder()
                    .positionId(positionId)
                    .assignmentType(item.getAssignmentType())
                    .systemPostId(item.getSystemPostId())
                    .userId(item.getUserId())
                    .active(item.getActive())
                    .changeReason(item.getChangeReason())
                    .build();
            assignmentMapper.insert(assignment);
        }
    }

    private void validatePayload(DccApprovalPositionConfigPackage payload) {
        if (payload == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包 JSON 非法");
        }
        if (!PACKAGE_VERSION.equals(payload.getPackageVersion())) {
            throw exception(CONFIG_PACKAGE_FORMAT_UNSUPPORTED, payload.getPackageVersion());
        }
        if (payload.getPositions() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包 positions 不能为空");
        }
    }

    private void validateItem(DccApprovalPositionConfigItem item) {
        if (item == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包存在空 position");
        }
        if (!StringUtils.hasText(item.getCode())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包缺少 position code");
        }
        if (!StringUtils.hasText(item.getName())) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包缺少 position name，code={}", item.getCode());
        }
        if (item.getAssignments() == null) {
            throw exception(CONFIG_PACKAGE_CONTENT_INVALID, "审批角色配置包缺少 assignments，code={}", item.getCode());
        }
    }

    @Data
    public static class DccApprovalPositionConfigPackage {
        private String packageVersion;
        private List<DccApprovalPositionConfigItem> positions = new ArrayList<>();
    }

    @Data
    public static class DccApprovalPositionConfigItem {
        private String code;
        private String name;
        private Boolean active;
        private String source;
        private String remark;
        private List<DccPositionAssignmentConfigItem> assignments = new ArrayList<>();
    }

    @Data
    public static class DccPositionAssignmentConfigItem {
        private String assignmentType;
        private Long systemPostId;
        private Long userId;
        private Boolean active;
        private String changeReason;
    }
}
