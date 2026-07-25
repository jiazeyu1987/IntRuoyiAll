# 20260725 Restart Local Frontend Backend

## Task Goal

重启 `E:\IntRuoyi` 当前 `int_main` 本地前后端程序，保持端口契约：前端 `8081`，后端 `48081`。

## Milestones

- [x] 读取本地运行、端口与任务收尾规则
- [x] 记录端口归属并停止确认属于本项目的旧进程
- [x] 启动后端并验证 `/actuator/health`
- [x] 启动前端并验证本地入口
- [x] 记录验证证据与最终状态

## Expected Verification

- `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- 端口占用进程可归属到 `E:\IntRuoyi` 当前工作区。

## Current Status

ready_for_closeout

## 经验门禁

### 本地重启脚本路径门禁

- Trigger: 本地重启、`restart-int-ruoyi-local`、`IntRuoyiFronted`。
- Preflight check: 确认前端根目录为 `E:\IntRuoyi\IntRuoyiFronted`。
- Blocker: 如果脚本或命令解析到缺失/错误前端目录，停止重启，不创建假目录、不换端口。
- Verification: 记录端口归属、后端健康检查与前端 HTTP 状态。
- Forbidden action: 禁止创建 `yudao-ui-admin-vue3` 假目录、改端口、强杀未知进程或跳过前端验证。
- Evidence: `docs/local-runtime.md`。

### 本地后端数据库凭据门禁

- Trigger: 启动 `int_main` 本地后端、`48081` 健康检查。
- Preflight check: 后端必须继续使用正式本地数据源配置，不改端口、不切换数据源。
- Blocker: 数据库拒绝连接、`master` 数据源创建失败或 health 非 `UP` 时，不声明后端成功。
- Verification: 记录 `48081` PID、命令行归属与 `/actuator/health` 状态。
- Forbidden action: 禁止临时改凭据、切换 mock/空数据源、只启动前端就宣称完成。
- Evidence: `docs/local-runtime.md`。

## Cleanup Keep

- `doc/tasks/20260725-restart-local-frontend-backend/backend-20260725-150714.out.log`
- `doc/tasks/20260725-restart-local-frontend-backend/backend-20260725-150714.err.log`
- `doc/tasks/20260725-restart-local-frontend-backend/frontend-20260725-150714.out.log`
- `doc/tasks/20260725-restart-local-frontend-backend/frontend-20260725-150714.err.log`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅执行本地运行态重启，不改动源码或配置。
- `是否存在临时补丁或绕过`：否。