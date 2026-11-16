import request from '@/config/axios'

export interface TeamRosterImportPerson {
  name: string
  role: string
  username?: string
  result: string
}

export const importTeamRoster = (file: File, mode: 'leader' | 'members') => {
  const data = new FormData()
  data.append('file', file)
  return request.post<TeamRosterImportPerson[]>({
    url: `/mes/pro/process-pool/team-leader/roster/import-${mode}`,
    headersType: 'multipart/form-data',
    data,
    timeout: 150000
  })
}
