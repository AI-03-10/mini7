package com.app.global.jwt.service;

import com.app.domain.member.constant.Role;
import com.app.global.error.ErrorCode;
import com.app.global.error.exception.AuthenticationException;
import com.app.global.jwt.constant.GrantType;
import com.app.global.jwt.constant.TokenType;
import com.app.global.jwt.dto.JwtTokenDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TokenManager {

    // application.yml에서 가져올 것임
    private final String accessTokenExpirationTime;
    private final String refreshTokenExpirationTime;
    private final String tokenSecret;

    // 액세스 토큰에 멤버 아이디와 역할을 담아서 반환할 것이다.
    public JwtTokenDto createJwtTokenDto(Long memberId, Role role) {
        Date accessTokenExpireTime = createAccessTokenExpireTime();
        Date refreshTokenExpireTime = createRefreshTokenExpireTime();

        String accessToken = createAccessToken(memberId, role, accessTokenExpireTime);
        String refreshToken = createRefreshToken(memberId, refreshTokenExpireTime);

        return JwtTokenDto.builder()
            .grantType(GrantType.BEARER.getType())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .accessTokenExpireTime(accessTokenExpireTime)
            .refreshTokenExpireTime(refreshTokenExpireTime)
            .build();
    }

    // 액세스 토큰의 만료 시간을 반환해 주는 메서드
    public Date createAccessTokenExpireTime() {
        // 현재 시간으로 부터 15분 뒤의 시간을 반환
        return new Date(System.currentTimeMillis() + Long.parseLong(accessTokenExpirationTime));
    }

    public Date createRefreshTokenExpireTime() {
        return new Date(System.currentTimeMillis() + Long.parseLong(refreshTokenExpirationTime));
    }

    public String createAccessToken(Long memberId, Role role, Date expirationTime) {
        String accessToken = Jwts.builder()
            .setSubject(TokenType.ACCESS.name())
            .setIssuedAt(new Date()) // 현재시간으로 토큰발급시간 설정
            .setExpiration(expirationTime)
            .claim("memberId", memberId) // 회원 아이디
            .claim("role", role)
            .signWith(SignatureAlgorithm.HS512, tokenSecret.getBytes(StandardCharsets.UTF_8))
            .setHeaderParam("type", "JWT")
            .compact();
        return accessToken;
    }

    public String createRefreshToken(Long memberId, Date expirationTime) {
        String refreshToken = Jwts.builder()
            .setSubject(TokenType.REFRESH.name())
            .setIssuedAt(new Date()) // 현재시간으로 토큰발급시간 설정
            .setExpiration(expirationTime)
            .claim("memberId", memberId) // 회원 아이디, refresh token에는 많은 정보를 담지 않기 위해 role은 제외
            .signWith(SignatureAlgorithm.HS512, tokenSecret.getBytes(StandardCharsets.UTF_8))
            .setHeaderParam("type", "JWT")
            .compact();
        return refreshToken;
    }

    // 클라이언트에서 토큰들이 어써라이제이션 헤더에 담겨서 들어올 텐데 이 값들이 발급한 만료되지 않은 토큰인지 검증
    // -> access 토큰 refresh 토큰 둘 다 검증 가능
    public void validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            log.info("token 만료", e);
            // todo 예외는 왜 또 따로 던져주는지 모르겠다 - 아마도 각각의 예외에 대한 에러코드와 메시지를 클라이언트에 보내주기 위해
            throw new AuthenticationException(ErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) { // 위조/변조/발급하지 않은 토큰일때
            log.info("유효하지 않은 token", e);
            throw new AuthenticationException(ErrorCode.NOT_VALID_TOKEN);
        }
    }

    // 토큰 정보를 서버에서 사용하기 위해 페이로드에 있는 클레임 정보들을 가지고 오는 메서드
    public Claims getTokenClaims(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token).getBody();
        } catch (Exception e) {
            log.info("유효하지 않은 token", e);
            throw new AuthenticationException(ErrorCode.NOT_VALID_TOKEN);
        }
        return claims;
    }

}
