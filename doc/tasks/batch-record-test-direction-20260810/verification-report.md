# Verification Report

## Summary

批记录测试的 CODE_READONLY 方法已从“完整实现正确性判定”调整为“业务方向是否偏离判定”。方向符合但缺少 Service、Mapper 或测试等实现细节证据时，Runner prompt 要求返回 PASS 并在实际回复中说明证据缺口；只有最小方向证据缺失时才 BLOCKED，方向相反或职责边界混淆时 FAIL。

## Verification

- RED: node tests\e2e\edhr-batch-record-test-tab-static.spec.cjs -> FAIL，证明旧测试方法仍是“完整支持”。
- RED: node tests\e2e\codex-runner-code-readonly-static.spec.cjs -> FAIL，证明旧 prompt 仍缺少业务方向判定规则。
- GREEN: node tests\e2e\edhr-batch-record-test-tab-static.spec.cjs -> PASS。
- GREEN: node tests\e2e\codex-runner-code-readonly-static.spec.cjs -> PASS。
- GREEN: node tests\e2e\batch-record-test-codex-cli-response-static.spec.cjs -> PASS。
- REGRESSION: tests/e2e/*batch-record-test*static*.cjs -> PASS，8 条静态合同通过。
- REGRESSION: pnpm ts:check -> PASS，vue-tsc 退出码 0。
- CHECK: git diff --check -- 当前任务文件 -> PASS。

## Risk

- 未运行真实页面点击测试；本次改动集中在测试定义生成和 Runner prompt，已用静态合同和类型检查覆盖。
