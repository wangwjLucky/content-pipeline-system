import request from './request'

export function getTasks(params: { page?: number; size?: number; status?: string }) {
  return request.get('/tasks', { params })
}

export function getTask(id: number) {
  return request.get(`/tasks/${id}`)
}

export function createTask(data: { topicId: number; title: string; contentType?: string }) {
  return request.post('/tasks', data)
}

export function cancelTask(id: number, data?: { operator?: string; comment?: string }) {
  return request.post(`/tasks/${id}/cancel`, data)
}

export function retryTask(id: number, data?: { operator?: string }) {
  return request.post(`/tasks/${id}/retry`, data)
}

export function getTaskTimeline(id: number) {
  return request.get(`/tasks/${id}/timeline`)
}