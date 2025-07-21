package org.example.vibelist.domain.post.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.global.exception.CustomException;
import org.example.vibelist.global.exception.ErrorCode;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.util.List;

@Slf4j
@Converter(autoApply = false)
public class TrackListConverter
        implements AttributeConverter<List<TrackRsDto>, PGobject> {

    private static final ObjectMapper om = new ObjectMapper();

    // Java -> DB
    @Override
    public PGobject convertToDatabaseColumn(List<TrackRsDto> attribute) {
        if (attribute == null) return null;

        try {
            PGobject jsonb = new PGobject();
            jsonb.setType("jsonb");                    // ← 핵심! 드라이버가 jsonb로 인식
            jsonb.setValue(om.writeValueAsString(attribute));
            return jsonb;
        } catch (Exception e) {
            log.info("[SYS_001] 트랙 리스트 직렬화 실패 - attribute: {}, error: {}", attribute, e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // DB -> Java
    @Override
    public List<TrackRsDto> convertToEntityAttribute(PGobject dbData) {
        if (dbData == null) return null;

        try {
            return om.readValue(dbData.getValue(), new TypeReference<>() {});
        } catch (IOException e) {
            log.info("[SYS_001] 트랙 리스트 역직렬화 실패 - dbData: {}, error: {}", dbData, e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}