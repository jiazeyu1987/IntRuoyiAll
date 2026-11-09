<template>
 <el-collapse class="mb-12px"><el-collapse-item title="分类组合筛选与日期范围" name="facts"><el-form inline label-width="120px">
  <el-form-item label="注册证类型"><el-select v-model="query.certificateType" clearable @change="changeType"><el-option label="国内" value="DOMESTIC"/><el-option label="CE" value="CE"/><el-option label="FDA" value="FDA"/></el-select></el-form-item>
  <el-form-item v-for="field in filterFields" :key="field" :label="factLabels[field]">
   <el-select v-if="factOptions(field,query.certificateType||'DOMESTIC').length" :model-value="query.fieldFilters?.[field]" clearable @update:model-value="value=>setFilter(field,value)"><el-option v-for="option in factOptions(field,query.certificateType||'DOMESTIC')" :key="String(option.value)" :label="option.label" :value="String(option.value)"/></el-select>
   <el-input v-else :model-value="query.fieldFilters?.[field]" clearable @update:model-value="value=>setFilter(field,value)"/>
  </el-form-item>
  <el-form-item v-if="query.certificateType==='CE'" label="证书Rev."><el-input v-model="query.ceRevision" clearable/></el-form-item>
  <el-form-item v-if="query.certificateType==='CE'" label="证书编号"><el-input v-model="query.ceCertificateNo" clearable/></el-form-item>
  <el-form-item v-for="date in dateFields" :key="date.start" :label="date.label"><el-date-picker v-model="query[date.start]" type="date" value-format="YYYY-MM-DD" placeholder="此日或之后" clearable/><span class="mx-4px">至</span><el-date-picker v-model="query[date.end]" type="date" value-format="YYYY-MM-DD" placeholder="此日或之前" clearable/></el-form-item>
  <el-form-item label="按字段排序"><el-select v-model="query.sortField" clearable><el-option v-for="key in sorts" :key="key" :value="key" :label="factLabels[key]||key"/></el-select><el-select v-model="query.sortOrder" class="ml-8px"><el-option label="正序" value="asc"/><el-option label="倒序" value="desc"/></el-select></el-form-item>
  <el-form-item><el-button type="primary" @click="apply">组合查询</el-button><el-button @click="clear">重置分类筛选</el-button></el-form-item>
 </el-form></el-collapse-item></el-collapse>
</template>
<script setup lang="ts">
import{computed}from'vue'
import{factLabels,factOptions}from'./facts'
const query=defineModel<any>('query',{required:true}),emit=defineEmits<{search:[]}>()
const filterFields=computed(()=>['ownProduct','certificateHolder','registrationLink',...(query.value.certificateType==='FDA'?['productNameEn','submissionType','kpNumber','regulationCode','productCode','listingNumber','customerHeld','approvalLetterStatus','factoryRegistrationStatus']:['certificateStatus',...(query.value.certificateType!=='CE'?['projectCode']:[]),...(query.value.certificateType==='CE'?['productNameEn','productCode','tradeName','notifiedBodyCode']:[])]),'remark','changeDescription',...(query.value.certificateType==='FDA'?[]:['entrustedProduction','selfProduction','entrustedEnterpriseName','structureComposition','intendedUse','technicalRequirements','residenceAddress'])])
const dateFields=computed(()=>query.value.certificateType==='FDA'?[{label:'批准日期',start:'approvalStart',end:'approvalEnd'},{label:'列名日期',start:'listingStart',end:'listingEnd'}]:query.value.certificateType==='CE'?[{label:'批准日期',start:'approvalStart',end:'approvalEnd'},{label:'签发日期',start:'ceIssueStart',end:'ceIssueEnd'},{label:'有效期至',start:'expiryStart',end:'expiryEnd'}]:[{label:'首次获证',start:'firstObtainedStart',end:'firstObtainedEnd'},{label:'批准日期',start:'approvalStart',end:'approvalEnd'},{label:'生效日期',start:'effectiveStart',end:'effectiveEnd'},{label:'有效期至',start:'expiryStart',end:'expiryEnd'}])
const sorts=computed(()=>Array.from(new Set(['certificateType','productName','classification','projectCode',...filterFields.value,...(query.value.certificateType==='FDA'?['approvalDate','listingDate']:query.value.certificateType==='CE'?['approvalDate','expiryDate','ceRevision','ceCertificateNo','ceIssueDate']:['certificateNo','firstObtainedDate','approvalDate','effectiveDate','expiryDate']),...(query.value.certificateType==='FDA'?[]:['registrantName','modelSpecification','productionAddress'])])))
const setFilter=(key:string,value:any)=>{query.value.fieldFilters??={};if(value===undefined||value===null||value==='')delete query.value.fieldFilters[key];else query.value.fieldFilters[key]=String(value)}
const apply=()=>{if(query.value.sortField&&!query.value.sortOrder)query.value.sortOrder='asc';if(!query.value.sortField)query.value.sortOrder=undefined;query.value.pageNo=1;emit('search')}
const changeType=()=>{query.value.fieldFilters={};query.value.ceCertificateNo=undefined;query.value.ceRevision=undefined;for(const field of ['firstObtainedStart','firstObtainedEnd','approvalStart','approvalEnd','effectiveStart','effectiveEnd','expiryStart','expiryEnd','ceIssueStart','ceIssueEnd','listingStart','listingEnd'])query.value[field]=undefined;query.value.sortField=undefined;query.value.sortOrder=undefined}
const clear=()=>{query.value.certificateType=undefined;changeType();apply()}
</script>
<style scoped>.el-select{width:190px}.el-input{width:190px}.el-date-editor{width:155px}</style>
