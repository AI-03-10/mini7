package com.app.api.tokenTest;

import com.app.domain.member.constant.Role;
import com.app.global.jwt.dto.JwtTokenDto;
import com.app.global.jwt.service.TokenManager;
import io.jsonwebtoken.Claims;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/token-test")
@RequiredArgsConstructor
public class TokenTestController {

    private final TokenManager tokenManager;

    // 토큰이 잘 생성되는지 확인
    @GetMapping("/create")
    public JwtTokenDto createJwtTokenDto() {
        return tokenManager.createJwtTokenDto(1L, Role.ADMIN);
    }

    // 입력받은 토큰이 유효한지 확인
    @GetMapping("/valid")
    public String validateJwtToken(@RequestParam String token) {
        tokenManager.validateToken(token);
        Claims tokenClaims = tokenManager.getTokenClaims(token);
        String subject = tokenClaims.getSubject();
        Date issuedAt = tokenClaims.getIssuedAt();
        Date expiration = tokenClaims.getExpiration();
        Long memberId = Long.valueOf((Integer) tokenClaims.get("memberId"));
        // 이렇게 해도 변환은 가능한데, Object를 Long으로 변환하는 과정을 좀더 안전하게 하고 싶으셔서 이런것 같다.
//        Long memberId = (Long) tokenClaims.get("memberId");
        String role = (String) tokenClaims.get("role");
        log.info("memberId : {}", memberId);
        log.info("role : {}", role);
        log.info("subject : {}, issuedAt : {}, expiration: {}", subject, issuedAt, expiration);
        return "success";
    }
}