BDD: 操作面板固定宽度 190 -> Given 生产组长查看报工列表 When 表格渲染操作列 Then 操作面板固定宽度应为 190，避免按钮区过窄。
RED: node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs -> FAIL expected before contract update because production operation width was not locked to 190.
GREEN: node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs -> PASS
GREEN: git diff --check -> PASS
GREEN: pnpm exec vue-tsc --noEmit --pretty false -> PASS
