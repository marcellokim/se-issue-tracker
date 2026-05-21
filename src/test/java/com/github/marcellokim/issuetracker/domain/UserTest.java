package com.github.marcellokim.issuetracker.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("사용자 도메인 모델")
class UserTest {

        private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 10, 0);

        @Test
        @DisplayName("새 사용자는 활성 계정과 동일한 생성/수정 시각으로 생성된다")
        void createActiveUserWithLoginIdentifierAndRole() {
                var user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

                assertEquals("dev1", user.getLoginId());
                assertEquals("Dev One", user.getName());
                assertEquals("hash", user.getPasswordHash());
                assertEquals(Role.DEV, user.getRole());
                assertTrue(user.isActive());
                assertEquals(NOW, user.getCreatedAt());
                assertEquals(NOW, user.getUpdatedAt());
                assertTrue(user.hasRole(Role.DEV));
                assertFalse(user.hasRole(Role.TESTER));
        }

        @Test
        @DisplayName("저장소에서 복원한 사용자는 저장된 계정 상태를 보존한다")
        void fromPersistencePreservesStoredUserState() {
                var user = User.fromPersistence("dev1", "Dev One", "hash", Role.DEV, false, null, null);

                assertEquals("dev1", user.getLoginId());
                assertEquals("Dev One", user.getName());
                assertEquals("hash", user.getPasswordHash());
                assertEquals(Role.DEV, user.getRole());
                assertFalse(user.isActive());
        }

        @Test
        @DisplayName("사용자 핵심 값은 비어 있을 수 없다")
        void rejectBlankUserFields() {
                assertThrows(IllegalArgumentException.class,
                                () -> User.create("", "Dev One", "hash", Role.DEV, NOW));
                assertThrows(IllegalArgumentException.class,
                                () -> User.create("dev1", " ", "hash", Role.DEV, NOW));
                assertThrows(IllegalArgumentException.class,
                                () -> User.create("dev1", "Dev One", "", Role.DEV, NOW));
        }

        @Test
        @DisplayName("사용자 역할은 필수 값이다")
        void rejectMissingRole() {
                assertThrows(NullPointerException.class,
                                () -> User.create("dev1", "Dev One", "hash", null, NOW));
        }

        @Test
        @DisplayName("새 사용자 생성 시점은 필수 값이다")
        void rejectMissingCreationTime() {
                assertThrows(NullPointerException.class,
                                () -> User.create("dev1", "Dev One", "hash", Role.DEV, null));
        }

        @Test
        @DisplayName("사용자는 비활성화될 수 있다")
        void deactivateUser() {
                var user = User.create("dev1", "Dev One", "hash", Role.DEV, NOW);

                user.deactivate();

                assertFalse(user.isActive());
        }
}
