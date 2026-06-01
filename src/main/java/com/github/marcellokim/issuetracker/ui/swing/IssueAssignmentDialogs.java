package com.github.marcellokim.issuetracker.ui.swing;

import com.github.marcellokim.issuetracker.service.AssignmentCandidateResult;
import com.github.marcellokim.issuetracker.service.AssignmentOptionsResult;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

final class IssueAssignmentDialogs {

    private static final String DIALOG_TITLE = "Assign issue";

    private IssueAssignmentDialogs() {
    }

    static Optional<IssueAssignmentRequest> prompt(
            Component parent,
            IssueAssignmentMode mode,
            AssignmentOptionsResult options) {
        AssignmentForm form = form(mode, options);
        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent,
                    form.panel(),
                    DIALOG_TITLE,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return Optional.empty();
            }
            try {
                return Optional.of(new IssueAssignmentRequest(
                        mode,
                        selectedLoginId(form.assigneeBox()),
                        selectedLoginId(form.verifierBox())));
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                        parent,
                        exception.getMessage(),
                        DIALOG_TITLE,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static AssignmentForm form(IssueAssignmentMode mode, AssignmentOptionsResult options) {
        List<AssignmentCandidateResult> devCandidates = candidatesFor(
                options.devAssigneeCandidates(),
                options.allDevAssignees(),
                mode == IssueAssignmentMode.REASSIGN_DEV ? "current assignee" : null);
        List<AssignmentCandidateResult> testerCandidates = candidatesFor(
                options.testerVerifierCandidates(),
                options.allTesterVerifiers(),
                mode == IssueAssignmentMode.CHANGE_TESTER ? "current verifier" : null);
        Set<String> recommendedDevIds = candidateIds(options.devAssigneeCandidates());
        Set<String> recommendedTesterIds = candidateIds(options.testerVerifierCandidates());

        JPanel panel = new JPanel(new BorderLayout(SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        panel.setBorder(BorderFactory.createEmptyBorder(
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP,
                SwingStyles.ROW_GAP));
        JLabel title = new JLabel(label(mode));
        title.setName("assignmentDialogTitle");
        panel.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel(new java.awt.GridLayout(0, 1, SwingStyles.ROW_GAP, SwingStyles.ROW_GAP));
        JComboBox<AssignmentCandidateResult> assigneeBox = candidateBox(
                "assignmentAssigneeBox",
                devCandidates,
                recommendedDevIds);
        JComboBox<AssignmentCandidateResult> verifierBox = candidateBox(
                "assignmentVerifierBox",
                testerCandidates,
                recommendedTesterIds);
        if (mode == IssueAssignmentMode.ASSIGN || mode == IssueAssignmentMode.REASSIGN_DEV) {
            fields.add(new JLabel("Assignee (DEV)"));
            fields.add(assigneeBox);
        }
        if (mode == IssueAssignmentMode.ASSIGN || mode == IssueAssignmentMode.CHANGE_TESTER) {
            fields.add(new JLabel("Verifier (TESTER)"));
            fields.add(verifierBox);
        }
        panel.add(fields, BorderLayout.CENTER);
        return new AssignmentForm(panel, assigneeBox, verifierBox);
    }

    private static List<AssignmentCandidateResult> candidatesFor(
            List<AssignmentCandidateResult> recommended,
            List<AssignmentCandidateResult> all,
            String excludedReason) {
        List<AssignmentCandidateResult> merged = mergedCandidates(recommended, all);
        if (excludedReason == null) {
            return merged;
        }
        Set<String> excludedIds = candidateIdsByReason(recommended, excludedReason);
        return merged.stream()
                .filter(candidate -> !excludedIds.contains(candidate.loginId()))
                .toList();
    }

    private static JComboBox<AssignmentCandidateResult> candidateBox(
            String name,
            List<AssignmentCandidateResult> candidates,
            Set<String> recommendedIds) {
        JComboBox<AssignmentCandidateResult> box =
                new JComboBox<>(candidates.toArray(AssignmentCandidateResult[]::new));
        box.setName(name);
        box.setRenderer(new CandidateRenderer(recommendedIds));
        return box;
    }

    private static String selectedLoginId(JComboBox<AssignmentCandidateResult> box) {
        AssignmentCandidateResult candidate = (AssignmentCandidateResult) box.getSelectedItem();
        return candidate == null ? null : candidate.loginId();
    }

    private static List<AssignmentCandidateResult> mergedCandidates(
            List<AssignmentCandidateResult> recommended,
            List<AssignmentCandidateResult> all) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<AssignmentCandidateResult> merged = new ArrayList<>();
        for (AssignmentCandidateResult candidate : recommended) {
            if (seen.add(candidate.loginId())) {
                merged.add(candidate);
            }
        }
        for (AssignmentCandidateResult candidate : all) {
            if (seen.add(candidate.loginId())) {
                merged.add(candidate);
            }
        }
        return merged;
    }

    private static Set<String> candidateIds(List<AssignmentCandidateResult> candidates) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (AssignmentCandidateResult candidate : candidates) {
            ids.add(candidate.loginId());
        }
        return ids;
    }

    private static Set<String> candidateIdsByReason(List<AssignmentCandidateResult> candidates, String reason) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (AssignmentCandidateResult candidate : candidates) {
            if (reason.equals(candidate.reason())) {
                ids.add(candidate.loginId());
            }
        }
        return ids;
    }

    private static String label(IssueAssignmentMode mode) {
        return switch (mode) {
            case ASSIGN -> DIALOG_TITLE;
            case REASSIGN_DEV -> "Reassign assignee";
            case CHANGE_TESTER -> "Change verifier";
        };
    }

    record AssignmentForm(
            JPanel panel,
            JComboBox<AssignmentCandidateResult> assigneeBox,
            JComboBox<AssignmentCandidateResult> verifierBox) {
    }

    private static final class CandidateRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;
        private final Set<String> recommendedIds;

        private CandidateRenderer(Set<String> recommendedIds) {
            this.recommendedIds = Set.copyOf(recommendedIds);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);
            label.setText(candidateLabel((AssignmentCandidateResult) value));
            return label;
        }

        private String candidateLabel(AssignmentCandidateResult candidate) {
            if (candidate == null) {
                return "";
            }
            String prefix = recommendedIds.contains(candidate.loginId()) ? "[Recommended] " : "";
            return prefix + candidate.loginId() + " (" + candidate.name() + ") - " + candidate.reason();
        }
    }
}
