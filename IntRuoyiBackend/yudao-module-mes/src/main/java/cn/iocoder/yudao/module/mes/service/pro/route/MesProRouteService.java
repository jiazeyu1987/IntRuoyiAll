package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * MES 工艺路线 Service 接口
 *
 * @author 瑛泰源码
 */
public interface MesProRouteService {

    /**
     * 创建工艺路线
     */
    Long createRoute(@Valid MesProRouteSaveReqVO createReqVO);

    /**
     * 更新工艺路线
     */
    void updateRoute(@Valid MesProRouteSaveReqVO updateReqVO);

    /**
     * 复制工艺路线
     */
    Long copyRoute(Long sourceRouteId, String targetCode, String targetName);

    /**
     * 工艺路线工序变更后维护路线版本。
     *
     * 已被排产引用的激活版本必须封存并生成新版本；未被引用的激活版本只刷新快照。
     *
     * @param routeId 工艺路线编号
     */
    void maintainRouteVersionAfterProcessChange(Long routeId);

    /**
     * 基于当前正式配置表构建完整路线版本快照。
     *
     * @param routeId 工艺路线编号
     * @param routeVersionId 路线版本编号
     * @return 完整路线快照 JSON
     */
    String buildCurrentRouteSnapshotJson(Long routeId, Long routeVersionId);

    /**
     * 为指定路线工序补齐默认智能排产用途和默认排产策略。
     *
     * @param routeId 工艺路线编号
     * @param routeProcessId 工艺路线工序编号
     */
    void ensureDefaultScheduleArtifacts(Long routeId, Long routeProcessId);

    /**
     * 更新工艺路线状态（启用/禁用）
     *
     * @param id 编号
     * @param status 状态
     */
    void updateRouteStatus(Long id, Integer status);

    /**
     * 删除工艺路线
     */
    void deleteRoute(Long id);

    /**
     * 获得工艺路线
     */
    MesProRouteDO getRoute(Long id);

    /**
     * 获得带展示聚合字段的工艺路线
     */
    MesProRouteRespVO getRouteRespVO(Long id);

    /**
     * 获得工艺路线分页
     */
    PageResult<MesProRouteDO> getRoutePage(MesProRoutePageReqVO pageReqVO);

    /**
     * 获得带展示聚合字段的工艺路线分页
     */
    PageResult<MesProRouteRespVO> getRoutePageRespVO(MesProRoutePageReqVO pageReqVO);

    /**
     * 获得工艺路线列表
     */
    List<MesProRouteDO> getRouteList();

    /**
     * 获得启用状态的工艺路线列表
     */
    List<MesProRouteDO> getRouteListByStatus(Integer status);

    /**
     * 校验工艺路线未启用（已启用则抛异常）
     *
     * @param routeId 工艺路线编号
     */
    void validateRouteNotEnable(Long routeId);

    /**
     * 校验工艺路线存在
     *
     * @param id 工艺路线编号
     * @return 工艺路线
     */
    MesProRouteDO validateRouteExists(Long id);

    /**
     * 获得工艺路线列表
     *
     * @param ids 编号数组
     * @return 工艺路线列表
     */
    List<MesProRouteDO> getRouteList(Collection<Long> ids);

    /**
     * 获得工艺路线列表（忽略逻辑删除）
     *
     * @param ids 编号数组
     * @return 工艺路线列表
     */
    List<MesProRouteDO> getRouteListIgnoreDeleted(Collection<Long> ids);

    /**
     * 获得工艺路线 Map
     *
     * @param ids 编号数组
     * @return 工艺路线 Map
     */
    default Map<Long, MesProRouteDO> getRouteMap(Collection<Long> ids) {
        return convertMap(getRouteList(ids), MesProRouteDO::getId);
    }

    /**
     * 获得工艺路线 Map（忽略逻辑删除）
     *
     * @param ids 编号数组
     * @return 工艺路线 Map
     */
    default Map<Long, MesProRouteDO> getRouteMapIgnoreDeleted(Collection<Long> ids) {
        return convertMap(getRouteListIgnoreDeleted(ids), MesProRouteDO::getId);
    }

}
