# DCC 四模块实现与主管理 Review

## Task Goal

按用户当轮明确授权使用子Agent完成A审批生命周期、B项目与属性配置、C版本/身份占用、D关联引用四模块开发。主管理统一共享接口、整合公共页面并Review真实测试证据。业务基线为docs/product/dcc-final-requirements.html v1.3。

## Milestones

1. 锁定当前已提交DCC代码基线，创建四个任务独立worktree并预约槽位。
2. 发布共同字段/接口合同，四模块按BDD→RED→GREEN分批实现。
3. 主线程Review各模块改动与负向/并发测试，接入共享模型和公共页面。
4. 执行适用定向回归、类型检查、构建及需求AC映射；记录未授权运行验证。
5. ready_for_closeout后按规则清理；本轮无提交推送授权，不将Git收尾写completed。

## Expected Verification

- A：上传升版六阶段，作废批准结束；受控和生效分离；新版生效时旧版才自动作废；真实指派和签名，培训与下发不伪成功。
- B：项目三组默认属性、申请可改/快照、项目负责人账号、文件夹模板权限与创建项目应用。
- C：横杠小版、局部/换版分开、A/9进位、实际变更类型；完整源文件名精确判重、保留期名称编号占用。
- D：跨项目关联最新受控和历史快照分离、统一选择器、受控后指定整改通知、仅项目负责人引用/取消、计数与颜色。
- 所有生产改动先记录Given/When/Then及真实RED/GREEN，依赖失败不是业务RED。Review不以旧测试PASS替新需求。
- 真实E2E、业务数据库写入、BPM部署、服务重启、发布、Git提交推送均无本轮授权，不执行。

## Current Status

in_progress — 用户改为手动派发四个独立worktree任务；A子Agent已中断，B/C/D未启动。四worker和槽位已创建，本轮交付四段提示词，不继续自动开发。

## 设计约束检查

- 旧线程计划与外部CLI启动记录仅作历史；本轮新实施目标不重复启动外部app-server。
- 实际开发分支int_qms。DCC/BPM/System/前端生产代码相对当前HEAD无未提交差异，旧基线a9bcb已被其他已提交工作取代；无需对既存脏改动执行未经授权基线提交。
- AGENTS.md及infra FileController相关既存改动保持，不纳入本任务；工作tree覆入当前AGENTS规则和本任务需求文档作为明确基线文档层。
- 四worktree使用相同HEAD、独立分支和稳定槽位；不启动服务，不共享定时器运行态。用户最新要求手动四线程，不再自动启动子Agent或外部app-server。
- 未确定的审核人、保留天数和提醒提前量不硬编码，缺配置准确报错；可实现正式配置入口而不猜业务值。

## Cleanup Keep

- doc/tasks/20260930-dcc-four-module-implementation/task.md
- doc/tasks/20260930-dcc-four-module-implementation/execution-log.md
- doc/tasks/20260930-dcc-four-module-implementation/verification-report.md
- doc/tasks/20260930-dcc-four-module-implementation/runtime-allocations.json
- doc/tasks/20260930-dcc-four-module-implementation/baseline-manifest.json
