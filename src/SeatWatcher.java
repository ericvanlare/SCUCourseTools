import java.io.*;
import java.util.ArrayList;

/**
 * Keeps track of classes specified by the user and updates the seats from
 * CourseAvail. Courses can be added or removed, and seats can be updated on
 * command.
 *
 * @author Eric Van Lare
 */
public class SeatWatcher {
    private ArrayList<String[]> seatWatchCourses;
    private ShellDisplay display;

    /**
     * Create a SeatWatcher that interacts with the given display.
     *
     * @param display ShellDisplay for IO
     */
    public SeatWatcher(ShellDisplay display) {
        seatWatchCourses = new ArrayList<String[]>();
        this.display = display;
    }

    /**
     * Add the given course to the seat watcher.
     *
     * @param courseID 5-digit course ID number
     * @param termID 4-digit term ID number
     * @param firstCourse first course in the term
     * @param seats number of seats remaining in course
     */
    public void addToSeatWatch(int courseID , int termID, int firstCourse, String seats) {
        boolean doesExist = false;
        for (int i=0; i<seatWatchCourses.size(); i++)
            if (seatWatchCourses.get(i)[0].substring(1,6).equals(""+courseID))
                doesExist = true;

        if (!doesExist) {
            if (seats != "~~~")
                SCUCourseTools.allCoursesAndTerms.get(""+termID)[courseID-firstCourse][17] = seats;
            String[] info = SCUCourseTools.allCoursesAndTerms.get(""+termID)[courseID-firstCourse];
            seatWatchCourses.add(info);
        }
    }

    /**
     * Add the given course to the seat watcher without knowing the seats remaining.
     *
     * @param courseID 5-digit course ID number
     * @param termID 4-digit term ID number
     * @param firstCourse first course in the term
     */
    public void addToSeatWatch(int courseID , int termID, int firstCourse) {
        addToSeatWatch(courseID, termID, firstCourse, "~~~");
    }

    /**
     * Load seat watcher data from the text/swcoursesXXXX.txt file where XXXX
     * is the term ID.
     *
     * @param termID 4-digit term ID number
     */
    public void loadSW(int termID) {
        try {
            File swCourseFile = new File("text/swcourses" + termID + ".txt");
            if (swCourseFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(swCourseFile.getAbsoluteFile()));
                String line = br.readLine();
                while (line != null) {
                    try {
                        addToSeatWatch(Integer.parseInt(line.substring(0,5)),termID,SCUCourseTools.currentFirstCourse,
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

    /**
     * Remove the specified course from the seat watcher.
     *
     * @param courseID 5-digit course ID number
     */
    public void removeFromSeatWatch(int courseID) {
        for (int i=0; i<seatWatchCourses.size(); i++) {
            if (seatWatchCourses.get(i)[0].substring(1,6).equals(""+courseID)) {
                seatWatchCourses.remove(i);
            }
        }
    }

    /**
     * Display all courses being watched. Update the seats if true is passed
     * in, just displays them otherwise.
     *
     * @param update update seats from courses if true
     */
    public void showMyCourses(boolean update) {
        display.loading(3);
        //Insertion sort
        for (int i=0; i<seatWatchCourses.size(); i++) {
            String[] temp = seatWatchCourses.get(i);
            int j;
            for (j=i-1; j>=0 && temp[3].compareTo(seatWatchCourses.get(j)[3])<0; j--)
                seatWatchCourses.set(j+1,seatWatchCourses.get(j));
            seatWatchCourses.set(j+1,temp);
        }
        display.addLine(SCUCourseTools.formatLine(SCUCourseTools.descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+(update?course.updateSeats():course.getSeatsRemaining());
            String addMe = SCUCourseTools.formatLine(course.getSWInfo(),1);
            display.addLine(addMe);
        }
        display.display();
    }

    /**
     * Update the text/swcoursesXXXX.txt file where XXXX is the term ID. Stores
     * the course numbers as well as the seats remaining in each course. One
     * course per line, uses '*' delimiter.
     *
     * @param termID 4-digit term ID number
     */
    public void updateFile(int termID) {
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
}
