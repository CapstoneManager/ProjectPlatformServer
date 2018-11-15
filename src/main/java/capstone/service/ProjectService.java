package capstone.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import capstone.model.Project;
import capstone.model.Ranking;
import capstone.model.users.Student;
import capstone.repository.AdminConfigurationRepository;
import capstone.repository.ProjectsRepository;
import capstone.repository.RankingRepository;
import capstone.util.Constants;
import capstone.util.EncryptPassword;
import capstone.util.ProjectAssignment;

@Service
public class ProjectService {
	@Autowired
	ProjectsRepository repository;
	@Autowired
	UserService userService;
	@Autowired
	RankingRepository rankRepo;
	@Autowired
	AdminConfigurationRepository configRepo;

	private ProjectAssignment maxAlgorithm;
	private static String folder_name = "src/main/java/capstone/algorithm/real_data";
	private static int NUM_RANKED = 5; // number of projects that each student can rank
	public static Map<Double, ProjectAssignment> algorithms = new HashMap<>();
	public static Map<Double, Integer> iterations = new HashMap<>();
	private List<Project> savedProjects = new ArrayList<Project>();

	// public List<Student> applyRankingsToStudents(List<Student> students,
	// List<Project> projects) {
	// List<Ranking> rankings = rankRepo.findAll();
	// for (Ranking rank : rankings) {
	// Student student = null;
	// for (Student s : students) {
	// if (s.getUserId() == rank.getStudentId()) {
	// student = s;
	// }
	// }

	// Project project = null;
	// for (Project p : projects) {
	// if (p.getProjectId() == rank.getProjectId()) {
	// project = p;
	// }
	// }

	// if (project != null && student != null) {
	// String projectName = project.getProjectName();
	// student.rankings.put(projectName, rank.getRank());
	// student.orderedRankings.add(projectName);

	// Integer p = ProjectAssignment.getStudentSatScore(rank.getRank());
	// project.incSum_p(p);
	// project.incN();
	// }
	// }
	// return students;
	// }

	public List<Project> runAlgorithm(String semester, String year) {

		for (int iteration = 0; iteration < 30; iteration++) {
			List<Project> projects = new ArrayList<>();
			List<Student> students = new ArrayList<>(userService.getStudents());

			for (Project p : findAll()) {

				if (p.getSemester().equals(semester) && p.getYear().equals(year)) // && p.getStatusId() == 2)
					projects.add(new Project(p));
			}
			for (Student s : userService.getStudents()) {
				if (s.getSemester().equals(semester) && s.getYear().equals(year))
					students.add(new Student(s));
			}
			if (projects.size() == 0 || students.size() == 0)
				continue;
			// students = (ArrayList<Student>) applyRankingsToStudents(students, projects);

			ProjectAssignment algorithm = new ProjectAssignment(projects, students);
			algorithm.run(iteration, NUM_RANKED, folder_name);
			double groupSatScore = algorithm.algoSatScore;
			algorithms.put(groupSatScore, algorithm);
			iterations.put(groupSatScore, iteration);
		}
		Double maxScore;
		if (algorithms.size() == 0) {
			maxScore = 0.0;
		} else {
			maxScore = Collections.max(algorithms.keySet());
		}

		maxAlgorithm = algorithms.get(maxScore);
		Integer maxIteration = iterations.get(maxScore);
		System.out.println("maxScore: " + maxScore + ". maxIteration: " + maxIteration);

		System.out.println(maxAlgorithm.JSONOutputWeb());
		setSavedProjects(maxAlgorithm.assignedProjects());
		return getSavedProjects();
	}

	public void initTables() {
		Vector<Project> projects = new Vector<>();
		Vector<Student> students = new Vector<>();
		String line = null;
		String currentYear = new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());

		try {
			BufferedReader projectsBR = new BufferedReader(new FileReader(folder_name + "/projects.txt"));

			while ((line = projectsBR.readLine()) != null) {
				String[] elements = line.split(" ");

				Project newProject = new Project(ProjectAssignment.getStudentSatScore(1));
				newProject.setProjectName(elements[0]);
				newProject.setProjectId(projects.size()); // TODO: MAKE THIS DYNAMIC WITH AUTOINCREMENT
				newProject.setMinSize(Integer.parseInt(elements[1]));
				newProject.setMaxSize(Integer.parseInt(elements[2]));
				newProject.setSemester(returnSemester());
				newProject.setYear(currentYear);
				newProject.setStakeholderId(newProject.getProjectId());
				projects.addElement(newProject);

				System.out.println("Saving project: " + newProject.getProjectName());
				save(newProject);
			}

			projectsBR.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// import users and rankings from text file
		try {
			BufferedReader studentsBR = new BufferedReader(new FileReader(folder_name + "/rankings.txt"));

			while ((line = studentsBR.readLine()) != null) {
				String[] elements = line.split(" ");

				Student newStudent = new Student();
				newStudent.setFirstName("Student");
				String last = elements[0].substring(7);
				newStudent.setLastName(last);
				newStudent.setEmail(elements[0] + "@usc.edu");
				newStudent.setPassword(EncryptPassword.encryptPassword("student"));
				newStudent.setSemester(returnSemester());
				newStudent.setYear(currentYear);

				// newStudent.setStudentId(students.size());
				// newStudent.setUserId(students.size());

				List<Integer> rankings = new ArrayList<>();

				for (int i = 1; i <= NUM_RANKED; i++) { // for the student's Top 5 projects...
					int projectId = Integer.parseInt(elements[i]);
					Project rankedProject = projects.elementAt(projectId - 1); // !!! SUBTRACT 1, as the ranking's
																				// indices skip 0 for readability
					rankings.add(rankedProject.getProjectId());
				}

				newStudent.setOrderedRankings(rankings);

				userService.saveUser(newStudent);
				students.addElement(newStudent);
				// writer.println(newStudent);
			}

			// writer.println("");
			studentsBR.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// for(Student s: students) {
		// for (Map.Entry<String, Integer> entry : s.rankings.entrySet()) {
		// Project p = GetProjectWithName(projects, entry.getKey());
		// int projectId = p.getProjectId();
		// Long studentId = s.getUserId();

		// rankRepo.save(new Ranking(studentId, projectId, entry.getValue()));
		// }
		// }
	}

	Project GetProjectWithName(Vector<Project> projects, String projname) {
		for (int j = 0; j < projects.size(); j++) {
			if (projects.get(j).getProjectName().equals(projname))
				return projects.get(j);
		}
		return null;
	}

	public void save(Project project) {
		repository.save(project);
	}

	public List<Project> findAll() {
		return (List<Project>) repository.findAll();
	}

	// public void saveRanking(int projectId, Long userId, int ranking) {
	// // rankRepo.save(new Ranking(userId, projectId, ranking));

	// }

	public Project findByProjectId(int projectId) {
		return repository.findByProjectId(projectId);
	}

	public void saveAssignment(List<Project> projects) {
		/*
		 * AdminConfiguration ac = configRepo.findById(Long.valueOf(1)); if (ac == null)
		 * { ac = new AdminConfiguration(); } ArrayList<Project> finalProjects =
		 * (ArrayList<Project>) ac.getAssignment(); for (Project p : projects) { Project
		 * saveProj = findByProjectId(p.getProjectId()); List<Student> saveMembers =
		 * saveProj.getMembers(); for (Student s : p.getMembers()) {
		 * saveMembers.add(userService.findByUserId(s.getUserId())); }
		 * saveProj.setMembers(saveMembers); finalProjects.add(saveProj); }
		 * ac.setAssignment(finalProjects); configRepo.save(ac);
		 */
		setSavedProjects(projects);
	}

	public List<Project> getExistingAssignments(String semester, String year) {
		/*
		 * AdminConfiguration ac = configRepo.findById(Long.valueOf(1)); if (ac == null)
		 * { return null; } System.out.println(ac.getAssignment()); return
		 * ac.getAssignment();
		 */
		System.out.println(semester + " " + year);
		List<Project> existingProjects = repository.findAll();
		List<Project> projectsToBeReturned = new ArrayList<Project>();
		for (Project p : existingProjects) {
			if (p.getSemester().equals(semester) && p.getYear().equals(year)) {
				projectsToBeReturned.add(new Project(p));
			}

		}
		
		List<Student> students = (List<Student>) userService.getStudents();
		// students = (ArrayList<Student>) applyRankingsToStudents(students,
		// existingProjects);
		for (Student s : students) {
			if (s.getSemester().equals(semester) && s.getYear().equals(year)) {
				int assignedIndex = existingProjects.indexOf(s.getProject());
				if (assignedIndex > -1 && assignedIndex < projectsToBeReturned.size()) {
					projectsToBeReturned.get(assignedIndex).members.add(s);
				}
			}
		}
		System.out.println(projectsToBeReturned.size());
		return projectsToBeReturned;
	}

	public boolean assignmentExistance() {
		boolean exists = false;
		List<Project> existingProjects = repository.findAll();
		List<Student> students = (List<Student>) userService.getStudents();
		for (Student s : students) {
			int assignedIndex = existingProjects.indexOf(s.getProject());
			if (assignedIndex > -1) {
				exists = true;
			}

		}
		return exists;
	}

	public String returnSemester() throws ParseException {
		String semester;

		String currentDateString = new SimpleDateFormat("MM-dd").format(Calendar.getInstance().getTime());

		SimpleDateFormat format = new SimpleDateFormat("MM-dd");

		java.util.Date currentDate = format.parse(currentDateString);
		java.util.Date fallDate = format.parse(Constants.FALLSTART);
		java.util.Date yearEnd = format.parse(Constants.YEAREND);
		java.util.Date yearStart = format.parse(Constants.YEARSTART);
		java.util.Date summerDate = format.parse(Constants.SUMMERSTART);

		if (currentDate.compareTo(fallDate) >= 0 && currentDate.compareTo(yearEnd) <= 0) {
			semester = Constants.FALL;
		} else if (currentDate.compareTo(yearStart) >= 0 && currentDate.compareTo(summerDate) <= 0) {
			semester = Constants.SPRING;
		} else {
			semester = Constants.SUMMER;
		}

		return semester;
	}

	public List<Project> getSavedProjects() {
		return savedProjects;
	}

	public void setSavedProjects(List<Project> savedProjects) {
		this.savedProjects = savedProjects;
	}
}
