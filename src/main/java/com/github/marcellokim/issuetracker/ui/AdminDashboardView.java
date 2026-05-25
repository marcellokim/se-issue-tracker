package com.github.marcellokim.issuetracker.ui;

import com.github.marcellokim.issuetracker.controller.AccountController;
import com.github.marcellokim.issuetracker.domain.Role;
import com.github.marcellokim.issuetracker.service.UserResult;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class AdminDashboardView {

    private final AccountController accountController;
    private final Consumer<String> onAccountChanged;
    private final TextField createLoginIdField = field("new account loginId");
    private final TextField createNameField = field("new account name");
    private final PasswordField createPasswordField = new PasswordField();
    private final ComboBox<Role> createRoleBox = roleBox();
    private final TextField renameLoginIdField = field("account loginId");
    private final TextField renameNameField = field("updated account name");
    private final TextField roleLoginIdField = field("account loginId");
    private final ComboBox<Role> roleUpdateBox = roleBox();
    private final TextField activationLoginIdField = field("account loginId");
    private final VBox root = new VBox(12);

    public AdminDashboardView(
            AccountController accountController,
            Consumer<String> onAccountChanged) {
        this.accountController = Objects.requireNonNull(accountController, "accountController");
        this.onAccountChanged = Objects.requireNonNull(onAccountChanged, "onAccountChanged");
        configure();
    }

    public Node root() {
        return root;
    }

    public void selectAccount(UserResult account) {
        Objects.requireNonNull(account, "account");
        renameLoginIdField.setText(account.loginId());
        renameNameField.setText(account.name());
        roleLoginIdField.setText(account.loginId());
        if (account.role() != Role.ADMIN) {
            roleUpdateBox.setValue(account.role());
        }
        activationLoginIdField.setText(account.loginId());
    }

    private void configure() {
        createPasswordField.setPromptText("temporary password");
        root.setPadding(new Insets(16));
        root.setStyle("-fx-border-color: #9ca3af; -fx-border-width: 2; -fx-background-color: #ffffff;");

        VBox createPanel = panel("Create Account");
        createPanel.getChildren().addAll(
                fieldsGrid(
                        "Login ID", createLoginIdField,
                        "Name", createNameField,
                        "Password", createPasswordField,
                        "Role", createRoleBox),
                actionRow(button("Create Account", () -> {
                    UserResult user = accountController.createAccount(
                            requiredText(createLoginIdField, "loginId"),
                            requiredText(createNameField, "name"),
                            requiredText(createPasswordField, "password"),
                            createRoleBox.getValue());
                    createPasswordField.clear();
                    return "Account created: " + formatAccount(user);
                })));

        VBox renamePanel = panel("Rename Account");
        renamePanel.getChildren().addAll(
                fieldsGrid(
                        "Login ID", renameLoginIdField,
                        "Name", renameNameField),
                actionRow(button("Rename Account", () -> {
                    UserResult user = accountController.renameAccount(
                            requiredText(renameLoginIdField, "loginId"),
                            requiredText(renameNameField, "name"));
                    return "Account renamed: " + formatAccount(user);
                })));

        VBox rolePanel = panel("Change Role");
        rolePanel.getChildren().addAll(
                fieldsGrid(
                        "Login ID", roleLoginIdField,
                        "Role", roleUpdateBox),
                actionRow(button("Change Role", () -> {
                    UserResult user = accountController.changeAccountRole(
                            requiredText(roleLoginIdField, "loginId"),
                            roleUpdateBox.getValue());
                    return "Account role changed: " + formatAccount(user);
                })));

        VBox activationPanel = panel("Activation");
        activationPanel.getChildren().addAll(
                fieldsGrid("Login ID", activationLoginIdField),
                actionRow(
                        button("Activate", () -> {
                            UserResult user = accountController.activateAccount(
                                    requiredText(activationLoginIdField, "loginId"));
                            return "Account activated: " + formatAccount(user);
                        }),
                        button("Deactivate", () -> {
                            UserResult user = accountController.deactivateAccount(
                                    requiredText(activationLoginIdField, "loginId"));
                            return "Account deactivated: " + formatAccount(user);
                        })));

        root.getChildren().addAll(
                sectionLabel("Account Management"),
                new HBox(12, createPanel, renamePanel, rolePanel, activationPanel));
    }

    private Button button(String text, Supplier<String> action) {
        Button button = new Button(text);
        button.setOnAction(event -> run(action));
        return button;
    }

    private void run(Supplier<String> action) {
        try {
            onAccountChanged.accept(action.get());
        } catch (RuntimeException exception) {
            onAccountChanged.accept("Failed: " + exception.getMessage());
        }
    }

    private static ComboBox<Role> roleBox() {
        ComboBox<Role> box = new ComboBox<>();
        box.getItems().setAll(Role.PL, Role.DEV, Role.TESTER);
        box.setValue(Role.DEV);
        return box;
    }

    private static VBox panel(String title) {
        VBox box = new VBox(10, sectionLabel(title));
        box.setPadding(new Insets(12));
        box.setStyle("-fx-border-color: #d1d5db; -fx-background-color: #f9fafb;");
        return box;
    }

    private static HBox actionRow(Node... buttons) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(buttons);
        return row;
    }

    private static GridPane fieldsGrid(Object... labelsAndFields) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        for (int index = 0; index < labelsAndFields.length; index += 2) {
            int row = index / 2;
            grid.add(new Label((String) labelsAndFields[index]), 0, row);
            grid.add((Node) labelsAndFields[index + 1], 1, row);
        }
        return grid;
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefColumnCount(16);
        return field;
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        return label;
    }

    private static String requiredText(TextInputControl field, String fieldName) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String formatAccount(UserResult user) {
        return "%s / %s / %s / %s".formatted(
                user.loginId(),
                user.name(),
                user.role(),
                user.active() ? "ACTIVE" : "INACTIVE");
    }
}
