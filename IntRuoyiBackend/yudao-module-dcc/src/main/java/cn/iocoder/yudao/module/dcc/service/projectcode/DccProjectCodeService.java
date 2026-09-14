package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeAssociatedFileAiCategoryRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeExportExcelVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DccProjectCodeService {

    Long createProjectCode(DccProjectCodeSaveReqVO reqVO);

    void updateProjectCode(DccProjectCodeUpdateReqVO reqVO);

    void deleteProjectCode(Long id);

    PageResult<DccProjectCodeDO> getProjectCodePage(Long userId, DccProjectCodePageReqVO reqVO);

    PageResult<DccProjectCodeDO> getProjectCodePage(DccProjectCodePageReqVO reqVO);

    DccProjectCodeDO getProjectCode(Long id);

    DccProjectCodeDO getProjectCode(Long userId, Long id);

    PageResult<DccControlledFileRespVO> getControlledFilePage(Long userId, Long id,
                                                              DccProjectCodeControlledFilePageReqVO reqVO);

    List<DccProjectCodeAssociatedFileAiCategoryRespVO> getAssociatedFileAiCategoryCandidates(Long userId, Long id);

    DccProjectCodeAssociatedFileAiCategoryRespVO classifyAssociatedFileByName(Long userId, Long id, Long fileId);

    List<DccProjectCodeExportExcelVO> getExportList(DccProjectCodePageReqVO reqVO);

    DccProjectCodeImportPreviewRespVO previewImport(MultipartFile file) throws Exception;

    DccProjectCodeImportPreviewRespVO confirmImport(Long batchId);
}
