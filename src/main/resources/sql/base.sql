-- VibeList Backend 데이터베이스 스키마

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS "user" (
    "id" BIGSERIAL PRIMARY KEY,
    "username" VARCHAR(30) NOT NULL UNIQUE,
    "password" VARCHAR(60) NOT NULL,
    "role" VARCHAR(20) NOT NULL DEFAULT 'USER',
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 사용자 프로필 테이블
CREATE TABLE IF NOT EXISTS "user_profile" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "email" VARCHAR(100),
    "name" VARCHAR(100),
    "phone" VARCHAR(20),
    "avatar_url" TEXT,
    "bio" TEXT,
    "locale" VARCHAR(10),
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ("user_id") REFERENCES "user"("id") ON DELETE CASCADE
);

-- 소셜 계정 연동 테이블
CREATE TABLE IF NOT EXISTS "user_social" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "provider" VARCHAR(20),
    "provider_user_id" VARCHAR(255),
    "provider_email" VARCHAR(255),
    "refresh_token_enc" TEXT,
    "access_token_enc" TEXT,
    "token_type" VARCHAR(20),
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ("user_id") REFERENCES "user"("id") ON DELETE CASCADE,
    UNIQUE ("provider", "provider_user_id")
);

-- OAuth2 토큰 상세 정보 테이블
CREATE TABLE IF NOT EXISTS "oauth2_token_info" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "provider" VARCHAR(20) NOT NULL,
    "access_token_enc" TEXT,
    "refresh_token_enc" TEXT,
    "token_type" VARCHAR(20),
    "expires_in" INTEGER,
    "scope" TEXT,
    "token_issued_at" TIMESTAMP,
    "token_expires_at" TIMESTAMP,
    "is_active" BOOLEAN DEFAULT true,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ("user_id") REFERENCES "user"("id") ON DELETE CASCADE,
    UNIQUE ("user_id", "provider")
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS "idx_user_username" ON "user"("username");
CREATE INDEX IF NOT EXISTS "idx_user_profile_email" ON "user_profile"("email");
CREATE INDEX IF NOT EXISTS "idx_user_social_provider" ON "user_social"("provider");
CREATE INDEX IF NOT EXISTS "idx_user_social_provider_user_id" ON "user_social"("provider", "provider_user_id");
CREATE INDEX IF NOT EXISTS "idx_oauth2_token_info_user_provider" ON "oauth2_token_info"("user_id", "provider");
CREATE INDEX IF NOT EXISTS "idx_oauth2_token_info_provider" ON "oauth2_token_info"("provider");
CREATE INDEX IF NOT EXISTS "idx_oauth2_token_info_active" ON "oauth2_token_info"("is_active");
CREATE INDEX IF NOT EXISTS "idx_oauth2_token_info_expires" ON "oauth2_token_info"("token_expires_at");