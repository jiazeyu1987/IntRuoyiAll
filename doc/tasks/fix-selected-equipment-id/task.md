# 修复一线提交 QA 末检设备身份缺失

## Task Goal

修复一线提交身份上下文缺少必填字段 `itemResults.CODX-AO5-QA-FINAL.selectedEquipmentId` 的问题，确保 QA 末检项目提交时按正式设备身份携带必填设备字段，不通过 fallback、默认值或吞异常掩盖数据链路缺失。

## Milestones

- [x] 建立可复现证据并定位 `selectedEquipmentId` 缺失链路
- [x] 编写先失败的回归测试，覆盖 QA 末检 `itemResults` 设备字段
- [x] 实施最小正式修复，确保提交载荷携带必填设备身份
- [x] 运行定向回归验证并记录 RED/GREEN 证据
- [x] 完成任务文档、验证报告与收尾状态更新

## Expected Verification

- `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` 先 RED，失败原因：缺少签名前逐项设备身份校验。
- 修复后同一定向静态合同 GREEN。
- `pnpm ts:check` PASS。
- `git diff --check` PASS。
- 不引入 fallback、默认设备、静默降级或异常吞噬。

## Experience Gate Summary

- 已读取 `docs/experience-index.md`，命中 `itemResults`、QA 末检、检验设备、必填设备参数缺失等关键词。
- 已读取 `docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`，定位为一线 PQC 前端正式提交载荷构造缺少签名前设备身份校验。
- 已按 `project-experience-consolidation` 合并长期经验到 `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁`，并补充 `docs/experience-index.md` 关键词。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，已把设备身份必填校验前移到签名前的正式提交 preflight，并用静态合同保护。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

实现、验证和 cleanup 已完成。保留 `task.md`、`execution-log.md`、`verification-report.md`；临时 `bug-regression-evidence.md` 已在 validator PASS 归档后由 cleanup 删除。
