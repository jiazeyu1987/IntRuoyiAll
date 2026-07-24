package cn.iocoder.yudao.module.showroom.controller.admin.keyword;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordPageReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordPageRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword.KeywordSaveReqVO;
import cn.iocoder.yudao.module.showroom.keyword.service.ShowroomKeywordService;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/showroom/keyword")
@Validated
public class ShowroomKeywordAdminController {

    public static final String SHOWROOM_PUBLICITY_ROLE_CODE = ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE;

    private final ShowroomKeywordService keywordService;
    private final SecurityFrameworkService securityFrameworkService;

    public ShowroomKeywordAdminController(ShowroomKeywordService keywordService,
                                          SecurityFrameworkService securityFrameworkService) {
        this.keywordService = keywordService;
        this.securityFrameworkService = securityFrameworkService;
    }

    @GetMapping("/page")
    public CommonResult<PageResult<KeywordPageRespVO>> getPage(@Valid KeywordPageReqVO reqVO) {
        requirePublicityRole("查看关键词中英对照");
        return success(keywordService.getPage(reqVO));
    }

    @GetMapping("/get")
    public CommonResult<KeywordRespVO> get(@RequestParam("id") Long id) {
        requirePublicityRole("查看关键词中英对照");
        return success(keywordService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody KeywordSaveReqVO reqVO) {
        requirePublicityRole("新增关键词中英对照");
        return success(keywordService.create(reqVO));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody KeywordSaveReqVO reqVO) {
        requirePublicityRole("修改关键词中英对照");
        keywordService.update(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        requirePublicityRole("删除关键词中英对照");
        keywordService.delete(id);
        return success(true);
    }

    private void requirePublicityRole(String actionLabel) {
        Long operatorUserId = requireOperatorUserId();
        if (!isShowroomPublicity(operatorUserId)) {
            throw exception0(FORBIDDEN.getCode(), "当前用户无权执行" + actionLabel);
        }
    }

    private boolean isShowroomPublicity(Long operatorUserId) {
        return operatorUserId != null && (securityFrameworkService.hasRole(SHOWROOM_PUBLICITY_ROLE_CODE)
                || securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode()));
    }

    private Long requireOperatorUserId() {
        Long operatorUserId = SecurityFrameworkUtils.getLoginUserId();
        if (operatorUserId == null) {
            throw exception0(FORBIDDEN.getCode(), "当前登录用户不存在，无法执行当前操作");
        }
        return operatorUserId;
    }

}
