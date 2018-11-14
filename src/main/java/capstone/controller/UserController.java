package capstone.controller;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import capstone.config.JwtTokenProvider;
import capstone.model.RegisteredStudentEmail;
import capstone.model.users.Admin;
import capstone.model.users.Stakeholder;
import capstone.model.users.Student;
import capstone.model.users.User;
import capstone.repository.RegisteredStudentEmailRepository;
import capstone.service.EmailService;
import capstone.service.UserService;
import capstone.util.Constants;
import capstone.util.EncryptPassword;

@RestController
@RequestMapping("/users")
public class UserController {
	@Autowired
	private UserService userService;
	@Autowired
	private RegisteredStudentEmailRepository regRepo;
	@Autowired
	private EmailService emailService;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	public UserController() {
	}

	@GetMapping("/init")
	public String setAdmin() {
		Admin admin = new Admin();
		admin.setFirstName("Jeffrey");
		admin.setLastName("Miller");
		admin.setEmail("admin@usc.edu");
		admin.setPassword(EncryptPassword.encryptPassword("admin"));
		userService.saveUser(admin);
		Stakeholder stakeholder = new Stakeholder();
		stakeholder.setFirstName("Micheal");
		stakeholder.setLastName("Schindler");
		stakeholder.setEmail("schindler@usc.edu");
		stakeholder.setOrganization("USC Viterbi School of Engineering");
		stakeholder.setPassword(EncryptPassword.encryptPassword("stakeholder"));
		userService.saveUser(stakeholder);
		return Constants.SUCCESS;
	}

	@GetMapping("")
	public Collection<User> getUsers() {
		return userService.getUsers();
	}

	@GetMapping("/{email:.+}")
	public User getUser(@PathVariable("email") String email) {
		System.out.println(email);
		return userService.findUserByEmail(email);
	}

	@PostMapping("/{email:.+}/delete")
	public Boolean deleteUser(@PathVariable("email") String email) {
		System.out.println("deleted");
		User user = userService.findUserByEmail(email);
		if (user != null) {
			userService.deleteUser(user);
			return true;
		}
		return false;
	}

	@GetMapping("/stakeholders")
	public Collection<Stakeholder> getStakeholders() {
		return userService.getStakeholders();
	}

	@GetMapping("/students")
	public Collection<Student> getStudents() {
		return userService.getStudents();
	}

	@PostMapping("/update-info")
	public void updateUserInfo(@RequestBody Map<String, String> info) {
		String originalEmail = info.get(Constants.ORIGINAL_EMAIL);
		String newEmail = info.get(Constants.EMAIL);
		String firstName = info.get(Constants.FIRST_NAME);
		String lastName = info.get(Constants.LAST_NAME);
		String userType = info.get(Constants.USER_TYPE);
		String phone = info.get(Constants.PHONE);
		String semester = info.get(Constants.SEMESTER);
		String year = info.get(Constants.YEAR);

		User user = findUser(originalEmail);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(newEmail);
		user.setUserType(userType);
		user.setPhone(phone);
		user.setSemester(semester);
		user.setYear(year);
		userService.saveUser(user);
	}

	public User findUser(String email) {
		return userService.findUserByEmail(email);
	}

	public User findUserByAddr(String addr) {
		return userService.findUserByAddr(addr);
	}

	/* Registration */

	// Admin registration
	@PostMapping("/register/admin")
	public @ResponseBody String adminRegistrationAttempt(@RequestBody Map<String, String> info) {
		String email = info.get(Constants.EMAIL);
		String firstName = info.get(Constants.FIRST_NAME);
		String lastName = info.get(Constants.LAST_NAME);
		String phone = info.get(Constants.PHONE);
		String encryptedPassword = EncryptPassword.encryptPassword(info.get(Constants.PASSWORD));

		// Check if email is a registered student email and not already registered
		if (regRepo.findByEmail(email) != null && userService.findStudentByEmail(email) == null) {
			Admin admin = new Admin();
			admin.setFirstName(firstName);
			admin.setLastName(lastName);
			admin.setEmail(email);
			admin.setPhone(phone);
			admin.setPassword(encryptedPassword);
			admin.setUserType(Constants.ADMIN);
			userService.saveUser(admin);
			System.out.println("New admin created");
			return Constants.SUCCESS;
		}
		return Constants.EMPTY;
	}

	// Student registration
	@PostMapping("/register/student")
	public @ResponseBody ResponseEntity<?> studentRegistrationAttempt(@RequestBody Map<String, String> info) {
		String email = info.get(Constants.EMAIL);
		String firstName = info.get(Constants.FIRST_NAME);
		String lastName = info.get(Constants.LAST_NAME);
		String phone = info.get(Constants.PHONE);
		String encryptedPassword = EncryptPassword.encryptPassword(info.get(Constants.PASSWORD));

		System.out.println(email);
		System.out.println(firstName);
		System.out.println(lastName);
		System.out.println(phone);
		System.out.println(encryptedPassword);

		// Check if email is a registered student email and not already registered

		if (regRepo.findByEmail(email) != null && userService.findStudentByEmail(email) == null) {
			RegisteredStudentEmail rse = regRepo.findByEmail(email);
			Student s = new Student();
			s.setFirstName(firstName);
			s.setLastName(lastName);
			s.setEmail(email);
			s.setPhone(phone);
			s.setPassword(encryptedPassword);
			s.setUserType(Constants.STUDENT);
			s.setSemester(rse.getSemester());
			s.setYear(rse.getYear());
			userService.saveUser(s);
			System.out.println("New student created");

			// regRepo.delete()
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
		}
		return ResponseEntity.badRequest().body(null);
	}

	// Stake holder registration
	@PostMapping("/register/stakeholder")
	public @ResponseBody ResponseEntity<?> stakeholderRegistrationAttempt(@RequestBody Map<String, String> info) {
		System.out.println("Start reg");
		String email = info.get(Constants.EMAIL);
		String name = info.get(Constants.FIRST_NAME);
		String lastName = info.get(Constants.LAST_NAME);
		String phone = info.get(Constants.PHONE);
		String companyName = info.get(Constants.COMPANY);
		String encryptedPassword = EncryptPassword.encryptPassword(info.get(Constants.PASSWORD));

		// Check if email has already been registered
		if (userService.findStakeholderByEmail(email) == null) {
			Stakeholder s = new Stakeholder();
			s.setFirstName(name);
			s.setLastName(lastName);
			s.setEmail(email);
			s.setPhone(phone);
			s.setOrganization(companyName);
			s.setPassword(encryptedPassword);
			s.setUserType(Constants.STAKEHOLDER);
			userService.saveUser(s);
			System.out.println("New stakeholder created");
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
		}
		return ResponseEntity.badRequest().body(null);
	}

	// Admin can register student emails and send an invitation to the platform
	@RequestMapping(value = "/invite/students", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
	public void studentEmailRegistrationAttempt(@RequestBody Map<String, String> emailsData) {
		System.out.println(emailsData);
		System.out.println("Received HTTP POST");
		String year = emailsData.get("year");
		String semester = emailsData.get("semester");
		System.out.println(year);
		System.out.println(semester);
		

		String[] emailsArray = emailsData.get(Constants.EMAILS).split("\n");

		for (String e : emailsArray) {
			// Save the email to registered student email table
			regRepo.save(new RegisteredStudentEmail(e, year, semester));
			// Send an email invitation
			emailService.sendEmail("401 Platform Invite",
					"Congratulations! \nPlease sign up using the following link. \n \ncs401Projects.tk/register/student",
					e);
			System.out.println("Sent invite to: " + e);
		}
	}

	/* Login */

	@PostMapping("/login")
	public ResponseEntity<String> login(@RequestBody User login) {

		String email = login.getEmail();
		String password = login.getPassword();

		User user = userService.findUserByEmail(email);
		boolean valid = user != null ? EncryptPassword.checkPassword(password, user.getPassword()) : false;

		if (!valid) {
			return ResponseEntity.badRequest().body(null);
		}

		String token = jwtTokenProvider.createToken(user.getEmail(), user.getUserType());
		return ResponseEntity.ok(token);
	}

}