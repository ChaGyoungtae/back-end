package PlanQ.PlanQ.security.Jwt;

import PlanQ.PlanQ.Member.Member;
import PlanQ.PlanQ.Member.MemberRepository;
import PlanQ.PlanQ.security.Dto.SecurityUserDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        return uri.startsWith("/swagger-ui/**") || uri.startsWith("/v3/api-docs/**") || uri.contains("token/refresh") || uri.contains("/login") || uri.contains("/loginSuccess");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // request Header에서 AccessToken을 가져온다.
        String atc = request.getHeader("Authorization");
        log.info(atc);
//        if (responseUri.startsWith("/login")) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        if(atc == null){
            filterChain.doFilter(request,response);
            return;
        }

        atc = atc.split(" ")[1];

        if (!StringUtils.hasText(atc)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization header is required");
            return;
        }

        // AccessToken을 검증하고, 만료되었을경우 예외를 발생시킨다.
        if(!jwtUtil.verifyToken(atc)) {
            throw new JwtException("Access Token 만료");
        }

        // AccessToken의 값이 있고, 유효한 경우에 진행한다.
        if(jwtUtil.verifyToken(atc)) {

            // AccessToken 내부의 payload에 있는 email로 user를 조회한다. 없다면 예외를 발생시킨다. -> 정상 케이스가 아님
            Member findMember = memberRepository.findByEmail(jwtUtil.getUid(atc))
                    .orElseThrow(IllegalStateException::new);

            // SecurityContext에 등록할 User 객체를 만들어준다.
            SecurityUserDto userDto = SecurityUserDto.builder()
                    .memberNo(findMember.getId())
                    .email(findMember.getEmail())
                    .picture(findMember.getProfile())
                    .role("ROLE_".concat(findMember.getUserRole()))
                    .nickname(findMember.getNickname())
                    .build();

            // SecurityContext에 인증 객체를 등록해준다.
            Authentication auth = getAuthentication(userDto);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    public Authentication getAuthentication(SecurityUserDto member) {
        return new UsernamePasswordAuthenticationToken(member, "",
                List.of(new SimpleGrantedAuthority(member.getRole())));
    }
}
