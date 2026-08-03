# DCC 受控浏览行操作按钮文案精简

## Task Goal

- 将截图红框内 DCC 受控浏览列表每行 4 个操作按钮文案依次改为：预览、追溯、签核、下载。
- 保留既有按钮 handler、权限、下载状态和受控打印/更多操作逻辑，仅变更用户可见文案。

## Milestones

- [x] M0: 建立任务文档并记录适用规则。
- [ ] M1: 更新静态契约并跑出 RED，证明旧长文案不符合新要求。
- [ ] M2: 最小修改 DCC 受控浏览页面按钮文案。
- [ ] M3: 运行聚焦静态契约、旧文案扫描、类型检查和 diff 检查。
- [ ] M4: 完成验证报告与收尾状态更新。

## Expected Verification

- `node tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`
- `rg -n "预览当前有效版|查看版本追溯|查看签核证据" src/views/dcc/controlled-file/browser tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`
- `pnpm ts:check`
- `git diff --check`

## Applicable Gates

- 前端截图按钮统一静态契约门禁：截图命中红框按钮，必须先更新静态契约 RED，再按既有 handler 最小改可见文案，不得改路由、权限、状态或接口。
- DCC 受控浏览当前有效版与权限隔离门禁：本任务只改文案，不改当前有效版、viewer、traceability、下载或权限隔离链路；真实 E2E 若缺账号/运行态则记录阻塞，不用 API-only 冒充页面验证。
- Windows 换行与脚本行为同步：修改 `tests/e2e/*static.spec.js` 时按 CRLF/LF 归一化读取，负向断言必须限定目标页面文案。

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接更新正式按钮文案与静态契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
