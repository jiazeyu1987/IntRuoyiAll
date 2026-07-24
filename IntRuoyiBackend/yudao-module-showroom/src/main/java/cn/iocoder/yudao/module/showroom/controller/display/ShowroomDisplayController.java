package cn.iocoder.yudao.module.showroom.controller.display;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.release.ShowroomLegacyWebsiteConfigProjector;
import cn.iocoder.yudao.module.showroom.release.ShowroomPublicReleaseScopeResolver;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/showroom/display")
@Validated
public class ShowroomDisplayController {

    private final ShowroomApiRuntime runtime;
    private final ShowroomLegacyWebsiteConfigProjector legacyWebsiteConfigProjector;
    private final ShowroomPublicReleaseScopeResolver scopeResolver;

    @Autowired
    public ShowroomDisplayController(ShowroomApiRuntime runtime,
                                     ShowroomLegacyWebsiteConfigProjector legacyWebsiteConfigProjector,
                                     ShowroomPublicReleaseScopeResolver scopeResolver) {
        this.runtime = runtime;
        this.legacyWebsiteConfigProjector = legacyWebsiteConfigProjector;
        this.scopeResolver = scopeResolver;
    }

    @GetMapping("/home")
    public CommonResult<HomePayload> getHome() {
        return success(runtime.displayHome());
    }

    @GetMapping("/hall/{hallId}")
    public CommonResult<HallPayload> getHall(@PathVariable("hallId") Long hallId) {
        return success(runtime.displayHall(hallId));
    }

    @GetMapping("/narration")
    public CommonResult<NarrationPayload> getNarration(@RequestParam("targetType") String targetType,
                                                       @RequestParam("targetId") Long targetId,
                                                       @RequestParam("audienceType") String audienceType,
                                                       @RequestParam("language") String language) {
        return success(runtime.displayNarration(targetType, targetId, audienceType, language));
    }

    @GetMapping("/website-config")
    @PermitAll
    public CommonResult<WebsiteConfigPayload> getWebsiteConfig(@RequestParam("siteKey") String siteKey,
                                                               @RequestParam("stage") String stage) {
        var scope = scopeResolver.resolve(siteKey, stage);
        return scopeResolver.executeInTenant(scope, () -> legacyWebsiteConfigProjector.projectCurrentPayload(scope));
    }

    @GetMapping("/website-config/response")
    @PermitAll
    public ResponseEntity<String> getWebsiteConfigResponse(@RequestParam("siteKey") String siteKey,
                                                           @RequestParam("stage") String stage,
                                                           @RequestHeader HttpHeaders headers) {
        var scope = scopeResolver.resolve(siteKey, stage);
        return scopeResolver.executeInTenant(scope, () -> legacyWebsiteConfigProjector.getCurrentResponse(scope, headers));
    }

    @GetMapping("/runtime-client-settings")
    @PermitAll
    public CommonResult<RuntimeClientSettingsRespVO> getRuntimeClientSettings(@RequestParam("siteKey") String siteKey,
                                                                              @RequestParam("stage") String stage) {
        var scope = scopeResolver.resolve(siteKey, stage);
        return scopeResolver.executeInTenant(scope, () -> success(runtime.getRuntimeClientSettings()));
    }

    @PutMapping("/runtime-client-settings")
    @PermitAll
    public CommonResult<RuntimeClientSettingsRespVO> saveRuntimeClientSettings(
            @RequestBody RuntimeClientSettingsSaveReqVO reqVO) {
        var scope = scopeResolver.resolve(reqVO.siteKey(), reqVO.stage());
        return scopeResolver.executeInTenant(scope, () -> success(runtime.saveRuntimeClientSettings(reqVO)));
    }

    public record CompanySummary(Long id, String displayName, String description) {
    }

    public record DisplayCard(Long id, String nameCn, String nameEn, boolean incompleteFlag,
                              String previewImageUrl) {
    }

    public record PublicField(String label, String value) {
    }

    public record BilingualPublicField(String fieldCode, String labelZh, String labelEn, String valueZh,
                                       String valueEn) {
    }

    public record NarrationSummary(String targetType, Long targetId) {
    }

    public record HomePayload(CompanySummary companySummary, List<DisplayCard> hallEntries,
                              NarrationSummary narrationSummary) {
    }

    public record HallInfo(Long id, String nameCn, String description) {
    }

    public record HallPayload(HallInfo hall, List<DisplayCard> productCards, List<PublicField> publicProductFields,
                              NarrationSummary narrationSummary) {
    }

    public record NarrationPayload(String text, String audioUrl) {
    }

    public record WebsiteConfigPayload(WebsiteConfigCompany company, List<WebsiteConfigShowroom> showrooms) {
    }

    public record WebsiteConfigCompany(Long companyId, String name, String nameEn, String homeImageUrl,
                                       String subtitleZh, String subtitleEn, String audioZhUrl, String audioEnUrl,
                                       List<PublicField> publicFields,
                                       List<BilingualPublicField> bilingualPublicFields) {
    }

    public record WebsiteConfigShowroom(Long hallId, String hallCode, String name, String nameEn, String description,
                                        String descriptionEn, String previewImageUrl,
                                        String audioZhUrl, String audioEnUrl,
                                        List<WebsiteConfigProduct> products) {
    }

    public record WebsiteConfigProduct(Long productId, String productCode, String nameCn, String nameEn,
                                       boolean incompleteFlag, String previewImageUrl, String subtitleZh,
                                       String subtitleEn, String audioZhUrl, String audioEnUrl,
                                       List<PublicField> publicFields,
                                       List<BilingualPublicField> bilingualPublicFields) {
    }

    public record RuntimeClientSettingsRespVO(RuntimeClientCompanyDetailSettings companyDetailSettings) {
    }

    public record RuntimeClientSettingsSaveReqVO(String siteKey, String stage,
                                                 RuntimeClientCompanyDetailSettings companyDetailSettings) {
    }

    public record RuntimeClientCompanyDetailSettings(Integer productItemHorizontalGap,
                                                     Integer productItemVerticalGap) {
    }

}
