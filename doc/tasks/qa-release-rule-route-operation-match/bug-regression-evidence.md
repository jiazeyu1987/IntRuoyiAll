# Bug Regression Evidence

## Bug Summary And Expected Behavior

QA 发布规则选择产品“球囊扩张压力泵”时，外观检验项的正式工序“清洗”在发布前被要求匹配同名激活路线工序，导致当前激活路线版本使用正式复合路线工序“清洗/精洗”时提示未匹配。

期望行为：PQC-ID-001 中可见检验项继续拆分为“清洗”和“精洗”两行，但发布 payload 应通过项目级显式映射把两行都归入正式激活路线版本工序“清洗/精洗”，并继续禁止粗洗或其它未配置工序被猜入。

## Reproduction

- Command: pnpm exec node tests\e2e\qa-regulation-process-scoped-publish-static.spec.cjs
- Path: IntRuoyiFronted/tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs

## Root Cause

IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue 中 ID 项目的 QA_PROCESS_SCOPE_BINDINGS_BY_PROJECT_CODE 将“清洗”和“精洗”分别绑定到同名工序。当前产品的激活路线版本使用正式复合工序“清洗/精洗”时，resolveQaRegulationItemRouteProcesses 只能按显式映射后的工序名匹配，因而“清洗”无法命中任何路线工序。

## Regression Test

Updated: IntRuoyiFronted/tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs

The test now requires ID 项目中“清洗”和“精洗”发布绑定均指向“清洗/精洗”，同时继续断言可见草稿行没有恢复为复合 key，且不发布粗洗规程。

## RED

- RED: pnpm exec node tests\e2e\qa-regulation-process-scoped-publish-static.spec.cjs -> FAIL, expected reason: 旧映射仍为 清洗 -> 清洗、精洗 -> 精洗，未覆盖激活路线版本正式复合工序“清洗/精洗”。

## GREEN

- GREEN: pnpm exec node tests\e2e\qa-regulation-process-scoped-publish-static.spec.cjs -> PASS

## Verification

- pnpm exec node tests\e2e\qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs -> PASS
- pnpm ts:check -> PASS
- git diff --check -> PASS, only existing line-ending warnings observed

## Risk And Regression Scope

- Scope: Frontend QA 规程页发布 payload 的项目级工序显式映射。
- Risk: 同一 ID 产品如果存在拆分为独立“清洗”和“精洗”的激活路线版本，本映射会 fail fast 而不是猜测双模式；这符合当前严格显式映射要求。
- Regression: 相邻 PDF 项目静态合同与 TypeScript 检查已通过。

## Blockers And Follow-Up

- No blocker.
- No fallback, graceful degradation, or formBindings substitution introduced.
