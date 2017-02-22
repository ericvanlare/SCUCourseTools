import java.io.*;
import java.util.ArrayList;

/**
 *
 *
 * @author Eric Van Lare
 */
public class SeatWatcher {
    private ArrayList<String[]> seatWatchCourses;
    private ShellDisplay display;

    public SeatWatcher(ShellDisplay display) {
        seatWatchCourses = new ArrayList<String[]>();
        this.display = display;
    }

    private void loadSW(int termID) {
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
        display.addLine(SCUCourseTools.formatLine(SCUCourseTools.descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+course.updateSeats();
            String addMe = SCUCourseTools.formatLine(course.getSWInfo(),1);
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
        display.addLine(SCUCourseTools.formatLine(SCUCourseTools.descriptions[1],1));
        String dashes = "";
        for (int i=0; i<27; i++)
            dashes += "-----";
        display.addLine(dashes);
        for (String[] str : seatWatchCourses) {
            SCUCourse course = new SCUCourse(str);
            str[str.length-4] = ""+course.getSeatsRemaining();
            String addMe = SCUCourseTools.formatLine(course.getSWInfo(),1);
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
                SCUCourseTools.allCoursesAndTerms.get(""+termID)[courseID-firstCourse][17] = seats;
            String[] info = SCUCourseTools.allCoursesAndTerms.get(""+termID)[courseID-firstCourse];
            boolean isRunning = true;
            seatWatchCourses.add(info);
            //seatWatcher();
        }
    }

    private void addToSeatWatch(int courseID , int termID, int firstCourse) {
        addToSeatWatch(courseID, termID, firstCourse, "~~~");
    }
}
