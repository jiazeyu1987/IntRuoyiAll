package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process.MesProRouteProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MES 工艺路线工序 Service 接口
 *
 * @author 瑛泰源码
 */
public interface MesProRouteProcessService {

    /**
     * 创建工艺路线工序
     */
    Long createRouteProcess(@Valid MesProRouteProcessSaveReqVO createReqVO);

    /**
     * 更新工艺路线工序
     */
    void updateRouteProcess(@Valid MesProRouteProcessSaveReqVO updateReqVO);

    /**
     * 删除工艺路线工序
     */
    void deleteRouteProcess(Long id);

    /**
     * 获得工艺路线工序
     */
    MesProRouteProcessDO getRouteProcess(Long id);

    /**
     * 按工艺路线获得工序列表
     */
    List<MesProRouteProcessDO> getRouteProcessListByRouteId(Long routeId);

    /**
     * 按多个工艺路线获得工序列表
     *
     * @param routeIds 工艺路线编号数组
     * @return 工序列表
     */
    List<MesProRouteProcessDO> getRouteProcessListByRouteIds(Collection<Long> routeIds);

    /**
     * 按多个工艺路线获得工序列表（忽略逻辑删除）
     *
     * @param routeIds 工艺路线编号数组
     * @return 工序列表
     */
    List<MesProRouteProcessDO> getRouteProcessListByRouteIdsIgnoreDeleted(Collection<Long> routeIds);

    /**
     * 按工艺路线和工序获得工艺路线工序
     *
     * @param routeId   工艺路线编号
     * @param processId 工序编号
     * @return 工艺路线工序
     */
    MesProRouteProcessDO getRouteProcessByRouteIdAndProcessId(Long routeId, Long processId);

    /**
     * 将历史工序身份解析为指定路线当前有效的路线工序。
     *
     * <p>优先使用有效的路线工序快照关系；快照关系已删除或未提供时，
     * 通过历史工序编码在指定路线中查找唯一当前工序。缺失或歧义均明确失败。</p>
     *
     * @param routeProcessId 历史路线工序编号，可为空
     * @param routeId 工艺路线编号
     * @param sourceProcessId 来源工序编号
     * @return 当前有效路线工序
     */
    MesProRouteProcessDO resolveCurrentRouteProcess(Long routeProcessId, Long routeId, Long sourceProcessId);

    /**
     * 将历史生产对象保存的路线工序编号解析为创建时冻结的路线工序。
     *
     * <p>以持久化的 routeProcessId 及其逻辑删除快照为稳定身份，不按当前 ACTIVE
     * 路线重新映射。当来源 processId 与路线工序 processId 不一致时，
     * 通过工序编码，或编码迁移场景下同名且产品名不冲突的工序身份确认；
     * 缺失或身份不匹配时明确失败。</p>
     *
     * @param routeProcessId 冻结路线工序编号，不能为空
     * @param routeId 工艺路线编号
     * @param sourceProcessId 来源工序编号
     * @return 冻结路线工序
     */
    MesProRouteProcessDO resolveFrozenRouteProcess(Long routeProcessId, Long routeId, Long sourceProcessId);

    /**
     * 按工序编码建立历史/当前工序编号到指定目标工序编号的身份映射。
     *
     * @param targetProcessIds 业务上下文中使用的目标工序编号
     * @return key 为所有同编码历史/当前工序编号，value 为对应目标工序编号
     */
    Map<Long, Long> getProcessIdentityMap(Collection<Long> targetProcessIds);

    /**
     * 按产品获得工序列表
     *
     * 根据产品查找关联的工艺路线，返回该路线的工序列表
     *
     * @param productId 产品编号
     * @return 工序列表
     */
    List<MesProRouteProcessDO> getRouteProcessListByProductId(Long productId);

    /**
     * 按工序获得工艺路线工序列表
     *
     * @param processId 工序编号
     * @return 工艺路线工序列表
     */
    List<MesProRouteProcessDO> getRouteProcessListByProcessId(Long processId);

    /**
     * 按多个工序获得工艺路线工序列表
     *
     * @param processIds 工序编号数组
     * @return 工艺路线工序列表
     */
    List<MesProRouteProcessDO> getRouteProcessListByProcessIds(Collection<Long> processIds);

    /**
     * 按工艺路线删除工序（级联删除使用）
     *
     * @param routeId 工艺路线编号
     */
    void deleteRouteProcessByRouteId(Long routeId);

}
