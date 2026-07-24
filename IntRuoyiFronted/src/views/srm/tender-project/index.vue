<template>
  <ContentWrap>
    <el-form ref="queryFormRef" class="-mb-15px" :inline="true" :model="queryParams" label-width="78px">
      <el-form-item label="项目编号" prop="projectNo">
        <el-input v-model="queryParams.projectNo" clearable class="!w-210px" placeholder="请输入项目编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="项目标题" prop="projectTitle">
        <el-input v-model="queryParams.projectTitle" clearable class="!w-220px" placeholder="请输入项目标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="projectStatus">
        <el-select v-model="queryParams.projectStatus" clearable class="!w-170px" placeholder="全部">
          <el-option v-for="item in srmTenderProjectStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="项目编号" prop="projectNo" width="170" />
      <el-table-column label="项目标题" prop="projectTitle" min-width="190" />
      <el-table-column label="状态" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveStatusType(row.projectStatus)">{{ row.projectStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源计划" prop="sourcePlanNo" width="170" />
      <el-table-column label="预计金额" prop="expectedAmount" width="120" align="right" />
      <el-table-column label="投标数" width="86" align="center">
        <template #default="{ row }">{{ row.submissions?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="评委数" width="86" align="center">
        <template #default="{ row }">{{ row.committeeMembers?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="候选数" width="86" align="center">
        <template #default="{ row }">{{ row.candidates?.length || 0 }}</template>
      </el-table-column>
      <el-table-column label="中标供应商" prop="dealSupplierName" min-width="160" />
      <el-table-column label="中标金额" prop="dealAmount" width="120" align="right" />
      <el-table-column label="投标截止" prop="submissionEndTime" width="180" :formatter="dateFormatter" />
      <el-table-column label="操作" width="360" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" :disabled="row.projectStatus !== 'DRAFT'" @click="openPublishDialog(row)" v-hasPermi="['srm:tender-project:publish']">发布</el-button>
          <el-button link type="warning" :disabled="row.projectStatus !== 'PUBLISHED'" @click="openBidDialog(row)" v-hasPermi="['srm:tender-project:submit-bid']">投标</el-button>
          <el-button link type="info" :disabled="row.projectStatus !== 'PUBLISHED'" @click="openExpertDialog(row)" v-hasPermi="['srm:tender-project:expert']">专家</el-button>
          <el-button link type="primary" :disabled="row.projectStatus !== 'PUBLISHED'" @click="openCommitteeDialog(row)" v-hasPermi="['srm:tender-project:committee']">评委会</el-button>
          <el-button link type="success" :disabled="!canCreateCandidate(row)" @click="openCandidateDialog(row)" v-hasPermi="['srm:tender-project:candidate']">候选</el-button>
          <el-button link type="success" :disabled="!canConfirmWinning(row)" @click="openWinningDialog(row)" v-hasPermi="['srm:tender-project:winning']">中标</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="publishVisible" title="发布招标项目" width="760px">
    <el-form ref="publishFormRef" :model="publishForm" :rules="publishRules" label-width="110px">
      <el-form-item label="公告标题" prop="noticeTitle">
        <el-input v-model="publishForm.noticeTitle" placeholder="请输入公告标题" />
      </el-form-item>
      <el-form-item label="公告附件" prop="noticeAttachmentUrl">
        <el-input v-model="publishForm.noticeAttachmentUrl" placeholder="请输入真实公告附件 URL" />
      </el-form-item>
      <el-form-item label="标书名称" prop="documentName">
        <el-input v-model="publishForm.documentName" placeholder="请输入标书名称" />
      </el-form-item>
      <el-form-item label="标书附件" prop="documentAttachmentUrl">
        <el-input v-model="publishForm.documentAttachmentUrl" placeholder="请输入真实标书附件 URL" />
      </el-form-item>
      <el-form-item label="投标开始" prop="submissionStartTime">
        <el-date-picker v-model="publishForm.submissionStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="投标截止" prop="submissionEndTime">
        <el-date-picker v-model="publishForm.submissionEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="publishVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitPublish">发布</el-button>
    </template>
  </Dialog>

  <Dialog v-model="bidVisible" title="提交供应商投标" width="680px">
    <el-form ref="bidFormRef" :model="bidForm" :rules="bidRules" label-width="100px">
      <el-form-item label="供应商ID" prop="supplierId">
        <el-input-number v-model="bidForm.supplierId" :min="1" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="投标金额" prop="bidAmount">
        <el-input-number v-model="bidForm.bidAmount" :min="0.01" :precision="2" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="投标附件" prop="attachmentUrl">
        <el-input v-model="bidForm.attachmentUrl" placeholder="请输入投标附件 URL" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="bidVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitBid">提交投标</el-button>
    </template>
  </Dialog>

  <Dialog v-model="expertVisible" title="创建并通过招标专家" width="720px">
    <el-form ref="expertFormRef" :model="expertForm" :rules="expertRules" label-width="100px">
      <el-form-item label="专家姓名" prop="expertNamesText">
        <el-input v-model="expertForm.expertNamesText" placeholder="请输入专家姓名，多个用逗号分隔" />
      </el-form-item>
      <el-form-item label="专业类型" prop="specialtyType">
        <el-input v-model="expertForm.specialtyType" placeholder="请输入专业类型" />
      </el-form-item>
      <el-form-item label="审核意见" prop="auditRemark">
        <el-input v-model="expertForm.auditRemark" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-alert title="专家创建成功后会立即调用通过接口，返回的真实专家 ID 会用于本次评委会组建。" type="info" show-icon :closable="false" />
      <el-table v-if="createdExpertRows.length" class="mt-16px" :data="createdExpertRows" border size="small">
        <el-table-column label="专家ID" prop="id" width="120" />
        <el-table-column label="专家姓名" prop="expertName" min-width="160" />
        <el-table-column label="专业类型" prop="specialtyType" min-width="160" />
      </el-table>
    </el-form>
    <template #footer>
      <el-button @click="expertVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitExperts">创建并通过</el-button>
    </template>
  </Dialog>

  <Dialog v-model="committeeVisible" title="组建评标委员会" width="720px">
    <el-form ref="committeeFormRef" :model="committeeForm" :rules="committeeRules" label-width="120px">
      <el-form-item label="专家产生方式" prop="applicationMethod">
        <el-select v-model="committeeForm.applicationMethod" class="!w-1/1">
          <el-option v-for="item in srmTenderApplicationMethodOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="要求专业类型" prop="requiredSpecialtyType">
        <el-input v-model="committeeForm.requiredSpecialtyType" placeholder="请输入要求专业类型" />
      </el-form-item>
      <el-form-item label="要求专家人数" prop="requiredExpertCount">
        <el-input-number v-model="committeeForm.requiredExpertCount" :min="1" class="!w-1/1" controls-position="right" />
      </el-form-item>
      <el-form-item label="专家ID列表" prop="expertIdsText">
        <el-input v-model="committeeForm.expertIdsText" placeholder="请输入已审核专家 ID，多个用逗号分隔" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="committeeVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitCommittee">组建评委会</el-button>
    </template>
  </Dialog>

  <Dialog v-model="candidateVisible" title="生成中标候选" width="680px">
    <el-form ref="candidateFormRef" :model="candidateForm" :rules="candidateRules" label-width="100px">
      <el-form-item label="投标记录" prop="submissionIds">
        <el-checkbox-group v-model="candidateForm.submissionIds">
          <el-checkbox v-for="submission in currentProject?.submissions || []" :key="submission.id" :label="submission.id">
            {{ submission.supplierName }} / {{ submission.bidAmount }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="candidateVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitCandidates">生成候选</el-button>
    </template>
  </Dialog>

  <Dialog v-model="winningVisible" title="确认中标结果" width="680px">
    <el-form ref="winningFormRef" :model="winningForm" :rules="winningRules" label-width="100px">
      <el-form-item label="中标候选" prop="candidateId">
        <el-select v-model="winningForm.candidateId" class="!w-1/1" placeholder="请选择中标候选">
          <el-option v-for="candidate in currentProject?.candidates || []" :key="candidate.id" :label="`${candidate.rankNo}. ${candidate.supplierName} / ${candidate.bidAmount}`" :value="candidate.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="中标说明" prop="winningRemark">
        <el-input v-model="winningForm.winningRemark" type="textarea" :rows="4" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="winningVisible = false">取消</el-button>
      <el-button type="primary" :loading="actionLoading" @click="submitWinning">确认中标</el-button>
    </template>
  </Dialog>

  <Dialog v-model="detailVisible" title="招标项目详情" width="1040px">
    <el-descriptions v-if="currentProject" :column="3" border>
      <el-descriptions-item label="项目编号">{{ currentProject.projectNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ currentProject.projectStatusLabel }}</el-descriptions-item>
      <el-descriptions-item label="来源计划">{{ currentProject.sourcePlanNo }}</el-descriptions-item>
      <el-descriptions-item label="投标时间">{{ currentProject.submissionStartTime || '-' }} 至 {{ currentProject.submissionEndTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="中标供应商">{{ currentProject.dealSupplierName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="合同ID">{{ currentProject.contractId || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-tabs class="mt-16px">
      <el-tab-pane label="公告与标书">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公告标题">{{ currentProject?.notice?.noticeTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公告附件">{{ currentProject?.notice?.noticeAttachmentUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标书名称">{{ currentProject?.document?.documentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="标书附件">{{ currentProject?.document?.documentAttachmentUrl || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
      <el-tab-pane label="投标记录">
        <el-table :data="currentProject?.submissions || []" border size="small">
          <el-table-column label="投标ID" prop="id" width="90" />
          <el-table-column label="供应商" prop="supplierName" min-width="180" />
          <el-table-column label="投标金额" prop="bidAmount" width="120" align="right" />
          <el-table-column label="投标人" prop="submittedName" width="120" />
          <el-table-column label="投标时间" prop="submittedTime" width="180" :formatter="dateFormatter" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="评委会">
        <el-table :data="currentProject?.committeeMembers || []" border size="small">
          <el-table-column label="成员ID" prop="id" width="90" />
          <el-table-column label="专家ID" prop="expertId" width="100" />
          <el-table-column label="专家姓名" prop="expertName" min-width="160" />
          <el-table-column label="专业类型" prop="specialtyType" min-width="160" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="候选与中标">
        <el-table :data="currentProject?.candidates || []" border size="small">
          <el-table-column label="排名" prop="rankNo" width="80" />
          <el-table-column label="供应商" prop="supplierName" min-width="180" />
          <el-table-column label="投标金额" prop="bidAmount" width="120" align="right" />
          <el-table-column label="候选状态" prop="candidateStatus" width="120" />
        </el-table>
        <el-descriptions class="mt-16px" :column="2" border>
          <el-descriptions-item label="中标供应商">{{ currentProject?.winningResult?.supplierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="中标金额">{{ currentProject?.winningResult?.winningAmount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="确认人">{{ currentProject?.winningResult?.confirmedName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="中标说明">{{ currentProject?.winningResult?.winningRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>
  </Dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import {
  SrmTenderProjectApi,
  srmTenderApplicationMethodOptions,
  srmTenderProjectStatusOptions,
  type SrmTenderProjectVO
} from '@/api/srm/tender-project'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'SrmTenderProject' })

const message = useMessage()
const loading = ref(false)
const actionLoading = ref(false)
const list = ref<SrmTenderProjectVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  projectNo: undefined as string | undefined,
  projectTitle: undefined as string | undefined,
  projectStatus: undefined as string | undefined
})

const publishVisible = ref(false)
const publishFormRef = ref<FormInstance>()
const publishForm = reactive({
  projectId: undefined as number | undefined,
  noticeTitle: '',
  noticeAttachmentUrl: '',
  documentName: '',
  documentAttachmentUrl: '',
  submissionStartTime: '',
  submissionEndTime: ''
})
const publishRules = reactive<FormRules>({
  noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeAttachmentUrl: [{ required: true, message: '请输入公告附件 URL', trigger: 'blur' }],
  documentName: [{ required: true, message: '请输入标书名称', trigger: 'blur' }],
  documentAttachmentUrl: [{ required: true, message: '请输入标书附件 URL', trigger: 'blur' }],
  submissionStartTime: [{ required: true, message: '请选择投标开始时间', trigger: 'change' }],
  submissionEndTime: [{ required: true, message: '请选择投标截止时间', trigger: 'change' }]
})

const bidVisible = ref(false)
const bidFormRef = ref<FormInstance>()
const bidForm = reactive({
  projectId: undefined as number | undefined,
  supplierId: undefined as number | undefined,
  bidAmount: 0,
  attachmentUrl: ''
})
const bidRules = reactive<FormRules>({
  supplierId: [{ required: true, message: '请输入供应商 ID', trigger: 'change' }],
  bidAmount: [{ required: true, message: '请输入投标金额', trigger: 'change' }]
})

const expertVisible = ref(false)
const expertFormRef = ref<FormInstance>()
const expertForm = reactive({
  projectId: undefined as number | undefined,
  expertNamesText: '',
  specialtyType: '',
  auditRemark: ''
})
const expertRules = reactive<FormRules>({
  expertNamesText: [{ required: true, message: '请输入专家姓名', trigger: 'blur' }],
  specialtyType: [{ required: true, message: '请输入专家专业类型', trigger: 'blur' }]
})
const createdExpertRows = ref<Array<{ id: number; expertName: string; specialtyType: string }>>([])

const committeeVisible = ref(false)
const committeeFormRef = ref<FormInstance>()
const committeeForm = reactive({
  projectId: undefined as number | undefined,
  applicationMethod: 'DIRECT',
  requiredSpecialtyType: '',
  requiredExpertCount: 1,
  expertIdsText: ''
})
const committeeRules = reactive<FormRules>({
  applicationMethod: [{ required: true, message: '请选择专家产生方式', trigger: 'change' }],
  requiredSpecialtyType: [{ required: true, message: '请输入要求专业类型', trigger: 'blur' }],
  requiredExpertCount: [{ required: true, message: '请输入要求专家人数', trigger: 'change' }],
  expertIdsText: [{ required: true, message: '请输入专家 ID', trigger: 'blur' }]
})

const candidateVisible = ref(false)
const candidateFormRef = ref<FormInstance>()
const candidateForm = reactive({
  projectId: undefined as number | undefined,
  submissionIds: [] as number[]
})
const candidateRules = reactive<FormRules>({
  submissionIds: [{ required: true, message: '请选择投标记录', trigger: 'change' }]
})

const winningVisible = ref(false)
const winningFormRef = ref<FormInstance>()
const winningForm = reactive({
  projectId: undefined as number | undefined,
  candidateId: undefined as number | undefined,
  winningRemark: ''
})
const winningRules = reactive<FormRules>({
  candidateId: [{ required: true, message: '请选择中标候选', trigger: 'change' }],
  winningRemark: [{ required: true, message: '请输入中标说明', trigger: 'blur' }]
})

const detailVisible = ref(false)
const currentProject = ref<SrmTenderProjectVO>()

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveStatusType = (status?: string) => {
  if (status === 'WINNING_CONFIRMED' || status === 'CONTRACT_CREATED') return 'success'
  if (status === 'COMMITTEE_CONFIRMED' || status === 'CANDIDATE_CONFIRMED') return 'warning'
  if (status === 'PUBLISHED') return 'primary'
  return 'info'
}

const canCreateCandidate = (row: SrmTenderProjectVO) =>
  row.projectStatus === 'COMMITTEE_CONFIRMED' && (row.submissions?.length || 0) > 0

const canConfirmWinning = (row: SrmTenderProjectVO) =>
  row.projectStatus === 'CANDIDATE_CONFIRMED' && (row.candidates?.length || 0) > 0

const getList = async () => {
  loading.value = true
  try {
    const data = await SrmTenderProjectApi.getProjectPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '招标项目列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery(true)
}

const parseDateTimeMillis = (value: string, label: string) => {
  const millis = new Date(value.replace(' ', 'T')).getTime()
  if (!Number.isFinite(millis)) {
    throw new Error(`${label}格式无效，请重新选择。`)
  }
  return millis
}

const parseIds = (value: string) =>
  value
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)

const parseExpertNames = (value: string) =>
  value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

const openPublishDialog = (row: SrmTenderProjectVO) => {
  publishForm.projectId = row.id
  publishForm.noticeTitle = `${row.projectTitle}招标公告`
  publishForm.noticeAttachmentUrl = 'http://127.0.0.1:9000/yudao/srm/tender/notice.pdf'
  publishForm.documentName = `${row.projectTitle}标书`
  publishForm.documentAttachmentUrl = 'http://127.0.0.1:9000/yudao/srm/tender/document.pdf'
  publishForm.submissionStartTime = ''
  publishForm.submissionEndTime = ''
  publishVisible.value = true
}

const submitPublish = async () => {
  await publishFormRef.value?.validate()
  if (!publishForm.projectId) return
  actionLoading.value = true
  try {
    const submissionStartTime = parseDateTimeMillis(publishForm.submissionStartTime, '投标开始时间')
    const submissionEndTime = parseDateTimeMillis(publishForm.submissionEndTime, '投标截止时间')
    if (submissionEndTime <= submissionStartTime) {
      throw new Error('投标截止时间必须晚于投标开始时间。')
    }
    await SrmTenderProjectApi.publishProject({
      projectId: publishForm.projectId,
      noticeTitle: publishForm.noticeTitle,
      noticeAttachmentUrl: publishForm.noticeAttachmentUrl,
      documentName: publishForm.documentName,
      documentAttachmentUrl: publishForm.documentAttachmentUrl,
      submissionStartTime,
      submissionEndTime
    })
    publishVisible.value = false
    message.success('招标项目已发布')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '招标项目发布失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openBidDialog = (row: SrmTenderProjectVO) => {
  bidForm.projectId = row.id
  bidForm.supplierId = 103
  bidForm.bidAmount = Number(row.expectedAmount || 0)
  bidForm.attachmentUrl = 'http://127.0.0.1:9000/yudao/srm/tender/bid.pdf'
  bidVisible.value = true
}

const submitBid = async () => {
  await bidFormRef.value?.validate()
  if (!bidForm.projectId || !bidForm.supplierId) return
  actionLoading.value = true
  try {
    await SrmTenderProjectApi.submitBid({
      projectId: bidForm.projectId,
      supplierId: bidForm.supplierId,
      bidAmount: bidForm.bidAmount,
      attachmentUrl: bidForm.attachmentUrl
    })
    bidVisible.value = false
    message.success('供应商投标已提交')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商投标提交失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openExpertDialog = (row: SrmTenderProjectVO) => {
  expertForm.projectId = row.id
  expertForm.expertNamesText = '招标专家一,招标专家二'
  expertForm.specialtyType = '医疗耗材'
  expertForm.auditRemark = '专家资格审核通过'
  expertVisible.value = true
}

const submitExperts = async () => {
  await expertFormRef.value?.validate()
  actionLoading.value = true
  try {
    const expertNames = parseExpertNames(expertForm.expertNamesText)
    if (!expertNames.length) {
      throw new Error('至少需要创建一名专家。')
    }
    const createdRows: Array<{ id: number; expertName: string; specialtyType: string }> = []
    for (const expertName of expertNames) {
      const id = await SrmTenderProjectApi.createExpert({
        expertName,
        specialtyType: expertForm.specialtyType
      })
      await SrmTenderProjectApi.approveExpert({
        id,
        auditRemark: expertForm.auditRemark
      })
      createdRows.push({ id, expertName, specialtyType: expertForm.specialtyType })
    }
    createdExpertRows.value = createdRows
    committeeForm.requiredSpecialtyType = expertForm.specialtyType
    committeeForm.requiredExpertCount = createdRows.length
    committeeForm.expertIdsText = createdRows.map((item) => item.id).join(',')
    message.success('招标专家已创建并审核通过')
  } catch (error) {
    message.error(resolveErrorMessage(error, '招标专家创建或审核失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openCommitteeDialog = (row: SrmTenderProjectVO) => {
  committeeForm.projectId = row.id
  if (!committeeForm.requiredSpecialtyType) {
    committeeForm.requiredSpecialtyType = '医疗耗材'
  }
  if (!committeeForm.expertIdsText && createdExpertRows.value.length) {
    committeeForm.expertIdsText = createdExpertRows.value.map((item) => item.id).join(',')
    committeeForm.requiredExpertCount = createdExpertRows.value.length
  }
  committeeVisible.value = true
}

const submitCommittee = async () => {
  await committeeFormRef.value?.validate()
  if (!committeeForm.projectId) return
  actionLoading.value = true
  try {
    const expertIds = parseIds(committeeForm.expertIdsText)
    if (expertIds.length < committeeForm.requiredExpertCount) {
      throw new Error('专家 ID 数量不能少于要求专家人数。')
    }
    await SrmTenderProjectApi.formCommittee({
      projectId: committeeForm.projectId,
      applicationMethod: committeeForm.applicationMethod,
      requiredSpecialtyType: committeeForm.requiredSpecialtyType,
      requiredExpertCount: committeeForm.requiredExpertCount,
      expertIds
    })
    committeeVisible.value = false
    message.success('评标委员会已组建')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '评标委员会组建失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openCandidateDialog = async (row: SrmTenderProjectVO) => {
  currentProject.value = await SrmTenderProjectApi.getProject(row.id)
  candidateForm.projectId = row.id
  candidateForm.submissionIds = currentProject.value.submissions.map((item) => item.id)
  candidateVisible.value = true
}

const submitCandidates = async () => {
  await candidateFormRef.value?.validate()
  if (!candidateForm.projectId) return
  actionLoading.value = true
  try {
    await SrmTenderProjectApi.createCandidates({
      projectId: candidateForm.projectId,
      submissionIds: candidateForm.submissionIds
    })
    candidateVisible.value = false
    message.success('中标候选已生成')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '中标候选生成失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openWinningDialog = async (row: SrmTenderProjectVO) => {
  currentProject.value = await SrmTenderProjectApi.getProject(row.id)
  winningForm.projectId = row.id
  winningForm.candidateId = currentProject.value.candidates?.[0]?.id
  winningForm.winningRemark = '确认第一候选人为中标供应商'
  winningVisible.value = true
}

const submitWinning = async () => {
  await winningFormRef.value?.validate()
  if (!winningForm.projectId || !winningForm.candidateId) return
  actionLoading.value = true
  try {
    await SrmTenderProjectApi.confirmWinning({
      projectId: winningForm.projectId,
      candidateId: winningForm.candidateId,
      winningRemark: winningForm.winningRemark
    })
    winningVisible.value = false
    message.success('中标结果已确认')
    await getList()
  } catch (error) {
    message.error(resolveErrorMessage(error, '中标结果确认失败。'))
    throw error
  } finally {
    actionLoading.value = false
  }
}

const openDetail = async (row: SrmTenderProjectVO) => {
  try {
    currentProject.value = await SrmTenderProjectApi.getProject(row.id)
    detailVisible.value = true
  } catch (error) {
    message.error(resolveErrorMessage(error, '招标项目详情加载失败。'))
    throw error
  }
}

onMounted(() => {
  getList()
})
</script>
