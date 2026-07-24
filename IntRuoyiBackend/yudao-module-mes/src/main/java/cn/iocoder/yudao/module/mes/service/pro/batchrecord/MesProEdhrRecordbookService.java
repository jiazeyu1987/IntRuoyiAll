package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrControlledTagStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntrySaveDraftReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEntrySubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateActivateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrRecordbookTemplateRespVO;

public interface MesProEdhrRecordbookService {

    PageResult<MesProEdhrRecordbookTemplateRespVO> getTemplatePage(MesProEdhrRecordbookTemplatePageReqVO reqVO);

    MesProEdhrRecordbookTemplateRespVO createTemplate(MesProEdhrRecordbookTemplateCreateReqVO reqVO);

    MesProEdhrRecordbookTemplateRespVO activateTemplate(MesProEdhrRecordbookTemplateActivateReqVO reqVO);

    PageResult<MesProEdhrRecordbookRespVO> getRecordbookPage(MesProEdhrRecordbookPageReqVO reqVO);

    PageResult<MesProEdhrRecordbookRespVO> getMyRecordbookPage(MesProEdhrRecordbookPageReqVO reqVO);

    MesProEdhrRecordbookRespVO createRecordbook(MesProEdhrRecordbookCreateReqVO reqVO);

    PageResult<MesProEdhrRecordbookEntryRespVO> getEntryPage(MesProEdhrRecordbookEntryPageReqVO reqVO);

    MesProEdhrRecordbookEntryRespVO getEntry(Long id);

    MesProEdhrRecordbookEntryRespVO createEntry(MesProEdhrRecordbookEntryCreateReqVO reqVO);

    MesProEdhrRecordbookEntryRespVO saveDraft(MesProEdhrRecordbookEntrySaveDraftReqVO reqVO);

    MesProEdhrRecordbookEntryRespVO submit(MesProEdhrRecordbookEntrySubmitReqVO reqVO);

    PageResult<MesProEdhrRecordbookEventRespVO> getEventPage(MesProEdhrRecordbookEventPageReqVO reqVO);

    PageResult<MesProEdhrControlledTagRespVO> getTagPage(MesProEdhrControlledTagPageReqVO reqVO);

    MesProEdhrControlledTagRespVO createTag(MesProEdhrControlledTagCreateReqVO reqVO);

    MesProEdhrControlledTagRespVO activateTag(MesProEdhrControlledTagStatusReqVO reqVO);

    MesProEdhrControlledTagRespVO disableTag(MesProEdhrControlledTagStatusReqVO reqVO);
}
