package cn.iocoder.yudao.module.dcc.service.category;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_REQUEST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class DccIntAuthFileCategoryClientImpl implements DccIntAuthFileCategoryClient {

    private static final String FILE_CATEGORY_PATH = "/internal/quality-system/file-categories";

    private final RestTemplate restTemplate;
    private final DccIntAuthProperties properties;

    @Override
    public List<IntAuthFileCategory> listFileCategories() {
        properties.validateFileCategorySyncConfig();

        ResponseEntity<Map<String, Object>> response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-Internal-Token", properties.getInternalServiceToken().trim());
            response = restTemplate.exchange(buildUrl(FILE_CATEGORY_PATH), HttpMethod.GET,
                    new HttpEntity<>(headers), new ParameterizedTypeReference<>() {
                    });
        } catch (RuntimeException ex) {
            throw exception(INTAUTH_FILE_CATEGORY_SYNC_REQUEST_FAILED, ex.getMessage());
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw exception(INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID);
        }
        Object fileCategoriesObject = body.get("file_categories");
        if (!(fileCategoriesObject instanceof List<?> rawCategories)) {
            throw exception(INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID);
        }
        return rawCategories.stream().map(this::normalizeCategory).toList();
    }

    private IntAuthFileCategory normalizeCategory(Object source) {
        if (!(source instanceof Map<?, ?> rawMap)) {
            throw exception(INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID);
        }
        Long id = toLong(rawMap.get("id"));
        String name = StrUtil.trimToEmpty(rawMap.get("name") == null ? null : String.valueOf(rawMap.get("name")));
        Boolean seededFromJson = toBoolean(rawMap.get("seeded_from_json"));
        Boolean active = toBoolean(rawMap.get("is_active"));
        if (id == null || id <= 0 || StrUtil.isBlank(name) || seededFromJson == null || active == null) {
            throw exception(INTAUTH_FILE_CATEGORY_SYNC_RESPONSE_INVALID);
        }
        return new IntAuthFileCategory(id, name, seededFromJson, active);
    }

    private String buildUrl(String path) {
        return StrUtil.removeSuffix(properties.getBaseUrl().trim(), "/") + path;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return null;
    }

}
