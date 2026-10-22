import request from '@/config/axios'

export interface CodexWebStatusVO {
  enabled: boolean
  mcpServerEnabled: boolean
  mcpConfigured: boolean
  mcpServerName: string
  mcpServerUrl?: string
  message: string
}

export const CodexWebApi = {
  getStatus: async () => {
    return await request.get<CodexWebStatusVO>({ url: '/ai/codex-web/status' })
  },

  createConversation: async () => {
    return await request.post<number>({ url: '/ai/codex-web/conversation/create' })
  }
}
