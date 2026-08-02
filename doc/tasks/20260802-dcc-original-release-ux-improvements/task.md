# 20260802 DCC 原版发布前端体验优化

## Task Goal

修复 DCC 文控“原版发布”链路中的前端操作体验问题：上传预览错误可解释、文件编号唯一性提前校验、审批详情处理态清晰、签名弹窗业务提示增强、审批进度可视化、发布后可跳转受控浏览、当前有效版标识突出、分类/项目代码/文件类型权限前置提示、审批待办展示 DCC 关键信息。

## Milestones

- [x] 建立任务文档、BDD 场景和前端证据契约
- [x] 定位 DCC 上传页、详情页、审批中心和受控浏览相关组件/API
- [x] 先写聚焦静态合同并跑出 RED
- [x] 实现最小前端修复，不改变后端审批/签名状态流转
- [x] 跑静态合同、类型/相邻回归
- [x] 输出 verification-report.md 与 frontend-feature-evidence.md

## Expected Verification

- 聚焦静态合同覆盖用户列出的 9 项 UX 行为。
- 不使用 API/SQL 修改 DCC 文件状态、审批状态或签名证据。
- 不使用 admin 账号执行业务路径。
- 若真实 Playwright 验证需要登录密码，必须仅通过环境变量注入，不写入日志或报告。
- 不顺手修其它 DCC 场景；若发现入口、权限、运行态或测试数据缺失，记录 BLOCKED 和影响。

## Experience Gate Summary

- DCC 文控审批必须证明真实页面处理态，不得用只读 viewer、API-only 或 BPM 原生行替代 DCC 审批入口。
- 上传页必须按正式类别权限投影过滤不可上传类别，后端仍保留 fail-fast 校验。
- 上传预览依赖本机 MinIO；若 object storage 不可用，真实 E2E 必须 BLOCKED，不能把“系统异常”当业务成功。
- 前端失败结果必须展示真实原因，不能只依赖短暂 toast 或通用“系统异常”。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是把正式错误、权限、状态和处理态在页面显式暴露。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Final Verification Summary

- PASS: 聚焦静态合同覆盖用户列出的 9 项 UX 行为。
- PASS: 相邻 DCC 上传、详情、审批中心和受控浏览静态合同通过。
- PASS: `pnpm ts:check` 通过。
- NOT RUN: 未运行真实审批/签名写入型 Playwright E2E；本任务只修前端 UX，不触发业务状态流转写入。
- BLOCKED CLOSEOUT: 当前工作区已有大量非本任务脏改动且分支 ahead；cleanup apply 已无删除项通过，未执行基线提交、实现提交或 push。

## Cleanup Keep

- doc/tasks/20260802-dcc-original-release-ux-improvements/frontend-feature-evidence.md

