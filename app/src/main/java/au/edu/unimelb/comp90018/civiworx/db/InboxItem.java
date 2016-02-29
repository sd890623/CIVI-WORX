package au.edu.unimelb.comp90018.civiworx.db;

public class InboxItem {

    public Long reportId;
    public Long firstMessageId;
    public String title;
    public String firstMessageText;
    public String firstMessageImageB64;

    public InboxItem(Long r, Long m, String t, String msg, String img) {
        this.reportId = r;
        this.firstMessageId = m;
        this.title = t;
        this.firstMessageText = msg;
        this.firstMessageImageB64 = img;
    }

    @Override
    public String toString() {
        return this.title;
    }

}
