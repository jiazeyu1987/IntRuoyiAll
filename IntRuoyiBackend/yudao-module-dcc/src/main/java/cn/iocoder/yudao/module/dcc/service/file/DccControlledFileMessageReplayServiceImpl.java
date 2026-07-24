package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMessageJobReplayReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_REPLAY_REQUEST_INVALID;

@Service
@Validated
public class DccControlledFileMessageReplayServiceImpl implements DccControlledFileMessageReplayService {

    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private DccControlledFileMessageDeliveryService messageDeliveryService;

    @Override
    public int replayMessageJobs(DccControlledFileMessageJobReplayReqVO reqVO) {
        if (reqVO == null || reqVO.getJobIds() == null || reqVO.getJobIds().isEmpty()) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_REPLAY_REQUEST_INVALID);
        }
        List<DccControlledFileMessageJobDO> jobs = messageJobMapper.selectBatchIds(reqVO.getJobIds());
        Map<Long, DccControlledFileMessageJobDO> jobMap = new LinkedHashMap<>();
        for (DccControlledFileMessageJobDO job : jobs) {
            jobMap.put(job.getId(), job);
        }
        for (Long jobId : reqVO.getJobIds()) {
            if (!jobMap.containsKey(jobId)) {
                throw exception(CONTROLLED_FILE_MESSAGE_JOB_NOT_EXISTS);
            }
        }
        for (Long jobId : reqVO.getJobIds()) {
            messageDeliveryService.replayMessageJob(Objects.requireNonNull(jobMap.get(jobId)));
        }
        return reqVO.getJobIds().size();
    }
}
