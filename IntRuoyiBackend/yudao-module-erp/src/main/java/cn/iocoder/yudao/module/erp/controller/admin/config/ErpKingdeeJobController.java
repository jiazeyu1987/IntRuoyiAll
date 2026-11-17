package cn.iocoder.yudao.module.erp.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.enums.kingdeeautosync.ErpKingdeeTableAutoSyncTypeEnum;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.job.vo.job.JobSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.Valid;
import org.quartz.SchedulerException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/** ERP 配置只维护现有 ERP 同步任务，不授予通用定时任务管理权限。 */
@RestController
@RequestMapping("/erp/kingdee-config/job")
@Validated
@PreAuthorize("@ss.hasPermission('erp:data-config:operate')")
public class ErpKingdeeJobController {
    @Resource private JobService jobService;

    @GetMapping("/page")
    public CommonResult<PageResult<JobRespVO>> getPage(@Valid JobPageReqVO request) {
        requireHandler(request.getHandlerName());
        PageResult<JobDO> page = jobService.getJobPage(request);
        var list = page.getList().stream().filter(job -> request.getHandlerName().equals(job.getHandlerName())).toList();
        return success(new PageResult<>(BeanUtils.toBean(list, JobRespVO.class), (long) list.size()));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody ScheduleRequest request) throws SchedulerException {
        JobDO job = requireJob(request.getId());
        // 此页只允许修改执行时间；处理器、参数、重试配置均沿用数据库记录。
        JobSaveReqVO update = BeanUtils.toBean(job, JobSaveReqVO.class);
        update.setCronExpression(request.getCronExpression());
        jobService.updateJob(update);
        return success(true);
    }

    @PutMapping("/update-status")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) throws SchedulerException {
        requireJob(id);
        jobService.updateJobStatus(id, status);
        return success(true);
    }

    @Data
    public static class ScheduleRequest {
        @NotNull private Long id;
        @NotBlank private String cronExpression;
    }

    private JobDO requireJob(Long id) {
        JobDO job = id == null ? null : jobService.getJob(id);
        if (job == null) throw new IllegalArgumentException("ERP synchronization job does not exist");
        requireHandler(job.getHandlerName());
        return job;
    }

    private void requireHandler(String handler) {
        if (ErpKingdeeTableAutoSyncTypeEnum.list().stream().noneMatch(type -> Objects.equals(type.getHandlerName(),handler))) {
            throw new IllegalArgumentException("Only existing ERP synchronization handlers can be maintained here");
        }
    }
}
