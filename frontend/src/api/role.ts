import request from './request'

export function getRoles(params: { page?: number; size?: number }) {
  return request.get('/roles', { params })
}

export function getRole(id: number) {
  return request.get(`/roles/${id}`)
}

export function createRole(data: { name: string; code: string; description?: string }) {
  return request.post('/roles', data)
}

export function updateRole(id: number, data: any) {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/roles/${id}`)
}