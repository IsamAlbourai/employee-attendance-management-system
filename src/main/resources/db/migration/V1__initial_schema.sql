-- Employee Attendance Management System
-- Initial schema derived from AttendanceDB_InitialSchema.sql (SSMS, 2026-10-21).
-- Run only on a NEW/EMPTY application database using Flyway.
-- For the existing AttendanceDB, baseline at version 1 instead of executing V1.
-- The database itself must already exist; this script only creates application objects.

CREATE TABLE [dbo].[users] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [active] BIT NOT NULL,
    [email] VARCHAR(255) NOT NULL,
    [name] VARCHAR(255) NOT NULL,
    [password] VARCHAR(255) NOT NULL,
    [role] VARCHAR(255) NOT NULL,
    [manager_id] BIGINT NULL,
    CONSTRAINT [PK_users] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [UK6dotkott2kjsp8vw4d0m25fb7] UNIQUE ([email])
);

CREATE TABLE [dbo].[attendance] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [check_in_time] DATETIME2(7) NOT NULL,
    [check_out_time] DATETIME2(7) NULL,
    [work_date] DATE NOT NULL,
    [user_id] BIGINT NOT NULL,
    [difference_minutes] BIGINT NULL,
    [required_minutes] INT NULL,
    [status] VARCHAR(255) NULL,
    [worked_minutes] BIGINT NULL,
    CONSTRAINT [PK_attendance] PRIMARY KEY CLUSTERED ([id])
);

CREATE TABLE [dbo].[email_notifications] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [attempt_count] INT NOT NULL,
    [body] VARCHAR(4000) NOT NULL,
    [created_at] DATETIME2(7) NOT NULL,
    [event_key] VARCHAR(200) NOT NULL,
    [last_attempt_at] DATETIME2(7) NULL,
    [last_error] VARCHAR(1000) NULL,
    [next_attempt_at] DATETIME2(7) NULL,
    [recipient_email] VARCHAR(255) NOT NULL,
    [sent_at] DATETIME2(7) NULL,
    [status] VARCHAR(20) NOT NULL,
    [subject] VARCHAR(255) NOT NULL,
    [version] BIGINT NULL,
    CONSTRAINT [PK_email_notifications] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_email_notification_event] UNIQUE ([event_key]),
    CONSTRAINT [CK_email_notifications_status] CHECK (
        [status] IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')
    )
);

CREATE TABLE [dbo].[leave_balances] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [annual_allowance] INT NOT NULL,
    [carried_over_days] INT NOT NULL,
    [leave_year] INT NOT NULL,
    [version] BIGINT NULL,
    [employee_id] BIGINT NOT NULL,
    CONSTRAINT [PK_leave_balances] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [uk_leave_balance_employee_year] UNIQUE ([employee_id], [leave_year])
);

CREATE TABLE [dbo].[leave_requests] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [created_at] DATETIME2(7) NOT NULL,
    [end_date] DATE NOT NULL,
    [manager_comment] VARCHAR(1000) NULL,
    [reason] VARCHAR(1000) NULL,
    [reviewed_at] DATETIME2(7) NULL,
    [start_date] DATE NOT NULL,
    [status] VARCHAR(20) NOT NULL,
    [version] BIGINT NULL,
    [employee_id] BIGINT NOT NULL,
    [manager_id] BIGINT NOT NULL,
    CONSTRAINT [PK_leave_requests] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [CK_leave_requests_status] CHECK (
        [status] IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    )
);

CREATE TABLE [dbo].[work_schedules] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [required_minutes] INT NOT NULL,
    [user_id] BIGINT NOT NULL,
    CONSTRAINT [PK_work_schedules] PRIMARY KEY CLUSTERED ([id]),
    CONSTRAINT [UKhko8ke57fl66l1511qwqa05wu] UNIQUE ([user_id])
);

CREATE NONCLUSTERED INDEX [idx_email_notification_created]
    ON [dbo].[email_notifications] ([created_at]);
CREATE NONCLUSTERED INDEX [idx_email_notification_status]
    ON [dbo].[email_notifications] ([status], [next_attempt_at]);
CREATE NONCLUSTERED INDEX [idx_leave_balance_year]
    ON [dbo].[leave_balances] ([leave_year]);
CREATE NONCLUSTERED INDEX [idx_leave_employee_status]
    ON [dbo].[leave_requests] ([employee_id], [status]);
CREATE NONCLUSTERED INDEX [idx_leave_manager_status]
    ON [dbo].[leave_requests] ([manager_id], [status]);

ALTER TABLE [dbo].[attendance]
    ADD CONSTRAINT [FKjcaqd29v2qy723owsdah2t8vx]
    FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]);
ALTER TABLE [dbo].[leave_balances]
    ADD CONSTRAINT [FK3ygm1a5uoway86agyic525p4n]
    FOREIGN KEY ([employee_id]) REFERENCES [dbo].[users] ([id]);
ALTER TABLE [dbo].[leave_requests]
    ADD CONSTRAINT [FKrvhupxdba11fck0okstm3l6i0]
    FOREIGN KEY ([manager_id]) REFERENCES [dbo].[users] ([id]);
ALTER TABLE [dbo].[leave_requests]
    ADD CONSTRAINT [FKsqls5en3a5v8l9md81umuu87c]
    FOREIGN KEY ([employee_id]) REFERENCES [dbo].[users] ([id]);
ALTER TABLE [dbo].[users]
    ADD CONSTRAINT [FK5p1ci5btqfwvtaqx5n2wxi182]
    FOREIGN KEY ([manager_id]) REFERENCES [dbo].[users] ([id]);
ALTER TABLE [dbo].[work_schedules]
    ADD CONSTRAINT [FKj81w5rs9r89mvwhvwm6vuqiln]
    FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]);
