package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.worker.MesMdWorkstationWorkerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationWorkerDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * MES 人力资源 Service 接口
 *
 * @author 瑛泰源码
 */
public interface MesMdWorkstationWorkerService {

    /**
     * 创建人力资源
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkstationWorker(@Valid MesMdWorkstationWorkerSaveReqVO createReqVO);

    /**
     * 更新人力资源
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkstationWorker(@Valid MesMdWorkstationWorkerSaveReqVO updateReqVO);

    /**
     * 删除人力资源
     *
     * @param id 编号
     */
    void deleteWorkstationWorker(Long id);

    /**
     * 获得人力资源列表
     *
     * @param workstationId 工作站编号
     * @return 人力资源列表
     */
    List<MesMdWorkstationWorkerDO> getWorkstationWorkerListByWorkstationId(Long workstationId);

    /**
     * 获得人力资源列表
     *
     * @param workstationIds 工作站编号集合
     * @return 人力资源列表
     */
    List<MesMdWorkstationWorkerDO> getWorkstationWorkerListByWorkstationIds(Collection<Long> workstationIds);

    /**
     * 按岗位编号获得人力资源列表
     *
     * @param postIds 岗位编号集合
     * @return 人力资源列表
     */
    List<MesMdWorkstationWorkerDO> getWorkstationWorkerListByPostIds(Collection<Long> postIds);

    /**
     * 按工作站编号删除人力资源（级联删除）
     *
     * @param workstationId 工作站编号
     */
    void deleteWorkstationWorkerByWorkstationId(Long workstationId);

}
