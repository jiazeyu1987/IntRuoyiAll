# 任务：DCC 岗位分配上传人派生岗位约束

## 目标

调整 DCC `岗位分配` 页面中 `编制人直接主管`、`部门负责人`、`部门授权代表` 的维护规则，使其不再允许指定具体人物，并与后端上传流中的“按上传人动态解析审批人”语义保持一致。

## 非目标

- 不改其他普通岗位的分配方式。
- 不重做 DCC 页面样式或表格结构。
- 不引入 mock、fallback、静默降级或前端假解析。

## 前置任务检查

- 前一个同仓库任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-browser-latest-version-default\task.md`
- 启动前状态：已完成。
- 影响：受控浏览写入边界与本任务不重叠，可独立推进。

## 范围

- `src/views/dcc/controlled-file/positions/index.vue`
- 如需补充页面契约字段，可修改：
  - `src/api/dcc/controlledFile/approvalPositions.ts`
- 本任务目录下的执行记录与前端证据

## 里程碑

- [x] M1：建档并核实现有岗位分配页行为与后端契约。
- [x] M2：补前端 RED 测试，证明这三个岗位当前仍可指定具体用户。
- [x] M3：实现页面约束与提示，使特殊岗位不再允许指定人物。
- [x] M4：运行前端 GREEN 验证并补齐证据。
- [x] M5：执行收尾预览，准备仅包含本任务改动的提交。

## 预期验证

- `node --test scripts/dcc-position-uploader-derived-roles.test.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-dcc-position-uploader-derived-roles/frontend-feature-evidence.md`
- 若本地前端入口可用：从 `http://localhost:8081` 进入 `DCC岗位分配` 页面，复核这三个岗位不再允许指定具体用户。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-dcc-position-uploader-derived-roles --mode preview`

## 当前状态

已完成：岗位分配页已改为对上传人派生岗位只读展示，不能再指定固定人员；其中 `部门授权代表` 额外明确标记为“来源待定 / 运行时将阻塞”，与后端 fail-fast 规则一致。

## Current Status

completed

## 写入边界

- `src/views/dcc/controlled-file/positions/index.vue`
- `src/api/dcc/controlledFile/approvalPositions.ts`
- `scripts/dcc-position-uploader-derived-roles.test.mjs`
- `doc/tasks/20260519-dcc-position-uploader-derived-roles/**`

## 风险与约束

- 页面约束必须与后端真实校验一致，不能只做前端隐藏。
- `编制人直接主管`、`部门负责人`、`部门授权代表` 的最终交互要基于后端真实派生语义，不能前端自行猜测审批人。
- 当前 `部门授权代表` 缺少确认的真实来源，因此前端必须显式暴露阻塞提示，不得伪装成已支持的动态解析。

## 验证结果

- PASS: `node --test scripts/dcc-position-uploader-derived-roles.test.mjs`
- PASS: `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-position-uploader-derived-roles run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-position-uploader-derived-roles\scripts\verify-dcc-position-uploader-derived-roles.mjs`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-position-uploader-derived-roles\frontend-feature-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-dcc-position-uploader-derived-roles --mode preview`
