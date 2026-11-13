package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccElectronicSignatureAuthorizationAuditMapper
        extends BaseMapperX<DccElectronicSignatureAuthorizationAuditDO> {
    default cn.iocoder.yudao.framework.common.pojo.PageResult<DccElectronicSignatureAuthorizationAuditDO> selectAuditPage(
            Long userId,cn.iocoder.yudao.module.dcc.controller.admin.signature.vo.DccElectronicSignatureAuthorizationAuditPageReqVO request) {
        var wrapper=new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DccElectronicSignatureAuthorizationAuditDO>();
        wrapper.eq("target_user_id",userId);
        String user="(SELECT "+DccSignatureAuthorizationUserMapper.displayName("u")+" FROM system_users u WHERE u.id=operator_id AND u.deleted=0)";
        cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.apply(request,wrapper,java.util.Map.of(
            "operatedAt",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.dateTime("operated_at"),
            "operatorName",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text(user),
            "beforeState",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text(DccSignatureAuthorizationUserMapper.stateLabel("before_state")),
            "afterState",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text(DccSignatureAuthorizationUserMapper.stateLabel("after_state")),
            "reason",cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("reason")));
        return selectPage(request,wrapper.orderByDesc("operated_at","id"));
    }
}
