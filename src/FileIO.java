import java.io.*;
import java.util.ArrayList;

/**
 * Handles all file reading and writing in SCUCourseTools.
 *
 * @author Eric Van Lare
 */
public class FileIO {
    private ShellDisplay display;

    public FileIO(ShellDisplay display) {
        this.display = display;
    }

    /**
     * Print a help screen loaded from text/help.txt that shows all available
     * commands.
     */
    protected void help() {
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

    /**
     * Find minor requirements for the given department.
     *
     * @param dept department of minor
     */
    protected void findMinor(String dept) {
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

    /**
     * Find major requirements for the given department.
     *
     * @param dept department of major
     */
    protected void findMajor(String dept) {
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

    /**
     * Scrape all course data from the given term into a file.
     *
     * @param termID 4-digit term ID number
     * @param firstCourse 5-digit course ID number of first course in term
     */
    protected void scrapeToFile(int termID, int firstCourse) {
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

    /**
     * Configures course data so that it can be printed to a file. Uses "{}" to
     * encapsulate a single course, "~" to delimit data fields in the course,
     * and "[]" to represent arrays (e.g. for core requirements).
     *
     * @param termID 4-digit term ID number
     * @param firstCourse 5-digit course ID number of the first course in term
     * @param lastCourse 5-digit course ID number of the last course in term
     * @return course data to be written to the file for the given term
     */
    private String configData(int termID, int firstCourse, int lastCourse) {
        ArrayList<String> displayMes = new ArrayList<String>();
        String data = "";
        for (int i=firstCourse; i<lastCourse+1; i++) {
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
                    data += "{";
                    for (int j=0; j<info.length-1; j++) {
                        data += info[j]+"~";
                    }
                    data += info[info.length-1]+"}\n";
                }
            }
        }
        return data;
    }

    /**
     * Load course data for the given term from the text/courseInfoXXXX.txt
     * file, where XXXX is the term ID.
     *
     * @param termID 4-digit term ID number
     * @param firstCourse 5-digit course ID number of the first course in term
     */
    protected void loadData(int termID, int firstCourse) {
        try {
            File courseInfo = new File("text/courseInfo" + termID + ".txt");
            if (courseInfo.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(courseInfo.getAbsoluteFile()));
                int i = 0;
                String[] lineArr;
                for (String line = br.readLine(); line != null; i++){
                    lineArr = line.split("~");
                    if (lineArr.length>0) {
                        SCUCourseTools.allCoursesAndTerms.get(""+
                                termID)[Integer.parseInt(line.substring(1,6))-firstCourse] = lineArr;
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
}
