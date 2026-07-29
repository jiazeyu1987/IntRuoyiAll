# Wangxin 附加表单切换填写人真实 E2E Evidence

- Result: `FAIL`
- Command: `node doc/tasks/20260728-switch-filler-extra-form-candidates/e2e-artifacts/switch-filler-extra-form-wangxin-real.e2e.cjs`
- Frontend: `http://127.0.0.1:8081`
- Backend: `http://127.0.0.1:48081`
- Identity: `芋道源码/wangxin`
- Fixture role setup: roleAdded=`false`, targetRole=`粗洗工序填写者角色`
- Fixture role restore: restored=`true`
- Execution detail snapshot: assistSwitchTaskCount=`4`, extraAssistSwitchTaskCount=`2`
- Extra form task: `<none>`
- Filler options: total=`0`, enabledExtraOptions=`0`, enabledExtraOthers=`0`
- Full batch detail reload before option selection: ``
- Selected extra-form filler: `<none>`
- API errors during switch: `0`

## Notes

- 该 E2E 通过真实前端登录 wangxin、个人待办“处理”、执行详情页和“填写人”切换弹窗完成验证。
- 脚本在验证前用本机授权 admin 临时补齐任务自有样本所需角色，finally 恢复 wangxin 原角色集合。
- 验证目标限定为非 MAIN 附加表单/表单槽位候选可见、可点击，并通过正式 task/open 切换上下文。
- 脚本未保存或提交表单；唯一业务动作是正式页面任务打开与填写人切换打开。
- 证据不记录密码、token 或其他凭据。

## Error

- filler switch must show at least current and one other option, got 0
