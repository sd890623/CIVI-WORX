package au.edu.unimelb.comp90018.civiworx.db;

import java.util.Date;

public class Message {

    public Long id;
    public Long report;
    public String userId, author;
    public Date postedDate;
    public Long replyTo;
    public String messageText;
    public String b64Image;

    public Message(Long id, Long report, String userId, String author, Date pDate,
                   Long replyTo, String msg, String b64) {
        this.id = id;
        this.report = report;
        this.userId = userId;
        this.author = author;
        this.postedDate = pDate;
        this.replyTo = replyTo;
        this.messageText = msg;
        this.b64Image = b64;
    }

    @Override
    public String toString() {
        return messageText;
    }

}
