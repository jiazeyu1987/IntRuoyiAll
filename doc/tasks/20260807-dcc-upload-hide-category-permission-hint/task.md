# DCC 上传页隐藏文件类别辅助提示

## Task Goal

在 DCC 受控文件上传页的只读“文件类别”区域中，不再显示用户截图红框内的“自动取文件分类最后一级”路径说明和橙色权限预检提示，同时保留文件类别值。本任务不以展示隐藏结果推断或决定类别权限应在哪个业务阶段校验。

## Milestones

- [x] M1: 定位截图对应页面、模板分支、权限逻辑和相邻测试。
- [x] M2: 先补充静态合同并取得预期 RED。
- [x] M3: 删除红框内两处展示，保持正式权限链路不变。
- [x] M4: 运行聚焦合同、相邻回归、类型检查和差异检查。
- [ ] M5: 完成证据归档、经验沉淀、cleanup、提交和推送。

## Expected Verification

- `node tests/e2e/dcc-upload-category-permission-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> PASS。
- `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS，或记录可证明与本任务无关的既有阻塞。
- `git diff --check -- <task-owned-paths>` -> PASS。
- 若真实页面、登录账号和只读候选数据可用，使用 Playwright 验证文件类别值可见且红框辅助提示不显示。

## Current Status

ready_for_closeout

用户要求的展示行为、聚焦静态合同和相邻分类合同已通过；实现已被共享分支并发基线提交并推送。最终只读 Playwright 复跑受当前登录链路超时阻塞，等待重新 cleanup 和任务记录提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接移除不需要的展示节点，不改变权限数据源、校验或 API 契约。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`。
- 适用门禁：`docs/frontend-development.md#DCC 上传类别权限投影门禁`。本任务只改变只读类别区域的辅助展示；权限职责由独立任务 `20260807-dcc-upload-permission-at-approval` 的明确用户要求和测试负责，不用本任务的 DOM 结果证明。
- 适用通用门禁：前端静态契约隔离、共享分支并发基线提交、同文件并行改动选择性暂存。

## Cleanup Candidates

- output/playwright/20260807-dcc-upload-hide-category-permission-hint/
