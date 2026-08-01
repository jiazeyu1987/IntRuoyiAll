# Verification Report

## Scope

修订岗位需求分解矩阵，使系统实现和操作路径复用当前真实系统模块，同时保持原有 8 列结构和业务化描述风格。

## Output

- `E:\IntRuoyi\outputs\019fb812-d0e3-7f20-8895-31a209f54b2e\岗位需求分解矩阵_复用现有模块修订版.xlsx`

## Checks

- Workbook 可读取。
- 主表 `岗位需求分解矩阵` 保持 27 行、8 列。
- 修订行保持业务时间顺序，没有改变职位、任务主线和矩阵结构。
- 系统入口改为复用：MES 生产工单、工序池班组长工作台、ERP 库存调拨或 MES 调拨单列表、一线 PQC 入口、电子批记录放行追溯。
- 旧入口词检查通过：`生产组长工作台`、`PQC 工作台`、`PQC 组长工作台`、`物料调拨关联` 命中数均为 0。

## Result

PASS。


## v2 Checks

- Workbook 可读取。
- 主表 `岗位需求分解矩阵` 保持 27 行、8 列。
- 第 12 行已改为：开工检查由系统展示检查项和阻塞原因，生产班组长自行决定是否异常上报，系统不自动判断、不自动生成异常记录。
- 第 21、22 行已改为：生产订单数量使用 ERP 下达后的固定产品数量，报工分配和进度更新不改变订单产品数量。

## v2 Result

PASS。

## v3 Scope

按用户要求，围绕主流程产生的衍生需求新增第二个 sheet `衍生需求`，覆盖职责目录中列出的班组基础维护、员工/设备/原因/负责范围、QA 规程、PQC 提交与复核、质量异常、日结和审计类需求。

## v3 Output

- `E:\IntRuoyi\outputs\019fb812-d0e3-7f20-8895-31a209f54b2e\岗位需求分解矩阵_复用现有模块修订版_v3.xlsx`

## v3 Checks

- Workbook 可读取。
- 主表 `岗位需求分解矩阵` 保持 27 行、8 列。
- 第二个 sheet `衍生需求` 为 43 行、8 列，列结构为：职位、衍生场景/任务、要干什么（需求）、系统怎么实现、输入什么、输出什么、怎么测试、怎么操作/来源。
- 关键衍生需求抽查通过：员工添加/禁用、设备绑定、设备参数上下限、QA 检验规程、PQC 组长复核、电子签名、历史快照均已写入。
- 表格运行库导入、区域检查、错误扫描和两个 sheet 渲染均通过；未发现公式错误文本。

## v3 Result

PASS。

## Development Plan Scope

按用户要求，将岗位需求分解矩阵中的主流程和衍生需求拆成开发任务表，写入 `development-plan.md`。文档要求第一阶段可并行开发，第二阶段及以后基于前序阶段逐步推进。

## Development Plan Output

- `E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel\development-plan.md`

## Development Plan Checks

- UTF-8 读取通过。
- 文档包含阶段总览、P1 可并行开发任务、P2 核心链路、P3 批记录与过程检验、P4 异常与放行、P5 完善型任务、P6 联调与上线准备。
- P1 明确公共数据契约、订单状态机、活跃订单池、调拨关联、报工、班组基础维护、QA 规程、PQC、批记录正式绑定和权限基础可以并行推进。
- P2-P6 明确依赖前序阶段，并列出每个任务的主要开发内容、输出物、依赖和验收。
- `python -X utf8` 读取当前任务 4 个 Markdown 文件通过；`rg` 阶段标题检查通过；`git diff --check` 未发现 whitespace error。
- 本次只修改当前任务文档，未修改生产代码、数据库、运行态、Excel 源文件或并行任务目录。

## Development Plan Result

PASS。由于当前工作区进入本次任务前已有 unrelated `ahead 1` 和 untracked 并行任务文档，本次未执行提交或推送，任务状态保留为 `ready_for_closeout`。

## Development Plan Optimization Scope

根据逻辑复核结果优化 `development-plan.md`，重点补强阶段依赖、调拨覆盖校验、PQC 跨天和数量变更、批记录字段映射顺序、异常处理责任以及 E2E 阻塞场景。

## Development Plan Optimization Checks

- P1 阶段已明确 `P1.1` 为先行门禁，`P1.2-P1.12` 才是可并行任务。
- QA 规程配置已补充产品、工艺路线、路线版本、工序基础数据和正式批记录绑定依赖。
- 调拨关联已补充多调拨单、分批发货、补料、退料、多批次和调拨覆盖明细。
- PQC 任务生成与数量计算已补充跨天、班次、轮次、订单数量变更、规程版本变更和已提交记录锁定。
- 批记录任务已调整为先定义字段映射和多次报工汇总规则，再执行回填。
- 异常闭环已补充处理责任、处理入口、状态来源和解除条件。
- P6 已扩展缺 SOP、缺生产系数、缺正式批记录绑定、调拨数量/批次不一致、PQC 跨天/数量变更、签名人与实际人员不一致等 E2E 场景。

## Development Plan Optimization Result

PASS。`python -X utf8` 读取通过，关键新增门禁与 E2E 场景检索通过，`git diff --check` 无 whitespace error。优化后文档更接近可执行开发规格，仍未修改生产代码、数据库、运行态或 Excel 源文件。
