# Verification Report

## Scope

- 产品：DCC 项目代码 ID 对应的“球囊扩张压力泵”QA 规程配置草稿模板。
- 行为：将默认草稿中的合并工序“清洗/精洗”拆为“清洗”和“精洗”两条可见行；两条记录除工序与唯一 itemCode 外，检验项目、适用检验类型、接受标准、检验方法、检验器具及设备、抽样方案和 PDF 来源内容保持一致。

## Verification Evidence

- RED: node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs -> FAIL，旧模板仍为 17 行。
- RED: node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs -> FAIL，旧绑定仍包含“清洗/精洗”复合工序。
- GREEN: node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs -> PASS。
- GREEN: node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs -> PASS。
- GREEN: pnpm.cmd ts:check -> 完成且无类型错误输出；底层 vue-tsc --noEmit -p tsconfig.relaxed.json 复核完成且无错误输出。
- GREEN: git diff --check -- task files -> PASS，仅 LF/CRLF warning。

## Result

- PASS：前端默认草稿模板已按用户要求拆分“清洗”和“精洗”两行，后续检验内容保持一致；未引入 fallback、默认成功或吞异常。
- Closeout：cleanup preview/apply 均通过，未删除文件；任务状态已标记 completed。
