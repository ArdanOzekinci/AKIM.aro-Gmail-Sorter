import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GmailSorterApp extends Application {

        @Override
        public void start(Stage stage) {
                Label emailLabel = new Label("Gmail address");
                TextField emailField = new TextField();
                emailField.setPromptText("you@gmail.com");

                Label passwordLabel = new Label("Gmail app password");
                PasswordField passwordField = new PasswordField();
                passwordField.setPromptText("16-character app password");

                Button runButton = new Button("Fetch Emails");

                Label statusLabel = new Label("Enter your Gmail address and app password.");
                TextArea outputArea = new TextArea();
                outputArea.setEditable(false);
                outputArea.setWrapText(true);
                outputArea.setPrefRowCount(18);

                runButton.setOnAction(event -> {
                        String email = emailField.getText().trim();
                        String password = passwordField.getText();

                        if (email.isEmpty() || password.isEmpty()) {
                                statusLabel.setText("Both fields are required.");
                                return;
                        }

                        runButton.setDisable(true);
                        statusLabel.setText("Connecting to Gmail...");
                        outputArea.clear();

                        Task<String> fetchTask = new Task<>() {
                                @Override
                                protected String call() throws Exception {
                                        return EmailFetcher.fetchEmails(email, password);
                                }
                        };

                        fetchTask.setOnSucceeded(taskEvent -> {
                                outputArea.setText(fetchTask.getValue());
                                statusLabel.setText("Fetch complete.");
                                runButton.setDisable(false);
                        });

                        fetchTask.setOnFailed(taskEvent -> {
                                Throwable error = fetchTask.getException();
                                String message = error == null ? "Unknown error." : error.getMessage();
                                outputArea.setText(message);
                                statusLabel.setText("Fetch failed.");
                                runButton.setDisable(false);
                        });

                        Thread worker = new Thread(fetchTask, "gmail-fetcher");
                        worker.setDaemon(true);
                        worker.start();
                });

                VBox root = new VBox(10, emailLabel, emailField, passwordLabel, passwordField, runButton, statusLabel, outputArea);
                root.setPadding(new Insets(16));

                Scene scene = new Scene(root, 620, 520);
                stage.setTitle("AKIM Gmail Sorter");
                stage.setScene(scene);
                stage.show();
        }

        public static void main(String[] args) {
                launch(args);
        }
}
