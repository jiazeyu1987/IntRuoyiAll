# 切换填写人真实 E2E Evidence

- Result: `PASS`
- Command: `node doc/tasks/20260727-switch-filler-snapshot-loading/e2e-artifacts/switch-filler-real.e2e.cjs`
- Frontend: `http://localhost:8081`
- Backend: `http://127.0.0.1:48081`
- Identity: `测试租户/aoteman`
- Execution detail snapshot: assistSwitchTaskCount=`1`
- Filler switch options: total=`3`, enabledOthers=`2`
- Full batch detail reload during filler switch: `0`
- Selected other filler: taskId=`5362`, userId=`912398`, openedExecutionId=`1273`
- API errors during switch: `0`
- Fixture adjustment: workTaskId=`1760`, updateRows=`1`, restoreRows=`1`; original candidate snapshot restored after E2E.

## Notes

- 该 E2E 通过真实前端登录、个人待办“处理”、执行详情页面和“填写人”切换弹窗完成验证。
- 脚本未保存或提交表单；唯一业务动作是正式页面的任务打开与填写人切换打开动作。
- 证据不记录密码、token 或其他凭据。
