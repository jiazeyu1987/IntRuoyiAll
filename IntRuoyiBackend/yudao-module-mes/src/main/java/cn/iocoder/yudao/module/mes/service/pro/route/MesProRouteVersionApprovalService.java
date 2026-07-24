package cn.iocoder.yudao.module.mes.service.pro.route;

/**
 * 工艺路线版本审批回调服务。
 */
public interface MesProRouteVersionApprovalService {

    MesProRouteVersionApprovalResult handleApprovalCallback(String approvalProcessInstanceId,
                                                            String approvalEventId,
                                                            String approvalResult,
                                                            String rejectReason,
                                                            Long actorUserId);
}
