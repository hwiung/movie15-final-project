-- [inquiry] 테이블: 사용자 ID, 상태, 생성일 기준 검색 성능 향상
CREATE INDEX idx_inquiry_user_id ON inquiry(user_id);
CREATE INDEX idx_inquiry_status ON inquiry(status);
CREATE INDEX idx_inquiry_created_at ON inquiry(created_at);

-- [inquiry_file] 테이블: 특정 문의에 연결된 파일 빠르게 조회
CREATE INDEX idx_inquiry_file_inquiry_id ON inquiry_file(inquiry_id);

-- [file] 테이블: URL 및 파일 타입 검색 성능 향상
CREATE INDEX idx_file_url ON file(url);
CREATE INDEX idx_file_type ON file(type);