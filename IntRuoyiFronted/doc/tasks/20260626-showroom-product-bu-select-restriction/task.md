# 任务：展厅产品 BU 改为受限下拉

## 任务目标

- 将展厅产品编辑弹窗中文页签里的 `BU` 字段从自由手填改为受限下拉选择。
- 下拉选项固定为 `非血管BU`、`外周血管BU`、`结构心BU`、`心血管BU`、`神经血管BU`、`心脏电生理BU` 这 6 个值。
- 不修改后端接口、数据库 schema、英文 BU 编辑行为或其他产品字段布局。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260623-dcc-browser-batch-recognition\task.md`
- 状态：`BLOCKED`
- 处理：已在旧任务文档中明确记录测试服内容识别仍被真实前置条件阻塞，本次新任务不复用其未完成链路。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 表单控件改动保持 IntPP 运维界面风格，不做无关视觉重构。
  - 本轮仅做前端静态合同与类型校验，不执行真实登录或写入链路；若后续追加真实 E2E，第一条登录命令必须先执行官方 `login-preflight.mjs`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。BU 字段直接改为固定枚举下拉，不保留“任意字符串可输入”的兼容分支。
- `是否从根因和长期维护角度解决`：是。抽出统一 BU 枚举常量，并让字段定义与表单控件同时收敛到枚举型。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 中文 BU 只能从固定 6 项里选择 -> Given 用户打开展厅产品编辑弹窗中文页签 / When 用户编辑 BU 字段 / Then 页面展示下拉选择器且只能选择 6 个合法 BU，不能再自由手填`

## 里程碑

1. M1：完成上一任务阻塞归档并创建本次任务台账。`COMPLETED`
2. M2：新增 RED 静态合同，锁定 BU 必须是受限下拉。`COMPLETED`
3. M3：最小修改表单与字段定义，使 RED 转 GREEN。`COMPLETED`
4. M4：运行静态验证、类型检查、证据校验和收尾预览。`COMPLETED`

## 预期验证

- `node tests/e2e/showroom-product-bu-select-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-bu-select-restriction\frontend-feature-evidence.md`

## 最终验证结果

- `node tests/e2e/showroom-product-bu-select-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-bu-select-restriction\frontend-feature-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-showroom-product-bu-select-restriction --mode preview` -> PASS，仅提示 `frontend-feature-evidence.md` 为可删候选，未执行删除。
