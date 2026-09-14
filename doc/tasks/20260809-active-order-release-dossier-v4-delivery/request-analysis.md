# Request Analysis

## User Goal

主 Agent 按 V4 开发计划启动 A1-A6 六个子 Agent，分配任务并逐项 review，未满足开发和测试文档时持续返修；全部完成后由主 Agent 集成并运行集成测试。

## Current System

- M0 已通过，冻结接口、blocker、writer、fixture、运行时顺序、事务、hash 和 RELEASE_APPROVE 契约。
- 现有申请接口、申请表、前端按钮、请求/业务幂等、eDHR batch open/create、precheck 和 submitForApproval 已存在。
- 当前 apply 未调用三类 writer，成功摘要 `signatureEvidenceCount=0`。
- 当前 PQC `SUBMITTED` 可计入 100%，source hash 未覆盖值/QA/映射/签名，单事务边界可能留下部分生成物。
- 批记录 backfill 已存在；过程检验单和损耗单 writer 及其正式映射尚未实现。
- 历史第一阶段任务已完成静态骨架，但真实 E2E 因 fixture/运行前置未完成。

## Constraints

- V4 和 `doc/tasks/20260809-active-order-release-dossier-m0/m0-contract-freeze.md` 是唯一实现契约。
- 三条配置术语链路必须分离；动态 `formBindings` 不得替代任何正式资料。
- 严格 BDD/TDD：先 RED，再最小 GREEN，再回归。
- E2E 必须走真实页面；API 仅用于最终只读核验。
- 无 fallback、mock success、SQL 直改进度、当前人/当前时间伪造签名。
- 工作区脏且有并发任务；只修改本任务 write scope，不执行 Git 操作。
- 最多 4 个活跃 Agent，按依赖分波运行。

## Unknowns

- 当前本地运行态、测试租户、五类测试账号、签名口令和任务自有成功 fixture 是否齐备，需 A6 按正式规则检查。
- 传统过程检验/损耗报表模板的真实 cell 映射是否已配置，缺失必须由正式 fixture 配置入口补齐或阻塞。
- 当前损耗模板不证明零损耗确认字段；首个成功 fixture 固定使用正损耗。

## Risks

- A2/A3-A5 共享后端服务导致写冲突，必须按包/类隔离并在 A2 集成阶段统一接线。
- writer 返回业务 blocker 与基础设施异常混淆可能掩盖失败。
- batch/form/release 同事务处理不当可能产生部分资料或孤立待办。
- 真实 fixture 若绕过历史页面，会造成静态测试绿但业务验收失败。
- 历史损坏 target 目录会干扰递归扫描；测试限定 `src/main`/`src/test`，不删除非任务产物。

## Validation Surface

- 后端 JUnit、Node 静态合同、schema 静态合同、Maven compile。
- 前端 Node 静态合同、TypeScript 类型检查。
- Playwright：一线生产、生产组长、一线 PQC、PQC 组长、生产负责人真实路径。
- 最终只读：batch execution、三类 execution、field audit、signature evidence、release transaction/event/work task。

## Blocking Prerequisites

当前无开发阶段 blocker。真实 E2E 若缺运行服务、测试租户/账号/签名/模板/映射，必须按项目规则记录精确 blocker，不得降级为 API-only。
