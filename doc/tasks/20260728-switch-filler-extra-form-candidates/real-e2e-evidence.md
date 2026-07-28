# Wangxin 附加表单切换填写人真实 E2E Evidence

- Result: `BLOCKED`
- Command: `node doc/tasks/20260728-switch-filler-extra-form-candidates/e2e-artifacts/switch-filler-extra-form-wangxin-real.e2e.cjs`
- Frontend: `http://127.0.0.1:8081`
- Backend: `http://127.0.0.1:48081`
- Identity: `芋道源码/wangxin`
- Execution detail snapshot: assistSwitchTaskCount=`3`, extraAssistSwitchTaskCount=`2`
- Extra form task: `<none>`
- Filler options: total=`4`, enabledExtraOptions=`0`, enabledExtraOthers=`0`
- Full batch detail reload during switch: `0`
- Selected extra-form filler: `<none>`
- API errors during switch: `0`

## Notes

- 该 E2E 通过真实前端登录 wangxin、个人待办“处理”、执行详情页和“填写人”切换弹窗完成验证。
- 验证目标限定为非 MAIN 附加表单/表单槽位候选可见、可点击，并通过正式 task/open 切换上下文。
- 脚本未保存或提交表单；唯一业务动作是正式页面任务打开与填写人切换打开。
- 证据不记录密码、token 或其他凭据。

## Error

- no_wangxin_extra_form_switch_sample_found
- Details: `{"workTaskPageAttempts":[{"pageIndex":1,"rowCount":1,"openableFillTaskCount":1,"candidateTaskIds":[2245],"rows":[{"id":2245,"taskCode":"EDHRT-1785224950178","taskType":"FILL","status":"TODO","batchExecutionId":900000000900,"batchTaskId":6981,"executionId":1586,"workOrderCode":"881MO090889","batchCode":"34126020001","processName":"粗洗工序","actionUrlPresent":true,"openableFillTask":true}]}],"candidateAttempts":[{"pageIndex":1,"candidateIndex":0,"taskId":2245,"taskCode":"EDHRT-1785224950178","message":"extra form task must produce enabled filler options"}]}`
