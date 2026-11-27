<template>
  <el-card shadow="never" class="mb-16px">
    <template #header><div class="binding-header"><span>关联产品</span><el-button v-if="editable" v-hasPermi="['dcc:registration-certificate:upload:create']" type="primary" plain @click="open">追加产品</el-button></div></template>
    <el-table :data="products" empty-text="未关联系统产品">
      <el-table-column prop="productCode" label="DCC产品编码"/>
      <el-table-column prop="productName" label="产品名称"/>
    </el-table>
    <el-dialog v-model="visible" title="为现有注册证追加产品" width="680px" append-to-body :close-on-click-modal="!saving" :show-close="!saving">
      <el-alert title="同一产品可关联多张注册证；本注册证内不能重复关联同一产品。" type="info" :closable="false" class="mb-16px"/>
      <el-select v-model="selected" multiple filterable remote :remote-method="search" :loading="loading" placeholder="选择系统项目代码" class="w-full">
        <el-option v-for="item in candidates" :key="item.id" :value="item.id" :label="[item.projectCode,item.projectName].filter(Boolean).join(' - ')" :disabled="item.disabled">
          {{ [item.projectCode,item.projectName].filter(Boolean).join(' - ') }} {{ item.bindingMessage }}
        </el-option>
      </el-select>
      <el-table :data="preview" class="mt-16px"><el-table-column prop="productCode" label="DCC产品编码"/><el-table-column prop="productName" label="产品名称"/></el-table>
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-16px"/>
      <template #footer><el-button :disabled="saving" @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" :disabled="!selected.length || resolving || !!error" @click="save">确认追加</el-button></template>
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { ref,watch } from 'vue'
import { addRegistrationCertificateProducts,getUploadProjectCodes,getUploadProjectProduct,type DccRegistrationCertificateProjectProduct,type DccRegistrationCertificateUploadProjectCodeRespVO } from '@/api/dcc/registrationCertificate'
const props=defineProps<{certificateId:number|string;rowVersion:number;products:DccRegistrationCertificateProjectProduct[];editable:boolean}>()
const emit=defineEmits<{saved:[]}>()
const visible=ref(false),saving=ref(false),loading=ref(false),resolving=ref(false),error=ref('')
const selected=ref<Array<number|string>>([]),preview=ref<DccRegistrationCertificateProjectProduct[]>([]),candidates=ref<DccRegistrationCertificateUploadProjectCodeRespVO[]>([])
const search=async(keyword='')=>{loading.value=true;try{candidates.value=await getUploadProjectCodes({keyword})}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{loading.value=false}}
const open=async()=>{selected.value=[];preview.value=[];error.value='';visible.value=true;await search()}
let revision=0
watch(selected,async ids=>{const version=++revision;error.value='';preview.value=[];resolving.value=true;try{
 const products=await Promise.all(ids.map(getUploadProjectProduct));if(version!==revision)return
 if(new Set(products.map(p=>String(p.productCatalogId))).size!==products.length)throw new Error('多个项目指向同一产品，请仅保留一个项目')
 if(products.some(p=>props.products.some(old=>String(old.productCatalogId)===String(p.productCatalogId))))throw new Error('所选产品已关联本注册证')
 preview.value=products
}catch(e){if(version===revision)error.value=e instanceof Error?e.message:String(e)}finally{if(version===revision)resolving.value=false}})
const save=async()=>{saving.value=true;error.value='';try{await addRegistrationCertificateProducts(props.certificateId,{expectedRowVersion:props.rowVersion,projectCodeIds:selected.value});visible.value=false;emit('saved')}catch(e){error.value=e instanceof Error?e.message:String(e)}finally{saving.value=false}}
</script>
<style scoped>.binding-header{display:flex;align-items:center;justify-content:space-between}</style>
