package org.example.vibelist.domain.post.tag.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.post.tag.entity.Tag;
import org.example.vibelist.domain.post.tag.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public List<Tag> autoComplete(String input, int limit) {
        input = input.trim();
        if (input.isBlank()) return List.of();

        // ① ‘아’ 한 글자만 → 28글자 범위 검색
        if (isHangulSyllable(input.charAt(0)) && input.length() == 1) {
            char base  = (char) ((input.charAt(0) - 0xAC00) / 28 * 28 + 0xAC00);
            char upper = (char) (base + 28);
            return tagRepository.findTopNByInitialRange(base, upper, limit);
        }

        // ② 그 외엔 LIKE prefix
        return tagRepository.findTopNByNamePrefix(input.toLowerCase(), limit);
    }

    @Transactional
    public Tag getOrCreate(String rawName) {
        if (rawName == null) throw new IllegalArgumentException("tag name is null");

        // ① 공백 제거 + 소문자 통일
        String name = rawName.trim().toLowerCase();
        if (name.isEmpty())
            throw new IllegalArgumentException("tag name is empty");

        // ② 먼저 존재 여부 확인
        return tagRepository.findByName(name)
                .orElseGet(() -> {
                    // ③ 없으면 새로 생성
                    Tag tag = new Tag();
                    tag.setName(name);
                    // (선택) 초성 필드가 있으면 여기서 세팅
                    // tag.setInitials(HangulUtils.toInitials(name));
                    return tagRepository.save(tag);
                });
    }

    private boolean isHangulSyllable(char c) {
        return c >= 0xAC00 && c <= 0xD7A3;
    }

}
