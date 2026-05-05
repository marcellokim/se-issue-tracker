package ITS.github.mckimR1972;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // --- 1번 메인 화면 구성 ---
        primaryStage.setTitle("ITS - 이슈 트래커 메인");

        Label label = new Label("이슈 트래커 시스템에 오신 것을 환영합니다.");
        Button btn = new Button("새로운 이슈 페이지 열기");

        // 버튼 클릭 시 이벤트 처리: 터미널 출력 대신 메서드 호출
        btn.setOnAction(event -> {
            showNewPage(); // 새로운 창을 띄우는 함수 호출
        });

        VBox root = new VBox(20); // 20은 요소 간의 간격
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(label, btn);

        Scene mainScene = new Scene(root, 400, 300);
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    /**
     * 새로운 창(Stage)을 생성하여 보여주는 메서드
     */
    private void showNewPage() {
        // 새로운 무대(Stage) 생성
        Stage secondaryStage = new Stage();
        secondaryStage.setTitle("새로운 이슈 등록 페이지");

        // 새로운 장면의 레이아웃과 배우(Node)들 배치
        Label subLabel = new Label("이곳은 이슈를 등록하는 새로운 페이지입니다.");
        Button closeBtn = new Button("이 창 닫기");

        // 닫기 버튼 기능
        closeBtn.setOnAction(e -> secondaryStage.close());

        VBox subLayout = new VBox(15);
        subLayout.setAlignment(Pos.CENTER);
        subLayout.getChildren().addAll(subLabel, closeBtn);

        // 새로운 장면(Scene) 생성
        Scene subScene = new Scene(subLayout, 300, 200);

        secondaryStage.setScene(subScene);
        secondaryStage.show(); // 창 띄우기
    }

    public static void main(String[] args) {
        launch(args);
    }
}