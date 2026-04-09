package com.example.demo.repository; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.TagName;

import java.util.Optional;

@Repository
public interface TagNameRepository extends JpaRepository<TagName, Long> {
    Optional<TagName> findByTagName(String tagName);
}