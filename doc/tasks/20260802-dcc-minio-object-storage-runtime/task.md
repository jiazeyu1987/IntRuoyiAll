# DCC 本机对象存储前置恢复

## Task Goal

恢复 DCC 原版发布真实 E2E 所需的本机对象存储/MinIO 前置，使 `upload-preview` 能通过真实页面完成文件上传预览。

## Milestones

1. `completed` - 创建任务目录并记录适用运行态/E2E/收尾门禁。
2. `completed` - 核查本机文件服务配置来源、端口监听、Docker 容器与标准启动方式。
3. `completed` - 按项目已有方式恢复本机对象存储，不修改 DCC 业务代码、不切换存储实现、不 mock 上传成功。
4. `completed` - 验证 `127.0.0.1:9000` 监听和真实页面 `upload-preview` 可继续。
5. `completed` - 更新验证报告和后续 E2E 继续条件。

## Expected Verification

- 配置来源核查：后端本机 profile 的文件服务/S3/MinIO endpoint、bucket 来源已确认且不输出密钥。
- 环境核查：Docker 容器/端口监听证据明确。
- 恢复验证：`127.0.0.1:9000` 监听，且 DCC 上传页真实 `upload-preview` 不再因连接对象存储失败返回业务 500。
- 不使用 API-only、SQL 改状态、mock 上传、切换存储实现或修改 DCC 业务代码。

## Applicable Gates

- `docs/local-runtime.md`：本机运行态、Docker 依赖和端口 fail-fast。
- `docs/e2e-rules.md`：真实上传链路必须走 Playwright 页面，API 仅可用于后置/只读辅助核验。
- `docs/task-closeout-rules.md`：任务文档、验证报告和收尾状态要求。
- `docs/powershell-encoding.md`：中文任务文档和命令输出使用 UTF-8，不记录密钥。
- `docs/experience-index.md`：MinIO/对象存储经验仅命中备份/发布长期门禁，本任务只恢复本机开发前置。

## Current Status

ready_for_closeout

## Cleanup Keep

doc/tasks/20260802-dcc-minio-object-storage-runtime/verify-upload-preview.cjs
doc/tasks/20260802-dcc-minio-object-storage-runtime/upload-preview-result.json

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是恢复正式本机对象存储前置。
- `是否存在临时补丁或绕过`：否。
