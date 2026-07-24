package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

public interface MesProEdhrPermissionScopeService {

    MesProEdhrPermissionEvaluateResult evaluate(MesProEdhrPermissionEvaluateCommand command);

    MesProEdhrPermissionScopeDetailResult saveRules(MesProEdhrPermissionScopeSaveCommand command);

    MesProEdhrPermissionScopeDetailResult getDetail(MesProEdhrPermissionScopeQueryCommand command);
}
