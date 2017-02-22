import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collections;
import java.io.*;
import java.lang.NumberFormatException;
import java.lang.ArrayIndexOutOfBoundsException;

/**
 * A little background on this project: Santa Clara has a website called
 * CourseAvail that lets students view details about classes, among which is
 * the amount of seats remaining. Back when I made this, it was not the easiest
 * to use if you wanted to keep track of all the courses you were watching, so
 * someone came along and made scuclasses.com. This website lets students pick
 * classes, see how they line up in a schedule, and watch the seats. This is a
 * great tool for students to use to keep track of classes and schedules, but
 * it didn't quite suit all of my needs. I wanted something quick and powerful
 * that I had complete control over, so I made SCUCourseTools.
 *
 * SCUCourseTools offers course viewing functionality from the command line,
 * including a seat watcher, a powerful course search, core requirement
 * planning, and course scheduling.
 *
 * @author Eric Van Lare
 */
public class SCUCourseTools {
    private ArrayList<String[]> seatWatchCourses;
    protected static Hashtable<String,String[][]> allCoursesAndTerms;
    private Hashtable<String,int[]> quarterInfo;
    private String[] quarterNames = {"S17","W17","F16","S16","W16","F15","S15","W15","F14"};
    private int[][] quarterIDs= {{3840,50000,53000},
                                {3820,46000,49000},
                                {3800,42000,45000},
                                {3740,34000,37000},
                                {3720,30000,33000},
                                {3700,26000,29000},
                                {3640,18000,21000},
                                {3620,14000,17000},
                                {3600,10000,13000}};
    protected static String[][] descriptions = {{"Course","Title","ID","Instructor","Day","Times","Core"},
            {"Course","Title","ID","Instructor","Day","Times","Seats Remaining"}};

    //change these when adding a new quarter
    private int currentQuarter;
    private String currentQuarterName;
    protected static int currentFirstCourse;

    private String lastCommand;
    private String secondLastCommand;
    private String[] lastArgs;
    private String[] secondLastArgs;

    private static int[][] spaces = {{10,34,5,22,4,11,30},{10,34,5,22,4,11,20}};

    private ArrayList<ArrayList<String>> schedules;

    private ShellDisplay display;
    private SeatWatcher seatWatcher;

    public SCUCourseTools() {
        setup();
        run();
    }

    public static void main(String[] args) {
        new SCUCourseTools();
    }

    private void setup() {

        currentQuarter = quarterIDs[0][0];
        currentQuarterName = quarterNames[0];
        currentFirstCourse = quarterIDs[0][1];

        display = new ShellDisplay();

//        seatWatchCourses = new ArrayList<String[]>();
        seatWatcher = new SeatWatcher(display);
        allCoursesAndTerms = new Hashtable<String,String[][]>();

        quarterInfo = new Hashtable<String,int[]>();
        for (int i=0; i<quarterIDs.length; i++) {
            quarterInfo.put(quarterNames[i],quarterIDs[i]);
            allCoursesAndTerms.put(""+quarterIDs[i][0],new String[3001][]);
            loadData(quarterIDs[i][0],quarterIDs[i][1]);
        }

        display.updateRowsAndCols();

        lastCommand = "";
        secondLastCommand = "";
        lastArgs = new String[0];
        secondLastArgs = new String[0];

        schedules = new ArrayList<ArrayList<String>>();

        loadSW(currentQuarter);
        help();
    }

    private void help() {
        try {
            File helpFile = new File("text/help.txt");
            if (helpFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(helpFile.getAbsoluteFile()));
                for (String line = br.readLine(); line != null; line = br.readLine())
                    display.addLine(line);
                br.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        display.display();
    }

    private void run() {
        while (true) {
            try {
                String input = display.readLine();
                String[] allLines = input.split(" ");
                String argc = allLines[0];
                String[] argv = {};
                if (allLines.length>1) {
                    argv = new String[allLines.length-1];
                    for (int i=1; i<allLines.length; i++) {
                        argv[i-1] = allLines[i];
                    }
                }
                doAction(argc,argv);
            } catch (ArrayIndexOutOfBoundsException e) {
                display.addLine("Invalid command");
                display.display();
            }
        }
    }

    private void doAction(String argc, String[] argv) {
        display.updateRowsAndCols();
        if (argc.equals("")) {
            argc = lastCommand;
            argv = lastArgs;
        }
        if (argc.equals("back") || argc.equals("b")) {
            argc = secondLastCommand;
            argv = secondLastArgs;
        }
        if (argc.equals("help") || argc.equals("h")) {
            if (argv.length > 0) {
                display.addLine("Error: too many arguments for help command");
                display.addLine("\t\t\"help\" OR \"h\"");
                display.display();
            }
            else
                help();
        }
        else if (argc.equals("quit") || argc.equals("q")) {
            updateFile(currentQuarter);
            display.quit();
            System.exit(0);
        }
        else if (argc.equals("update")) {
            if (argv.length != 1) {
                display.addLine("Error: invalid number of arguments for update command");
                display.addLine("\t\t\"update Qyy\" (e.g. \"update F15\")");
                display.display();
            }
            else {
                boolean isProper = false;
                String qName = "";
                for (String str : quarterNames)
                    if (argv[0].equals(str)) {
                        isProper = true;
                        qName = str;
                    }
                if (isProper)
                    scrapeToFile(quarterInfo.get(qName)[0],quarterInfo.get(qName)[1]);
                else {
                    display.addLine("Error: invalid argument for update command");
                    display.addLine("\t\t\"update Qyy\" (e.g. \"update F15\")");
                    display.display();
                }
            }
        }
        else if (argc.equals("seatwatcher") || argc.equals("sw")) {
            if (argv.length > 0) {
                display.addLine("Error: too many arguments for seatwatcher command");
                display.addLine("\t\t\"seatwatcher\" OR \"sw\"");
                display.display();
            }
            else
                seatWatcher();
        }
        else if (argc.equals("mycourses") || argc.equals("mc")) {
            if (argv.length > 0) {
                display.addLine("Error: too many arguments for seatwatcher command");
                display.addLine("\t\t\"seatwatcher\" OR \"sw\"");
                display.display();
            }
            else
                showMyCourses();
        }
        else if (argc.equals("schedule") || argc.equals("sch")) {
            if (argv.length < 1) {
                display.addLine("Error: too few arguments for schedule command");
                display.addLine("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
                display.addLine("Alternatively, if you have already defined a scheduleName:");
                display.addLine("\t\t\"schedule scheduleName\" OR \"sch scheduleName\"");
                display.display();
            }
            else
                schedule(argv);
        }
        else if (argc.equals("add")) {
            if (argv.length < 1) {
                display.addLine("Error: invalid number of arguments for add command");
                display.addLine("\t\t\"add xxxxx\" (e.g. \"add 27000\")");
                display.display();
            }
            for (int i=0; i<argv.length; i++) {
                if (argv[i].length() != 5) {
                    display.addLine("Error: invalid size for argument of add command");
                    display.addLine("\t\t\"add xxxxx\" (e.g. \"add 27000\")");
                    display.display();
                }
                else {
                    addToSeatWatch(Integer.parseInt(argv[i]),currentQuarter,currentFirstCourse);
                }
            }
            seatWatcher();
        }
        else if (argc.equals("remove")) {
            if (argv.length < 1) {
                display.addLine("Error: invalid number of arguments for remove command");
                display.addLine("\t\t\"remove xxxxx\" (e.g. \"remove 27000\")");
                display.display();
            }
            for (int i=0; i<argv.length; i++) {
                if (argv[0].length() != 5) {
                    display.addLine("Error: invalid size for argument of remove command");
                    display.addLine("\t\t\"remove xxxxx\" (e.g. \"remove 27000\")");
                    display.display();
                }
                else {
                    removeFromSeatWatch(Integer.parseInt(argv[i]));
                }
            }
            seatWatcher();
        }
        else if (argc.equals("search") || argc.equals("f")) {
            if (argv.length == 0) {
                display.addLine("Error: too few arguments for search command");
                display.addLine("\t\t\"search -q Qyy -d DEPT -n xxx -p Proflastname -i xxxxx\" " +
                        "(e.g. search -q F15 -d COEN -n 20 -p Lewis -i 27000)");
                display.display();
            }
            else if (argv.length%2 != 0) {
                display.addLine("Error: invalid number of arguments for search command");
                display.addLine("\t\t\"search -q Qyy -d DEPT -n xxx -p Proflastname -i xxxxx\" " +
                        "(e.g. search -q F15 -d COEN -n 20 -p Lewis -i 27000)");
                display.display();
            }
            else {
                ArrayList<String[]> allCoursesEver = new ArrayList<String[]>();
                for (String[][] termArr : (new ArrayList<String[][]>(allCoursesAndTerms.values()))) {
                    for (String[] courseArr : termArr) {
                        if (courseArr != null)
                            allCoursesEver.add(courseArr);
                    }
                }
                boolean quarterSearched = false;
                boolean doDisplay = true;
                for (int i=0; i<argv.length; i++) {
                    if (argv[i].equals("-q")) {
                        allCoursesEver = findByX("quarter", allCoursesEver, argv[i+1]);
                        i++;
                        quarterSearched = true;
                    }
                    else if (argv[i].equals("-d")) {
                        allCoursesEver = findByX("dept", allCoursesEver, argv[i+1].toUpperCase());
                        i++;
                    }
                    else if (argv[i].equals("-n")) {
                        allCoursesEver = findByX("number", allCoursesEver, argv[i+1]);
                        i++;
                    }
                    else if (argv[i].equals("-ng")) {
                        allCoursesEver = findByX("greaterThan", allCoursesEver, argv[i+1]);
                        i++;
                    }
                    else if (argv[i].equals("-nl")) {
                        allCoursesEver = findByX("lessThan", allCoursesEver, argv[i+1]);
                        i++;
                    }
                    else if (argv[i].equals("-p")) {
                        allCoursesEver = findByX("professor", allCoursesEver, argv[i+1]);
                        i++;
                    }
                    else if (argv[i].equals("-l")) {
                        try {
                            allCoursesEver = findByX("level", allCoursesEver, argv[i+1]);
                        } catch (Error e) {
                            display.addLine("Error: usage unsupported for some majors");
                            display.display();
                            doDisplay = false;
                            break;
                        }
                        i++;
                    }
                    else if (argv[i].equals("-i")) {
                        //System.out.println("here");
                        allCoursesEver = findByX("ID", allCoursesEver, argv[i+1]);
                        i++;
                    }
                    else {
                        display.addLine("Error: invalid argument(s) for search command");
                        display.addLine("\t\t\"search -q Qyy -d DEPT -n xxx -p Proflastname -i xxxxx\" " +
                                "(e.g. search -q F15 -d COEN -n 20 -p Lewis -i 27000)");
                        display.display();
                        doDisplay = false;
                        break;
                    }
                }
                if (!quarterSearched) {
                    allCoursesEver = findByX("quarter", allCoursesEver, currentQuarterName);
                }
                if (doDisplay) {
                    displayClassList(allCoursesEver);
                }
            }
        }
        else if (argc.equals("minor")) {
            if (argv.length != 1) {
                display.addLine("Need argument for command 'minor'");
                display.display();
            }
            else {
                findMinor(argv[0]);
            }
        }
        else if (argc.equals("major")) {
            if (argv.length != 1) {
                display.addLine("Need argument for command 'major'");
                display.display();
            }
            else {
                findMajor(argv[0]);
            }
        }
        else if (argc.equals("details") || argc.equals("d")) {
            displayDetails(Integer.parseInt(argv[0]));
        }
        //corewatcher commands
        else if (argc.equals("doubledip") || argc.equals("dd")) {
            findDoubleCore();
        }
        else if (argc.equals("mydoubledip") || argc.equals("mdd")) {
            findMyDoubleCore();
        }
        else {
            display.addLine("Please enter a valid command");
            display.display();
        }
        secondLastCommand = lastCommand;
        secondLastArgs = lastArgs;
        lastCommand = argc;
        lastArgs = argv;
    }

    private void schedule(String[] argv) {
        if (argv.length == 1) {
            boolean isFound = false;
            for (ArrayList<String> sch : schedules) {
                if (sch.get(0).equals(argv[0])) {
                    isFound = true;
                    //proceed to display existing schedule
                    displaySchedule(sch);
                }
            }
            if (!isFound) {
                display.addLine("Error: could not find schedule "+argv[0]+". Please try formatting command as follows:");
                display.addLine("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
                display.addLine("Alternatively, if you have already defined a scheduleName:");
                display.addLine("\t\t\"schedule scheduleName\" OR \"sch scheduleName\"");
                display.display();
            }
        }
        else {
            boolean isConflicting = false;
            String conf1 = "xxxxx";
            String conf2 = "yyyyy";
            for (int i=1; i<argv.length; i++) {
                for (int j=1; j<i; j++) {
                    if (j!=i) {
                        if (/*conflicts(argv[i],argv[j])*/false) {
                            isConflicting = true;
                            conf1 = argv[i];
                            conf2 = argv[j];
                            break;
                        }
                    }
                }
            }
            if (isConflicting) {
                display.addLine("Error: classes "+conf1+" and "+conf2+" have a time conflict. Try again using the following command:");
                display.addLine("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
                display.addLine("Alternatively, if you have already defined a scheduleName:");
                display.addLine("\t\t\"schedule scheduleName\" OR \"sch scheduleName\"");
                display.display();
            }
            else {
                ArrayList<String> newSchedule = new ArrayList<String>();
                for (String str : argv)
                    newSchedule.add(str);
                schedules.add(newSchedule);
                displaySchedule(newSchedule);
            }
        }
    }

    private void displaySchedule(ArrayList<String> schedule) {
        String[] days = {"M","T","W","R","F"};
        String[][] spots = new String[39][5];
        for (int i=0; i<39; i++)
            for (int j=0; j<5; j++)
                spots[i][j]="                       ";
        for (int i=1; i<schedule.size(); i++) {
            SCUCourse course = new SCUCourse(Integer.parseInt(schedule.get(i)),currentQuarter);
            int startH = Integer.parseInt(course.getTimes().substring(0,2));
            int startM = Integer.parseInt(course.getTimes().substring(3,5));
            int endH = Integer.parseInt(course.getTimes().substring(6,8));
            int endM = Integer.parseInt(course.getTimes().substring(9,11));
            //display.addLine(startH+":"+startM+"-"+endH+":"+endM);
            if (startH < 8) {
                startH += 12;
                endH += 12;
            }
            else if (endH <= 8) {
                endH += 12;
            }
            int startI = (startH-8)*3+startM/20;
            int endI = (endH-8)*3+endM/20;
            for (int j=startI; j<=endI; j++) {
                for(int k=0; k<5; k++) {
                    if (course.getDays().contains(days[k])) {
                        if (j==startI || j==endI) {
                            spots[j][k] = " --------------------- ";
                        }
                        else if (j-startI == 1) {
                            String jStr = " | "+course.getCourseID()+" - "+course.getCourse();
                            for (int l=0; l<10-course.getCourse().length(); l++)
                                jStr += " ";
                            jStr += "| ";
                            spots[j][k] = jStr;
                        }
                        else if (j-startI == 2) {
                            int seats = course.updateSeats();
                            spots[j][k] = " |"+seats+" seats remaining";
                            if (seats > 10)
                                spots[j][k] += " ";
                            spots[j][k] += "| ";
                        }
                        else {
                            spots[j][k] = " |                   | ";
                        }
                    }
                }
            }
        }
        display.addLine("\t         Monday                Tuesday               Wednesday               Thursday               Friday");
        for (int i=0; i<39; i++) {
            String displayLine = "\t";
            if (i%3==0) {
                if (i<12)
                    displayLine = ""+(((i/3)+7)%12+1)+"am\t";
                else
                    displayLine = ""+(((i/3)+7)%12+1)+"pm\t";
            }
            for (int j=0; j<5; j++) {
                displayLine += spots[i][j];
            }
            display.addLine(displayLine);
        }
        display.display();
    }

    private void findMinor(String dept) {
        try {
            File minorFile = new File("text/degreeReqs/"+dept+"minor.txt");
            if (minorFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(minorFile.getAbsoluteFile()));
                for (String line = br.readLine(); line != null; line = br.readLine())
                    display.addLine(line);
                br.close();
            }
            else {
                display.addLine("Unfortunately no minor was found for the department you gave.");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        display.display();
    }

    private void findMajor(String dept) {
        try {
            File minorFile = new File("text/degreeReqs/"+dept+"major.txt");
            if (minorFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(minorFile.getAbsoluteFile()));
                for (String line = br.readLine(); line != null; line = br.readLine())
                    display.addLine(line);
                br.close();
            }
            else {
                display.addLine("Unfortunately no minor was found for the department you gave.");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        display.display();
    }

    private void displayClassList(ArrayList<String[]> allCoursesEver) {
        ArrayList<String> courses = new ArrayList<>();
        for (String[] info : allCoursesEver) {
            courses.add(formatLine((new SCUCourse(info)).getInfo(),0));
        }
        Collections.sort(courses);
        display.addLine(formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String course : courses)
            display.addLine(course);
        display.display();
    }

    private ArrayList<String[]> findByX(String x, ArrayList<String[]> allCoursesEver, String arg) {
        ArrayList<String[]> returnMe = new ArrayList<String[]>();
        for (String[] info : allCoursesEver) {
            SCUCourse course = new SCUCourse(info);
            if (x.equals("quarter")) {
                if (course.getQuarter().equals(arg) || arg.equals("all"))
                    returnMe.add(info);
            }
            else if (x.equals("dept")) {
                if (course.getDept().equals(arg))
                    returnMe.add(info);
            }
            else if (x.equals("number")) {
                if (course.getNumber().equals(arg))
                    returnMe.add(info);
            }
            else if (x.equals("greaterThan")) {
                try {
                    if ((new Integer(course.getNumber())).intValue() >= (new Integer(arg)).intValue())
                        returnMe.add(info);
                } catch (NumberFormatException e) {
                    try {
                        if ((new Integer(course.getNumber().substring(0,course.getNumber().length()-1))).intValue()>=(new Integer(arg)).intValue())
                            returnMe.add(info);
                    } catch (NumberFormatException ex) {
                        throw (new Error("usage unsupported for easy majors"));
                    }
                }
            }
            else if (x.equals("lessThan")) {
                try {
                    if ((new Integer(course.getNumber())).intValue() <= (new Integer(arg)).intValue())
                        returnMe.add(info);
                } catch (NumberFormatException e) {
                    try {
                        if ((new Integer(course.getNumber().substring(0,course.getNumber().length()-1))).intValue()<=(new Integer(arg)).intValue())
                            returnMe.add(info);
                    } catch (NumberFormatException ex) {
                        throw (new Error("usage unsupported for easy majors"));
                    }
                }
            }
            else if (x.equals("professor")) {
                if (course.getInstructorL().equals(arg))
                    returnMe.add(info);
            }
            else if (x.equals("level")) {
                if (arg.equals("l")) {
                    allCoursesEver = findByX("greaterThan", allCoursesEver, "1");
                    allCoursesEver = findByX("lessThan", allCoursesEver, "99");
                }
                else if (arg.equals("u")) {
                    allCoursesEver = findByX("greaterThan", allCoursesEver, "100");
                    allCoursesEver = findByX("lessThan", allCoursesEver, "199");
                }
                else if (arg.equals("m")) {
                    allCoursesEver = findByX("greaterThan", allCoursesEver, "200");
                }
                return allCoursesEver;
            }
            else if (x.equals("ID")) {
                //System.out.println("ja");
                if ((""+course.getCourseID()).equals(arg)) {
                    System.out.println("ja");
                    returnMe.add(info);
                }
            }
            else {
                display.addLine("Something went wrong :(");
                display.display();
            }
        }
        return returnMe;
    }

    private int getTermID(int courseID) {
        for (int i=0; i<quarterIDs.length; i++) {
            if (quarterIDs[i][2]-courseID <= quarterIDs[i][2]-quarterIDs[i][1] && quarterIDs[i][2]-courseID >= 0)
                return quarterIDs[i][0];
        }
        return currentQuarter;
    }

    private int getFirstCourse(int courseID) {
        int termID = getTermID(courseID);
        for (int i=0; i<quarterIDs.length; i++) {
            if (quarterIDs[i][0] == termID)
                return quarterIDs[i][1];
        }
        return currentFirstCourse;
    }

    private void displayDetails(int courseID) {
        SCUCourse currentCourse = (new SCUCourse(allCoursesAndTerms.get(""+getTermID(courseID))[courseID-getFirstCourse(courseID)]));
        display.addLine(formatLine(currentCourse.getInfo(),0));
        String[] allCourseInfo = currentCourse.getAllInfo();
        String[] fieldDescriptions = {"Course ID\t","Term ID\t\t","Subject\t\t","Course\t\t","Title\t\t","Description\t","Core Requirements",
                "Pathway Requirements","My Core Requirements","Enrollment Info\t","Max Units\t","Min Units\t",
                "Instructor Last Name","Instructor First Name","Days\t\t","Time\t\t","Location\t","SeatsRemaining\t",
                "Term\t\t","Student Level\t","School\t\t"};
        for (int i=0; i<allCourseInfo.length; i++) {
            if (fieldDescriptions[i].equals("Description\t")) {
                display.addLine(fieldDescriptions[i]+"\t|\t\t"+allCourseInfo[i].substring(0,display.getWidth()-40));
                display.addLine(allCourseInfo[i].substring(display.getWidth()-40));
            }
            else
                display.addLine(fieldDescriptions[i]+"\t|\t\t"+allCourseInfo[i]);
        }
        display.display();
    }

    private void findDoubleCore() {
        int termID = currentQuarter;
        int firstCourse = currentFirstCourse;
        int lastCourse = currentFirstCourse+3000;
        display.addLine(formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (int i=firstCourse; i<lastCourse; i++) {
            if (allCoursesAndTerms.get(""+termID)[i-firstCourse] != null && allCoursesAndTerms.get(""+termID)[i-firstCourse][6].contains(",")) {
                display.addLine(formatLine((new SCUCourse(allCoursesAndTerms.get(""+termID)[i-firstCourse])).getInfo(),0));
            }
        }
        display.display();
    }

    private void findMyDoubleCore() {
        //Try again:
        ArrayList<String> lines = new ArrayList<String>();
        String[] coreNames1 = {"Ethics","Diversity","Social Science","RTC 2","C&I 3","RTC 3","ELSJ"};
        String[] coreNames = {"Ethics","RTC 3","ELSJ"};
        display.addLine(formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        SCUCourse course;
        int coreCount;
        for (int j=0; j<quarterIDs.length; j++) {
            int firstCourse = quarterIDs[j][1];
            int lastCourse = firstCourse+3000;
            int termID = quarterIDs[j][0];
            for (int i=firstCourse; i<lastCourse; i++) {
                coreCount=0;
                if (allCoursesAndTerms.get(""+termID)[i-firstCourse] != null) {
                    course = new SCUCourse(allCoursesAndTerms.get(""+termID)[i-firstCourse]);
                    if (course.getCoreReqs().size()>1) {
                        for (String name : coreNames) {
                            for (String coreReq : course.getCoreReqs()) {
                                if (name.equals(coreReq))
                                    coreCount++;
                            }
                        }
                        if (coreCount >= 2)
                            lines.add(formatLine(course.getInfo(),0));
                    }
                }
            }
        }
        //Sort the lines
        Collections.sort(lines);
        for (String str : lines) {
            display.addLine(str);
        }
        display.display();
    }

    private void loadSW(int termID) {
        try {
            File swCourseFile = new File("text/swcourses" + termID + ".txt");
            if (swCourseFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(swCourseFile.getAbsoluteFile()));
                String line = br.readLine();
                while (line != null) {
                    try {
                        addToSeatWatch(Integer.parseInt(line.substring(0,5)),termID,currentFirstCourse,
                                line.substring(6,line.length()));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    line = br.readLine();
                }
                br.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void updateFile(int termID) {
        ArrayList<String> seatsRemaining = new ArrayList<String>();
        ArrayList<String> courseIDs = new ArrayList<String>();
        for (String[] arr : seatWatchCourses) {
            seatsRemaining.add(arr[17]);
            courseIDs.add(arr[0].substring(1,6));
        }
        try {
            File swCourseFile = new File("text/swcourses" + termID + ".txt");
            String writeMe = "";
            if (!swCourseFile.exists()) {
                swCourseFile.createNewFile();
            }
            FileWriter fw = new FileWriter(swCourseFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            for (int i=0; i<courseIDs.size(); i++) {
                writeMe += courseIDs.get(i)+"*"+seatsRemaining.get(i);
                if (i<courseIDs.size()-1)
                    writeMe += "\n";
            }
            bw.write(writeMe);
            bw.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public String seatWatcher() {
        String returnMe = "";
        display.loading(3);
        //Insertion sort
        for (int i=0; i<seatWatchCourses.size(); i++) {
            String[] temp = seatWatchCourses.get(i);
            int j;
            for (j=i-1; j>=0 && temp[3].compareTo(seatWatchCourses.get(j)[3])<0; j--)
                seatWatchCourses.set(j+1,seatWatchCourses.get(j));
            seatWatchCourses.set(j+1,temp);
        }
        display.addLine(formatLine(descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+course.updateSeats();
            String addMe = formatLine(course.getSWInfo(),1);
            display.addLine(addMe);
            returnMe += addMe;
        }
        display.display();
        return returnMe;
    }

    public String showMyCourses() {
        String returnMe = "";
        display.loading(3);
        //Insertion sort
        for (int i=0; i<seatWatchCourses.size(); i++) {
            String[] temp = seatWatchCourses.get(i);
            int j;
            for (j=i-1; j>=0 && temp[3].compareTo(seatWatchCourses.get(j)[3])<0; j--)
                seatWatchCourses.set(j+1,seatWatchCourses.get(j));
            seatWatchCourses.set(j+1,temp);
        }
        display.addLine(formatLine(descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+course.getSeatsRemaining();
            String addMe = formatLine(course.getSWInfo(),1);
            display.addLine(addMe);
            returnMe += addMe;
        }
        display.display();
        return returnMe;
    }

    private void removeFromSeatWatch(int courseID) {
        for (int i=0; i<seatWatchCourses.size(); i++) {
            if (seatWatchCourses.get(i)[0].substring(1,6).equals(""+courseID)) {
                seatWatchCourses.remove(i);
            }
        }
    }

    private void addToSeatWatch(int courseID , int termID, int firstCourse, String seats) {
        boolean doesExist = false;
        for (int i=0; i<seatWatchCourses.size(); i++) {
            //System.out.println(""+courseID+" "+seatWatchCourses.get(i)[0].substring(1,6));
            if (seatWatchCourses.get(i)[0].substring(1,6).equals(""+courseID)) {
                doesExist = true;
            }
        }
        //display.loading(3);
        if (!doesExist) {
            if (seats != "~~~")
                allCoursesAndTerms.get(""+termID)[courseID-firstCourse][17] = seats;
            String[] info = allCoursesAndTerms.get(""+termID)[courseID-firstCourse];
            boolean isRunning = true;
            seatWatchCourses.add(info);
            //seatWatcher();
        }
    }

    private void addToSeatWatch(int courseID , int termID, int firstCourse) {
        addToSeatWatch(courseID, termID, firstCourse, "~~~");
    }

    /* * * * * * * * * * * * * * * * FILE IO FUNCTIONS * * * * * * * * * * * * * * * */

    private void scrapeToFile(int termID, int firstCourse) {
        int lastCourse = firstCourse+3000;
        try {
            File courseInfo = new File("text/courseInfo" + termID + ".txt");
            if (!courseInfo.exists()) {
                courseInfo.createNewFile();
            }
            FileWriter fw = new FileWriter(courseInfo.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(configData(termID, firstCourse, lastCourse));
            bw.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private String configData(int termID, int firstCourse, int lastCourse) {
        ArrayList<String> displayMes = new ArrayList<String>();
        String returnMe = "";
        for (int i=firstCourse; i<lastCourse+1; i++) { //27510
            if (i%100 == 0) {
                displayMes.add("Done through: " + i);
                for (String displayer : displayMes) {
                    display.addLine(displayer);
                }
                display.display();
            }
            SCUCourse course = new SCUCourse(i,termID);
            if (course.getCoreReqs().size()>=0 && course.doesExist()) {
                String[] info = course.getAllInfo();
                if (info[4] != null) {
                    returnMe += "{";
                    for (int j=0; j<info.length-1; j++) {
                        returnMe += info[j]+"~";
                    }
                    returnMe += info[info.length-1]+"}\n";
                }
            }
        }
        return returnMe;
    }

    private void loadData(int termID, int firstCourse) {
        try {
            File courseInfo = new File("text/courseInfo" + termID + ".txt");
            if (courseInfo.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(courseInfo.getAbsoluteFile()));
                int i = 0;
                String[] lineArr;
                for (String line = br.readLine(); line != null; i++){
                    lineArr = line.split("~");
                    if (lineArr.length>0) {
                        allCoursesAndTerms.get(""+termID)[Integer.parseInt(line.substring(1,6))-firstCourse] = lineArr;
                    }
                    line = br.readLine();
                }
                br.close();
            }
            else {
                display.addLine("No data for term "+termID+" found.");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static String formatLine(String[] info, int format) {
        String returnMe = " ";
        for (int i=0; i<info.length-1; i++) {
            if (info[i] == null || info[i].equals("null"))
                info[i] = "---";
            returnMe += info[i];
            for (int diff = 0; diff < spaces[format][i]-(info[i].length()); diff++)
                returnMe += " ";
            returnMe += " | ";
        }
        if (info[info.length-1] == null || info[info.length-1].equals("null"))
            info[info.length-1] = "---";
        returnMe += info[info.length-1];
        return returnMe;
    }
}
