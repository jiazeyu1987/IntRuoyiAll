# 任务：文控类别生命周期阶段筛选与表单下拉

- Task ID: `20260702-dcc-category-lifecycle-stage`
- Created: 2026-07-02
- Current Status: completed

## 任务目标

在文控中心文控权限“类别列表”页签中新增阶段列和阶段下拉筛选；新增/编辑类别时新增阶段必填下拉，只能选择固定 6 个阶段，不能手动输入。

## 里程碑

1. 建立任务台账、经验门禁、BDD/TDD 证据。completed
2. 补前端静态 RED 测试，固化阶段列、筛选、表单下拉契约。completed
3. 实现 API 类型、共享阶段选项、列表列/筛选与表单保存。completed
4. 运行前端静态测试与类型检查。completed
5. 真实 E2E 验证阶段筛选与新增/编辑保存。completed

## Expected Verification

- `node tests/e2e/dcc-category-lifecycle-stage-static.spec.js`
- `node tests/e2e/dcc-category-governance-summary-static.spec.js`
- `node tests/e2e/dcc-category-directory-binding-refresh-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- 真实 Playwright E2E：测试租户 `测试租户/aoteman` 进入 `http://localhost:8081/dcc/controlled-file/categories`，验证阶段列、INPUT 筛选、新增/编辑临时类别阶段保存。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文文本和测试输出必须显式 UTF-8，不使用 `&&`。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：新增列、筛选和表单控件沿用现有紧凑运营台风格。
- 命中 `frontend-feature-delivery`：不引入 mock，不隐藏 API 错误，不改变无关视觉结构。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，共享固定阶段选项并由接口字段持久化。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 类别列表显示阶段 -> Given 后端返回 lifecycleStage / When 管理员打开类别列表 / Then 阶段列显示对应固定标签。`
- `BDD: 阶段下拉筛选 -> Given 类别列表包含多个阶段 / When 管理员选择 02 input 输入 / Then 页面只保留 INPUT 阶段类别。`
- `BDD: 类别表单阶段必选 -> Given 管理员新增或编辑类别 / When 打开表单 / Then 阶段只能通过下拉选择并随保存提交 lifecycleStage。`

## Current Blockers

- 暂无。

## Final Verification Result

- `node tests/e2e/dcc-category-lifecycle-stage-static.spec.js` -> `PASS`。
- `node tests/e2e/dcc-category-lifecycle-stage-real.e2e.js` with `DCC_CATEGORY_STAGE_E2E_ALLOW_TEST_WRITE=1` -> `PASS`，真实测试租户完成阶段列、筛选、新增、编辑、删除验证。
- `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`，已同步修正 MES 排产页残留旧类型名 `MesProScheduleOrderRespVO` 为现有 `MesProScheduleOrderVO`。
