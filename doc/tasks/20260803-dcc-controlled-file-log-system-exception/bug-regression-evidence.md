# Bug Regression Evidence

## Bug Summary

文控中心 > 文控日志页面加载后展示“系统异常”。截图显示列表区域已渲染，但主分页请求失败并触发全局错误提示。

## Expected Behavior

文控日志分页接口应稳定返回正式日志分页数据；如果主查询确实失败，页面必须展示真实错误原因，不得吞异常、默认成功或返回 mock/placeholder 数据。

## Reproduction

待记录具体命令或真实页面路径。

## Root Cause

待定位。

## Regression Test

待新增或更新。

## RED

待记录。

## GREEN

待记录。

## Risk And Regression Scope

- DCC 文控日志统一分页接口。
- 文控日志前端错误展示。
- 关联日志来源：访问审计、生命周期、分发、项目代码修正、培训执行。

## Follow-Up

待验证后记录。
