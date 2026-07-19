import request from './request'

export function getTopics(params: { page?: number; size?: number; status?: string }) {
  return request.get('/topics', { params })
}

export function getTopic(id: number) {
  return request.get(`/topics/${id}`)
}

export function createTopic(data: { title: string; source?: string }) {
  return request.post('/topics', data)
}

export function updateTopic(id: number, data: { title?: string; source?: string; status?: string }) {
  return request.put(`/topics/${id}`, data)
}

export function deleteTopic(id: number) {
  return request.delete(`/topics/${id}`)
}

export function generateTask(id: number) {
  return request.post(`/topics/${id}/generate-task`)
}