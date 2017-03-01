import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collections;
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
    protected static int currentQuarter;
    private String currentQuarterName;
    protected static int currentFirstCourse;

    private String lastCommand;
    private String secondLastCommand;
    private String[] lastArgs;
    private String[] secondLastArgs;

    private static int[][] spaces = {{10,34,5,22,4,11,30},{10,34,5,22,4,11,20}};

    private ShellDisplay display;
    private SeatWatcher seatWatcher;
    private Scheduler scheduler;
    private FileIO fileIO;

    /**
     * Setup and run the SCUCourseTools.
     */
    public SCUCourseTools() {
        setup();
        run();
    }

    /**
     * Create a new SCUCourseTools object.
     *
     * @param args
     */
    public static void main(String[] args) {
        new SCUCourseTools();
    }

    /**
     * Setup all all variables and load the seat watcher and the help screen.
     */
    private void setup() {

        currentQuarter = quarterIDs[0][0];
        currentQuarterName = quarterNames[0];
        currentFirstCourse = quarterIDs[0][1];

        display = new ShellDisplay();
        seatWatcher = new SeatWatcher(display);
        scheduler = new Scheduler(display);
        fileIO = new FileIO(display);

        allCoursesAndTerms = new Hashtable<String,String[][]>();

        quarterInfo = new Hashtable<String,int[]>();
        for (int i=0; i<quarterIDs.length; i++) {
            quarterInfo.put(quarterNames[i],quarterIDs[i]);
            allCoursesAndTerms.put(""+quarterIDs[i][0],new String[3001][]);
            fileIO.loadData(quarterIDs[i][0],quarterIDs[i][1]);
        }

        display.updateRowsAndCols();

        lastCommand = "";
        secondLastCommand = "";
        lastArgs = new String[0];
        secondLastArgs = new String[0];

        seatWatcher.loadSW(currentQuarter);
        fileIO.help();
    }

    /**
     * Poll and parse for input, then attempt to execute it.
     */
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

    /**
     * Attempts to execute given command and arguments.
     *
     * @param argc command
     * @param argv arguments
     */
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
                fileIO.help();
        }
        else if (argc.equals("quit") || argc.equals("q")) {
            seatWatcher.updateFile(currentQuarter);
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
                    fileIO.scrapeToFile(quarterInfo.get(qName)[0],quarterInfo.get(qName)[1]);
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
                seatWatcher.showMyCourses(true);
        }
        else if (argc.equals("mycourses") || argc.equals("mc")) {
            if (argv.length > 0) {
                display.addLine("Error: too many arguments for seatwatcher command");
                display.addLine("\t\t\"seatwatcher\" OR \"sw\"");
                display.display();
            }
            else
                seatWatcher.showMyCourses(false);
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
                scheduler.schedule(argv);
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
                    seatWatcher.addToSeatWatch(Integer.parseInt(argv[i]),currentQuarter,currentFirstCourse);
                }
            }
            seatWatcher.showMyCourses(true);
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
                    seatWatcher.removeFromSeatWatch(Integer.parseInt(argv[i]));
                }
            }
            seatWatcher.showMyCourses(true);
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
                fileIO.findMinor(argv[0]);
            }
        }
        else if (argc.equals("major")) {
            if (argv.length != 1) {
                display.addLine("Need argument for command 'major'");
                display.display();
            }
            else {
                fileIO.findMajor(argv[0]);
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

    /**
     * Display all the courses passed in the courses ArrayList.
     *
     * @param courses List of courses to be displayed
     */
    private void displayClassList(ArrayList<String[]> courses) {
        ArrayList<String> courseStrings = new ArrayList<>();
        for (String[] info : courses) {
            courseStrings.add(formatLine((new SCUCourse(info)).getInfo(),0));
        }
        Collections.sort(courseStrings);
        display.addLine(formatLine(descriptions[0],0));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String course : courseStrings)
            display.addLine(course);
        display.display();
    }

    /**
     * Narrows down a list of courses by eliminating all that don't fit the
     * given criteria.
     *
     * @param x category to sort by
     * @param courses list of courses to be narrowed down
     * @param arg argument of type x
     * @return narrowed down list of courses
     */
    private ArrayList<String[]> findByX(String x, ArrayList<String[]> courses, String arg) {
        ArrayList<String[]> newCourseList = new ArrayList<String[]>();
        for (String[] info : courses) {
            SCUCourse course = new SCUCourse(info);
            if (x.equals("quarter")) {
                if (course.getQuarter().equals(arg) || arg.equals("all"))
                    newCourseList.add(info);
            }
            else if (x.equals("dept")) {
                if (course.getDept().equals(arg))
                    newCourseList.add(info);
            }
            else if (x.equals("number")) {
                if (course.getNumber().equals(arg))
                    newCourseList.add(info);
            }
            else if (x.equals("greaterThan")) {
                try {
                    if (Integer.parseInt(course.getNumber()) >= Integer.parseInt(arg))
                        newCourseList.add(info);
                } catch (NumberFormatException e) {
                    try {
                        if (Integer.parseInt(course.getNumber().substring(0,course.getNumber().length()-1))>=
                                Integer.parseInt(arg))
                            newCourseList.add(info);
                    } catch (NumberFormatException ex) {
                        throw (new Error("usage unsupported for easy majors"));
                    }
                }
            }
            else if (x.equals("lessThan")) {
                try {
                    if (Integer.parseInt(course.getNumber()) <= Integer.parseInt(arg))
                        newCourseList.add(info);
                } catch (NumberFormatException e) {
                    try {
                        if (Integer.parseInt(course.getNumber().substring(0,course.getNumber().length()-1))<=
                                Integer.parseInt(arg))
                            newCourseList.add(info);
                    } catch (NumberFormatException ex) {
                        throw (new Error("usage unsupported for easy majors"));
                    }
                }
            }
            else if (x.equals("professor")) {
                if (course.getInstructorL().equals(arg))
                    newCourseList.add(info);
            }
            else if (x.equals("level")) {
                if (arg.equals("l")) {
                    courses = findByX("greaterThan", courses, "1");
                    courses = findByX("lessThan", courses, "99");
                }
                else if (arg.equals("u")) {
                    courses = findByX("greaterThan", courses, "100");
                    courses = findByX("lessThan", courses, "199");
                }
                else if (arg.equals("m")) {
                    courses = findByX("greaterThan", courses, "200");
                }
                return courses;
            }
            else if (x.equals("ID")) {
                //System.out.println("ja");
                if ((""+course.getCourseID()).equals(arg)) {
                    System.out.println("ja");
                    newCourseList.add(info);
                }
            }
            else {
                display.addLine("Something went wrong :(");
                display.display();
            }
        }
        return newCourseList;
    }

    /**
     * Get the term in which a given course occurs.
     *
     * @param courseID 5-digit course ID number
     * @return term that the given course occurs in
     */
    private int getTermID(int courseID) {
        for (int i=0; i<quarterIDs.length; i++) {
            if (quarterIDs[i][2]-courseID <= quarterIDs[i][2]-quarterIDs[i][1] && quarterIDs[i][2]-courseID >= 0)
                return quarterIDs[i][0];
        }
        return currentQuarter;
    }

    /**
     * Get the first course in the term obtained from the given course ID
     * number.
     *
     * @param courseID 5-digit course ID number
     * @return first course ID number in the term
     */
    private int getFirstCourse(int courseID) {
        int termID = getTermID(courseID);
        for (int i=0; i<quarterIDs.length; i++) {
            if (quarterIDs[i][0] == termID)
                return quarterIDs[i][1];
        }
        return currentFirstCourse;
    }

    /**
     * Display all details about the given course.
     *
     * @param courseID 5-digit course ID number
     */
    private void displayDetails(int courseID) {
        SCUCourse currentCourse = (new SCUCourse(allCoursesAndTerms.get(""+
                getTermID(courseID))[courseID-getFirstCourse(courseID)]));
        display.addLine(formatLine(currentCourse.getInfo(),0));
        String[] allCourseInfo = currentCourse.getAllInfo();
        String[] fieldDescriptions = {"Course ID\t","Term ID\t\t","Subject\t\t","Course\t\t","Title\t\t",
                "Description\t","Core Requirements", "Pathway Requirements","My Core Requirements","Enrollment Info\t",
                "Max Units\t","Min Units\t", "Instructor Last Name","Instructor First Name","Days\t\t","Time\t\t",
                "Location\t","SeatsRemaining\t", "Term\t\t","Student Level\t","School\t\t"};
        for (int i=0; i<allCourseInfo.length; i++) {
            if (fieldDescriptions[i].equals("Description\t")) {
                display.addLine(fieldDescriptions[i]+"\t|\t\t"+
                        allCourseInfo[i].substring(0,display.getWidth()-40));
                display.addLine(allCourseInfo[i].substring(display.getWidth()-40));
            }
            else
                display.addLine(fieldDescriptions[i]+"\t|\t\t"+allCourseInfo[i]);
        }
        display.display();
    }

    /**
     * Find and display all courses that satisfy two core requirements.
     */
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
            if (allCoursesAndTerms.get(""+termID)[i-firstCourse] != null &&
                    allCoursesAndTerms.get(""+termID)[i-firstCourse][6].contains(",")) {
                display.addLine(formatLine((new SCUCourse(allCoursesAndTerms.get(""+
                        termID)[i-firstCourse])).getInfo(),0));
            }
        }
        display.display();
    }

    /**
     * Find and display all courses that satisfy two core requirements that are
     * still in the list of cores I need to satisfy.
     */
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

    /**
     * Formats course info into a line using the given format
     *
     * @param info array with course info
     * @param format 1 for seat watcher, 0 for everything else
     * @return formatted line containing course info
     */
    public static String formatLine(String[] info, int format) {
        String line = " ";
        for (int i=0; i<info.length-1; i++) {
            if (info[i] == null || info[i].equals("null"))
                info[i] = "---";
            line += info[i];
            for (int diff = 0; diff < spaces[format][i]-(info[i].length()); diff++)
                line += " ";
            line += " | ";
        }
        if (info[info.length-1] == null || info[info.length-1].equals("null"))
            info[info.length-1] = "---";
        line += info[info.length-1];
        return line;
    }
}
