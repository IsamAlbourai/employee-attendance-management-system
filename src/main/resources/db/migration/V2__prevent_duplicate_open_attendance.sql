
-- V2: Prevent multiple open attendance sessions
-- for the same employee.
--
-- A session is open when check_out_time IS NULL.
-- Historical checked-out records are unaffected.

CREATE UNIQUE NONCLUSTERED INDEX
    ux_attendance_one_open_session
ON dbo.attendance (user_id)
WHERE check_out_time IS NULL;
