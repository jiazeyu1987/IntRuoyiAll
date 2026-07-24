package cn.iocoder.yudao.module.mes.service.pro.route;

/**
 * 工艺路线创建者权限 Service。
 */
public interface MesProRouteOwnerPermissionService {

    /**
     * 将当前登录用户绑定为指定路线的所有者。
     *
     * @param routeId 工艺路线编号
     */
    void bindCurrentUserAsOwner(Long routeId);

}
