import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Telegram {
    public static void sendMsg(String chatId, String message) {
        try {
            String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";
            String urlString = String.format(TELEGRAM_API_URL, Secrets.botToken);
            URL url = new URL(urlString);

            // Открываем соединение
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            // Формируем JSON-данные для отправки
            String jsonInputString = String.format("{\"chat_id\":\"%s\", \"text\":\"%s\"}", chatId, message);

            // Записываем JSON-данные в поток
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Проверяем ответ сервера
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Message sent successfully!");
            } else {
                System.out.println("Failed to send message. Response code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
