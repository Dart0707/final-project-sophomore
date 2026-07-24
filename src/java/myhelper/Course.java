package myhelper;
 
/**
 * Model class representing a seminar / course.
 */
public class Course {
 
    private int    id;
    private String title;
    private String courseDate;      // stored as String for simple display (yyyy-MM-dd)
    private int    instructorId;
    private String instructorName;  // populated for Student view
    private int    participantCount; // populated for Instructor view
    private boolean enrolled;       // populated for Student view
    private String status;          // ACTIVE or INACTIVE
 
    public Course() {}
 
    // ── Getters ──────────────────────────────────────────────────────────────────
 
    public int     getId()               { return id; }
    public String  getTitle()            { return title; }
    public String  getCourseDate()       { return courseDate; }
    public int     getInstructorId()     { return instructorId; }
    public String  getInstructorName()   { return instructorName; }
    public int     getParticipantCount() { return participantCount; }
    public boolean isEnrolled()          { return enrolled; }
    public String  getStatus()           { return status; }
 
    // ── Setters ──────────────────────────────────────────────────────────────────
 
    public void setId(int id)                         { this.id = id; }
    public void setTitle(String title)                { this.title = title; }
    public void setCourseDate(String courseDate)      { this.courseDate = courseDate; }
    public void setInstructorId(int instructorId)     { this.instructorId = instructorId; }
    public void setInstructorName(String name)        { this.instructorName = name; }
    public void setParticipantCount(int count)        { this.participantCount = count; }
    public void setEnrolled(boolean enrolled)         { this.enrolled = enrolled; }
    public void setStatus(String status)              { this.status = status; }
 
    @Override
    public String toString() {
        return "Course{id=" + id + ", title='" + title + "', date='" + courseDate + "'}";
    }
}