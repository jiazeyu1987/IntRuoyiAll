# eDHR 可视化填写配置设计

## 1. 设计目标

在现有批记录表单列表中，将“单元格规则”入口收敛为一个“填写配置”入口，让模板管理员直接基于原表完成四类操作：

1. 指定哪些单元格可填写，并纠正识别错误的文本、数字、日期、签名和下拉框类型。
2. 指定哪些可填写单元格在辅助模式中组成同一行。
3. 为每个辅助行填写一条清晰的操作描述。
4. 将每个辅助行分配给一个员工或一个现有角色候选来源。

本设计只形成实现前的 BDD、严格 TDD 和验收依据，不修改生产代码。

## 2. 最小产品决策

### 2.1 配置入口

- 复用 `BatchRecordCellRulesConfirmDialog.vue` 的原表只读预览、单元格选择和规则编辑能力。
- 将列表中的“规则”操作改名为“填写配置”，不新增独立菜单、独立页面或第二套模板设计器。
- 配置弹窗左侧继续显示原表，右侧增加“辅助行”区域，并保留选中单元格的类型配置。
- 不引入连线画布、自由拖拽编排、流程节点或复杂权限矩阵。

### 2.2 辅助行是唯一责任单元

- 一个可填写单元格必须且只能属于一个辅助行。
- 一个辅助行可以包含一个或多个可填写单元格，单元格不要求在原表中物理相邻。
- 同一辅助行共用一条描述和一组填写人候选规则。
- 若只需把一个单元格交给一个员工，就创建仅包含该单元格的一行。
- 不再设计“辅助行负责人”和“单元格负责人”两层覆盖关系，避免冲突和冗余。

### 2.3 简单操作流程

1. 管理员打开“填写配置”。
2. 点击左侧原表单元格，在右侧纠正字段类型和控件参数。
3. 点击“新建辅助行”，输入行描述和选择员工或角色。
4. 在选行状态下点击左侧可填写单元格，将其加入或移出当前辅助行。
5. 若单元格已属于其他行，界面明确提示“移动到当前行”；确认后完成移动，不允许重复归属。
6. 点击辅助行时，左侧高亮该行全部单元格；点击已映射单元格时，右侧定位对应辅助行。
7. 点击一次“保存填写配置”，界面依次保存现有单元格规则/辅助行和现有填写人规则；任一步失败都显示真实错误且不提示成功。

辅助行顺序使用数组中的 `sort` 值，通过“上移/下移”调整；首版不提供自由拖拽。

## 3. 复用现有能力

| 需求 | 复用点 | 最小扩展 |
| --- | --- | --- |
| 原表可视化选择 | `BatchRecordCellRulesConfirmDialog.vue` | 增加辅助行选中态和映射高亮 |
| 单元格规则 | `GET/PUT /mes/pro/batch-record-report/cell-rules`、`BatchRecordReportCellRuleVO`、`edhrCellRule` | 请求/响应增加 `assistRows` |
| 文本/数字/日期 | 现有 `STRING/NUMBER/DATE/DATETIME` 和控件标记 | 不新增值类型 |
| 签名 | 现有 `SIGNATURE` 与 `edhrSignature` | 在同一侧栏复用签名标记配置 |
| 下拉框 | `STRING`、选项控件、`constraints.selectionMode/options` | 在现有规则编辑器增加选项维护 UI |
| 员工/角色候选 | `get-by-report`、`save-by-report` 和现有候选来源解析 | 单条 `fillRule` 扩展为按 `scopeKey` 的 `fillAssignments` |
| 运行态任务 | 现有一个表单填写工作任务和 `BATCH_SHARED` | 工作任务增加不可变责任范围快照 |
| 字段权限 | 现有 `fillableScopeJson` 与字段审计写入校验 | 范围语法增加精确单元格坐标 |
| 辅助模式值 | `ExecutionPage.vue` 的 `draftFieldValues` | 按快照 `assistRows` 分组，不新增草稿对象 |
| 历史追溯 | 现有执行快照、字段审计链 | 执行快照增加 `assistRows` |

## 4. 配置模型

### 4.1 辅助行

辅助行保存在当前报表 JSON 根节点，不新增辅助布局表：

```json
{
  "edhrAssistRows": [
    {
      "rowKey": "AR_001",
      "description": "填写生产批号和实际数量",
      "sort": 1,
      "fields": [
        {
          "rowIndex": 4,
          "columnIndex": 2
        },
        {
          "rowIndex": 4,
          "columnIndex": 4
        }
      ]
    }
  ]
}
```

约束：

- `rowKey` 在一个报表版本内唯一且保存后稳定，移动、改名和排序不改变 `rowKey`。
- `description` 去除首尾空格后必须非空。
- `fields` 至少包含一个坐标，且坐标必须指向当前报表中的可填写单元格。
- 同一坐标不能出现在两个辅助行中。
- 每个可填写单元格都必须被一个辅助行覆盖，才能通过发布门禁。

### 4.2 单元格规则

继续使用 `BatchRecordReportCellRuleVO`：

- 文本：`valueType=STRING`、`componentFlag=input-text`。
- 数字：`valueType=NUMBER`、`componentFlag=input-number`。
- 日期：`valueType=DATE`、`componentFlag=date`。
- 日期时间：`valueType=DATETIME`、`componentFlag=datetime`。
- 签名：`valueType=SIGNATURE`、`componentFlag=signature`，并要求同坐标存在启用的 `edhrSignature`。
- 下拉框：`valueType=STRING`，使用现有选择控件标记，`constraints.selectionMode=single`，`constraints.options=[{label,value}]`。

下拉框至少保留两个 `label`、`value` 均非空且 `value` 唯一的选项。选项顺序即运行态展示顺序。

### 4.3 填写责任

继续使用 `mes_pro_edhr_process_form_permission_rule`，不新增平行权限表。最小新增字段：

- `scope_key`：辅助行 `rowKey`；旧全表规则迁移后使用 `ALL`。
- `fillable_scope_json`：服务端根据 `edhrAssistRows` 生成的精确单元格范围，客户端不重复提交坐标。

`save-by-report` 的单条 `fillRule` 扩展为：

```json
{
  "batchRecordReportId": "REPORT-001",
  "fillAssignments": [
    {
      "scopeKey": "AR_001",
      "candidateSourceType": "USER",
      "candidateSourceIds": [1001],
      "completionPolicy": "ANY_ONE",
      "dueMinutes": 120,
      "enabled": true,
      "remark": "生产批号与数量填写"
    }
  ]
}
```

服务端必须从当前报表版本的 `edhrAssistRows` 解析 `scopeKey` 并生成 `fillable_scope_json`。请求中不接收客户端提供的坐标，避免布局和权限保存两份坐标。

### 4.4 精确填写范围

现有路线/批次的行范围仍是外层业务边界。辅助行责任范围使用 `schemaVersion=2` 的精确单元格：

```json
{
  "schemaVersion": 2,
  "cells": [
    {
      "sourceTableIndex": 0,
      "rowIndex": 4,
      "columnIndex": 2
    }
  ]
}
```

当前用户的有效范围为：

`路线或批次范围 ∩ 当前用户责任行范围`

后端字段写入校验必须同时比较 `sourceTableIndex`、`rowIndex`、`columnIndex`，不能只按行放行。

## 5. API 变化

### 5.1 单元格规则接口

`BatchRecordReportCellRulesReqVO/RespVO` 增加 `assistRows`。`PUT /cell-rules` 同时校验并保存：

- 单元格规则；
- 辅助行结构；
- 单元格唯一归属；
- 可填写单元格覆盖率；
- 签名和下拉框约束。

保存后 `GET /cell-rules` 必须原样读回人工规则和辅助行，不重新生成 `rowKey`。

### 5.2 填写人接口

`get-by-report` 响应增加 `fillAssignments`，`save-by-report` 请求接受 `fillAssignments`。不允许同一个请求同时出现旧 `fillRule` 和新 `fillAssignments`。

保存时必须校验：

- `scopeKey` 存在且唯一；
- 每个辅助行恰好有一条启用的责任配置；
- 用户或角色存在且启用；
- 候选来源能解析出至少一个有效用户；
- 服务端生成的坐标范围非空。

### 5.3 一次保存的失败语义

首版不新增聚合配置 API。前端“保存填写配置”按固定顺序调用两个现有接口：

1. 保存单元格规则和辅助行。
2. 保存辅助行填写责任。

第二步失败时不得回报整体成功；弹窗保持打开并显示“辅助行已保存，填写人保存失败”的明确状态。发布门禁会阻止缺少责任覆盖的版本进入运行态，管理员修正后可再次保存。

## 6. 运行态与快照

### 6.1 工作任务

- 继续为一个表单创建一个填写工作任务，不按辅助行拆成多个任务。
- 工作任务候选用户是所有启用辅助行候选用户的并集。
- `mes_pro_edhr_work_task` 增加 `responsibility_scope_json`，保存创建任务时解析出的不可变责任快照。
- 快照包含 `scopeKey`、候选来源、已解析用户 ID 和精确单元格坐标。
- 打开任务时，后端根据当前用户和责任快照生成现有响应字段 `fillableScopeJson`。
- 保存字段时，后端再次根据同一责任快照校验，不信任前端传回的可写状态。

### 6.2 执行快照

现有执行快照在 `fields` 旁增加 `assistRows`：

```json
{
  "fields": [],
  "assistRows": [
    {
      "rowKey": "AR_001",
      "description": "填写生产批号和实际数量",
      "sort": 1,
      "fieldIdentities": [
        "0:4:2",
        "0:4:4"
      ]
    }
  ]
}
```

- 执行创建后，原表和辅助模式只读取执行快照。
- 当前模板的后续编辑不得改变已创建执行。
- 快照缺少 `assistRows` 时，不从 `fields` 临时推导；辅助模式显示“未配置辅助模式”，原表模式按已冻结的字段和权限继续工作。

### 6.3 两种模式

- 辅助模式只展示当前用户责任范围内的辅助行。
- 原表模式保留完整表格用于上下文，但其他员工负责的单元格只读。
- 两种模式继续读写同一个 `draftFieldValues`，并通过同一字段审计和电子签名入口保存。
- 切换模式不复制、不转换、不另存字段值。

## 7. Fail-Fast 门禁

保存或发布必须拒绝以下配置：

- 可填写单元格未映射到辅助行。
- 单元格映射到多个辅助行。
- `rowKey` 缺失或重复。
- 辅助行描述为空。
- 辅助行没有单元格。
- 坐标越界、坐标不存在或指向不可填写单元格。
- 下拉框少于两个有效选项、选项值为空或重复。
- 签名类型没有启用的签名标记。
- 辅助行没有启用的员工/角色责任配置。
- 员工或角色不存在、停用或不能解析出有效用户。
- 同时提交旧 `fillRule` 和新 `fillAssignments`。
- 运行态责任快照缺失、损坏或与执行/任务不匹配。
- 当前用户写入不在有效精确单元格范围内。

所有失败必须返回可定位到 `rowKey` 或单元格坐标的错误，不吞异常、不自动改成其他类型、不自动扩大填写范围。

## 8. 版本与迁移

- 辅助行和单元格规则属于批记录报表版本内容，随版本复制、确认、审批和发布。
- 新版本可继承上一版本配置，但继承后的配置必须重新通过坐标、签名、候选人和覆盖率校验。
- 已发布版本和历史执行快照不可被当前草稿配置更新。
- 旧单条 `fillRule` 通过一次性迁移转换为 `scopeKey=ALL` 的显式配置。
- 迁移脚本从对应不可变版本的可填写字段生成精确 `cells`；不能在请求处理或任务打开时动态回退推导。
- 没有 `assistRows` 的旧版本不显示辅助模式，直到管理员在新草稿版本中完成辅助行配置。
- 迁移完成并验证后，运行时代码只接受新结构，不保留双读兼容分支。

## 9. 非目标

- 不设计跨报表的辅助行。
- 不支持一个单元格同时由多人分别填写。
- 不支持辅助行内再定义单元格级责任覆盖。
- 不新增通用表单设计器、通用权限引擎或独立辅助模式数据库。
- 不在首版提供拖拽连线、批量公式、条件显示、动态重复行或自动 AI 重排。
- 不改变现有字段值、字段审计、提交审批和电子签名主链路。

## 10. 实施边界

实现应按严格 TDD 分为以下最小增量：

1. 扩展并验证辅助行 JSON 模型。
2. 扩展单元格规则接口并完成保存读回。
3. 扩展按辅助行的填写责任接口与数据迁移。
4. 生成任务责任快照并实施精确到列的后端授权。
5. 改造现有配置弹窗。
6. 使用执行快照渲染当前用户辅助行。
7. 完成真实用户路径 E2E 和版本/历史回归。

每个增量必须先出现符合预期原因的 RED，再实现最小生产行为并取得 GREEN。
