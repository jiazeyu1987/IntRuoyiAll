$ErrorActionPreference='Stop'
$root='D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\排产员'
$output=Join-Path $root 'output\本地主链重测-20261127-1500'
$screens='C:\Users\BJB110\.agent-browser\tmp\screenshots'
$evidence=@{'编译错误一'='1795779573920';'编译错误二'='1795781373009';'低系数组合'='1795778683768';'工作台'='1795783514056';'日历'='1795783147133';'甘特图'='1795783294555';'保护单'='1795782863212';'恢复设置'='1795788263996';'恢复计划'='1795782369330'}
foreach($name in $evidence.Keys){Copy-Item -LiteralPath (Join-Path $screens ('screenshot-'+$evidence[$name]+'.jpg')) -Destination (Join-Path $output ($name+'.jpg'))}
$context='本地 http://localhost:8081；租户测试1；账号赵杰 zhaojie。'
$order='排产工单中查找来源881MO104106，排产单SCH-881MO104106-20261125-0001。'
$cases=@(
@{Sheet='已通过';Id='核实01';Title='测试参数和工单计划已恢复';Action='清除粗洗单据产能覆盖，把路线生产系数恢复为1，再仅重排104106。';Expected='恢复每小时100件、每班1050件、需求2019件及原计划；保护工单105183不变。';Actual='最终重排成功时间为2026年11月27日21:56:55。104106的14道工序均需求2019件、完成0件、报工0次；粗洗系数1，每小时100件，每班1050件，正式工单采用V34。计划为11月26日08:00至11月27日17:42。105183列表计划未变。';Conclusion='恢复值和正式计划已核实，不表示整条主链全部通过，也不表示下方问题已经修复。后来进入路线修改时另生成了V35草稿，未修改或发布该草稿，正式工单仍采用V34。';Repro=($context+$order+'点击查看，展开粗洗工序，核对产能快照、生产系数和预计结束；关闭详情后比较两张工单的计划日期。');Images=@('恢复设置','恢复计划')},
@{Sheet='未通过';Id='问题01';Title='前端编译错误挡住系数恢复页面';Action='在工艺流程中恢复粗洗工序的生产系数，进入生产配置字段。';Expected='正常查看和编辑候选版本，不被其他页面的代码错误打断。';Actual='页面出现黑色编译错误遮罩，无法正常继续。错误指出展示模块中的语音标签变量被重复声明，对应文件CompanyVersionWorkbench.vue第248行附近。';Conclusion='当时实际发生的环境阻断。之后刷新可以继续，但本任务未修复代码，不能宣称根因已消除。热更新时的代码状态可能影响复现。';Repro=($context+'进入eDHR批记录、工艺流程，搜索ID-GYLC-0001，打开球囊扩张压力泵，查看或修改粗洗工序生产配置，在恢复系数时观察遮罩。错误文件为IntRuoyiFronted/src/views/showroom-admin/company-version/CompanyVersionWorkbench.vue。请结合当时前端热更新记录复现，不要把刷新成功当成修复。');Images=@('编译错误一')},
@{Sheet='未通过';Id='问题02';Title='前端编译错误挡住确认重排';Action='恢复参数后选择104106，打开手动重排并设置开始日期。';Expected='能够确认应用重排，并取得明确结果。';Actual='确认前出现黑色编译错误遮罩，本次确认点击未提交。错误指出文控文件夹模板页面第7行存在表达式转义解析错误。';Conclusion='当时实际发生的环境阻断，不是排程业务校验失败。之后重新进入页面完成恢复重排；本任务未修复对应代码。';Repro=($context+$order+'仅勾选104106，点击手动重排，完成前置检查，点击开始重排，开始日期填写2026年11月26日，在确认应用重排时观察遮罩。错误文件为IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/FolderTemplateLibraryEditor.vue，第7行第68列。需结合当时前端热更新状态复现。');Images=@('编译错误二')},
@{Sheet='存疑';Id='疑点01';Title='调低系数后数量与排程时间取整口径不清';Action='粗洗生产系数0.8，班次产能525件，即每小时50件，只重排104106，从11月26日开始。';Expected='数量显示及工时计算采用清楚一致的取整规则，能根据页面数据复算结束时间。';Actual='2019件乘以0.8为1615.2件，正式工序显示需求1615件，实际结束为11月30日08:50。每班630分钟、周日休息时，按显示的1615件计算并向上取整工时为1938分钟，应结束于08:48，相差2分钟。';Conclusion='可能按1616件计算，或存在其他取整规则，尚未证实原因。不能直接认定算法算错。需比较未取整需求、显示需求和实际计算工时。';Repro=($context+'工艺流程中搜索ID-GYLC-0001，将粗洗生产系数在候选版本中保存为0.8并发布；在排产员工作台把粗洗班次产能设为525并保存。'+$order+'仅勾选104106，手动重排，从2026年11月26日开始并确认应用。打开查看、展开粗洗，比较需求和结束时间；按每日630分钟、11月29日休息复算。核实后恢复系数1及资源产能，并只重排104106。');Images=@('低系数组合')},
@{Sheet='存疑';Id='疑点02';Title='工作台预计日期与正式工序计划不一致';Action='恢复需求2019件、系数1、班次产能1050件后，比较工作台与正式工单。';Expected='两个页面时间能对应；若算法或日期含义不同，应说明区别。';Actual='当轮页面文字读取：工作台粗洗预计11月27日00:00至11月29日00:00；正式工单为11月26日08:00至11月27日17:42。截图显示工作台粗洗产能1050，但日期在右侧未进入截图。';Conclusion='存在显示口径差异，尚不能认定两页必须同值。日期依据当轮页面文字核对，截图未完整覆盖日期，需补充横向滚动后的证据并明确预计日期含义。';Repro=($context+'确认104106需求2019件、粗洗系数1、每小时100件，每班1050件。进入智能排产、排产员工作台、工序列表，找到ID-GX-0001粗洗并向右滚动读取预计日期；再进入排产工单，打开104106查看，比较正式开始和结束日期。注意区分工序预计日期与每日汇总日期。');Images=@('工作台','恢复设置')},
@{Sheet='存疑';Id='疑点03';Title='范围外工单的历史任务在日历和甘特图未显示';Action='仅重排104106后，核对未选中的保护工单105183及其历史任务。';Expected='范围外工单和历史任务不被覆盖；若只显示最近一次排程，应明确说明。';Actual='105183列表仍为11月25日08:00至11月26日17:42，14道工序快照仍在；日历与甘特图当前只看到104106。恢复后日历显示104106共24个任务。';Conclusion='仅能确认历史任务可见性存疑，不能认定任务被删除或数据丢失。需确认日期、筛选范围以及是否仅展示最近一次排程，并核查历史任务保留情况。';Repro=($context+'在排产工单记录105183计划和工序，仅勾选104106，开始日期选2026年11月26日并应用重排。回到列表确认105183未变；进入排程日历，覆盖11月25日至27日并检查筛选条件，查找105183；再进入生产排产甘特图使用同一日期范围查找。比较列表、工序、日历和甘特图，不重排或修改105183。');Images=@('日历','甘特图','保护单')}
)
$commands=[System.Collections.Generic.List[object]]::new()
$headers=@('编号','问题或核实项','测试操作','预期结果','实际结果','判断和影响','完整测试路径','证据截图')
$columns=@('A','B','C','D','E','F','G','H')
$widths=@(12,27,40,40,57,53,70,110)
$palette=@{'已通过'='217568';'未通过'='AC3B3B';'存疑'='95651C'}
foreach($sheet in @('已通过','未通过','存疑')){
 $commands.Add(@{command='set';path="/$sheet";props=@{freeze='C3';tabColor=$palette[$sheet]}})
 $commands.Add(@{command='set';path="/$sheet/row[1]";props=@{height='48'}})
 $commands.Add(@{command='set';path="/$sheet/A1";props=@{value='2026年11月27日本地主链问题整理。仅整理已发现问题和恢复核实，不是全部主链验收报告。未通过记录当时阻断，存疑不等于确认缺陷。';'alignment.wrapText'='true';'font.name'='Microsoft YaHei';'font.size'='11'}})
 $commands.Add(@{command='set';path="/$sheet/A1:H1";props=@{merge='true';fill='E9EFF2'}})
 $commands.Add(@{command='set';path="/$sheet/row[2]";props=@{height='30'}})
 for($index=0;$index -lt 8;$index++){
  $commands.Add(@{command='set';path="/$sheet/col[$($columns[$index])]";props=@{width=[string]$widths[$index]}})
  $commands.Add(@{command='set';path="/$sheet/$($columns[$index])2";props=@{value=$headers[$index];'font.bold'='true';'font.color'='FFFFFF';fill=$palette[$sheet];'font.name'='Microsoft YaHei';'font.size'='11'}})
 }
 $row=3
 foreach($item in @($cases|Where-Object Sheet -eq $sheet)){
  $values=@($item.Id,$item.Title,$item.Action,$item.Expected,$item.Actual,$item.Conclusion,$item.Repro)
  $end=$row+$item.Images.Count-1
  for($index=0;$index -lt 7;$index++){
   $cell="/$sheet/$($columns[$index])$row"
   $commands.Add(@{command='set';path=$cell;props=@{value=$values[$index];'font.name'='Microsoft YaHei';'font.size'='11';'alignment.wrapText'='true';'alignment.vertical'='top';fill='F5F8FA'}})
   if($end -gt $row){$commands.Add(@{command='set';path="${cell}:$($columns[$index])$end";props=@{merge='true'}})}
  }
  foreach($image in $item.Images){
   $commands.Add(@{command='set';path="/$sheet/row[$row]";props=@{height='325'}})
   $commands.Add(@{command='add';parent="/$sheet";type='picture';props=@{src=(Join-Path $output ($image+'.jpg'));anchor="H${row}:I$($row+1)";alt=($item.Id+' '+$image)}})
   $row++
  }
 }
}
$batch=Join-Path $output '问题表排版.json'
$commands|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $batch -Encoding UTF8
$file=Join-Path $root ('report\'+(Get-Date -Format 'yyyyMMddHHmmss')+'.xlsx')
function Invoke-Office{param([string[]]$Arguments) & officecli @Arguments; if($LASTEXITCODE -ne 0){throw 'OfficeCLI执行失败'}}
Invoke-Office @('create',$file)
Invoke-Office @('set',$file,'/Sheet1','--prop','name=已通过')
Invoke-Office @('add',$file,'/','--type','sheet','--prop','name=未通过')
Invoke-Office @('add',$file,'/','--type','sheet','--prop','name=存疑')
Invoke-Office @('batch',$file,'--input',$batch,'--stop-on-error')
Invoke-Office @('close',$file)
Set-Content -LiteralPath (Join-Path $output '最新问题表路径.txt') -Value $file -Encoding UTF8
Write-Output ('报告路径：'+$file)
