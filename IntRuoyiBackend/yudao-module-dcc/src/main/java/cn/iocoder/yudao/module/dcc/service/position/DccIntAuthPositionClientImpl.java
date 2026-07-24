package cn.iocoder.yudao.module.dcc.service.position;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.service.category.DccIntAuthProperties;
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
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_CREATE_REQUEST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_CREATE_RESPONSE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_REQUEST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_POSITION_SYNC_RESPONSE_INVALID;

@Component
@RequiredArgsConstructor
public class DccIntAuthPositionClientImpl implements DccIntAuthPositionClient {

    private static final String POSITION_PATH = "/internal/quality-system/positions";

    private final RestTemplate restTemplate;
    private final DccIntAuthProperties properties;

    @Override
    public List<IntAuthPosition> listPositions() {
        properties.validatePositionSyncConfig();

        ResponseEntity<Map<String, Object>> response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("X-Internal-Token", properties.getInternalServiceToken().trim());
            response = restTemplate.exchange(buildUrl(POSITION_PATH), HttpMethod.GET,
                    new HttpEntity<>(headers), new ParameterizedTypeReference<>() {
                    });
        } catch (RuntimeException ex) {
            throw exception(INTAUTH_POSITION_SYNC_REQUEST_FAILED, ex.getMessage());
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw exception(INTAUTH_POSITION_SYNC_RESPONSE_INVALID);
        }
        Object positionsObject = body.get("positions");
        if (!(positionsObject instanceof List<?> rawPositions)) {
            throw exception(INTAUTH_POSITION_SYNC_RESPONSE_INVALID);
        }
        return rawPositions.stream().map(this::normalizePosition).toList();
    }

    @Override
    public IntAuthPosition createPosition(String name, String changeReason) {
        properties.validatePositionSyncConfig();

        ResponseEntity<Map<String, Object>> response;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", properties.getInternalServiceToken().trim());
            response = restTemplate.exchange(buildUrl(POSITION_PATH), HttpMethod.POST,
                    new HttpEntity<>(Map.of(
                            "name", name,
                            "change_reason", changeReason
                    ), headers), new ParameterizedTypeReference<>() {
                    });
        } catch (RuntimeException ex) {
            throw exception(INTAUTH_POSITION_CREATE_REQUEST_FAILED, ex.getMessage());
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw exception(INTAUTH_POSITION_CREATE_RESPONSE_INVALID);
        }
        return normalizePosition(body, INTAUTH_POSITION_CREATE_RESPONSE_INVALID);
    }

    private IntAuthPosition normalizePosition(Object source) {
        return normalizePosition(source, INTAUTH_POSITION_SYNC_RESPONSE_INVALID);
    }

    private IntAuthPosition normalizePosition(Object source,
                                              cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (!(source instanceof Map<?, ?> rawMap)) {
            throw exception(errorCode);
        }
        Long id = toLong(rawMap.get("id"));
        String name = StrUtil.trimToEmpty(rawMap.get("name") == null ? null : String.valueOf(rawMap.get("name")));
        if (!(rawMap.get("assigned_users") instanceof List<?>)
                || id == null || id <= 0 || StrUtil.isBlank(name)) {
            throw exception(errorCode);
        }
        return new IntAuthPosition(id, name);
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

}
