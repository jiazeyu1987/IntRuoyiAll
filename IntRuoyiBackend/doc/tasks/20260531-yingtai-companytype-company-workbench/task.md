# 任务：修复瑛泰医疗公司信息缺少 companyType

## 任务目标

- 修复使用 `瑛泰医疗 / admin / admin123` 访问展厅 `公司信息` 时提示 `公司工作台缺少字符串字段：companyType` 的问题。
- 优先确认真实运行库数据与接口契约，不引入前端 fallback，不隐藏契约错误。
- 修改范围限定为公司信息当前接口的空态返回契约；不修改 `芋道源码` 租户数据，不复制业务数据。

## BDD 场景

- BDD: 瑛泰医疗公司信息可打开 -> Given 瑛泰医疗 admin 已登录 When 访问 `/showroom/company` Then 公司信息工作台正常显示且不出现 `companyType` 缺失错误
- BDD: 公司接口满足前端契约 -> Given 瑛泰医疗租户存在公司当前数据 When 请求 `/admin-api/showroom/company/current` Then 响应 `data.companyType` 为字符串
- BDD: 源租户与既有账号不受影响 -> Given 本任务仅修复瑛泰医疗公司数据 When 修复完成 Then `芋道源码/admin` 与 `瑛泰医疗/yingtai` 仍可登录

## 里程碑

- [x] M1：建立任务记录与 BDD 场景。
- [x] M2：复现接口和前端错误，定位根因。
- [x] M3：按最小范围修复目标租户数据或接口契约。
- [x] M4：完成 RED/GREEN/REGRESSION 真实验证。
- [x] M5：记录证据、收尾清理并提交。

## 验证计划

- RED：用真实接口或真实前端复现 `companyType` 缺失。
- GREEN：修复后 `/admin-api/showroom/company/current` 返回字符串 `companyType`，真实前端 `/showroom/company` 不再显示缺字段错误。
- REGRESSION：`瑛泰医疗/admin`、`瑛泰医疗/yingtai`、`芋道源码/admin` 登录仍可用。

## 回滚策略

- 回滚本次代码提交即可恢复接口空态返回。
- 不删除或改写现有公司、产品、展柜等业务记录。
- 已补充失败测试后最小实现。

## 当前状态

状态：已完成实现、真实验证与收尾清理，待提交。

## Current Status

completed.
