import java.util.ArrayList;

/**
 * WIP, displays sample schedule based on classes given. Can create multiple
 * different schedules, and each one can be displayed to compare different
 * course combinations.
 */
public class Scheduler {
    private ShellDisplay display;
    private ArrayList<ArrayList<String>> schedules;

    /**
     * Create a Scheduler that interacts with the given display.
     *
     * @param display ShellDisplay for IO
     */
    public Scheduler(ShellDisplay display) {
        this.display = display;
        schedules = new ArrayList<ArrayList<String>>();
    }

    /**
     * Take argument input and determine whether or not to display a schedule
     * based on the arguments.
     *
     * @param argv arguments describing schedule i.e. scheduleName xxxxx yyyyy ...
     */
    public void schedule(String[] argv) {
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
                display.addLine("Error: could not find schedule "+argv[0]+
                        ". Please try formatting command as follows:");
                display.addLine("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" "+
                        "OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
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
                display.addLine("Error: classes "+conf1+" and "+conf2+
                        " have a time conflict. Try again using the following command:");
                display.addLine("\t\t\"schedule scheduleName xxxxx yyyyy zzzzz\" "+
                        "OR \"sch scheduleName xxxxx yyyyy zzzzz\"");
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

    /**
     * Use the ShellDisplay to print the given schedule.
     *
     * @param schedule schedule to display
     */
    public void displaySchedule(ArrayList<String> schedule) {
        String[] days = {"M","T","W","R","F"};
        String[][] spots = new String[39][5];
        for (int i=0; i<39; i++)
            for (int j=0; j<5; j++)
                spots[i][j]="                       ";
        for (int i=1; i<schedule.size(); i++) {
            SCUCourse course = new SCUCourse(Integer.parseInt(schedule.get(i)),SCUCourseTools.currentQuarter);
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
        display.addLine("\t         Monday                Tuesday               Wednesday"+"" +
                "               Thursday               Friday");
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
}
