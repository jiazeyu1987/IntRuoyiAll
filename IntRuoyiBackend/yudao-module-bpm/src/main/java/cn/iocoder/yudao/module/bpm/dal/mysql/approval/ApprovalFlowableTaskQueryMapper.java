package cn.iocoder.yudao.module.bpm.dal.mysql.approval;

import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

/** Approval-center-only MySQL 8 / Flowable 7.2 query; never replaces the engine mapper. */
@Mapper
@com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true")
public interface ApprovalFlowableTaskQueryMapper {
    long count(Map<String,Object> parameters);
    List<String> window(Map<String,Object> parameters);
    long countBrokenProvenance(Map<String,Object> parameters);
    List<BpmApprovalProcessOwnershipDO> ownerships(Map<String,Object> parameters);
    long countInitiated(Map<String,Object> parameters);
    List<String> windowInitiated(Map<String,Object> parameters);
    long countCopied(Map<String,Object> parameters);
    List<cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmProcessInstanceCopyDO> windowCopied(Map<String,Object> parameters);
}
