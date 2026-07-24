# 执行日志

BDD: 发布按钮弹框 -> Given 用户在运行控制台点击发布类按钮, When 弹框打开, Then 只出现发布包相关字段和候选。
BDD: 备份按钮弹框 -> Given 用户点击立即备份, When 弹框打开, Then 选择备份环境并展示 `Backup/BackupPackage/<backupId>` 目标。
BDD: 回滚与恢复区分 -> Given 用户分别点击回滚版本和恢复数据, When 弹框打开, Then 回滚只展示发布包和应用版本，恢复只展示备份点和数据恢复字段。
BDD: 事故闭环只记录 -> Given 用户点击事故闭环, When 保存闭环, Then 前端只调用闭环接口，不提交运行控制台动作接口。
BDD: 备份环境选择 -> Given 用户点击立即备份, When 选择测试服或正式服, Then 弹框按所选环境显示 PROD 门禁并提交 `targetEnvironment`。
BDD: 测试通过结论 -> Given 用户点击标记测试通过, When 当前测试服存在 releaseTag, Then 弹框要求填写验证结论并随请求提交。
BDD: 发布包完整性筛选 -> Given 后端返回 ReleasePackage 元数据, When 打开测试部署或正式上线弹框, Then 只展示 manifest/checksum 完整且符合测试通过条件的发布包。
BDD: 回滚正式服历史提示 -> Given 回滚候选来自 ReleasePackage, When 弹框渲染候选, Then 展示正式服发布历史路径，和恢复数据的 checksum/演练/快照字段区分。

## 证据

- 2026-05-30: 创建前端任务文档并读取统一前端样式，等待 RED 测试。
- RED: `node tests\e2e\runtime-control-ops-static.spec.js` -> FAIL, 现有通用弹框缺少 `Backup/ReleasePackage`、`Backup/BackupPackage`、目标目录、责任人门禁和回滚/恢复边界文案。
- GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-ops-static.spec.js` -> PASS。
- GREEN: `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS，使用主工作区依赖临时 junction 到当前 worktree 后完成；验证后已删除 junction。
- GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-ops-static.spec.js` -> PASS，覆盖 `targetEnvironment`、`testConclusion`、发布包 tested/checksum 元数据和恢复候选详情。
- GREEN: `pnpm ts:check` -> PASS，使用主工作区依赖临时 junction 到当前 worktree 后完成；验证后已删除 junction。
- GREEN: `node tests\e2e\runtime-control-release-package-static.spec.js; node tests\e2e\runtime-control-ops-static.spec.js` -> PASS，覆盖回滚正式服发布历史和恢复候选详情区分。
- GREEN: `pnpm ts:check` -> PASS，使用主工作区依赖临时 junction 到当前 worktree 后完成；验证后已删除 junction。
- GREEN: `node --check tests\e2e\runtime-control-real-release-backup-setup.e2e.js; node --check tests\e2e\runtime-control-real-test-backup-setup.e2e.js; node --check tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS。
- GREEN: `node tests\e2e\runtime-control-real-release-backup-setup.e2e.js` with `RUNTIME_CONTROL_REAL_SETUP_SCOPE=build-release-only` -> PASS，测试租户真实点击“构建发布包”，发布包写入 `Backup/ReleasePackage/26-05-31_00-33-43`。
- GREEN: `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` with `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8098` -> PASS，功能分支 `芋道源码/admin` 真实只读 E2E 覆盖 AC-01 至 AC-11。
- MERGE: frontend `codex/20260530-runtime-control-nas-assets` -> `int_main` -> PASS，merge commit `70f9e8871`；合并前未跟踪旧任务文档已保留到 `doc/tasks/20260530-runtime-control-nas-assets.pre-merge-untracked-20260531_005256`。
- GREEN: `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` with `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081` -> PASS，融合后前端 `int_main` + 后端 `int_main:48081` 真实只读 E2E 覆盖 AC-01 至 AC-11。
