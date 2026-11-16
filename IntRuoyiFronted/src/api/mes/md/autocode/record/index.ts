import request from '@/config/axios'

// MES 编码生成 API
export const AutoCodeRecordApi = {
  // 生成编码
  generateAutoCode: async (ruleCode: string, inputChar?: string, projectId?: number) => {
    return await request.post({
      url: `/mes/md/auto-code-record/generate`,
      data: { ruleCode, inputChar, projectId }
    })
  }
}
