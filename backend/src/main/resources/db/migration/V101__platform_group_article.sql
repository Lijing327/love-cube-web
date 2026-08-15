CREATE TABLE platform_group_article (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  summary VARCHAR(500),
  content TEXT NOT NULL,
  cover_url VARCHAR(512),
  author_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'published',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_group_status_created (group_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
