package com.schedule.api.legal.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountDeletionController {

    private final String supportEmail;

    public AccountDeletionController(@Value("${app.support.email:sungchul05.dev@gmail.com}") String supportEmail) {
        this.supportEmail = supportEmail;
    }

    @GetMapping(value = "/account-deletion", produces = MediaType.TEXT_HTML_VALUE)
    public String accountDeletion() {
        String escapedEmail = escapeHtml(supportEmail);
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>linkTogether 계정 및 데이터 삭제 요청</title>
                  <style>
                    body {
                      margin: 0;
                      font-family: Arial, "Noto Sans KR", sans-serif;
                      line-height: 1.6;
                      color: #202124;
                      background: #fff;
                    }
                    main {
                      max-width: 760px;
                      margin: 0 auto;
                      padding: 40px 20px;
                    }
                    h1 {
                      font-size: 28px;
                      margin: 0 0 24px;
                    }
                    h2 {
                      font-size: 18px;
                      margin: 28px 0 8px;
                    }
                    p, li {
                      font-size: 15px;
                    }
                    a {
                      color: #0b57d0;
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>linkTogether 계정 및 데이터 삭제 요청</h1>
                    <p>이 페이지는 Google Play 스토어에 윤성철 개발자 이름으로 등록된 linkTogether 앱 계정과 관련 데이터 삭제 요청을 위한 안내 페이지입니다.</p>

                    <h2>앱 및 개발자 정보</h2>
                    <ul>
                      <li>앱 이름: linkTogether</li>
                      <li>개발자 이름: 윤성철</li>
                    </ul>

                    <h2>삭제 요청 방법</h2>
                    <p>
                      계정 삭제를 원하시면 아래 이메일로 요청해 주세요.
                      요청 시 앱에 로그인한 카카오 계정 이메일 또는 앱 닉네임을 함께 보내 주세요.
                    </p>
                    <p><a href="mailto:%1$s">%1$s</a></p>

                    <h2>삭제되는 데이터</h2>
                    <ul>
                      <li>앱 사용자 계정 정보</li>
                      <li>일정 및 근무표 데이터</li>
                      <li>그룹 및 초대 관련 데이터</li>
                      <li>앱 알림을 위한 기기 토큰</li>
                    </ul>

                    <h2>보관될 수 있는 데이터</h2>
                    <p>
                      법령 준수, 보안, 부정 이용 방지 등 정당한 사유가 있는 데이터는 필요한 기간 동안 보관된 후 삭제될 수 있습니다.
                    </p>
                  </main>
                </body>
                </html>
                """.formatted(escapedEmail);
    }

    @GetMapping(value = "/child-safety-standards", produces = MediaType.TEXT_HTML_VALUE)
    public String childSafetyStandards() {
        String escapedEmail = escapeHtml(supportEmail);
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>linkTogether 아동 안전 표준</title>
                  <style>
                    body {
                      margin: 0;
                      font-family: Arial, "Noto Sans KR", sans-serif;
                      line-height: 1.6;
                      color: #202124;
                      background: #fff;
                    }
                    main {
                      max-width: 760px;
                      margin: 0 auto;
                      padding: 40px 20px;
                    }
                    h1 {
                      font-size: 28px;
                      margin: 0 0 24px;
                    }
                    h2 {
                      font-size: 18px;
                      margin: 28px 0 8px;
                    }
                    p, li {
                      font-size: 15px;
                    }
                    a {
                      color: #0b57d0;
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>linkTogether 아동 안전 표준</h1>
                    <p>linkTogether는 아동 성적 학대 및 착취(CSAE)와 아동 성적 학대 자료(CSAM)를 엄격히 금지합니다. 이 표준은 앱 이용자 보호와 관련 법규 준수를 위해 공개됩니다.</p>

                    <h2>앱 및 개발자 정보</h2>
                    <ul>
                      <li>앱 이름: linkTogether</li>
                      <li>개발자 이름: 윤성철</li>
                      <li>연락처: <a href="mailto:%1$s">%1$s</a></li>
                    </ul>

                    <h2>금지되는 행위 및 콘텐츠</h2>
                    <ul>
                      <li>아동을 성적으로 착취하거나 학대하는 콘텐츠의 생성, 공유, 요청, 홍보</li>
                      <li>아동 성적 학대 자료(CSAM)의 업로드, 저장, 배포, 링크 공유</li>
                      <li>미성년자를 대상으로 한 성적 접근, 유인, 협박, 그루밍 행위</li>
                    </ul>

                    <h2>신고 및 조치</h2>
                    <p>앱 내 아동 안전 관련 우려사항 또는 위반 의심 사례는 아래 이메일로 신고할 수 있습니다. 신고에는 가능한 경우 관련 사용자 정보, 발생 일시, 문제 내용을 포함해 주세요.</p>
                    <p><a href="mailto:%1$s">%1$s</a></p>
                    <p>신고된 내용은 검토 후 필요한 경우 계정 제한, 콘텐츠 삭제, 서비스 이용 차단 등의 조치를 취할 수 있습니다.</p>

                    <h2>법률 준수 및 기관 협조</h2>
                    <p>linkTogether는 관련 아동 안전 법규를 준수하며, 아동 안전과 관련된 긴급하거나 중대한 사안에 대해 관련 당국 및 법 집행기관의 정당한 요청에 협조합니다.</p>

                    <h2>정책 검토</h2>
                    <p>이 표준은 서비스 운영 및 관련 정책 변화에 따라 업데이트될 수 있습니다.</p>
                  </main>
                </body>
                </html>
                """.formatted(escapedEmail);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
