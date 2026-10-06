# G68 任务账号OWNER与查看矩阵真实UI配置

## Current Status

ready_for_closeout — 两处授权配置已由独立真实Playwright页面保存并正常重开确认；Root扩展授权的自有部门parent修正也完成。已发UI_FREE并关闭owned浏览器，session27539 terminal0。凭据只在内存读取AGENTS，无生产/服务/DB/API/Git操作；本文是任务配置PASS，不是后续checkout/会签或整体HTML验收。

## Scope / Given When Then

Given项目271/G53-P-20261005保原USER OWNER1、USER910328当前EDIT，When真实项目权限页将此行改OWNER并填G67原因，Then重新打开准确项目权限回显两OWNER、原负责人仍1、其他原规则不变。

Given类别908710原查看矩阵、新部门910335唯一成员910328且无子部门，When真实查看矩阵追加DEPT910335/●ALL_MEMBERS/active/G67主体标签，Then保存前该新组preview精确1人G57独立文控验收，保存后重开规则/preview保留；不授真实103部门、admin/scopeall角色，不做密码/会签/checkout。

## Evidence constraints

UI动作均由独立Playwright实际page完成。仅监听自然请求method/status，不解析API JSON作为通过oracle；不fetch/request/mock/SQL或注入router/state。通过依据为准确行/字段/预览与重开保存回显，不只成功toast。只最终截图，不宣称逐步trace截图；避免trace把登录/Authorization secrets落盘。截图只登录完成后的配置视图、密码输入mask；无storageState/credential logs。

Root暂时为同源G68包停其owned后端，8061仍在线；准备期间不向离线后端提交配置，等Root恢复通知再登录/操作，服务离线不写产品失败。

## Actual result

运行态由Root核：本机8061/48061，owned Java5760 healthUP，Jar SHA `becee2d85e2f039e6b30a9558a3dab79bf6138614f3c3db9564fd82b45494524`；来源 `doc/tasks/20261001-dcc-integration-unblock/g68-package-root-review.json` 与 `g56-backend-start-receipt-r15.json`。独立Playwright实际登录芋道源码/admin，用户页只读显示910328属于G67独立文控会签验收、普通员工、仅G58/G67角色；没有更改用户/角色/密码/图片。

1. 真实菜单搜索打开 `/mdm/project-code`（不是component路径），G53-P-20261005精确行“权限”。旧两行原OWNER admin、910328 EDIT；仅910328级别改OWNER/填G67理由，按“保存正式权限”。自然PUT `/admin-api/dcc/project-codes/271/access-rules` 200，重新打开同项目看到准确两USER OWNER；admin原理由保留，测试账号新理由保存。此权限dialog没有项目leader字段，本次未触该字段，Root只读另核leader1。
2. 初908710查看矩阵原规则属瑛泰医疗，新增department树只列其子部门，Root新G67在芋道源码parent100无法匹配，未保存。截图留原“无匹配/未解析”事实；这是任务夹具公司范围边界，不是生产功能被放宽。Root随后明确授权只把自有910335 parent移到原matrix公司root。真实部门页编辑该行→上级部门选唯一瑛泰医疗→保存→重开显示parent瑛泰医疗、原name/leader瑛泰管理员/status开启/sort967不变；Root只读佐证parentID124/leader1。该步script网络监听仅DCC路径，**未录systemDept写请求HTTPstatus，不能补编200**；保存后重开DOM及Root只读为其证明。
3. 重新打开 `/dcc/controlled-file/categories?tab=view-matrix` 精确DCC_FVM_DHF_002/技术调研报告“编辑”，保旧两行，新建第3行label=`G67测试账号查看`、启用、部门、●全员、选910335任务部门、填理由。保存前等真实预览，新组唯一“瑛泰医疗-G67独立文控会签验收”显示1人、唯一userchip=`G57独立文控验收`；旧新品开发部▲组1人/QMS●组4人仍保。自然POST effective-preview200、PUT `/admin-api/dcc/file-categories/908710/view-matrix` 200，正常重开仍3行，精确新组1人。正式replace可能新ruleID，本次核语义保留不要求旧ID不变。Root另只读核DEPT910335/●/ALL_MEMBERS/active及原DEPT136▲/226●。

最终只关闭只读dialog和自有browser，立即向RootUI_FREE，不影响Root下一包/服务；未做Doc认证、checkout、签名、会签、业务API或SQL。两个自然DCC写请求只method/path/status，不读取responseJSON为通过依据。

## Protected evidence

目录 `C:/IntRuoyiBackups/20261006-dcc-task-actor-config/g68-ui-r1`；所有JSON是可见DOM/input/自然method-status，不含密码/Authorization/Cookie/storageState。共58个UI状态JSON、6张截图；不称逐步trace截图。已实际view_image检查两final配置截图。

| 精确结果 | DOM JSON / final screenshot |
|---|---|
| 用户当前部门/岗位/两角色前置 | `003.json` |
| 项目旧权限/真实PUT/重开 | `007.json` / `012.json` / `014.json`；`project-owner-final.png` |
| 自有部门parent重开 | `038.json`；`task-department-parent-final.png` |
| matrix原2规则 | `018.json` |
| 新组精确1人ready（51初截图尚无预览，不作ready通过） | `053.json`；`view-matrix-one-user-ready.png` |
| 真实matrix保存/重开3规则 | `054.json` / `057.json`；`view-matrix-saved-final.png` |

封存回执 `g68-task-actor-ui-receipt.json` SHA `228bb8941cd01395533c30d5a597490d8126979ba4b39897f8ba06f3abbfb82e`，64个保护asset各bytes/SHA在内。脚本SHA `7997ee4ff2a20a1bab9e879eeb9280b7fd883c9e7070219af0bce570d964d948`，node --check0。一次错误locator`.el-tree-select`实际timeout，后用已观察DeptForm实际`.el-select`继续；从未forceclick/model注入或隐式重试保存，所有事实原样保留。
