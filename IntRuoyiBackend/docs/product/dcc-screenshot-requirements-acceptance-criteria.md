# Acceptance Criteria: DCC 截图需求分析草稿

## Purpose and Scope

本文记录 DCC 截图需求的可验收行为草稿。后续进入实现前，应将这些标准转为正式 BDD 场景和严格 TDD 用例。

## Evidence Reviewed

- 用户提供的 DCC 需求截图。
- `doc/tasks/20260525-dcc-requirements-analysis/requirements-analysis.md`
- `docs/product/dcc-screenshot-requirements-prd.md`
- `docs/product/dcc-screenshot-requirements-user-flows.md`

## Acceptance Criteria

1. Given 申请人发起文件受控审批，When 上传允许的可编辑源文件，Then 系统接受该文件作为源文件。
2. Given 申请人上传 `dwg`、`sldprt`、`sldasm` 或 `slddrw` 文件，When 未上传 PDF 伴随文件并提交，Then 系统拒绝提交。
3. Given 申请人上传图纸类文件和 PDF 伴随文件，When 其他必填项完整，Then 文件上传校验通过。
4. Given 文件受控审批表单缺少文件类别、现行有效版本或 14 位产品编号，When 申请人提交，Then 系统拒绝提交并提示缺失字段。
5. Given 节点负责人回退流程至申请人，When 申请人查看待办，Then 待办中显示该流程和 `有流程回退，需处理`。
6. Given 申请人处理退回流程，When 申请人再次提交，Then 原流程实例继续推进。
7. Given 申请人主动撤回流程，When 撤回完成，Then 申请人可选择删除流程或重新提交。
8. Given 表单选择需要培训，When 流程到达第四节点前，Then 申请人必须上传培训记录。
9. Given 表单未选择需要培训，When 流程到达第四节点前，Then 系统不强制上传培训记录。
10. Given 文控处理第四节点，When 未上传加盖受控章后的 PDF，Then 节点不可完成。
11. Given 申请人到达会签节点，When 需要选择会签人员，Then 申请人可按权限选择会签参与人。
12. Given 用户下载文档，When 用户确认下载，Then 界面提示下载文件为非受控文件。
13. Given 文档下载成功，When 查看下载记录，Then 记录包含下载人 id 和下载时间。
14. Given 文档编码满足确认后的 `INT/RE` 体系记录规则，When 任意用户下载，Then 系统允许下载。
15. Given 文控选择电子发放接收人，When 接收人处理任务，Then 接收人可按权限加签。
16. Given 纸质发放回收记录存在，When 用户导出或打印，Then 输出包含文件编号、版本、名称、发放人、接收人、发放日期、回收人、回收日期。
17. Given 用户设置密码，When 密码少于 8 位或未同时包含英文和数字，Then 系统拒绝保存。
18. Given 账户达到确认后的密码更新周期，When 用户进入系统或按确认策略操作，Then 系统强制更新密码。
19. Given 文件处于修改中，When 用户查看相关文件视图，Then 界面显示 `修改中`。
20. Given 外来文件评审流程已完成业务定义，When 用户发起该流程，Then 流程按确认后的字段、节点、参与人和输出物执行。

## Rejection Criteria

- 图纸类文件缺少 PDF 伴随文件仍允许提交。
- 下载成功但没有非受控文件提醒或下载留痕。
- 退回至申请人后重新发起新流程而不是继续原流程。
- 第四节点未上传加盖受控章后的 PDF 仍允许完成。
- 未确认密码更新周期就上线强制更新逻辑。
- 对缺少文件、缺少流程节点、下载留痕失败等场景做静默成功或 fallback。

## Product Blockers

- 文件受控审批相关验收需先确认当前流程和第四节点。
- 密码定期更新相关验收需先确认周期和上线规则。
- 外来文件评审相关验收需先确认完整业务流程。
