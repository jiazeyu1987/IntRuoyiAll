# Change Request: 发票凭证打印助手入口纠偏

## Request Summary And Source

用户指出入口应定为 `ERP 系统 / 财务管理 / 发票凭证打印`，且页面内容应来自 `E:\ProjectPackage\erp-invoice-voucher-print-assistant`。上一版实现落成 `分贝通凭证`，不符合业务目标。

## Current Baseline Reviewed

- 当前新增菜单 SQL 指向 `分贝通凭证`、`erp:fenbeitong-voucher:*` 和 `erp/finance/fenbeitong-voucher/index`。
- `分贝通凭证`前端页面是系统已有 ERP 分贝通业务页，不是打印助手项目。
- 打印助手项目 README 明确本机入口为 `http://127.0.0.1:18733/`，页面标题为“发票与对应凭证一键打印”。
- ERP 财务父菜单编号为 `2645`。

## Classification

Bug and requirement correction.

## Impact Analysis

- Product: 菜单名称和用户入口必须改为 `发票凭证打印`。
- Design: 主系统只提供权限控制入口和承载页面，不复制打印助手代码。
- Data: 新菜单 SQL 需使用独立权限码，不能复用 `erp:fenbeitong-voucher:*`。
- API: 主系统不新增打印业务接口；打印助手仍由自身服务提供接口。
- Test: 需要菜单 SQL 合同、前端壳页面合同和真实浏览器路径验证。
- Release: 主系统和打印助手可分开发布；主系统发布只影响入口和地址配置。
- Operations: 正式环境必须提供打印助手服务地址，缺失时页面明确阻断。

## Decision

Accept.

## Required Approvals

用户已明确给出目标路径和内容来源；正式打印助手 URL 仍需发布前配置。

## Downstream Skill Reruns

- `database-schema-delivery`
- `frontend-feature-delivery`
- Playwright E2E

## Blockers And Next Action

- Blocker: 正式环境打印助手 URL 尚未提供。
- Next action: 先按环境变量承载独立助手地址，本机使用 `http://127.0.0.1:18733/` 验证入口和内容。
