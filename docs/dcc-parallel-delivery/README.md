# DCC 四线程修复任务包

版本：1.0，2026-09-30。主管理：本线程。业务依据：[最终需求v1.3](../product/dcc-final-requirements.html)。本包定义后续开发分工，当前仅完成计划与静态Review，不表示四个模块已经修复。

## 1. 四个线程的任务

| 线程 | 任务书 | 范围 | 优先级 |
|---|---|---|---|
| A | [审批与生命周期](task-a-workflow.md) | 会签指派、批准、培训、文控审核、受控、生效、作废终点、下发及日期提醒 | P0 |
| B | [项目与配置](task-b-project.md) | 三组默认属性及申请属性、项目产品、文件夹模板、类型、负责人身份 | P0 |
| C | [版本与占用](task-c-version.md) | 检出检入、版本策略、变更来源、精确同名、保留期名称编号占用 | P1 |
| D | [关联与引用](task-d-relations.md) | 文件选择器、跨项目关联、最新受控解析、关联整改、项目引用及取消 | P1 |
| 主管理 | [Review门禁](review-checklist.md) | 公共模型、接口、公共页面整合、迁移顺序、交叉回归及最终Review | 全程 |

先读[共同契约](shared-contract.md)、[文件归属](ownership.md)、[验证计划](verification-plan.md)及[初始Review](review-report.md)。每份任务书末尾有可直接复制给新线程的启动指令。

## 2. 开工门禁：共同基线

目前目录实际分支为 `int_qms`，不是根据目录名推测的int_main。核对HEAD为 `a9bcb6d36d96145ddc1252f111347b644b328deb`；工作区有385个Git状态条目（236个tracked改动、149个untracked条目），当前需求文档和部分DCC实现也未提交。

不能直接让四个worktree都从该HEAD开始，并声称他们拿到了当前实现。关键代码指纹见[基线文件](baseline-fingerprints.json)，规范化为UTF-8、LF；指纹只证明当时读取的工作区文件，不等同完整可恢复快照。

主管理需在开工前取得一种明确基线：用户确认改动归属并授权基线提交，或用户自行提供包括现有改动的共同提交。未完成前，四线程可以读需求、设计测试和提接口方案，但不得开始依赖遗漏实现的代码修复。不会替用户提交385条既存改动，也不会丢弃它们。

## 3. 分支与环境

建议独立分支：`codex/dcc-a-workflow`、`codex/dcc-b-project`、`codex/dcc-c-version`、`codex/dcc-d-relations`，统筹集成分支 `codex/dcc-integration`。这是命名计划，当前未创建。

每个线程绑定独立worktree，从同一已确认基线建立；槽位使用 `scripts/runtime/reserve-worktree-slot.ps1` 分配，不能手工猜测。实际runtime profile需主管理登记，不能从目录名推断；附加worktree禁止使用48081或其他基准端口。参见[worktree规则](../worktree-restrictions.md)和[端口矩阵](../branch-runtime-ports.md)。

不同端口不能隔离数据库、Redis、BPM执行器、定时生效任务或对象存储。默认只做离线构建和测试；启动服务前逐项登记资源归属，避免多后端共同调度同一业务任务。不能改共享 `.env` 或 `application-local.yaml` 来抢占端口。

## 4. 实施顺序

1. **G0基线：**共同代码基线及任务归属明确。所有线程核对指纹差异。
2. **G1契约：**四线程提交数据、状态、接口和组件方案；主管理Review通过，形成统一契约版本。公共模型只有主管理修改。
3. **G2第一批实现：**A推进审批与双版本状态；B推进项目属性与配置。C可并行做独立版本策略和名称策略，D可并行做选择器组件与独立引用服务，但等待A/B正式契约再接入共享状态和权限。
4. **G3模块Review：**每个小批次提交变更清单、RED/GREEN、回归证据和接入说明，主管理Review后才整合。被退回的模块先修复，不合入有未解决P0/P1问题的代码。
5. **G4整合：**B默认属性→公共申请字段；C版本与占用→A流程调用；D选择器与关系→公共页面；A生效事件→D通知。主管理统一接入上传、详情和浏览页面。
6. **G5交叉验证：**按验证计划完成编译、类型检查、业务单测和契约测试；真实页面E2E仅另获当轮明确授权后执行。

## 5. 授权边界

本轮授权分工与Review及并行任务审查；不等于授权Git提交推送、数据库写入、BPM部署、服务器操作、发布、停止主分支服务或真实页面E2E。任何线程不得因收到本任务包就执行这些动作。后续用户授权应记录具体范围，不重复索要已有授权。

数据库迁移由各模块提出，主管理统筹依赖、历史数据保护和执行顺序；仅在获授权且资源归属明确的环境执行。保留历史Master/Version和签名事实，不批量重写旧数据以适配新模型。

## 6. 固定交付格式

各线程独立建立 `doc/tasks/<自身task-id>/`，包含task.md、execution-log.md、verification-report.md，生产变更必须记录Given/When/Then及RED→GREEN→REGRESSION。

每次交付必须列明：业务范围、基线与契约版本、修改文件、数据库变更、接口/组件、真实执行命令及退出码、测试数量和覆盖、失败/未运行/阻塞项、主管理接入清单。未运行的测试不得写PASS。

Review状态使用 `not_submitted / under_review / changes_requested / accepted_for_integration / integrated / runtime_verified`；这是Review进度，不替代仓库任务收尾状态。通过静态Review不能宣称runtime_verified。
