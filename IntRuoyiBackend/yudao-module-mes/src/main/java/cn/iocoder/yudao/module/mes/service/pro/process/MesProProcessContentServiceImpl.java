package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.content.MesProProcessContentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessContentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessContentMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_CONTENT_NOT_EXISTS;

/**
 * MES 生产工序内容 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
public class MesProProcessContentServiceImpl implements MesProProcessContentService {

    @Resource
    private MesProProcessContentMapper processContentMapper;

    @Resource
    @Lazy
    private MesProProcessService processService;

    @Resource @Lazy
    private cn.iocoder.yudao.module.mes.service.common.MesSharedMasterDataChangeGuard sharedChangeGuard;

    @Override
    public Long createProcessContent(MesProProcessContentSaveReqVO createReqVO) {
        sharedChangeGuard.assertDirectUpdateAllowed("PROCESS", createReqVO.getProcessId());
        // 校验工序存在
        processService.validateProcessExists(createReqVO.getProcessId());

        // 插入
        MesProProcessContentDO content = BeanUtils.toBean(createReqVO, MesProProcessContentDO.class);
        processContentMapper.insert(content);
        return content.getId();
    }

    @Override
    public void updateProcessContent(MesProProcessContentSaveReqVO updateReqVO) {
        // 校验存在
        validateProcessContentExists(updateReqVO.getId());
        MesProProcessContentDO existing = processContentMapper.selectById(updateReqVO.getId());
        sharedChangeGuard.assertDirectUpdateAllowed("PROCESS", existing.getProcessId());
        if (!java.util.Objects.equals(existing.getProcessId(), updateReqVO.getProcessId()))
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException("操作步骤不可转移至其他工序");

        // 更新
        MesProProcessContentDO updateObj = BeanUtils.toBean(updateReqVO, MesProProcessContentDO.class);
        processContentMapper.updateById(updateObj);
    }

    @Override
    public void deleteProcessContent(Long id) {
        // 校验存在
        validateProcessContentExists(id);
        sharedChangeGuard.assertDirectUpdateAllowed("PROCESS", processContentMapper.selectById(id).getProcessId());

        // 删除
        processContentMapper.deleteById(id);
    }

    private void validateProcessContentExists(Long id) {
        if (processContentMapper.selectById(id) == null) {
            throw exception(PRO_PROCESS_CONTENT_NOT_EXISTS);
        }
    }

    @Override
    public MesProProcessContentDO getProcessContent(Long id) {
        return processContentMapper.selectById(id);
    }

    @Override
    public List<MesProProcessContentDO> getProcessContentListByProcessId(Long processId) {
        return processContentMapper.selectListByProcessId(processId);
    }

    @Override
    public void deleteProcessContentByProcessId(Long processId) {
        sharedChangeGuard.assertDirectUpdateAllowed("PROCESS", processId);
        processContentMapper.deleteByProcessId(processId);
    }

}
