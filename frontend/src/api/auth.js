import client from './client'

/** Returns { token, tokenType, expiresAt, user: { userId, username, role, employeeId } }. */
export const login = (username, password) =>
  client.post('/auth/login', { username, password }).then((r) => r.data)
