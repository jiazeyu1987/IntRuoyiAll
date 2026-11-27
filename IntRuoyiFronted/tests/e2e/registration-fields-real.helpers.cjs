const { expect } = require('playwright/test')
const base = process.env.REG_CERT_E2E_BASE_URL
if (!base) throw new Error('REG_CERT_E2E_BASE_URL is required')
const field = (dialog, label) => dialog.locator('.el-form-item').filter({ has: dialog.page().locator('.el-form-item__label', { hasText: new RegExp(`^${label}$`) }) })
async function choose(page, dialog, label, option) {
  const select = field(dialog, label).locator('.el-select')
  await select.click()
  const controls = await select.locator('input[role=combobox]').getAttribute('aria-controls')
  const options = page.locator(`[id="${controls}"] .el-select-dropdown__item:not(.is-disabled)`)
  await (option ? page.locator(`[id="${controls}"]`).getByRole('option',{name:option,exact:true}) : options.first()).click()
}
async function input(dialog, label, value) { await field(dialog, label).locator('input,textarea').first().fill(value) }
async function date(dialog, label, value) {
  await input(dialog, label, value)
  await field(dialog, label).locator('input').press('Enter')
  await field(dialog, label).locator('input').press('Tab')
}
function pdf(name) {
  const objects = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>', '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R >>', '<< /Length 0 >>\nstream\n\nendstream']
  let source = '%PDF-1.4\n', offsets = [0]
  objects.forEach((object,i)=>{ offsets.push(Buffer.byteLength(source)); source += `${i+1} 0 obj\n${object}\nendobj\n` })
  const xref = Buffer.byteLength(source)
  source += `xref\n0 5\n0000000000 65535 f \n${offsets.slice(1).map(x=>String(x).padStart(10,'0')+' 00000 n ').join('\n')}\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  return { name, mimeType:'application/pdf', buffer:Buffer.from(source) }
}
async function files(dialog,label,names) { await field(dialog,label).locator('input[type=file]').setInputFiles(names.map(pdf)) }
async function result(page, fragment, action) {
  const response = page.waitForResponse(r=>r.url().includes(fragment)&&r.request().method()==='POST',{timeout:60000})
  await action()
  const payload = await (await response).json()
  expect(payload.code, payload.msg).toBe(0)
  return payload.data
}
async function login(page, username='admin', password='admin123') {
  await page.goto(base+'/login?redirect=/index')
  const form=page.locator('form.login-form:visible')
  await expect(form).toBeVisible({timeout:60000})
  const tenant=form.locator('.el-select input[role=combobox]').first()
  if(await tenant.count()) {await tenant.fill('芋道源码');await page.locator('.el-select-dropdown__item:visible').filter({hasText:'芋道源码'}).first().click()}
  else await form.locator('input.el-input__inner').first().fill('芋道源码')
  await form.locator('input.el-input__inner:not([role=combobox]):visible').first().fill(username)
  await form.locator('input[type=password]').fill(password)
  await result(page,'/system/auth/login',()=>form.getByRole('button',{name:'登录',exact:true}).click())
  await expect(form).toBeHidden({timeout:60000})
}
async function list(page) {
  await page.goto(base+'/mdm/registration-certificate')
  await expect(page.getByTestId('registration-certificate-read-page')).toBeVisible({timeout:60000})
}
async function create(page,type,name) {
  await list(page)
  await page.getByRole('button',{name:'上传注册证',exact:true}).click()
  const d=page.getByRole('dialog',{name:'新建注册证',exact:true})
  await expect(d).toBeVisible()
  await choose(page,d,'注册证类型',type==='DOMESTIC'?'国内':type)
  await choose(page,d,'产品归属','其他（不关联系统产品）')
  await choose(page,d,'数据所属公司')
  await input(d,'产品中文名称',name)
  await input(d,'产品分类','II')
  await input(d,'证件持证主体',name+'主体')
  await input(d,'注册证链接','https://example.com/'+name)
  await input(d,'备注',name+'备注')
  await expect(field(d,'变更描述')).toHaveCount(0)
  if(type!=='FDA') {
    await expect(field(d,'批准日期')).toHaveCount(0)
    await date(d,'有效期至','2031-01-01')
    await choose(page,d,'是否委托生产','否')
    await choose(page,d,'是否自行生产','否')
  }
  if(type==='DOMESTIC') {
    await input(d,'注册证号',name+'CN')
    await date(d,'首次获证日期','2025-01-01')
    await date(d,'生效日期','2025-01-02')
    await input(d,'项目代码（非系统关联）',name+'P')
  } else if(type==='CE') {
    await expect(field(d,'型号规格')).toHaveCount(0)
    await expect(field(d,'Initial Issue Date（首次获证日期，可选）')).toHaveCount(0)
    for(const label of ['产品英文名称','产品代码','商品名','公告机构代码']) await input(d,label,name+label)
    await d.getByRole('button',{name:'添加Rev.记录'}).click()
    await field(d,'证书Rev.记录').locator('.el-table__body tr').nth(0).locator('input').nth(0).fill('Rev.01')
    await d.getByRole('button',{name:'添加Rev.记录'}).click()
    const row=field(d,'证书Rev.记录').locator('.el-table__body tr').nth(1)
    await row.locator('input').nth(0).fill('Rev.02')
    await row.locator('input').nth(1).fill(name+'CE')
  } else {
    for(const label of ['产品英文名称','K/P号','法规监管代码','产品代码','列名编号']) await input(d,label,name+label)
    await choose(page,d,'注册提交类型','Traditional 510(k)')
    await choose(page,d,'是否客户持证','否')
    await choose(page,d,'批准信函状态','有效')
    await choose(page,d,'工厂登记状态','Active 激活')
    await date(d,'批准日期','2025-02-01')
    await date(d,'列名日期','2025-03-01')
  }
  await files(d,type==='FDA'?'批准信函':'注册证文件',[name+'-1.pdf',name+'-2.pdf'])
  if(type==='FDA') {
    await d.getByRole('button',{name:'确认提交审批'}).click()
    await expect(d.getByRole('alert')).toContainText('FDA必须上传批准后的产品描述材料')
    await files(d,'产品描述文件',[name+'-description.pdf'])
  } else {
    await d.getByRole('button',{name:'确认提交审批'}).click()
    await expect(d.getByRole('alert')).toContainText('两项不能同时为否')
    await choose(page,d,'是否自行生产','是')
  }
  await files(d,'证件附件',[name+'-attachment.pdf'])
  const data=await result(page,'/registration-certificates/uploads',()=>d.getByRole('button',{name:'确认提交审批'}).click())
  await expect(d).toBeHidden({timeout:30000})
  return data
}
async function approve(initiatorPage,requestId,name) {
  const username=process.env.REG_CERT_E2E_APPROVER_USERNAME
  const password=process.env.REG_CERT_E2E_APPROVER_PASSWORD
  expect(username,'a separate real approver account is required').toBeTruthy()
  expect(password,'real approver password is required').toBeTruthy()
  const context=await initiatorPage.context().browser().newContext()
  const page=await context.newPage()
  try {
  await login(page,username,password)
  const pending=page.waitForResponse(r=>r.url().includes('/approval-center/tasks/page')&&r.url().includes('viewType=TODO'))
  await page.goto(base+'/approval-center/todo?keyword='+encodeURIComponent(String(requestId)))
  const payload=await (await pending).json()
  expect(payload.code,payload.msg).toBe(0)
  const tasks=payload.data.list
  expect(tasks.filter(task=>task.decisionDetailQuery?.requestId===String(requestId)),`unique approval for ${name} request ${requestId}`).toHaveLength(1)
  const index=tasks.findIndex(task=>task.decisionDetailQuery?.requestId===String(requestId))
  await page.locator('.approval-center__table .el-table__row').nth(index).getByRole('button',{name:/审核|审批/}).first().click()
  const d=page.locator('.approval-center__review-dialog:visible')
  await d.locator('input[type=password]').fill(password)
  await result(page,'/approval-center/tasks/review',()=>d.getByRole('button',{name:'确认审核'}).click())
  await expect(d).toBeHidden({timeout:60000})
  } finally { await context.close() }
}
async function openChange(page,name) {
  await list(page)
  const filter=page.getByTestId('registration-certificate-current-tab').locator('.table-multi-filter').first()
  if(await filter.locator('.table-multi-filter__condition-row').count()===0) await filter.getByRole('button',{name:'新增筛选条件'}).click()
  await filter.locator('.table-multi-filter__field-select').first().click()
  await page.getByRole('option',{name:'产品',exact:true}).click()
  await filter.getByPlaceholder('输入产品名称').fill(name)
  const queried=page.waitForResponse(r=>r.url().includes('/registration-certificates/page')&&r.request().method()==='GET')
  await page.getByTestId('registration-certificate-current-tab').getByRole('button',{name:'查询',exact:true}).click()
  expect((await (await queried).json()).code).toBe(0)
  const row=page.locator('.registration-certificate-current-table .el-table__body .el-table__row').filter({hasText:name})
  await expect(row).toHaveCount(1)
  const loaded=page.waitForResponse(r=>/registration-certificates\/\d+(\?|$)/.test(r.url())&&r.request().method()==='GET')
  await row.getByRole('button',{name:'变更',exact:true}).click()
  const detail=(await (await loaded).json()).data
  const d=page.getByRole('dialog',{name:'注册证变更',exact:true})
  await expect(d).toBeVisible()
  await expect(d.locator('.el-loading-mask:visible')).toHaveCount(0)
  await expect(field(d,'变更内容')).toHaveCount(0)
  return {d,detail}
}
async function change(page,type,name) {
  const {d,detail}=await openChange(page,name)
  await expect(field(d,'注册证链接').locator('input')).toHaveValue('https://example.com/'+name)
  await expect(field(d,'备注').locator('textarea')).toHaveValue(name+'备注')
  await expect(field(d,'当前已批准材料').locator('.el-table__body tr')).toHaveCount(type==='FDA'?4:3)
  if(type==='FDA') {
    for(const label of ['型号规格','结构组成','适用范围','产品技术要求','住所','生产地址']) await expect(field(d,label)).toHaveCount(0)
    await choose(page,d,'工厂登记状态','Inactive 非激活')
  } else if(type==='CE') {
    await expect(field(d,'证书Rev.记录').locator('.el-table__body tr')).toHaveCount(2)
    await d.getByRole('button',{name:'添加型号',exact:true}).click()
    await field(d,'型号规格').locator('input').fill('MODEL-A')
    await input(d,'预期用途','本次CE预期用途')
  } else {
    await input(d,'型号规格','MODEL-CN')
    await input(d,'结构组成','本次国内结构组成')
    await input(d,'适用范围','本次国内适用范围')
  }
  await date(d,'本次批准日期','2026-11-25')
  await files(d,'变更批件文件',[name+'-change1.pdf',name+'-change2.pdf'])
  await input(d,'其他变更说明',name+'补充说明')
  if(type==='CE') {
    await d.getByRole('button',{name:'提交审核',exact:true}).click()
    await expect(d.getByRole('alert').filter({hasText:'CE型号调整必须填写本次调整说明'})).toBeVisible()
    await input(d,'变更描述','本次增加MODEL-A型号')
  }
  const requestId=await result(page,'/changes',()=>d.getByRole('button',{name:'提交审核',exact:true}).click())
  await expect(d).toBeHidden({timeout:30000})
  await approve(page,requestId,name)
  const next=await openChange(page,name)
  const before=JSON.parse(detail.factsJson), after=JSON.parse(next.detail.factsJson)
  expect(next.detail.productName).toBe(detail.productName)
  const changed=type==='FDA'?['factoryRegistrationStatus','otherContentDescription']:type==='CE'?['modelSpecifications','changeDescription','otherContentDescription']:['otherContentDescription']
  for(const key of Object.keys(before)) if(!changed.includes(key)) expect(after[key],key).toEqual(before[key])
  expect(after.otherContentDescription).toBe(name+'补充说明')
  if(type==='FDA') expect(after.factoryRegistrationStatus).toBe('INACTIVE')
  if(type==='CE') expect(after.modelSpecifications).toEqual(['MODEL-A'])
  if(type==='DOMESTIC') expect(next.detail.modelSpecification).toBe('MODEL-CN')
  const materialRows=field(next.d,'当前已批准材料').locator('.el-table__body tr')
  await expect(materialRows).toHaveCount(type==='FDA'?6:5)
  const names=await materialRows.allTextContents()
  for(const suffix of ['-1.pdf','-2.pdf','-attachment.pdf','-change1.pdf','-change2.pdf']) expect(names.join('\n')).toContain(name+suffix)
  if(type==='FDA') {
    await input(next.d,'K/P号',name+'-KNEW')
    await date(next.d,'本次批准日期','2026-11-25')
    await files(next.d,'变更批件文件',[name+'-next.pdf'])
    await next.d.getByRole('button',{name:'提交审核',exact:true}).click()
    await expect(next.d.getByRole('alert').filter({hasText:'新增或替代FDA批准信息必须填写本次变更描述'})).toBeVisible()
  }
  await next.d.getByRole('button',{name:'取消',exact:true}).click()
  return {requestId,certificateId:detail.certificateId}
}
module.exports={field,choose,input,date,files,result,login,list,create,approve,change,openChange,base}
