# Employee Attendance Management System

A backend employee attendance management system built with Java and Spring Boot.

The application provides employee registration, JWT-based authentication, role-based authorization, work schedule management, attendance tracking, worked-time calculation, and attendance history.

## Features

- Employee registration
- BCrypt password hashing
- JWT-based stateless authentication
- Bearer token authorization
- Role-based authorization with `EMPLOYEE` and `ADMIN` roles
- Inactive account protection
- Admin work schedule management
- Employee check-in and check-out
- Duplicate check-in prevention
- Check-out validation
- Automatic worked-time calculation
- Comparison against required working time
- `COMPLETED` and `INCOMPLETE` attendance status
- Employee attendance history
- Persistent attendance calculation results
- Custom exception handling
- Input validation
- Microsoft SQL Server integration
- Automated testing with JUnit, Mockito, and MockMvc

## Technology Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Hibernate
- Microsoft SQL Server
- Maven
- Lombok
- JJWT
- JUnit
- Mockito
- MockMvc
- Postman

## Project Structure

```text
src/
├── main/
│   └── java/com/example/attendance/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── exception/
│       ├── repository/
│       └── service/
│
└── test/
    └── java/com/example/attendance/
        ├── config/
        └── service/
```

## Roles and Authorization

The system currently supports two roles.

### EMPLOYEE

Employees can:

- Log in
- Check in
- Check out
- View their own attendance history

Employees cannot manage employee work schedules.

### ADMIN

Administrators can:

- Access attendance functionality
- Create employee work schedules
- Update employee work schedules
- View employee work schedules

Work schedule endpoints are protected with Spring Security and require the `ADMIN` role.

## Authentication

Authentication is JWT-based and stateless.

When a user logs in successfully:

1. The submitted email and password are authenticated by Spring Security.
2. A signed JWT is generated.
3. The JWT is returned to the client.
4. The client includes the token in the `Authorization` header on protected requests.

Example:

```http
Authorization: Bearer <JWT_TOKEN>
```

The application does not create a server-side login session.

Passwords are stored using BCrypt hashes rather than plain-text passwords.

Inactive users cannot authenticate.

JWT validation also checks whether the user's account is still enabled. This means an already-issued token will no longer authenticate a user after the account is deactivated.

## JWT Configuration

JWT configuration is stored in `application.properties` using an environment variable for the signing secret.

```properties
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

The current expiration value is:

```text
86400000 milliseconds = 24 hours
```

The real signing secret is not stored in the repository.

The application expects:

```text
JWT_SECRET
```

as an environment variable.

## API Endpoints

### Authentication

#### Register Employee

```http
POST /api/auth/register
```

Example request:

```json
{
  "name": "Example Employee",
  "email": "employee@example.com",
  "password": "Password123"
}
```

Newly registered users receive the `EMPLOYEE` role by default.

#### Login

```http
POST /api/auth/login
```

Example request:

```json
{
  "email": "employee@example.com",
  "password": "Password123"
}
```

Example response:

```json
{
  "id": 2,
  "name": "Example Employee",
  "email": "employee@example.com",
  "role": "EMPLOYEE",
  "active": true,
  "token": "<JWT_TOKEN>",
  "tokenType": "Bearer"
}
```

### Logout

Because authentication is stateless, there is no server-side HTTP session to invalidate.

Logout is performed by the client by deleting or discarding the JWT.

A production system requiring immediate server-side token revocation could add:

- Token revocation or blacklisting
- Short-lived access tokens
- Refresh tokens

## Attendance

Attendance endpoints require a valid JWT belonging to an `EMPLOYEE` or `ADMIN`.

### Check In

```http
POST /api/attendance/check-in
```

Header:

```http
Authorization: Bearer <JWT_TOKEN>
```

No request body is required.

The authenticated employee is determined from the JWT authentication context rather than from a client-supplied user ID.

An employee cannot create another check-in while an open attendance record already exists.

### Check Out

```http
POST /api/attendance/check-out
```

Header:

```http
Authorization: Bearer <JWT_TOKEN>
```

No request body is required.

During checkout, the system:

1. Finds the authenticated employee.
2. Finds the employee's active attendance record.
3. Finds the employee's required work schedule.
4. Records the checkout time.
5. Calculates the total worked minutes.
6. Compares worked minutes against required minutes.
7. Calculates the difference.
8. Determines whether the required time was completed.
9. Stores the calculated result in the attendance record.

Example response:

```json
{
  "id": 4,
  "userId": 2,
  "employeeName": "Second Employee",
  "employeeEmail": "second.employee@example.com",
  "workDate": "2026-09-06",
  "checkInTime": "2026-09-06T12:39:49.1093306",
  "checkOutTime": "2026-09-06T12:41:33.1709239",
  "workedMinutes": 1,
  "requiredMinutes": 480,
  "differenceMinutes": -479,
  "status": "INCOMPLETE"
}
```

### Attendance History

```http
GET /api/attendance/history
```

Header:

```http
Authorization: Bearer <JWT_TOKEN>
```

Returns attendance records belonging to the currently authenticated user, ordered from newest to oldest.

The endpoint does not accept an employee ID from the client. This prevents an employee from changing a URL parameter to retrieve another employee's attendance history.

## Work Schedules

Work schedule management requires the `ADMIN` role.

### Create or Update Work Schedule

```http
POST /api/work-schedules
```

Header:

```http
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

Example request:

```json
{
  "userId": 2,
  "requiredMinutes": 480
}
```

Example response:

```json
{
  "id": 1,
  "userId": 2,
  "employeeName": "Second Employee",
  "employeeEmail": "second.employee@example.com",
  "requiredMinutes": 480
}
```

### Get Employee Work Schedule

```http
GET /api/work-schedules/user/{userId}
```

Header:

```http
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

## Attendance Calculation

Worked time is calculated when an employee checks out.

Conceptually:

```text
workedMinutes = checkOutTime - checkInTime
differenceMinutes = workedMinutes - requiredMinutes
```

If:

```text
workedMinutes >= requiredMinutes
```

the status is:

```text
COMPLETED
```

Otherwise:

```text
INCOMPLETE
```

Example:

```text
Worked:       500 minutes
Required:     480 minutes
Difference:    20 minutes
Status:       COMPLETED
```

Or:

```text
Worked:         1 minute
Required:     480 minutes
Difference:  -479 minutes
Status:       INCOMPLETE
```

The calculated values are stored with the attendance record at checkout time.

This preserves historical accuracy. If an administrator changes an employee's work schedule later, previously completed attendance records retain the requirement and result that applied at the time of checkout.

## Error Handling

The application uses custom exception handling for attendance business rules.

### Duplicate Check-In

```http
409 Conflict
```

Example:

```json
{
  "status": 409,
  "message": "Employee is already checked in"
}
```

### Check-Out Without Active Check-In

```http
409 Conflict
```

Example:

```json
{
  "status": 409,
  "message": "Employee is not currently checked in"
}
```

### Missing Resource

Missing required resources can return:

```http
404 Not Found
```

### Authentication and Authorization

A request to a protected endpoint without a valid JWT returns:

```http
401 Unauthorized
```

A valid authenticated user without the required role receives:

```http
403 Forbidden
```

For example:

```text
EMPLOYEE token → work schedule endpoint → 403 Forbidden
ADMIN token    → work schedule endpoint → allowed
```

## Inactive Accounts

The `users` table contains an `active` field.

When:

```text
active = true
```

the user can authenticate normally.

When:

```text
active = false
```

login is rejected.

The system also checks the current account state when validating a JWT.

Therefore, if:

1. A user logs in while active.
2. A JWT is issued.
3. An administrator later disables the user.

The previously issued token will no longer be accepted for protected requests.

## Database

The project uses Microsoft SQL Server.

Development database:

```text
AttendanceDB
```

Main tables:

```text
users
work_schedules
attendance
```

The attendance table stores information including:

```text
id
user_id
work_date
check_in_time
check_out_time
worked_minutes
required_minutes
difference_minutes
status
```

## Database and JWT Configuration

Sensitive configuration values are provided through environment variables.

Required variables:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Example `application.properties`:

```properties
spring.application.name=attendance

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=AttendanceDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

No real database password or JWT signing secret should be committed to source control.

## Running the Application

### Requirements

Make sure the following are available:

- Java 21
- Microsoft SQL Server
- `AttendanceDB` database
- Maven or the included Maven Wrapper

Set the following environment variables:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Then run the project through IntelliJ IDEA or use the Maven Wrapper.

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Other Environments

```bash
./mvnw spring-boot:run
```

By default, the application runs at:

```text
http://localhost:8080
```

## Testing

The project contains automated tests using:

- JUnit
- Mockito
- Spring MockMvc

The automated test suite covers:

- Successful employee check-in
- Duplicate check-in rejection
- Worked-time calculation
- Required-time comparison
- Attendance status calculation
- JWT generation
- JWT username extraction
- JWT validation
- JWT rejection for a different user
- JWT rejection for an inactive user
- Login token generation
- Unauthenticated endpoint rejection
- Employee role rejection from admin endpoints
- Administrator access to admin endpoints
- Spring application context startup

The current automated suite contains 13 passing tests.

Run the full suite with the required environment variables configured.

### Windows PowerShell

```powershell
$env:DB_USERNAME="YOUR_DATABASE_USERNAME"
$env:DB_PASSWORD="YOUR_DATABASE_PASSWORD"
$env:JWT_SECRET="YOUR_JWT_SECRET"

.\mvnw.cmd test
```

The API has also been manually tested using Postman for:

- Registration
- JWT login
- Protected requests with Bearer tokens
- Requests without tokens
- Employee authorization
- Admin authorization
- Inactive account login protection
- Work schedule creation/update
- Check-in
- Duplicate check-in
- Check-out
- Check-out without an active check-in
- Attendance calculations
- Attendance history

## Security

The application currently implements:

- BCrypt password hashing
- JWT-based authentication
- Signed JWT tokens
- Token expiration
- Stateless Spring Security configuration
- Bearer token authentication filter
- `EMPLOYEE` and `ADMIN` role authorization
- Protected attendance endpoints
- Admin-only work schedule management
- Inactive account protection
- Rejection of JWTs belonging to disabled accounts
- Generic invalid-login behavior
- Environment-based database credentials
- Environment-based JWT signing secret

The security configuration uses:

```text
SessionCreationPolicy.STATELESS
```

so authentication state is not stored in an HTTP session.

## CSRF

CSRF protection is currently disabled.

For this project, authenticated requests use JWT Bearer tokens in the `Authorization` header rather than session cookies.

This configuration is suitable for the current REST API and Postman-based technical-assessment environment.

If the authentication architecture is changed in the future, particularly if tokens are stored and automatically submitted through browser cookies, the CSRF strategy should be reviewed accordingly.

## Development and Production Notes

This project is currently configured for development and technical-assessment use.

Hibernate uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This is convenient during development because Hibernate can update the database schema automatically.

For production, schema changes should instead be managed using a migration tool such as:

- Flyway
- Liquibase

Additional production improvements could include shorter-lived access tokens, refresh tokens, token revocation, structured logging, and dedicated application profiles.

## Possible Future Improvements

Potential future improvements include:

- Flyway or Liquibase database migrations
- OpenAPI / Swagger documentation
- Refresh tokens
- JWT revocation or token blacklisting
- Additional integration tests
- Pagination for large attendance histories
- Admin attendance reporting
- Date-based attendance filtering
- Dedicated enum types for roles and attendance status
- Production environment profiles
- Centralized audit logging

## Purpose

This project was developed as a backend learning and technical-assessment project demonstrating practical use of:

- Java
- Spring Boot
- REST APIs
- Spring Security
- JWT authentication
- Authentication and authorization
- Role-based access control
- JPA/Hibernate
- SQL Server
- DTOs
- Service and repository layers
- Exception handling
- Unit testing
- Security integration testing