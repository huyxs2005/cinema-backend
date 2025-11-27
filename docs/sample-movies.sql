USE CinemaBooking_Backlog;
GO

INSERT INTO Movies (Title, OriginalTitle, Description, DurationMinutes, AgeRating, PosterUrl, TrailerUrl, ImdbUrl, Status, ReleaseDate)
VALUES
(N'Chú Thuật Hồi Chiến 0 – Tái Khởi Chiếu', N'Jujutsu Kaisen 0', N'Yuta Okkotsu joins Tokyo Jujutsu High to control a powerful curse.', 105, N'T13',
 N'/uploads/movies/jjk.jpeg', N'https://youtu.be/jujutsu0', N'https://www.imdb.com/title/tt14331144/', N'NowShowing', '2025-11-14'),

(N'Mộ Đom Đóm - K (Phụ đề)', N'Grave of the Fireflies', N'Two siblings struggle to survive in wartime Japan.', 88, N'T13',
 N'/img/movies/fireflies.jpg', N'https://youtu.be/fireflies', N'https://www.imdb.com/title/tt0095327/', N'ComingSoon', '2025-11-07'),

(N'Wicked: Phần 2 - K', NULL, N'The untold story of the witches of Oz continues.', 160, N'T16',
 N'/img/movies/wicked2.jpg', N'https://youtu.be/wicked2', N'https://www.imdb.com/title/tt1262426/', N'ComingSoon', '2025-11-21'),

(N'Truy Tìm Long Diên Hương', NULL, N'Một cuộc phiêu lưu hài hước trong nông trại', 115, N'T16',
 N'/img/movies/longdienhuong.jpg', N'https://youtu.be/longdien', N'https://www.imdb.com/title/tt0000001/', N'NowShowing', '2025-11-14'),

(N'Cục Vàng Của Ngoại', NULL, N'Gia đình vui nhộn chiến đấu để giữ lại tiệm tạp hóa thân yêu.', 110, N'T13',
 N'/img/movies/cucvang.jpg', N'https://youtu.be/cucvang', N'https://www.imdb.com/title/tt0000002/', N'NowShowing', '2025-10-17');

INSERT INTO Movies (Title, OriginalTitle, Description, DurationMinutes, AgeRating, PosterUrl, TrailerUrl, ImdbUrl, Status, ReleaseDate)
VALUES
(N'Quái Thú Vô Hình: Vùng Đất Chết', NULL, N'Phi vụ săn quái thú nơi vùng đất bị nguyền rủa.', 118, N'T16',
 N'/img/movies/quai-thu.jpg', NULL, NULL, N'ComingSoon', '2025-11-07'),
(N'Tình Người Duyên Ma: Nhắm Mắt Yêu Luôn', NULL, N'Chuyện tình giữa cô gái hiện đại và hồn ma chân thành.', 112, N'T13',
 N'/img/movies/tinhnguoi.jpg', NULL, NULL, N'NowShowing', '2025-11-07'),
(N'Mộ Đom Đóm - K', N'Grave of the Fireflies', N'Tragic story of two siblings in wartime.', 88, N'T13',
 N'/img/movies/modomdom.jpg', NULL, NULL, N'ComingSoon', '2025-11-07'),
(N'Trốn Chạy Tử Thần', NULL, N'Đội đặc nhiệm chặn đứng âm mưu khủng bố toàn cầu.', 128, N'T18',
 N'/img/movies/tronchay.jpg', NULL, NULL, N'NowShowing', '2025-11-14'),
(N'Wicked: Phần 2', NULL, N'Phần tiếp theo của câu chuyện phù thủy Oz huyền thoại.', 160, N'T13',
 N'/img/movies/wicked2.jpg', NULL, NULL, N'ComingSoon', '2025-11-21'),
(N'Ký Ức Rực Cháy', NULL, N'Nhạc sĩ trẻ tìm lại cảm hứng qua chuyến du lịch bụi.', 102, N'T13',
 N'/img/movies/kyuc.jpg', NULL, NULL, N'NowShowing', '2025-10-05'),
(N'Sóng Dữ Biển Xanh', NULL, N'Hải quân chiến đấu cứu thị trấn trước cơn sóng thần.', 130, N'T16',
 N'/img/movies/songdu.jpg', NULL, NULL, N'NowShowing', '2025-09-30'),
(N'Điều Ước Cuối Cùng', NULL, N'Cô bé mắc bệnh hiểm nghèo thực hiện danh sách ước mơ.', 108, N'P',
 N'/img/movies/dieu-uoc.jpg', NULL, NULL, N'ComingSoon', '2025-12-01'),
(N'Sứ Mệnh Bất Khả, Kế Hoạch Sấm Rền', NULL, N'Điệp viên Ethan đối đầu tổ chức bí ẩn.', 150, N'T16',
 N'/img/movies/sumenh.jpg', NULL, NULL, N'ComingSoon', '2025-12-15'),
(N'Bí Ẩn Nhà Kho', NULL, N'Nhà kho bỏ hoang chứa bí mật giết người hàng loạt.', 96, N'T18',
 N'/img/movies/bian.jpg', NULL, NULL, N'NowShowing', '2025-09-25'),
(N'Hẹn Yêu Mùa Thu', NULL, N'Mối tình lãng mạn giữa hai tâm hồn cô đơn ở Đà Lạt.', 109, N'T13',
 N'/img/movies/henyeu.jpg', NULL, NULL, N'NowShowing', '2025-09-10'),
(N'Vệ Binh Huyền Thoại', NULL, N'Các chiến binh bảo vệ cổ vật cứu thế giới.', 125, N'T16',
 N'/img/movies/vebinh.jpg', NULL, NULL, N'ComingSoon', '2025-12-20'),
(N'Đường Đua Bão Táp', NULL, N'Câu chuyện tay đua trẻ chinh phục giải thế giới.', 119, N'T13',
 N'/img/movies/duongdua.jpg', NULL, NULL, N'NowShowing', '2025-09-18'),
(N'Bản Nhạc Hà Nội', NULL, N'Hai nghệ sĩ trẻ đấu tranh giữa nghệ thuật và thực tế.', 111, N'T13',
 N'/img/movies/bannhac.jpg', NULL, NULL, N'NowShowing', '2025-08-30'),
(N'Trò Chơi Định Mệnh', NULL, N'Game thủ phát hiện trò chơi thực chất là thử nghiệm tâm lý.', 107, N'T16',
 N'/img/movies/trochoi.jpg', NULL, NULL, N'ComingSoon', '2025-11-30'),
(N'Điệp Vụ Không Gian', NULL, N'Phi hành gia giải cứu trạm vũ trụ khỏi sự cố bí ẩn.', 132, N'T13',
 N'/img/movies/diepvu.jpg', NULL, NULL, N'ComingSoon', '2025-12-05'),
(N'Lạc Lối Ở Tokyo', NULL, N'Một nhà văn Việt tìm lại bản ngã tại Nhật.', 114, N'T13',
 N'/img/movies/lacloi.jpg', NULL, NULL, N'NowShowing', '2025-10-01'),
(N'Trại Hè Bí Ẩn', NULL, N'Nhóm học sinh khám phá vụ mất tích liên hoàn.', 106, N'T13',
 N'/img/movies/traihe.jpg', NULL, NULL, N'ComingSoon', '2025-12-08'),
(N'Dòng Máu Rồng', NULL, N'Gia tộc võ học đối đầu thế lực ngầm.', 140, N'T18',
 N'/img/movies/dongmau.jpg', NULL, NULL, N'NowShowing', '2025-09-05'),
(N'Người Giữ Đền', NULL, N'Bảo vệ kỳ quan cổ vũ khí tối thượng.', 121, N'T16',
 N'/img/movies/nguoidenden.jpg', NULL, NULL, N'ComingSoon', '2025-12-12'),
(N'Hộp Âm Thanh', NULL, N'Chiếc hộp giam giữ ký ức và lời nguyền gia đình.', 103, N'T16',
 N'/img/movies/hopamthanh.jpg', NULL, NULL, N'NowShowing', '2025-10-22');

GO

/* Seed genres */
MERGE INTO Genres AS target
USING (VALUES
    (N'Hoạt hình'),
    (N'Thần thoại'),
    (N'Hành động'),
    (N'Phiêu lưu'),
    (N'Nhạc kịch'),
    (N'Tâm lý'),
    (N'Tình cảm'),
    (N'Kinh dị'),
    (N'Hài')
) AS source(Name)
ON target.Name = source.Name
WHEN NOT MATCHED THEN INSERT(Name) VALUES (source.Name);

/* Map movies to genres */
MERGE INTO MovieGenres AS target
USING (VALUES
    (1, (SELECT GenreId FROM Genres WHERE Name = N'Hoạt hình')),
    (1, (SELECT GenreId FROM Genres WHERE Name = N'Thần thoại')),
    (2, (SELECT GenreId FROM Genres WHERE Name = N'Thần thoại')),
    (3, (SELECT GenreId FROM Genres WHERE Name = N'Phiêu lưu')),
    (3, (SELECT GenreId FROM Genres WHERE Name = N'Nhạc kịch')),
    (4, (SELECT GenreId FROM Genres WHERE Name = N'Hài')),
    (4, (SELECT GenreId FROM Genres WHERE Name = N'Phiêu lưu')),
    (5, (SELECT GenreId FROM Genres WHERE Name = N'Tâm lý')),
    (5, (SELECT GenreId FROM Genres WHERE Name = N'Tình cảm')),
    (6, (SELECT GenreId FROM Genres WHERE Name = N'Hành động')),
    (6, (SELECT GenreId FROM Genres WHERE Name = N'Kinh dị')),
    (7, (SELECT GenreId FROM Genres WHERE Name = N'Tình cảm')),
    (8, (SELECT GenreId FROM Genres WHERE Name = N'Thần thoại')),
    (9, (SELECT GenreId FROM Genres WHERE Name = N'Phiêu lưu')),
    (10, (SELECT GenreId FROM Genres WHERE Name = N'Hoạt hình')),
    (11, (SELECT GenreId FROM Genres WHERE Name = N'Tình cảm')),
    (12, (SELECT GenreId FROM Genres WHERE Name = N'Hành động')),
    (13, (SELECT GenreId FROM Genres WHERE Name = N'Hài')),
    (14, (SELECT GenreId FROM Genres WHERE Name = N'Tâm lý')),
    (15, (SELECT GenreId FROM Genres WHERE Name = N'Kinh dị')),
    (16, (SELECT GenreId FROM Genres WHERE Name = N'Hành động')),
    (17, (SELECT GenreId FROM Genres WHERE Name = N'Tâm lý')),
    (18, (SELECT GenreId FROM Genres WHERE Name = N'Phiêu lưu')),
    (19, (SELECT GenreId FROM Genres WHERE Name = N'Hành động')),
    (20, (SELECT GenreId FROM Genres WHERE Name = N'Kinh dị'))
) AS source(MovieId, GenreId)
ON target.MovieId = source.MovieId AND target.GenreId = source.GenreId
WHEN NOT MATCHED THEN INSERT(MovieId, GenreId) VALUES (source.MovieId, source.GenreId);

GO

/* Seed banners without title/subtitle */
INSERT INTO HomeBanners (ImagePath, LinkType, MovieId, TargetUrl, SortOrder, IsActive, StartDate, EndDate)
SELECT *
FROM (VALUES
    ('/uploads/banners/dune.jpg', N'MOVIE', (SELECT MovieId FROM Movies WHERE Title = N'Chú Thuật Hồi Chiến 0 – Tái Khởi Chiếu'), NULL, 1, 1, '2024-06-01', '2024-08-15'),
    ('/uploads/banners/spiderman.jpg', N'MOVIE', (SELECT MovieId FROM Movies WHERE Title = N'Mộ Đom Đóm - K (Phụ đề)'), NULL, 2, 1, '2024-06-20', '2024-09-01'),
    ('/uploads/banners/insideout2.jpg', N'MOVIE', (SELECT MovieId FROM Movies WHERE Title = N'Wicked: Phần 2 - K'), NULL, 3, 1, '2024-06-10', NULL),
    ('/uploads/banners/summer-sale.jpg', N'URL', NULL, '/promo/summer-sale', 4, 1, '2024-06-01', '2024-07-31')
) AS payload(ImagePath, LinkType, MovieId, TargetUrl, SortOrder, IsActive, StartDate, EndDate)
WHERE NOT EXISTS (
    SELECT 1 FROM HomeBanners hb
    WHERE hb.ImagePath = payload.ImagePath
      AND ISNULL(hb.TargetUrl, '') = ISNULL(payload.TargetUrl, '')
);

GO

/* Seed talent (actors/directors) */
MERGE INTO People AS target
USING (VALUES
    (N'Sunghoo Park', N'Đạo diễn Nhật Bản đứng sau Jujutsu Kaisen 0.'),
    (N'Megumi Ogata', N'Diễn viên lồng tiếng cho Yuta Okkotsu.'),
    (N'Kana Hanazawa', N'Thủ vai Rika Orimoto trong bản anime.'),
    (N'Khương Ngọc', N'Đạo diễn phim Việt Nam Cục Vàng Của Ngoại.'),
    (N'Việt Hương', N'Diễn viên chính trong Cục Vàng Của Ngoại.'),
    (N'Hồng Đào', N'Diễn viên gạo cội của làng phim Việt.'),
    (N'Băng Di', N'Diễn viên trẻ tham gia dự án gia đình.'),
    (N'Jon M. Chu', N'Đạo diễn Wicked: Part Two.'),
    (N'Cynthia Erivo', N'Đảm nhận vai Elphaba.'),
    (N'Ariana Grande', N'Thủ vai Glinda trong Wicked phần 2.')
) AS source(FullName, Bio)
ON target.FullName = source.FullName
WHEN NOT MATCHED THEN INSERT(FullName, Bio) VALUES (source.FullName, source.Bio);

GO

/* Map movie credits */
WITH CreditData AS (
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Chú Thuật Hồi Chiến 0 – Tái Khởi Chiếu') AS MovieId,
        (SELECT PersonId FROM People WHERE FullName = N'Sunghoo Park') AS PersonId,
        N'Director' AS CreditType,
        NULL AS CharacterName,
        1 AS SortOrder
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Chú Thuật Hồi Chiến 0 – Tái Khởi Chiếu'),
        (SELECT PersonId FROM People WHERE FullName = N'Megumi Ogata'),
        N'Actor',
        N'Yuta Okkotsu',
        1
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Chú Thuật Hồi Chiến 0 – Tái Khởi Chiếu'),
        (SELECT PersonId FROM People WHERE FullName = N'Kana Hanazawa'),
        N'Actor',
        N'Rika Orimoto',
        2
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Cục Vàng Của Ngoại'),
        (SELECT PersonId FROM People WHERE FullName = N'Khương Ngọc'),
        N'Director',
        NULL,
        1
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Cục Vàng Của Ngoại'),
        (SELECT PersonId FROM People WHERE FullName = N'Việt Hương'),
        N'Actor',
        NULL,
        1
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Cục Vàng Của Ngoại'),
        (SELECT PersonId FROM People WHERE FullName = N'Hồng Đào'),
        N'Actor',
        NULL,
        2
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Cục Vàng Của Ngoại'),
        (SELECT PersonId FROM People WHERE FullName = N'Băng Di'),
        N'Actor',
        NULL,
        3
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Wicked: Phần 2 - K'),
        (SELECT PersonId FROM People WHERE FullName = N'Jon M. Chu'),
        N'Director',
        NULL,
        1
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Wicked: Phần 2 - K'),
        (SELECT PersonId FROM People WHERE FullName = N'Cynthia Erivo'),
        N'Actor',
        N'Elphaba',
        1
    UNION ALL
    SELECT
        (SELECT MovieId FROM Movies WHERE Title = N'Wicked: Phần 2 - K'),
        (SELECT PersonId FROM People WHERE FullName = N'Ariana Grande'),
        N'Actor',
        N'Glinda',
        2
)
MERGE INTO MovieCredits AS target
USING CreditData AS source
ON target.MovieId = source.MovieId
   AND target.PersonId = source.PersonId
   AND target.CreditType = source.CreditType
WHEN NOT MATCHED AND source.MovieId IS NOT NULL AND source.PersonId IS NOT NULL THEN
    INSERT (MovieId, PersonId, CreditType, CharacterName, SortOrder)
    VALUES (source.MovieId, source.PersonId, source.CreditType, source.CharacterName, source.SortOrder);

GO
