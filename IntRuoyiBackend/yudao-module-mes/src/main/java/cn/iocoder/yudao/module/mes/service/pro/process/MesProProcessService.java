package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * MES 生产工序 Service 接口
 *
 * @author 瑛泰源码
 */
public interface MesProProcessService {

    /**
     * 创建生产工序
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createProcess(@Valid MesProProcessSaveReqVO createReqVO);

    /**
     * 更新生产工序
     *
     * @param updateReqVO 更新信息
     */
    void updateProcess(@Valid MesProProcessSaveReqVO updateReqVO);

    /**
     * 删除生产工序
     *
     * @param id 编号
     */
    void deleteProcess(Long id);

    /**
     * 获得生产工序
     *
     * @param id 编号
     * @return 生产工序
     */
    MesProProcessDO getProcess(Long id);

    /**
     * 获得生产工序详情，包含当前可用产能、路线、批记录表单和填写对象。
     *
     * @param id 编号
     * @param routeId 工艺路线编号，为空时按所有关联路线聚合
     * @return 生产工序详情
     */
    MesProProcessRespVO getProcessWithCapacity(Long id, Long routeId);

    /**
     * 获得生产工序列表
     *
     * @param ids 编号列表
     * @return 生产工序列表
     */
    List<MesProProcessDO> getProcessList(Collection<Long> ids);

    /**
     * 获得生产工序分页
     *
     * @param pageReqVO 分页查询
     * @return 生产工序分页
     */
    PageResult<MesProProcessDO> getProcessPage(MesProProcessPageReqVO pageReqVO);

    /**
     * 获得生产工序分页，包含设备数量和当前可用产能。
     *
     * @param pageReqVO 分页查询
     * @return 生产工序分页
     */
    PageResult<MesProProcessRespVO> getProcessPageWithCapacity(MesProProcessPageReqVO pageReqVO);

    /**
     * 获得生产工序关联设备明细。
     *
     * @param processId 工序编号
     * @return 设备明细
     */
    List<MesProProcessMachineryRespVO> getProcessMachineryList(Long processId);

    /**
     * 获得指定状态的生产工序列表
     *
     * @param status 状态
     * @return 生产工序列表
     */
    List<MesProProcessDO> getProcessListByStatus(Integer status);

    /**
     * 校验工序存在
     *
     * @param id 编号
     */
    void validateProcessExists(Long id);

    /**
     * 校验工序存在且启用
     *
     * @param id 编号
     */
    void validateProcessExistsAndEnable(Long id);

    /**
     * 获得工序 Map
     *
     * @param ids 编号列表
     * @return 工序 Map
     */
    default Map<Long, MesProProcessDO> getProcessMap(Collection<Long> ids) {
        return convertMap(getProcessList(ids), MesProProcessDO::getId);
    }

}
