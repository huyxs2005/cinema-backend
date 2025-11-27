USE CinemaBooking_Backlog;
GO

IF COL_LENGTH('dbo.Bookings', 'CustomerEmail') IS NULL
BEGIN
    ALTER TABLE dbo.Bookings
        ADD CustomerEmail NVARCHAR(255) NULL;
END
GO

IF COL_LENGTH('dbo.Bookings', 'CustomerPhone') IS NULL
BEGIN
    ALTER TABLE dbo.Bookings
        ADD CustomerPhone NVARCHAR(20) NULL;
END
GO
