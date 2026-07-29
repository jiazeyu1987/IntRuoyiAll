# 20260729 eDHR 填写页软键盘按钮

## Task Goal

在 eDHR 批记录填写页左侧红框位置新增软键盘图标按钮，点击后在页面内弹出软键盘，不改动后端接口、不引入降级或 mock。

## Milestones

- [x] 建立任务目录并读取前端、E2E、PowerShell、收尾和经验门禁
- [x] 保存任务前脏工作区基线，避免本次改动混入既有变更
- [ ] 新增聚焦静态合同，先 RED 锁定软键盘入口、弹层和按键行为
- [ ] 实现 eDHR 填写页左侧软键盘按钮和页面内软键盘弹层
- [ ] 运行目标静态合同、TypeScript 检查和证据校验
- [ ] 更新任务文档、收尾清理并完成提交推送

## Expected Verification

- `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-soft-keyboard-button/frontend-feature-evidence.md`
- `git diff --check`

## Current Status

in_progress

## Applicable Gates

- 前端功能必须按 BDD/TDD 记录 RED/GREEN，不允许通过 mock、默认成功或隐藏错误代替真实页面行为。
- eDHR 填写页相关改动需要保留原有填写模式、原表模式、保存草稿、提交执行和全屏按钮链路。
- 当前任务命中同文件并行改动和提交后残余改动门禁：本次提交必须只包含软键盘按钮/弹层/合同/任务文档。
- 本次只在页面内增加软键盘辅助输入 UI，不修改 `assistRows`、批记录表单、表单槽位、工序开始或后端数据来源。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接在 eDHR 填写页左侧工具栏提供正式软键盘入口和可测交互。
- `是否存在临时补丁或绕过`：否。

