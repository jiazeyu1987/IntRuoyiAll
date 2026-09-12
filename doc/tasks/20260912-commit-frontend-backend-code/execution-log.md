# Execution Log

BDD: 提交边界保持可追溯 -> Given 工作区同时存在多个任务的前后端、迁移、文档和资源改动，When 执行提交前盘点，Then 只有已验证且归属明确的代码、测试、迁移和任务记录进入提交，未验证或资源文件留在工作区。

BDD: 推送后分支与远端一致 -> Given 实现和收尾提交均通过验证，When 推送 `int_main`，Then `HEAD` 与 `origin/int_main` 相同且无 ahead/behind。

## RED/GREEN Evidence

- RED: `mvn.cmd -q -pl yudao-module-mes -am ... MesTeamLeaderActiveOrderServiceTest ...` -> FAIL，23 个旧测试夹具未提供新的 production-process config schema，另有 4 个断言受并行改动影响；该结果不作为本次全量通过证据。
- GREEN: DCC 上传/发布定向回归 -> PASS，215 tests，0 failures，0 errors（任务 `20260912-dcc-upload-flow-static-hardening` 既有证据）。
- GREEN: MES 受影响业务定向回归 -> PASS，83 tests，0 failures，0 errors（任务 `20260911-frontline-route-device-followup-hardening` 既有证据）。
- GREEN: `pnpm.cmd ts:check` -> PASS，8GB relaxed TypeScript 检查完成。
- GREEN: DCC/MES/前端静态合同 -> PASS；仅 `dcc-loss-order-form-center-chain-static.spec.js` 缺少历史脚本、用户列配置合同需对当前详情页组件边界另行对齐，均不纳入本次修复。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，621 migrations。
- GREEN: `git diff-files --check -- IntRuoyiBackend IntRuoyiFronted docs doc` -> PASS，仅 CRLF 提示。

## Blockers

- 提交范围：前后端源码、正式测试、SQL 迁移、已完成任务记录和长期规则文档。
- 排除范围：`resource/` Office 文件、临时 tsconfig、运行输出、未完成任务的专用中间产物。
