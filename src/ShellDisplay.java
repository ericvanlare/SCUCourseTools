import java.util.ArrayList;
import java.io.*;

/**
 * Handles all direct console IO, including reading lines from the user and
 * writing to the display.
 *
 * @author Eric Van Lare
 */
public class ShellDisplay {
    private Console c;
    private ArrayList<String> displayMe;

    private final char ESC = 27;
    private int WINDOW_SPACES = 52;
    private int WINDOW_COLS = 209;

    /**
     * Construct a ShellDisplay and make sure it recognizes the console
     * dimensions.
     */
    public ShellDisplay() {
        c = System.console();
        displayMe = new ArrayList<String>();
        updateRowsAndCols();
    }

    /**
     * Add the given line to the display. If the length of the line is greater
     * than the width of the console, split the line accordingly.
     *
     * @param line text to be added to the display
     */
    public void addLine(String line) {
        int i=0;
        while (i<line.length()/WINDOW_COLS)
            displayMe.add(line.substring(WINDOW_COLS*i,WINDOW_COLS*(++i)));
        displayMe.add(line.substring(WINDOW_COLS*i,line.length()));
    }

    /**
     * Clear the display using java.io.Console
     */
    public void clear() {
        String printMe = "";
        for (int i=0; i<=WINDOW_COLS+1; i++) {
            printMe += " ";
        }
        for (int i=0; i<WINDOW_SPACES; i++) {
            c.writer().print(ESC+"["+(i+1)+";1H");
            c.writer().println(printMe);
            c.flush();
        }
    }

    /**
     * Print all the lines that have been added to the console, and add a
     * user prompt ">" at the bottom.
     */
    public void display() {
        clear();
        c.writer().print(ESC + "[1;1H");
        c.flush();
        String[] displayAlts = {"\u001B[0;38m", "\u001B[0;37m"};

        for (int i = 0; i < WINDOW_SPACES-1; i++) {
            if (i<displayMe.size()) {
                c.writer().print(ESC + "["+(i+1)+";1H");
                c.flush();
                c.writer().println(displayAlts[i%2]+displayMe.get(i));
                c.flush();
            }
            else {
                c.writer().print(ESC + "["+(i+1)+";1H");
                c.flush();
                c.writer().println();
                c.flush();
            }
        }

        c.writer().print(ESC + "["+(WINDOW_SPACES)+";1H");
        c.flush();
        c.writer().print("> ");
        c.flush();

        displayMe = new ArrayList<String>();
    }

    /**
     * Get the maximum number of spaces that can be displayed on a line on the
     * console with its current width.
     *
     * @return width of console
     */
    public int getWidth() {
        return WINDOW_COLS;
    }

    /**
     * Get the maximum number of rows that can be displayed on the console with
     * its current height.
     *
     * @return height of console
     */
    public int getHeight() {
        return WINDOW_SPACES;
    }

    /**
     * Displays a loading screen so that the user knows something is actually
     * happening in their program and it hasn't frozen. Allows for a variable
     * amount of dots to be displayed.
     *
     * @param dots number of dots to output on loading screen
     */
    public void loading(int dots) {
        String addMe = "Loading";
        while (dots>0) {
            addMe += ".";
            dots--;
        }
        displayMe.add(addMe);
        display();
    }

    /**
     * Clear everything from the display and leave it ready for user input
     * after program terminates
     */
    public void quit() {
        clear();
        c.writer().println(ESC+"[1;1H");
        c.flush();
    }

    /**
     * Read the next line from the console.
     *
     * @return line from console input
     */
    public String readLine() {
        return c.readLine();
    }

    /**
     * Update the maximum number of rows and columns possible to display on the
     * console. Currently only supported on Mac.
     */
    public void updateRowsAndCols() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            String s;
            String a = "";
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", "tput lines 2> /dev/tty"});
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = stdInput.readLine()) != null) {
                    a += s;
                }
                WINDOW_SPACES = Integer.parseInt(a) - 1;
            } catch (IOException e) {
            }
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", "tput cols 2> /dev/tty"});
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((s = stdInput.readLine()) != null) {
                    a += s;
                }
                a = a.substring(2, 5);
                WINDOW_COLS = Integer.parseInt(a) - 1;
            } catch (IOException e) {}
        }
    }
}
