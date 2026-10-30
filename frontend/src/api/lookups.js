import client from './client'

export const getDepartments = () => client.get('/departments').then((r) => r.data)

export const getRoles = () => client.get('/roles').then((r) => r.data)
