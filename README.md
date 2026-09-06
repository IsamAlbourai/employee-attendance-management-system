# Employee Attendance Management System

A backend employee attendance management system built with Java and Spring Boot.

The application provides employee registration and authentication, role-based authorization, work schedule management, attendance tracking, worked-time calculation, and attendance history.

## Features

- Employee registration
- BCrypt password hashing
- Session-based authentication
- Login and logout
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
- Unit testing with JUnit and Mockito

## Technology Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Hibernate
- Microsoft SQL Server
- Maven
- Lombok
- JUnit
- Mockito
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
        └── service/
```

## Roles and Authorization

The system currently supports two roles.

### EMPLOYEE

Employees can:

- Log in and log out
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

Authentication is session-based.

After a successful login, Spring Security stores the authenticated user's security context in the HTTP session.

Passwords are stored using BCrypt hashes rather than plain-text passwords.

Inactive users cannot authenticate.

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

#### Logout

```http
POST /api/auth/logout
```

### Attendance

Attendance endpoints require an authenticated user with the `EMPLOYEE` or `ADMIN` role.

#### Check In

```http
POST /api/attendance/check-in
```

No request body is required.

The authenticated user is determined from the current session rather than from a client-supplied user ID.

An employee cannot create another check-in while an open attendance record already exists.

#### Check Out

```http
POST /api/attendance/check-out
```

No request body is required.

During checkout, the system:

1. Finds the employee's active attendance record.
2. Finds the employee's required work schedule.
3. Records the checkout time.
4. Calculates the total worked minutes.
5. Compares worked minutes against required minutes.
6. Calculates the difference.
7. Determines whether the required time was completed.
8. Stores the result in the attendance record.

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

#### Attendance History

```http
GET /api/attendance/history
```

Returns attendance records belonging to the currently authenticated user, ordered from newest to oldest.

The endpoint does not accept a user ID from the employee. This prevents an employee from simply changing a URL parameter to retrieve another employee's attendance history.

## Work Schedules

Work schedule management requires the `ADMIN` role.

### Create or Update Work Schedule

```http
POST /api/work-schedules
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

For example:

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

This means that if an administrator changes the employee's work schedule later, previously completed attendance records retain the requirement and result that applied when the employee originally checked out.

## Error Handling

The application uses custom exception handling for attendance business rules.

Examples include:

### Duplicate Check-In

```http
409 Conflict
```

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

Unauthenticated access to protected resources returns:

```http
401 Unauthorized
```

Authenticated users without the required role receive:

```http
403 Forbidden
```

For example, an `EMPLOYEE` attempting to modify a work schedule receives `403 Forbidden`.

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

authentication is rejected.

The login API returns a generic authentication error rather than revealing that the account has been disabled.

## Database

The project uses Microsoft SQL Server.

Development database:

```text
AttendanceDB
```

The main tables are:

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

## Database Configuration

Database credentials are not hard-coded into the Java source code.

The application expects:

```text
DB_USERNAME
DB_PASSWORD
```

as environment variables.

Example `application.properties` configuration:

```properties
spring.application.name=attendance

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=AttendanceDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## Running the Application

### Requirements

Make sure the following are available:

- Java 21
- Maven
- Microsoft SQL Server
- `AttendanceDB` database

Set the required database environment variables:

```text
DB_USERNAME
DB_PASSWORD
```

Run the project from IntelliJ IDEA or execute:

```bash
mvn spring-boot:run
```

By default, the application runs at:

```text
http://localhost:8080
```

## Testing

The project includes automated unit tests using:

- JUnit
- Mockito

Run the tests with:

```bash
mvn test
```

The current attendance service test suite verifies:

- Successful employee check-in
- Duplicate check-in rejection
- Worked-time calculation
- Required-time comparison
- `COMPLETED` attendance status

The current `AttendanceServiceTest` contains three passing tests.

The API was also manually tested using Postman for:

- Registration
- Login
- Logout
- Employee authorization
- Admin authorization
- Inactive account authentication
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
- Session-based authentication
- Spring Security authorization
- `EMPLOYEE` and `ADMIN` roles
- Protected attendance endpoints
- Admin-only work schedule management
- Inactive account protection
- Generic invalid-login responses
- Environment-based database credentials

## Development and Production Notes

This project is currently configured for development and technical-assessment use.

Hibernate currently uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This is convenient during development because Hibernate can update the database schema automatically.

For a production system, schema changes should instead be managed using a migration tool such as Flyway or Liquibase.

CSRF protection is currently disabled to simplify REST API testing with Postman.

Because authentication is session-based, CSRF protection should be properly configured before using this backend with a production browser-based frontend.

## Possible Future Improvements

Potential future improvements include:

- Flyway or Liquibase database migrations
- OpenAPI / Swagger documentation
- Additional integration tests
- Pagination for large attendance histories
- Admin attendance reporting
- Date-based attendance filtering
- Dedicated enum types for roles and attendance status
- Production-ready CSRF configuration
- Production environment profiles

## Purpose

This project was developed as a backend learning and technical-assessment project demonstrating practical use of:

- Java
- Spring Boot
- REST APIs
- Spring Security
- Authentication and authorization
- JPA/Hibernate
- SQL Server
- DTOs
- Service and repository layers
- Exception handling
- Unit testing