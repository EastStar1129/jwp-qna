package qna.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.sun.istack.NotNull;

import qna.domain.common.DatedAtEntity;
import qna.exception.CannotDeleteException;

@Entity
@Table(name = "question")
public class Question extends DatedAtEntity {
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	@Column(name = "title", length = 100)
	private String title;
	@Column(name = "contents")
	private String contents;
	@NotNull
	@Column(name = "deleted")
	private boolean deleted = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "writer_id", foreignKey = @ForeignKey(name = "fk_question_writer"))
	private User writer;
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "question")
	private List<Answer> answers;

	public Question(String title, String contents) {
		this(null, title, contents);
	}

	public Question(Long id, String title, String contents) {
		this.id = id;
		this.title = title;
		this.contents = contents;
		answers = new ArrayList<>();
	}

	public Question writeBy(User writer) {
		this.writer = writer;
		return this;
	}

	public boolean isOwner(User writer) {
		return this.writer.equals(writer);
	}

	public void addAnswer(Answer answer) {
		answer.toQuestion(this);
		answers.add(answer);
	}

	public List<DeleteHistory> delete(User loginUser) throws CannotDeleteException {
		validationOwner(loginUser);
		validationAnswers(loginUser);

		List<DeleteHistory> deleteHistories = new ArrayList<>();
		setDeleted(true);
		deleteHistories.add(new DeleteHistory(ContentType.QUESTION, id, writer, LocalDateTime.now()));
		for (Answer answer : answers) {
			answer.setDeleted(true);
			deleteHistories.add(
					new DeleteHistory(ContentType.ANSWER, answer.getId(), answer.getWriter(), LocalDateTime.now()));
		}
		return deleteHistories;
	}

	private void validationOwner(User loginUser) throws CannotDeleteException {
		if (!isOwner(loginUser)) {
			throw new CannotDeleteException("질문을 삭제할 권한이 없습니다.");
		}
	}

	private void validationAnswers(User loginUser) throws CannotDeleteException {
		for (Answer answer : answers) {
			validationAnswer(answer, loginUser);
		}
	}

	private void validationAnswer(Answer answer, User loginUser) throws CannotDeleteException {
		if (!answer.isOwner(loginUser)) {
			throw new CannotDeleteException("다른 사람이 쓴 답변이 있어 삭제할 수 없습니다.");
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public User getWriter() {
		return writer;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public List<Answer> answers() {
		return answers;
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Question other = (Question) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Question{" + "id=" + id + ", title='" + title + '\'' + ", contents='" + contents + '\'' + ", writerId="
				+ writer.getId() + ", deleted=" + deleted + '}';
	}
}
