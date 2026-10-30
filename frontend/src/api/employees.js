import client from './client'

/** GET /api/employees with search, filters, sorting and pagination as query parameters. */
export const searchEmployees = (params) => client.get('/employees', { params }).then((r) => r.data)

export const getEmployee = (id) => client.get(`/employees/${id}`).then((r) => r.data)

export const getPersonalDetails = (id) => client.get(`/employees/${id}/personal-details`).then((r) => r.data)

export const createEmployee = (data) => client.post('/employees', data).then((r) => r.data)

export const updateEmployee = (id, data) => client.put(`/employees/${id}`, data).then((r) => r.data)

export const deactivateEmployee = (id) => client.patch(`/employees/${id}/deactivate`).then((r) => r.data)

export const transferEmployee = (id, data) => client.post(`/employees/${id}/transfer`, data).then((r) => r.data)

export const getTransferHistory = (id) => client.get(`/employees/${id}/transfers`).then((r) => r.data)
