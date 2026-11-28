import json,pathlib,subprocess,datetime,collections,zipfile
from PIL import Image
root=pathlib.Path(__file__).resolve().parent
rows=[]
for group in ['01-06','02-04','03-05','07-08']:
    rows.extend(json.loads((root/group/'cases.json').read_text(encoding='utf-8-sig')))
for row in rows:
    if row['caseId']=='08-06':
        row['actual']='主线程已实际点击通用A/2下载原件并确认，页面提示当前账号无权访问此受控文件，未取得原件。当前目录及类型的实际编辑资格未核实，不判定为系统故障。'
        row['reproduction']='本地http://localhost:8081，测试1，张鑫悦zhangxinyue。文控中心，受控浏览，通用文件夹，找到DCC0-GENERAL-20261127-G08的A/2，点击下载原件，确认下载，提示无权访问。2026年11月27日主线程补测。'
        row['screenshotPaths']=[str(root/'07-08'/'08主线程补测下载拒绝.png')]
        row['status']='存疑'
        row['evidenceGap']='本项已实际触发下载被拒绝；资格尚未核实，不能判为系统故障。'
reportdir=root.parent.parent/'report'
reportdir.mkdir(exist_ok=True)
target=reportdir/(datetime.datetime.now().strftime('%Y%m%d%H%M%S')+'.xlsx')
def run(*args):
    result=subprocess.run(['officecli',*map(str,args)],capture_output=True,encoding='utf-8',errors='replace')
    if result.returncode: raise RuntimeError(result.stdout+result.stderr)
    return result.stdout
run('create',target)
commands=[{'command':'set','path':'/Sheet1','props':{'name':'已通过'}}]
for sheet in ['未通过','存疑']: commands.append({'command':'add','parent':'/','type':'sheet','props':{'name':sheet}})
headers=['用例编号','主链','检查内容','操作账号','文件与版本','应该发生什么','实际发生什么','完整测试路径与复现步骤','证据说明','页面截图']
counts=collections.Counter()
cache={}
imagedir=root/'report-images'
imagedir.mkdir(exist_ok=True)
for sheet,color in [('已通过','166534'),('未通过','991B1B'),('存疑','92400E')]:
    subset=[row for row in rows if row['status']==sheet]
    commands.append({'command':'set','path':'/'+sheet,'props':{'freeze':'D4','autoFilter':'A3:J'+str(len(subset)+3),'tabColor':color}})
    for number,text in [(1,'文控八主链本地测试1复测：'+sheet),(2,'2026年11月27日。八条指南均已纳入；受阻未执行不算通过。07及08只检查既有文件访问下载，不要求新建升版。截图可能只证明前置错误，请结合证据说明。')]:
        commands.append({'command':'set','path':'/'+sheet+'/A'+str(number)+':J'+str(number),'props':{'merge':'true'}})
        commands.append({'command':'set','path':'/'+sheet+'/A'+str(number),'props':{'value':text,'alignment.wrapText':'true','font.bold':'true'}})
    commands.append({'command':'set','path':'/'+sheet+'/row[2]','props':{'height':'35'}})
    for col,width,title in zip('ABCDEFGHIJ',[15,8,30,24,38,44,68,85,48,90],headers):
        commands.append({'command':'set','path':'/'+sheet+'/col['+col+']','props':{'width':str(width)}})
        commands.append({'command':'set','path':'/'+sheet+'/'+col+'3','props':{'value':title,'fill':color,'font.color':'FFFFFF','font.bold':'true'}})
    for number,row in enumerate(subset,4):
        counts[sheet]+=1
        note=row.get('evidenceNote','')+row.get('evidenceGap','')
        if sheet=='存疑': note='尚未取得完整验证结论。截图可能只证明前置阻塞或页面位置，不代表下游动作已执行。'+note
        values=[row['caseId'],row['chain'],row['title'],row['account'],row['object'],row['expected'],row['actual'],row['reproduction'],note]
        for col,value in zip('ABCDEFGHI',values):
            commands.append({'command':'set','path':'/'+sheet+'/'+col+str(number),'props':{'value':str(value),'font.name':'Microsoft YaHei','font.size':'11','alignment.wrapText':'true','alignment.vertical':'top','fill':'F3F6FA' if number%2==0 else 'FFFFFF'}})
        commands.append({'command':'set','path':'/'+sheet+'/row['+str(number)+']','props':{'height':'300'}})
        source=pathlib.Path(row['screenshotPaths'][0])
        if not source.is_file(): raise FileNotFoundError(source)
        if str(source) not in cache:
            image=Image.open(source).convert('RGB')
            image.thumbnail((1200,750))
            destination=imagedir/(str(len(cache)+1)+'.jpg')
            image.save(destination,quality=82)
            cache[str(source)]=destination
        commands.append({'command':'add','parent':'/'+sheet,'type':'picture','props':{'src':str(cache[str(source)]),'anchor':'J'+str(number)+':K'+str(number+1),'alt':row['caseId']+'本轮页面证据；以证据说明界定覆盖范围'}})
batch=root/'report-batch.json'
batch.write_text(json.dumps(commands,ensure_ascii=False),encoding='utf-8')
(root/'report-build.log').write_text(run('batch',target,'--input',batch,'--stop-on-error'),encoding='utf-8')
run('close',target)
with zipfile.ZipFile(target) as archive:
    pictures=sum(archive.read(name).count(b'<xdr:pic>') for name in archive.namelist() if name.startswith('xl/drawings/drawing') and name.endswith('.xml'))
summary={'report':str(target),'counts':dict(counts),'caseCount':len(rows),'images':pictures}
(root/'report-validation.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
(root/'merged-cases.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=True))
