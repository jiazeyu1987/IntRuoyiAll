<template>
 <el-dialog v-model="visible" :title="requestId ? '变更驳回修改重提' : '注册证变更'" width="960px" destroy-on-close>
  <el-alert title="提交后正式内容保持原样，审核通过后更新并保留全部历史。" type="info" :closable="false"/>
  <el-form v-if="detail" v-loading="loading" label-width="140px" class="mt-16px">
   <el-descriptions :column="2" border><el-descriptions-item label="所属公司">{{detail.ownerCompanyName}}</el-descriptions-item><el-descriptions-item label="系统关联项目">{{detail.projectCode}}</el-descriptions-item><el-descriptions-item label="DCC产品编码">{{facts.projectProduct?.productCode}}</el-descriptions-item></el-descriptions>
   <FactsFields v-model="facts" :locked-fields="lockedFields" lock-type change/>
   <el-form-item label="当前已批准材料"><el-table :data="currentFiles" border empty-text="当前版本没有材料"><el-table-column label="材料类型" width="160"><template #default="{row}">{{row.fileKind==='REGISTRATION_CERTIFICATE'&&facts.certificateType==='FDA'?'批准信函':fileLabels[row.fileKind]||row.fileKind}}</template></el-table-column><el-table-column prop="originalName" label="文件名"/><el-table-column label="查看" width="100"><template #default="{row}"><el-button link type="primary" @click="previewFile(row.id)">在线查看</el-button></template></el-table-column></el-table></el-form-item>
   <el-row :gutter="24"><el-col v-for="field in fields.filter(f=>!['CE_MODELS','CE_NOTIFIED_BODY'].includes(f.key))" :key="field.key" :span="12"><el-form-item :label="field.label"><el-input v-model="values[field.key]" type="textarea" :rows="2" maxlength="4096" :placeholder="`变更后的${field.label}`"/></el-form-item></el-col></el-row>
   <template v-if="facts.certificateType !== 'FDA'">
    <el-form-item label="是否委托生产"><el-select v-model="entrusted"><el-option label="是" :value="true"/><el-option label="否" :value="false"/></el-select></el-form-item>
    <el-form-item label="是否自行生产"><el-select v-model="self"><el-option label="是" :value="true"/><el-option label="否" :value="false"/></el-select></el-form-item>
    <el-form-item v-if="entrusted" label="受托企业"><el-input v-if="facts.certificateType==='CE'" v-model="enterpriseNames" type="textarea" placeholder="每行一个受托企业"/><el-select v-else v-model="enterpriseIds" multiple filterable remote :remote-method="searchEnterprises"><el-option v-for="item in enterprises" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item>
   </template>
   <el-form-item label="本次批准日期" required><el-date-picker v-model="approvalDate" type="date" value-format="YYYY-MM-DD"/></el-form-item>
   <el-form-item label="变更批件文件" required><el-upload v-model:file-list="files" action="#" multiple :auto-upload="false"><el-button>选择文件</el-button></el-upload></el-form-item>
   <el-form-item v-if="retainedFiles.length" label="原申请文件"><div><div v-for="file in retainedFiles" :key="file.id"><el-checkbox v-model="file.retained">{{file.originalName}}（{{file.fileKind}}）</el-checkbox></div></div></el-form-item>
   <el-form-item v-if="facts.certificateType==='FDA'" label="新版产品描述材料"><el-upload v-model:file-list="descriptions" action="#" multiple :auto-upload="false"><el-button>上传批准后的产品说明、规格型号文件</el-button></el-upload></el-form-item>
   <el-form-item label="变更附件"><el-upload v-model:file-list="attachments" action="#" multiple :auto-upload="false"><el-button>添加附件</el-button></el-upload></el-form-item>
   <el-form-item label="其他变更说明"><el-input v-model="other" type="textarea" maxlength="4096" placeholder="选填，填写其他变更内容"/></el-form-item>
  </el-form>
  <el-dialog v-model="previewOpen" title="当前已批准材料" width="90%" append-to-body destroy-on-close><ProtectedPdfViewer v-if="previewSource" :preview-source="previewSource"/></el-dialog>
  <el-alert v-if="error" :title="error" type="error" :closable="false"/>
  <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="!detail||loading" @click="submit">提交审核</el-button></template>
 </el-dialog>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { UploadUserFile } from 'element-plus'
import { getUploadEntrustedEnterprises, getRegistrationCertificateDetail, getRegistrationCertificateEvidence, getMyRegistrationCertificateUpload, submitRegistrationCertificateChange, type DccRegistrationCertificateDetailVO, type DccRegistrationCertificatePageItemVO } from '@/api/dcc/registrationCertificate'
import { generateUUID } from '@/utils'
import FactsFields from '../shared/FactsFields.vue'
import ProtectedPdfViewer from '@/views/dcc/controlled-file/view/index.vue'
import { buildDccRegistrationCertificatePreviewSource, type OnlineFilePreviewSource } from '@/api/common/filePreview'
const previewOpen=ref(false),previewSource=ref<OnlineFilePreviewSource>()
const previewFile=(id:number|string)=>{previewSource.value=buildDccRegistrationCertificatePreviewSource(id);previewOpen.value=true}
import { emptyFacts, parseFacts, validateChangeFacts } from '../shared/facts'
const props=defineProps<{modelValue:boolean;certificate?:DccRegistrationCertificatePageItemVO;requestId?:number|string}>()
const emit=defineEmits<{'update:modelValue':[boolean];saved:[]}>()
const visible=computed({get:()=>props.modelValue,set:(v:boolean)=>emit('update:modelValue',v)})
const detail=ref<DccRegistrationCertificateDetailVO>(),facts=ref(emptyFacts()),loading=ref(false),saving=ref(false),error=ref('')
const domesticFields=[{key:'PRODUCT_NAME',prop:'productName',label:'产品中文名称'},{key:'MODEL_SPECIFICATION',prop:'modelSpecification',label:'型号规格'},{key:'STRUCTURE_COMPOSITION',prop:'structureComposition',label:'结构组成'},{key:'INTENDED_USE',prop:'intendedUse',label:'适用范围'},{key:'TECHNICAL_REQUIREMENTS',prop:'technicalRequirements',label:'产品技术要求'},{key:'REGISTRANT_NAME',prop:'registrantName',label:'注册人名称'},{key:'RESIDENCE_ADDRESS',prop:'residenceAddress',label:'住所'},{key:'PRODUCTION_ADDRESS',prop:'productionAddress',label:'生产地址'}]
const fields=computed(()=>facts.value.certificateType==='FDA'?[domesticFields[0]]:facts.value.certificateType==='CE'?[domesticFields[0],{key:'INTENDED_USE',prop:'intendedUse',label:'预期用途'},{key:'CE_MODELS',prop:'modelSpecifications',label:'型号增加／删除'},domesticFields[2],domesticFields[4],{key:'REGISTRANT_NAME',prop:'registrantName',label:'持证人名称'},{key:'RESIDENCE_ADDRESS',prop:'residenceAddress',label:'持证人地址'},domesticFields[7],{key:'CE_NOTIFIED_BODY',prop:'notifiedBodyCode',label:'公告机构代码'}]:domesticFields)
const lockedFields=computed(()=>['certificateType','ownProduct','projectProduct','projectCode'])
const values=reactive<Record<string,string>>({}),entrusted=ref<boolean>(),self=ref<boolean>(),enterpriseNames=ref(''),approvalDate=ref(''),other=ref(''),files=ref<UploadUserFile[]>([]),attachments=ref<UploadUserFile[]>([]),descriptions=ref<UploadUserFile[]>([])
const retainedFiles=ref<Array<{id:number|string;fileKind:string;originalName:string;retained:boolean}>>([])
const currentFiles=ref<Array<{id:number|string;fileKind:string;originalName:string}>>([])
const fileLabels:Record<string,string>={REGISTRATION_CERTIFICATE:'注册证',CERTIFICATE_ATTACHMENT:'附件',FDA_PRODUCT_DESCRIPTION:'FDA产品描述',CHANGE_APPROVAL:'变更批件'}
const enterpriseIds=ref<Array<number|string>>([]),enterprises=ref<Array<{id:number|string;name:string}>>([])
const searchEnterprises=async(keyword='')=>{const results=await getUploadEntrustedEnterprises({keyword});const selectedOptions=enterprises.value.filter(item=>enterpriseIds.value.some(id=>String(id)===String(item.id)));enterprises.value=[...new Map([...selectedOptions,...results].map(item=>[String(item.id),item])).values()]}
const enterpriseOptionsFromSnapshot=(rows:Array<{enterpriseId:number|string;enterpriseName:string}>)=>{enterprises.value=rows.filter(item=>item.enterpriseId!=null).map(item=>({id:item.enterpriseId,name:item.enterpriseName}))}
const enterpriseRows=()=>facts.value.certificateType==='CE'?enterpriseNames.value.split(/\r?\n/).map(name=>name.trim()).filter(Boolean).map(enterpriseName=>({enterpriseName})):enterpriseIds.value.map(id=>{const item=enterprises.value.find(e=>String(e.id)===String(id));if(!item)throw new Error('受托企业不在有效企业清单中，请重新选择');return {enterpriseId:Number(id),enterpriseName:item.name}})
let initialFacts='',initialRelation=''
let generation=0
const relation=()=>JSON.stringify([entrusted.value,self.value,enterpriseIds.value,enterpriseNames.value])
watch(()=>props.modelValue,async show=>{const current=++generation;if(!show)return;detail.value=undefined;error.value='';loading.value=true;previewOpen.value=false;previewSource.value=undefined;currentFiles.value=[];files.value=[];attachments.value=[];descriptions.value=[];retainedFiles.value=[];approvalDate.value='';other.value='';try{
 const source=props.requestId?await getMyRegistrationCertificateUpload(props.requestId):undefined
 if(current!==generation)return
 let frozen:any
 if(source){frozen=JSON.parse(source.request.detailJson);if(source.request.status!=='REJECTED'||frozen.operation!=='CHANGE_CERTIFICATE')throw new Error('该申请不是可重提的驳回变更申请');if(!frozen.factsJson||!frozen.draft||!source.request.certificateId)throw new Error('该申请未保存完整变更内容，不能自动回填')}
 const certificateId=source?.request.certificateId??props.certificate?.certificateId
 if(!certificateId)throw new Error('未选择注册证')
 const data=await getRegistrationCertificateDetail(certificateId)
 if(current!==generation)return
 facts.value=parseFacts(data.factsJson);initialFacts=JSON.stringify(facts.value);other.value=facts.value.otherContentDescription??''
 for(const field of fields.value)values[field.key]=String((data as any)[field.prop]??'')
 entrusted.value=data.entrustedProduction;self.value=data.selfProduction
 const enterprises=JSON.parse(data.entrustedEnterprisesJson||'[]');if(!Array.isArray(enterprises))throw new Error('受托企业数据格式无效')
 enterpriseIds.value=enterprises.map(item=>item.enterpriseId);enterpriseOptionsFromSnapshot(enterprises);enterpriseNames.value=enterprises.map(item=>item.enterpriseName).join('\n');if(facts.value.certificateType==='DOMESTIC')await searchEnterprises();initialRelation=relation()
 if(frozen){
  facts.value=parseFacts(frozen.factsJson)
  for(const field of fields.value)values[field.key]=String(frozen.draft[field.prop]??'')
  entrusted.value=frozen.draft.entrustedProduction;self.value=frozen.draft.selfProduction
  const names=JSON.parse(frozen.draft.entrustedEnterprisesJson);if(!Array.isArray(names))throw new Error('原申请受托企业数据无效');enterpriseIds.value=names.map(item=>item.enterpriseId);enterpriseOptionsFromSnapshot(names);enterpriseNames.value=names.map(item=>item.enterpriseName).join('\n')
  approvalDate.value=frozen.approvalDate||'';other.value=frozen.otherDescription||''
  retainedFiles.value=source.files.map(file=>({...file,retained:true}))
 }
 const evidence=await getRegistrationCertificateEvidence(certificateId,data.versionId)
 if(current!==generation)return
 currentFiles.value=evidence.files
 detail.value=data
}catch(e){if(current===generation)error.value=e instanceof Error?e.message:String(e)}finally{if(current===generation)loading.value=false}})
const validateChangeTextValues = (changes: Record<string, string>) => {
 for (const [field, label] of [['PRODUCT_NAME', '产品名称'], ['REGISTRANT_NAME', '注册人名称']]) {
  if (changes[field]?.trim().length > 255) throw new Error(`${label}长度不能超过255个字符`)
 }
}
const submit=async()=>{error.value='';try{
 if(!detail.value)throw new Error('当前正式信息未加载');validateChangeFacts(facts.value,JSON.parse(initialFacts),descriptions.value.length>0||retainedFiles.value.some(f=>f.retained&&f.fileKind==='FDA_PRODUCT_DESCRIPTION'));validateChangeTextValues(values)
 for(const field of fields.value)if(!field.key.startsWith('CE_')&&values[field.key]!==String((detail.value as any)[field.prop]??'')&&!values[field.key]?.trim())throw new Error(`请填写${field.label}变更值`)
 if(!approvalDate.value)throw new Error('请填写变更批件批准日期');if(!files.value[0]?.raw&&!retainedFiles.value.some(f=>f.retained&&f.fileKind==='CHANGE_APPROVAL'))throw new Error('请上传变更批件文件')
 if(facts.value.certificateType==='CE'&&facts.value.notifiedBodyCode!==JSON.parse(initialFacts).notifiedBodyCode&&!facts.value.notifiedBodyCode?.trim())throw new Error('请填写公告机构代码')
 if(facts.value.certificateType==='CE'&&JSON.stringify(facts.value.modelSpecifications)!==JSON.stringify(JSON.parse(initialFacts).modelSpecifications)&&Array.isArray(facts.value.modelSpecifications)){const models=facts.value.modelSpecifications;if(!Array.isArray(models)||!models.length||models.some(m=>!m.trim())||new Set(models.map(m=>m.trim())).size!==models.length)throw new Error('型号必须非空且不能重复');facts.value.modelSpecifications=models.map(m=>m.trim())}
 const data=new FormData(),changes:Record<string,string>={}
 const submittedFacts=JSON.parse(JSON.stringify(facts.value))
 const otherChanged=other.value.trim()!==(JSON.parse(initialFacts).otherContentDescription??'').trim()
 if(otherChanged)submittedFacts.otherContentDescription=other.value.trim()

 if(initialFacts!==JSON.stringify(submittedFacts))changes.CERTIFICATE_INFORMATION=JSON.stringify(submittedFacts)
 for(const field of fields.value)if(!field.key.startsWith('CE_')&&values[field.key]!==String((detail.value as any)[field.prop]??'')){if(!values[field.key].trim())throw new Error(`${field.label}变更值不能为空`);changes[field.key]=values[field.key].trim()}
 if(facts.value.certificateType!=='FDA'&&relation()!==initialRelation){if(entrusted.value===undefined||self.value===undefined||(!entrusted.value&&!self.value))throw new Error('生产选项不可同时为否');const names=enterpriseRows();if(entrusted.value&&!names.length)throw new Error('请填写受托企业');changes.PRODUCTION_RELATION='生产关系';data.append('entrustedProduction',String(entrusted.value));data.append('selfProduction',String(self.value));data.append('entrustedEnterprisesJson',JSON.stringify(entrusted.value?names:[]))}
 const hasSupportingFiles=attachments.value.length||descriptions.value.length||retainedFiles.value.some(f=>f.retained&&['CERTIFICATE_ATTACHMENT','FDA_PRODUCT_DESCRIPTION'].includes(f.fileKind))
 if(!Object.keys(changes).length&&!otherChanged&&!hasSupportingFiles)throw new Error('请至少修改一项内容')
 // Address and production relation have separate audit entries.
 if(changes.PRODUCTION_ADDRESS&&!changes.PRODUCTION_RELATION){data.append('entrustedProduction',String(entrusted.value));data.append('selfProduction',String(self.value));data.append('entrustedEnterprisesJson',JSON.stringify(entrusted.value?enterpriseRows():[]))}
 if(!Object.keys(changes).length&&hasSupportingFiles){changes.CERTIFICATE_INFORMATION=JSON.stringify(facts.value)}
 for(const [key,value]of Object.entries(changes)){data.append('changeTypes',key);data.append(`structuredValues[${key}]`,value)}
 if(otherChanged&&other.value.trim()){data.append('changeTypes','OTHER_CONTENT');data.append('otherDescription',other.value.trim())}
 data.append('expectedRowVersion',String(detail.value.rowVersion));data.append('approvalDate',approvalDate.value);if(files.value[0]?.raw)data.append('file',files.value[0].raw);for(const file of files.value.slice(1)){if(!file.raw)throw new Error('文件内容丢失，请重新选择');data.append('additionalPrimaryFiles',file.raw)}
 if(props.requestId){data.append('sourceRequestId',String(props.requestId));retainedFiles.value.filter(f=>f.retained).forEach(f=>data.append('retainedFileIds',String(f.id)))}
 for(const [key,list]of [['attachments',attachments.value],['productDescriptionFiles',descriptions.value]] as const)for(const file of list){if(!file.raw)throw new Error('文件内容丢失，请重新选择');data.append(key,file.raw)}
 saving.value=true;await submitRegistrationCertificateChange(detail.value.certificateId,data,`DCC-REG-CERT-CHANGE-${generateUUID()}`);useMessage().success('变更已提交审核');visible.value=false;emit('saved')
}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{saving.value=false}}
</script>
