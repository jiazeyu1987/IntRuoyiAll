# 菜单名称去重

本次覆盖本地菜单审计发现的24组重名，调整28个既有目录或页面名称。隐藏菜单包含在内；不合格评审保持原名。所有新名称均为4字。

保留业务含义明确的通用名称，其他按业务对象区分；父子目录采用不同名称。仅修改名称，不改变菜单ID、层级、路由、组件、权限、可见性、角色或租户套餐授权。

| 原位置 | 新名称 |
|---|---|
| 公众号管理 / 数据统计 | 运营统计 |
| 公众号管理 / 菜单管理 | 微信菜单 |
| CRM 系统 / 产品管理 | 销售产品 |
| AI 大模型 / 控制台 / 工具管理 | 模型工具 |
| IoT 物联网 / 设备接入 / 产品管理 | 物联产品 |
| IoT 物联网 / 设备接入 / 设备管理 | 物联设备 |
| IoT 物联网 / 设备接入 / 产品分类 | 物联分类 |
| MES 系统 / 基础数据 | 制造档案 |
| MES 系统 / 基础数据 / 客户管理 | 客户档案 |
| MES 系统 / 基础数据 / 供应商管理 | 供方档案 |
| MES 系统 / 仓库管理 / 采购入库 | 物料入库 |
| MES 系统 / 仓库管理 / 采购退货 | 物料退货 |
| MES 系统 / 仓库管理 / 销售退货 | 产品退货 |
| MES 系统 / 仓库管理 / 库存盘点 | 仓储盘点 |
| MES 系统 / 仓库管理 / 销售出库 | 产品出库 |
| ERP 系统 / 配置管理 | 金蝶配置 |
| ERP 系统 / 生产管理 | 生产业务 |
| WMS 系统 / 基础数据 | 仓储档案 |
| 文控中心 / 受控浏览 | 文控审批 |
| eDHR批记录 / 生产执行 / 过程检验 | 检验填报 |
| 展厅 / 产品管理 | 展品管理 |
| 展厅 / 审批中心 | 展厅审批 |
| SRM / 基础配置 | 供应配置 |
| SRM / 基础配置 / 编码规则 | 供应编码 |
| SRM / 供应商管理 | 供方管理 |
| eDHR批记录 / 工艺流程 | 工艺路线 |
| eDHR批记录 / 表单追溯 | 追溯管理 |
| eDHR批记录 / 生产表单 / 模板管理 | 表单模板 |

迁移脚本：`IntRuoyiBackend/sql/mysql/20261119_unique_navigation_names.sql`。迁移前校验菜单身份及旧名称，迁移后检查全部有效目录、页面不再同名；按钮权限不纳入页签重名统计。

## 本地核验结果

- 已执行迁移，28个名称更新成功；核对460个未删除目录、页面，重名数量为0，所改名称均为4字。
- 菜单身份、层级、路由、组件、权限、显示范围及角色、租户套餐授权均保持不变；不合格评审保持原名。
- 8个修改的Vue/TypeScript文件编译检查通过；菜单迁移门禁通过；新迁移及79个依赖脚本的元数据、依赖闭包校验通过。
- 重新登录后，展品管理、工艺路线/工艺流程、追溯管理/历史追溯的跳转、标题和面包屑核验通过。当前账号无可见入口的其余改名项以数据库核验为准。
- 业务数据验收未通过：历史追溯、列表配置及工作流交互日志等请求出现HTTP 500。名称核验通过不代表业务数据加载通过。
- 全库发布清单校验受已有迁移`20261116_dcc_merge_duplicate_roles.sql`的`type=role`元数据阻断；本次未修改该脚本。

核验记录：`output/playwright/menu-renaming/deduplication-validation-summary.json`、`deduplication-migration-verification.json`、`deduplication-ui-verification.json`。

## 隐藏路由标题去重

继续清理静态隐藏路由与菜单、其他隐藏入口之间的同名标题，共调整28处，所有新名称均为4字。不合格评审保持原名。

| 路由名称 | 原标题 | 新标题 |
|---|---|---|
| ErpTableAutoSyncConfig | ERP配置 | 同步配置 |
| MesPqcProductionReleaseActionUrl | PQC生产放行 | 放行执行 |
| SystemRoleCompatibilityIndex | 权限角色 | 角色权限 |
| SystemPostCompatibilityIndex | 组织角色 | 岗位角色 |
| SSOLogin | 登录（国际化） | 单点登录 |
| ApprovalCenterFormCenterTemplate | 表单模板 | 审批模板 |
| ApprovalCenterFormCenterPolicy | 表单策略 | 模板策略 |
| ApprovalCenterFormCenterEffect | 生效待处理 | 生效处理 |
| MdmProjectCodeHidden | 项目代码 | 项目维护 |
| NoFound | 404 | 错误页面 |
| /:pathMatch(.*)* | 404 | 页面缺失 |
| BpmModel | 流程模型 | 模型维护 |
| BpmForm | 流程表单 | 表单维护 |
| BpmCategory | 流程分类 | 分类维护 |
| BpmBusinessApprovalPolicy | 业务审批策略 | 审批规则 |
| BpmUserGroup | 用户分组 | 分组维护 |
| BpmProcessExpression | 流程表达式 | 条件表达 |
| BpmProcessInstanceCreate | 发起流程 | 流程申请 |
| DccExternalFileReview | 外来文件评审 | 外来评审 |
| DccExternalFileReviewAlias | 外来文件评审 | 文件评审 |
| DccExternalFileReviewShortAlias | 外来文件评审 | 外件审核 |
| DccControlledFilePositionsCompatibility | 审批角色 | 会签岗位 |
| CrmProductDetail | 产品详情 | 销售详情 |
| AiWorkflowCreate | 设计 AI 工作流 | 智能编排 |
| AiWorkflowUpdate | 设计 AI 工作流 | 编排编辑 |
| IoTProductDetail | 产品详情 | 物联详情 |
| MesProRouteEdit | 工艺流程 | 流程编辑 |
| MesPqcProductionRelease | PQC生产放行 | 放行办理 |

本轮仅修改两个路由文件中的标题字面量；逐字符还原标题后与修改前源码完全一致，确认路径、路由名称、组件、重定向、权限及可见性未改动。数据库无需新迁移，菜单迁移登记已注明具体豁免原因。

复核本轮读取的460个数据库菜单及163个静态页面路由：不同页面地址之间标题重名为0；相同地址的静态路由与数据库菜单视为同一页面，布局容器不计为页面。审批工作台可见入口共用的`tagsViewTitle: 审批中心`属于既有合并页签机制，不在隐藏路由去重范围内。

TypeScript转译及菜单迁移门禁通过。以上为标题及配置核验，未新增业务流程E2E验收结论；前述业务数据HTTP 500和全库发布清单问题未在本轮处理。

核验记录：`output/playwright/menu-renaming/hidden-route-audit-after.json`、`hidden-route-rename-plan.json`、`hidden-route-menu-gate.txt`。
