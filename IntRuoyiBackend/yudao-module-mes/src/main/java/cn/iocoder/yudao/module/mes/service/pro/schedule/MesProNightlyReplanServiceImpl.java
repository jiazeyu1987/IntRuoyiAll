package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleSummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MesProNightlyReplanServiceImpl implements MesProNightlyReplanService {

    private static final String CAPACITY_MODE_PLANNED = "PLANNED";

    private final MesProScheduleOrderMapper scheduleOrderMapper;
    private final MesProAutoScheduleService autoScheduleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProNightlyReplanResult executeNightlyReplan(LocalDateTime startTime) {
        List<MesProScheduleOrderDO> scheduleOrders = scheduleOrderMapper.selectListForNightlyReplan();
        MesProNightlyReplanResult result = new MesProNightlyReplanResult();
        result.setScheduleOrderCount(scheduleOrders.size());
        if (CollUtil.isEmpty(scheduleOrders)) {
            return result;
        }

        MesProAutoScheduleReplanReqVO reqVO = new MesProAutoScheduleReplanReqVO();
        reqVO.setScheduleOrderIds(scheduleOrders.stream().map(MesProScheduleOrderDO::getId).toList());
        reqVO.setStartTime(startTime);
        reqVO.setRuntimeCapacityBasis(CAPACITY_MODE_PLANNED);
        reqVO.setPreserveManualLockedTasks(Boolean.TRUE);
        reqVO.setReason("夜间自动重排");

        MesProAutoScheduleReplanPreviewRespVO previewRespVO = autoScheduleService.replanPreview(reqVO);
        if (previewRespVO == null || previewRespVO.getCalendarContextToken() == null
                || previewRespVO.getCalendarContextToken().isBlank()) {
            throw new IllegalStateException("夜间自动重排预览缺少日历上下文令牌");
        }
        reqVO.setCalendarContextToken(previewRespVO.getCalendarContextToken());
        MesProAutoScheduleApplyRespVO applyRespVO = autoScheduleService.replanApplyForNightly(reqVO);
        MesProAutoScheduleSummaryRespVO summary = applyRespVO.getSummary();
        if (summary != null) {
            result.setGeneratedTaskCount(nvl(summary.getGeneratedTaskCount()));
            result.setPreservedTaskCount(nvl(summary.getPreservedTaskCount()));
            result.setBlockingIssueCount(nvl(summary.getBlockingIssueCount()));
            result.setShortageCount(nvl(summary.getShortageCount()));
        }
        return result;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

}
