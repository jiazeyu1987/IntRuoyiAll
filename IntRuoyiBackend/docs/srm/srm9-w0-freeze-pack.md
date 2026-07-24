# SRM srm9 W0 冻结包

## 1. 使用目的

本文件是 `srm9` worktree 中 SRM 后续开发的正式前置冻结包。它把当前文档规划拆成可由业务、接口方、测试和开发共同确认的输入表。

任何 W1-W5 生产代码任务启动前，必须先让对应冻结包从 `BLOCKED` 变为 `FROZEN`。否则不得写接口假实现、默认成功、静态看板、mock 联通或受控模拟改名。

按可落地程度排序的执行计划见 `docs/srm/srm9-landing-plan.md`。本文件负责冻结状态和输入模板，落地计划负责说明先做哪一类任务、何时可以开代码、如何测试和如何避免重复开发。

## 2. 总体门禁

| 冻结包 | 覆盖需求 | 当前状态 | 当前允许动作 | 禁止动作 |
| --- | --- | --- | --- | --- |
| W0-01 主数据集成 | `1/2` | `BLOCKED` | 复用已确认的金蝶基础连接/读同步证据，继续补企业信息库与 SRM 供应商写回 K3 的字段、样本、失败策略 | 重复开发金蝶基础连接，或开发 SRM 供应商写回成功路径 |
| W0-02 询比价规则 | `4` | `BLOCKED` | 补模具价、阶梯价、审批规则样例 | 先建价格模型或审批流 |
| W0-03 绩效与看板 | `3/12` | `BLOCKED` | 补指标、权重、阈值、数据源、角色和移动端去向 | 静态图表、硬编码分数、截图看板 |
| W0-04 委外真实联通 | `7/8/9/10` | `BLOCKED` | 补 PDA/仓储/物流/质检/对账接口契约 | 把受控模拟改名为真实联通 |
| W0-05 付款真实联通 | `11` | `BLOCKED` | 补签章/财务接口、凭据、回执、失败策略 | 默认财务推送成功或跳过签章 |
| W0-06 回归基线 | `5/6` 和既有主链 | `FROZEN` | 作为回归保护输入 | 重复开发已符合主链 |

## 3. W0-01 主数据集成冻结

### 当前结论

`BLOCKED`。企业信息库供应商、授权方式、字段映射未冻结；主系统已确认存在金蝶基础连接配置、账套配置、既有读同步水位和供应商映射记录，但 SRM 供应商主数据写回 K3 的可执行 `BD_Supplier.Save` 字段契约仍未冻结。2026-06-22 已在授权 K3 测试账套上多轮最小保存探针，仍因“创建组织/使用组织必填”失败，说明 K3 写回成功路径暂不可开发。

### admin NAS 只读发现证据

2026-06-21 已按用户补充说明，通过主系统 `芋道源码/admin` 真实登录后进入 `/system/nas` 做只读发现，不保存配置、不测试连接、不转移、不导入、不下载、不读取文件内容。

| 检查项 | 证据结论 | 对 W0-01 的影响 |
| --- | --- | --- |
| 主系统登录 | `芋道源码/admin` 登录成功，tenant-id 为 `1` | 证明 admin 只读发现路径可用 |
| NAS 配置 | NAS 配置完整且根目录可读；未在文档中记录服务器、账号、密码、token 或外部端点 | 证明资料源可访问，不等于 K3 写回契约已冻结 |
| NAS 根目录 | 根目录返回 `9` 个目录；只读扫描访问 `400` 个目录，`3` 个目录因 NAS 权限拒绝 | 证明存在可浏览真实资料，但仍有权限边界 |
| K3/ERP 命中 | 发现的 `K3/ERP` 命中来自产品编码、工艺/采购技术文件名 | 不能作为供应商主数据写回契约 |
| SRM K3 写回契约 | 已确认 `BD_Supplier` 保存探针能登录 K3，但多轮最小保存仍因创建/使用组织字段契约未解而失败 | W0-01 保持 `BLOCKED`，不得启动 K3 同步成功路径开发 |

结论：NAS 入口和真实资料源已确认可访问，但本次只读发现没有冻结 SRM 供应商写回 K3 的目标对象、字段、样本和错误策略。后续若业务方指出具体 NAS 目录或文件，应先做只读定位和脱敏摘录，再把 W0-01 对应行逐项改为 `FROZEN`。

### admin ERP 金蝶配置与同步历史只读证据

2026-06-21 按用户补充说明使用主系统 `芋道源码/admin` 继续做只读验证：打开 `/erp/kingdee-config`，读取 `/admin-api/erp/kingdee-config/get`，并用真实库只读聚合查询同步记录。证据文件为 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\srm9-admin-erp-kingdee-config-readonly-2026-06-21T13-40-21-692Z.json`。该证据未输出密码、token、外部端点 host、NAS 凭据或供应商明细。

| 检查项 | 证据结论 | 对 W0-01 的影响 |
| --- | --- | --- |
| 金蝶配置页 | `/erp/kingdee-config` 可由 `芋道源码/admin` 打开，`/erp/kingdee-config/get` 返回成功 | 证明金蝶基础连接配置入口可复用，不需要重复开发配置页 |
| 金蝶必填配置 | 基础地址、账套、用户名、密码、LCID、产品/生产/BOM/采购/销售同步配置均存在，敏感值已脱敏 | 证明“登录方式/基础连接”已存在，但不证明供应商写回授权 |
| 供应商映射记录 | `erp_kingdee_supplier_sync_record` 在 admin 租户有 `98` 条不同金蝶供应商号映射 | 证明已有 K3 采购同步产生的供应商映射，可作为复用输入 |
| 采购同步记录 | `erp_kingdee_purchase_order_sync_record` 在 admin 租户有 `297` 条 `PUR_PurchaseOrder` 成功同步记录 | 证明已有 K3 -> 本系统采购读同步历史 |
| 同步水位 | `erp_kingdee_sync_run` 和 `erp_kingdee_sync_watermark` 覆盖 `BOM/PRODUCT/PRODUCTION_ORDER/PURCHASE_ORDER/SALE_ORDER/STOCK` 等读同步类型 | 证明已有金蝶读同步运行态，避免重复开发基础读同步 |
| 供应商写回能力 | 代码和表结构未发现 SRM 供应商主数据写回 K3 的保存实现、写回记录表或 `BD_Supplier` 写入契约 | W0-01 仍保持 `BLOCKED`，不得宣称 K3 供应商同步已可 E2E |

### 测试租户最近似候选样本（仅用于 SRM 侧只读页面验证）

当用户无法提供精确 K3 写回样本时，当前测试阶段允许选取最接近真实业务的已存在样本做只读页面验证，但它仍然不是 K3 写回冻结样本，也不能解除 W0-01 阻塞。

本轮选择供应商 ID `103`、供应商名称 `山东瑛泰医疗器械有限公司`、金蝶来源编号 `INT-010` 作为最近似候选样本。

| 项目 | 候选值 | 说明 |
| --- | --- | --- |
| 租户 | `测试租户` / `tenant_id=122` | 仅用于本机测试租户只读复验 |
| 供应商 ID | `103` | 当前最像的真实供应商样本 |
| 供应商名称 | `山东瑛泰医疗器械有限公司` | 与金蝶映射和 SRM 准入同时存在 |
| 金蝶来源编号 | `INT-010` | 真实库已有 `erp_kingdee_supplier_sync_record` 映射 |
| SRM 门户状态 | `APPROVED` | 通过真实门户审核台完成补齐 |
| SRM 准入状态 | `APPROVED` | 通过真实资格校验，页面可继续后续链路 |
| 风险状态 | `0` 个未处理高风险 | 仅说明当前页面较干净，不代表外部契约已冻结 |

2026-06-22 已用 Playwright 真实打开 `http://127.0.0.1:8081`，登录 `测试租户/aoteman`，在 `/srm/supplier-portal-review` 审核通过 `103` 的门户申请，并在 `/srm/supplier/access` 执行资格校验返回 `eligible=true`。证据文件为 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\srm9-candidate-103-portal-approval-e2e-2026-06-21T16-49-15-646Z.json`。

结论：这条候选样本已经可以证明 SRM 门户审核、准入档案和资格校验的真实页面链路可走通，但仍不能把它当作 K3 供应商主数据写回样本，更不能用来证明 W1 已可开发。

### SRM 侧可走通候选样本（已完成 Playwright 只读验证）

继续查真实库后确认，当前测试租户已经具备一条同时满足“金蝶映射、门户审核通过、准入通过、风险干净”的可走通样本：103。108 仍保留为无金蝶映射的页面链路对照样本。

本轮实际只读 E2E 使用供应商 ID `108`、供应商名称 `SRM Portal E2E 20260620183546`。

| 项目 | 候选值 | 说明 |
| --- | --- | --- |
| 租户 | `测试租户` / `tenant_id=122` | 本机主系统只读验证 |
| 供应商 ID | `108` | 实际通过 SRM 准入页只读 E2E 的样本 |
| 供应商名称 | `SRM Portal E2E 20260620183546` | 由现有门户申请链路产生 |
| 门户申请 | `APPROVED` | 由真实审核台完成 |
| SRM 准入状态 | `APPROVED` / `enabled=true` | 页面显示已通过且启用 |
| 风险状态 | `0` 个未处理高风险 | 资格校验通过 |
| 金蝶映射 | 无金蝶映射 | 不能作为 K3 写回样本 |

2026-06-21 已用 Playwright 真实打开 `http://localhost:8081`，登录 `测试租户/aoteman`，在 `/srm/supplier/access` 搜索 `108 / SRM Portal E2E 20260620183546`，执行资格校验并进入档案页，结果通过。证据文件为 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\srm9-portal-ready-readonly-2026-06-21T14-29-36-448Z.json`。

### 必填冻结项

| 项目 | 冻结要求 | 状态 |
| --- | --- | --- |
| 企业信息库供应商 | 明确供应商、环境、网络、授权方式 | `BLOCKED` |
| 企业信息库查询键 | 明确统一社会信用代码、企业名称或其他唯一键 | `BLOCKED` |
| 企业信息库字段映射 | 明确 SRM 字段、外部字段、类型、必填、枚举 | `BLOCKED` |
| 企业信息库失败策略 | 明确超时、无结果、字段缺失、授权失败行为 | `BLOCKED` |
| K3 产品线/版本 | K3Cloud 基础连接和读同步已确认；仍需确认供应商写回接口包和版本差异 | `BLOCKED` |
| K3 账套/测试环境 | 账套配置已确认存在；仍需确认供应商主数据测试写入授权和可回读样本 | `BLOCKED` |
| K3 FormId | 明确供应商主数据或其他目标业务对象 FormId | `BLOCKED` |
| K3 写入字段映射 | 明确 SRM 字段、K3 字段、类型、必填、枚举 | `BLOCKED` |
| K3 幂等与重推 | 明确幂等键、重试次数、人工重推入口和责任人 | `BLOCKED` |

### 字段映射模板

| SRM 字段 | 外部字段 | 方向 | 类型 | 必填 | 枚举/格式 | 来源 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 待填写 | 待填写 | 企业信息库 -> SRM | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| 待填写 | 待填写 | SRM -> K3 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

### 进入 W1 条件

- 字段映射表完成且经业务和接口方确认。
- 企业信息库和 K3 都有测试环境、账号、样本和错误返回。
- 复用现有 ERP 金蝶配置和读同步能力时，必须明确哪些数据来自既有 `PUR_PurchaseOrder` 读同步，哪些是本次新增 SRM 供应商写回。
- 页面提示、接口返回、日志和人工处置策略完整。

## 4. W0-02 询比价规则冻结

### 当前结论

`BLOCKED`。模具价适用范围、阶梯价区间规则、比价表审批流和审批拒绝后的成交处理未冻结。

### 必填冻结项

| 项目 | 冻结要求 | 状态 |
| --- | --- | --- |
| 模具价适用范围 | 明确物料、项目、一次性/分摊、币种和税率 | `BLOCKED` |
| 阶梯价区间 | 明确数量区间、单位、边界包含关系和重叠处理 | `BLOCKED` |
| 报价附件规则 | 明确必须上传的附件类型、大小和审批可见性 | `BLOCKED` |
| 自动比价口径 | 明确含税/未税、运费、模具价、阶梯价参与方式 | `BLOCKED` |
| 比价表审批流 | 明确审批节点、角色、拒绝、退回和撤回规则 | `BLOCKED` |
| 成交阻断规则 | 明确审批未通过时能否定标、下单或生成合同 | `BLOCKED` |

### 规则样例模板

| 场景 | 输入 | 期望比价结果 | 审批要求 | 异常处理 |
| --- | --- | --- | --- | --- |
| 模具价一次性 | 待填写 | 待填写 | 待填写 | 待填写 |
| 模具价按数量分摊 | 待填写 | 待填写 | 待填写 | 待填写 |
| 阶梯价不重叠 | 待填写 | 待填写 | 待填写 | 待填写 |
| 阶梯价重叠/缺口 | 待填写 | 待填写 | 待填写 | 待填写 |

### 进入 W2 条件

- 模具价、阶梯价和审批口径均有业务确认样例。
- 拒绝、退回、撤回、审批未完成的阻断规则已确认。

## 5. W0-03 绩效与看板指标冻结

### 当前结论

`BLOCKED`。月度评分指标、权重、阈值、年度分级、管理层/采购员指标和移动端载体均未冻结。

### 必填冻结项

| 项目 | 冻结要求 | 状态 |
| --- | --- | --- |
| 月度评分指标 | 明确交付、质量、响应、价格、信用等指标定义 | `BLOCKED` |
| 指标权重 | 明确每项权重、总分公式、缺失数据处理 | `BLOCKED` |
| 年度分级阈值 | 明确 A/B/C 或优胜劣汰阈值和审批规则 | `BLOCKED` |
| 动态预警规则 | 明确触发条件、预警等级、责任人和关闭条件 | `BLOCKED` |
| 采购员看板指标 | 明确待处理订单、待确认交期、逾期列表等口径 | `BLOCKED` |
| 管理层看板指标 | 明确采购金额、准时率、合格率、成本节约口径 | `BLOCKED` |
| 移动端载体 | 明确 Web 响应式、独立移动端或延期独立立项 | `BLOCKED` |

### 指标字典模板

| 指标 | 定义 | 数据源 | 计算公式 | 刷新频率 | 权限范围 | 空数据策略 |
| --- | --- | --- | --- | --- | --- | --- |
| 待填写 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

### 进入 W3 条件

- 指标字典、权重、阈值、数据源和权限全部确认。
- 移动端支持范围已明确。
- 空数据、缺失数据和异常数据展示规则已确认。

## 6. W0-04 委外真实联通冻结

### 当前结论

`BLOCKED`。真实 PDA、仓储、物流、质检、对账权威来源和差异确认规则未冻结。

### 必填冻结项

| 项目 | 冻结要求 | 状态 |
| --- | --- | --- |
| PDA 发料接口 | 明确端点、认证、请求/响应、错误码、幂等键 | `BLOCKED` |
| 库存扣减接口 | 明确权威系统、扣减时机、回滚策略、库存不足处理 | `BLOCKED` |
| 仓储收货接口 | 明确收货状态、数量、批次、时间、异常回写 | `BLOCKED` |
| 物流状态来源 | 明确供应商、物流接口或仓储接口的权威优先级 | `BLOCKED` |
| 质检移动端入口 | 明确质检系统、移动端认证、检验结果枚举 | `BLOCKED` |
| 退货闭环规则 | 明确退货单、补货、扣款、让步接收分支 | `BLOCKED` |
| 对账权威数据源 | 明确采购订单价、入库数量、合格数量来源 | `BLOCKED` |
| 差异确认流程 | 明确供应商确认、采购复核、争议关闭规则 | `BLOCKED` |

### 状态映射模板

| 外部系统 | 外部状态/事件 | SRM 状态 | 是否阻断下游 | 失败提示 | 备注 |
| --- | --- | --- | --- | --- | --- |
| PDA | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| 仓储 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| 质检 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| 对账 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

### 进入 W4 条件

- 所有接口协议和错误码已确认。
- 外部系统测试环境、账号和样本已确认。
- 受控模拟状态和真实联通状态在 UI、日志、验收证据中可区分。

## 7. W0-05 付款真实联通冻结

### 当前结论

`BLOCKED`。电子签章平台、财务接口、审批凭据、真实回执字段和失败重试策略未冻结。

### 必填冻结项

| 项目 | 冻结要求 | 状态 |
| --- | --- | --- |
| 电子签章平台 | 明确平台名称、环境、认证方式、签章凭据 | `BLOCKED` |
| 签章审批链 | 明确采购主管、生产总监、财务主管/经理等节点 | `BLOCKED` |
| 财务系统接口 | 明确端点、认证、请求/响应、回执字段、错误码 | `BLOCKED` |
| 付款计划来源 | 明确合同、付款条款、月结账期和比例规则 | `BLOCKED` |
| 失败重试策略 | 明确自动/人工重推、次数、间隔、责任人 | `BLOCKED` |
| 打印/归档规则 | 明确是否仍需打印、签章文件归档和审计留痕 | `BLOCKED` |

### 回执字段模板

| 字段 | 来源 | 类型 | 必填 | 成功示例 | 失败示例 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 待填写 | 签章平台 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |
| 待填写 | 财务系统 | 待填写 | 待填写 | 待填写 | 待填写 | 待填写 |

### 进入 W5 条件

- 签章和财务接口测试环境、凭据、字段和错误码全部确认。
- 审批节点、拒绝、撤回、重推和归档规则全部确认。
- 付款成功、失败和部分失败状态可在页面显式展示。

## 8. W0-06 回归基线冻结

### 当前结论

`FROZEN`。本项只作为回归保护输入，不作为新增开发范围。

### 后端回归命令

```powershell
mvn -pl yudao-module-srm -Dtest=SrmSupplierAccessRiskServiceTest,SrmSupplierPortalApplicationServiceTest,SrmNonBiddingProcurementServiceTest,SrmPurchaseOrderServiceTest,SrmOutsourceExecutionServiceTest,SrmPaymentExecutionServiceTest test
```

### 前端与 E2E 回归入口

| 范围 | 脚本 |
| --- | --- |
| 供应商门户 | `node tests/e2e/srm/supplier-portal-real-flow.e2e.js` |
| Phase 1 | `node tests/e2e/srm/supplier-phase1-real-flow.e2e.js` |
| Phase 1 admin 只读 | `node tests/e2e/srm/supplier-phase1-admin-readonly.e2e.js` |
| 非招标 | `npx playwright test tests/e2e/srm/non-bidding.spec.ts --project=chromium` |
| 非招标 admin 只读 | `npx playwright test tests/e2e/srm/non-bidding-admin-readonly.spec.ts --project=chromium` |
| 采购计划 | `npx playwright test tests/e2e/srm/procurement-plan.spec.ts --project=chromium` |
| 采购订单 | `node tests/e2e/srm/purchase-order-real-flow.e2e.js` |
| 订单变更 | `node tests/e2e/srm/purchase-order-change-real-flow.e2e.js` |
| Phase45 受控模拟 | `node tests/e2e/srm/phase45-simulated-real-flow.e2e.js` |

### 样本治理

| 项目 | 规则 |
| --- | --- |
| 写入租户 | 默认仅 `测试租户/aoteman/111111` |
| 只读复验 | `芋道源码/admin`，不得写入 |
| 样本占用 | 每次实现任务开始前刷新样本编号、状态和是否可复用 |
| 端口 | worktree 任务不得默认复用 `8081/48081` |
| 模拟标识 | Phase45 未真实联通前必须保留受控模拟标识 |

## 9. 下一步

业务/接口方按本文件补齐任一冻结包后，执行者需先更新对应状态为 `FROZEN`，再创建对应 W1-W5 实现任务。未达到 `FROZEN` 时继续编码应视为错误需求。

依赖和 blocker 获取清单见：

- `docs/srm/srm9-landing-plan.md`
- `docs/dependencies/srm9-blocker-manifest.json`
- `docs/dependencies/dependency-inventory.md`
- `docs/dependencies/launch-blockers.md`

## 10. 可执行门禁

本 worktree 提供 W0 冻结检查脚本：

```powershell
python -X utf8 script/srm/check_srm_w0_freeze_gate.py --freeze-pack docs/srm/srm9-w0-freeze-pack.md --wave W1
```

返回规则：

| 返回码 | 含义 |
| --- | --- |
| `0` | 目标波次对应冻结包为 `FROZEN`，或 W0 文件可读 |
| `1` | 目标波次对应冻结包仍为 `BLOCKED` 或 `DEFERRED` |
| `2` | 冻结包缺失、波次不支持或状态不可解析 |

当前示例：

- `--wave W1` -> `W1 blocked: W0-01 is BLOCKED`
- `--wave W0` -> `W0 freeze pack readable`
- `--wave W0-06` -> `W0-06 allowed: W0-06 is FROZEN`

机器 blocker manifest 一致性检查：

```powershell
python -X utf8 script/srm/check_srm_blocker_manifest.py --manifest docs/dependencies/srm9-blocker-manifest.json --freeze-pack docs/srm/srm9-w0-freeze-pack.md --landing-plan docs/srm/srm9-landing-plan.md
```
