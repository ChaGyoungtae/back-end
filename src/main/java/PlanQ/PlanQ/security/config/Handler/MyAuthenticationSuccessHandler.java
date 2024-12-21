package PlanQ.PlanQ.security.config.Handler;

import PlanQ.PlanQ.Member.MemberService;
import PlanQ.PlanQ.security.Jwt.GeneratedToken;
import PlanQ.PlanQ.security.Jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    private final MemberService memberService;

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // OAuth2User로 캐스팅하여 인증된 사용자 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        // 사용자 이메일을 가져온다.
        String email = oAuth2User.getAttribute("email");
        // 서비스 제공 플랫폼(google, kakao, naver)이 어디인지 가져온다.
        String provider = oAuth2User.getAttribute("provider");

        // 이름
        String nickname = oAuth2User.getAttribute("nickname");

        // 프로필사진url
        String profile_image_url = oAuth2User.getAttribute("profile_image_url");

        // CustomOAuth2UserService에서 셋팅한 로그인한 회원 존재 여부를 가져온다.
        boolean isExist = oAuth2User.getAttribute("exist");
        // OAuth2User로부터 Role을 얻어온다.
        String role = oAuth2User.getAuthorities().stream().
                findFirst() // 첫번째 Role을 찾아온다
                .orElseThrow(IllegalAccessError::new) // 존재하지않을시 예외를 던진다.
                .getAuthority();  // Role을 가져온다.
        // 회원이 존재하지 않는 경우
        if(!isExist) {
            log.info("회원가입 시작");
            memberService.register(email, provider, nickname, profile_image_url);
            log.info("회원가입 완료");
        }
        // 회원이 존재하면 jwt token 발행을 시작한다.
            GeneratedToken token = jwtUtil.generateToken(email, role);
            log.info("jwtToken = {}", token.getAccessToken());
            //log.info("jwtToken = {}", token.getRefreshToken());

            // accessToken을 쿼리스트링에 담는 url을 만들어준다.
            //String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/loginSuccess")
            String targetUrl = UriComponentsBuilder.fromUriString("http://planq.choizeus.com:9001/loginSuccess")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            // response 헤더에 토큰 추가
            response.setHeader("Authorization", "Bearer " + token.getAccessToken());

            log.info("redirect 준비");
            // 로그인 확인 페이지로 리다이렉트
            getRedirectStrategy().sendRedirect(request,response,targetUrl);
    }
}
