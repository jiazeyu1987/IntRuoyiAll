# DCC 预览不可用原因 E2E 验证

## Task Goal

通过真实前端 Playwright 路径验证 DCC 受控文件预览在接口返回 `previewUnavailableReason` 时，会展示明确不可预览原因，并且不会继续请求预览二进制流。

## Milestones

- [x] 建立任务记录并确认适用门禁
- [x] 确认本机前后端运行态与 Playwright 前置
- [x] 运行真实前端路径 E2E 验证
- [x] 回填验证报告
- [x] 收尾清理
- [ ] 提交并推送

## Expected Verification

- `npx --version`
- `Invoke-WebRequest http://127.0.0.1:48081/actuator/health`
- `Invoke-WebRequest http://127.0.0.1:8081/`
- Playwright 通过本机前端登录并进入 DCC 受控文件预览 viewer 页面。
- 验证 metadata 返回 `previewUnavailableReason` 后，页面展示不可预览原因，且目标预览二进制请求计数为 0。
- 必要时复跑 DCC 预览相关静态合同，确认代码层短路逻辑仍成立。

## Applicable Gates

- E2E 必须使用 Playwright 操作真实前端页面，API 仅可用于只读辅助或最终核验。
- 默认本机入口为 `http://127.0.0.1:8081`，后端健康检查为 `http://127.0.0.1:48081/actuator/health`。
- DCC 受控浏览预览验证需记录目标 viewer URL、目标链路错误、DCC 写请求计数和只读范围。
- Playwright 目标链路异常需和外部资源异常区分，不得用 API-only 或静态合同冒充真实 E2E 通过。
- PowerShell 命令不得使用 `&&`，中文任务文档必须按 UTF-8 读写。
- 任务提交前需记录 Git 状态、基线提交、当前任务提交与推送结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务只验证现有预览不可用短路行为，不新增 fallback。
- `是否从根因和长期维护角度解决`：是；验证目标是前端正式 viewer 对 metadata 不可用原因的根因短路链路。
- `是否存在临时补丁或绕过`：否；若运行态或真实路径前置缺失，将记录 blocker，不以 API-only 替代。

## Current Status

completed

- 已按项目规则先保存既有脏改动基线提交：`e44ae6ba6 chore: baseline docs before DCC preview E2E validation`。
- 本次 E2E 脚本和初始任务文档被并行基线提交 `0fada3212 chore: baseline current main workspace before upload policy fix` 带入历史，后续只选择性提交本任务收尾记录。
- 真实前端路径 E2E 已通过：7 类预览类型均显示 `previewUnavailableReason`，目标二进制预览请求数为 0。
- cleanup preview/apply 已通过，仅删除本任务临时 JSON 和截图，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 已创建本任务收尾提交 `4b5e47587 test: record DCC preview unavailable E2E validation`，并已通过 `git -c http.https://github.com.proxy= push origin int_main` 推送当前分支。
