# 任务：修复展柜产品候选中文名空字符串校验

## 任务目标

修复展柜维护产品弹框加载候选产品时，`products[9].nameCn` 为空字符串被前端契约误判为缺少字符串字段的问题。字段缺失或非字符串仍必须失败，不能用产品编码、默认值或其他字段替代。

## 里程碑

- [x] M1：确认上一个前端任务文档已完成，并建立本次任务记录。
- [x] M2：添加 RED 回归测试，复现空字符串 `nameCn` 被拒绝的问题。
- [x] M3：最小修复展柜候选产品契约，使字段存在且为字符串即可通过。
- [x] M4：运行目标回归与类型检查，记录 GREEN 证据。
- [x] M5：收尾清理预览、提交本次直接相关改动。

## 预期验证

- `node scripts/showroom-admin-hall-candidate-namecn-contract.test.mjs` 先失败后通过。
- `node scripts/showroom-admin-product-hall-operability.test.mjs` 通过。
- `pnpm ts:check` 通过。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260531-showroom-hall-product-namecn-contract/bug-regression-evidence.md` 通过。

## 当前状态

- status: completed

Completed. 已修复展柜维护产品弹框中空字符串 `nameCn` 被误判为缺字段的问题；候选产品和已选产品现在保留 `nameCn: ""`，字段缺失仍失败。目标回归、既有展柜契约、类型检查、真实 UI 探测和 bug 证据校验均已通过。收尾清理预览无待删除项。

## Current Status

Completed. The hall product mapping contract now preserves explicit empty `nameCn` strings for candidate and selected products while still failing when the field is absent. Targeted regression, existing hall contract checks, type check, real UI probe, bug evidence validation, and cleanup preview passed.

## Final Verification

- `node scripts/showroom-admin-hall-candidate-namecn-contract.test.mjs` -> PASS。
- `node scripts/showroom-admin-product-hall-operability.test.mjs` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- Playwright 真实 UI 探测 `http://127.0.0.1:8081/showroom/hall` -> PASS。
- `validate_bug_regression.py --evidence doc/tasks/20260531-showroom-hall-product-namecn-contract/bug-regression-evidence.md` -> PASS。
- `task_closeout.py --task-id 20260531-showroom-hall-product-namecn-contract --mode preview` -> PASS，delete 为 `<none>`。
- `task_closeout.py --task-id 20260531-showroom-hall-product-namecn-contract --mode apply` -> PASS，delete 为 `<none>`。

## 阻塞

无。

## Cleanup Keep

- `doc/tasks/20260531-showroom-hall-product-namecn-contract/bug-regression-evidence.md`
- `scripts/showroom-admin-hall-candidate-namecn-contract.test.mjs`
