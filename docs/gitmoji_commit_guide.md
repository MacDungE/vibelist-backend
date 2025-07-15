# ✨ Gitmoji 커밋 메시지 가이드

팀에서는 커밋 메시지를 명확하게 분류하고 시각적으로 쉽게 이해할 수 있도록 **Gitmoji**를 사용합니다.

---

## ✅ 기본 커밋 메시지 형식

```
<이모지> <타입>: <간단한 설명> [선택: (#이슈번호)]
```

### 예시:

```
✨ feat: 로그인 API 구현 (#12)
🐛 fix: 토큰 만료 오류 수정
📝 docs: README 파일 작성
```

---

## 🎯 Gitmoji 종류와 사용 시점

| 이모지 | 타입          | 사용 시점 / 예시                            |
|-----|-------------|---------------------------------------|
| +   | `add:`      | **파일추가 커밋**                           |
| ✨   | `feat:`     | **새로운 기능 추가**ex) 검색 필터 기능 추가          |
| 🐛  | `fix:`      | **버그 수정**ex) 로그인 오류 해결, NullPointer 예외 수정 |
| ♻️  | `refactor:` | **리팩터링 (기능은 그대로)**ex) 코드 구조 개선, 중복 제거 |
| 📝  | `docs:`     | **문서 관련 작업**ex) README 작성, API 명세 수정  |
| 🎨  | `style:`    | **코드 포맷 수정**ex) 들여쓰기, 세미콜론, 공백 등      |
| ✅   | `test:`     | **테스트 코드 추가/수정**ex) 회원가입 유닛 테스트 작성    |
| 📦  | `build:`    | **빌드 시스템 변경**ex) 패키지 설치, Webpack 설정 변경 |
| 🚀  | `deploy:`   | **배포 관련 설정**ex) AWS 설정, Dockerfile 수정 |
| 🔥  | `remove:`   | **불필요한 코드/파일 제거**ex) 사용하지 않는 이미지 삭제   |
| 🚧  | `wip:`      | **작업 중인 커밋 (임시 저장)**ex) 아직 미완성 기능 업로드 시 |

---

## 🧪 커밋 메시지 예시 모음

| 목적        | 메시지 예시                     |
| --------- | -------------------------- |
| 로그인 기능 구현 | ✨ feat: 유저 로그인 기능 추가 (#12) |
| 로그인 오류 수정 | 🐛 fix: JWT 토큰 갱신 실패 오류 해결 |
| 코드 정리     | 🎨 style: 코드 들여쓰기 및 공백 수정  |
| 문서 작성     | 📝 docs: API 명세에 로그인 설명 추가 |
| 테스트 작성    | ✅ test: 로그인 실패 테스트 코드 추가   |

---

## 🔗 GitHub 이슈와 연동 (선택)

커밋 메시지나 PR에 `(#이슈번호)` 또는 `Closes #번호`를 작성하면 → GitHub에서 해당 이슈와 자동으로 연결됩니다.

### 예시:

```
✨ feat: 다크모드 UI 전환 기능 추가 (#34)
```

또는 PR 설명에:

```
Closes #34
```

---

## 🛠 Gitmoji CLI 사용법 (선택)

> 커밋 시 자동으로 이모지를 붙이고 싶다면 다음을 사용하세요:

### 📦 설치

```bash
npm install -g gitmoji-cli
```

### 🧭 사용 방법

#### 1. 변경사항 스테이징

```bash
git add .
```

#### 2. 커밋 실행

```bash
gitmoji -c
```

#### 3. 터미널에서 다음 순서로 입력

- 이모지 선택 (`✨`, `🐛` 등)
- 커밋 제목 입력 (예: "로그인 기능 추가")
- 선택적으로 상세 설명 입력 (엔터로 생략 가능)

#### ✅ 예시 흐름

```bash
$ git add .
$ gitmoji -c
? Choose a gitmoji: ✨  - feat: 새로운 기능 추가
? Enter the commit title: 로그인 기능 추가
? Enter the commit message: JWT 발급 포함
✔ Commit created!
```

### 🔁 Hook 방식으로 사용하려면

```bash
gitmoji -i   # Git commit hook 설치
```

이후 `git commit` 입력 시 자동으로 gitmoji CLI가 실행됩니다.

#### ❌ `gitmoji -c`와 hook은 함께 쓰면 안 됩니다!

- hook을 제거하려면:

```bash
gitmoji -r
```

---

## 📘 참고 링크

- Gitmoji 공식 사이트: [https://gitmoji.dev](https://gitmoji.dev)
- Gitmoji CLI 문서: [https://github.com/carloscuesta/gitmoji-cli](https://github.com/carloscuesta/gitmoji-cli)

