import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Arrays;
import jakarta.mail.*;
import org.jsoup.Jsoup;


import java.util.Properties;

public class EmailFetcher {

        static HashMap<String, Integer> vocab = new HashMap<>();


        public static void main(String[] args) {
                try {
//connection configuration
                        /* =====================================================================
                         * SECTION: IMAP CONNECTION (GMAIL)
                         * PURPOSE: Keep the app IMAP-only so any user can compile/run it.
                         * ISSUE: Gmail still requires secure auth (app password or OAuth).
                         * NOTE: IMAP is fine for a desktop app, but auth setup must be documented.
                         * ===================================================================== */
                        Properties props = new Properties();
                        props.put("mail.store.protocol", "imaps");
                        props.put("mail.imaps.host", "imap.gmail.com");
                        props.put("mail.imaps.port", "993");

                        // Connection details
                        String host = "imap.gmail.com";
                        /* =====================================================================
                         * SECTION: CREDENTIALS
                         * PURPOSE: Required for IMAP login.
                         * ISSUE: Hard-coded credentials are unsafe and will break for other users.
                         * NEXT: Read from env vars or a local config file ignored by git.
                         * ===================================================================== */
                        String username = "aytyyz38@gmail.com";
                        String password = "Censored";

                        // Create session
                        Session session = Session.getInstance(props);

                        // Get store and connect
                        Store store = session.getStore("imaps");
                        store.connect(host, username, password);

                        //App Password: gdiagyvgbsnkrgrr


                        Folder inbox = store.getFolder("INBOX");
                        inbox.open(Folder.READ_ONLY);

//Get Messages

                        int messageCount = inbox.getMessageCount();
                        /* =====================================================================
                         * SECTION: MESSAGE RANGE
                         * PURPOSE: Fetch a small window of recent messages.
                         * ISSUE: If the inbox has < 4 messages, start becomes <= 0.
                         * NEXT: Guard with Math.max(1, messageCount - 4).
                         * ===================================================================== */
                        int start = messageCount - 4;
                        int end = messageCount;
                        Message[] messages = inbox.getMessages(start, end);

//print email info

                        for (int i = 0; i < messages.length; i++) {
                                Message msg = messages[i];
                                /* =====================================================================
                                 * SECTION: LABELING STRATEGY
                                 * PURPOSE: Demonstrate labeling via IMAP.
                                 * ISSUE: Only the first message in the batch gets labeled.
                                 * NEXT: Label based on ML prediction or user rule, not index.
                                 * ===================================================================== */
                                if (i == 0) {
                                        addLabel(msg, store, "Academic");
                                        System.out.println("Added Academic label!");
                                }
                                String subject = msg.getSubject();
                                Address[] from = msg.getFrom();
                                String body = getTextFromMessage(msg);
                                String cleanbody = cleanHTML(body);
                                String[] tokens = tokenize(cleanbody);
                                /* =====================================================================
                                 * SECTION: JAVA <-> PYTHON INTEGRATION
                                 * PURPOSE: Run ML sorter to classify an email.
                                 * ISSUE: No email data is passed; output is not tied to this message.
                                 * NEXT: Pass content (stdin/args/file) and wait for process exit.
                                 * ===================================================================== */
                                String line = Process_Builder();


                                System.out.println("Subject: " + subject);
                                System.out.print("From: ");

                                int limit;
                                if (from.length > 3) {
                                        limit = 3;
                                } else {
                                        limit = from.length;
                                }


                                for (int j = 0; j < limit; j++) {
                                        System.out.print(from[j]);
                                        if (j < limit - 1) {
                                                System.out.print(", ");
                                        }
                                }


                                if (from.length > 3) {
                                        System.out.print(", ...");
                                }

                                System.out.println("\nBody: " + cleanbody);
                                System.out.println(Arrays.toString(tokens));

                                System.out.println("---");
                                System.out.println(line);
                        }


//sever connections

                        inbox.close(false);
                        store.close();

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }


        private static String getTextFromMessage(Message message) throws Exception {
                /* =====================================================================
                 * SECTION: MESSAGE BODY EXTRACTION
                 * PURPOSE: Read plain/text or HTML parts.
                 * ISSUE: Return position and null/empty handling are unclear for multipart messages.
                 * NEXT: Return after the loop; prefer empty string or exception on failure.
                 * ===================================================================== */
                Object msgContent = message.getContent();
                if (msgContent instanceof String){
                        return (String) msgContent;
                } else if (msgContent instanceof Multipart) {
                        StringBuilder result = new StringBuilder();
                        Multipart multipart = (Multipart) msgContent;
                        for (int i = 0; i < multipart.getCount(); i++) {
                                BodyPart bodyPart = multipart.getBodyPart(i);
                                String contentType = bodyPart.getContentType();
                                if (contentType.contains("text/plain") ||contentType.contains("text/html")){
                                        String content = bodyPart.getContent().toString();
                                        result.append(content);
                                        result.append("\n");
                                }
                        }

                } else {
                        System.out.println("WTF");
                }


        return "";
        }

//Labelling
        private static void addLabel(Message message, Store store, String labelName) throws Exception {
                /* =====================================================================
                 * SECTION: IMAP LABELING (GMAIL)
                 * PURPOSE: Place a message into a label folder.
                 * ISSUE: IMAP append can create a copy instead of a true Gmail label.
                 * NEXT: Investigate Gmail-specific IMAP flags or accept copy semantics.
                 * ===================================================================== */
                Folder folder = store.getFolder(labelName);
                if (!folder.exists()) {
                folder.create(Folder.HOLDS_MESSAGES);
                }

                folder.open(Folder.READ_WRITE);
                folder.appendMessages(new Message[]{message});
                folder.close(false);

        }

//"Cleaning" dirty HTML (don't audit the laundromat)


        private static String cleanHTML(String html) {
                String text = Jsoup.parse(html).text();
                return text;
        }


//Tokenization
        private static String[] tokenize(String text) {
                /* =====================================================================
                 * SECTION: TOKENIZATION
                 * PURPOSE: Normalize and split text into tokens.
                 * ISSUE: Duplicate lowercasing; token quality may include empties.
                 * NEXT: Remove duplicate call and consider stopwords/short tokens.
                 * ===================================================================== */
                text = text.toLowerCase();
                text = text.toLowerCase();
                text = text.replaceAll("[^a-zA-Z0-9\\s]", " ");
                return text.split("\\s+");
        }

//Vectorization

        private static int[] vectorization(String text) {
                /* =====================================================================
                 * SECTION: VECTORIZATION
                 * PURPOSE: Convert tokens into a bag-of-words vector.
                 * ISSUE: vocab is never populated in this class, so vectors are all zeros.
                 * NEXT: Build vocab from training data and persist it.
                 * ===================================================================== */

                String[] words = tokenize(text);
                int[] vector = new int[vocab.size()];
                for (int i = 0; i < words.length; i++) {
                        if (vocab.containsKey(words[i])){
                                int idx=vocab.get(words[i]);
                                vector[idx]++;


                        }

                }

        return vector;
        }

        private static String Process_Builder() throws IOException {
                /* =====================================================================
                 * SECTION: PYTHON PROCESS
                 * PURPOSE: Invoke ML classifier from Java.
                 * ISSUE: Relative path and no stderr/exit handling.
                 * NEXT: Set working dir, read stderr, and wait for exit.
                 * ===================================================================== */
                String python = "python3";
                ProcessBuilder pb = new ProcessBuilder(python, "src/main/RandomForestSorter.py");
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                );
                String line = reader.readLine();
                return line;
        }




}




