# E2E-10 目录范围纠正及剩余前置

## 旧归因撤回

R2把管理员目录空列表及同时出现的“系统异常”视为目录服务故障，这一归因不成立。Root的directory-scope-inspection.json证明原目录913887存在、active；admin有4条ACTIVE项目指派。DccDirectoryAdminServiceImpl先应用指派范围，即使有管理权限也不能看到范围外目录。E2E17的root-directory-diagnosis.json进一步表明children/search/tree正常，实际错误来自公共待办/类别缺字段。该只读诊断是Root辅助证据，不替代本例业务页面断言。

## BDD：以本例配置身份回读原目录

Given 本例原目录913887（DCC241016C10DIR）已保存，本例配置账号DCC241016C10Matrix从未由本任务绑定项目指派。
When 管理员真实页面仅给DCC241016C10MATRIX补“文档目录”维护权限，配置账号在独立context重新登录、查询并打开原目录编辑页。
Then 原编码、名称、启用状态均在页面可核对，类别表单可以选择该原目录；不新建重复目录、不改admin指派、不授admin或审计高权限。

R3实际：已通过。查询1行，编辑页编码/名称/启用一致；自然页面读取路径为/admin-api/dcc/directories/913887；类别表单也已选中同一目录。证据在runs/directory-recovery-2026-10-16T11-08-55-821Z/。

## RED / GREEN 与当前暂停

- R2“目录保存失败/目录查询系统错误”的产品RED结论撤回，归因修正为admin指派范围不适用于本例配置任务。
- R3目录前置GREEN：真实页面独立身份回读、最小权限回显、原目录选择均已完成；不是整个矩阵变化/重提E2E通过。
- R3脚本未完成项：保存类别前“要求培训”选择器命中隐藏input而超时。已改为点击可见el-switch并读原生checkbox状态，node --check退出码0；未作真实复跑，不将此脚本错误归为产品/schema故障。
- Root要求保存状态并停止业务写入等待48061部署重启：已保存runtime-pause-checkpoint.json，浏览器已关闭；类别保存未执行，未发起矩阵/申请业务动作。
- 最新Root通知：固定五份SQL迁移已获授权，副本首次/重复验证中，实际库尚未迁移。本线程继续不操作后端或数据库。Root恢复通知后，从未保存的类别表单继续；若类别返回错误，需按该表单独立证据记录，不再借公共待办toast归因。
