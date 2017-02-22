import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.ArrayList;
import java.lang.Integer;

/**
 * A full description of a course at Santa Clara University, with information
 * scraped from their CourseAvail website.
 *
 * @author Eric Van Lare
 */
public class SCUCourse implements Comparable<SCUCourse> {

    // Field definition for SCUCourse object
    private URL url;
    private int courseID;
    private int termID;
    private String subject;
    private String course;
    private String title;
    private String description;
    private ArrayList<String> coreReqs;
    private ArrayList<String> pathwayReqs;
    private ArrayList<String> myCoreReqs;
    private String enrollmentInfo;
    private int maxUnits;
    private int minUnits;
    private String instructorL;
    private String instructorF;
    private String days;
    private String times;
    private String location;
    private int seatsRemaining;
    private String term;
    private String studentLevel;
    private String school;
    private boolean doesExist;
    private String downFrom;

    // Arrays of identifiers and their names, used arrays instead of hashtable for compactability
    private String[] foundations        = {"F_CTW1","F_CTW2","F_CI1","F_CI2","F_SLA1","F_SLA2","F_MATH","F_RTC1"};
    private String[] foundationsNames   = {"CTW 1","CTW 2","C&I 1","C&I 2","Second Language 1","Second Language 2",
                                            "Math","RTC 1"};
    private String[] explorations       = {"E_ETH","E_CE","E_DV","E_ARTS","E_SOSC","E_NTSC","E_RTC2","E_CI3","E_STS",
                                            "E_RTC3"};
    private String[] explorationsNames  = {"Ethics","Civil Engagement","Diversity","Arts","Social Science",
                                            "Natural Science","RTC 2","C&I 3","Science Technology Society","RTC 3",
                                            "ELSJ"};
    private String[] integrations       = {"I_AW","I_EL"};
    private String[] integrationsNames  = {"Advanced Writing","ELSJ"};
    private String[] pathways           = {"I_PTHAE","I_PTHAMS","I_PTHB","I_PTHCHD","I_PTHCINST","I_PTHDEM","I_PTHDA",
                                            "I_PTHDT","I_PTHFHP","I_PTHGSB","I_PTHGH","I_PTHHR","I_PTHIS","I_PTHJA",
                                            "I_PTHLSJ","I_PTHLPOSC","I_PTHPR","I_PTHPP","I_PTHPS","I_PTHRPSI","I_PTHS",
                                            "I_PTHVST","I_PTHV"};
    private String[] pathwaysNames      = {"Applied Ethics","American Studies","Beauty","Childhood, Family & Society",
                                            "Cinema Studies","Democracy","The Digital Age","Design Thinking",
                                            "Food, Hunger, Poverty Environment","Gender, Sexuality & the Body",
                                            "Global Health","Human Rights","Islamic Studies","Justice & the Arts",
                                            "Law & Social Justice","Leading People, Organizations & Social Change",
                                            "Politics & Religion","Public Policy","Paradigm Shifts",
                                            "Race Place & Social Inequities","Sustainability",
                                            "Values in Science & Technology","Vocation"};
    private String[] myCores            = {"E_ETH","E_DV","E_RTC2","E_RTC3","I_EL"};
    private String[] coreNames          = {"Ethics","Diversity","RTC 2","RTC 3","ELSJ"};

    /**
     * Construct an SCUCourse object from the course and term IDs, using
     * CourseAvail to find the rest of the info on the course.
     *
     * @param courseID
     * @param termID
     */
    public SCUCourse(int courseID, int termID) {
        this.courseID = courseID;
        this.termID = termID;
        coreReqs = new ArrayList<String>();
        myCoreReqs = new ArrayList<String>();
        pathwayReqs = new ArrayList<String>();
        update();
    }

    /**
     * Construct an SCUCourse object from an array containing all the
     * information about the class. Doesn't update any info from courseavail
     * in the constructor itself.
     *
     * @param info
     */
    public SCUCourse(String[] info) {
        String[] returnMe = {""+courseID,""+termID,subject,course,title,description,""+coreReqs,""+pathwayReqs,
                             ""+myCoreReqs,enrollmentInfo,""+maxUnits,""+minUnits,instructorL,instructorF,days,times,
                             location,""+seatsRemaining,term,studentLevel,school};
        courseID=Integer.parseInt(info[0].substring(1,6));
        termID=Integer.parseInt(info[1]);
        subject=info[2];
        course=info[3];
        title=info[4];
        description=info[5];
        coreReqs = new ArrayList<String>();
        myCoreReqs = new ArrayList<String>();
        pathwayReqs = new ArrayList<String>();

        try {
            String coreReqsString = info[6];
            String[] coreReqsArr = coreReqsString.substring(1,coreReqsString.length()-1).split(", ");
            for (String str : coreReqsArr) {
                if (str.length()>0) {
                    coreReqs.add(str);
                }
            }
        } catch (NullPointerException e) {}
        try {
            String pathwayReqsString = info[7];
            String[] pathwayReqsArr = pathwayReqsString.substring(1,pathwayReqsString.length()-1).split(", ");
            for (String str : pathwayReqsArr) {
                if (str.length()>0)
                    pathwayReqs.add(str);
            }
        } catch (NullPointerException e) {}
        try {
            String myCoreReqsString = info[8];
            String[] myCoreReqsArr = myCoreReqsString.substring(1,myCoreReqsString.length()-1).split(", ");
            for (String str : myCoreReqsArr) {
                if (str.length()>0)
                    myCoreReqs.add(str);
            }
        } catch (NullPointerException e) {}

        enrollmentInfo=info[9];
        maxUnits=Integer.parseInt(info[10]);
        minUnits=Integer.parseInt(info[11]);
        instructorL=info[12];
        instructorF=info[13];
        days=info[14];
        times=info[15];
        location=info[16];
        seatsRemaining=Integer.parseInt(info[17]);
        term=info[18];
        studentLevel=info[19];
        school=info[20];
        doesExist=true;
        myCoreFromCore();
        downFrom="";
    }

    /**
     * Check how stressed I should be about whether or not I'll be able to get
     * into a class based on how many seats are left. >=30 is green for in the
     * clear, 30>x>=15 is yellow for starting to have to worry, 15>x>=1 is red
     * for probably not going to get the class unless I'm lucky or register
     * very soon, and 0 is blue for the class being filled.
     *
     * @param seats number of seats remaining in this class
     * @return A String containing the number of seats remaining, appropriately
     *         colored based on how screwed I am if I wanna take the class.
     */
    public String checkDangerZone(int seats) {
        String ANSI_RESET = "\u001B[0m";
        String ANSI_RED = "\u001B[31m";
        String ANSI_GREEN = "\u001B[32m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_BLUE = "\u001B[34m";

        String seatColor = "";
        if (seats >= 30)
            seatColor += ANSI_GREEN;
        else if (seats >= 15)
            seatColor += ANSI_YELLOW;
        else if (seats >= 1)
            seatColor += ANSI_RED;
        else
            seatColor += ANSI_BLUE;
        return seatColor + seats + ANSI_RESET;
    }

    /**
     * Compare this SCUCourse to another SCUCourse based on the course
     * identifier (e.g. COEN 12).
     *
     * @param comparingCourse SCUCourse to be compared to this one
     * @return 0 if equal, negative if argument is greater, positive if this is
     *         greater.
     */
    @Override
    public int compareTo(SCUCourse comparingCourse) {
        return this.course.compareTo(comparingCourse.getCourse());
    }

    /**
     * Determine if there is actually a course corresponding to the course and
     * term IDs given when constructed.
     *
     * @return true if course exists
     */
    public boolean doesExist() {
        return doesExist;
    }

    /**
     * Determine if this course satisfies any core requirements I still need to
     * take.
     */
    private void myCoreFromCore() {
        for (String schoolCore : coreReqs)
            for (String myCoreName : coreNames)
                if (myCoreName.equals(schoolCore))
                    myCoreReqs.add(schoolCore);
    }

    /**
     * Get a String representation of the course including the course ID,
     * title, course number, instructor, date and time, and core requirements
     * satisfied.
     *
     * @return A String representation of the course
     */
    @Override
    public String toString() {
        return course+" | "+title+" | "+courseID+" | "+instructorL+" | "+days+" | "+times+" | "+coreReqs;
    }

    /**
     * Scrape all the information about the course from CourseAvail and save it
     * in the fields defined in this class.
     */
    public void update() {
        try {
            URL url  = new URL("https://legacy.scu.edu/courseavail/class/?fuseaction=details&class_nbr="+courseID+
                    "&term="+termID);
            URLConnection uc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
            String inputLine;
            int lineCounter = 1;
            boolean isFound = false;
            while ((inputLine = in.readLine()) != null && !isFound) {
                if (lineCounter == 103) {
                    if (inputLine.contains("text/javascript")) {
                        isFound = true;
                        doesExist = false;
                    }
                    else {
                        try {
                            seatsRemaining = Integer.parseInt(inputLine.substring(inputLine.indexOf("3>")+2,
                                    inputLine.indexOf("seat")-1));
                        } catch (StringIndexOutOfBoundsException e) {
                            seatsRemaining = 0;
                        }
                        inputLine = in.readLine();
                        inputLine = in.readLine();
                        term = inputLine.substring(inputLine.indexOf("4>")+2,inputLine.indexOf(":"));
                        studentLevel = inputLine.substring(inputLine.indexOf(":")+2,inputLine.indexOf(","));
                        school = inputLine.substring(inputLine.indexOf(",")+2,inputLine.indexOf("</"));
                        lineCounter+=2;
                        doesExist = true;
                    }
                }
                else if (lineCounter == 111) {
                    subject = inputLine.substring(inputLine.indexOf(">")+1,inputLine.indexOf("</"));
                }
                else if (lineCounter == 117) {
                    int starti = inputLine.indexOf("\">")+2;
                    int endi = inputLine.indexOf("</");
                    course = inputLine.substring(starti,endi);
                }
                else if (lineCounter == 123) {
                    int starti = inputLine.indexOf(">")+1;
                    int endi = inputLine.indexOf("</");
                    title = inputLine.substring(starti,endi);
                }
                else if (lineCounter == 130) {
                    description = inputLine.substring(inputLine.indexOf("v>")+2,inputLine.indexOf("</"));
                }
                else if (inputLine.contains("Core Attribute Keys")) {
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    lineCounter+=2;
                    for (int i=0; i<foundations.length; i++) {
                        if (inputLine.contains(foundations[i]))
                            coreReqs.add(foundationsNames[i]);
                    }
                    for (int i=0; i<explorations.length; i++) {
                        if (inputLine.contains(explorations[i]))
                            coreReqs.add(explorationsNames[i]);
                    }
                    for (int i=0; i<integrations.length; i++) {
                        if (inputLine.contains(integrations[i]))
                            coreReqs.add(integrationsNames[i]);
                    }
                    for (int i=0; i<pathways.length; i++) {
                        if (inputLine.contains(pathways[i]))
                            pathwayReqs.add(pathwaysNames[i]);
                    }
                    for (String core : coreReqs) {
                        for (String myCore : myCores)
                            if (core.equals(myCore))
                                myCoreReqs.add(core);
                    }
                }
                else if (inputLine.contains("Enroll")) {
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    lineCounter+=2;
                    enrollmentInfo = inputLine.substring(inputLine.indexOf("d>")+2,inputLine.indexOf("</"));
                }
                else if (inputLine.contains("Units")) {
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    lineCounter+=2;
                    maxUnits = Integer.parseInt(inputLine.substring(inputLine.indexOf("d>")+2,inputLine.indexOf("/")));
                    minUnits = Integer.parseInt(inputLine.substring(inputLine.indexOf("/")+1,inputLine.indexOf("</")));
                }
                else if (inputLine.contains("Books")) {
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    lineCounter+=4;

                    int starti = inputLine.indexOf("<td>")+4;
                    int endi = inputLine.indexOf(",");
                    if (endi == -1)
                        endi = inputLine.indexOf("</");
                    instructorL = inputLine.substring(starti,endi);
                    if (endi != inputLine.indexOf("</"))
                        instructorF = inputLine.substring(endi,inputLine.indexOf("</"));

                    inputLine = in.readLine();
                    lineCounter++;
                    days = inputLine.substring(inputLine.indexOf("<td>")+4,inputLine.indexOf("<br"));

                    inputLine = in.readLine();
                    lineCounter++;
                    starti = inputLine.indexOf("<td>")+4;
                    endi = inputLine.indexOf("<br");
                    times = inputLine.substring(starti,endi);
                    times = times.substring(0,5)+times.substring(8,14);

                    inputLine = in.readLine();
                    inputLine = in.readLine();
                    lineCounter+=2;
                    starti=inputLine.indexOf("d>")+2;
                    if (inputLine.contains("map"))
                        starti=inputLine.indexOf("\">")+2;
                    location = inputLine.substring(starti,inputLine.indexOf("</"));
                }
                lineCounter++;
            }
            in.close();
        }
        catch (NumberFormatException|StringIndexOutOfBoundsException|IOException e) {}
        myCoreFromCore();
    }

    /**
     * Just update the seats remaining instead of scraping for all the
     * information again.
     *
     * @return number of seats remaining
     */
    public int updateSeats() {
        try {
            downFrom = "";
            URL url  = new URL("https://legacy.scu.edu/courseavail/class/?fuseaction=details&class_nbr="+courseID+
                    "&term="+termID);
            URLConnection uc = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
            String inputLine;
            int lineCounter = 1;
            int seatsRemainingOld = seatsRemaining;
            boolean isFound = false;
            while ((inputLine = in.readLine()) != null && !isFound) {
                if (lineCounter == 103) {
                    if (!inputLine.contains("text/javascript")) {
                        try {
                            seatsRemaining = Integer.parseInt(inputLine.substring(inputLine.indexOf("3>")+2,
                                    inputLine.indexOf("seat")-1));
                            if (seatsRemaining<seatsRemainingOld)
                                downFrom = " down from " + checkDangerZone(seatsRemainingOld);
                            else if (seatsRemaining>seatsRemainingOld)
                                downFrom = " up from " + checkDangerZone(seatsRemainingOld);
                        } catch (StringIndexOutOfBoundsException e) {
                            seatsRemaining = 0;
                            if (seatsRemaining<seatsRemainingOld)
                                downFrom = " down from " + checkDangerZone(seatsRemainingOld);
                            else if (seatsRemaining>seatsRemainingOld)
                                downFrom = " up from " + checkDangerZone(seatsRemainingOld);
                        }
                        isFound = true;
                    }
                }
                lineCounter++;
            }
            in.close();
        }
        catch (NumberFormatException|StringIndexOutOfBoundsException|IOException e) {}
        return seatsRemaining;
    }

    // Getters
    public int getCourseID() {return courseID;}
    public int getTermID() {return termID;}
    public String getSubject() {return subject;}
    public String getCourse() {return course;}
    public String getTitle() {return title;}
    public String getDescription() {return description;}
    public ArrayList<String> getCoreReqs() {return coreReqs;}
    public ArrayList<String> getPathwayReqs() {return pathwayReqs;}
    public ArrayList<String> getMyCoreReqs() {return myCoreReqs;}
    public String getEnrollmentInfo() {return enrollmentInfo;}
    public int getMaxUnits() {return maxUnits;}
    public int getMinUnits() {return minUnits;}
    public String getInstructorL() {return instructorL;}
    public String getInstructorF() {return instructorF;}
    public String getDays() {return days;}
    public String getTimes() {return times;}
    public String getLocation() {return location;}
    public int getSeatsRemaining() {return seatsRemaining;}
    public String getTerm() {return term;}
    public String getStudentLevel() {return studentLevel;}
    public String getSchool() {return school;}
    public String getDept() {return course.substring(0,4);}
    public String getNumber() {return course.substring(5,course.length());}
    public String getQuarter() {return term.substring(0,1)+term.substring(term.length()-2,term.length());}
    public String getDownFrom() {return downFrom;}

    /**
     * Get basic info about the course, including course, title, course number,
     * instructor, date and time, and core requirements satisfied by the
     * course.
     *
     * @return basic course info
     */
    public String[] getInfo() {
        String[] info = {course,title,""+courseID,instructorL,days,times,""+coreReqs};
        return info;
    }

    /**
     * Get the info from all the fields of the SCUCourse in an array. This
     * array can be used to construct an SCUCourse using the second constructor
     * as well.
     *
     * @return an array representation of the course
     */
    public String[] getAllInfo() {
        String[] info = {""+courseID,""+termID,subject,course,title,description,""+coreReqs,""+pathwayReqs,
                ""+myCoreReqs,enrollmentInfo,""+maxUnits,""+minUnits,instructorL,instructorF,days,times,location,
                ""+seatsRemaining,term,studentLevel,school};
        return info;
    }

    /**
     * Get only the info needed for the SeatWatcher: course, title, course
     * number, instructor, date and time, and seats remaining.
     *
     * @return info needed for the SeatWatcher
     */
    public String[] getSWInfo() {
        String[] info = {course,title,""+courseID,instructorL,days,times,""+checkDangerZone(seatsRemaining)+downFrom};
        return info;
    }

    // Setters
    public void setCourseID(int courseID) {this.courseID = courseID;}
    public void setTermID(int termID) {this.termID = termID;}
    public void setSubject(String subject) {this.subject = subject;}
    public void setCourse(String course) {this.course = course;}
    public void setTitle(String title) {this.title = title;}
    public void setDescription(String description) {this.description = description;}
    public void setCoreReqs(ArrayList<String> coreReqs) {this.coreReqs = coreReqs;}
    public void setPathwayReqs(ArrayList<String> pathwayReqs) {this.pathwayReqs = pathwayReqs;}
    public void setMyCoreReqs(ArrayList<String> myCoreReqs) {this.myCoreReqs = myCoreReqs;}
    public void setEnrollmentInfo(String enrollmentInfo) {this.enrollmentInfo = enrollmentInfo;}
    public void setMaxUnits(int maxUnits) {this.maxUnits = maxUnits;}
    public void setMinUnits(int minUnits) {this.minUnits = minUnits;}
    public void setInstructorL(String instructorL) {this.instructorL = instructorL;}
    public void setInstructorF(String instructorF) {this.instructorF = instructorF;}
    public void setDays(String days) {this.days = days;}
    public void setTimes(String times) {this.times = times;}
    public void setLocation(String location) {this.location = location;}
    public void setSeatsRemaining(int seatsRemaining) {this.seatsRemaining = seatsRemaining;}
    public void setTerm(String term) {this.term = term;}
    public void setStudentLevel(String studentLevel) {this.studentLevel = studentLevel;}
    public void setSchool(String school) {this.school = school;}
}
