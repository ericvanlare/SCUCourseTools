import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Collections;
import java.io.*;
import java.lang.NumberFormatException;
import java.lang.ArrayIndexOutOfBoundsException;
import java.math.*;

/**
 * Created by eric on 4/24/15.
 */
public class SCUCourseTools {
    private static final char ESC = 27;
    private static int WINDOW_SPACES = 52;
    private static int WINDOW_COLS = 209;
    private static ArrayList<String> displayMe;
    private static String input;
    private static Scanner scan;
    private static ArrayList<String[]> seatWatchCourses;
    private static Hashtable<String,String[][]> allCoursesAndTerms;
    private static Hashtable<String,int[]> quarterInfo;
    private static String[] quarterNames = {"F16","S16","W16","F15","S15","W15","F14"};
    private static int[][] quarterIDs= {{3800,42000,45000},
                                        {3740,34000,37000},
                                        {3720,30000,33000},
                                        {3700,26000,29000},
                                        {3640,18000,21000},
                                        {3620,14000,17000},
                                        {3600,10000,13000}};
    private static String[][] descriptions = {{"Course","Title","ID","Instructor","Day","Times","Core"},
                                              {"Course","Title","ID","Instructor","Day","Times","Seats Remaining"}};
    private static int[][] spaces = {{10,34,5,22,4,11,30},{10,34,5,22,4,11,20}};

    private static ArrayList<ArrayList<String>> pages;
    private static int page;
    
    //change these when adding a new quarter
    private static int currentQuarter;
    private static String currentQuarterName;
    private static int currentFirstCourse;

    private static String lastCommand;
    private static String secondLastCommand;
    private static String[] lastArgs;
    private static String[] secondLastArgs;
    private static ArrayList<String> helpStrs;
    private static Console c;

    private static ArrayList<ArrayList<String>> schedules;

    public static void main(String[] args) {
        setup();
        run();
    }

    private static void setup() {
        currentQuarter = quarterIDs[0][0];
        currentQuarterName = quarterNames[0];
        currentFirstCourse = quarterIDs[0][1];

        c = System.console();
        //c.writer().print(ESC + "[2J");
        //c.flush();

        scan = new Scanner(System.in);
        displayMe = new ArrayList<String>();
        seatWatchCourses = new ArrayList<String[]>();
        allCoursesAndTerms = new Hashtable<String,String[][]>();

        pages = new ArrayList<ArrayList<String>>();
        page = 0;

        quarterInfo = new Hashtable<String,int[]>();
        for (int i=0; i<quarterIDs.length; i++) {
            quarterInfo.put(quarterNames[i],quarterIDs[i]);
            allCoursesAndTerms.put(""+quarterIDs[i][0],new String[3001][]);
            loadData(quarterIDs[i][0],quarterIDs[i][1]);
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            getLinesAndCols();
        }

        lastCommand = "";
        secondLastCommand = "";
        lastArgs = new String[0];
        secondLastArgs = new String[0];

        schedules = new ArrayList<ArrayList<String>>();

        loadSW(currentQuarter);
        helpStrs = new ArrayList<String>();
        help();
    }

    private static void getLinesAndCols() {
        String s;
        String a="";
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"bash", "-c", "tput lines 2> /dev/tty" });
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = stdInput.readLine()) != null) {
                a+=s;
            }
            WINDOW_SPACES = (new Integer(a)).intValue()-1;
        } catch (IOException e) {}
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"bash", "-c", "tput cols 2> /dev/tty" });
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = stdInput.readLine()) != null) {
                a+=s;
            }
            a=a.substring(2,5);
            WINDOW_COLS = (new Integer(a)).intValue()-1;
        } catch (IOException e) {}
    }

    private static void help() {
        try {
            File helpFile = new File("docs/help.txt");
            if (helpFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(helpFile.getAbsoluteFile()));
                int i = 0;
                for (String line = br.readLine(); line != null; line = br.readLine()){
                    displayMe.add(line);
                    helpStrs.add(line);
                }
                br.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        display();
    }

    private static void run() {
        while (true) {
            try {
                input = c.readLine();//scan.nextLine();
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
                displayMe.add("Invalid command");
                display();
            }
        }
    }

    private static void doAction(String argc, String[] argv) {
        getLinesAndCols();
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
                displayMe.add("Error: too many arguments for help command");
                displayMe.add("\t\t\"help\" OR \"h\"");
                display();
            }
            else 
                help();
        }
        else if (argc.equals("quit") || argc.equals("q")) {
            updateFile(currentQuarter);
            clearEverything();
            c.writer().println(ESC+"[1;1H");
            c.flush();
            System.exit(0);
        }
        //seatwatcher commands
        else if (argc.equals("tentative") || argc.equals("t")) {
            if (argv.length != 1) {
                displayMe.add("Error: invalid number of arguments for tentative command");
                displayMe.add("\t\t\"help\" OR \"h\"");
                display();
            }
            else {
                getTentative((new Integer(argv[0])).intValue());
            }
        }
        else if (argc.equals("me")) {
            meTask();
        }
        else if (argc.equals("update")) {
            if (argv.length != 1) {
                displayMe.add("Error: invalid number of arguments for update command");
                displayMe.add("\t\t\"update Qyy\" (e.g. \"update F15\")");
                display();
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
                    displayMe.add("Error: invalid argument for update command");
                    displayMe.add("\t\t\"update Qyy\" (e.g. \"update F15\")");
                    display();
                }
            }
        }
        else if (argc.equals("seatwatcher") || argc.equals("sw")) {
            if (argv.length > 0) {
                displayMe.add("Error: too many arguments for seatwatcher command");
                displayMe.add("\t\t\"seatwatcher\" OR \"sw\"");
                display();
            }
            else 
                seatWatcher();
        }
        else if (argc.equals("mycourses") || argc.equals("mc")) {
            if (argv.length > 0) {
                displayMe.add("Error: too many arguments for seatwatcher command");
                displayMe.add("\t\t\"seatwatcher\" OR \"sw\"");
                display();
            }
            else 
                showMyCourses();
        }
        else if (argc.equals("schedule") || argc.equals("sch")) {
            if (argv.length < 1) { 
                displayMe.add("Error: too few arguments for schedule command");
                displayMe.add("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
                displayMe.add("Alternatively, if you have already defined a scheduleName:");
                displayMe.add("\t\t\"schedule scheduleName\" OR \"sch scheduleName\"");
                display();
            }
            else
                schedule(argv);
        }
        else if (argc.equals("add")) {
            if (argv.length < 1) {
                displayMe.add("Error: invalid number of arguments for add command");
                displayMe.add("\t\t\"add xxxxx\" (e.g. \"add 27000\")");
                display();
            }
            for (int i=0; i<argv.length; i++) {
                if (argv[i].length() != 5) {
                    displayMe.add("Error: invalid size for argument of add command");
                    displayMe.add("\t\t\"add xxxxx\" (e.g. \"add 27000\")");
                    display();
                }
                else {
                    addToSeatWatch((new Integer(argv[i])).intValue(),currentQuarter,currentFirstCourse);
                }
            }
            seatWatcher();
        }
        else if (argc.equals("remove")) {
            if (argv.length < 1) {
                displayMe.add("Error: invalid number of arguments for remove command");
                displayMe.add("\t\t\"remove xxxxx\" (e.g. \"remove 27000\")");
                display();
            }
            for (int i=0; i<argv.length; i++) {
                if (argv[0].length() != 5) {
                    displayMe.add("Error: invalid size for argument of remove command");
                    displayMe.add("\t\t\"remove xxxxx\" (e.g. \"remove 27000\")");
                    display();
                }
                else {
                    removeFromSeatWatch((new Integer(argv[i])).intValue(),currentQuarter,currentFirstCourse);
                }
            }
            seatWatcher();
        }
        else if (argc.equals("search") || argc.equals("f")) {
            if (argv.length == 0) {
                displayMe.add("Error: too few arguments for search command");
                displayMe.add("\t\t\"search -q Qyy -d DEPT -n xxx -p Proflastname -i xxxxx\" " +
                              "(e.g. search -q F15 -d COEN -n 20 -p Lewis -i 27000)");
                display();
            }
            else if (argv.length%2 != 0) {
                displayMe.add("Error: invalid number of arguments for search command");
                displayMe.add("\t\t\"search -q Qyy -d DEPT -n xxx -p Proflastname -i xxxxx\" " +
                              "(e.g. search -q F15 -d COEN -n 20 -p Lewis -i 27000)");
                display();
            }
            else {
                ArrayList<String[]> allCoursesEver = new ArrayList<String[]>();
                //ArrayList<String[]> newArr = new ArrayList<String[]>();
                for (String[][] termArr : (new ArrayList<String[][]>(allCoursesAndTerms.values()))) {
                    for (String[] courseArr : termArr) {
                        if (courseArr != null)
                            allCoursesEver.add(courseArr);
                    }
                }
                /*boolean quarterSpecified = false;
                for (int i=0; i<argv.length; i++) {
                    if (argv[i].equals("-q")) {
                        quarterSpecified = true;
                        allCoursesEver = findByX("quarter", allCoursesEver, argv[i+1]);
                        i++;
                    }
                }
                if (!quarterSpecified)
                    allCoursesEver = findByX("quarter", allCoursesEver, "F15");*/
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
                            displayMe.add("Error: usage unsupported for some majors");
                            display();
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
                        displayMe.add("Error: invalid argument(s) for search command");
                        displayMe.add("\t\t\"search -q Qyy -d DEPT -n xxx -p Proflastname -i xxxxx\" " +
                                      "(e.g. search -q F15 -d COEN -n 20 -p Lewis -i 27000)");
                        display();
                        doDisplay = false;
                        break;
                    }
                }
                if (!quarterSearched) {
                    allCoursesEver = findByX("quarter", allCoursesEver, currentQuarterName);
                }
                if (doDisplay) {
                    displayClassList(allCoursesEver, "search");
                }
            }
        }
        else if (argc.equals("minor")) {
            if (argv.length != 1) {
                displayMe.add("Need argument for command 'minor'");
                display();
            }
            else {
                findMinor(argv[0]);
            }
        }
        else if (argc.equals("major")) {
            if (argv.length != 1) {
                displayMe.add("Need argument for command 'major'");
                display();
            }
            else {
                findMajor(argv[0]);
            }
        }
        else if (argc.equals("details") || argc.equals("d")) {
            displayDetails((new Integer(argv[0])).intValue());
        }
        //corewatcher commands
        else if (argc.equals("doubledip") || argc.equals("dd")) {
            findDoubleCore();
        }
        else if (argc.equals("mydoubledip") || argc.equals("mdd")) {
            findMyDoubleCore();
        }
        else if (argc.equals("coremanager") || argc.equals("cm")) {

        }
        else {
            displayMe.add("Please enter a valid command");
            display();
        }
        secondLastCommand = lastCommand;
        secondLastArgs = lastArgs;
        lastCommand = argc;
        lastArgs = argv;
    }

    private static void schedule(String[] argv) {
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
                displayMe.add("Error: could not find schedule "+argv[0]+". Please try formatting command as follows:");
                displayMe.add("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
                displayMe.add("Alternatively, if you have already defined a scheduleName:");
                displayMe.add("\t\t\"schedule scheduleName\" OR \"sch scheduleName\"");
                display();
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
                displayMe.add("Error: classes "+conf1+" and "+conf2+" have a time conflict. Try again using the following command:");
                displayMe.add("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
                displayMe.add("Alternatively, if you have already defined a scheduleName:");
                displayMe.add("\t\t\"schedule scheduleName\" OR \"sch scheduleName\"");
                display();
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

    private static void displaySchedule(ArrayList<String> schedule) {
        String[] days = {"M","T","W","R","F"};
        String[][] spots = new String[39][5];
        for (int i=0; i<39; i++)
            for (int j=0; j<5; j++)
                spots[i][j]="                       ";
        int lineCount = 0;
        for (int i=1; i<schedule.size(); i++) {
            SCUCourse course = new SCUCourse(Integer.parseInt(schedule.get(i)),currentQuarter);
            int startH = Integer.parseInt(course.getTimes().substring(0,2));
            int startM = Integer.parseInt(course.getTimes().substring(3,5));
            int endH = Integer.parseInt(course.getTimes().substring(6,8));
            int endM = Integer.parseInt(course.getTimes().substring(9,11));
            //displayMe.add(startH+":"+startM+"-"+endH+":"+endM);
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
        displayMe.add("\t         Monday                Tuesday               Wednesday               Thursday               Friday");
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
            displayMe.add(displayLine);
        }
        /*for (int hr=8; hr<=12; hr++) {
            for (int min=0; min<60; min+=20) {
                String displayStr = "\t";
                if (min == 0) {
                    displayStr = hr+"am\t";
                    if (hr == 12)
                        displayStr = hr+"pm\t";
                }
                for (int day=0; day<5; day++) {
                    for (int i=1; i<schedule.size(); i++) {
                        SCUCourse course = new SCUCourse(Integer.parseInt(schedule.get(i)),currentQuarter);
                        displayStr += getViewFrom(course, hr, min, days[day]);
                    }
                }
                displayMe.add(displayStr);
            }
        }
        for (int hr=1; hr<=8; hr++) {
            for (int min=0; min<60; min+=20) {
                String displayStr = "\t";
                if (min == 0)
                    displayStr = hr+"pm\t";
                for (int day=0; day<5; day++) {
                    boolean hasClassForDay = false;
                    for (int i=1; i<schedule.size(); i++) {
                        SCUCourse course = new SCUCourse(Integer.parseInt(schedule.get(i)),currentQuarter);
                        String newStr = getViewFrom(course, hr, min, days[day]);
                        displayStr += newStr;
                        if (!newStr.equals("")) {
                            hasClassForDay = true;
                            break;
                        }
                    }
                    if (!hasClassForDay)
                        displayStr += "                       ";
                }
                displayMe.add(displayStr);
            }
        }*/
        display();
    }

    private static String getViewFrom(SCUCourse course, int hr, int min, String day) {
        if (course.getDays().contains(day)) {
            int startH = Integer.parseInt(course.getTimes().substring(0,2));
            int startM = Integer.parseInt(course.getTimes().substring(3,5));
            int endH = Integer.parseInt(course.getTimes().substring(6,8));
            int endM = Integer.parseInt(course.getTimes().substring(9,11));
            if (hr <= endH && hr >= startH && min < endM+20 && min >= startM-20) {
                //stage 1
                if (hr == startH && startM-min<20 && startM>=min) {
                    return " --------------------- ";
                }
                //stage 2, 20 minutes after stage 1
                else if ((min==0 && hr-1==startH && startM-40<20 && startM>=40)
                        || (min!=0 && hr==startH && startM-(min-20)<20 && startM>=min-20)) {
                    String returnMe = " | "+course.getCourseID()+" - "+course.getCourse();
                    for (int i=0; i<10-course.getCourse().length(); i++)
                        returnMe += " ";
                    returnMe += "| ";
                    return returnMe;
                }
                /*//stage 3, 40 minutes after stage 1
                else if () {
                    course.updateSeats();

                }
                //stage 5, end of class
                else if () {

                }*/
                //stage 4, blank pretty much, still more space to fill
                else {
                    return " |                   | ";
                }
            }
        }
        return "";
    }

    private static boolean conflicts(String id1, String id2) {
        SCUCourse course1 = new SCUCourse(Integer.parseInt(id1), currentQuarter);
        SCUCourse course2 = new SCUCourse(Integer.parseInt(id2), currentQuarter);
        int startTime1H = Integer.parseInt(course1.getTimes().substring(0,2));
        int startTime1M = Integer.parseInt(course1.getTimes().substring(3,5));
        int startTime2H = Integer.parseInt(course2.getTimes().substring(0,2));
        int startTime2M = Integer.parseInt(course1.getTimes().substring(3,5));
        int endTime1H = Integer.parseInt(course1.getTimes().substring(6,8));
        int endTime1M = Integer.parseInt(course1.getTimes().substring(9,11));
        int endTime2H = Integer.parseInt(course2.getTimes().substring(6,8));
        int endTime2M = Integer.parseInt(course2.getTimes().substring(9,11));
        if (startTime1H<8) startTime1H += 12;
        if (startTime2H<8) startTime2H += 12;
        if (endTime1H<=8) endTime1H += 12;
        if (endTime2H<=8) endTime2H += 12;
        //check for 2nd end time in first class
        if (endTime2H<=endTime1H && endTime2H>=startTime1H) {
            if ((endTime2H==endTime1H && endTime2M>endTime1M) || (endTime2H==startTime1H && endTime2M>startTime1M))
                return false;
            return true;
        }
        //check for 2nd start time in first class
        if (startTime2H<=endTime1H && startTime2H>=startTime1H) {
            if ((startTime2H==endTime1H && startTime2M>endTime1M) || (startTime2H==startTime1H && startTime2M<startTime1M))
                return false;
            return true;
        }
        return false;
    }

    private static void findMinor(String dept) {
        int overflow = 0;
        try {
            File minorFile = new File("docs/degreeReqs/"+dept+"minor.txt");
            if (minorFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(minorFile.getAbsoluteFile()));
                int i = 0;
                for (String line = br.readLine(); line != null; line = br.readLine()){
                    if (line.length()>WINDOW_COLS)
                        overflow+=line.length()/WINDOW_COLS;
                    displayMe.add(line);
                }
                br.close();
            }
            else {
                displayMe.add("Unfortunately no minor was found for the department you gave.");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        display(overflow);
    }

    private static void findMajor(String dept) {
        int overflow = 0;
        try {
            File minorFile = new File("docs/degreeReqs/"+dept+"major.txt");
            if (minorFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(minorFile.getAbsoluteFile()));
                int i = 0;
                for (String line = br.readLine(); line != null; line = br.readLine()){
                    if (line.length()>WINDOW_COLS)
                        overflow+=line.length()/WINDOW_COLS;
                    displayMe.add(line);
                }
                br.close();
            }
            else {
                displayMe.add("Unfortunately no minor was found for the department you gave.");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        display(overflow);
    }

    private static void displayClassList(ArrayList<String[]> allCoursesEver, String displayType) {
        for (String[] info : allCoursesEver) {
            displayMe.add(formatLine((new SCUCourse(info)).getInfo(),0));
        }
        Collections.sort(displayMe);
        displayMe.add(0,formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        displayMe.add(1,dashes);
        display();
    }

    private static ArrayList<String[]> findByX(String x, ArrayList<String[]> allCoursesEver, String arg) {
        ArrayList<String[]> returnMe = new ArrayList<String[]>();
        //System.out.println(x+" "+arg+(x.equals("ID")));
        for (String[] info : allCoursesEver) {
            //System.out.println("jep");
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
                displayMe.add("Something went wrong :(");
                display();
            }
        }
        return returnMe;
    }

    private static int getTermID(int courseID) {
        for (int i=0; i<quarterIDs.length; i++) {
            if (quarterIDs[i][2]-courseID <= quarterIDs[i][2]-quarterIDs[i][1] && quarterIDs[i][2]-courseID >= 0)
                return quarterIDs[i][0];
        }
        return currentQuarter;
    }

    private static int getFirstCourse(int courseID) {
        int termID = getTermID(courseID);
        for (int i=0; i<quarterIDs.length; i++) {
            if (quarterIDs[i][0] == termID)
                return quarterIDs[i][1];
        }
        return currentFirstCourse;
    }

    private static void displayDetails(int courseID) {
        SCUCourse currentCourse = (new SCUCourse(allCoursesAndTerms.get(""+getTermID(courseID))[courseID-getFirstCourse(courseID)]));
        displayMe.add(formatLine(currentCourse.getInfo(),0));
        String[] allCourseInfo = currentCourse.getAllInfo();
        String[] fieldDescriptions = {"Course ID\t","Term ID\t\t","Subject\t\t","Course\t\t","Title\t\t","Description\t","Core Requirements",
                                      "Pathway Requirements","My Core Requirements","Enrollment Info\t","Max Units\t","Min Units\t",
                                      "Instructor Last Name","Instructor First Name","Days\t\t","Time\t\t","Location\t","SeatsRemaining\t",
                                      "Term\t\t","Student Level\t","School\t\t"};
        for (int i=0; i<allCourseInfo.length; i++) {
            if (fieldDescriptions[i].equals("Description")) {
                ArrayList<String> description = new ArrayList<String>();
                int j=0;
                displayMe.add(" "+allCourseInfo[i].length()/WINDOW_COLS);
                String curr = "";
                String leftover = "";
                for (j=0; j<allCourseInfo[i].length()/WINDOW_COLS; j++) {
                    curr = allCourseInfo[i].substring(j*WINDOW_COLS, (j+1)*(WINDOW_COLS));
                    displayMe.add(leftover+curr);
                    displayMe.add("\n"+allCourseInfo[i].substring(j*WINDOW_COLS, (j+1)*(WINDOW_COLS)));
                }
            }
            else
                displayMe.add(fieldDescriptions[i]+"\t|\t\t"+allCourseInfo[i]);
        }
        display((allCourseInfo[5].length()+"Description              |                ".length())/WINDOW_COLS);
    }

    private static void displayDescription(int courseID) {
        for (String[][] termArr : (new ArrayList<String[][]>(allCoursesAndTerms.values()))) {
            for (String[] courseArr : termArr) {
                if (courseArr != null) {
                    SCUCourse course = new SCUCourse(courseArr);
                    if (course.getCourseID() == courseID)
                        displayMe.add(course.getDescription());
                }
            }
        }
        display();
    }

    private static void findDoubleCore() {
        int termID = currentQuarter;
        int firstCourse = currentFirstCourse;
        int lastCourse = currentFirstCourse+3000;
        displayMe.add(formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        displayMe.add(dashes);
        for (int i=firstCourse; i<lastCourse; i++) {
            if (allCoursesAndTerms.get(""+termID)[i-firstCourse] != null && allCoursesAndTerms.get(""+termID)[i-firstCourse][6].contains(",")) {
                displayMe.add(formatLine((new SCUCourse(allCoursesAndTerms.get(""+termID)[i-firstCourse])).getInfo(),0));
            }
        }
        display();
    }

    private static void findMyDoubleCore() {
        //Try again:
        ArrayList<String> lines = new ArrayList<String>();
        String[] coreNames1 = {"Ethics","Diversity","Social Science","RTC 2","C&I 3","RTC 3","ELSJ"};
        String[] coreNames = {"Ethics","RTC 3","ELSJ"};
        displayMe.add(formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        displayMe.add(dashes);
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
            displayMe.add(str);
        }
        display();
    }

    private static void loadSW(int termID) {
        try {
            File swCourseFile = new File("docs/swcourses" + termID + ".txt");
            if (swCourseFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(swCourseFile.getAbsoluteFile()));
                int i = 0;
                for (String line = br.readLine(); line != null; i++){
                    try {
                        addToSeatWatch((new Integer(line.substring(0,5))).intValue(),termID,currentFirstCourse,line.substring(6,line.length()));
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

    private static void updateFile(int termID) {
        ArrayList<String> seatsRemaining = new ArrayList<String>();
        ArrayList<String> courseIDs = new ArrayList<String>();
        for (String[] arr : seatWatchCourses) {
            seatsRemaining.add(arr[17]);
            courseIDs.add(arr[0].substring(1,6));
        }
        try {
            File swCourseFile = new File("docs/swcourses" + termID + ".txt");
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

    private static void getTentative(int termID) {
        try {
            File tentFile = new File("docs/tentative" + termID + ".txt");
            File swCourseFile = new File("docs/courseInfo" + termID + ".txt");
            if (!swCourseFile.exists()) {
                swCourseFile.createNewFile();
            }
            FileWriter fw = new FileWriter(swCourseFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            String writeMe = "";
            if (tentFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(tentFile.getAbsoluteFile()));
                int i = 0;
                int count = 90001;
                for (String line = br.readLine(); line != null; i++){
                    try {
                        if (line.contains("TENTATIVES")) {
                            for (int j=0; j<9; j++) {
                                line = br.readLine();
                                i++;
                            }
                        }
                        else {
                            String[] info = new String[21];
                            String courseName = "";
                            String times = "";
                            for (int j=0; j<21; j++) {
                                if (j == 0) {
                                    info[j] = ""+count;
                                    count++;
                                }
                                else if (j == 1) {
                                    info[j] = "3740";
                                }
                                else if (j == 6 || j == 7 || j == 8)
                                    info[j] = "[]";
                                else if (j == 10 || j == 11 || j == 17)
                                    info[j] = "1";
                                else if (j == 18)
                                    info[j] = "Spring 2016";
                                else if (j == 19)
                                    info[j] = "Undergraduate";
                                else
                                    info[j] = "-";
                            }
                            System.out.println(line);
                            for (int j=0; j<=7; j++) {
                                if (j == 0)
                                    courseName += line+" ";
                                else if (j == 1)
                                    info[3] = courseName+line;
                                else if (j == 2)
                                    info[4] = line;
                                else if (j == 4)
                                    info[14] = line;
                                else if (j == 5)
                                    times += line+"-";
                                else if (j == 6) {
                                    info[15] = times+line;
                                }
                                else if (j == 7) {
                                    System.out.println(j+line);
                                    if (line.contains(",")) {
                                        info[12] = line.substring(0,line.indexOf(","));
                                        info[13] = line.substring(line.indexOf(","),line.length());
                                    }
                                    else {
                                        info[12] = line;
                                        info[13] = ","+line;
                                    }
                                }
                                line = br.readLine();
                                i++;
                            }
                            writeMe += "{";
                            for (int j=0; j<info.length-1; j++) {
                                writeMe += info[j]+"~";
                            }
                            writeMe += info[info.length-1]+"}\n";
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    //line = br.readLine();
                }
                br.close();
            }
            bw.write(writeMe);
            bw.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static String seatWatcher() {
        String returnMe = "";
        loading(3);
        //Insertion sort
        for (int i=0; i<seatWatchCourses.size(); i++) {
            String[] temp = seatWatchCourses.get(i);
            int j;
            for (j=i-1; j>=0 && temp[3].compareTo(seatWatchCourses.get(j)[3])<0; j--)
                seatWatchCourses.set(j+1,seatWatchCourses.get(j));
            seatWatchCourses.set(j+1,temp);
        }
        boolean isRunning = true;
        displayMe.add(formatLine(descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        displayMe.add(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+course.updateSeats();
            String addMe = formatLine(course.getSWInfo(),1);
            displayMe.add(addMe);
            returnMe += addMe;
        }
        //updateFile(currentQuarter);
        display();
        return returnMe;
    }

    public static String showMyCourses() {
        String returnMe = "";
        loading(3);
        //Insertion sort
        for (int i=0; i<seatWatchCourses.size(); i++) {
            String[] temp = seatWatchCourses.get(i);
            int j;
            for (j=i-1; j>=0 && temp[3].compareTo(seatWatchCourses.get(j)[3])<0; j--)
                seatWatchCourses.set(j+1,seatWatchCourses.get(j));
            seatWatchCourses.set(j+1,temp);
        }
        boolean isRunning = true;
        displayMe.add(formatLine(descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        displayMe.add(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+course.getSeatsRemaining();
            String addMe = formatLine(course.getSWInfo(),1);
            displayMe.add(addMe);
            returnMe += addMe;
        }
        //updateFile(currentQuarter);
        display();
        return returnMe;
    }

    private static void removeFromSeatWatch(int courseID, int termID, int firstCourse) {
        for (int i=0; i<seatWatchCourses.size(); i++) {
            if (seatWatchCourses.get(i)[0].substring(1,6).equals(""+courseID)) {
                seatWatchCourses.remove(i);
            }
        }
    }

    private static void addToSeatWatch(int courseID , int termID, int firstCourse, String seats) {
        boolean doesExist = false;
        for (int i=0; i<seatWatchCourses.size(); i++) {
            //System.out.println(""+courseID+" "+seatWatchCourses.get(i)[0].substring(1,6));
            if (seatWatchCourses.get(i)[0].substring(1,6).equals(""+courseID)) {
                doesExist = true;
            }
        }
        //loading(3);
        if (!doesExist) {
            if (seats != "~~~")
                allCoursesAndTerms.get(""+termID)[courseID-firstCourse][17] = seats;
            String[] info = allCoursesAndTerms.get(""+termID)[courseID-firstCourse];
            boolean isRunning = true;
            seatWatchCourses.add(info);
            //seatWatcher();
        }
    }

    private static void addToSeatWatch(int courseID , int termID, int firstCourse) {
        addToSeatWatch(courseID, termID, firstCourse, "~~~");
    }

    /* * * * * * * * * * * * * * * * FILE IO FUNCTIONS * * * * * * * * * * * * * * * */

    private static void scrapeToFile(int termID, int firstCourse) {
        int lastCourse = firstCourse+3000;
        try {
            File courseInfo = new File("docs/courseInfo" + termID + ".txt");
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

    private static String configData(int termID, int firstCourse, int lastCourse) {
        ArrayList<String> displayMes = new ArrayList<String>();
        String returnMe = "";
        for (int i=firstCourse; i<lastCourse+1; i++) { //27510
            if (i%100 == 0) {
                displayMes.add("Done through: " + i);
                for (String displayer : displayMes) {
                    displayMe.add(displayer);
                }
                display();
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

    private static void loadData(int termID, int firstCourse) {
        int lastCourse = firstCourse+3000;
        try {
            File courseInfo = new File("docs/courseInfo" + termID + ".txt");
            if (courseInfo.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(courseInfo.getAbsoluteFile()));
                int i = 0;
                int phase = 0;
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
                displayMe.add("No data for term "+termID+" found.");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void meTask() {
        //load file about user
        //display options
        //get argc and argv like in run()
        displayMe.add("Testing this stuff");
        display();
        boolean isRunning = true;
        while (isRunning) {
            try {
                input = c.readLine();//scan.nextLine();
                String[] allLines = input.split(" ");
                String argc = allLines[0];
                String[] argv = {};
                if (allLines.length>1) {
                    argv = new String[allLines.length-1];
                    for (int i=1; i<allLines.length; i++) {
                        argv[i-1] = allLines[i];
                    }
                }
                isRunning = doMeTask(argc,argv);
            } catch (ArrayIndexOutOfBoundsException e) {
                displayMe.add("Invalid command");
                display();
            }
        }
        help();
    }

    private static boolean doMeTask(String argc, String[] argv) {
        
        return false;
    }

    /* * * * * * * * * * * * * * * * INTERFACE FUNCTIONS * * * * * * * * * * * * * * * */

    private static void display() {
        clearEverything();
        c.writer().print(ESC + "[1;1H");
        c.flush();
        String[] displayAlts = {"\u001B[0;38m", "\u001B[0;37m"};
        String dashes = "";
        /*for (int i=0; i<27; i++)
            dashes += "-----";
        System.out.println(dashes);*/
        for (int i = 0; i < WINDOW_SPACES-1; i++) {
            if (i<displayMe.size()) {
                c.writer().print(ESC + "["+(i+1)+";1H");
                c.flush();
                c.writer().println(displayAlts[i%2]+displayMe.get(i));
                c.flush();
                //System.out.println(displayAlts[i%2]+displayMe.get(i));
            }
            else {
                c.writer().print(ESC + "["+(i+1)+";1H");
                c.flush();
                c.writer().println();
                c.flush();
                //System.out.println();
            }
        }
        c.writer().print(ESC + "["+(WINDOW_SPACES)+";1H");
        c.flush();
        c.writer().print("> ");
        c.flush();
        //System.out.print("> ");
        displayMe = new ArrayList<String>();
    }

    private static void display(int overflow) {
        clearEverything();
        c.writer().print(ESC + "[1;1H");
        c.flush();
        String[] displayAlts = {"\u001B[0;38m", "\u001B[0;37m"};
        /*String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        System.out.println(dashes);*/
        int actualO = 0;
        for (int i = 0; i < WINDOW_SPACES-overflow-1; i++) {
            if ((i+actualO)<displayMe.size()) {
                c.writer().print(ESC + "["+(i+1+actualO)+";1H");
                c.flush();
                c.writer().println(displayAlts[i%2]+displayMe.get(i));
                c.flush();
                if ((displayAlts[i%2]+displayMe.get(i)).length()>WINDOW_COLS)
                    actualO += (displayAlts[i%2]+displayMe.get(i)).length()/WINDOW_COLS;
                //System.out.println(displayAlts[i%2]+displayMe.get(i));
            }
            else {
                c.writer().print(ESC + "["+(i+1+actualO)+";1H");
                c.flush();
                c.writer().println();
                c.flush();
                //System.out.println();
            }
        }
        c.writer().print(ESC + "["+(WINDOW_SPACES)+";1H");
        c.flush();
        c.writer().print("> ");
        c.flush();
        //System.out.print("> ");
        displayMe = new ArrayList<String>();
    }

    public static void clearEverything() {
        String printMe = "";
        for (int i=0; i<=WINDOW_COLS; i++) {
            printMe += " ";
        }
        for (int i=0; i<WINDOW_SPACES; i++) {
            c.writer().print(ESC+"["+(i+1)+";1H");
            c.writer().println(printMe);
            c.flush();
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

    private static void loading(int dots) {
        String addMe = "Loading";
        while (dots>0) {
            addMe += ".";
            dots--;
        }
        displayMe.add(addMe);
        display();
    }
}
