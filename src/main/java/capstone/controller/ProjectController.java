package capstone.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import capstone.model.Project;
import capstone.model.users.Stakeholder;
import capstone.model.users.Student;
import capstone.model.users.User;
import capstone.service.EmailService;
import capstone.service.ProjectService;
import capstone.service.UserService;
import capstone.util.Constants;

@RestController
@RequestMapping("/projects")
public class ProjectController {
	@Autowired
	private ProjectService projectService;
	@Autowired
	private UserService userService;
	@Autowired
	private EmailService emailService;

	// Initialize database tables with sample students and projects taken from the
	// Spring 2018 class.
	// Used for initializing the database for testing purposes
	@GetMapping("/init")
	public String initTables() {
		projectService.initTables();
		return Constants.SUCCESS;
	}

	/* Getting projects from user information */

	@GetMapping("")
	public List<Project> getProjects() {
		return projectService.findAll();
	}

	@GetMapping("/student/{email:.+}/rankings")
	public List<Project> getSemesterProjects(@PathVariable("email") String email) {
		User user = userService.findUserByEmail(email);
		String semester = user.getSemester();
		List<Project> all = projectService.findAll();
		for (java.util.Iterator<Project> i = all.iterator(); i.hasNext();) {
			Project p = (Project) i.next();
			if (!p.getSemester().equals(semester)) {
				i.remove();
			}
		}
		return all;
	}

	// Get all projects that a stakeholder owns
	@GetMapping("/stakeholder/{email:.+}")
	public List<Project> getProjectsByEmail(@PathVariable("email") String email) {
		Stakeholder user = userService.findStakeholderByEmail(email);
		List<Project> projects = userService.getStakeholderProjects(user);
		return projects;
	}

	// Get one project that a stakeholder owns
	@GetMapping("/stakeholder/{email:.+}/{projectId}")
	public Project getProjectByEmailAndId(@PathVariable("email") String email,
			@PathVariable("projectId") Long projectId) {
		Stakeholder user = userService.findStakeholderByEmail(email);
		List<Project> projects = userService.getStakeholderProjects(user);
		for (Project project : projects) {
			if (project.getProjectId() == projectId) {
				return project;
			}
		}
		return null;
	}

	// Get a student's project
	@GetMapping("/student/{email:.+}")
	public @ResponseBody Project getUserProject(@PathVariable("email") String email) {
		Student user = (Student) userService.findUserByEmail(email);
		if (user.getProject() == null) {
			Project newProject = new Project();
			return newProject;
		}
		return user.getProject();
	}

	/* Getting users from project information */

	@GetMapping("/{projectId}/students")
	public @ResponseBody List<User> getAllStudentsOnProject(@PathVariable("projectId") int projectId) {
		return userService.findAllByProject(projectService.findByProjectId(projectId));
	}

	@GetMapping("/{projectId}/stakeholder")
	public @ResponseBody User getStakeholderOnProject(@PathVariable("projectId") int projectId) {
		List<Stakeholder> stakeholders = (List<Stakeholder>) userService.getStakeholders();
		for (Stakeholder s : stakeholders) {
			for (Project p : s.getProjectIds()) {
				if (p.getProjectId() == projectId) {
					return s;
				}
			}
		}
		Stakeholder s = new Stakeholder();
		return s;
	}

	/* Project Matching */

	@GetMapping("/assignment")
	public @ResponseBody List<Project> projectAssignment(@RequestParam String semester, String year) {
		System.out.println("RUN ALGORITHM");
		List<Project> assignments = projectService.runAlgorithm(semester, year);
		return assignments;
	}

	@GetMapping("/getassignment")
	public @ResponseBody List<Project> getProjectAssignment(@RequestParam String semester, String year) {
		List<Project> projects = new ArrayList<Project>();
		if (projectService.assignmentExistance()) {
			System.out.println("Assignment exists!");
			projects = projectService.getExistingAssignments(semester, year);
		}
		return projects;
	}

	@GetMapping("/assignment/exists")
	public @ResponseBody String assignmentExists(@RequestParam String semester, String year) {
		List<Project> existing = projectService.getExistingAssignments(semester, year);
		if (existing != null && existing.size() > 0) {
			return "true";
		}
		return "false";
	}

	// Assign projects to students
	@PostMapping("/assign-to-students")
	public @ResponseBody String assignProjectsToStudents(@RequestBody String projectMatches) throws JSONException {
		JSONObject data = new JSONObject(projectMatches);
		System.out.println("IN ASSIGN TO STUDENTS");
		List<Project> updatedProjects = new ArrayList<Project>();
		
		Iterator<String> keys = data.keys();
		System.out.println(data.toString());
		
		while(keys.hasNext()) {
		    String key = keys.next();
			JSONArray projectValue = (JSONArray) data.get(key);

			String projectId = projectValue.getString(0);
			Project project = projectService.findByProjectId(Integer.parseInt(projectId));
			if (project.getProjectId() > 0) {
				updatedProjects.add(project);
				// Email each student in the group the information
				Student student = userService.findByUserId(Long.parseLong(key));
				System.out.println("Project: " + project.getProjectId() + "\n" + "Student: " + student.getUserId());
				// Set the given project for each student
				student.setProject(project);

				userService.saveUser(student);
			}
		}
		projectService.saveAssignment(updatedProjects);
		System.out.println("END ASSIGN TO STUDENTS");
		return Constants.SUCCESS;
	}

	// Save projects when altered
	@PostMapping("/save-assignments")
	public @ResponseBody String saveProjectsToStudents(@RequestBody List<Project> projectMatches) {
		List<Project> updatedProjects = new ArrayList<Project>();

		for (Project proj : projectMatches) {
			if (proj.getProjectId() > 0) {
				Project project = projectService.findByProjectId(proj.getProjectId());
				updatedProjects.add(project);

				for (Student student : proj.getMembers()) {
					// Set the given project for each student
					Student saveStudent = userService.findByUserId(student.getUserId());
					saveStudent.setProject(project);

					userService.saveUser(saveStudent);
				}
			}
		}
		projectService.saveAssignment(updatedProjects);
		return Constants.SUCCESS;
	}

	// Submit project ranking for a student
	// @PostMapping("/rankingsSubmitAttempt/{email:.+}")
	@PostMapping("/{email:.+}/submit-ranking")
	public @ResponseBody String projectRankingsSubmission(@PathVariable("email") String email,
			@RequestBody List<Integer> rankings) {
		Student student = (Student) userService.findUserByEmail(email);
		List<Integer> orderedRankings = rankings.subList(0, 5);
		System.out.println(orderedRankings);
		student.setOrderedRankings(orderedRankings);
		userService.saveUser(student);
		return Constants.SUCCESS;
	}

	/* Project submission and status */

	// When a stakeholder submits a proposal
	// Save a new project and attach a stakeholder to that project
	@PostMapping("/save/{email:.+}")
	public @ResponseBody Project saveData(@PathVariable("email") String email, @RequestBody Project project) {
		System.out.println("Received HTTP POST");
		System.out.println(project);
		System.out.println(project.getProjectName());
		project.setStatusId(1);
		User user = userService.findUserByEmail(email);
		project.setStakeholderId(user.getUserId());
		projectService.save(project);
		userService.saveProject(user, project);
		return project;
	}

	@PostMapping("/pending/{projectId}")
	public @ResponseBody String pendingProjects(@PathVariable("projectId") int projectId) {
		Project project = projectService.findByProjectId(projectId);
		project.setStatusId(1);
		projectService.save(project);
		return Constants.SUCCESS;
	}

	@PostMapping("/approve/{projectId}")
	public @ResponseBody String approveProjects(@PathVariable("projectId") int projectId) {
		Project project = projectService.findByProjectId(projectId);
		project.setStatusId(2);
		projectService.save(project);
		return Constants.SUCCESS;
	}

	@PostMapping("/reject/{projectId}")
	public @ResponseBody String rejectProjects(@PathVariable("projectId") int projectId) {
		Project project = projectService.findByProjectId(projectId);
		project.setStatusId(3);
		projectService.save(project);
		return Constants.SUCCESS;
	}

	@PostMapping("/change/{projectId}")
	public @ResponseBody String requestChangeProjects(@PathVariable("projectId") int projectId) {
		Project project = projectService.findByProjectId(projectId);
		project.setStatusId(4);
		projectService.save(project);
		return Constants.SUCCESS;
	}

	@PostMapping("/edit/{projectId}")
	public @ResponseBody String editProject(@PathVariable("projectId") int projectId,
			@RequestBody Project updated_project) {
		System.out.println("Updating Project");
		Project project = projectService.findByProjectId(projectId);
		project.setStatusId(4);

		project.setSemester(updated_project.getSemester());
		project.setYear(updated_project.getYear());
		project.setTechnologies(updated_project.getTechnologies());
		project.setProjectName(updated_project.getProjectName());
		project.setDescription(updated_project.getDescription());
		project.setBackground(updated_project.getBackground());
		project.setMinSize(updated_project.getMinSize());
		project.setMaxSize(updated_project.getMaxSize());

		System.out.println(project);
		System.out.println(project.getProjectName());
		projectService.save(project);
		return Constants.SUCCESS;
	}

	@PostMapping("/editMinor/{projectid}")
	public @ResponseBody String editProjectMinor(@PathVariable("projectId") int projectId,
			@RequestBody Project updated_project) {
		Project project = projectService.findByProjectId(projectId);
		project.setProjectName(updated_project.getProjectName());
		project.setTechnologies(updated_project.getTechnologies());
		project.setBackground(updated_project.getBackground());
		project.setDescription(updated_project.getDescription());

		projectService.save(project);
		return Constants.SUCCESS;
	}
}
