# Verification Report

## Bug

表单模板右侧“编辑”被误解为跳到批记录表单模块，导致用户看到的模块不对，且不符合“编辑按钮仍在表单模板页里”的业务口径。

## Expected

表单模板右侧“编辑”应保留在表单模板页内，打开当前模板自身的规则编辑工作区；右侧编辑交互与批记录表单一致，包括可填写/不可填写、字段名称、字段类型等配置项。

## Reproduction

真实路径：登录本机 `int_main`，进入 `/mdm/form-center/template` 表单模板列表，选中“按压式压力泵过程检验记录 / V14.0”，点击右侧“编辑”。

## Root Cause

前一次修正把“与批记录表单右侧编辑一致”误收敛为“跳到批记录表单模块”。正确根因是表单模板页缺少对“模块归属”和“右侧规则编辑交互”的边界约束：应保留模板模块，只复用批记录式单元格规则编辑交互。

## RED:

`node tests/e2e/form-template-independent-button-actions-static.spec.js` -> FAIL, 旧实现仍把表单模板“编辑”导向 `/mes/pro/batch-record-form-list?mode=designer&reportMode=edit`。

## GREEN:

`node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS

`node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS

`node tests/e2e/form-template-edit-designer-parity-static.spec.js` -> PASS

`node tests/e2e/form-center-static.spec.js` -> PASS

`node tests/e2e/form-template-edit-designer-parity-real.e2e.js` -> PASS

`pnpm exec eslint src/api/form-center/template.ts src/views/form-center/template/index.vue tests/e2e/form-template-independent-button-actions-static.spec.js tests/e2e/form-template-button-interaction-parity-static.spec.js tests/e2e/form-template-edit-designer-parity-static.spec.js tests/e2e/form-template-edit-designer-parity-real.e2e.js` -> PASS

`pnpm ts:check` -> PASS

`git diff --check -- <task-owned frontend files>` -> PASS

## Verification

静态合同、真实 Playwright、eslint、类型检查和 diff 检查均通过。真实页面证明“编辑”没有进入批记录表单模块，而是留在表单模板页内，并在点击左侧规则单元格后显示右侧规则编辑控件。

## Real E2E Evidence

- Frontend: `http://127.0.0.1:8081/`
- Backend health: `http://127.0.0.1:48081/actuator/health` -> `UP`
- Tenant/account label: `芋道源码/admin`
- Result URL: `http://127.0.0.1:8081/mdm/form-center/template?templateId=33&versionNo=V14.0&mode=designer&templateMode=edit`
- Workspace: `form-template`
- Rule cells found: `138`
- Screenshot: `doc/tasks/20260828-form-template-edit-button-batch-record-designer/template-edit-current-workspace.png`

## External Resource Notes

Playwright 过程中出现外部头像和百度统计请求 `ERR_ABORTED`，以及一次非目标资源 `502` 控制台提示；目标模板页、模板版本读取、按钮点击、路由和右侧编辑控件断言均已通过，本任务未发起模板保存写请求。

## Git / Push

- Local commit: `8b5799a89 test(form-center): lock template edit workspace parity`
- Push status: BLOCKED, GitHub HTTPS 443 当前不可达，且本机 GitHub 代理 `127.0.0.1:7890` 未监听；SSH 443 也未通过当前公钥认证，未切换推送方式。

## Blockers

远端推送未完成。本地实现和验证已完成，`int_main` 当前领先 `origin/int_main` 1 个提交，待网络或代理恢复后继续推送。
