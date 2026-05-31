package com.github.marcellokim.issuetracker.ui.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.domain.User;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Swing account management panel")
class AccountManagementPanelTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 31, 0, 0);

    @Test
    @DisplayName("renders users and selection-sensitive actions")
    void rendersUsersAndSelectionActions() throws Exception {
        SwingComponentTestSupport.onEdt(() -> {
            AccountManagementPanel panel = panel(new FixedAccountDialogs());
            panel.showUsers(List.of(
                    userResult("dev1", Role.DEV, true),
                    userResult("tester1", Role.TESTER, false)));

            JTable table = SwingComponentTestSupport.find(panel, "accountUserTable", JTable.class);
            JButton rename = SwingComponentTestSupport.find(panel, "renameAccountButton", JButton.class);
            JButton activate = SwingComponentTestSupport.find(panel, "activateAccountButton", JButton.class);
            JButton deactivate = SwingComponentTestSupport.find(panel, "deactivateAccountButton", JButton.class);

            assertEquals(2, table.getRowCount());
            assertEquals("dev1", table.getValueAt(0, 0));
            assertEquals("Yes", table.getValueAt(0, 3));
            assertEquals(false, rename.isEnabled());

            table.setRowSelectionInterval(1, 1);

            assertEquals(true, rename.isEnabled());
            assertEquals(true, activate.isEnabled());
            assertEquals(false, deactivate.isEnabled());
        });
    }

    @Test
    @DisplayName("uses dialogs and selected row to publish account actions")
    void publishesAccountActions() throws Exception {
        var createRef = new AtomicReference<AccountCreateRequest>();
        var renameRef = new AtomicReference<String>();
        var roleRef = new AtomicReference<Role>();
        var activateRef = new AtomicReference<String>();
        var deactivateRef = new AtomicReference<String>();
        var backClicks = new AtomicInteger();
        var logoutClicks = new AtomicInteger();
        FixedAccountDialogs dialogs = new FixedAccountDialogs();

        SwingComponentTestSupport.onEdt(() -> {
            AccountManagementPanel panel = new AccountManagementPanel(
                    userResult("admin", Role.ADMIN, true),
                    dialogs,
                    createRef::set,
                    (loginId, name) -> renameRef.set(loginId + ":" + name),
                    (loginId, role) -> roleRef.set(role),
                    activateRef::set,
                    deactivateRef::set,
                    backClicks::incrementAndGet,
                    logoutClicks::incrementAndGet);
            panel.showUsers(List.of(userResult("dev1", Role.DEV, true)));
            JTable table = SwingComponentTestSupport.find(panel, "accountUserTable", JTable.class);
            table.setRowSelectionInterval(0, 0);

            SwingComponentTestSupport.find(panel, "createAccountButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "renameAccountButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "changeRoleButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "deactivateAccountButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "accountBackButton", JButton.class).doClick();
            SwingComponentTestSupport.find(panel, "accountLogoutButton", JButton.class).doClick();

            panel.showUsers(List.of(userResult("tester1", Role.TESTER, false)));
            table.setRowSelectionInterval(0, 0);
            SwingComponentTestSupport.find(panel, "activateAccountButton", JButton.class).doClick();
        });

        assertEquals(new AccountCreateRequest("newdev", "New Dev", "password", Role.DEV), createRef.get());
        assertEquals("dev1:Renamed User", renameRef.get());
        assertEquals(Role.TESTER, roleRef.get());
        assertEquals("dev1", deactivateRef.get());
        assertEquals("tester1", activateRef.get());
        assertEquals(1, backClicks.get());
        assertEquals(1, logoutClicks.get());
    }

    private static AccountManagementPanel panel(FixedAccountDialogs dialogs) {
        return new AccountManagementPanel(
                userResult("admin", Role.ADMIN, true),
                dialogs,
                ignored -> {
                },
                (loginId, name) -> {
                },
                (loginId, role) -> {
                },
                ignored -> {
                },
                ignored -> {
                },
                () -> {
                },
                () -> {
                });
    }

    private static UserResult userResult(String loginId, Role role, boolean active) {
        return UserResult.from(User.fromPersistence(loginId, loginId, "stored-password", role, active, NOW, NOW));
    }

    private static final class FixedAccountDialogs implements AccountDialogs {

        @Override
        public Optional<AccountCreateRequest> requestCreate(AccountManagementPanel parent) {
            return Optional.of(new AccountCreateRequest("newdev", "New Dev", "password", Role.DEV));
        }

        @Override
        public Optional<String> requestRename(AccountManagementPanel parent, UserResult selectedUser) {
            return Optional.of("Renamed User");
        }

        @Override
        public Optional<Role> requestRole(AccountManagementPanel parent, UserResult selectedUser) {
            return Optional.of(Role.TESTER);
        }

        @Override
        public boolean confirmActivation(AccountManagementPanel parent, UserResult selectedUser, boolean active) {
            return true;
        }
    }
}
