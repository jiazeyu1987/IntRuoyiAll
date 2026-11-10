<template>
  <el-dialog v-model="dialogVisible" class="registration-certificate-upload-dialog" destroy-on-close :title="requestId ? '驳回申请修改重提' : '新建注册证'" width="960px" @closed="resetForm">
    <el-form label-width="140px">
      <el-row :gutter="24">
        <el-col :span="12"><el-form-item label="数据所属公司" required><el-select v-model="companyId" filterable remote :remote-method="searchOwnerCompanies" :loading="companyLoading"><el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item></el-col>
        <el-col v-if="facts.ownProduct" :span="12"><el-form-item label="系统项目代码" :required="facts.ownProduct"><el-select v-model="projectCodeId" :disabled="!facts.ownProduct || !!taskProject" filterable remote clearable :remote-method="searchProjectCodes" :loading="projectLoading"><el-option v-for="item in projects" :key="item.id" :label="[item.projectCode,item.projectName].filter(Boolean).join(' - ')" :value="item.id"/></el-select></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="产品中文名称" required><el-input v-model="productName" maxlength="255"/></el-form-item></el-col>
      </el-row>
      <FactsFields v-model="facts"/>
      <el-row v-if="facts.certificateType !== 'FDA'" :gutter="24">
        <el-col :span="12"><el-form-item label="是否委托生产" required><el-select v-model="entrustedProduction" @change="onProductionChange"><el-option label="是" :value="true"/><el-option label="否" :value="false"/></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="是否自行生产" required><el-select v-model="selfProduction"><el-option label="是" :value="true"/><el-option label="否" :value="false"/></el-select></el-form-item></el-col>
        <el-col v-if="entrustedProduction" :span="24"><el-form-item label="受托企业名称" required><el-select v-model="entrustedEnterpriseIds" multiple filterable remote :remote-method="searchEnterprises"><el-option v-for="item in enterprises" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item></el-col>
      </el-row>
      <el-form-item :label="facts.certificateType === 'FDA' ? '批准信函' : '注册证文件'" required><el-upload v-model:file-list="files" action="#" :auto-upload="false" :limit="1"><el-button>选择文件</el-button></el-upload></el-form-item>
      <el-form-item v-if="facts.certificateType === 'FDA'" label="产品描述文件" required><el-upload v-model:file-list="descriptions" action="#" multiple :auto-upload="false"><el-button>选择规格型号等产品描述材料</el-button><template #tip><div>必须上传批准后的产品描述文件，链接不能替代文件。</div></template></el-upload></el-form-item>
      <el-form-item label="证件附件"><el-upload v-model:file-list="attachments" action="#" multiple :auto-upload="false"><el-button>选择多个附件</el-button></el-upload></el-form-item>
      <el-form-item v-if="retainedFiles.length" label="原申请文件"><div><div v-for="file in retainedFiles" :key="file.id"><el-checkbox v-model="file.retained">{{file.originalName}}（{{file.fileKind}}）</el-checkbox></div><small>勾选保留原文件；替换注册证时上传新文件即可。</small></div></el-form-item>
      <el-alert v-if="error" :title="error" type="error" :closable="false"/>
    </el-form>
    <template #footer><el-button :disabled="saving" @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">确认提交审批</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { UploadUserFile } from 'element-plus'
import { getMyRegistrationCertificateUpload, getUploadEntrustedEnterprises, getUploadOwnerCompanies, getUploadProjectCodes, submitRegistrationCertificateUpload,
 type DccRegistrationCertificateUploadCompanyRespVO, type DccRegistrationCertificateUploadEntrustedEnterpriseRespVO, type DccRegistrationCertificateUploadProjectCodeRespVO } from '@/api/dcc/registrationCertificate'
import { generateUUID } from '@/utils'
import FactsFields from '../shared/FactsFields.vue'
import { emptyFacts, validateFacts } from '../shared/facts'
const props = defineProps<{modelValue:boolean;requestId?:number|string;taskProject?:{id:string;projectCode:string;projectName:string}}>()
const emit = defineEmits<{'update:modelValue':[boolean]; saved:[]}>()
const dialogVisible=computed({get:()=>props.modelValue,set:(value:boolean)=>emit('update:modelValue',value)})
const retainedFiles=ref<Array<{id:number|string;fileKind:string;originalName:string;retained:boolean}>>([])
const facts=ref(emptyFacts()), productName=ref(''), companyId=ref<number|string>(), projectCodeId=ref<number|string>()
const entrustedProduction=ref<boolean>(),selfProduction=ref<boolean>(),entrustedEnterpriseIds=ref<Array<number|string>>([])
const companies=ref<DccRegistrationCertificateUploadCompanyRespVO[]>([]),projects=ref<DccRegistrationCertificateUploadProjectCodeRespVO[]>([]),enterprises=ref<DccRegistrationCertificateUploadEntrustedEnterpriseRespVO[]>([])
const companyLoading=ref(false), projectLoading=ref(false),saving=ref(false),error=ref('')
const files=ref<UploadUserFile[]>([]),attachments=ref<UploadUserFile[]>([]),descriptions=ref<UploadUserFile[]>([])
const message=useMessage()
const searchOwnerCompanies=async(keyword='')=>{companyLoading.value=true;try{companies.value=await getUploadOwnerCompanies({keyword})}finally{companyLoading.value=false}}
const searchProjectCodes=async(keyword='')=>{projectLoading.value=true;try{projects.value=await getUploadProjectCodes({keyword})}finally{projectLoading.value=false}}
const searchEnterprises=async(keyword='')=>{enterprises.value=await getUploadEntrustedEnterprises({keyword})}
const onProductionChange=async()=>{if(!entrustedProduction.value) entrustedEnterpriseIds.value=[];else await searchEnterprises()}
const resetForm=()=>{retainedFiles.value=[];facts.value=emptyFacts();productName.value='';companyId.value=undefined;projectCodeId.value=undefined;entrustedProduction.value=undefined;selfProduction.value=undefined;entrustedEnterpriseIds.value=[];files.value=[];attachments.value=[];descriptions.value=[];error.value=''}
const appendFiles=(payload:FormData,key:string,list:UploadUserFile[])=>{for(const file of list){if(!file.raw)throw new Error('上传文件内容丢失，请重新选择');payload.append(key,file.raw)}}
const submit=async()=>{
 error.value=''
 try{
  validateFacts(facts.value)
  if(!companyId.value||!productName.value.trim())throw new Error('请填写数据所属公司和产品中文名称')
  if(facts.value.ownProduct&&!projectCodeId.value)throw new Error('请选择实际项目代码')
  if(facts.value.certificateType!=='FDA'&&(entrustedProduction.value===undefined||selfProduction.value===undefined||(!entrustedProduction.value&&!selfProduction.value)))throw new Error('请选择生产方式，两项不能同时为否')
  if(entrustedProduction.value&&!entrustedEnterpriseIds.value.length)throw new Error('委托生产为是时必须填写受托企业')
  if(!files.value.length&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='REGISTRATION_CERTIFICATE'))throw new Error('请上传注册证或FDA批准信函')
  if(facts.value.certificateType==='FDA'&&!descriptions.value.length&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='FDA_PRODUCT_DESCRIPTION'))throw new Error('FDA必须上传批准后的产品描述材料')
  const payload=new FormData();if(props.requestId){payload.append('sourceRequestId',String(props.requestId));retainedFiles.value.filter(f=>f.retained).forEach(f=>payload.append('retainedFileIds',String(f.id)))}payload.append('companyId',String(companyId.value));payload.append('productName',productName.value.trim());if(projectCodeId.value)payload.append('projectCodeId',String(projectCodeId.value))
  payload.append('factsJson',JSON.stringify(facts.value));payload.append('classification',facts.value.classification)
  for(const field of ['certificateNo','firstObtainedDate','effectiveDate','expiryDate','remark'])if(facts.value[field])payload.append(field,facts.value[field])
  payload.append('entrustedProduction',String(facts.value.certificateType==='FDA'?false:entrustedProduction.value));payload.append('selfProduction',String(facts.value.certificateType==='FDA'?false:selfProduction.value))
  if(facts.value.certificateType!=='FDA')entrustedEnterpriseIds.value.forEach(id=>payload.append('entrustedEnterpriseIds',String(id)))
  appendFiles(payload,'file',files.value);appendFiles(payload,'attachments',attachments.value);if(facts.value.certificateType==='FDA')appendFiles(payload,'productDescriptionFiles',descriptions.value)
  saving.value=true;await submitRegistrationCertificateUpload(payload,`DCC-REG-CERT-UPLOAD-${generateUUID()}`);message.success('已提交审批');dialogVisible.value=false;emit('saved')
 }catch(e){error.value=e instanceof Error?e.message:String(e);message.error(error.value)}finally{saving.value=false}
}
watch(()=>facts.value.certificateType,(type,previous)=>{
 if(!previous||type===previous||props.requestId)return
 const old=facts.value
 facts.value={certificateType:type,ownProduct:old.ownProduct,classification:old.classification,certificateHolder:old.certificateHolder,registrationLink:old.registrationLink,remark:old.remark,...(type==='FDA'?{}:{certificateStatus:'VALID'}),...(type==='CE'?{ceCertificates:[]}:{})}
 entrustedProduction.value=undefined;selfProduction.value=undefined;entrustedEnterpriseIds.value=[];files.value=[];descriptions.value=[]
})
watch(()=>facts.value.ownProduct,own=>{if(props.taskProject&&!own){facts.value.ownProduct=true;return}if(!own)projectCodeId.value=undefined})
watch(()=>props.modelValue,async visible=>{if(!visible)return;try{await searchOwnerCompanies();if(props.taskProject){facts.value.ownProduct=true;projectCodeId.value=props.taskProject.id;projects.value=[props.taskProject]}else await searchProjectCodes();if(props.requestId){
 const result=await getMyRegistrationCertificateUpload(props.requestId),frozen=JSON.parse(result.request.detailJson),draft=frozen.draft
 if(!draft||!frozen.factsJson)throw new Error('该申请未保留完整提交内容，不能自动回填，请联系注册部核对')
 facts.value=JSON.parse(frozen.factsJson);productName.value=draft.productName;companyId.value=draft.ownerCompanyId;projectCodeId.value=draft.projectCodeId
 if(!companies.value.some(c=>String(c.id)===String(companyId.value)))companies.value.push({id:companyId.value!,name:frozen.ownerCompanyName})
 entrustedProduction.value=draft.entrustedProduction;selfProduction.value=draft.selfProduction;entrustedEnterpriseIds.value=draft.entrustedEnterpriseIds??[]
 if(entrustedProduction.value)await searchEnterprises()
 retainedFiles.value=result.files.map(file=>({...file,retained:true}))
}}catch(e){error.value=e instanceof Error?e.message:String(e)}})
</script>
<style scoped>.registration-certificate-upload-dialog :deep(.el-dialog__body){max-height:70vh;overflow:auto}.el-select,.el-date-editor{width:100%}</style>
