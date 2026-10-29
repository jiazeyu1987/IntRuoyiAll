package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** No permission expansion: only current formal SYSTEM_USER responsibility records are recipients. */
@Service
public class MesNcrFreezeRecipientResolver {
    @Resource private MesProcessPoolActiveOrderMapper orders;
    @Resource private MesActiveOrderHandoffTaskMapper handoffs;
    @Resource private MesProEdhrWorkTaskMapper workTasks;
    @Resource private AdminUserMapper users;

    public String resolve(MesProEdhrNonconformanceReviewDO review) {
        require(review!=null&&Objects.equals(tenant(),review.getTenantId())&&review.getWorkOrderId()!=null,
                "冻结告知缺少本租户正式工单");
        var active=orders.selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
                .eq(MesProcessPoolActiveOrderDO::getTenantId,tenant())
                .eq(MesProcessPoolActiveOrderDO::getWorkOrderId,review.getWorkOrderId())
                .eq(MesProcessPoolActiveOrderDO::getActiveStatus,"ACTIVE"));
        require(active!=null&&!active.isEmpty(),"冻结告知缺少当前正式责任周期");
        var result=new TreeSet<Long>();
        var cycleIds=new HashSet<Long>();
        for(var order:active) {
            require(Objects.equals(tenant(),order.getTenantId())&&Objects.equals(review.getWorkOrderId(),order.getWorkOrderId())
                    &&"ACTIVE".equals(order.getActiveStatus())&&order.getId()!=null,"冻结告知周期责任越界");
            require(order.getLeaderUserId()!=null&&order.getLeaderUserId()>0,"冻结告知当前正式生产组长缺失");
            cycleIds.add(order.getId());result.add(order.getLeaderUserId());
        }
        // Completing a production acceptance does not remove its frozen personnel responsibility
        // while that exact active cycle is still running. Do not expand to current role memberships.
        var current=handoffs.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getTenantId,tenant())
                .eq(MesActiveOrderHandoffTaskDO::getWorkOrderId,review.getWorkOrderId())
                .in(MesActiveOrderHandoffTaskDO::getActiveOrderId,cycleIds)
                .and(q->q.eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO").or(done->done
                        .eq(MesActiveOrderHandoffTaskDO::getTaskType,"PRODUCTION_HANDOFF")
                        .eq(MesActiveOrderHandoffTaskDO::getStatus,"DONE"))));
        require(current!=null,"冻结告知正式交接责任查询失败");
        for(var task:current) {
            require(Objects.equals(tenant(),task.getTenantId())&&cycleIds.contains(task.getActiveOrderId())
                    &&Objects.equals(review.getWorkOrderId(),task.getWorkOrderId())
                    &&("TODO".equals(task.getStatus())||("DONE".equals(task.getStatus())&&"PRODUCTION_HANDOFF".equals(task.getTaskType()))),"冻结告知交接责任越界");
            require("SYSTEM_USER".equals(JsonUtils.parseTree(task.getResponsibilitySnapshotJson()).path("identityDomain").asText()),
                    "冻结告知不得将人员档案身份当作系统账号");
            result.addAll(candidates(task.getCandidateUserSnapshot()));
        }
        // work_task owns tenant_id in SQL (legacy DO inherits BaseDO); keep tenant explicit.
        var pending=workTasks.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .apply("tenant_id={0}",tenant()).eq(MesProEdhrWorkTaskDO::getWorkOrderId,review.getWorkOrderId())
                .in(MesProEdhrWorkTaskDO::getStatus,List.of("TODO","DOING","OVERDUE"))
                .in(MesProEdhrWorkTaskDO::getTaskType,List.of("FILL","REVIEW","APPROVE","RELEASE_APPROVE","REWORK","ARCHIVE","CLOSE")));
        require(pending!=null,"冻结告知正式执行责任查询失败");
        for(var task:pending) {
            require(Objects.equals(review.getWorkOrderId(),task.getWorkOrderId())
                    &&Set.of("TODO","DOING","OVERDUE").contains(task.getStatus())
                    &&Set.of("FILL","REVIEW","APPROVE","RELEASE_APPROVE","REWORK","ARCHIVE","CLOSE").contains(task.getTaskType()),"冻结告知执行责任越界");
            // Assignee supersedes candidate snapshot after a real acceptance or transfer.
            if(task.getAssigneeUserId()!=null)result.add(task.getAssigneeUserId());
            else result.addAll(candidates(task.getCandidateUserSnapshot()));
        }
        // QA already receives the original review notification, which states the freeze reason.
        result.remove(review.getQaReviewerUserId());
        for(Long id:result) {
            require(id!=null&&id>0,"冻结告知正式系统账号缺失");
            var user=users.selectById(id);
            require(user!=null&&Objects.equals(tenant(),user.getTenantId())&&CommonStatusEnum.isEnable(user.getStatus()),
                    "冻结告知正式责任账号不存在、非本租户或已停用");
        }
        return MesActiveOrderHandoffOwnerResolver.snapshot(result);
    }
}
