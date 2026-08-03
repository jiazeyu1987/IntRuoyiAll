# 20260803-dcc-upload-project-code-hint

## Task Goal

修复 DCC 受控文件上传页中 DHF/DMR 类别下“产品编号”提示固定显示红色错误的问题：未绑定项目代码时提示错误，已自动带出项目代码时显示成功确认，避免员工误以为已选择 DCC 项目仍校验失败。

## Milestones

- [x] M0: 建立任务记录、读取前置规则，并隔离任务前脏工作区基线。
- [ ] M1: 用静态回归测试复现当前红色提示固定显示的问题。
- [ ] M2: 最小修改上传页提示逻辑与样式，不改变提交和后端绑定链路。
- [ ] M3: 运行目标静态测试、相邻回归与必要前端检查。
- [ ] M4: 更新验证报告、收尾状态和提交记录。

## Expected Verification

- `node tests/e2e/dcc-upload-project-code-hint-static.spec.js`
- `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js`
- `pnpm ts:check`（如受现有历史问题阻塞，记录首个阻塞并保留目标静态合同结果）

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按 UI 状态区分未绑定错误与已绑定成功提示。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- 待读取 `docs/experience-index.md` 后补充适用门禁。

