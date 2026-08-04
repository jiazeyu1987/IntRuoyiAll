# DCC Upload Precheck Layout

## Task Goal

将 DCC 受控文件上传页的“提交前校验”区域从上传文件预览流下方移动到页面左侧表单信息区域下方，避免长文件预览导致校验内容需要向下滚动很久才能看到。

## Milestones

- [x] M1: 定位上传页面组件、现有布局结构和相邻静态契约。
- [x] M2: 先补充最小静态契约，证明旧布局会失败。
- [x] M3: 调整页面布局，保持上传、预览、提交和校验逻辑不变。
- [x] M4: 运行聚焦静态契约、类型检查或记录明确阻塞。
- [x] M5: 更新任务证据和收尾状态。

## Expected Verification

- `node tests/e2e/<task-owned-static-contract>.spec.js` 先 RED 后 GREEN。
- `pnpm ts:check` 通过，或记录非本任务引入的明确阻塞。
- 如本地前端入口、登录账号和测试数据可用，再用 Playwright 走真实上传页路径确认校验区在左侧表单信息下方，上传预览变长不影响校验区可见性。

## Current Status

ready_for_closeout

实现和聚焦静态验证已完成；最终 closeout/提交/推送仍受当前工作区既有未提交改动、分支已 ahead origin 以及非本任务 `ts:check` 历史类型错误阻塞。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，调整 DOM 布局归属，不通过隐藏、遮挡或滚动补丁规避。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`；适用门禁：前端静态契约隔离、Windows 换行与脚本行为同步、DCC 上传页相关前置只允许改布局归属，不得改上传/预览/API/权限链路。
