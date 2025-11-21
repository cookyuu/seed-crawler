package com.seed_crawler.core.global.auth;

import com.seed_crawler.core.entity.Member;
import com.seed_crawler.core.entity.enums.MemberRole;
import com.seed_crawler.core.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest httpReq, HttpServletResponse httpRes, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = jwtTokenProvider.resolveToken(httpReq);
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            UUID memberId = jwtTokenProvider.getMemberId(accessToken);
            MemberRole role = jwtTokenProvider.getRole(accessToken);


            Member member = memberRepository.findById(memberId).orElse(null);

            CustomUserDetails userDetails;
            if (member != null) {
                userDetails = new CustomUserDetails(
                        memberId,
                        role,
                        member.isAccountLock(),
                        member.isActive()
                );
            } else {
                // Member가 DB에 없어도 일단 인증은 설정 (Service에서 검증)
                userDetails = new CustomUserDetails(
                        memberId,
                        role,
                        false,  // accountLock: false (기본값)
                        true    // active: true (기본값)
                );
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(httpReq, httpRes);
    }
}
