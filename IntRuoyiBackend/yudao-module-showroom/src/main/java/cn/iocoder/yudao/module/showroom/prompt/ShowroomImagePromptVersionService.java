package cn.iocoder.yudao.module.showroom.prompt;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.showroom.dal.dataobject.prompt.ShowroomImagePromptVersionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.prompt.ShowroomImagePromptVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShowroomImagePromptVersionService {

    public static final String SCENE_PRODUCT_COVER = "PRODUCT_COVER";
    public static final String SCENE_AWARD_COVER = "AWARD_COVER";
    public static final String PLACEHOLDER_PRODUCT_NAME_CN = "product_name_cn";
    public static final String PLACEHOLDER_PRODUCT_NAME_EN = "product_name_en";
    public static final String PLACEHOLDER_AWARD_NAME_CN = "award_name_cn";
    public static final String PLACEHOLDER_AWARD_NAME_EN = "award_name_en";
    public static final String PLACEHOLDER_AWARD_ISSUER = "award_issuer";
    public static final String PLACEHOLDER_AWARD_DATE_TEXT = "award_date_text";
    public static final String PLACEHOLDER_AWARD_DESCRIPTION_ZH = "award_description_zh";
    public static final String DEFAULT_PRODUCT_COVER_TEMPLATE = """
            生成一张横向医疗器械产品展示图，用于产品列表卡片缩略图。
            背景：
            极简圆角卡片风格背景，整体为很浅的冰蓝色到白色渐变，四周有轻微柔和蓝色光晕，背景干净、通透、明亮，带高级医疗科技感。不要真实展台，不要桌面，不要道具，不要复杂场景，不要文字。
            主体：
            主体是“{{product_name_cn}}”，英文名参考“{{product_name_en}}”。
            如果该产品属于导丝类，只展示一根指引导丝；如果不是导丝类，只展示一个对应的医疗器械产品主体。完整展示，居中偏上放置，轮廓清晰，质感精致，具有医疗器械产品图风格。若有参考产品图，则以前景产品的真实外形、颜色和结构为准，不要随意改造。
            构图与大小：
            产品大小控制在画面宽度的45%到55%，高度约占画面30%到40%，不要太大，不要贴边，四周保留充足留白，视觉比例接近医疗展厅产品卡片中的缩略图效果。
            风格：
            简洁、克制、现代、专业、柔和打光、轻微悬浮感，高端医疗器械目录图风格。
            避免：
            人物、多个物体、复杂背景、重阴影、夸张反光、文字、logo、水印、产品过大、产品贴边、背景过花。
            约束：
            仅根据“{{product_name_cn}}”对应产品生成单个主体，不要替换为其他产品，不要生成方图，不要输出任何说明文字。
            只进行一次原生图片生成。
            最终只返回一个本地 PNG 绝对路径，不要输出其他内容。
            """.trim();
    public static final String DEFAULT_AWARD_COVER_TEMPLATE = """
            Create a landscape image enhancement based on the provided existing award cover image for showroom award display.
            Scene: keep the original award subject and visual meaning, refine composition, clean the background, and preserve the single award-focused subject without introducing unrelated objects.
            Style: polished premium editorial visual, clean and professional, suitable for an enterprise honor showroom.
            Composition: centered subject, upright orientation, balanced margins, consistent thumbnail framing, wide landscape layout, clear silhouette.
            Lighting and mood: bright, soft, controlled lighting, professional and trustworthy, no dramatic shadows.
            Details: award name "{{award_name_cn}}", optional English name "{{award_name_en}}", issuer "{{award_issuer}}", award date "{{award_date_text}}", Chinese description "{{award_description_zh}}". Use them only to preserve the intended award context; do not render readable text in the image.
            Constraints: beautify the current award image, unify size and framing, keep the subject centered and visually straight, avoid changing the subject into a different object, do not add people, trophies, certificates, UI, logos, watermark, brand marks, or unrelated scene elements.
            Avoid: watermark, random logos, distorted text, unintended branding, extra objects.
            Only perform one native image generation.
            Return only one local absolute PNG path and nothing else.
            """.trim();

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");
    private static final Set<String> SUPPORTED_PLACEHOLDER_CODES = Set.of(
            PLACEHOLDER_PRODUCT_NAME_CN,
            PLACEHOLDER_PRODUCT_NAME_EN,
            PLACEHOLDER_AWARD_NAME_CN,
            PLACEHOLDER_AWARD_NAME_EN,
            PLACEHOLDER_AWARD_ISSUER,
            PLACEHOLDER_AWARD_DATE_TEXT,
            PLACEHOLDER_AWARD_DESCRIPTION_ZH
    );

    private final ShowroomImagePromptVersionMapper promptVersionMapper;

    public ShowroomImagePromptVersionService(ShowroomImagePromptVersionMapper promptVersionMapper) {
        this.promptVersionMapper = promptVersionMapper;
    }

    public ShowroomImagePromptVersion requireCurrent(String sceneCode) {
        String resolvedSceneCode = requireSupportedSceneCode(sceneCode);
        ShowroomImagePromptVersionDO version = promptVersionMapper.selectLatestBySceneCode(resolvedSceneCode);
        if (version == null) {
            version = ensureDefaultVersionDO(resolvedSceneCode);
        }
        return toDomain(version);
    }

    public Long requireCurrentVersionId(String sceneCode) {
        return requireCurrent(sceneCode).id();
    }

    public List<ShowroomImagePromptVersion> history(String sceneCode) {
        String resolvedSceneCode = requireSupportedSceneCode(sceneCode);
        return promptVersionMapper.selectListBySceneCode(resolvedSceneCode).stream()
                .map(this::toDomain)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomImagePromptVersion saveNewVersion(String sceneCode, String templateText, String changeNote) {
        String resolvedSceneCode = requireSupportedSceneCode(sceneCode);
        String normalizedTemplateText = normalizeTemplateText(templateText);
        List<String> placeholderCodes = resolveAndValidatePlaceholderCodes(normalizedTemplateText);
        ShowroomImagePromptVersionDO latest = promptVersionMapper.selectLatestBySceneCode(resolvedSceneCode);
        int nextVersionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        ShowroomImagePromptVersionDO version = ShowroomImagePromptVersionDO.builder()
                .sceneCode(resolvedSceneCode)
                .versionNo(nextVersionNo)
                .templateText(normalizedTemplateText)
                .changeNote(normalizeChangeNote(changeNote))
                .placeholderCodesJson(JsonUtils.toJsonString(placeholderCodes))
                .useCount(0)
                .lastUsedAt(null)
                .build();
        promptVersionMapper.insert(version);
        return toDomain(version);
    }

    public String renderProductCoverPrompt(Long promptVersionId, String productNameCn, String productNameEn) {
        ShowroomImagePromptVersionDO version = requireVersionDO(promptVersionId);
        if (!SCENE_PRODUCT_COVER.equals(version.getSceneCode())) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_SCENE_UNSUPPORTED: prompt version "
                    + promptVersionId + " is not PRODUCT_COVER");
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(normalizePromptTextContent(version.getTemplateText()));
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String placeholderCode = matcher.group(1).trim();
            String replacement = switch (placeholderCode) {
                case PLACEHOLDER_PRODUCT_NAME_CN -> requirePlaceholderValue(productNameCn, placeholderCode,
                        "SHOWROOM_COVER_GENERATION_FAILED: product chinese name is required");
                case PLACEHOLDER_PRODUCT_NAME_EN -> requirePlaceholderValue(productNameEn, placeholderCode,
                        "SHOWROOM_COVER_GENERATION_FAILED: product english name is required");
                default -> throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_PLACEHOLDER_UNSUPPORTED: unsupported placeholder "
                        + placeholderCode);
            };
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString().trim();
    }

    public String renderAwardCoverPrompt(Long promptVersionId, String awardNameCn, String awardNameEn,
                                         String issuer, String awardDateText, String descriptionZh) {
        ShowroomImagePromptVersionDO version = requireVersionDO(promptVersionId);
        if (!SCENE_AWARD_COVER.equals(version.getSceneCode())) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_SCENE_UNSUPPORTED: prompt version "
                    + promptVersionId + " is not AWARD_COVER");
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(normalizePromptTextContent(version.getTemplateText()));
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String placeholderCode = matcher.group(1).trim();
            String replacement = switch (placeholderCode) {
                case PLACEHOLDER_AWARD_NAME_CN -> requirePlaceholderValue(awardNameCn, placeholderCode,
                        "SHOWROOM_AWARD_COVER_GENERATION_FAILED: award chinese name is required");
                case PLACEHOLDER_AWARD_NAME_EN -> StrUtil.nullToEmpty(awardNameEn).trim();
                case PLACEHOLDER_AWARD_ISSUER -> StrUtil.nullToEmpty(issuer).trim();
                case PLACEHOLDER_AWARD_DATE_TEXT -> StrUtil.nullToEmpty(awardDateText).trim();
                case PLACEHOLDER_AWARD_DESCRIPTION_ZH -> StrUtil.nullToEmpty(descriptionZh).trim();
                default -> throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_PLACEHOLDER_UNSUPPORTED: unsupported placeholder "
                        + placeholderCode);
            };
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString().trim();
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordUsage(Long promptVersionId) {
        ShowroomImagePromptVersionDO version = requireVersionDO(promptVersionId);
        if (promptVersionMapper.incrementUsage(version.getId(), LocalDateTime.now()) == 0) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_NOT_FOUND: prompt version not found: "
                    + promptVersionId);
        }
    }

    private ShowroomImagePromptVersionDO requireVersionDO(Long promptVersionId) {
        if (promptVersionId == null) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_NOT_FOUND: prompt version id is required");
        }
        ShowroomImagePromptVersionDO version = promptVersionMapper.selectById(promptVersionId);
        if (version == null) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_NOT_FOUND: prompt version not found: "
                    + promptVersionId);
        }
        return version;
    }

    @Transactional(rollbackFor = Exception.class)
    protected ShowroomImagePromptVersionDO ensureDefaultVersionDO(String sceneCode) {
        ShowroomImagePromptVersionDO existing = promptVersionMapper.selectLatestBySceneCode(sceneCode);
        if (existing != null) {
            return existing;
        }
        ShowroomImagePromptVersion seeded = saveNewVersion(sceneCode, defaultTemplateForScene(sceneCode), "system-default");
        ShowroomImagePromptVersionDO version = promptVersionMapper.selectById(seeded.id());
        if (version == null) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_NOT_FOUND: failed to initialize default prompt version for scene "
                    + sceneCode);
        }
        return version;
    }

    private static String requireSupportedSceneCode(String sceneCode) {
        String normalized = StrUtil.blankToDefault(sceneCode, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_SCENE_UNSUPPORTED: scene code is required");
        }
        if (!SCENE_PRODUCT_COVER.equals(normalized) && !SCENE_AWARD_COVER.equals(normalized)) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_SCENE_UNSUPPORTED: unsupported scene code "
                    + normalized);
        }
        return normalized;
    }

    private static String defaultTemplateForScene(String sceneCode) {
        return switch (requireSupportedSceneCode(sceneCode)) {
            case SCENE_PRODUCT_COVER -> DEFAULT_PRODUCT_COVER_TEMPLATE;
            case SCENE_AWARD_COVER -> DEFAULT_AWARD_COVER_TEMPLATE;
            default -> throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_SCENE_UNSUPPORTED: unsupported scene code "
                    + sceneCode);
        };
    }

    private static String normalizeTemplateText(String templateText) {
        String normalizedTemplateText = normalizePromptTextContent(StrUtil.nullToEmpty(templateText)).trim();
        if (normalizedTemplateText.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_TEMPLATE_INVALID: template text is required");
        }
        return normalizedTemplateText;
    }

    private static String normalizeChangeNote(String changeNote) {
        return StrUtil.nullToEmpty(changeNote).trim();
    }

    private static List<String> resolveAndValidatePlaceholderCodes(String templateText) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateText);
        LinkedHashSet<String> placeholderCodes = new LinkedHashSet<>();
        while (matcher.find()) {
            String placeholderCode = matcher.group(1).trim();
            if (!SUPPORTED_PLACEHOLDER_CODES.contains(placeholderCode)) {
                throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_PLACEHOLDER_UNSUPPORTED: unsupported placeholder "
                        + placeholderCode);
            }
            placeholderCodes.add(placeholderCode);
        }
        boolean hasProductPlaceholder = placeholderCodes.contains(PLACEHOLDER_PRODUCT_NAME_CN)
                || placeholderCodes.contains(PLACEHOLDER_PRODUCT_NAME_EN);
        boolean hasAwardPlaceholder = placeholderCodes.contains(PLACEHOLDER_AWARD_NAME_CN)
                || placeholderCodes.contains(PLACEHOLDER_AWARD_NAME_EN);
        if (!hasProductPlaceholder && !hasAwardPlaceholder) {
            throw new IllegalStateException("SHOWROOM_IMAGE_PROMPT_PLACEHOLDER_REQUIRED: prompt must contain "
                    + "{{" + PLACEHOLDER_PRODUCT_NAME_CN + "}} / {{" + PLACEHOLDER_PRODUCT_NAME_EN + "}} / {{"
                    + PLACEHOLDER_AWARD_NAME_CN + "}} / {{" + PLACEHOLDER_AWARD_NAME_EN + "}}");
        }
        return new ArrayList<>(placeholderCodes);
    }

    private static String requirePlaceholderValue(String value, String placeholderCode, String errorMessage) {
        String normalizedValue = StrUtil.nullToEmpty(value).trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalStateException(errorMessage + " for placeholder " + placeholderCode);
        }
        return normalizedValue;
    }

    private ShowroomImagePromptVersion toDomain(ShowroomImagePromptVersionDO version) {
        return new ShowroomImagePromptVersion(
                version.getId(),
                version.getSceneCode(),
                version.getVersionNo(),
                normalizePromptTextContent(version.getTemplateText()),
                normalizePromptTextContent(version.getChangeNote()),
                parsePlaceholderCodes(version.getPlaceholderCodesJson()),
                Objects.requireNonNullElse(version.getUseCount(), 0),
                version.getCreateTime(),
                version.getCreator(),
                version.getLastUsedAt()
        );
    }

    static String normalizePromptTextContent(String value) {
        if (StrUtil.isBlank(value) || !looksLikeUtf8Mojibake(value)) {
            return value;
        }
        String normalizedByLine = normalizePromptTextLines(value);
        if (countHanCharacters(normalizedByLine) > countHanCharacters(value)) {
            return normalizedByLine;
        }
        return value;
    }

    private static boolean looksLikeUtf8Mojibake(String value) {
        return value.contains("ç")
                || value.contains("æ")
                || value.contains("å")
                || value.contains("ã€")
                || value.contains("ï¼")
                || value.contains("å…")
                || value.contains("äº")
                || value.contains("é£")
                || value.contains("åœ");
    }

    private static int countHanCharacters(String value) {
        if (value == null) {
            return 0;
        }
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (Character.UnicodeScript.of(value.charAt(index)) == Character.UnicodeScript.HAN) {
                count++;
            }
        }
        return count;
    }

    private static String tryRepairUtf8Mojibake(String value) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(value.length());
        for (int index = 0; index < value.length(); index++) {
            int mapped = reverseSingleByteMojibakeChar(value.charAt(index));
            if (mapped < 0) {
                return null;
            }
            buffer.write(mapped);
        }
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(buffer.toByteArray())).toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private static String normalizePromptTextLines(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        int lineStart = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\r' && current != '\n') {
                continue;
            }
            builder.append(normalizePromptTextSegment(value.substring(lineStart, index)));
            if (current == '\r' && index + 1 < value.length() && value.charAt(index + 1) == '\n') {
                builder.append("\r\n");
                index++;
            } else {
                builder.append(current);
            }
            lineStart = index + 1;
        }
        builder.append(normalizePromptTextSegment(value.substring(lineStart)));
        return builder.toString();
    }

    private static String normalizePromptTextSegment(String value) {
        if (StrUtil.isBlank(value) || !looksLikeUtf8Mojibake(value)) {
            return value;
        }
        String repaired = tryRepairUtf8Mojibake(value);
        if (repaired != null && countHanCharacters(repaired) > countHanCharacters(value)) {
            return repaired;
        }
        return value;
    }

    private static int reverseSingleByteMojibakeChar(char value) {
        if (value <= 0x00FF) {
            return value;
        }
        return switch (value) {
            case '€' -> 0x80;
            case '‚' -> 0x82;
            case 'ƒ' -> 0x83;
            case '„' -> 0x84;
            case '…' -> 0x85;
            case '†' -> 0x86;
            case '‡' -> 0x87;
            case 'ˆ' -> 0x88;
            case '‰' -> 0x89;
            case 'Š' -> 0x8A;
            case '‹' -> 0x8B;
            case 'Œ' -> 0x8C;
            case 'Ž' -> 0x8E;
            case '‘' -> 0x91;
            case '’' -> 0x92;
            case '“' -> 0x93;
            case '”' -> 0x94;
            case '•' -> 0x95;
            case '–' -> 0x96;
            case '—' -> 0x97;
            case '˜' -> 0x98;
            case '™' -> 0x99;
            case 'š' -> 0x9A;
            case '›' -> 0x9B;
            case 'œ' -> 0x9C;
            case 'ž' -> 0x9E;
            case 'Ÿ' -> 0x9F;
            default -> -1;
        };
    }

    private static List<String> parsePlaceholderCodes(String placeholderCodesJson) {
        if (StrUtil.isBlank(placeholderCodesJson)) {
            return List.of();
        }
        List<String> placeholderCodes = JsonUtils.parseArray(placeholderCodesJson, String.class);
        return placeholderCodes == null ? List.of() : List.copyOf(placeholderCodes);
    }
}
