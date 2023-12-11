package com.joeun.jwtsecurity.security.jwt.provider;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.joeun.jwtsecurity.domain.Users;
import com.joeun.jwtsecurity.prop.JwtProps;
import com.joeun.jwtsecurity.security.domain.CustomUser;
import com.joeun.jwtsecurity.security.jwt.constants.SecurityConstants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;


/**
 * 🔐 JWT 토큰 관련 기능을 제공해주는 클래스
 * ✅ 토큰 생성
 * ✅ 토큰 해석
 * ✅ 토큰 유효성 검사
 */
@Slf4j
@Component
public class JwtTokenProvider {


    @Autowired
    private JwtProps jwtProps;

    /*
     * 👩‍💼➡🔐 토큰 생성
     */
    public String createToken(int userNo, String userId, List<String> roles) {
        byte[] signingKey = getSigningKey();

        // JWT 토큰 생성
        String jwt = Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(signingKey), Jwts.SIG.HS512)      // 서명에 사용할 키와 알고리즘 설정
                // .setHeaderParam("typ", SecurityConstants.TOKEN_TYPE)        // deprecated (version: before 1.0)
                .header()                                                      // update (version : after 1.0)
                    .add("typ", SecurityConstants.TOKEN_TYPE)              // 헤더 설정
                .and()
                .expiration(new Date(System.currentTimeMillis() + 864000000))  // 토큰 만료 시간 설정 (10일)
                .claim("uno", "" + userNo)                                // 클레임 설정: 사용자 번호
                .claim("uid", userId)                                     // 클레임 설정: 사용자 아이디
                .claim("rol", roles)                                      // 클레임 설정: 권한
                .compact();      

        log.info("jwt : " + jwt);

        return jwt;
    }


    /**
     * 🔐➡👩‍💼 토큰 해석
     * 
     * Authorization : Bearer + {jwt}  (authHeader)
     * ➡ jwt 추출 
     * ➡ UsernamePasswordAuthenticationToken
     * @param authHeader
     * @return
     */
    public UsernamePasswordAuthenticationToken getAuthentication(String authHeader) {
        if(authHeader == null || authHeader.length() == 0 ) 
            return null;

        try {
            
            // jwt 추출 
            String jwt = authHeader.replace("Bearer ", "");

            // 🔐➡👩‍💼 JWT 파싱
            Jws<Claims> parsedToken = Jwts.parser()
                                            .verifyWith(getShaKey())
                                            .build()
                                            .parseSignedClaims(jwt);    

            log.info("parsedToken : " + parsedToken);

            // 인증된 사용자 번호
            Integer userNo = (Integer) parsedToken.getPayload().get("uno");
            userNo = userNo == null ? 0 : userNo;
            log.info("userNo : " + userNo);

            // 인증된 사용자 아이디
            String userId = parsedToken.getPayload().get("uid").toString();
            log.info("userId : " + userId);

            // 인증된 사용자 권한
            Claims claims = parsedToken.getPayload();
            Object roles = claims.get("rol");
            log.info("roles : " + roles);


            // 토큰에 userId 있는지 확인
            if( userId == null || userId.length() == 0 )
                return null;


            Users user = new Users();
            user.setNo(userNo);
            user.setUserId(userId);
            // TODO: 권한도 바로 Users 객체에 담아보기

            // OK: CustomeUser 에 권한 담기
            List<SimpleGrantedAuthority> authorities = ((List<?>) roles )
                                                        .stream()
                                                        .map(auth -> new SimpleGrantedAuthority( (String) auth ))
                                                        .collect( Collectors.toList() );

            UserDetails userDetails = new CustomUser(user, authorities);

            // TODO: UsernamePasswordAuthenticationToken() 인자의 의미 확인하기
            return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        } catch (ExpiredJwtException exception) {
            log.warn("Request to parse expired JWT : {} failed : {}", authHeader, exception.getMessage());
        } catch (UnsupportedJwtException exception) {
            log.warn("Request to parse unsupported JWT : {} failed : {}", authHeader, exception.getMessage());
        } catch (MalformedJwtException exception) {
            log.warn("Request to parse invalid JWT : {} failed : {}", authHeader, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            log.warn("Request to parse empty or null JWT : {} failed : {}", authHeader, exception.getMessage());
        }

        return null;
    }



    // 
    /**
     * 🔐❓ 토큰 유효성 검사
     * @param jwt
     * @return
     *  ⭕ true     : 유효
     *  ❌ false    : 만료
     */
    public boolean validateToken(String jwt) {

        try {

            // 🔐➡👩‍💼 JWT 파싱
	        Jws<Claims> claims = Jwts.parser()
                                    .verifyWith(getShaKey())
                                    .build()
                                    .parseSignedClaims(jwt);    
            /*
                PAYLOAD
                {
                    "exp": 1703140095,        ⬅ 만료기한 추출
                    "uid": "joeun",
                    "rol": [
                        "ROLE_USER"
                    ]   
                }
            */
	        return !claims.getPayload().getExpiration().before(new Date());
	    } catch (ExpiredJwtException exception) {
            log.error("Token Expired");
            return false;
        } catch (JwtException exception) {
            log.error("Token Tampered");
            return false;
        } catch (NullPointerException exception) {
            log.error("Token is null");
            return false;
        } catch (Exception e) {
	        return false;
	    }
    }


    // secretKey ➡ signingKey
    private byte[] getSigningKey() {
		return jwtProps.getSecretKey().getBytes();
	}

    // secretKey ➡ (HMAC-SHA algorithms) ➡ signingKey
    private SecretKey getShaKey() {
        return Keys.hmacShaKeyFor(getSigningKey());
    }

    
}
