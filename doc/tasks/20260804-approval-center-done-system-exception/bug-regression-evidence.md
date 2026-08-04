# Bug Regression Evidence: 审批中心已办页系统异常

## Bug Summary

进入审批中心“已办”页时，列表区域显示“系统异常”。

## Expected Behavior

`DONE` 视图应通过统一审批中心接口加载已办任务，存在数据时展示列表，无数据时展示空态，不应因视图参数、provider 支持范围或前端路由同步问题触发系统异常。

## Reproduction

- Reproduction path: `审批中心 -> 已办`。
- Reproduction evidence: 用户提供截图显示“已办”菜单高亮，列表顶部出现“系统异常”。

## Root Cause

- pending.

## Regression Test

- pending.

## RED

- pending.

## GREEN

- pending.

## Risk And Scope

- 影响审批中心统一列表 DONE 视图；需避免改变 TODO、MY_INITIATED、CC、模块筛选、分页和显示字段配置行为。

## Blockers And Follow-Up

- 当前工作树已有无关未提交改动和本地分支 ahead 状态，最终提交/推送可能被既有状态阻塞。

