package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

final class IssueGraphScreen extends VBox {

    private static final double NODE_RADIUS = 26;
    private static final double PADDING = 60;
    private static final int FORCE_ITERATIONS = 120;
    private static final double REPULSION = 8000;
    private static final double ATTRACTION = 0.005;
    private static final double DAMPING = 0.85;
    private static final Map<IssueStatus, Color> STATUS_COLORS = Map.of(
            IssueStatus.NEW, Color.web("#60A5FA"),
            IssueStatus.ASSIGNED, Color.web("#FBBF24"),
            IssueStatus.FIXED, Color.web("#34D399"),
            IssueStatus.RESOLVED, Color.web("#A78BFA"),
            IssueStatus.CLOSED, Color.web("#9CA3AF"),
            IssueStatus.REOPENED, Color.web("#F87171"),
            IssueStatus.DELETED, Color.web("#6B7280"));

    private final IssueController issueController;
    private final long projectId;
    private final Label messageLabel = ScreenComponents.messageLabel();
    private final ListView<String> issueListView = new ListView<>();
    private List<IssueSummary> graphIssues;
    private List<DependencyResult> graphDeps;
    private Canvas canvas;
    private Runnable onBack;

    IssueGraphScreen(IssueController issueController, long projectId){
        this.issueController = issueController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Issues", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Issue Dependency Graph");

        Label legend = new Label("NEW(blue)  ASSIGNED(yellow)  FIXED(green)  RESOLVED(purple)  CLOSED(gray)  REOPENED(red)");
        legend.getStyleClass().add("muted-label");

        Pane canvasHolder = new Pane();
        canvasHolder.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        issueListView.setPrefHeight(100);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel),
                legend, canvasHolder,
                new Label("Dependency issues:"), issueListView, messageLabel);

        loadData();
        canvasHolder.widthProperty().addListener((obs, old, val) -> renderGraph(canvasHolder));
        canvasHolder.heightProperty().addListener((obs, old, val) -> renderGraph(canvasHolder));
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadData(){
        try{
            List<IssueSummary> allIssues = issueController.viewRelatedProjectIssues(projectId);
            List<DependencyResult> allDeps = issueController.viewProjectDependencies(projectId);
            graphDeps = visibleDependencies(allIssues, allDeps);
            graphIssues = dependencyIssues(allIssues, graphDeps);
            issueListView.getItems().clear();
            for (IssueSummary issue : graphIssues){
                issueListView.getItems().add(String.format("[%s] %s | %s | %s",
                        ScreenComponents.shortIssueId(issue.issueId()), issue.title(), issue.status(), issue.priority()));
            }
            if (graphIssues.isEmpty()){
                ScreenComponents.showInfo(messageLabel, "No dependencies in this project");
            } else{
                ScreenComponents.showInfo(messageLabel, graphIssues.size() + " issues with dependencies, " + graphDeps.size() + " links");
            }
        } catch (Exception exception){
            graphIssues = List.of();
            graphDeps = List.of();
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    static List<DependencyResult> visibleDependencies(List<IssueSummary> issues, List<DependencyResult> deps){
        Set<Long> visibleIssueIds = issues.stream()
                .map(IssueSummary::id)
                .collect(Collectors.toSet());
        return deps.stream()
                .filter(dep -> visibleIssueIds.contains(dep.blockingIssueId())
                        && visibleIssueIds.contains(dep.blockedIssueId()))
                .toList();
    }

    static List<IssueSummary> dependencyIssues(List<IssueSummary> issues, List<DependencyResult> deps){
        Set<Long> depIssueIds = new HashSet<>();
        for (DependencyResult dep : deps){
            depIssueIds.add(dep.blockingIssueId());
            depIssueIds.add(dep.blockedIssueId());
        }
        return issues.stream()
                .filter(issue -> depIssueIds.contains(issue.id()))
                .toList();
    }

    private void renderGraph(Pane holder){
        double w = holder.getWidth();
        double h = holder.getHeight();
        if (w <= 0 || h <= 0 || graphIssues.isEmpty()) return;

        if (canvas == null){
            canvas = new Canvas(w, h);
            holder.getChildren().add(canvas);
        } else{
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        Map<Long, double[]> positions = forceDirectedLayout(graphIssues, graphDeps, w, h);
        drawEdges(gc, graphDeps, positions);
        drawNodes(gc, graphIssues, positions);
    }

    private static Map<Long, double[]> forceDirectedLayout(
            List<IssueSummary> issues, List<DependencyResult> deps, double w, double h){
        Map<Long, double[]> pos = new HashMap<>();
        Map<Long, double[]> vel = new HashMap<>();
        int count = issues.size();
        double cx = w / 2;
        double cy = h / 2;
        double initRadius = Math.min(w, h) / 3;
        for (int i = 0; i < count; i++){
            double angle = 2 * Math.PI * i / count;
            long id = issues.get(i).id();
            pos.put(id, new double[]{cx + initRadius * Math.cos(angle), cy + initRadius * Math.sin(angle)});
            vel.put(id, new double[]{0, 0});
        }
        for (int iter = 0; iter < FORCE_ITERATIONS; iter++){
            Map<Long, double[]> forces = new HashMap<>();
            for (IssueSummary issue : issues) forces.put(issue.id(), new double[]{0, 0});
            for (int i = 0; i < count; i++){
                for (int j = i + 1; j < count; j++){
                    long idA = issues.get(i).id();
                    long idB = issues.get(j).id();
                    double[] pA = pos.get(idA);
                    double[] pB = pos.get(idB);
                    double dx = pB[0] - pA[0];
                    double dy = pB[1] - pA[1];
                    double dist = Math.max(Math.sqrt(dx * dx + dy * dy), 1);
                    double force = REPULSION / (dist * dist);
                    double fx = force * dx / dist;
                    double fy = force * dy / dist;
                    forces.get(idA)[0] -= fx;
                    forces.get(idA)[1] -= fy;
                    forces.get(idB)[0] += fx;
                    forces.get(idB)[1] += fy;
                }
            }
            for (DependencyResult dep : deps){
                double[] pA = pos.get(dep.blockingIssueId());
                double[] pB = pos.get(dep.blockedIssueId());
                if (pA == null || pB == null) continue;
                double dx = pB[0] - pA[0];
                double dy = pB[1] - pA[1];
                double fx = ATTRACTION * dx;
                double fy = ATTRACTION * dy;
                double[] fA = forces.get(dep.blockingIssueId());
                double[] fB = forces.get(dep.blockedIssueId());
                if (fA != null){ fA[0] += fx; fA[1] += fy; }
                if (fB != null){ fB[0] -= fx; fB[1] -= fy; }
            }
            for (IssueSummary issue : issues){
                long id = issue.id();
                double[] v = vel.get(id);
                double[] f = forces.get(id);
                double[] p = pos.get(id);
                v[0] = (v[0] + f[0]) * DAMPING;
                v[1] = (v[1] + f[1]) * DAMPING;
                p[0] = Math.max(PADDING, Math.min(w - PADDING, p[0] + v[0]));
                p[1] = Math.max(PADDING, Math.min(h - PADDING, p[1] + v[1]));
            }
        }
        return pos;
    }

    private static void drawEdges(GraphicsContext gc, List<DependencyResult> deps, Map<Long, double[]> positions){
        for (DependencyResult dep : deps){
            double[] from = positions.get(dep.blockingIssueId());
            double[] to = positions.get(dep.blockedIssueId());
            if (from == null || to == null) continue;

            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < NODE_RADIUS * 2 + 6) continue;
            double ux = dx / dist;
            double uy = dy / dist;

            double x1 = from[0] + ux * NODE_RADIUS;
            double y1 = from[1] + uy * NODE_RADIUS;
            double x2 = to[0] - ux * (NODE_RADIUS + 6);
            double y2 = to[1] - uy * (NODE_RADIUS + 6);

            gc.setStroke(Color.web("#94A3B8"));
            gc.setLineWidth(1.5);
            gc.strokeLine(x1, y1, x2, y2);

            double arrowSize = 10;
            double ax = x2 + ux * 2;
            double ay = y2 + uy * 2;
            gc.setFill(Color.web("#64748B"));
            gc.fillPolygon(
                    new double[]{ax + ux * arrowSize, ax - ux * arrowSize * 0.3 + uy * arrowSize * 0.5, ax - ux * arrowSize * 0.3 - uy * arrowSize * 0.5},
                    new double[]{ay + uy * arrowSize, ay - uy * arrowSize * 0.3 - ux * arrowSize * 0.5, ay - uy * arrowSize * 0.3 + ux * arrowSize * 0.5},
                    3);
        }
    }

    private static void drawNodes(GraphicsContext gc, List<IssueSummary> issues, Map<Long, double[]> positions){
        gc.setFont(Font.font("System", 10));
        for (IssueSummary issue : issues){
            double[] pos = positions.get(issue.id());
            if (pos == null) continue;
            double x = pos[0];
            double y = pos[1];

            Color color = STATUS_COLORS.getOrDefault(issue.status(), Color.GRAY);

            gc.setFill(Color.rgb(0, 0, 0, 0.08));
            gc.fillOval(x - NODE_RADIUS + 2, y - NODE_RADIUS + 2, NODE_RADIUS * 2, NODE_RADIUS * 2);

            gc.setFill(color);
            gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            gc.setStroke(color.darker());
            gc.setLineWidth(2);
            gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            double brightness = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
            gc.setFill(brightness > 0.5 ? Color.web("#1e293b") : Color.WHITE);
            String label = ScreenComponents.shortIssueId(issue.issueId());
            gc.fillText(label, x - label.length() * 2.8, y + 4);
        }
    }
}
