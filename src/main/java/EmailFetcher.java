import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Message;

import java.util.Properties;

public class EmailFetcher {
        public static void main(String[] args) {
                try {
//connection configuration
                        Properties props = new Properties();
                        props.put("mail.store.protocol","imaps");
                        props.put("mail.imaps.host", "imap.gmail.com");
                        props.put("mail.imaps.port","993");

                        // Connection details (declare these FIRST)
                        String host = "imap.gmail.com";
                        String username = "aytyyz38@gmail.com";
                        String password = "gdiagyvgbsnkrgrr";

                        // Create session
                        Session session = Session.getInstance(props);

                        // Get store and connect
                        Store store = session.getStore("imaps");


//App Password: gdiagyvgbsnkrgrr

//Open inbox
                        Folder inbox = store.getFolder("INBOX");
                        inbox.open(Folder.READ_ONLY);

//Get Messages

                        int messageCount = inbox.getMessageCount();
                        int start = messageCount - 4;
                        int end = messageCount;
                        Message[] messages = inbox.getMessages(start, end);

//print email info

                        for (int i=1; i < message.length; i++){
                                Message msg = messages[i];
                                String subject = msg.getSubject();
                                Address[] from = msg.getFrom();

                                System.out.println("Subject: " + subject);
                                System.out.println("From: " + from[0]);
                                System.out.println("---");

                        }

//sever connections

                        inbox.close(false);
                        store.close();

                } catch (Exception e) {
                        e.printStackTrace();
                }
        }












