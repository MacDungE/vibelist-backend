package org.example.vibelist.domain.post.tag.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.post.tag.dto.TagDTO;
import org.example.vibelist.domain.post.tag.entity.Tag;
import org.example.vibelist.domain.post.tag.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public RsData<List<TagDTO>> autoComplete(String input, int limit) {
        try {
            input = input.trim();
            if (input.isBlank()) return RsData.success(ResponseCode.TAG_AUTOCOMPLETE_SUCCESS, List.of());
            List<Tag> tags;
            if (isHangulSyllable(input.charAt(0)) && input.length() == 1) {
                char base  = (char) ((input.charAt(0) - 0xAC00) / 28 * 28 + 0xAC00);
                char upper = (char) (base + 28);
                tags = tagRepository.findTopNByInitialRange(base, upper, limit);
            } else {
                tags = tagRepository.findTopNByNamePrefix(input.toLowerCase(), limit);
            }
            List<TagDTO> TagDTOs = tags.stream()
                    .map(TagDTO::from)
                    .collect(Collectors.toList());
            return RsData.success(ResponseCode.TAG_AUTOCOMPLETE_SUCCESS, TagDTOs);
        } catch (Exception e) {
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "태그 자동완성 중 오류 발생: " + e.getMessage());
        }
    }

    @Transactional
    public Tag getOrCreate(String rawName) {
        if (rawName == null) throw new GlobalException(ResponseCode.TAG_INVALID, "태그 이름은 null일 수 없습니다.");

        // ① 공백 제거 + 소문자 통일
        String name = rawName.trim().toLowerCase();
        if (name.isEmpty())
            throw new GlobalException(ResponseCode.TAG_INVALID, "태그 이름은 비어있을 수 없습니다.");

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
