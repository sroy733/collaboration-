package com.family_tree.familytree;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")  // Specifies the table name in the database
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Uses AUTO_INCREMENT in MySQL
    @Column(name = "user_id")  // Maps this field to the "user_id" column in the database
    private Integer id;

    @Column(name = "username", nullable = false, length = 200)  // Maps to "username", not null, max length 200
    private String username;

    @Column(name = "email_address", nullable = false, length = 200)  // Maps to "email_address", not null, max length 200
    private String email;

    // One user can be associated with multiple collaborations
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collaboration> collaborations;

    // One user can own multiple family trees
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FamilyTree> ownedFamilyTrees;

    //Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Collaboration> getCollaborations() {
        return collaborations;
    }

    public void setCollaborations(List<Collaboration> collaborations) {
        this.collaborations = collaborations;
    }

    public List<FamilyTree> getOwnedFamilyTrees() {
        return ownedFamilyTrees;
    }

    public void setOwnedFamilyTrees(List<FamilyTree> ownedFamilyTrees) {
        this.ownedFamilyTrees = ownedFamilyTrees;
    }
}
