INSERT INTO tags (name, type, status) VALUES
('学习', 0, 1),
('社团', 0, 1),
('校园活动', 0, 1),
('二手交易', 0, 1),
('竞赛', 0, 1),
('求职实习', 0, 1),
('生活', 0, 1),
('兴趣', 0, 1),
('科研', 0, 1),
('运动', 0, 1),
('心理健康', 0, 1),
('失物招领', 0, 1)
ON DUPLICATE KEY UPDATE
  type = VALUES(type),
  status = VALUES(status);
