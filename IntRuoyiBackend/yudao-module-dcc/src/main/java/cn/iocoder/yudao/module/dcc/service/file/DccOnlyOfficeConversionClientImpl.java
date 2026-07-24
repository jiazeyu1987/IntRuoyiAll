package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PDF_CONVERSION_FAILED;

@Service
public class DccOnlyOfficeConversionClientImpl implements DccOnlyOfficeConversionClient {

    private static final int CONVERSION_TIMEOUT_MILLIS = 300_000;

    @Override
    public byte[] convertToPdf(DccOnlyOfficeConversionCommand command) {
        requireCommand(command);
        Map<String, Object> conversionPayload = new LinkedHashMap<>();
        conversionPayload.put("async", false);
        conversionPayload.put("filetype", command.fileType());
        conversionPayload.put("key", command.key());
        conversionPayload.put("outputtype", "pdf");
        conversionPayload.put("title", command.title());
        conversionPayload.put("url", command.documentUrl());
        String jwt = createJwt(conversionPayload, command.jwtSecret());
        Map<String, Object> requestBody = new LinkedHashMap<>(conversionPayload);
        requestBody.put("token", jwt);
        try (HttpResponse response = HttpRequest.post(command.converterUrl())
                .header("Accept", "application/json")
                .contentType("application/json")
                .timeout(CONVERSION_TIMEOUT_MILLIS)
                .body(JsonUtils.toJsonString(requestBody))
                .execute()) {
            if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
                throw conversionFailed("OnlyOffice converter HTTP status " + response.getStatus());
            }
            DccOnlyOfficeConversionResponse conversionResponse =
                    JsonUtils.parseObject(response.body(), DccOnlyOfficeConversionResponse.class);
            if (conversionResponse == null) {
                throw conversionFailed("OnlyOffice converter returned an empty response");
            }
            if (conversionResponse.getError() != null) {
                throw conversionFailed("OnlyOffice converter error " + conversionResponse.getError());
            }
            if (!Boolean.TRUE.equals(conversionResponse.getEndConvert())) {
                throw conversionFailed("OnlyOffice converter did not finish synchronously");
            }
            if (!"pdf".equalsIgnoreCase(StrUtil.trimToEmpty(conversionResponse.getFileType()))
                    || StrUtil.isBlank(conversionResponse.getFileUrl())) {
                throw conversionFailed("OnlyOffice converter returned a non-PDF result");
            }
            return downloadConvertedPdf(conversionResponse.getFileUrl());
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw conversionFailed(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private void requireCommand(DccOnlyOfficeConversionCommand command) {
        if (command == null
                || StrUtil.isBlank(command.converterUrl())
                || StrUtil.isBlank(command.jwtSecret())
                || StrUtil.isBlank(command.fileType())
                || StrUtil.isBlank(command.key())
                || StrUtil.isBlank(command.title())
                || StrUtil.isBlank(command.documentUrl())) {
            throw conversionFailed("OnlyOffice conversion command is incomplete");
        }
    }

    private byte[] downloadConvertedPdf(String fileUrl) {
        try (HttpResponse response = HttpRequest.get(fileUrl)
                .timeout(CONVERSION_TIMEOUT_MILLIS)
                .execute()) {
            if (response.getStatus() < HttpStatus.HTTP_OK || response.getStatus() >= HttpStatus.HTTP_MULT_CHOICE) {
                throw conversionFailed("OnlyOffice converted file HTTP status " + response.getStatus());
            }
            byte[] body = response.bodyBytes();
            if (body == null || body.length == 0) {
                throw conversionFailed("OnlyOffice converted file is empty");
            }
            return body;
        }
    }

    private String createJwt(Map<String, Object> payload, String jwtSecret) {
        String header = base64Url(JsonUtils.toJsonString(Map.of("alg", "HS256", "typ", "JWT")));
        String body = base64Url(JsonUtils.toJsonString(payload));
        String signingInput = header + "." + body;
        return signingInput + "." + sign(signingInput, jwtSecret);
    }

    private String sign(String payload, String jwtSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw conversionFailed("OnlyOffice conversion token signing failed");
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private ServiceException conversionFailed(String reason) {
        return new ServiceException(CONTROLLED_FILE_PDF_CONVERSION_FAILED.getCode(),
                StrUtil.blankToDefault(reason, "OnlyOffice conversion failed"));
    }

    @Data
    public static class DccOnlyOfficeConversionResponse {
        private Boolean endConvert;
        private Integer error;
        private String fileType;
        private String fileUrl;
        private Integer percent;
    }
}
