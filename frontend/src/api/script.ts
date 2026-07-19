import request from './request'

export function getScripts(params: { page?: number; size?: number; taskId?: number; status?: string }) {
  return request.get('/scripts', { params })
}

export function getScript(id: number) {
  return request.get(`/scripts/${id}`)
}

export function updateScript(id: number, data: { content?: string; subtitle?: string }) {
  return request.put(`/scripts/${id}`, data)
}

export function approveScript(id: number) {
  return request.post(`/scripts/${id}/approve`)
}

export function rejectScript(id: number, reason?: string) {
  return request.post(`/scripts/${id}/reject`, { reason })
}

export function reviewScript(id: number, action: 'approve' | 'reject', reviewerId?: number, reason?: string) {
  return request.post(`/scripts/${id}/review`, { action, reviewerId, reason })
}

export function generateScript(data: { taskId: number; topicTitle: string }) {
  return request.post('/scripts/generate', data)
}