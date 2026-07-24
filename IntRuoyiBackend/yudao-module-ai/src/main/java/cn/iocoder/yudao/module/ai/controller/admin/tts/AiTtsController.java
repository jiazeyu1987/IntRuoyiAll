package cn.iocoder.yudao.module.ai.controller.admin.tts;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.ai.controller.admin.tts.vo.AiTtsAliyunNlsAppKeySaveReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.tts.vo.AiTtsAliyunNlsDefaultsRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.tts.vo.AiTtsAliyunNlsTokenRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.tts.vo.AiTtsAliyunNlsTokenSaveReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.tts.vo.AiTtsAliyunNlsVoiceSaveReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.tts.vo.AiTtsGenerateReqVO;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI TTS 测试")
@RestController
@RequestMapping("/ai/tts-test")
public class AiTtsController {

    @Resource
    private AiTtsService aiTtsService;
    @Resource
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @Resource
    private YudaoAiProperties yudaoAiProperties;

    @PostMapping("/generate")
    @Operation(summary = "生成 TTS 测试音频")
    @PreAuthorize("@ss.hasPermission('ai:music:query')")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody AiTtsGenerateReqVO reqVO) {
        AiTtsService.AudioPayload payload = aiTtsService.generateSpeech(reqVO.getText(), reqVO.getProvider(), reqVO.getVoice());
        MediaType contentType = payload.contentType() != null ? payload.contentType() : MediaType.parseMediaType("audio/wav");
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(payload.audioBytes());
    }

    @GetMapping("/aliyun-nls-defaults")
    @Operation(summary = "获得阿里云 NLS 默认配置")
    @PreAuthorize("@ss.hasPermission('ai:music:query')")
    public CommonResult<AiTtsAliyunNlsDefaultsRespVO> getAliyunNlsDefaults() {
        AiTtsAliyunNlsCredentialService.VoiceStatus voiceStatus =
                aliyunNlsCredentialService.getVoiceStatus(yudaoAiProperties.getTts());
        AiTtsAliyunNlsCredentialService.AppKeyStatus appKeyStatus =
                aliyunNlsCredentialService.getAppKeyStatus(yudaoAiProperties.getTts());
        AiTtsAliyunNlsCredentialService.AccessTokenStatus tokenStatus =
                aliyunNlsCredentialService.getAccessTokenStatus(yudaoAiProperties.getTts());
        return success(new AiTtsAliyunNlsDefaultsRespVO(voiceStatus.voice(), voiceStatus.saved(),
                voiceStatus.configured(), voiceStatus.source(), appKeyStatus.saved(), appKeyStatus.configured(),
                appKeyStatus.source(), appKeyStatus.maskedAppKey(), tokenStatus.saved(), tokenStatus.configured(),
                tokenStatus.source(), tokenStatus.maskedAccessToken()));
    }

    @GetMapping("/aliyun-nls-token")
    @Operation(summary = "获得阿里云 NLS AccessToken 状态")
    @PreAuthorize("@ss.hasPermission('ai:music:query')")
    public CommonResult<AiTtsAliyunNlsTokenRespVO> getAliyunNlsTokenStatus() {
        AiTtsAliyunNlsCredentialService.AccessTokenStatus status =
                aliyunNlsCredentialService.getAccessTokenStatus(yudaoAiProperties.getTts());
        return success(new AiTtsAliyunNlsTokenRespVO(status.saved(), status.configured(), status.source(),
                status.maskedAccessToken()));
    }

    @PutMapping("/aliyun-nls-token")
    @Operation(summary = "保存阿里云 NLS AccessToken")
    @Parameter(name = "accessToken", description = "阿里云 NLS AccessToken", required = true, example = "test-access-token")
    @PreAuthorize("@ss.hasPermission('ai:music:update')")
    public CommonResult<Boolean> saveAliyunNlsToken(@Valid @RequestBody AiTtsAliyunNlsTokenSaveReqVO reqVO) {
        aliyunNlsCredentialService.saveAccessToken(reqVO.getAccessToken());
        return success(true);
    }

    @PutMapping("/aliyun-nls-default-voice")
    @Operation(summary = "保存阿里云 NLS 默认音色")
    @PreAuthorize("@ss.hasPermission('ai:music:update')")
    public CommonResult<Boolean> saveAliyunNlsDefaultVoice(@Valid @RequestBody AiTtsAliyunNlsVoiceSaveReqVO reqVO) {
        aliyunNlsCredentialService.saveVoice(reqVO.getVoice());
        return success(true);
    }

    @PutMapping("/aliyun-nls-appkey")
    @Operation(summary = "保存阿里云 NLS AppKey")
    @PreAuthorize("@ss.hasPermission('ai:music:update')")
    public CommonResult<Boolean> saveAliyunNlsAppKey(@Valid @RequestBody AiTtsAliyunNlsAppKeySaveReqVO reqVO) {
        aliyunNlsCredentialService.saveAppKey(reqVO.getAppKey());
        return success(true);
    }

}
