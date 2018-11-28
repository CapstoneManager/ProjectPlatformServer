package capstone.controller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import capstone.model.AdminConfiguration;
import capstone.model.Project;
import capstone.model.users.Student;
import capstone.repository.AdminConfigurationRepository;
import capstone.service.ProjectService;
import capstone.service.UserService;

@RestController
@RequestMapping("/admin")
public class AdminConfigurationController {

	private final String USER_AGENT = "Mozilla/5.0";

	@Autowired
	private AdminConfigurationRepository acRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private ProjectService projectService;

	@PostMapping("/configurations/save")
	public AdminConfiguration saveConfigurations(@RequestBody AdminConfiguration adminConfig) {
		acRepository.deleteAll();
		System.out.println("Date one: " + adminConfig.deliverableOneDate.toString());
		System.out.println("Number of ranked projects: " + adminConfig.numRankedProjects);
		return acRepository.save(adminConfig);
	}

	@GetMapping("/configurations/current")
	public AdminConfiguration getConfiguration() {
		Long currentId = (long) 1;
		return acRepository.findOne(currentId);
	}

	@GetMapping("/algorithm-status")
	public @ResponseBody Boolean getAlgorithmStatus() {
		try {
			HttpsURLConnection connection;
			URL url = new URL("https://cs401projects.tk:5000/status");
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			System.out.println("SERVER RESPONSE: " + response.toString());
			if (response.toString().equals("true")) {
				return Boolean.TRUE;
			}

		} catch (Exception e) {
			return Boolean.FALSE;
		}
		return Boolean.FALSE;
	}

	@GetMapping("/run-algorithm")
	public @ResponseBody String runAlgorithm() {
		JSONObject toRet = new JSONObject();
		Random random = new Random();

		try {
			Collection<Student> studentsList = userService.getStudents();
			Collection<Project> projectsList = projectService.findAll();

			// students
			JSONArray students = new JSONArray();
			for (Student student : studentsList) {
				students.put(student.getUserId().toString());
			}

			// projects
			JSONArray projects = new JSONArray();
			for (Project project : projectsList) {
				projects.put(Integer.toString(project.getProjectId()));
			}

			// projMinCapacity
			JSONObject projMinCapacity = new JSONObject();
			for (Project project : projectsList) {
				projMinCapacity.put(Integer.toString(project.getProjectId()), project.getMinSize());
			}

			// projMaxCapacity
			JSONObject projMaxCapacity = new JSONObject();
			for (Project project : projectsList) {
				projMaxCapacity.put(Integer.toString(project.getProjectId()), project.getMaxSize());
			}

			// rankings
			JSONArray rankings = new JSONArray();
			for (Student student : studentsList) {
				JSONArray studentRankings = new JSONArray();
				HashMap<Integer, Integer> rankedProjectIds = student.getRankedProjectIds(); // id -> value
				HashSet<Integer> rankSet = new HashSet<>(rankedProjectIds.values());
				for (Project project : projectsList) {
					if (rankedProjectIds.containsKey(project.getProjectId())) {
						studentRankings.put(rankedProjectIds.get(project.getProjectId()) + 1);
					} else {
						studentRankings.put(6);
					}
				}
				while (rankSet.size() < 5) {
					int rand = random.nextInt(projectsList.size());
					while (((Integer) studentRankings.get(rand)).equals(6) == false) {
						rand = random.nextInt(projectsList.size());
					}
					boolean set = false;
					for (int i = 0; i < 5 && set == false; i++) {
						if (rankSet.contains(i) == false) {
							rankSet.add(i);
							studentRankings.put(rand, i + 1);
							set = true;
						}
					}
				}
				rankings.put(studentRankings);
			}

			toRet.put("students", students);
			toRet.put("projects", projects);
			toRet.put("projMinCapacity", projMinCapacity);
			toRet.put("projMaxCapacity", projMaxCapacity);
			toRet.put("rankings", rankings);
		} catch (Exception e) {

		}
		try {
		HttpsURLConnection connection;
		URL url = new URL("https://cs401projects.tk:5000/algorithm");
		connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);

		System.out.println("writing");

		DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(toRet.toString());
		wr.flush();
		wr.close();

		connection.getResponseCode();

		System.out.println("written");
		} catch (Exception e) {
		e.printStackTrace();
		}

		return toRet.toString();
	}
}
