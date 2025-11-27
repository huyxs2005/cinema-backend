USE CinemaBooking_Backlog;
GO

DELETE FROM Users WHERE Email = ''nhanvien.demo@cinema.vn'';
GO

IF NOT EXISTS (SELECT 1 FROM Roles WHERE Name = ''Staff'')
    INSERT INTO Roles (Name) VALUES (''Staff'');

DECLARE @StaffRoleId INT = (SELECT RoleId FROM Roles WHERE Name = ''Staff'');

INSERT INTO Users (Email, PasswordHash, FullName, Phone, RoleId, IsActive)
VALUES (
    ''nhanvien.demo@gmail.com'',
    ''$2a$10$A3AGn1nP6h4iS6yCyHkUwu5gWeK1qG1s0ZlZ1aU5vT1cs5hZVXAB6'', -- BCrypt for Staff@123
    N''Nguyễn Văn Nhân Viên'',
    ''0900000001'',
    @StaffRoleId,
    1
);
GO
