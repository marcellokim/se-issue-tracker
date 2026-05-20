package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("사용자 도메인 모델")
class UserTest {

        @Test
        @DisplayName("사용자는 인증 식별자와 역할을 가진 활성 계정으로 생성된다")
        void createActiveUserWithLoginIdentifierAndRole() {
                // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
                var user = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);

                assertEquals("dev1", user.getLoginId());
                assertEquals("Dev One", user.getName());
                assertEquals("hash", user.getPasswordHash());
                assertEquals(Role.DEV, user.getRole());
                assertTrue(user.isActive());
                assertTrue(user.hasRole(Role.DEV));
                assertFalse(user.hasRole(Role.TESTER));
        }

        @Test
        @DisplayName("사용자 핵심 값은 비어 있을 수 없다")
        void rejectBlankUserFields() {
                // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
                assertThrows(IllegalArgumentException.class,
                                () -> User.create("", "Dev One", "hash", Role.DEV, true, null, null));
                assertThrows(IllegalArgumentException.class,
                                () -> User.create("dev1", " ", "hash", Role.DEV, true, null, null));
                assertThrows(IllegalArgumentException.class,
                                () -> User.create("dev1", "Dev One", "", Role.DEV, true, null, null));
        }

        @Test
        @DisplayName("사용자 역할은 필수 값이다")
        void rejectMissingRole() {
                // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
                assertThrows(NullPointerException.class,
                                () -> User.create("dev1", "Dev One", "hash", null, true, null, null));
        }

        @Test
        @DisplayName("사용자는 비활성화될 수 있다")
        void deactivateUser() {
                // userId 제거: 5-param → 7-param 통합 (DCD ver1 기준)
                var user = User.create("dev1", "Dev One", "hash", Role.DEV, true, null, null);

                user.deactivate();

                assertFalse(user.isActive());
        }
}
