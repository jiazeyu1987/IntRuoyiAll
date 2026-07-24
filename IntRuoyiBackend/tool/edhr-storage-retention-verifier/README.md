# eDHR Storage Retention Verifier

这个工具用于真实验证 S3 Object Lock、Retention 与 legal hold 证据。它不会 mock、skip 或在缺少前置条件时返回成功。

## 运行方式

```powershell
$env:PYTHONUTF8='1'
python -X utf8 tool\edhr-storage-retention-verifier\verify.py
```

## 必需环境变量

- `EDHR_S3_ENDPOINT`
- `EDHR_S3_BUCKET`
- `EDHR_S3_REGION`
- `EDHR_S3_ACCESS_KEY`
- `EDHR_S3_SECRET_KEY`
- `EDHR_S3_RETENTION_MODE`: `GOVERNANCE` 或 `COMPLIANCE`
- `EDHR_S3_RETAIN_UNTIL_DAYS`: 正整数
- `EDHR_S3_REQUIRE_LEGAL_HOLD`: `true` 或 `false`

脚本不会输出 access key、secret key 或 presigned URL。

## 验证内容

1. 必需环境变量完整。
2. `boto3` 与 `botocore` 可用。
3. bucket versioning 为 `Enabled`。
4. bucket `ObjectLockEnabled` 为 `Enabled`。
5. 上传测试对象到 `edhr-retention-verifier/<UTC timestamp>-<uuid>.txt`，并写入 `ObjectLockMode`、`ObjectLockRetainUntilDate`，在要求 legal hold 时写入 `ObjectLockLegalHoldStatus='ON'`。
6. 读取上传返回的 `VersionId`，缺失即 `FAIL`。
7. 使用 `get_object_retention` 与 `get_object_legal_hold` 读取同一 version 的真实证据并校验策略。
8. 对同一 `VersionId` 尝试 `delete_object`；如果删除成功则 `FAIL`，如果被拒绝则继续。
9. 再次使用 `get_object` 读取同一 `VersionId`，证明受保护版本仍可读取。

## 输出与退出码

输出为 JSON，包含 `status`、`bucket`、`key`、`versionId`、`retentionMode`、`retainUntil`、`legalHoldStatus` 与 `checks`。

- `PASS`: 退出码 `0`
- `FAIL`: 退出码 `1`
- `BLOCKED`: 退出码 `2`

缺少环境变量、Python 依赖、bucket versioning/Object Lock 配置、S3 权限或真实 API 可达性时，结果为 `BLOCKED`，并在 `missingPrerequisites` 中列出阻塞项。
