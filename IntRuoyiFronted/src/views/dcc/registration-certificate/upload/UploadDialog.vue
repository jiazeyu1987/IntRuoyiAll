<template>
  <el-dialog v-model="dialogVisible" class="registration-certificate-upload-dialog" top="5vh" append-to-body destroy-on-close :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving" :title="requestId ? '驳回申请修改重提' : '新建注册证'" width="980px" @closed="handleClosed">
    <div class="certificate-type-bar">
      <span class="certificate-type-label"><span class="required-mark">*</span> 注册证类型</span>
      <el-radio-group v-model="facts.certificateType" class="certificate-type-tabs" aria-label="注册证类型">
        <el-radio-button value="DOMESTIC">国内 <small>NMPA</small></el-radio-button>
        <el-radio-button value="CE">CE <small>EU CE</small></el-radio-button>
        <el-radio-button value="FDA">FDA <small>US FDA</small></el-radio-button>
      </el-radio-group>
    </div>
    <div v-loading="hydrating" class="registration-certificate-upload-body">
      <el-form label-width="140px" label-position="top">
        <section class="certificate-section">
          <div class="certificate-section-heading"><h3>产品与归属</h3><span>{{ facts.ownProduct ? '关联系统产品' : '手动录入产品' }}</span></div>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="8"><el-form-item label="产品归属" required><el-select v-model="facts.ownProduct"><el-option label="瑛泰产品（关联系统产品）" :value="true"/><el-option label="其他（不关联系统产品）" :value="false"/></el-select></el-form-item></el-col>
            <el-col :xs="24" :sm="12" :md="8"><el-form-item label="数据所属公司" required><el-select v-model="companyId" filterable remote :remote-method="searchOwnerCompanies" :loading="companyLoading"><el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item></el-col>
            <el-col v-if="facts.ownProduct" :xs="24" :sm="24" :md="8"><el-form-item label="系统项目代码" :required="facts.ownProduct"><el-select v-model="projectCodeIds" multiple :disabled="!facts.ownProduct" filterable remote clearable :remote-method="searchProjectCodes" :loading="projectLoading"><el-option v-for="item in projects" :key="item.id" :label="[item.projectCode,item.projectName].filter(Boolean).join(' - ')" :value="item.id" :disabled="item.disabled"><span>{{ [item.projectCode,item.projectName].filter(Boolean).join(' - ') }}</span><small v-if="item.bindingMessage">（{{ item.bindingMessage }}）</small></el-option></el-select></el-form-item></el-col>
          </el-row>
          <div class="certificate-product-readout">
            <el-row :gutter="20">
              <el-col v-if="facts.ownProduct" :xs="24" :sm="8"><el-form-item label="DCC产品编码"><el-input :model-value="facts.projectProducts?.map(p => p.productCode).join('、')" readonly placeholder="选择项目后自动显示"/></el-form-item></el-col>
              <el-col :xs="24" :sm="facts.ownProduct ? 16 : 24"><el-form-item label="产品中文名称" required><el-input v-model="productName" :placeholder="facts.ownProduct ? '选择项目后自动显示，可按证载名称修改' : '请输入产品中文名称'" maxlength="255"/></el-form-item></el-col>
            </el-row>
          </div>
        </section>
        <section class="certificate-section">
          <div class="certificate-section-heading"><h3>主体与产品属性</h3></div>
          <FactsFields v-model="facts" creation creation-section="attributes"/>
          <el-row v-if="facts.certificateType !== 'FDA'" :gutter="20">
            <el-col :xs="24" :sm="12" :md="8"><el-form-item label="是否委托生产" required><el-select v-model="entrustedProduction" @change="onProductionChange"><el-option label="是" :value="true"/><el-option label="否" :value="false"/></el-select></el-form-item></el-col>
            <el-col :xs="24" :sm="12" :md="8"><el-form-item label="是否自行生产" required><el-select v-model="selfProduction"><el-option label="是" :value="true"/><el-option label="否" :value="false"/></el-select></el-form-item></el-col>
            <el-col v-if="entrustedProduction" :xs="24" :sm="24" :md="8"><el-form-item label="受托企业名称" required><el-select v-model="entrustedEnterpriseIds" multiple filterable remote :remote-method="searchEnterprises"><el-option v-for="item in enterprises" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item></el-col>
          </el-row>
        </section>
        <section class="certificate-section">
          <div class="certificate-section-heading"><h3>{{ facts.certificateType === 'FDA' ? 'FDA 审批与登记' : '证件核心信息' }}</h3></div>
          <FactsFields v-model="facts" creation creation-section="certificate"/>
        </section>
        <section class="certificate-section">
          <div class="certificate-section-heading"><h3>上传材料</h3><span>支持点击选择或拖拽，正文和附件均可多选</span></div>
          <div class="upload-material-grid" :class="{ 'is-fda': facts.certificateType === 'FDA' }">
            <UploadMaterialSlot @update:model-value="error=''" v-model="files" :title="facts.certificateType === 'FDA' ? '批准信函' : '注册证文件'" required :hint="facts.certificateType === 'FDA' ? '上传 FDA 批准信函，支持 PDF / 图片' : '上传正式注册证，支持 PDF / Word / 图片'" :error-message="primaryMaterialError" />
            <UploadMaterialSlot @update:model-value="error=''" v-if="facts.certificateType === 'FDA'" v-model="descriptions" title="产品描述文件" required hint="上传规格型号等产品描述材料" notice="必须上传批准后的产品描述文件，链接不能替代文件。" :error-message="descriptionMaterialError" />
            <UploadMaterialSlot v-model="attachments" title="证件附件" hint="可选，补充相关证明材料" />
          </div>
          <el-form-item v-if="retainedFiles.length" label="原申请文件"><div><div v-for="file in retainedFiles" :key="file.id"><el-checkbox v-model="file.retained">{{file.originalName}}（{{file.fileKind}}）</el-checkbox></div><small>勾选保留原文件；替换注册证时上传新文件即可。</small></div></el-form-item>
        </section>
        <section class="certificate-section">
          <div class="certificate-section-heading"><h3>补充信息</h3></div>
          <FactsFields v-model="facts" creation creation-section="supplement"/>
        </section>
        <el-alert v-if="productError" :title="productError" type="error" :closable="false"/>
        <el-alert v-if="error" :title="error" type="error" :closable="false"/>
      </el-form>
    </div>
    <template #footer>
      <div class="certificate-footer">
        <span class="certificate-footer-hint"><span class="required-mark">*</span> 为必填项，请核对信息及材料后提交</span>
        <div class="certificate-footer-actions"><el-button :disabled="saving" @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="hydrating || (facts.ownProduct && (productLoading || !facts.projectProducts?.length || !!productError))" @click="submit">确认提交审批</el-button></div>
      </div>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { UploadUserFile } from 'element-plus'
import { getMyRegistrationCertificateUpload, getUploadEntrustedEnterprises, getUploadOwnerCompanies, getUploadProjectCodes, getUploadProjectProduct, submitRegistrationCertificateUpload,
 type DccRegistrationCertificateUploadCompanyRespVO, type DccRegistrationCertificateUploadEntrustedEnterpriseRespVO, type DccRegistrationCertificateUploadProjectCodeRespVO } from '@/api/dcc/registrationCertificate'
import { generateUUID } from '@/utils'
import FactsFields from '../shared/FactsFields.vue'
import UploadMaterialSlot from '../shared/UploadMaterialSlot.vue'
import { emptyFacts, validateCreationFacts } from '../shared/facts'
const props = defineProps<{modelValue:boolean;requestId?:number|string;handlingTaskId?:number|string;taskProject?:{id:string;projectCode:string;projectName:string}}>()
const emit = defineEmits<{'update:modelValue':[boolean]; saved:[]}>()
let submitted = false
const handleClosed = () => {
 const notifySaved = submitted
 submitted = false
 resetForm()
 if (notifySaved) emit('saved')
}
const dialogVisible=computed({get:()=>props.modelValue,set:(value:boolean)=>emit('update:modelValue',value)})
const retainedFiles=ref<Array<{id:number|string;fileKind:string;originalName:string;retained:boolean}>>([])
const facts=ref(emptyFacts()), productName=ref(''), companyId=ref<number|string>(), projectCodeIds=ref<Array<number|string>>([])
const entrustedProduction=ref<boolean>(),selfProduction=ref<boolean>(),entrustedEnterpriseIds=ref<Array<number|string>>([])
const companies=ref<DccRegistrationCertificateUploadCompanyRespVO[]>([]),projects=ref<DccRegistrationCertificateUploadProjectCodeRespVO[]>([]),enterprises=ref<DccRegistrationCertificateUploadEntrustedEnterpriseRespVO[]>([])
const companyLoading=ref(false), projectLoading=ref(false),saving=ref(false),error=ref('')
const productLoading=ref(false), productError=ref(''), hydrating=ref(false)
let productLoadVersion=0
const loadProjectProduct=async(preserveName=false)=>{
 const version=++productLoadVersion
 productError.value='';productLoading.value=false;delete facts.value.projectProduct;facts.value.projectProducts=[]
 if(!preserveName)productName.value=''
 if(!facts.value.ownProduct||!projectCodeIds.value.length)return
 const ids=[...projectCodeIds.value]
 productLoading.value=true
 try{
  const products=await Promise.all(ids.map(id=>getUploadProjectProduct(id)))
  if(version!==productLoadVersion)return
  if(products.some((p,i)=>String(p.projectCodeId)!==String(ids[i])))throw new Error('项目与DCC产品关联不一致，请重新选择项目')
  const unique=new Map(products.map(product=>[String(product.productCatalogId),product]))
  if(unique.size!==products.length)throw new Error('多个项目指向同一产品，请仅保留一个项目')
  facts.value.projectProducts=products;facts.value.projectProduct=products[0]
  if(!preserveName||!productName.value.trim())productName.value=products.map(p=>p.productName).join('、')
 }catch(e){if(version===productLoadVersion)productError.value=e instanceof Error?e.message:String(e)}
 finally{if(version===productLoadVersion)productLoading.value=false}
}
watch([()=>facts.value.ownProduct,projectCodeIds],()=>{if(!hydrating.value)void loadProjectProduct()})
const files=ref<UploadUserFile[]>([]),attachments=ref<UploadUserFile[]>([]),descriptions=ref<UploadUserFile[]>([])
const materialValidationAttempted=ref(false)
const primaryMaterialError=computed(()=>materialValidationAttempted.value&&!files.value.length&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='REGISTRATION_CERTIFICATE')?(facts.value.certificateType==='FDA'?'请上传 FDA 批准信函':'请上传正式注册证文件'):'')
const descriptionMaterialError=computed(()=>materialValidationAttempted.value&&facts.value.certificateType==='FDA'&&!descriptions.value.length&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='FDA_PRODUCT_DESCRIPTION')?'请上传批准后的产品描述文件':'')

const message=useMessage()
const searchOwnerCompanies=async(keyword='')=>{companyLoading.value=true;try{companies.value=await getUploadOwnerCompanies({keyword})}finally{companyLoading.value=false}}
const searchProjectCodes=async(keyword='')=>{projectLoading.value=true;try{projects.value=await getUploadProjectCodes({keyword,sourceRequestId:props.requestId})}finally{projectLoading.value=false}}
const searchEnterprises=async(keyword='')=>{enterprises.value=await getUploadEntrustedEnterprises({keyword})}
const onProductionChange=async()=>{if(!entrustedProduction.value) entrustedEnterpriseIds.value=[];else await searchEnterprises()}
const resetForm=()=>{++productLoadVersion;productLoading.value=false;productError.value='';materialValidationAttempted.value=false;retainedFiles.value=[];facts.value=emptyFacts();productName.value='';companyId.value=undefined;projectCodeIds.value=[];entrustedProduction.value=undefined;selfProduction.value=undefined;entrustedEnterpriseIds.value=[];files.value=[];attachments.value=[];descriptions.value=[];error.value=''}
const appendFiles=(payload:FormData,key:string,list:UploadUserFile[])=>{for(const file of list){if(!file.raw)throw new Error('上传文件内容丢失，请重新选择');payload.append(key,file.raw)}}
const submit=async()=>{
 if(saving.value)return
 materialValidationAttempted.value=true
 error.value=''
 try{
  validateCreationFacts(facts.value)
  if(facts.value.ownProduct&&(productLoading.value||productError.value||!facts.value.projectProducts?.length))throw new Error(productError.value||'请先选择已绑定DCC产品的项目')
  if(props.taskProject&&!projectCodeIds.value.some(id=>String(id)===String(props.taskProject!.id)))throw new Error('必须保留任务所属项目')
  if(productName.value.length>255)throw new Error('关联产品名称总长度不能超过255个字符')
  if(!companyId.value||!productName.value.trim())throw new Error('请填写数据所属公司和产品中文名称')
  if(facts.value.ownProduct&&!projectCodeIds.value.length)throw new Error('请选择实际项目代码')
  if(facts.value.certificateType!=='FDA'&&(entrustedProduction.value===undefined||selfProduction.value===undefined||(!entrustedProduction.value&&!selfProduction.value)))throw new Error('请选择生产方式，两项不能同时为否')
  if(entrustedProduction.value&&!entrustedEnterpriseIds.value.length)throw new Error('委托生产为是时必须填写受托企业')
  if(!files.value.length&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='REGISTRATION_CERTIFICATE'))throw new Error('请上传注册证或FDA批准信函')
  if(facts.value.certificateType==='FDA'&&!descriptions.value.length&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='FDA_PRODUCT_DESCRIPTION'))throw new Error('FDA必须上传批准后的产品描述材料')
  const payload=new FormData();if(props.handlingTaskId)payload.append('handlingTaskId',String(props.handlingTaskId));if(props.requestId){payload.append('sourceRequestId',String(props.requestId));retainedFiles.value.filter(f=>f.retained).forEach(f=>payload.append('retainedFileIds',String(f.id)))}payload.append('companyId',String(companyId.value));payload.append('productName',productName.value.trim());if(projectCodeIds.value.length)payload.append('projectCodeId',String(projectCodeIds.value[0]))
  payload.append('factsJson',JSON.stringify(facts.value));payload.append('classification',facts.value.classification)
  for(const field of ['certificateNo','firstObtainedDate','effectiveDate','expiryDate','remark'])if(facts.value[field])payload.append(field,facts.value[field])
  payload.append('entrustedProduction',String(facts.value.certificateType==='FDA'?false:entrustedProduction.value));payload.append('selfProduction',String(facts.value.certificateType==='FDA'?false:selfProduction.value))
  if(facts.value.certificateType!=='FDA')entrustedEnterpriseIds.value.forEach(id=>payload.append('entrustedEnterpriseIds',String(id)))
  appendFiles(payload,'file',files.value.slice(0,1));appendFiles(payload,'additionalPrimaryFiles',files.value.slice(1));appendFiles(payload,'attachments',attachments.value);if(facts.value.certificateType==='FDA')appendFiles(payload,'productDescriptionFiles',descriptions.value)
  saving.value=true;await submitRegistrationCertificateUpload(payload,`DCC-REG-CERT-UPLOAD-${generateUUID()}`);message.success('已提交审批');submitted=true;dialogVisible.value=false
 }catch(e){error.value=e instanceof Error?e.message:String(e);message.error(error.value)}finally{saving.value=false}
}
watch(()=>facts.value.certificateType,(type,previous)=>{
 if(!previous||type===previous||props.requestId)return
 materialValidationAttempted.value=false
 const old=facts.value
 facts.value={certificateType:type,projectProduct:old.projectProduct,projectProducts:old.projectProducts,ownProduct:old.ownProduct,classification:old.classification,certificateHolder:old.certificateHolder,registrationLink:old.registrationLink,remark:old.remark,...(type==='FDA'?{}:{certificateStatus:'VALID'}),...(type==='CE'?{ceCertificates:[]}:{})}
 entrustedProduction.value=undefined;selfProduction.value=undefined;entrustedEnterpriseIds.value=[];files.value=[];descriptions.value=[]
})
watch(()=>facts.value.ownProduct,own=>{if(props.taskProject&&!own){facts.value.ownProduct=true;return}if(!own)projectCodeIds.value=[]})
watch(()=>props.modelValue,async visible=>{if(!visible)return;hydrating.value=true;try{await searchOwnerCompanies();if(props.taskProject){facts.value.ownProduct=true;projectCodeIds.value=[props.taskProject.id];projects.value=[props.taskProject]}else await searchProjectCodes();if(props.requestId){
 const result=await getMyRegistrationCertificateUpload(props.requestId),frozen=JSON.parse(result.request.detailJson),draft=frozen.draft
 if(!draft||!frozen.factsJson)throw new Error('该申请未保留完整提交内容，不能自动回填，请联系注册部核对')
 facts.value=JSON.parse(frozen.factsJson);productName.value=draft.productName;companyId.value=draft.ownerCompanyId;projectCodeIds.value=(facts.value.projectProducts??[]).map(p=>p.projectCodeId)
 if(!companies.value.some(c=>String(c.id)===String(companyId.value)))companies.value.push({id:companyId.value!,name:frozen.ownerCompanyName})
 entrustedProduction.value=draft.entrustedProduction;selfProduction.value=draft.selfProduction;entrustedEnterpriseIds.value=draft.entrustedEnterpriseIds??[]
 if(entrustedProduction.value)await searchEnterprises()
 retainedFiles.value=result.files.map(file=>({...file,retained:true}))
}}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{await nextTick();hydrating.value=false;if(facts.value.ownProduct)await loadProjectProduct(!!props.requestId)}})
</script>
<style scoped src="../shared/certificateForm.css"></style>
