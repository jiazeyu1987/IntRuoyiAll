# DCC 本机对象存储前置恢复验证报告

## Scope

本任务仅恢复 DCC 原版发布 E2E 所需的本机对象存储/MinIO 前置，不修改 DCC 业务代码、不切换存储实现、不 mock 上传成功、不用 API-only 或 SQL 改状态冒充业务链路通过。

## Current Result

PASS

## Verification

- MinIO/Docker：`docker-minio-1` 已运行并处于 healthy，`http://127.0.0.1:9000/minio/health/ready` 返回 HTTP 200，`yudao` bucket 目录存在。
- 后端运行态：`restart-int-ruoyi-local.ps1 -Component backend` 已恢复本机 `int_main` 后端，`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- 前端运行态：`http://127.0.0.1:8081/` 返回 HTTP 200。
- 真实页面验证：使用非 admin 上传人 `pengyunfeng` 登录真实前端，进入 `/dcc/controlled-file/upload`，选择 DCC 上传字段和本地样本 `resource\批记录节点-解析样本.docx`，真实触发 `POST /admin-api/dcc/controlled-files/upload-preview`。
- 验证结果：`upload-preview-result.json` 显示 `status=PASS`，runId=`20260802084900`，fileNumber=`CODX-DCC-MINIO-20260802084900`，previewKind=`OFFICE`，`targetNetworkFailures=[]`，`consoleErrors=[]`，`pageErrors=[]`。
- 后端日志：最新 `CODX-DCC-MINIO-20260802084900` 对应的 `current-version` 和 `upload-preview` 均正常完成，未再出现对象存储连接拒绝。
- 安全边界：未使用 admin，未 API-only/SQL 改状态，未 mock 上传成功，未提交上传审批或发布链路；密码仅通过进程环境表达式注入，未写入本报告。

## Blockers

- 无当前 blocker。可以继续原版发布完整 E2E，但应从新文件编号重新开始上传，不复用本次仅用于 MinIO 前置验证的临时预览记录。
