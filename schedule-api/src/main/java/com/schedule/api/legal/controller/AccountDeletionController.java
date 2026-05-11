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
                  <title>계정 및 데이터 삭제 요청</title>
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
                    <h1>계정 및 데이터 삭제 요청</h1>
                    <p>이 페이지는 schedule-api 앱 계정과 관련 데이터 삭제 요청을 위한 안내 페이지입니다.</p>

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

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
