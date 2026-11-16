package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read current QA responsibility from formal NCR bindings, never from role candidates. */
@Service
public class MesActiveOrderNcrResponsibilityReader {
    @Resource private MesProEdhrNonconformanceReviewMapper reviews;
    @Resource private AdminUserApi users;

    public void populate(List<MesTeamLeaderActiveOrderRow> rows) {
        if (rows.isEmpty()) return;
        var ids = rows.stream().map(MesTeamLeaderActiveOrderRow::getId).toList();
        var pending = reviews.selectList(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .in(MesProEdhrNonconformanceReviewDO::getActiveOrderId, ids)
                .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, "pending_review")
                .orderByAsc(MesProEdhrNonconformanceReviewDO::getId));
        var reviewerIds = pending.stream().map(MesProEdhrNonconformanceReviewDO::getQaReviewerUserId)
                .filter(Objects::nonNull).distinct().toList();
        var userMap = reviewerIds.isEmpty() ? Map.<Long, AdminUserRespDTO>of()
                : users.getUserMap(reviewerIds);
        var byOrder = pending.stream().collect(Collectors.groupingBy(MesProEdhrNonconformanceReviewDO::getActiveOrderId));
        for (var row : rows) {
            row.setPendingNonconformanceReviews(byOrder.getOrDefault(row.getId(), List.of()).stream().map(review -> {
                if (!Objects.equals(row.getWorkOrderId(), review.getWorkOrderId()))
                    throw new IllegalStateException("不合格评审与活跃订单的工单不一致：" + review.getId());
                Long reviewerId = review.getQaReviewerUserId();
                var user = reviewerId == null ? null : userMap.get(reviewerId);
                if (reviewerId != null && (user == null || !Objects.equals(user.getTenantId(), review.getTenantId()) || user.getNickname() == null || user.getNickname().isBlank()))
                    throw new IllegalStateException("不合格评审已指派的QA责任人信息缺失：" + review.getId());
                return new MesTeamLeaderActiveOrderRow.PendingNonconformanceReview()
                        .setReviewId(review.getId().toString()).setReviewCode(review.getReviewCode())
                        .setQaReviewerUserId(reviewerId == null ? null : reviewerId.toString())
                        .setQaReviewerName(user == null ? null : user.getNickname());
            }).toList());
        }
    }
}
