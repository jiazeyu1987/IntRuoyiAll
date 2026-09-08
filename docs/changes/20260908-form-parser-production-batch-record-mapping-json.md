# 表单解析生产批记录输出批记录映射 JSON 变更

## Request Summary And Source

用户在 2026-09-08 反馈：当前“表单解析 -> 生产批记录”下载的是 Jimu 表单 JSON，但实际期望下载类似 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json` 的批记录业务映射 JSON。

## Current Baseline Reviewed

- 当前前端 `form-center/parser/index.vue` 上传 Word 后下载整个 parse response。
- 当前后端 `parseProductionBatchRecordJson` 返回 `recognizedSchemaJson`、`jimuSchemaJson` 和字段列表，语义偏表单中心模板识别。
- 目标样例 JSON 顶层为 `product`、`schemaVersion`、`processes`，工序下包含 `inputs`、`outputs`、`equipmentGroups`、`parameters`、`selectionMode`，属于生产批记录业务映射格式。
- 本任务已确认旧 `.doc` 可生成 visual schema，但该 schema 不是用户期望的下载格式。

## Classification

需求变更 / API 输出数据合同变更 / 前端下载内容变更。

## Impact

- Product: “生产批记录”按钮下载的 JSON 应面向生产批记录配置映射，而不是 Jimu 表单版式。
- Design: 页面仍保留一个上传按钮和 `.json` 下载，不新增导入持久化或审批流程。
- Data: 不新增数据库表和迁移，不写入模板版本、审批或 MES 报表。
- API: 新增 MES parse-only 接口 `POST /mes/pro/batch-record-report/production-batch-record/total-recognition-json` 返回批记录业务映射 JSON 字符串；前端“生产批记录”按钮改调用该接口，旧 BPM Jimu 解析接口不作为本次下载入口。
- Test: 需要用目标样例结构建立 RED/GREEN，至少断言顶层 `schemaVersion/product/processes`、工序、物料、设备和参数结构。
- Release: 后端代码变更需要重新加载服务后页面才会体现。
- Operations: 不重启 `int_main`，不执行远程操作，不提交/推送，除非用户当轮明确授权。

## Decision

accept

接受本次变更：生产批记录解析下载目标从 Jimu 表单 JSON 调整为批记录业务映射 JSON。实现必须从 Word 内容解析结构，不得直接返回样例文件、空 JSON、mock JSON 或默认成功。

## Required Approvals

用户已明确提出输出格式修正，可在当前任务范围内修改本地代码与运行本地测试。数据库写入、服务重启、远程操作、Git 提交/推送仍需另行授权。

## Downstream Skill Reruns

- `backend-api-delivery`：新增 parse-only API 的数据合同与服务转换，确认不写 Jimu 报表、批记录版本或 DCC 项目编码。
- 前端静态合同：确认下载内容取业务映射 JSON，文件扩展名仍为 `.json`。
- `project-experience-consolidation`：完成后把“表单解析业务映射 JSON 不等于 Jimu JSON”的经验合并到已有 Word 解析设计文档。

## Blockers And Next Action

当前无需求阻塞。下一步先补 RED 测试，证明当前返回仍是 Jimu/response 结构，不满足 `批记录总对应.json` 格式；再实现最小转换并运行定向回归。
