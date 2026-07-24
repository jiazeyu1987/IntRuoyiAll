# 任务：发布测试服最新 DCC viewer token 后端

## 任务目标

将本机当前 `ruoyi-vue-pro` 中包含 DCC viewer token 固定密钥临时修复的最新后端代码发布到测试服 `172.30.30.58`，确认运行中的 backend 镜像与当前本地改动一致，不再停留在旧版本。

## 上一任务检查

- 上一个后端任务 `20260602-dcc-viewer-token-config-missing` 已标记 `completed`。
- 本次任务基于用户在当前对话中的明确授权执行测试服发布与验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只负责把已存在的用户批准临时方案发布到测试服，不新增新的降级路径。
- `是否从根因和长期维护角度解决`：否。本任务是环境发布，不改变“固定密钥为临时方案”的性质；正式长期方案仍应恢复显式运行时配置并移除硬编码密钥。
- `是否存在临时补丁或绕过`：是。发布内容包含上一任务中用户批准的固定 viewer token 密钥；本任务本身采用 code-only 发布，不同步数据库或 MinIO 数据。

## BDD 场景

BDD: 测试服 backend 切换到当前最新代码 -> Given 本机 `ruoyi-vue-pro` 已包含 DCC viewer token 固定密钥修复且本地测试通过 / When 执行统一发布脚本向测试服进行 code-only 发布 / Then 测试服 backend 镜像标签、运行容器和健康检查应更新到本次发布版本。

BDD: 发布后旧 viewer token 配置缺失错误不应继续来自旧镜像 -> Given 测试服旧 backend 镜像早于本次本地修复 / When 发布完成并重启 backend 容器 / Then 测试服运行中的 backend 不应继续停留在发布前镜像。

## 里程碑

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：核对统一发布脚本、测试服目标和本地待发布改动。
- [x] M3：执行测试服 code-only 发布。
- [x] M4：验证测试服 backend 镜像/容器/健康检查已更新到本次发布版本。
- [x] M5：更新任务记录并给出后续验证结论。

## 预期验证

- 发布命令成功：`powershell -ExecutionPolicy Bypass -File script\deploy\publish-int-ruoyi.ps1 -Environment test -SkipDatabaseSync -SkipMinioSync -Tag <tag>`
- 测试服 `http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`。
- 测试服 `docker compose ps` 或等价只读核对显示 `intruoyi-backend` 已重建到本次发布镜像标签。

## 当前状态

completed

## 最终结果

测试服 `172.30.30.58` 已切换到本次发布版本 `20260602_dcc_viewer_token_latest_f9f50cf157`，运行中的 backend 镜像为 `intruoyi-backend:20260602_dcc_viewer_token_latest_f9f50cf157`。统一发布脚本执行的是 code-only 路径，但按脚本既有契约仍会重放目标绑定 post-import SQL 与 required SQL，未执行数据库 dump/import，也未执行 MinIO mirror。

发布过程中先暴露了本机前端依赖目录损坏问题：`pnpm build:test` 缺少 `@babel/helper-validator-identifier`。该问题已通过 `pnpm install --force` 修复，随后重跑发布成功。

## 最终验证结果

- `show-int-ruoyi-test-status.bat` -> PASS，Current release package=`20260602_dcc_viewer_token_latest_f9f50cf157`。
- `ssh root@172.30.30.58 "docker ps --format '{{.Names}} {{.Image}} {{.Status}}' | grep intruoyi-backend"` -> PASS，运行镜像=`intruoyi-backend:20260602_dcc_viewer_token_latest_f9f50cf157`。
- `curl http://172.30.30.58:48081/actuator/health` -> PASS，返回 `{"status":"UP"}`。
