# 任务：电子批记录按批记录名称删除

## 任务目标

- 后端新增按批记录名称删除电子批记录模板的正式接口。
- 删除时复用现有已绑定报表保护规则：未绑定报表删除，已绑定报表保留并返回数量。
- 前端左侧每个批记录名称行提供单独删除入口，不再只能删除全部模板。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-showroom-product-one-click-translate-publish\task.md`
- 状态：`COMPLETED`
- 处理说明：本任务只修改 MES 电子批记录模板相关接口、服务和测试，不触碰展厅任务范围。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面必须沿用 IntPP 运维台样式：白底、轻边框、紧凑列表、明确操作区、稳定尺寸。
  - 本次默认执行静态/单元/集成验证；如进入真实 Playwright 登录 E2E，必须先记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少批记录名称或后端删除失败时直接报错，不做前端假删除。
- `是否从根因和长期维护角度解决`：是。新增正式后端按名称删除接口，前端调用明确 API，不复用全量删除或客户端循环猜测。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 按批记录名称删除未绑定模板 -> Given 批记录名称下存在多份未绑定电子批记录报表 / When 调用按名称删除接口 / Then 删除对应 Jimu 报表与元数据，并返回删除数量。`
- `BDD: 已绑定模板保留 -> Given 批记录名称下同时存在已绑定和未绑定报表 / When 调用按名称删除接口 / Then 未绑定报表删除，已绑定报表保留并返回保留数量。`
- `BDD: 缺少批记录名称失败 -> Given 删除请求缺少批记录名称 / When 调用接口 / Then 后端失败并提示批记录名称不能为空。`

## 里程碑

1. M1：补充后端 API 合同测试和服务回归测试。`COMPLETED`
2. M2：实现按批记录名称删除接口、服务和前端调用。`COMPLETED`
3. M3：运行前后端目标验证。`COMPLETED`
4. M4：更新证据、收尾预览并按验证结果处理提交。`COMPLETED`

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `node scripts/electronic-batch-record-jimu-list.test.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260626-electronic-batch-record-row-delete/backend-api-evidence.md`

## 最终验证结果

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，43 tests。
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS。
- `node scripts/electronic-batch-record-jimu-list.test.mjs` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

## Cleanup Keep

- `doc/tasks/20260626-electronic-batch-record-row-delete/backend-api-evidence.md`
