# MES 工序 Excel 源数据一致性修复

## Task Goal

- 将系统 MES 工序列表与 `C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx` 中的工序数据修正为完全一致。
- 验收口径：系统列表不多、不少、不改名、不乱序；若正式数据链路缺少源文件或目标位置，必须 fail fast，不用默认值、mock 或兜底补齐。

## Milestones

- [x] 建立源数据基线：解析 Excel 中的 MES 工序清单、字段、数量和顺序。
- [x] 定位系统 MES 工序列表的数据来源、导入逻辑或种子数据。
- [x] 编写或更新回归测试，先证明当前系统列表与 Excel 不一致。
- [x] 实施最小正式修复，使系统数据与 Excel 完全一致。
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION 证据。
- [x] 修复 2026-08-01 运行态路由缺失：重建并切换 48081 后端 runtime Jar，应用本机目录表 SQL。
- [ ] 收尾：更新验证报告、经验沉淀、cleanup、提交并推送。

## Expected Verification

- Excel 解析结果可复核：记录 sheet、表头、工序条数、首尾工序和规范化规则。
- RED：定向测试或校验脚本在修复前报告系统列表与 Excel 有差异。
- GREEN：同一测试或校验脚本在修复后通过，差异数为 0。
- REGRESSION：按触发规则运行相关后端/前端/数据库定向验证。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将定位正式数据源并以 Excel 为唯一验收基线。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 已读取 `docs/experience-index.md`，命中方向：MES 工序、工艺路线 Excel 导入、数据库种子/基线数据、PowerShell 编码与任务收尾。
- 已按实际修改范围读取并执行：`docs/database-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`。

## Completed Work

- Excel 基线：`压力泵工序.xlsx` / `二代压力泵` 工作表，12 个原始列，Excel 行 2-33 为 32 条有效 MES 工序；行 34-36 仅有孤立产能值，已明确排除。
- 根因修复：MES 工序页不再复用 `/mes/pro/route-resource/page` 路线资源聚合读模型，改为独立 `/mes/pro/mes-process/page` 只读目录。
- 数据修复：新增 `mes_pro_mes_process_catalog` 与 `mes_pro_mes_process_catalog_machinery` 迁移种子，32 条目录数据按 Excel 原始列、源行号和顺序写入。
- 租户修复：Excel 只读目录 DO 显式 `@TenantIgnore`，避免 `tenant_id=0` 源基线被当前业务租户过滤导致列表为空。
- 前端修复：MES 工序列表展示 Excel 12 个原始列，保留 `/` 与空白原值，不提供新增、编辑、删除、导入、启停等维护入口。

## Verification Evidence

- GREEN: `node doc\tasks\mes-process-xlsx-sync-20260731\tools\parse-pressure-pump-xlsx.mjs` -> PASS，解析到 32 条有效行、3 条排除行。
- GREEN: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS，校验页面、接口、权限、租户忽略、SQL 32 行全字段与 Excel 基线一致。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，1 test passed。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\mes-process-xlsx-sync-20260731\migration-policy-gate.json` -> PASS，migrationCount=403。
- NEW BUG: 2026-08-01 用户反馈运行态请求 `admin-api/mes/pro/mes-process/page` 返回“请求地址不存在”，旧验证尚未证明当前 48081 运行后端已加载新增 Controller，需要补充路由注册/运行态验证。
- RED: `node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> FAIL，当前 48081 actuator mappings 缺少 `/mes/pro/mes-process/page`，运行 Jar 内未加载新 Controller。
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> BUILD SUCCESS，并复制为 `output\runtime\int_main\backend-runtime-control-20260801-002326.jar`。
- GREEN: 48081 后端已切换到新 runtime Jar，health `UP`，`node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> PASS。
- GREEN: 本机 MySQL 应用 `20260731_mes_process_catalog_from_pressure_pump_xlsx.sql` -> PASS；目录表 32 行，源 Excel 行号 2-33，排序 1-32。
- GREEN: 登录态请求 `/admin-api/mes/pro/mes-process/page?pageNo=1&pageSize=50` -> `code=0`，`total=32`，首条 `粗洗`，末条 `W包装打包`。
- BLOCKED REGRESSION: 重新运行 `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 在 `testCompile` 阶段被无关历史 MES 测试缺失符号阻塞；本轮未修改这些测试，运行态 API 与静态合同已覆盖本次 404/500 修复。

## Known Risks

- 当前 `int_main` 工作区已有大量未提交改动且分支领先 `origin/int_main`，本任务不得覆盖或混入无关改动；若目标文件与既有改动冲突，需先阻塞确认。
- 收尾提交/推送尚未执行：当前工作区存在无关第三方报工进度任务改动，且最新并发基线提交 `ec52d8dc8` 已吸收部分 MES 工序任务文件；本轮未 stage、未 commit、未 push，避免混入无关改动。
- Cleanup apply 未执行：cleanup preview 将 `C:\Users\BJB110\.cache\codex-runtimes\...node_modules` 等工作区外运行时依赖列入 delete，超出本任务清理范围。

## Cleanup Keep

doc/tasks/mes-process-xlsx-sync-20260731/bug-regression-evidence.md
doc/tasks/mes-process-xlsx-sync-20260731/tools/check-mes-process-runtime-route.mjs
