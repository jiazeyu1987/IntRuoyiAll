const { test }=require('playwright/test')
const flow=require('./registration-fields-real.helpers.cjs')
test.use({actionTimeout:30000,navigationTimeout:60000})
for(const type of ['DOMESTIC','CE','FDA']) {
  test(`${type} creation, approval, change and unchanged information retention`,async({page},testInfo)=>{
    test.setTimeout(360000)
    const name=`E2E-FIELDS-${type}-${Date.now()}`
    await flow.login(page)
    const created=await flow.create(page,type,name)
    await flow.approve(page,created,name)
    const changed=await flow.change(page,type,name)
    await testInfo.attach('business-records',{body:JSON.stringify({name,created,changed}),contentType:'application/json'})
    await page.screenshot({path:testInfo.outputPath(type+'.png'),fullPage:true})
  })
}
