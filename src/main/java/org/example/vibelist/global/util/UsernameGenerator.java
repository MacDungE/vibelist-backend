package org.example.vibelist.global.util;

import org.springframework.stereotype.Component;

import java.util.Random;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;

@Component
public class UsernameGenerator {

    private static final String[] PREFIXES = {
        "iu", "bts", "blackpink", "twice", "redvelvet", "exo", "nct", "seventeen", 
        "txt", "enhypen", "newjeans", "ive", "le_sserafim", "aespa", "itzy", "straykids",
        "ateez", "theboyz", "treasure", "nct", "superjunior", "shinee", "bigbang",
        "winner", "ikon", "got7", "monstax", "pentagon", "oneus", "onewe", "dreamcatcher"
    };

    private static final String[] SUFFIXES = {
        "fan", "lover", "stan", "supporter", "enthusiast", "admirer", "devotee", "follower"
    };

    private final Random random = new Random();

    /**
     * 랜덤 사용자명 생성 (예: iu_8347)
     */
    public String generateRandomUsername() {
        String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
        int randomNumber = random.nextInt(10000); // 0-9999
        return prefix + "_" + String.format("%04d", randomNumber);
    }

    /**
     * 중복되지 않는 사용자명 생성
     */
    public String generateUniqueUsername(java.util.function.Predicate<String> existsChecker) {
        String username;
        int attempts = 0;
        do {
            username = generateRandomUsername();
            attempts++;
            if (attempts > 100) {
                throw new GlobalException(ResponseCode.USERNAME_GENERATION_FAILED, "사용자명 생성에 100회 이상 실패했습니다. 잠시 후 다시 시도해주세요.");
            }
        } while (existsChecker.test(username));
        
        return username;
    }
} 