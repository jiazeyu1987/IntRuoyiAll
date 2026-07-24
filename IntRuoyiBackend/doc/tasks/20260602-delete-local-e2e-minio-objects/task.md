# 任务：清理本机 yudao-dcc-e2e 测试对象

## Task Goal

按用户明确授权，清理本机 MinIO 中 `yudao-dcc-e2e/**` 下的隔离测试对象，包括 `showroom/**` 与该 bucket 下其他对象，并删除 `yudao-dcc-e2e` bucket 本身；不修改 `yudao/**`、`infra_file_config.id=28`、数据库记录、服务器或远程环境。

## 前置检查

- 上一个后端任务 `doc/tasks/20260602-showroom-version-center-auxiliary-blockers/task.md` 已标记 `completed`。
- 用户明确说明：`yudao-dcc-e2e/showroom/**` 和 `yudao-dcc-e2e` 下其他对象都可以删除。
- 用户后续明确补充：`bucket` 也删了。
- 本任务默认仅操作本机 MinIO 容器 `docker-minio-1`，不操作测试服或正式服。

## BDD Scenarios

BDD: 本机 E2E 测试桶对象可整体清理 -> Given 本机 MinIO 存在 `yudao-dcc-e2e/**` 测试对象 / When 执行本次清理 / Then `yudao-dcc-e2e` bucket 保留但对象数量为 0，且 `yudao` bucket 不被删除或修改。

BDD: 本机 E2E 测试桶可删除 -> Given 用户明确要求删除 `yudao-dcc-e2e` bucket / When bucket 中不存在任何当前对象、历史版本和 delete markers / Then S3 `delete_bucket` 应成功，且默认业务 bucket `yudao` 仍存在。

## Milestones

- [x] M1: 建立任务文档并确认上一任务已完成。
- [x] M2: 通过对象存储 API 清点 `yudao-dcc-e2e` 当前对象。
- [x] M3: 删除 `yudao-dcc-e2e/**` 当前对象并保留 bucket。
- [x] M4: 验证 `yudao-dcc-e2e` 当前对象数量为 0，`yudao` bucket 仍存在。
- [ ] M5: 永久删除 `yudao-dcc-e2e` 历史版本和 delete markers。
- [ ] M6: 删除 `yudao-dcc-e2e` bucket 本身。
- [x] M7: 运行 task-closeout-cleanup 预览；阻塞解除且验证通过后再提交本任务文档。

## Expected Verification

- RED: 清理前列出 `yudao-dcc-e2e` 对象数量 -> FAIL，存在待清理对象。
- GREEN: 清理后列出 `yudao-dcc-e2e` 对象数量 -> PASS，数量为 0。
- REGRESSION: 列出 `yudao` bucket -> PASS，默认业务 bucket 仍存在。
- BLOCKER: 永久删除历史版本 -> 仅在不存在 WORM Object Lock 阻塞时可通过。
- BLOCKER: 删除 `yudao-dcc-e2e` bucket -> 仅在 bucket 不包含任何当前对象、历史版本和 delete markers 时可通过。
- CLOSEOUT: `task-closeout-cleanup --mode preview` -> PASS。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。对象 API 或凭据缺失时直接阻塞。
- `是否从根因和长期维护角度解决`：是。本任务只清理隔离 E2E bucket，不触碰默认受保护媒体链路。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

## Completed Work

- 已创建任务文档，明确清理边界、BDD 场景和验收方式。
- 已通过本机 MinIO S3 API 确认 `yudao-dcc-e2e` 清理前存在 1174 个当前对象，总大小 1529549927 bytes。
- 已删除 `yudao-dcc-e2e/**` 当前对象；清理后普通对象列表数量为 0。
- 已确认默认业务 bucket `yudao` 仍存在。

## Verification Evidence

- RED: `yudao-dcc-e2e` 清理前当前对象数量为 1174。
- GREEN: `yudao-dcc-e2e` 清理后当前对象数量为 0。
- REGRESSION: 默认业务 bucket `yudao` 仍存在。
- BLOCKER: `yudao-dcc-e2e` 开启 versioning；永久版本清理时，MinIO 对 `codex-object-lock-test.txt` 版本 `0226b192-1fe9-4d41-a2aa-54b05eb66cc6` 返回 `InvalidRequest: Object is WORM protected and cannot be overwritten`。该版本为 `COMPLIANCE` 模式，保留至 `2026-06-08T20:03:36Z`，且 legal hold 为 `ON`。
- BLOCKER: 当前仍有 10629 个历史版本和 9956 个 delete markers；不得把“当前对象列表为 0”误报为永久版本清理完成。
- BLOCKER: `delete_bucket(Bucket='yudao-dcc-e2e')` 返回 `BucketNotEmpty: The bucket you tried to delete is not empty. You must delete all versions in the bucket.`，bucket 删除被历史版本/delete markers 阻塞。
- CLOSEOUT: `task-closeout-cleanup --mode preview` 返回 `status: ready`，仅保留 `task.md` 与 `execution-log.md`，无 delete/blocked/warnings。

## Blockers

- 永久删除 `yudao-dcc-e2e/**` 历史版本被 Object Lock 阻塞。必须等待 `COMPLIANCE` 保留到期，并由用户明确批准处理 legal hold 后再继续永久清理。
- 删除 `yudao-dcc-e2e` bucket 本身被同一批历史版本/delete markers 阻塞。MinIO 要求先删除所有版本，才能删除 bucket。
- 因任务仍为 `blocked`，本次不提交任务文档。
