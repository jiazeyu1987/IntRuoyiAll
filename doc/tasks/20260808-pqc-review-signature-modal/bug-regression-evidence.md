# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 用户反馈“复核的时候，弹框里没有电子签名的地方”，复核弹框展示内部签名字段而非可填写的电子签名密码。
- Expected: 复核弹框展示“电子签名”密码输入；用户提交签名密码后，后端通过正式签名服务生成签名 ID、签名员工和签名快照，客户端不得伪造或覆盖这些内部字段。

## Reproduction

- RED: `node tests\e2e\team-leader-review-signature-dialog-static.spec.cjs` -> FAIL，旧弹框没有 `data-team-leader-review-signature` 密码输入，且存在“复核签名ID / 签名员工ID / 签名快照”内部字段。

## Root Cause

- 复核表单把服务端派生的签名 ID、签名员工 ID、签名快照暴露给用户填写，并在请求契约中继续接受这些内部字段，导致正式电子签名密码输入缺失。

## Regression Test

- Added: `IntRuoyiFronted\tests\e2e\team-leader-review-signature-dialog-static.spec.cjs`
- Updated adjacent coverage: `team-leader-report-allocation-dialog-hide-static.spec.cjs`、`team-leader-pqc-review-gate-static.spec.js`、`MesP0TeamLeaderReviewSignatureSchemaTest`、`MesProcessPoolTeamLeaderControllerTest`

## GREEN

- GREEN: `node tests\e2e\team-leader-review-signature-dialog-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Verification

- PASS: `node tests\e2e\team-leader-report-allocation-dialog-hide-static.spec.cjs`
- PASS: `node tests\e2e\team-leader-pqc-review-gate-static.spec.js`
- PASS: `pnpm ts:check`
- PASS: `git diff --check`

## Blockers

- None.

## Risks And Follow-Up

- No fallback introduced.
- No data repair performed.
- Real browser write-path E2E was not run in this turn; current verification is static UI contract plus targeted backend unit/contract tests.
