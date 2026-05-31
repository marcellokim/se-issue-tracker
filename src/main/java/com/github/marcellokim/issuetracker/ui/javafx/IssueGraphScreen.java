package com.github.marcellokim.issuetracker.ui.javafx;

import com.github.marcellokim.issuetracker.controller.IssueController;
import com.github.marcellokim.issuetracker.domain.IssueStatus;
import com.github.marcellokim.issuetracker.service.DependencyResult;
import com.github.marcellokim.issuetracker.service.IssueSummary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

final class IssueGraphScreen extends VBox {

    private static final double NODE_RADIUS = 24;
    private static final double PADDING = 60;
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
    private List<IssueSummary> cachedIssues;
    private List<DependencyResult> cachedDeps;
    private Canvas canvas;
    private Runnable onBack;

    IssueGraphScreen(IssueController issueController, long projectId){
        this.issueController = issueController;
        this.projectId = projectId;
        ScreenComponents.applyScreenDefaults(this);

        Button backButton = ScreenComponents.backButton("← Issues", () -> { if (onBack != null) onBack.run(); });
        Label titleLabel = ScreenComponents.titleLabel("Issue Dependency Graph");

        Label legend = new Label("NEW(blue) ASSIGNED(yellow) FIXED(green) RESOLVED(purple) CLOSED(gray) REOPENED(red)");
        legend.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        Pane canvasHolder = new Pane();
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        getChildren().addAll(
                ScreenComponents.header(backButton, titleLabel),
                legend, canvasHolder, messageLabel);

        loadData();
        canvasHolder.widthProperty().addListener((obs, old, val) -> renderGraph(canvasHolder));
        canvasHolder.heightProperty().addListener((obs, old, val) -> renderGraph(canvasHolder));
    }

    void setOnBack(Runnable action){ this.onBack = action; }

    private void loadData(){
        try{
            cachedIssues = issueController.viewRelatedProjectIssues(projectId);
            cachedDeps = issueController.viewProjectDependencies(projectId);
            ScreenComponents.showInfo(messageLabel, cachedIssues.size() + " issues, " + cachedDeps.size() + " dependencies");
        } catch (Exception exception){
            cachedIssues = List.of();
            cachedDeps = List.of();
            ScreenComponents.showError(messageLabel, exception);
        }
    }

    private void renderGraph(Pane holder){
        double w = holder.getWidth();
        double h = holder.getHeight();
        if (w <= 0 || h <= 0 || cachedIssues.isEmpty()) return;

        if (canvas == null){
            canvas = new Canvas(w, h);
            holder.getChildren().add(canvas);
        } else{
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        Map<Long, double[]> positions = layoutNodes(cachedIssues, w, h);
        drawEdges(gc, cachedDeps, positions);
        drawNodes(gc, cachedIssues, positions);
    }

    private static Map<Long, double[]> layoutNodes(List<IssueSummary> issues, double w, double h){
        Map<Long, double[]> positions = new HashMap<>();
        int count = issues.size();
        double usableW = w - PADDING * 2;
        double usableH = h - PADDING * 2;

        if (count <= 8){
            double cx = w / 2;
            double cy = h / 2;
            double radius = Math.min(usableW, usableH) / 2.5;
            for (int i = 0; i < count; i++){
                double angle = 2 * Math.PI * i / count - Math.PI / 2;
                double x = cx + radius * Math.cos(angle);
                double y = cy + radius * Math.sin(angle);
                positions.put(issues.get(i).id(), new double[]{x, y});
            }
        } else{
            int cols = (int) Math.ceil(Math.sqrt(count));
            int rows = (int) Math.ceil((double) count / cols);
            double cellW = usableW / cols;
            double cellH = usableH / rows;
            for (int i = 0; i < count; i++){
                int col = i % cols;
                int row = i / cols;
                double x = PADDING + cellW * col + cellW / 2;
                double y = PADDING + cellH * row + cellH / 2;
                positions.put(issues.get(i).id(), new double[]{x, y});
            }
        }
        return positions;
    }

    private static void drawEdges(GraphicsContext gc, List<DependencyResult> deps, Map<Long, double[]> positions){
        gc.setStroke(Color.web("#94A3B8"));
        gc.setLineWidth(1.5);
        for (DependencyResult dep : deps){
            double[] from = positions.get(dep.blockingIssueId());
            double[] to = positions.get(dep.blockedIssueId());
            if (from == null || to == null) continue;

            double dx = to[0] - from[0];
            double dy = to[1] - from[1];
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1) continue;
            double ux = dx / dist;
            double uy = dy / dist;

            double x1 = from[0] + ux * NODE_RADIUS;
            double y1 = from[1] + uy * NODE_RADIUS;
            double x2 = to[0] - ux * NODE_RADIUS;
            double y2 = to[1] - uy * NODE_RADIUS;

            gc.strokeLine(x1, y1, x2, y2);

            double arrowSize = 8;
            double ax = x2 - ux * arrowSize;
            double ay = y2 - uy * arrowSize;
            gc.fillPolygon(
                    new double[]{x2, ax + uy * arrowSize * 0.4, ax - uy * arrowSize * 0.4},
                    new double[]{y2, ay - ux * arrowSize * 0.4, ay + ux * arrowSize * 0.4},
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
            gc.setFill(color);
            gc.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            gc.setStroke(color.darker());
            gc.setLineWidth(1.5);
            gc.strokeOval(x - NODE_RADIUS, y - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            gc.setFill(Color.WHITE);
            String label = issue.issueId();
            gc.fillText(label, x - label.length() * 3, y + 4);
        }
    }
}
