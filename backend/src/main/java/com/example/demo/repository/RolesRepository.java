package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Roles;

public interface RolesRepository extends JpaRepository<Roles, Integer> {
	Optional<Roles> findByRoleName(String roleName);

}
