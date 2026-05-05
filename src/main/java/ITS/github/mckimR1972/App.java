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
    public void start(Stage stage) {
        // 1. UI 요소 생성 (배우들)
        Label label = new Label("버튼을 기다리는 중...");
        Button button = new Button("나를 눌러봐!");

        // 2. 버튼 클릭 이벤트 설정 (이벤트 핸들러)
        // 버튼이 눌렸을 때 실행될 동작을 정의합니다.
        button.setOnAction(e -> {
            // 콘솔창(터미널)에 출력
            System.out.println("콘솔: 버튼을 눌렀습니다!");
            // 화면의 라벨 글자도 변경
            label.setText("와! 버튼이 눌렸어요!");
        });

        // 3. 레이아웃 설정 (무대 장치)
        // VBox는 요소들을 위에서 아래로(Vertical) 정렬합니다.
        VBox root = new VBox(20); // 요소 간 간격 20px
        root.setAlignment(Pos.CENTER); // 중앙 정렬
        root.getChildren().addAll(label, button); // 라벨과 버튼을 담기

        // 4. 장면 및 무대 설정
        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.setTitle("JavaFX 이벤트 테스트");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}