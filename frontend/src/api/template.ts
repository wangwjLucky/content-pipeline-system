import request from './request'

export function getTemplates(params: { page?: number; size?: number; type?: string }) {
  return request.get('/templates', { params })
}

export function getTemplate(id: number) {
  return request.get(`/templates/${id}`)
}

export function createTemplate(data: any) {
  return request.post('/templates', data)
}

export function updateTemplate(id: number, data: any) {
  return request.put(`/templates/${id}`, data)
}

export function deleteTemplate(id: number) {
  return request.delete(`/templates/${id}`)
}