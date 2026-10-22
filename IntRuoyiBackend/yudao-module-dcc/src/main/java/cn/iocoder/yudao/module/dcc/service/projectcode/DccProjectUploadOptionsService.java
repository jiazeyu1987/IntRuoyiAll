package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;

/** Project selection readiness only; category, folder and application checks still run after selection. */
@Service
public class DccProjectUploadOptionsService {
    @Resource private DccProjectCodeService projectCodeService;
    @Resource private DccControlledFileWorkflowService workflowService;

    public record Option(Long id, String projectName, String projectCode, String status,
                         Long projectLeaderUserId, String projectLeader, Long associatedFileCount,
                         boolean uploadSelectable, List<String> uploadIssues) { }

    @Transactional(readOnly = true)
    public List<Option> list(Long userId, String keyword) {
        var request=new DccProjectCodePageReqVO();
        request.setPageSize(PageParam.PAGE_SIZE_NONE); request.setStatus("ENABLE"); request.setKeyword(keyword);
        return projectCodeService.getReadableProjectCodePage(userId,request).getList().stream()
                .map(project -> option(userId,project))
                .sorted(Comparator.comparing(Option::uploadSelectable).reversed()
                        .thenComparing(Option::projectCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(Option::id))
                .toList();
    }

    private Option option(Long userId, DccProjectCodeDO project) {
        var issues=new ArrayList<String>();
        if (project.getProjectCode()==null || project.getProjectCode().isBlank()) issues.add("缺少项目代码");
        if (project.getProjectName()==null || project.getProjectName().isBlank()) issues.add("缺少项目名称");
        checkDefaults(project.getDefaultAttributesJson(),issues);
        try {
            workflowService.previewProjectProduct(userId,project.getId());
        } catch (ServiceException error) {
            if (error.getCode().equals(DCC_PROJECT_ACCESS_DENIED.getCode())) issues.add("当前账号缺少项目 EDIT/OWNER 上传权限，请联系项目管理员授权");
            else if (error.getCode().equals(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING.getCode())
                    || error.getCode().equals(CONTROLLED_FILE_PRODUCT_CODE_INVALID.getCode()))
                issues.add("项目关联产品资料不完整或不一致，请在 DCC产品目录核对产品代码、名称及审批关联");
            else throw error;
        }
        return new Option(project.getId(),project.getProjectName(),project.getProjectCode(),project.getStatus(),
                project.getProjectLeaderUserId(),project.getProjectLeader(),project.getAssociatedFileCount(),
                issues.isEmpty(),List.copyOf(issues));
    }

    private void checkDefaults(String json, List<String> issues) {
        String entry="；请到【基础数据 → DCC项目代码 → 修改项目默认属性与负责人账号】维护";
        if (json==null || json.isBlank()) {
            issues.add("缺少默认属性：目标市场、是否为注册人、是否为生产方、文件转移"+entry); return;
        }
        try {
            var value=JsonUtils.getObjectMapper().readValue(json,DccProjectAttributes.class);
            if (value==null) { issues.add("默认属性为空"+entry); return; }
            var missing=new ArrayList<String>();
            if (value.targetMarkets()==null || value.targetMarkets().isEmpty()) missing.add("目标市场");
            if (value.licenseHolder()==null || value.licenseHolder().isBlank()) missing.add("是否为注册人");
            if (value.actualManufacturer()==null || value.actualManufacturer().isBlank()) missing.add("是否为生产方");
            if (value.documentTransfer()==null || value.documentTransfer().isBlank()) missing.add("文件转移");
            if (value.targetMarkets()!=null && value.targetMarkets().contains("OTHER")
                    && (value.otherMarket()==null || value.otherMarket().isBlank())) missing.add("其他目标市场说明");
            if ("Y".equals(value.documentTransfer()) && (value.transferTo()==null || value.transferTo().isBlank())) missing.add("文件转移目标");
            if (!missing.isEmpty()) { issues.add("默认属性缺少："+String.join("、",missing)+entry); return; }
            try { value.validated(); }
            catch (ServiceException error) { issues.add(error.getMessage()+entry); }
        } catch (JsonProcessingException error) {
            issues.add("默认属性格式不合法"+entry);
        }
    }
}
