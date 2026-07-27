# 表单模板三按钮交互对齐

## Task Goal

将表单模板的“打开 / 编辑 / 填写”交互行为与批记录管理对齐，同时保持两个领域的数据完全独立：

- 打开：在当前页面进入模板查看工作区。
- 编辑：在当前页面进入模板编辑工作区。
- 填写：进入独立的模板模拟填写页面。
- 所有动作只使用 `templateId`、模板版本和 `jimuSchemaJson` 等表单模板上下文，不引入批记录 `reportId` 或绑定状态。

## Milestones

1. [x] 确认两套页面的现有交互差异及可复用边界。
2. [x] 以聚焦静态合同记录 BDD，并完成 RED。
3. [x] 实现表单模板三按钮页面流转对齐。
4. [x] 完成聚焦测试、类型检查和真实页面路径验证。
5. [ ] 更新任务证据并完成收尾。

## Expected Verification

- `node tests/e2e/form-template-button-interaction-parity-static.spec.js`
- `node tests/e2e/form-template-independent-button-actions-static.spec.js`
- `node tests/e2e/form-center-static.spec.js`
- `pnpm ts:check`
- Playwright 从真实菜单路径 `/mdm/form-center/template` 验证三个按钮的 URL、工作区和数据领域边界。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；对齐交互壳层，不混用批记录数据契约。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 表单模板与批记录表单没有直接数据关系；交互相似不得解释为共享 `reportId`。
- 三按钮必须只使用当前模板上下文；`打开/编辑`使用 `mode=designer` 的 FormCenter 专属包装页，`填写`使用独立模拟填写页面。
- 三个页面级动作都必须按 `templateId + versionNo` 精确读取当前模板版本，普通模板不得因未绑定批记录而不可操作。
- 既有宽合同若先失败于无关断言，必须新增任务专用最小合同完成 RED/GREEN，不修改无关产品逻辑。
- 真实页面验证必须使用 Playwright；不得用 API-only 或 mock 代替。

## Scope

- `IntRuoyiFronted/src/views/form-center/template/`
- 必要的表单中心路由配置和任务专用前端测试。
- 表单中心按 `templateId + versionNo` 精确查询模板版本的只读后端接口及回归测试。
- 与本次规则直接相关的 `docs/frontend-development.md`、`docs/experience-index.md`。

## Out Of Scope

- MES 批记录数据绑定。
- 批记录 `reportId`、`BOUND` 状态和批记录模板模拟 API。
- 后端数据库、菜单权限、批记录管理页面行为修改。

## Cleanup Keep

- doc/tasks/20260727-form-template-button-interaction-parity/bug-regression-evidence.md
