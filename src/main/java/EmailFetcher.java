import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

public class EmailFetcher {

        static HashMap<String, Integer> vocab = new HashMap<>();

        public static void main(String[] args) {
                if (args.length < 2) {
                        System.err.println("Usage: EmailFetcher <email> <app-password>");
                        return;
                }

                try {
                        System.out.print(fetchEmails(args[0], args[1]));
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public static String fetchEmails(String username, String password) throws Exception {
                Properties props = new Properties();
                props.put("mail.store.protocol", "imaps");
                props.put("mail.imaps.host", "imap.gmail.com");
                props.put("mail.imaps.port", "993");

                String host = "imap.gmail.com";
                Session session = Session.getInstance(props);
                StringBuilder output = new StringBuilder();

                try (Store store = session.getStore("imaps")) {
                        store.connect(host, username, password);

                        Folder inbox = store.getFolder("INBOX");
                        inbox.open(Folder.READ_ONLY);
                        try {
                                int messageCount = inbox.getMessageCount();
                                if (messageCount == 0) {
                                        return "Inbox is empty.\n";
                                }

                                int start = messageCount - 4;
                                if (start < 1) {
                                        start = 1;
                                }
                                int end = messageCount;
                                Message[] messages = inbox.getMessages(start, end);

                                for (int i = 0; i < messages.length; i++) {
                                        Message msg = messages[i];
                                        if (i == 0) {
                                                addLabel(msg, store, "Academic");
                                                output.append("Added Academic label!\n");
                                        }

                                        String subject = msg.getSubject();
                                        Address[] from = msg.getFrom();
                                        String body = getTextFromMessage(msg);
                                        String cleanBody = cleanHTML(body);
                                        String[] tokens = tokenize(cleanBody);
                                        String line = processBuilder();

                                        output.append("Subject: ")
                                                        .append(subject == null ? "(no subject)" : subject)
                                                        .append('\n');
                                        output.append("From: ").append(formatSenders(from)).append('\n');
                                        output.append("Body: ").append(cleanBody).append('\n');
                                        output.append(Arrays.toString(tokens)).append('\n');
                                        output.append("---\n");
                                        output.append(line).append('\n');
                                }
                        } finally {
                                inbox.close(false);
                        }
                }

                return output.toString();
        }

        private static String getTextFromMessage(Message message) throws Exception {
                Object msgContent = message.getContent();
                if (msgContent instanceof String) {
                        return (String) msgContent;
                } else if (msgContent instanceof Multipart) {
                        StringBuilder result = new StringBuilder();
                        Multipart multipart = (Multipart) msgContent;
                        for (int i = 0; i < multipart.getCount(); i++) {
                                BodyPart bodyPart = multipart.getBodyPart(i);
                                String contentType = bodyPart.getContentType();
                                if (contentType.contains("text/plain") || contentType.contains("text/html")) {
                                        String content = bodyPart.getContent().toString();
                                        result.append(content);
                                        result.append("\n");
                                }
                        }
                        return result.toString();
                }

                return "";
        }

        private static void addLabel(Message message, Store store, String labelName) throws Exception {
                Folder folder = store.getFolder(labelName);
                if (!folder.exists()) {
                        folder.create(Folder.HOLDS_MESSAGES);
                }

                folder.open(Folder.READ_WRITE);
                try {
                        folder.appendMessages(new Message[]{message});
                } finally {
                        folder.close(false);
                }
        }

        private static String cleanHTML(String html) {
                if (html == null || html.isBlank()) {
                        return "";
                }
                return Jsoup.parse(html).text();
        }

        private static String[] tokenize(String text) {
                if (text == null || text.isBlank()) {
                        return new String[0];
                }
                text = text.toLowerCase();
                text = text.replaceAll("[^a-zA-Z0-9\\s]", " ");
                return text.split("\\s+");
        }

        private static int[] vectorization(String text) {
                String[] words = tokenize(text);
                int[] vector = new int[vocab.size()];
                for (String word : words) {
                        if (vocab.containsKey(word)) {
                                int idx = vocab.get(word);
                                vector[idx]++;
                        }
                }

                return vector;
        }

        private static String processBuilder() throws IOException {
                File projectDir = new File(System.getProperty("user.dir"));
                File venvPython = new File(projectDir, ".venv/bin/python");
                String python = venvPython.isFile() ? venvPython.getAbsolutePath() : "python3";
                String scriptPath = new File(projectDir, "src/main/RandomForestSorter.py").getAbsolutePath();

                ProcessBuilder pb = new ProcessBuilder(python, scriptPath);
                pb.directory(projectDir);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line = reader.readLine();

                        try {
                                int exitCode = process.waitFor();
                                if (exitCode != 0) {
                                        throw new IOException("Python process exited with code " + exitCode);
                                }
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IOException("Python process interrupted", e);
                        }

                        return line == null ? "" : line;
                }
        }

        private static String formatSenders(Address[] from) {
                if (from == null || from.length == 0) {
                        return "(unknown sender)";
                }

                StringBuilder senderText = new StringBuilder();
                int limit = from.length;
                if (limit > 3) {
                        limit = 3;
                }
                for (int j = 0; j < limit; j++) {
                        senderText.append(from[j]);
                        if (j < limit - 1) {
                                senderText.append(", ");
                        }
                }

                if (from.length > 3) {
                        senderText.append(", ...");
                }

                return senderText.toString();
        }
}
