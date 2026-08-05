# Feature

## Goal

在新增人员弹窗标题栏内显示临时工新增错误，并提供完整的消失与退出机制。

## Non-goals

- 不修改后端校验、错误码或错误原文。
- 不修改正式工关联、人员列表或其它模块错误展示。
- 不引入 mock、fallback、默认成功或异常吞噬。

## Entry And Owned Files

- 页面：生产组长 -> 人员管理 -> 新增人员。
- 组件：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 测试：`production-personnel-duplicate-inline-error-static.spec.js` 和相邻人员管理合同。

# Acceptance

- `AC-INLINE-01`：错误在新增人员弹窗标题栏黄框位置显示。
- `AC-INLINE-02`：错误文本为红色并保留后端原文。
- `AC-INLINE-03`：临时工新增失败不调用全局 `ElMessage.error`。
- `AC-INLINE-04`：错误 6 秒后自动消失，并可手动关闭。
- `AC-INLINE-05`：修改显示名或关闭弹窗时清理错误及定时器。
- `AC-INLINE-06`：组件卸载时清理定时器。

# API Contracts And Data States

- API contract：无变更，继续调用 `createTemporaryTeamEmployee`。
- Error state：后端错误经 `resolveErrorMessage` 写入弹窗局部状态。
- Loading/success：提交 loading、成功 toast 和人员列表刷新保持。
- Permission：无变更。
- Responsive：标题栏在窄屏改为两行，错误文本允许换行。
- Accessibility：错误使用 `role="alert"` 和 `aria-live="assertive"`；手动关闭按钮有 `aria-label`。

# BDD

BDD: 同名错误在新增人员弹窗内闭环 -> Given 用户在新增人员弹窗手动录入一个已存在的临时工显示名 When 后端返回同名有效员工错误 Then 错误以红字显示在弹窗标题栏，不触发全局错误，并可自动消失、手动关闭、修改姓名清除或关闭弹窗清理。

# RED

RED: `node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js` -> FAIL，弹窗没有局部错误 header，临时工 catch 仍调用全局错误。

# GREEN

GREEN: `node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js` -> PASS。

# Verification

- 聚焦错误归属静态合同通过。
- 新增人员弹窗、生产人员管理和黄框内容删除相邻合同通过。
- `pnpm ts:check` 通过。
- task-path `git diff --check` 通过。

# Blockers

- 当前共享分支存在非本任务 ahead 基线提交，推送仍需边界复核。
- 未执行真实浏览器 E2E。

# Follow-up Skills

- `project-experience-consolidation`：现有截图样式、静态合同隔离和共享分支并发门禁足以覆盖，无需新建经验文档。

