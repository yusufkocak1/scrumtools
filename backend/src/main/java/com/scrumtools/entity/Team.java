package com.scrumtools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String teamName;

    @Column(nullable = false)
    private String teamCode;

    @Column(nullable = false)
    private String adminEmail;

    // Task customId üretimi için kalıcı sayaç (null = mevcut task'lerden başlatılacak)
    @Column(name = "task_sequence")
    private Long taskSequence;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TeamMember> members = new ArrayList<>();

    // Her takım bir organizasyona bağlı olmak zorundadır
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Birincil (varsayılan) proje — görev oluşturulurken proje seçilmezse buraya düşer.
     * {@link #projects} kümesinin içinde de yer alır.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Takımın üzerinde çalıştığı projeler — bir takım aynı anda birden fazla projede
     * çalışabilir. Sprint takım seviyesinde kalır (takımın zaman kutusu), görevler ise
     * bu projelerden birine ait olur.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_projects",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    @Builder.Default
    private Set<Project> projects = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
