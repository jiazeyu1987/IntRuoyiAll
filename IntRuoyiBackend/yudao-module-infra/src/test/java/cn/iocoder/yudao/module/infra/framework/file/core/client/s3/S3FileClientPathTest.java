package cn.iocoder.yudao.module.infra.framework.file.core.client.s3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S3FileClientPathTest {

    @Test
    void resolveObjectKeyForPresign_preservesRawObjectKeyPercentAndPlus() {
        S3FileClient client = newClient();
        String rawPath = "dcc/original/20260530/Pebax管5533+60%W 出厂检验报告.pdf";

        assertEquals(rawPath, client.resolveObjectKeyForPresign(rawPath));
    }

    @Test
    void resolveObjectKeyForPresign_decodesFullUrlPathAndRemovesQuery() {
        S3FileClient client = newClient();
        String fullUrl = "http://minio:9000/yudao/dcc/original/Pebax%E7%AE%A15533+60%25W%20%E5%87%BA%E5%8E%82.pdf?X-Amz-Signature=abc";

        assertEquals("dcc/original/Pebax管5533+60%W 出厂.pdf",
                client.resolveObjectKeyForPresign(fullUrl));
    }

    private static S3FileClient newClient() {
        S3FileClientConfig config = new S3FileClientConfig();
        config.setDomain("http://minio:9000/yudao");
        config.setBucket("yudao");
        return new S3FileClient(1L, config);
    }

}
