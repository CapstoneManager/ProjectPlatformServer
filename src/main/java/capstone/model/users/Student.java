package capstone.model.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import capstone.model.Project;

@Entity
public class Student extends User {

	public String uscid; // only valid if userType = Student

	@OneToOne(targetEntity = Project.class)
	Project project;

	@Transient
	public List<Integer> orderedRankings;

	private Integer one;
	private Integer two;
	private Integer three;
	private Integer four;
	private Integer five;

	public Student() {
		setOrderedRankings(getOrderedRankings());
	}

	public Student(Student orig) {
		this.setFirstName(orig.getFirstName());
		this.setLastName(orig.getLastName());
		this.setEmail(orig.getEmail());
		this.setUserId(orig.getUserId());
		this.setSemester(orig.getSemester());
		this.project = orig.project;
	}

	public String toString() {
		return ("Student #" + this.uscid + ": '" + this.getFirstName());
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public void setOrderedRankings(List<Integer> rankings) {
		this.one = rankings.get(0);
		this.two = rankings.get(1);
		this.three = rankings.get(2);
		this.four = rankings.get(3);
		this.five = rankings.get(4);
		this.orderedRankings = rankings.subList(0, 5);
	}

	public List<Integer> getOrderedRankings() {
		List<Integer> rankings = new ArrayList<>();
		rankings.add(0, one);
		rankings.add(1, two);
		rankings.add(2, three);
		rankings.add(3, four);
		rankings.add(4, five);
		return rankings;
	}

	public HashMap<Integer, Integer> getRankedProjectIds() { // projectId -> rank
		HashMap<Integer, Integer> rankedProjectIds = new HashMap<>();
		List<Integer> rankings = getOrderedRankings();
		for (int i = 0; i < 5; i++) {
			if (rankings.get(i) != null) {
				rankedProjectIds.put(rankings.get(i), i);
			}
		}
		return rankedProjectIds;
	}

}
