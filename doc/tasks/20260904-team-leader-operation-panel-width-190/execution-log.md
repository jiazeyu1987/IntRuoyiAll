BDD: 操作面板固定宽度 190 -> Given 生产组长查看报工列表 When 表格渲染操作列 Then 操作面板固定宽度应为 190，避免按钮区过窄。
RED: node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs -> FAIL expected before contract update because production operation width was not locked to 190.
GREEN: node IntRuoyiFronted/tests/e2e/team-leader-production-report-payload-columns-static.spec.cjs -> PASS
GREEN: git diff --check -> PASS
GREEN: pnpm exec vue-tsc --noEmit --pretty false -> PASS
COMMIT: 88c6d3f7d fix: 固定生产报工操作列宽度 -> committed task-owned width change and verification records.
BLOCKED: git push origin int_main -> FAIL, TLS connect error unexpected eof while reading. Branch remains ahead of origin; project rule forbids completed status until push succeeds.
