package com.backend.api.forumhub.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(schema = "hub")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Long id;
    @Column(length = 200)
    @NotBlank(message = "Title is mandatory")
    private String title;
    @Column
    @NotBlank(message = "Message is mandatory")
    private String message;
    @Column
    private LocalDateTime createdAt;
    @Column(name = "status", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    @ManyToOne
    @JoinColumns(@JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "user_id")))
    private User author;
    @ManyToOne
    @JoinColumns(@JoinColumn(name = "course_id", foreignKey = @ForeignKey(name = "course_id")))
    private Course course;
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Answer> answers;

    public Topic(String title, String message, User author, Course course){
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.author = author;
        this.course = course;
        this.status = Status.UNSOLVED;
    }

    public Topic(String title, String message, Status status){
        this.title = title;
        this.message = message;
        this.status = status;
    }


}
