# 任务：运行控制台按钮弹框操作逻辑（前端）

## 目标

按用户定义补全每个按钮的点击逻辑、弹框内容、提交前校验和执行预期：

- 发布类按钮只展示发布包候选。
- 备份恢复类按钮只展示备份点候选。
- 回滚版本与恢复数据必须在视觉和字段上明确区分。
- 正式服高危动作必须要求 `PROD` 和责任人。
- 事故闭环只记录闭环，不执行发布或恢复。

## 里程碑

1. 补前端 RED 测试，锁定按钮、弹框、候选列表和提交体。
2. 实现弹框字段、阻断状态、候选详情和操作记录刷新。
3. 运行静态测试和可用 E2E 路径。

## 预期验证

- 静态测试覆盖所有按钮标签、弹框字段、ReleasePackage/BackupPackage 文案和高危确认逻辑。
- E2E 路径验证按钮点击后弹框出现、缺前置条件时不提交。

## 当前状态

已完成并已融合到前端 `int_main`。运行控制台按钮弹框已补充 NAS 双目录、备份环境选择、测试通过验证结论、来源/目标目录、责任人门禁、发布包完整性/测试通过筛选、回滚正式服发布历史详情、恢复候选详情和预期结果展示；真实 E2E 已覆盖测试租户写入路径、`芋道源码/admin` 只读路径和融合后 `int_main` 只读复测。

## 验证结果

- `node tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-ops-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS，使用主工作区依赖临时 junction 到当前 worktree 后完成，验证后已删除 junction。
- `node tests\e2e\runtime-control-real-release-backup-setup.e2e.js` with `RUNTIME_CONTROL_REAL_SETUP_SCOPE=build-release-only` -> PASS，测试租户真实点击“构建发布包”，发布包写入 `Backup/ReleasePackage/26-05-31_00-33-43`。
- `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` with `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8098` -> PASS，功能分支 `芋道源码/admin` 真实只读 E2E 覆盖 AC-01 至 AC-11。
- `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` with `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081` -> PASS，融合后前端 `int_main` + 后端 `int_main:48081` 真实只读 E2E 覆盖 AC-01 至 AC-11。
