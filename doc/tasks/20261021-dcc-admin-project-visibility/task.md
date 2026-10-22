# 超级管理员项目可见范围

## Goal
admin（正式 super_admin 角色）可读取当前租户全部启用 DCC 项目，不要求逐项目授权或修正任务分配。
追加用户要求：上传下拉仅显示项目代码；可选绿色、资料不全/无上传权限红色禁选、绿色始终在上，展示原因；返回全部候选而非前 50 条。完成后更新并重启 int_qms 后端（当轮已明确授权）。

## Milestones
BDD / RED；实现共享读取边界；GREEN / 回归；清理。

## BDD
Given 全量项目候选含完整/不完整项目，When 打开或搜索上传项目下拉，Then 只展示代码、绿先红后，红色禁选并提示缺项及维护入口。
Given 系统依赖或数据库失败，When 查询候选，Then 请求明确失败，不把系统错误转换成项目资料不全。
Given 当前租户启用超级管理员，When 查询项目范围或读取项目，Then 返回本租户全部启用项目并允许读取。
Given 普通用户、停用账号或其他租户项目，When 读取，Then 原授权及租户边界仍生效。
Given 超级管理员无项目 EDIT/OWNER，When 修改或上传，Then 不因本次只读特权获得写权限。

## Expected Verification
实际 service 源码 javac 编译，JUnit/Mockito 定向测试 RED/GREEN，前端定向合同及 SFC 编译、pnpm ts:check、Maven reactor package、不可变归档核对、int_qms 重启及 health。用户本轮明确追加重启；不执行 E2E、不写业务数据库、不提交推送。

## 设计约束检查
使用现有 PermissionApi 正式 super_admin 角色识别，不以用户名/固定 ID/菜单权限识别。保留租户、启用状态、账号合法性及写入权限。新只读候选接口返回完整授权范围并按可选状态排序；只读校验复用正式产品预览和属性校验。只重启 48061，不影响 int_main 或前端服务。

## Current Status
blocked

前端 25 项及后端 25 项测试、最终 pnpm ts:check、完整后端构建、cleanup preview/apply 均 PASS；已重启 int_qms，health UP。仅 Git 提交推送未获当轮授权，按仓库收尾规则不标记 completed；E2E 未授权未执行。
