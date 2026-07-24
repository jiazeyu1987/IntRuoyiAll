# 任务：DCC 基础数据页新增产品目录页签

## 任务目标

- 新增独立只读接口 `GET /dcc/product-catalog/page`，权限复用 `dcc:project-code:query`。
- 后端实时读取固定桌面文件 `C:\Users\BJB110\Desktop\总经办瑛泰产品目录0420.xlsx`，解析 `子公司产品` 与 `瑛泰产品（含璞慧、七木）` 两个 sheet。
- 解析时执行表头标准化校验、空行过滤、层级字段向下补齐、日期标准化、分页和基础筛选；文件缺失、sheet 缺失或表头异常时直接失败。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-feedback-attribution-active-task-candidate-regression\task.md`
- 状态：`COMPLETED`
- 处理：上一后端任务已收口，不阻塞本次 DCC 产品目录接口开发。
- 用户计划中指定的旧 DCC 任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260623-dcc-browser-batch-recognition\task.md` 已在本轮显式更新为 `BLOCKED`，允许继续新任务。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\agent-memory\project-error-prevention.md`
- 适用强制门禁：
  - Excel 有效区域不能依赖 `sheet.getLastRowNum()` 或格式刷后的百万空行；必须按实际存在行迭代。
  - 本轮是本机源码与定向测试开发阶段，不触发真实 E2E / 服务器写入 / 发布，因此当前不需要 `experience-preflight`；若后续进入真实浏览器验收，再补该门禁记录。
  - 不允许用缓存、数据库落库、兜底 sheet 名或表头模糊猜测掩盖真实桌面文件问题。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。文件不存在、sheet 缺失、表头不符、读取失败均直接失败并返回明确错误。
- `是否从根因和长期维护角度解决`：是。新增独立 `productcatalog` controller/service/vo，隔离项目代码服务职责。
- `是否存在临时补丁或绕过`：否。不会把数据导入数据库、不会偷偷忽略坏表头、不会在后端构造默认空页成功。

## BDD 场景

- `BDD: 两个业务 sheet 可实时合表分页 -> Given 固定 Excel 存在且 sheet 名正确 / When 调用 /dcc/product-catalog/page / Then 返回按 数据来源 + 原 sheet 行号 排序的合并分页结果。`
- `BDD: 层级列按真实 Excel 规则向下补齐 -> Given 某些数据行的 产品类别 I、产品类别 II、产品序号、产品 为空 / When 解析相邻非空层级行后的记录 / Then 返回值沿用最近一个非空层级值。`
- `BDD: 基础筛选按计划字段生效 -> Given 合表结果已生成 / When 传入 keyword、categoryLevel1、categoryLevel2、productStatus、dataSource / Then 返回符合条件的只读分页数据。`
- `BDD: 缺少固定文件、sheet 或表头时直接失败 -> Given 文件缺失、目标 sheet 缺失或表头异常 / When 调用分页接口 / Then 后端返回明确错误且不做降级处理。`

## 里程碑

1. M1：建立后端任务台账并补旧 DCC 任务状态。`COMPLETED`
2. M2：补 RED 单测与控制器契约，锁定解析规则和失败前置条件。`COMPLETED`
3. M3：实现产品目录只读服务、VO、控制器和错误码。`COMPLETED`
4. M4：运行定向测试并补齐 API 证据。`COMPLETED`

## 最终验证结果

- `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*ProductCatalog*Test -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\backend-api-evidence.md` -> PASS
- 真实运行态接口验收 -> PASS，`/admin-api/dcc/product-catalog/page` 返回 `total=213`，筛选关键词 `导管鞘组（大腔鞘）` 返回 `total=1`。

## 预期验证

- `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*ProductCatalog*Test -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\backend-api-evidence.md`

## Cleanup Keep

- `doc/tasks/20260626-dcc-basic-data-product-catalog-tab/task.md`
- `doc/tasks/20260626-dcc-basic-data-product-catalog-tab/execution-log.md`
- `doc/tasks/20260626-dcc-basic-data-product-catalog-tab/backend-api-evidence.md`
