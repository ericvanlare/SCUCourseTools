import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.math.*;
import java.util.ArrayList;
import java.lang.Integer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by eric on 4/24/15.
 */
class SCUCourse implements Comparable<SCUCourse> {
    //String colors for fancy output
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

	private URL url;
	private static int courseID;
	private static int termID;
	private static String subject;
	private static String course;
	private static String title;
	private static String description;
	private static ArrayList<String> coreReqs;
	private static ArrayList<String> pathwayReqs;
	private static ArrayList<String> myCoreReqs;
	private static String enrollmentInfo;
	private static int maxUnits;
	private static int minUnits;
	private static String instructorL;
	private static String instructorF;
	private static String days;
	private static String times;
	private static String location;
	private static int seatsRemaining;
	private static String term;
	private static String studentLevel;
	private static String school;
	private static boolean doesExist;
	private static String downFrom;

	static private String[] foundations = {"F_CTW1","F_CTW2","F_CI1","F_CI2","F_SLA1","F_SLA2","F_MATH","F_RTC1"};
	static private String[] foundationsNames = {"CTW 1","CTW 2","C&I 1","C&I 2","Second Language 1","Second Language 2","Math","RTC 1"};
	static private String[] explorations = {"E_ETH","E_CE","E_DV","E_ARTS","E_SOSC","E_NTSC","E_RTC2","E_CI3","E_STS","E_RTC3"};
	static private String[] explorationsNames = {"Ethics","Civil Engagement","Diversity","Arts","Social Science","Natural Science","RTC 2","C&I 3",
												 "Science Technology Society","RTC 3","ELSJ"};
	static private String[] integrations = {"I_AW","I_EL"};
	static private String[] integrationsNames = {"Advanced Writing","ELSJ"};
	static private String[] pathways = {"I_PTHAE","I_PTHAMS","I_PTHB","I_PTHCHD","I_PTHCINST","I_PTHDEM","I_PTHDA","I_PTHDT","I_PTHFHP","I_PTHGSB",
										"I_PTHGH","I_PTHHR","I_PTHIS","I_PTHJA","I_PTHLSJ","I_PTHLPOSC","I_PTHPR","I_PTHPP","I_PTHPS","I_PTHRPSI",
										"I_PTHS","I_PTHVST","I_PTHV"};
	static private String[] pathwaysNames = {"Applied Ethics","American Studies","Beauty","Childhood, Family & Society","Cinema Studies","Democracy",
											 "The Digital Age","Design Thinking","Food, Hunger, Poverty Environment","Gender, Sexuality & the Body",
											 "Global Health","Human Rights","Islamic Studies","Justice & the Arts","Law & Social Justice",
											 "Leading People, Organizations & Social Change","Politics & Religion","Public Policy","Paradigm Shifts",
											 "Race Place & Social Inequities","Sustainability","Values in Science & Technology","Vocation"};
	static private String[] myCores = {"E_ETH","E_DV","E_RTC2","E_RTC3","I_EL"};
	static private String[] coreNames = {"Ethics","Diversity","RTC 2","RTC 3","ELSJ"};


	public SCUCourse(int courseID, int termID) {
		this.courseID = courseID;
		this.termID = termID;
		coreReqs = new ArrayList<String>();
		myCoreReqs = new ArrayList<String>();
		pathwayReqs = new ArrayList<String>();
		update();
		//String[] stuff = scrapeCourseInfo(courseID);
		//coreReqs = getCoreReqs(courseID);
	}

	public SCUCourse(String[] info) {
		String[] returnMe = {""+courseID,""+termID,subject,course,title,description,""+coreReqs,""+pathwayReqs,""+myCoreReqs,enrollmentInfo,
							 ""+maxUnits,""+minUnits,instructorL,instructorF,days,times,location,""+seatsRemaining,term,studentLevel,school};
		courseID=Integer.parseInt(info[0].substring(1,6));
		//for (String str : info)
                        //System.out.print(str + ", ");
		termID=Integer.parseInt(info[1]);
		subject=info[2];
		course=info[3];
		title=info[4];
		description=info[5];
		coreReqs = new ArrayList<String>();
		myCoreReqs = new ArrayList<String>();
		pathwayReqs = new ArrayList<String>();

		try {
			//System.out.println(info[6]);
			String coreReqsString = info[6];
			String[] coreReqsArr = coreReqsString.substring(1,coreReqsString.length()-1).split(", ");
			for (String str : coreReqsArr) {
				//System.out.println(str);
				if (str.length()>0) {
					//System.out.println("Yes");
					coreReqs.add(str);
					//System.out.println("Here: "+coreReqs.get(0));
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

	public boolean doesExist() {
		return doesExist;
	}

	public void update() {

		/*int phase = 0;

		//Look for seats, should be 103, can just check for "seats remaining"
		//Add two to get quarter and school
		//Look for "width=\"7" - phase 1
		int subjectNum = 111;
		//Look for "class=\"normal" - phase 2
		int courseNum = 117;
		//Look for "Title", add 2 - phase 3
		int titleNum = 123;
		//Look for "Description", add 2 - phase 4
		int descriptionNum = 130;
		//Core
		//Enrollment
		//Units
		//Instructor - phase 8 (books+4)
		//then days
		//then times*/

		String[] returnMe = new String[6];
		try {
			int addMe;
			URL url  = new URL("https://cms.scu.edu/legacy/courseavail/web/class/?fuseaction=details&class_nbr="+courseID+"&term="+termID);
			URLConnection uc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
			String inputLine;
			int lineCounter = 1;
			//StringBuilder a = new StringBuilder();
			boolean isFound = false;
			boolean isAdjusted = false;
			while ((inputLine = in.readLine()) != null && !isFound) {
				//System.out.println(lineCounter+"\t"+inputLine);
				//a.append(inputLine);
				if (lineCounter == 498) { /////FIX ME PLZ
					if (inputLine.contains("text/javascript")) {
						isFound = true;
						doesExist = false;
					}
					else {
						try {
							seatsRemaining = (new Integer(inputLine.substring(inputLine.indexOf("3>")+2,inputLine.indexOf("seat")-1))).intValue();
						} catch (StringIndexOutOfBoundsException e) {
							seatsRemaining = 0;
						}
						inputLine = in.readLine();
						inputLine = in.readLine();
						term = inputLine.substring(inputLine.indexOf("4>")+2,inputLine.indexOf(":"));
						studentLevel = inputLine.substring(inputLine.indexOf(":")+2,inputLine.indexOf(","));
						school = inputLine.substring(inputLine.indexOf(",")+2,inputLine.indexOf("</")-1);
						lineCounter+=2;
						doesExist = true;
					}
				}
				else if (lineCounter == 506) {
					subject = inputLine.substring(inputLine.indexOf(">")+1,inputLine.indexOf("</"));
				}
				else if (lineCounter == 512) {
					int starti = inputLine.indexOf("\">")+2;
					int endi = inputLine.indexOf("</");
					returnMe[5] = inputLine.substring(starti,endi);
				}
				else if (lineCounter == 518) {
					int starti = inputLine.indexOf(">")+1;
					int endi = inputLine.indexOf("</");
					returnMe[0] = inputLine.substring(starti,endi);
				}
				else if (lineCounter == 525) {
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
					maxUnits = (new Integer(inputLine.substring(inputLine.indexOf("d>")+2,inputLine.indexOf("/")))).intValue();
					minUnits = (new Integer(inputLine.substring(inputLine.indexOf("/")+1,inputLine.indexOf("</")))).intValue();
				}
				else if (inputLine.contains("<th>Avail")) {
					inputLine = in.readLine();
					inputLine = in.readLine();
					inputLine = in.readLine();
					inputLine = in.readLine();
					inputLine = in.readLine();
					lineCounter+=5;

					int starti = inputLine.indexOf("<td>")+4;
					int endi = inputLine.indexOf(",");
					if (endi == -1)
						endi = inputLine.indexOf("</");
					returnMe[2] = inputLine.substring(starti,endi);
					if (endi != inputLine.indexOf("</"))
						instructorF = inputLine.substring(endi,inputLine.indexOf("</"));

					inputLine = in.readLine();
					lineCounter++;
					returnMe[3] = inputLine.substring(inputLine.indexOf("<td>")+4,inputLine.indexOf("<br"));

					inputLine = in.readLine();
					lineCounter++;
					starti = inputLine.indexOf("<td>")+4;
					endi = inputLine.indexOf("<br");
					returnMe[4] = inputLine.substring(starti,endi);
					returnMe[4] = returnMe[4].substring(0,5)+returnMe[4].substring(8,14);

					inputLine = in.readLine();
					inputLine = in.readLine();
					lineCounter+=2;
					//System.out.println(inputLine);
					starti=inputLine.indexOf("d>")+2;
					if (inputLine.contains("map"))
						starti=inputLine.indexOf("\">")+2;
					location = inputLine.substring(starti,inputLine.indexOf("</"));
				}
				lineCounter++;
			}
			in.close();
		}
		catch (NumberFormatException|StringIndexOutOfBoundsException|IOException e) {
			//e.printStackTrace();
		}
		course = returnMe[5];
		title = returnMe[0];
		instructorL = returnMe[2];
		days = returnMe[3];
		times = returnMe[4];
		myCoreFromCore();
	}

	public int updateSeats() {
		try {
			downFrom = "";
			int addMe;
			URL url  = new URL("https://cms.scu.edu/legacy/courseavail/web/class/?fuseaction=details&class_nbr="+courseID+"&term="+termID);
			URLConnection uc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
			String inputLine;
			int lineCounter = 1;
			int seatsRemainingOld = seatsRemaining;
			//StringBuilder a = new StringBuilder();
			boolean isFound = false;
			while ((inputLine = in.readLine()) != null && !isFound) {
				//System.out.println(lineCounter+"\t"+inputLine);
				//a.append(inputLine);
				if (lineCounter == 503) {
					if (!inputLine.contains("text/javascript")) {
						try {
							seatsRemaining = (new Integer(inputLine.substring(inputLine.indexOf("3>")+2,inputLine.indexOf("seat")-1))).intValue();
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
		catch (NumberFormatException|StringIndexOutOfBoundsException|IOException e) {
			//e.printStackTrace();
		}
		return seatsRemaining;
	}

	public void myCoreFromCore() {
		//System.out.println(coreReqs);
		for (String schoolCore : coreReqs)
			for (String myCoreName : coreNames)
				if (myCoreName.equals(schoolCore))
					myCoreReqs.add(schoolCore);
	}

	/*public static String[] scrapeCourseInfo(int courseID) {
		String[] returnMe = new String[6];
		try {
			int addMe;
			URL url  = new URL("http://www.scu.edu/courseavail/class/?fuseaction=details&class_nbr="+courseID+"&term=3700");
			URLConnection uc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
			String inputLine;
			int lineCounter = 1;
			//StringBuilder a = new StringBuilder();
			boolean isFound = false;
			boolean isAdjusted = false;
			while ((inputLine = in.readLine()) != null && !isFound) {
				//System.out.println(lineCounter+"\t"+inputLine);
				//a.append(inputLine);
				if (lineCounter == 117) {
					int starti = inputLine.indexOf("\">")+2;
					int endi = inputLine.indexOf("</");
					returnMe[5] = inputLine.substring(starti,endi);
				}
				else if (lineCounter == 123) {
					int starti = inputLine.indexOf(">")+1;
					int endi = inputLine.indexOf("</");
					returnMe[0] = inputLine.substring(starti,endi);
				}
				else if (inputLine.contains("Books")) {
					lineCounter = 165;
					isAdjusted = true;
				}
				if (isAdjusted) {
					if (lineCounter == 168) {
						int starti = inputLine.indexOf("<td>")+4;
						int endi = inputLine.indexOf("</");
						returnMe[1] = inputLine.substring(starti,endi);
					}
					else if (lineCounter == 169) {
						int starti = inputLine.indexOf("<td>")+4;
						int endi = inputLine.indexOf(",");
						if (endi == -1)
							endi = inputLine.indexOf("</");
						returnMe[2] = inputLine.substring(starti,endi);
					}
					else if (lineCounter == 170) {
						int starti = inputLine.indexOf("<td>")+4;
						int endi = inputLine.indexOf("<br");
						returnMe[3] = inputLine.substring(starti,endi);
					}
					else if (lineCounter == 171) {
						int starti = inputLine.indexOf("<td>")+4;
						int endi = inputLine.indexOf("<br");
						String str = inputLine.substring(starti,endi);
						str = str.substring(0,5)+str.substring(8,14);
						returnMe[4] = str;
					}
				}
				lineCounter++;
			}
			in.close();
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		catch(StringIndexOutOfBoundsException e) {
			//e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		course = returnMe[5];
		title = returnMe[0];
		instructorL = returnMe[2];
		days = returnMe[3];
		times = returnMe[4];
		return returnMe;
	}

	public static ArrayList<String> getCoreReqs(int courseID) {
		ArrayList<String> returnMe = new ArrayList<String>();
		try {
			URL url  = new URL("http://www.scu.edu/courseavail/class/?fuseaction=details&class_nbr="+courseID+"&term=3700");
			URLConnection uc = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "UTF-8"));
			String inputLine;
			int lineCounter = 1;
			//StringBuilder a = new StringBuilder();
			boolean isFound = false;
			boolean isAdjusted = false;
			while ((inputLine = in.readLine()) != null && !isFound) {
				//a.append(inputLine);
				if (inputLine.contains("Core Attribute Keys")) {
					lineCounter = 145;
					isAdjusted = true;
				}
				if (isAdjusted && lineCounter == 147) {
					for (int i=0; i<myCores.length; i++) {
						if (inputLine.contains(myCores[i]))
							returnMe.add(coreNames[i]);
					}
					isFound = true;
				}
				lineCounter++;
			}
			in.close();
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		catch(StringIndexOutOfBoundsException e) {
			//e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return returnMe;
	}*/

	//Getters
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

	//Setters
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

	public String toString() {
		return course+" | "+title+" | "+courseID+" | "+instructorL+" | "+days+" | "+times+" | "+coreReqs;
	}

	public String[] getInfo() {
		String[] returnMe = {course,title,""+courseID,instructorL,days,times,""+coreReqs};
		return returnMe;
	}

	public String[] getMoreInfo() {
		String[] returnMe = {term,studentLevel,school,""+seatsRemaining,description,""+myCoreReqs,""+pathwayReqs};
		return returnMe;
	}

	public String[] getEvenMoreInfo() {
		String[] returnMe = {""+maxUnits,""+minUnits,enrollmentInfo,location};
		return returnMe;
	}

	public String[] getAllInfo() {
		String[] returnMe = {""+courseID,""+termID,subject,course,title,description,""+coreReqs,""+pathwayReqs,""+myCoreReqs,enrollmentInfo,
							 ""+maxUnits,""+minUnits,instructorL,instructorF,days,times,location,""+seatsRemaining,term,studentLevel,school};
		return returnMe;
	}

	public String[] getSWInfo() {
		String[] returnMe = {course,title,""+courseID,instructorL,days,times,""+checkDangerZone(seatsRemaining)+downFrom};
		return returnMe;
	}

	public String getSWString() {
		return course+"\t| "+seatsRemaining+" seats"+downFrom;
	}

	@Override
	public int compareTo(SCUCourse compareMe) {
		return this.course.compareTo(compareMe.getCourse());
	}

    public static String checkDangerZone(int seats) {
        String returnMe = "";
        if (seats >= 30)
            returnMe += ANSI_GREEN;
        else if (seats >= 15)
            returnMe += ANSI_YELLOW;
        else if (seats >= 1)
            returnMe += ANSI_RED;
        else
            returnMe += ANSI_BLUE;
        return returnMe + seats + ANSI_RESET;
    }
}
