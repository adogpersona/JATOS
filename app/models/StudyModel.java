package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.TypedQuery;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import services.ErrorMessages;
import services.IOUtils;
import services.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Domain model and DAO of a study.
 * 
 * @author Kristian Lange
 */
@Entity
public class StudyModel {

	public static final String MEMBERS = "user";
	public static final String TITLE = "title";
	public static final String JSON_DATA = "jsonData";
	public static final String DESCRIPTION = "description";
	public static final String DIRNAME_PREFIX = "dirNamePrefix";
	public static final String STUDY = "study";

	@Id
	@GeneratedValue
	@JsonView(JsonUtils.JsonForPublix.class)
	private Long id;

	@JsonView({ JsonUtils.JsonForPublix.class, JsonUtils.JsonForIO.class })
	private String title;

	@Lob
	@JsonView({ JsonUtils.JsonForPublix.class, JsonUtils.JsonForIO.class })
	private String description;

	/**
	 * Timestamp of the creation or the last update of this study
	 */
	private Timestamp date;

	/**
	 * If a study is locked, it can't be changed.
	 */
	private boolean locked = false;

	/**
	 * Prefix of the directory name of this study
	 */
	@JsonView(JsonUtils.JsonForIO.class)
	private String dirNamePrefix;

	@Lob
	@JsonView({ JsonUtils.JsonForPublix.class, JsonUtils.JsonForIO.class })
	private String jsonData;

	@JsonIgnore
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "StudyMemberMap", joinColumns = { @JoinColumn(name = "study_id", referencedColumnName = "id") }, inverseJoinColumns = { @JoinColumn(name = "member_email", referencedColumnName = "email") })
	private Set<UserModel> memberList = new HashSet<UserModel>();

	@JsonView(JsonUtils.JsonForIO.class)
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@OrderColumn(name = "componentList_order")
	@JoinColumn(name = "study_id")
	private List<ComponentModel> componentList = new ArrayList<ComponentModel>();

	public StudyModel() {
	}

	/**
	 * Constructor for cloning (without members)
	 */
	public StudyModel(StudyModel study) {
		// Don't clone members and field 'locked'
		this.description = study.description;
		this.dirNamePrefix = study.dirNamePrefix;
		this.jsonData = study.jsonData;
		this.title = study.title;
		this.locked = false;
		ComponentModel clone;
		for (ComponentModel component : study.componentList) {
			clone = new ComponentModel(component);
			clone.setStudy(this);
			componentList.add(clone);
		}
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return this.title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDirNamePrefix(String dirNamePrefix) {
		this.dirNamePrefix = dirNamePrefix;
	}

	public String getDirNamePrefix() {
		return this.dirNamePrefix;
	}

	public void setDate(Timestamp timestamp) {
		this.date = timestamp;
	}

	public Timestamp getDate() {
		return this.date;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public boolean isLocked() {
		return this.locked;
	}

	public String getJsonData() {
		if (this.jsonData == null) {
			return null;
		}
		return JsonUtils.makePretty(jsonData);
	}

	public void setJsonData(String jsonDataStr) {
		if (!JsonUtils.isValidJSON(jsonDataStr)) {
			// Set the invalid string anyway. It will cause an error during
			// validate().
			this.jsonData = jsonDataStr;
			return;
		}
		this.jsonData = JsonUtils.asStringForDB(jsonDataStr);
	}

	public void setMemberList(Set<UserModel> memberList) {
		this.memberList = memberList;
	}

	public Set<UserModel> getMemberList() {
		return memberList;
	}

	public void addMember(UserModel user) {
		memberList.add(user);
	}

	public void removeMember(UserModel user) {
		memberList.remove(user);
	}

	public boolean hasMember(UserModel user) {
		return memberList.contains(user);
	}

	public void setComponentList(List<ComponentModel> componentList) {
		this.componentList = componentList;
	}

	public List<ComponentModel> getComponentList() {
		return this.componentList;
	}

	public void addComponent(ComponentModel component) {
		componentList.add(component);
	}

	public void removeComponent(ComponentModel component) {
		componentList.remove(component);
	}

	public boolean hasComponent(ComponentModel component) {
		return componentList.contains(component);
	}

	@JsonIgnore
	public ComponentModel getFirstComponent() {
		if (componentList.size() > 0) {
			return componentList.get(0);
		}
		return null;
	}

	@JsonIgnore
	public ComponentModel getNextComponent(ComponentModel component) {
		int index = componentList.indexOf(component);
		if (index < componentList.size() - 1) {
			return componentList.get(index + 1);
		}
		return null;
	}

	public void componentOrderMinusOne(ComponentModel component) {
		int index = componentList.indexOf(component);
		if (index > 0) {
			ComponentModel prevComponent = componentList.get(index - 1);
			componentOrderSwap(component, prevComponent);
		}
	}

	public void componentOrderPlusOne(ComponentModel component) {
		int index = componentList.indexOf(component);
		if (index < (componentList.size() - 1)) {
			ComponentModel nextComponent = componentList.get(index + 1);
			componentOrderSwap(component, nextComponent);
		}
	}

	public void componentOrderSwap(ComponentModel component1,
			ComponentModel component2) {
		int index1 = componentList.indexOf(component1);
		int index2 = componentList.indexOf(component2);
		ComponentModel.changeComponentOrder(component1, index2);
		ComponentModel.changeComponentOrder(component2, index1);
	}

	public List<ValidationError> validate() {
		List<ValidationError> errorList = new ArrayList<ValidationError>();
		if (title == null || title.isEmpty()) {
			errorList.add(new ValidationError(TITLE,
					ErrorMessages.MISSING_TITLE));
		}
		if (!Jsoup.isValid(title, Whitelist.none())) {
			errorList.add(new ValidationError(TITLE,
					ErrorMessages.NO_HTML_ALLOWED));
		}
		if (description != null
				&& !Jsoup.isValid(description, Whitelist.none())) {
			errorList.add(new ValidationError(DESCRIPTION,
					ErrorMessages.NO_HTML_ALLOWED));
		}
		if (dirNamePrefix == null || dirNamePrefix.isEmpty()) {
			errorList.add(new ValidationError(DIRNAME_PREFIX,
					ErrorMessages.MISSING_DIRNAME));
		}
		Pattern pattern = Pattern.compile(IOUtils.REGEX_ILLEGAL_IN_FILENAME);
		Matcher matcher = pattern.matcher(dirNamePrefix);
		if (dirNamePrefix != null && matcher.find()) {
			errorList.add(new ValidationError(DIRNAME_PREFIX,
					ErrorMessages.INVALID_DIR_NAME));
		}
		if (jsonData != null && !JsonUtils.isValidJSON(jsonData)) {
			errorList.add(new ValidationError(JSON_DATA,
					ErrorMessages.INVALID_JSON_FORMAT));
		}
		return errorList.isEmpty() ? null : errorList;
	}

	@Override
	public String toString() {
		return id + " " + title;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof StudyModel)) {
			return false;
		}
		StudyModel other = (StudyModel) obj;
		if (id == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!id.equals(other.getId())) {
			return false;
		}
		return true;
	}

	public static StudyModel findById(Long id) {
		return JPA.em().find(StudyModel.class, id);
	}

	public static List<StudyModel> findAll() {
		TypedQuery<StudyModel> query = JPA.em().createQuery(
				"SELECT e FROM StudyModel e", StudyModel.class);
		return query.getResultList();
	}

	public void persist() {
		JPA.em().persist(this);
	}

	public void merge() {
		JPA.em().merge(this);
	}

	public void remove() {
		JPA.em().remove(this);
	}

	public void refresh() {
		JPA.em().refresh(this);
	}

}
