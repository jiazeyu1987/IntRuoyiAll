# Execution Log

## User Intent

- 上午巡检、下午巡检默认包含。
- 抽样方案只识别首检、首检数量及巡检 AQL 抽样比例。
- 末检是否适用仅由顶部“是否需要末检”开关决定。
- 红框“适用检验类型”不得继续作为独立手工数据源。

## BDD / TDD

- BDD: QA 适用检验类型自动派生 -> Given QA 检验项目包含抽样方案且页面存在末检开关，When 页面展示或构建保存载荷，Then 上午巡检和下午巡检固定包含，抽样方案含“首件/首检：N 件”时额外包含首检并使用 N，AQL 值作为巡检抽样比例，末检只随顶部开关加入或移除。
- RED: `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> FAIL，预期原因：尚无正式抽样方案派生 helper，页面仍把 `row.applicableTypes` 作为独立可编辑数据源。
- GREEN: `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> PASS。

## Milestone Updates

- M1 completed：聚焦合同已按预期失败于正式派生 helper 缺失。
- M2 completed：新增抽样方案派生 helper；适用类型改为只读标签；页面、完整性检查和保存载荷统一使用正式派生规则；AQL 百分比按后端正式公式原值传递。
- M3 completed：聚焦合同、相邻回归、类型检查、真实页面联动和无写入约束均已通过；状态进入 `ready_for_closeout`。

## Verification Evidence

- `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> FAIL（预期 RED）。
- 保存链路契约核对：`MesTeamLeaderActiveOrderServiceImpl.calculatePatrolInspectionQuantity` 按 `plannedQuantity × ratio ÷ 100` 计算，因此 `AQL=0.4` 必须原值写入 `patrolInspectionRatio=0.4`，不得在前端二次除以 100。
- `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> PASS。
- `node tests/e2e/qa-regulation-applicable-types-default-visible-static.spec.js` -> PASS。
- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-pressure-pump-complete-pdf-items-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js` -> PASS。
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- 真实验收首次诊断 -> FAIL：`getByLabel('DCC 项目代码')` 同时命中项目选择框和复制按钮；改用精确角色 `combobox`，不改变产品代码。
- 真实验收第二次诊断 -> FAIL：远程下拉不接受完整展示标签作为检索词；按产品名称检索后再用正式 `ID / 球囊扩张压力泵 / 112` 候选精确选择。
- 真实验收第三次诊断 -> FAIL：开关响应后立即读取标签命中 Vue 渲染时序；增加面向可观察标签数组的等待，不放宽断言。
- `node doc/tasks/20260809-qa-applicable-types-derived/qa-applicable-types-derived.e2e.cjs` -> PASS：开启末检时，无首检方案为“上午巡检、下午巡检、末检”，含“首件：13 件”的方案为“首检、上午巡检、下午巡检、末检”；关闭末检后两类行均移除“末检”；MES 写请求 0、目标请求失败 0、console error 0、pageerror 0。
- `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md` -> PASS；`--self-test` -> PASS。
- `git diff --check -- <task-owned tracked files>` -> PASS，仅有现有 LF/CRLF 转换提示，无空白错误；任务自有新增文件尾随空白扫描 -> PASS。
- `task_closeout.py --task-id 20260809-qa-applicable-types-derived --mode preview` -> PASS；删除范围仅为本任务 `frontend-feature-evidence.md`、一次性 Playwright 脚本及对应输出目录，无 blocked/warnings。
- `task_closeout.py --task-id 20260809-qa-applicable-types-derived --mode apply` -> PASS；上述任务自有临时产物已删除，保留三份核心任务文档、生产代码和正式回归测试；当前为主工作区，无 worktree 合并或删除动作。
- `project-experience-consolidation` -> 已将“QA 抽样方案与适用检验类型共用项目级来源、AQL 百分比仅除以 100 一次”的复用门禁合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md`，未新建长期经验文档。
- Final status -> `completed`；用户未授权 Git 操作，未提交、合并或推送。

## Blockers

- 暂无。
