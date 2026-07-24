package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccDistributionTaskServiceImpl implements DccDistributionTaskService {

    static final String STATUS_READY_TO_ACKNOWLEDGE = "READY_TO_ACKNOWLEDGE";

    private static final Set<String> DISTRIBUTION_VISIBLE_FILE_STATUSES = Set.of(
            DccControlledFileStatusEnum.ACTIVE.getStatus(),
            DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
            DccControlledFileStatusEnum.OBSOLETE.getStatus()
    );

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;

    @Override
    public PageResult<DccDistributionTaskRespVO> getMyDistributionTaskPage(Long userId,
                                                                          DccDistributionTaskPageReqVO reqVO) {
        List<DccDistributionTaskRespVO> rows = distributionRecipientMapper.selectListByUserId(userId).stream()
                .filter(recipient -> recipient.getAcknowledgedAt() == null)
                .map(this::buildTaskRowOrNull)
                .filter(Objects::nonNull)
                .filter(row -> matchesTaskFilter(row, reqVO))
                .sorted(Comparator.comparing(
                                DccDistributionTaskRespVO::getPublishedTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                DccDistributionTaskRespVO::getRecipientId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return slicePage(reqVO.getPageNo(), reqVO.getPageSize(), rows);
    }

    private DccDistributionTaskRespVO buildTaskRowOrNull(DccControlledFileDistributionRecipientDO recipient) {
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(recipient.getDistributionId());
        if (distribution == null) {
            throw new IllegalStateException("DCC distribution recipient " + recipient.getId()
                    + " references missing distribution " + recipient.getDistributionId());
        }
        if (!DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distribution.getDistributionMedium())) {
            return null;
        }
        DccControlledFileDO file = controlledFileMapper.selectById(distribution.getControlledFileId());
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!DISTRIBUTION_VISIBLE_FILE_STATUSES.contains(file.getStatus())) {
            return null;
        }
        DccDistributionTaskRespVO respVO = new DccDistributionTaskRespVO();
        respVO.setRecipientId(recipient.getId());
        respVO.setDistributionId(distribution.getId());
        respVO.setControlledFileId(file.getId());
        respVO.setCategoryId(file.getCategoryId());
        respVO.setFileName(StrUtil.blankToDefault(file.getFileName(), file.getTitle()));
        respVO.setTitle(file.getTitle());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setFileStatus(file.getStatus());
        respVO.setUserId(recipient.getUserId());
        respVO.setDepartmentId(distribution.getDepartmentId());
        respVO.setDistributionMedium(distribution.getDistributionMedium());
        respVO.setReadAt(recipient.getReadAt());
        respVO.setAcknowledgedAt(recipient.getAcknowledgedAt());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setStatus(STATUS_READY_TO_ACKNOWLEDGE);
        return respVO;
    }

    private boolean matchesTaskFilter(DccDistributionTaskRespVO row, DccDistributionTaskPageReqVO reqVO) {
        if (reqVO.getCategoryId() != null && !reqVO.getCategoryId().equals(row.getCategoryId())) {
            return false;
        }
        return StrUtil.isBlank(reqVO.getStatus()) || StrUtil.equals(reqVO.getStatus(), row.getStatus());
    }

    private <T> PageResult<T> slicePage(Integer pageNo, Integer pageSize, List<T> rows) {
        if (rows.isEmpty()) {
            return PageResult.empty(0L);
        }
        int resolvedPageNo = Math.max(pageNo == null ? 1 : pageNo, 1);
        int resolvedPageSize = Math.max(pageSize == null ? 10 : pageSize, 1);
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, rows.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }
}
