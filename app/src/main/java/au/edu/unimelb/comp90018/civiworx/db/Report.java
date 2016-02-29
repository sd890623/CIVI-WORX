package au.edu.unimelb.comp90018.civiworx.db;

import java.util.Date;

public class Report {

    public Long id;
    public String userId, author;
    public Date reportedDate;
    public String title;
    public Double lat, lng;

    public Report(Long id, String userId, String author, Date rDate, String title, Double lat, Double lng) {
        this.id = id;
        this.userId = userId;
        this.author = author;
        this.reportedDate = rDate;
        this.title = title;
        this.lat = lat;
        this.lng = lng;
    }

}
