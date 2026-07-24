# Task: DCC 上传目录下钻到叶子目录

## Goal

让 DCC 受控文件上传时，文件类别绑定目录只作为起点，用户必须继续选择到最深层叶子目录后才能提交；同时让目录浏览页按父目录汇总子孙目录文件。

## Scope

- 先确认上一条任务已完成，再创建本任务记录。
- 后端提交接口增加最终 `directoryId`，并校验其必须属于类别绑定目录子树且为叶子目录。
- 后端新增上传目录树查询接口，供前端级联选择使用。
- 浏览查询支持按祖先目录汇总子孙文件。
- 不引入 fallback、兼容分支或 mock 成功。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-mes-pro-route-list-owner-last-process/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this DCC upload-path change.

## BDD

BDD: 上传时必须选到叶子目录 -> Given 文件类别已经绑定到包含多层子目录的目录树根节点 / When 用户打开受控文件上传页并继续选择目录 / Then 系统必须要求用户一直选到最后一层叶子目录后才能提交。

BDD: 浏览页汇总子孙目录文件 -> Given 用户在目录浏览页选择某个父目录 / When 查询受控文件列表 / Then 页面应显示该目录及其子孙目录下的受控文件，而不是只显示当前目录本身。

## Milestones

- [x] M1: 写 RED 单测，覆盖叶子目录提交、非叶子拒绝、越界目录拒绝、祖先汇总查询。
- [x] M2: 实现后端上传目录树接口与提交校验。
- [x] M3: 实现浏览查询祖先汇总逻辑。
- [x] M4: 运行后端测试并记录 GREEN 证据。
- [x] M5: 完成后端任务文档与提交准备。

## Expected Verification

- `mvn -pl yudao-module-dcc -am test -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest`
- `mvn -pl yudao-server -am -DskipTests package`

## Current Status

Completed.

## Final Verification Result

- `mvn -pl yudao-module-dcc -am test "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS
- 后端现在支持：
  - 提交请求显式携带叶子 `directoryId`
  - 上传目录树按类别绑定目录返回完整后代结构
  - 父目录浏览汇总子孙目录文件
  - 目录管理用户可浏览文件元数据
