# User Endpoints

---

### GET `/api/v1/users/me`
Returns the profile of the currently authenticated user.

**Response `200 OK`:**
```json
{
  "id": "uuid",
  "username": "alumno",
  "email": "alumno@reitera.com",
  "firstName": "Alumno",
  "lastName": "Demo",
  "roleName": "STUDENT"
}
```
