# Bug Regression Evidence

## Bug Summary

选择产品信息工序后，部分辅助网格单元格出现两个输入控件叠加。

## Expected Behavior

每个辅助网格位置只允许一个正式字段卡片和一个匹配字段类型的输入控件；重复字段引用应去重，不同字段位置冲突应 fail fast。

## Reproduction

- 用户截图：产品信息辅助网格中“三通旋塞”等单元格出现 textarea 与普通输入框叠加。
- 真实只读页面和聚焦回归命令待执行。

## Root Cause

待真实 DOM 和任务预览数据确认。

## Regression Test

待新增聚焦静态或逻辑合同，覆盖重复字段去重和不同字段位置冲突。

## RED

- RED: 待执行。

## GREEN

- GREEN: 待执行。

## Verification

- 待执行。

## Risk And Regression Scope

- 风险集中在辅助网格字段构造和位置冲突检测。
- 不修改后端批次任务来源、批记录表单绑定、表单槽位、填写权限和保存接口。

## Blockers And Follow-up

- 当前无阻塞。
